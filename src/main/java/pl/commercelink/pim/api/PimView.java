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
