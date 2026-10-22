package pl.commercelink.pim.api;

import java.util.List;
import java.util.function.Consumer;

/**
 * Entry point to a PIM service. It holds no lookups itself: every read picks its scope first, through
 * {@link #withGlobalData()} or {@link #withStoreData(String)}, and then works on the returned {@link PimView}.
 * Views are cheap snapshots of the current index, so take a fresh one per operation rather than keeping it.
 */
public interface PimCatalog {

    /** Global entries only: what every store sees, without any store's own entries. */
    PimView withGlobalData();

    /**
     * The given store's view: its own entries first, then the global ones it does not shadow.
     *
     * @throws IllegalArgumentException when {@code storeId} is null or blank; global reads use {@link #withGlobalData()}
     */
    PimView withStoreData(String storeId);

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
