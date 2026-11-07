package pl.commercelink.pim.api;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Immutable lookup structure over a PIM index snapshot. Global entries form one scope, each store's entries
 * another; a store's view searches its own scope first, then the global one (GTIN before MPN within each scope).
 * Within a scope a shared GTIN resolves to the later entry; an MPN finds only an entry of the given brand, chosen
 * among all entries under the MPN, so an own entry of another brand does not hide a global one. In a store's view a
 * global entry sharing a GTIN with the store's own entries is shadowed: lookups by that GTIN resolve to the own
 * entry, {@code findAll()} leaves the global one out, and a composite lookup carrying its pimId resolves to the
 * shadowing entry. An MPN never shadows: the same code can stand for another product (another brand, another size).
 * When several own entries shadow one global entry, the composite lookup takes the first one found by the global
 * entry's own GTINs, in its order, not by the key's codes. {@code findByPimId} still returns a visible entry by its
 * own id.
 */
public final class PimEntryIndex {

    private final UnaryOperator<String> gtinKey;
    private final UnaryOperator<String> mpnKey;
    private final Map<String, PimEntry> byPimId = new HashMap<>();
    private final Scope global = new Scope();
    private final Map<String, Scope> stores = new HashMap<>();
    private final PimView globalView;

    public PimEntryIndex(Collection<PimEntry> entries, UnaryOperator<String> gtinKey, UnaryOperator<String> mpnKey) {
        this.gtinKey = gtinKey;
        this.mpnKey = mpnKey;
        for (PimEntry entry : entries) {
            if (entry == null || entry.pimId() == null) {
                continue;
            }
            byPimId.put(entry.pimId(), entry);
            Scope scope = entry.global() ? global : stores.computeIfAbsent(entry.ownerStoreId(), k -> new Scope());
            scope.add(entry);
        }
        this.globalView = new View(null, null);
    }

    public static PimEntryIndex empty() {
        return new PimEntryIndex(List.of(), UnaryOperator.identity(), UnaryOperator.identity());
    }

    public PimView global() {
        return globalView;
    }

    /** @throws IllegalArgumentException when {@code storeId} is null or blank; global reads use {@link #global()} */
    public PimView forStore(String storeId) {
        if (storeId == null || storeId.isBlank()) {
            throw new IllegalArgumentException("storeId is required for a store's view; global entries come from global() (PimCatalog.withGlobalData())");
        }
        return new View(storeId, stores.get(storeId));
    }

    public int size() {
        return byPimId.size();
    }

