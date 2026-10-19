package pl.commercelink.pim.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Product lookups within one scope: the global catalog or a store's view. A store's view checks the store's own
 * entries first, by any of the given codes, and only then global ones; within a scope GTIN beats MPN.
 * See {@link PimEntryIndex} for shadowing.
 */
public interface PimLookup {

    List<PimEntry> findAll();

    Optional<PimEntry> findByPimId(String pimId);

    Optional<PimEntry> findByGtin(String gtin);

    Optional<PimEntry> findByMpn(String mpn);

    Optional<PimEntry> findByGtinOrMpn(String gtin, String mpn);

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
