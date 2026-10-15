package pl.commercelink.pim.api;

import java.util.List;
import java.util.function.Consumer;

public interface PimCatalog extends PimLookup {

    /** Lookups of this catalog cover global entries only; a store's view adds that store's own entries first. */
    default PimLookup forStore(String storeId) {
        return this;
    }

    void submit(PimEntryRequest request);

    void refresh();

    void onEntryAdded(Consumer<PIMEntryAddedEvent> listener);

    void onEntryDeleted(Consumer<PIMEntryDeletedEvent> listener);

    default void dispatch(Object event) {}

    default List<Brand> allBrands() {
        return List.of();
    }

    default List<PimCategory> allCategories() {
        return List.of();
    }

    default void submitCategoryMatch(CategoryMatchRequest request) {}

    default void onCategoryMatched(Consumer<CategoryMatchedEvent> listener) {}
}
