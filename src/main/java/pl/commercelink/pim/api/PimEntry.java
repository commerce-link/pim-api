package pl.commercelink.pim.api;

import com.fasterxml.jackson.annotation.JsonInclude;

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
        String ownerStoreId,
        // pimId of the global entry this store entry was cloned from; null for every other entry
        @JsonInclude(JsonInclude.Include.NON_NULL) String clonedFromPimId
) {

    public PimEntry(String pimId, List<PimIdentifier> identifiers, String brand, String name,
                    String category, String subcategory, boolean approved,
                    Integer netWeightInGrams, Integer grossWeightInGrams, String categoryId, String ownerStoreId) {
        this(pimId, identifiers, brand, name, category, subcategory, approved,
                netWeightInGrams, grossWeightInGrams, categoryId, ownerStoreId, null);
    }

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
        return valuesOf(PimIdentifierType.GTIN);
    }

    public List<String> mpns() {
        return valuesOf(PimIdentifierType.MPN);
    }

    private List<String> valuesOf(PimIdentifierType type) {
        if (identifiers == null) {
            return List.of();
        }
        return identifiers.stream()
                .filter(i -> i != null && i.type() == type)
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
        List<PimIdentifier> ofType = identifiers.stream().filter(i -> i != null && i.type() == type).toList();
        return ofType.stream().filter(PimIdentifier::primary).findFirst()
                .or(() -> ofType.stream().findFirst())
                .map(PimIdentifier::value);
    }
}
