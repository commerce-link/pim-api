package pl.commercelink.pim.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMatchContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void categoryMatchRequestSurvivesJsonRoundTrip() throws Exception {
        // given
        CategoryMatchRequest request = new CategoryMatchRequest(null, "5901234567890", "MFN-1", "Acme", "Widget Pro", null);

        // when
        CategoryMatchRequest result = objectMapper.readValue(objectMapper.writeValueAsString(request), CategoryMatchRequest.class);

        // then
        assertThat(result).isEqualTo(request);
    }

    @Test
    void categoryMatchedEventSurvivesJsonRoundTrip() throws Exception {
        // given
        CategoryMatchedEvent event = new CategoryMatchedEvent("5901234567890", "MFN-1", "CPU", "301", 0.87, "mock");

        // when
        CategoryMatchedEvent result = objectMapper.readValue(objectMapper.writeValueAsString(event), CategoryMatchedEvent.class);

        // then
        assertThat(result).isEqualTo(event);
    }

    @Test
    void categoryMatchMethodsHaveNoopDefaults() {
        // given
        PimCatalog catalog = new PimCatalog() {
            @Override public PimView withGlobalData() { return PimEntryIndex.empty().global(); }
            @Override public PimView withStoreData(String storeId) { return PimEntryIndex.empty().forStore(storeId); }
            @Override public void submit(PimEntryRequest request) {}
            @Override public void refresh() {}
            @Override public void onEntryAdded(Consumer<PIMEntryAddedEvent> listener) {}
            @Override public void onEntryDeleted(Consumer<PIMEntryDeletedEvent> listener) {}
        };

        // when / then
        catalog.submitCategoryMatch(new CategoryMatchRequest(null, "e", "m", "b", "n", null));
        catalog.onCategoryMatched(event -> {});
    }
}
