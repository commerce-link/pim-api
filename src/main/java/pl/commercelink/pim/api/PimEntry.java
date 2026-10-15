package pl.commercelink.pim.api;

import java.util.List;
import java.util.Optional;

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
        String categoryId,
        String ownerStoreId
) {

    public PimEntry(String pimId, List<PimIdentifier> identifiers, String brand, String name,
                    String category, String subcategory, boolean approved,
                    Integer netWeightInGrams, Integer grossWeightInGrams) {
        this(pimId, identifiers, brand, name, category, subcategory, approved,
                netWeightInGrams, grossWeightInGrams, null, null);
    }

    public PimEntry(String pimId, List<PimIdentifier> identifiers, String brand, String name,
                    String category, String subcategory, boolean approved,
                    Integer netWeightInGrams, Integer grossWeightInGrams, String categoryId) {
        this(pimId, identifiers, brand, name, category, subcategory, approved,
                netWeightInGrams, grossWeightInGrams, categoryId, null);
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

    // no get/is prefix on purpose: PIM serializes this record with Jackson
    public boolean global() {
        return ownerStoreId == null;
    }

    public boolean visibleTo(String storeId) {
        return ownerStoreId == null || ownerStoreId.equals(storeId);
    }

    public Optional<String> primaryGtin() {
        return primaryOf(PimIdentifierType.GTIN);
    }

    public Optional<String> primaryMpn() {
        return primaryOf(PimIdentifierType.MPN);
    }

    // a PIM older than 0.3.0 sends no flags: the first identifier of the type is the primary one
    private Optional<String> primaryOf(PimIdentifierType type) {
        if (identifiers == null) {
            return Optional.empty();
        }
        List<PimIdentifier> ofType = identifiers.stream().filter(i -> i.type() == type).toList();
        return ofType.stream().filter(PimIdentifier::primary).findFirst()
                .or(() -> ofType.stream().findFirst())
                .map(PimIdentifier::value);
    }
}
