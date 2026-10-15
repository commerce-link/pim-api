package pl.commercelink.pim.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Immutable lookup structure over a PIM index snapshot. Global entries form one scope, each store's entries
 * another; a store's view searches its own scope first, then the global one. Within a scope a shared identifier
 * resolves to the later entry.
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
            if (entry.pimId() == null) {
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
            List<PimEntry> all = new ArrayList<>(global.entries);
            if (own != null) {
                all.addAll(own.entries);
            }
            return all;
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
            return findByGtin(gtin).or(() -> findByMpn(mpn));
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
