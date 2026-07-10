package pl.commercelink.pim.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PimCategoriesTest {

    private final PimCategories categories = new PimCategories(List.of(
            new PimCategory("100", null, "Komputery", "Computers"),
            new PimCategory("200", null, "Dom", "Home"),
            new PimCategory("110", "100", "Podzespoły", "Components"),
            new PimCategory("111", "110", "Procesory", "Processors"),
            new PimCategory("112", "110", "Karty graficzne", "Graphics Cards"),
            new PimCategory("120", "100", "Laptopy", "Laptops"),
            new PimCategory("210", "200", "Meble", "Furniture")
    ));

    @Test
    void topLevelsReturnsOnlyCategoriesWithoutParent() {
        assertThat(categories.topLevels())
                .extracting(PimCategory::id)
                .containsExactly("100", "200");
    }

    @Test
    void childrenOfReturnsDirectChildrenOnly() {
        assertThat(categories.childrenOf("100"))
                .extracting(PimCategory::id)
                .containsExactly("110", "120");
    }

    @Test
    void childrenOfUnknownIdReturnsEmptyList() {
        assertThat(categories.childrenOf("999")).isEmpty();
    }

    @Test
    void leavesUnderReturnsAllLeafDescendants() {
        assertThat(categories.leavesUnder("100"))
                .extracting(PimCategory::id)
                .containsExactly("111", "112", "120");
    }

    @Test
    void leavesUnderALeafReturnsTheLeafItself() {
        assertThat(categories.leavesUnder("120"))
                .extracting(PimCategory::id)
                .containsExactly("120");
    }

    @Test
    void allReturnsEveryCategory() {
        assertThat(categories.all()).hasSize(7);
    }
}
