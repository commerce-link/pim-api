package pl.commercelink.pim.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Immutable lookup structure over a PIM index snapshot. Global entries form one scope, each store's entries
 * another; a store's view searches its own scope first, by any identifier, then the global one (GTIN before MPN
 * within each scope). Within a scope a shared identifier resolves to the later entry. In a store's view a global
 * entry sharing any GTIN/MPN with the store's own entries is shadowed: lookups by that identifier resolve to the
 * own entry, {@code findAll()} leaves the global one out, and a composite lookup carrying its pimId resolves to
 * the shadowing entry. {@code findByPimId} still returns a visible entry by its own id.
 */
public final class PimEntryIndex {

    private final UnaryOperator<String> gtinKey;
    private final UnaryOperator<String> mpnKey;
    private final Map<String, PimEntry> byPimId = new HashMap<>();
    private final Scope global = new Scope();
    private final Map<String, Scope> stores = new HashMap<>();
    private final PimLookup globalView;

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

    public PimLookup global() {
        return globalView;
    }

    public PimLookup forStore(String storeId) {
        if (storeId == null || storeId.isBlank()) {
            return globalView;
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

    private final class Scope {
        private final List<PimEntry> entries = new ArrayList<>();
        private final Map<String, PimEntry> byGtin = new HashMap<>();
        private final Map<String, PimEntry> byMpn = new HashMap<>();

        void add(PimEntry entry) {
            entries.add(entry);
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
                (gtin ? byGtin : byMpn).put(key, entry);
            }
        }
    }

    private final class View implements PimLookup {
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
            for (PimIdentifier id : entry.identifiers()) {
                if (id == null || id.type() == null) {
                    continue;
                }
                boolean gtin = id.type() == PimIdentifierType.GTIN;
                String key = key(gtin ? gtinKey : mpnKey, id.value());
                if (key != null && (gtin ? own.byGtin : own.byMpn).containsKey(key)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Optional<PimEntry> findByPimIdOrGtinsOrMpns(String pimId, Collection<String> gtins, Collection<String> mpns) {
            if (own == null) {
                return PimLookup.super.findByPimIdOrGtinsOrMpns(pimId, gtins, mpns);
            }
            Collection<String> gtinValues = gtins == null ? Collections.emptyList() : gtins;
            Collection<String> mpnValues = mpns == null ? Collections.emptyList() : mpns;
            PimEntry byId = pimId == null || pimId.isBlank() ? null : byPimId.get(pimId);
            if (byId != null && storeId.equals(byId.ownerStoreId())) {
                return Optional.of(byId);
            }
            if (byId != null && byId.global()) {
                // a global pimId yields only to the own entry shadowing that very entry: an own entry matching some
                // other code of the key must not pull a second inventory group onto itself (one pimId, two products)
                return Optional.of(shadowing(byId).orElse(byId));
            }
            Optional<PimEntry> ownHit = firstHit(own, gtinValues, mpnValues);
            if (ownHit.isPresent()) {
                return ownHit;
            }
            return firstHit(global, gtinValues, mpnValues);
        }

        private Optional<PimEntry> shadowing(PimEntry globalEntry) {
            if (globalEntry.identifiers() == null) {
                return Optional.empty();
            }
            return firstHit(own, valuesOf(globalEntry, PimIdentifierType.GTIN), valuesOf(globalEntry, PimIdentifierType.MPN));
        }

        private List<String> valuesOf(PimEntry entry, PimIdentifierType type) {
            List<String> values = new ArrayList<>();
            for (PimIdentifier id : entry.identifiers()) {
                if (id != null && id.type() == type) {
                    values.add(id.value());
                }
            }
            return values;
        }

        private Optional<PimEntry> firstHit(Scope scope, Collection<String> gtins, Collection<String> mpns) {
            for (String gtin : gtins) {
                String key = key(gtinKey, gtin);
                if (key != null && scope.byGtin.containsKey(key)) {
                    return Optional.of(scope.byGtin.get(key));
                }
            }
            for (String mpn : mpns) {
                String key = key(mpnKey, mpn);
                if (key != null && scope.byMpn.containsKey(key)) {
                    return Optional.of(scope.byMpn.get(key));
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
        public Optional<PimEntry> findByGtin(String gtin) {
            return find(key(gtinKey, gtin), true);
        }

        @Override
        public Optional<PimEntry> findByMpn(String mpn) {
            return find(key(mpnKey, mpn), false);
        }

        @Override
        public Optional<PimEntry> findByGtinOrMpn(String gtin, String mpn) {
            // own entries by either code first, then global ones: the order of the composite lookup and of findAll
            List<String> gtins = Collections.singletonList(gtin);
            List<String> mpns = Collections.singletonList(mpn);
            if (own != null) {
                Optional<PimEntry> ownHit = firstHit(own, gtins, mpns);
                if (ownHit.isPresent()) {
                    return ownHit;
                }
            }
            return firstHit(global, gtins, mpns);
        }

        private Optional<PimEntry> find(String key, boolean gtin) {
            if (key == null) {
                return Optional.empty();
            }
            if (own != null) {
                PimEntry hit = (gtin ? own.byGtin : own.byMpn).get(key);
                if (hit != null) {
                    return Optional.of(hit);
                }
            }
            return Optional.ofNullable((gtin ? global.byGtin : global.byMpn).get(key));
        }
    }
}
