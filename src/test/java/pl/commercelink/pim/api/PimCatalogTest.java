package pl.commercelink.pim.api;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class PimCatalogTest {

    @Test
    void allCategoriesDefaultsToEmptyList() {
        PimCatalog catalog = new MinimalPimCatalog();

        assertThat(catalog.allCategories()).isEmpty();
    }

    @Test
    void catalogHasNoLookupsOfItsOwnSoEveryReadPicksAScope() {
        assertThat(PimView.class).isInterface();
        assertThat(PimView.class.isAssignableFrom(PimCatalog.class)).isFalse();
    }

    private static class MinimalPimCatalog implements PimCatalog {

        private final PimEntryIndex index = PimEntryIndex.empty();

        @Override
        public PimView withGlobalData() {
            return index.global();
        }

        @Override
        public PimView withStoreData(String storeId) {
            return index.forStore(storeId);
        }

        @Override
        public void submit(PimEntryRequest request) {
        }

        @Override
        public void refresh() {
        }

        @Override
        public void onEntryAdded(Consumer<PIMEntryAddedEvent> listener) {
        }

        @Override
        public void onEntryDeleted(Consumer<PIMEntryDeletedEvent> listener) {
        }
    }
}
