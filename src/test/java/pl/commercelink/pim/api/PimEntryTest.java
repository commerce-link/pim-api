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

    @Test
    void categoryIdIsExposedByAccessor() {
        PimEntry entry = new PimEntry(
                "pim-3",
                List.of(),
                "Gigabyte",
                "RTX 4070",
                "Karty graficzne",
                "GPU",
                true,
                300,
                500,
                "170"
        );

        assertThat(entry.categoryId()).isEqualTo("170");
    }

    @Test
    void nineArgConstructorLeavesCategoryIdNull() {
        PimEntry entry = new PimEntry(
                "pim-4",
                List.of(),
                "Lenovo",
                "ThinkPad",
                "Laptops",
                "Business",
                true,
                1500,
                1800
        );

        assertThat(entry.categoryId()).isNull();
    }

    @Test
    void primaryGtinIsTheFlaggedOne() {
        PimEntry entry = entry(List.of(
                new PimIdentifier("1", PimIdentifierType.GTIN),
                new PimIdentifier("2", PimIdentifierType.GTIN, true),
                new PimIdentifier("M1", PimIdentifierType.MPN)), null);
        assertThat(entry.primaryGtin()).contains("2");
        assertThat(entry.primaryMpn()).contains("M1");
    }

    @Test
    void primaryFallsBackToTheFirstOfItsTypeWhenNothingIsFlagged() {
        PimEntry entry = entry(List.of(
                new PimIdentifier("M1", PimIdentifierType.MPN),
                new PimIdentifier("1", PimIdentifierType.GTIN),
                new PimIdentifier("2", PimIdentifierType.GTIN)), null);
        assertThat(entry.primaryGtin()).contains("1");
        assertThat(entry.primaryMpn()).contains("M1");
    }

    @Test
    void primaryIsEmptyWhenTheTypeIsMissing() {
        PimEntry entry = entry(List.of(new PimIdentifier("1", PimIdentifierType.GTIN)), null);
        assertThat(entry.primaryMpn()).isEmpty();
    }

    @Test
    void globalEntryIsVisibleToEveryStoreAndStoreEntryOnlyToItsOwner() {
        PimEntry global = entry(List.of(), null);
        PimEntry owned = entry(List.of(), "s1");
        assertThat(global.global()).isTrue();
        assertThat(global.visibleTo("s1")).isTrue();
        assertThat(global.visibleTo(null)).isTrue();
        assertThat(owned.global()).isFalse();
        assertThat(owned.visibleTo("s1")).isTrue();
        assertThat(owned.visibleTo("s2")).isFalse();
        assertThat(owned.visibleTo(null)).isFalse();
    }

    @Test
    void oldConstructorsGiveAGlobalEntry() {
        PimEntry entry = new PimEntry("p", List.of(), "b", "n", "c", "s", true, 1, 2);
        assertThat(entry.ownerStoreId()).isNull();
        assertThat(new PimEntry("p", List.of(), "b", "n", "c", "s", true, 1, 2, "cid").ownerStoreId()).isNull();
    }

    private static PimEntry entry(List<PimIdentifier> ids, String owner) {
        return new PimEntry("p", ids, "b", "n", "c", "s", true, null, null, null, owner);
    }

    @Test
    void elevenArgConstructorGivesNoSource() {
        PimEntry entry = new PimEntry("p", List.of(), "b", "n", "c", "s", true, null, null, "cid", "s1");
        assertThat(entry.clonedFromPimId()).isNull();
        assertThat(entry.ownerStoreId()).isEqualTo("s1");
    }

    @Test
    void cloneKeepsItsSource() {
        PimEntry clone = new PimEntry("p2", List.of(), "b", "n", "c", "s", true, null, null, "cid", "s1", "p1");
        assertThat(clone.clonedFromPimId()).isEqualTo("p1");
        assertThat(clone.global()).isFalse();
    }
}
