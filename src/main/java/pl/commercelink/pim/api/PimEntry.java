package pl.commercelink.pim.api;

import java.util.List;

public record PimEntry(
        String pimId,
        List<PimIdentifier> identifiers,
        String brand,
        String name,
        String category,
        String subcategory,
        boolean approved,
        Integer netWeightInGrams,
        Integer grossWeightInGrams,
        String categoryId
) {

    public PimEntry(String pimId, List<PimIdentifier> identifiers, String brand, String name,
                    String category, String subcategory, boolean approved,
                    Integer netWeightInGrams, Integer grossWeightInGrams) {
        this(pimId, identifiers, brand, name, category, subcategory, approved,
                netWeightInGrams, grossWeightInGrams, null);
    }

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
