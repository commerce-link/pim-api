package pl.commercelink.pim.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PimEntryJsonTest {

    private final ObjectMapper json = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void payloadOfAnOldPimReadsAsAGlobalEntryWithoutFlags() throws Exception {
        String old = """
                {"pimId":"p","identifiers":[{"value":"1","type":"GTIN"}],"brand":"b","name":"n",
                 "category":"c","subcategory":"s","approved":true,"netWeightInGrams":1,
                 "grossWeightInGrams":2,"categoryId":"10"}""";
        PimEntry entry = json.readValue(old, PimEntry.class);
        assertThat(entry.ownerStoreId()).isNull();
        assertThat(entry.identifiers().get(0).primary()).isFalse();
        assertThat(entry.primaryGtin()).contains("1");
    }

    @Test
    void serializedEntryHasOnlyRecordComponents() throws Exception {
        PimEntry entry = new PimEntry("p", List.of(new PimIdentifier("1", PimIdentifierType.GTIN, true)),
                "b", "n", "c", "s", true, null, null, null, "s1");
        String out = json.writeValueAsString(entry);
        assertThat(out).contains("\"ownerStoreId\":\"s1\"").contains("\"primary\":true")
                .doesNotContain("\"global\"").doesNotContain("primaryGtin");
    }

    @Test
    void addedEventOfAnOldPimHasNoOwner() throws Exception {
        PIMEntryAddedEvent event = json.readValue("{\"pimId\":\"p\",\"eans\":[\"1\"],\"mfnCodes\":[]}",
                PIMEntryAddedEvent.class);
        assertThat(event.ownerStoreId()).isNull();
    }

    @Test
    void payloadWithoutClonedFromReadsAsNull() throws Exception {
        PimEntry entry = json.readValue("{\"pimId\":\"p\",\"identifiers\":[],\"ownerStoreId\":\"s1\"}", PimEntry.class);
        assertThat(entry.clonedFromPimId()).isNull();
    }

    @Test
    void nullClonedFromIsNotSerialized() throws Exception {
        PimEntry entry = new PimEntry("p", List.of(), "b", "n", "c", "s", true, null, null);
        assertThat(json.writeValueAsString(entry)).doesNotContain("clonedFromPimId");
    }

    @Test
    void clonedFromIsSerializedWhenSet() throws Exception {
        PimEntry entry = new PimEntry("p2", List.of(), "b", "n", "c", "s", true, null, null, null, "s1", "p1");
        String out = json.writeValueAsString(entry);
        assertThat(out).contains("\"clonedFromPimId\":\"p1\"");
        assertThat(json.readValue(out, PimEntry.class).clonedFromPimId()).isEqualTo("p1");
    }

    @Test
    void addedEventWithoutClonedFromReadsAsNull() throws Exception {
        PIMEntryAddedEvent event = json.readValue(
                "{\"pimId\":\"p\",\"eans\":[\"1\"],\"mfnCodes\":[],\"ownerStoreId\":\"s1\"}", PIMEntryAddedEvent.class);
        assertThat(event.ownerStoreId()).isEqualTo("s1");
        assertThat(event.clonedFromPimId()).isNull();
        assertThat(new PIMEntryAddedEvent("p", List.of(), List.of(), "s1").clonedFromPimId()).isNull();
    }
}
