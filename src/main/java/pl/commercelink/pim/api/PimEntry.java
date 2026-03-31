package pl.commercelink.pim.api;

import pl.commercelink.taxonomy.ProductCategory;

import java.util.List;

public record PimEntry(
        String pimId,
        List<PimIdentifier> identifiers,
        String brand,
        String name,
        ProductCategory category,
        String subcategory,
        boolean approved
) {

    public List<String> gtins() {
        return identifiers.stream()
                .filter(i -> i.type() == PimIdentifierType.GTIN)
                .map(PimIdentifier::value)
                .toList();
    }

    public List<String> mpns() {
        return identifiers.stream()
                .filter(i -> i.type() == PimIdentifierType.MPN)
                .map(PimIdentifier::value)
                .toList();
    }
}
