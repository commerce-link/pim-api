package pl.commercelink.pim.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    void exposesKeyDisplayNameAndGroupKey() {
        Category category = new Category("CPU", "CPU", "PcComponents");

        assertThat(category.key()).isEqualTo("CPU");
        assertThat(category.displayName()).isEqualTo("CPU");
        assertThat(category.groupKey()).isEqualTo("PcComponents");
    }

    @Test
    void groupKeyMayBeNull() {
        Category category = new Category("Custom", "Custom", null);

        assertThat(category.groupKey()).isNull();
    }
}
