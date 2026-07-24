package pl.commercelink.pim.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
            @Override public List<PimEntry> findAll() { return List.of(); }
            @Override public Optional<PimEntry> findByPimId(String pimId) { return Optional.empty(); }
            @Override public Optional<PimEntry> findByGtin(String gtin) { return Optional.empty(); }
            @Override public Optional<PimEntry> findByMpn(String mpn) { return Optional.empty(); }
            @Override public Optional<PimEntry> findByGtinOrMpn(String gtin, String mpn) { return Optional.empty(); }
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
