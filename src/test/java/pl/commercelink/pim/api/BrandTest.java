package pl.commercelink.pim.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrandTest {

    @Test
    void compactConstructorDefaultsNullAliasesToEmptyList() {
        Brand brand = new Brand("Apple", null);

        assertThat(brand.aliases()).isEmpty();
    }

    @Test
    void compactConstructorFiltersNullAliasElements() {
        Brand brand = new Brand("Asus", Arrays.asList("asus", null, "asustek"));

        assertThat(brand.aliases()).containsExactly("asus", "asustek");
    }

    @Test
    void compactConstructorMakesAliasesImmutable() {
        List<String> mutable = new ArrayList<>(List.of("asus", "asustek"));
        Brand brand = new Brand("Asus", mutable);

        mutable.add("rogue");

        assertThat(brand.aliases()).containsExactly("asus", "asustek");
        assertThatThrownBy(() -> brand.aliases().add("intruder"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void matchesReturnsFalseForNullInput() {
        Brand brand = new Brand("Apple", List.of("apple"));

        assertThat(brand.matches(null)).isFalse();
    }

    @Test
    void matchesReturnsFalseForBlankInput() {
        Brand brand = new Brand("Apple", List.of("apple"));

        assertThat(brand.matches("")).isFalse();
        assertThat(brand.matches("   ")).isFalse();
        assertThat(brand.matches("\t")).isFalse();
    }

    @Test
    void matchesAgainstCanonicalNameCaseInsensitively() {
        Brand brand = new Brand("Apple", List.of("apple"));

        assertThat(brand.matches("Apple")).isTrue();
        assertThat(brand.matches("APPLE")).isTrue();
        assertThat(brand.matches("apple")).isTrue();
    }

    @Test
    void matchesAgainstAliasCaseInsensitively() {
        Brand brand = new Brand("Asus", List.of("asus", "asustek"));

        assertThat(brand.matches("Asustek")).isTrue();
        assertThat(brand.matches("ASUSTEK")).isTrue();
    }

    @Test
    void matchesTrimsLeadingAndTrailingWhitespace() {
        Brand brand = new Brand("Asus", List.of("asustek"));

        assertThat(brand.matches("  Asustek  ")).isTrue();
        assertThat(brand.matches("\tAsus\n")).isTrue();
    }

    @Test
    void matchesReturnsFalseForUnknownInput() {
        Brand brand = new Brand("Asus", List.of("asus", "asustek"));

        assertThat(brand.matches("Frobnitz")).isFalse();
    }

    @Test
    void matchesWorksWhenNameIsNullAsLongAsAliasMatches() {
        Brand brand = new Brand(null, List.of("only-alias"));

        assertThat(brand.matches("only-alias")).isTrue();
        assertThat(brand.matches("ONLY-ALIAS")).isTrue();
        assertThat(brand.matches("nope")).isFalse();
    }

    @Test
    void matchesReturnsFalseWhenBothNameAndAliasesAreEmpty() {
        Brand brand = new Brand(null, List.of());

        assertThat(brand.matches("anything")).isFalse();
    }
}
