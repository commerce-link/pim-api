package pl.commercelink.pim.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    void exposesComponentsThroughAccessors() {
        Category category = new Category("LAPTOP", "Laptop", "COMPUTERS");

        assertThat(category.key()).isEqualTo("LAPTOP");
        assertThat(category.displayName()).isEqualTo("Laptop");
        assertThat(category.groupKey()).isEqualTo("COMPUTERS");
    }

    @Test
    void equalityIsValueBased() {
        Category one = new Category("LAPTOP", "Laptop", "COMPUTERS");
        Category same = new Category("LAPTOP", "Laptop", "COMPUTERS");
        Category different = new Category("PHONE", "Phone", "MOBILE");

        assertThat(one).isEqualTo(same);
        assertThat(one).hasSameHashCodeAs(same);
        assertThat(one).isNotEqualTo(different);
    }

    @Test
    void allowsNullComponents() {
        Category category = new Category("KEY", null, null);

        assertThat(category.key()).isEqualTo("KEY");
        assertThat(category.displayName()).isNull();
        assertThat(category.groupKey()).isNull();
    }
}
