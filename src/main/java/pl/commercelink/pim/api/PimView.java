package pl.commercelink.pim.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Product lookups within one scope, taken from {@link PimCatalog#withGlobalData()} (global entries) or
 * {@link PimCatalog#withStoreData(String)} (a store's view). A store's view checks the store's own entries first, by
 * any of the given codes, and only then global ones; within a scope GTIN beats MPN. See {@link PimEntryIndex} for
 * shadowing.
 */
public interface PimView {

    List<PimEntry> findAll();

    Optional<PimEntry> findByPimId(String pimId);

    Optional<PimEntry> findByGtin(String gtin);

    Optional<PimEntry> findByMpn(String mpn);

    /**
     * The store's own entry cloned from the global entry {@code sourcePimId}, found by that id and not by codes (a
     * clone may have changed its codes). Empty in the global view, for a blank id and when the store has no such clone.
     */
    default Optional<PimEntry> findCloneOf(String sourcePimId) {
        return Optional.empty();
    }

    /** The store's own entries that are clones (clonedFromPimId set); empty for the global view. */
    default List<PimEntry> findOwnClones() {
        return List.of();
    }

    Optional<PimEntry> findByGtinOrMpn(String gtin, String mpn);

    /**
     * Resolves an inventory key: its pimId first, then its GTINs, then its MPNs. In a store's view a global pimId gives
     * way only to the store's own entry that shadows that very global entry (see {@link PimEntryIndex}); an own entry
     * that matches only some other code of the key does not take it over. For a key with mixed codes (the GTIN of a
     * global entry and the MPN of an unrelated own entry) the result can therefore differ from
     * {@link #findByGtinOrMpn}, which has no pimId and returns the own entry.
     */
    default Optional<PimEntry> findByPimIdOrGtinsOrMpns(String pimId, Collection<String> gtins, Collection<String> mpns) {
        if (pimId != null && !pimId.isBlank()) {
            Optional<PimEntry> result = findByPimId(pimId);
            if (result.isPresent()) {
                return result;
            }
        }
        for (String gtin : gtins == null ? List.<String>of() : gtins) {
            Optional<PimEntry> result = findByGtin(gtin);
            if (result.isPresent()) {
                return result;
            }
        }
        for (String mpn : mpns == null ? List.<String>of() : mpns) {
            Optional<PimEntry> result = findByMpn(mpn);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
