package pl.commercelink.pim.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class PimCatalogTest {

    /** Minimal catalog that leaves every default method untouched. */
    private static final PimCatalog MINIMAL = new PimCatalog() {
        @Override
        public List<PimEntry> findAll() {
            return List.of();
        }

        @Override
        public Optional<PimEntry> findByPimId(String pimId) {
            return Optional.empty();
        }

        @Override
        public Optional<PimEntry> findByGtin(String gtin) {
            return Optional.empty();
        }

        @Override
        public Optional<PimEntry> findByMpn(String mpn) {
            return Optional.empty();
        }

        @Override
        public Optional<PimEntry> findByGtinOrMpn(String gtin, String mpn) {
            return Optional.empty();
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
    };

    @Test
    void allBrandsDefaultsToEmptyList() {
        assertThat(MINIMAL.allBrands()).isEmpty();
    }

    @Test
    void allCategoriesDefaultsToEmptyList() {
        assertThat(MINIMAL.allCategories()).isEmpty();
    }
}