    private String key(UnaryOperator<String> normalizer, String value) {
        if (value == null) {
            return null;
        }
        String key = normalizer.apply(value);
        return key == null || key.isBlank() ? null : key;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean sameBrand(String brand, String other) {
        return !isBlank(brand) && !isBlank(other) && brand.trim().equalsIgnoreCase(other.trim());
    }

    private final class Scope {
        private final List<PimEntry> entries = new ArrayList<>();
        private final Map<String, PimEntry> byGtin = new HashMap<>();
        private final Map<String, PimEntry> byMpn = new HashMap<>();
        // only MPNs held by more than one entry, so that a unique code costs no list
        private final Map<String, List<PimEntry>> bySharedMpn = new HashMap<>();
        private final Map<String, PimEntry> byClonedFrom = new HashMap<>();

        void add(PimEntry entry) {
            entries.add(entry);
            if (entry.clonedFromPimId() != null && !entry.clonedFromPimId().isBlank()) {
                byClonedFrom.put(entry.clonedFromPimId(), entry);
            }
            if (entry.identifiers() == null) {
                return;
            }
            for (PimIdentifier id : entry.identifiers()) {
                if (id == null || id.type() == null) {
                    continue;
                }
                boolean gtin = id.type() == PimIdentifierType.GTIN;
                String key = key(gtin ? gtinKey : mpnKey, id.value());
                if (key == null) {
                    continue;
                }
                if (gtin) {
                    byGtin.put(key, entry);
                } else {
                    addMpn(key, entry);
                }
            }
        }

        private void addMpn(String key, PimEntry entry) {
            PimEntry previous = byMpn.put(key, entry);
            if (previous != null && previous != entry) {
                bySharedMpn.computeIfAbsent(key, k -> new ArrayList<>(List.of(previous))).add(entry);
            }
        }

        PimEntry findByGtinOrMpnAndBrand(String gtin, String mpn, String brand) {
            PimEntry byCode = gtin == null ? null : byGtin.get(gtin);
            return byCode != null ? byCode : findByMpnAndBrand(mpn, brand);
        }

        PimEntry findByMpnAndBrand(String mpn, String brand) {
            if (mpn == null || brand == null) {
                return null;
            }
            List<PimEntry> shared = bySharedMpn.get(mpn);
            if (shared == null) {
                PimEntry only = byMpn.get(mpn);
                return only != null && sameBrand(brand, only.brand()) ? only : null;
            }
            for (int i = shared.size() - 1; i >= 0; i--) {
                PimEntry candidate = shared.get(i);
                if (sameBrand(brand, candidate.brand())) {
                    return candidate;
                }
            }
            return null;
        }
    }

    private final class View implements PimView {
        private final String storeId;
        private final Scope own;

        View(String storeId, Scope own) {
            this.storeId = storeId;
            this.own = own;
        }

        @Override
        public List<PimEntry> findAll() {
            if (own == null) {
                return new ArrayList<>(global.entries);
            }
            // own entries first: callers that group by identifier (first match takes it) must hit the store's entry
            List<PimEntry> all = new ArrayList<>(own.entries);
            for (PimEntry entry : global.entries) {
                if (!shadowed(entry)) {
                    all.add(entry);
                }
            }
            return all;
        }

        private boolean shadowed(PimEntry entry) {
            if (entry.identifiers() == null) {
                return false;
            }
            for (String gtin : gtinsOf(entry)) {
                String key = key(gtinKey, gtin);
                if (key != null && own.byGtin.containsKey(key)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Optional<PimEntry> findByPimIdOrGtins(String pimId, Collection<String> gtins) {
            if (own == null) {
                return PimView.super.findByPimIdOrGtins(pimId, gtins);
            }
            Collection<String> gtinValues = gtins == null ? Collections.emptyList() : gtins;
            PimEntry byId = pimId == null || pimId.isBlank() ? null : byPimId.get(pimId);
            if (byId != null && storeId.equals(byId.ownerStoreId())) {
                return Optional.of(byId);
            }
            if (byId != null && byId.global()) {
                // a global pimId yields only to the own entry shadowing that very entry: an own entry matching some
                // other code of the key must not pull a second inventory group onto itself (one pimId, two products)
                return Optional.of(shadowing(byId).orElse(byId));
            }
            Optional<PimEntry> ownHit = firstHit(own, gtinValues);
            if (ownHit.isPresent()) {
                return ownHit;
            }
            return firstHit(global, gtinValues);
        }

        private Optional<PimEntry> shadowing(PimEntry globalEntry) {
            if (globalEntry.identifiers() == null) {
                return Optional.empty();
            }
            return firstHit(own, gtinsOf(globalEntry));
        }

        private List<String> gtinsOf(PimEntry entry) {
            List<String> values = new ArrayList<>();
            for (PimIdentifier id : entry.identifiers()) {
                if (id != null && id.type() == PimIdentifierType.GTIN) {
                    values.add(id.value());
                }
            }
            return values;
        }

        private Optional<PimEntry> firstHit(Scope scope, Collection<String> gtins) {
            for (String gtin : gtins) {
                String key = key(gtinKey, gtin);
                if (key != null && scope.byGtin.containsKey(key)) {
                    return Optional.of(scope.byGtin.get(key));
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<PimEntry> findByPimId(String pimId) {
            if (pimId == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(byPimId.get(pimId)).filter(e -> e.visibleTo(storeId));
        }

        @Override
        public Optional<PimEntry> findCloneOf(String sourcePimId) {
            if (own == null || sourcePimId == null || sourcePimId.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(own.byClonedFrom.get(sourcePimId));
        }

        @Override
        public List<PimEntry> findOwnClones() {
            return own == null ? List.of() : List.copyOf(own.byClonedFrom.values());
        }

        @Override
        public Optional<PimEntry> findByGtin(String gtin) {
            String key = key(gtinKey, gtin);
            if (key == null) {
                return Optional.empty();
            }
            if (own != null) {
                PimEntry hit = own.byGtin.get(key);
                if (hit != null) {
                    return Optional.of(hit);
                }
            }
            return Optional.ofNullable(global.byGtin.get(key));
        }

        @Override
        public Optional<PimEntry> findByMpnAndBrand(String mpn, String brand) {
            String key = key(mpnKey, mpn);
            if (own != null) {
                PimEntry hit = own.findByMpnAndBrand(key, brand);
                if (hit != null) {
                    return Optional.of(hit);
                }
            }
            return Optional.ofNullable(global.findByMpnAndBrand(key, brand));
        }

        @Override
        public Optional<PimEntry> findByGtinOrMpnAndBrand(String gtin, String mpn, String brand) {
            String gtinValue = key(gtinKey, gtin);
            String mpnValue = key(mpnKey, mpn);
            if (own != null) {
                PimEntry hit = own.findByGtinOrMpnAndBrand(gtinValue, mpnValue, brand);
                if (hit != null) {
                    return Optional.of(hit);
                }
            }
            return Optional.ofNullable(global.findByGtinOrMpnAndBrand(gtinValue, mpnValue, brand));
        }
    }
}
