package pl.commercelink.pim.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PimCategoryTest {

    @Test
    void categoryWithoutParentIsTopLevel() {
        PimCategory category = new PimCategory("151", null, "Komputery i urządzenia peryferyjne", "pl");

        assertThat(category.topLevel()).isTrue();
    }

    @Test
    void categoryWithParentIsNotTopLevel() {
        PimCategory category = new PimCategory("788", "151", "Procesory", "pl");

        assertThat(category.topLevel()).isFalse();
    }

    @Test
    void serializesToExactlyTheFourContractFields() throws Exception {
        PimCategory category = new PimCategory("788", "151", "Procesory", "pl");

        String json = new ObjectMapper().writeValueAsString(category);

        assertThat(json).isEqualTo(
                "{\"id\":\"788\",\"parentId\":\"151\",\"name\":\"Procesory\",\"lang\":\"pl\"}");
    }
}
