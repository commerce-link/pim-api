package pl.commercelink.pim.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PimEntryTest {

    @Test
    void categoryIsAPlainStringExposedByAccessor() {
        PimEntry entry = new PimEntry(
                "pim-1",
                List.of(),
                "Lenovo",
                "ThinkPad",
                "Laptops",
                "Business",
                true,
                1500,
                1800
        );

        assertThat(entry.category()).isEqualTo("Laptops");
    }

    @Test
    void categoryAcceptsArbitraryStringOutsideTheLegacyEnumDictionary() {
        PimEntry entry = new PimEntry(
                "pim-2",
                List.of(),
                "Bambu Lab",
                "X1 Carbon",
                "Drukarki 3D",
                "Resin",
                true,
                null,
                null
        );

        assertThat(entry.category()).isEqualTo("Drukarki 3D");
    }
}
