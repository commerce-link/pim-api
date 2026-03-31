package pl.commercelink.pim.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PimCatalog {

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
        for (String gtin : gtins) {
            Optional<PimEntry> result = findByGtin(gtin);
            if (result.isPresent()) {
                return result;
            }
        }
        for (String mpn : mpns) {
            Optional<PimEntry> result = findByMpn(mpn);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
