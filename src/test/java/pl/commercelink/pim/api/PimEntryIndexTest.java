package pl.commercelink.pim.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

class PimEntryIndexTest {

    private static final UnaryOperator<String> TRIM = v -> v == null ? null : v.trim();

    private static PimEntry entry(String pimId, String gtin, String mpn, String owner) {
        return new PimEntry(pimId, List.of(new PimIdentifier(gtin, PimIdentifierType.GTIN),
                new PimIdentifier(mpn, PimIdentifierType.MPN)), "b", "n", "c", "s", true, null, null, null, owner);
    }

    private final PimEntry global = entry("g1", "590", "M-1", null);
    private final PimEntry ownedByS1 = entry("s1-1", "590", "M-1", "s1");
    private final PimEntry onlyInS1 = entry("s1-2", "591", "M-2", "s1");
    private final PimEntry ownedByS2 = entry("s2-1", "592", "M-3", "s2");
    private final PimEntryIndex index = new PimEntryIndex(List.of(global, ownedByS1, onlyInS1, ownedByS2), TRIM, TRIM);

    @Test
    void globalLookupSeesOnlyGlobalEntries() {
        assertThat(index.global().findByGtin("590")).contains(global);
        assertThat(index.global().findByGtin("591")).isEmpty();
        assertThat(index.global().findByPimId("s1-2")).isEmpty();
        assertThat(index.global().findAll()).containsExactly(global);
    }

    @Test
    void storeLookupPrefersItsOwnEntryOverTheGlobalOne() {
        assertThat(index.forStore("s1").findByGtin("590")).contains(ownedByS1);
        assertThat(index.forStore("s1").findByMpn("M-1")).contains(ownedByS1);
        assertThat(index.forStore("s1").findByGtin("591")).contains(onlyInS1);
    }

    @Test
    void storeLookupFallsBackToGlobalAndNeverSeesOtherStores() {
        assertThat(index.forStore("s2").findByGtin("590")).contains(global);
        assertThat(index.forStore("s2").findByGtin("591")).isEmpty();
        assertThat(index.forStore("s2").findByPimId("s1-2")).isEmpty();
        assertThat(index.forStore("s2").findByPimId("g1")).contains(global);
        assertThat(index.forStore("s2").findAll()).containsExactlyInAnyOrder(global, ownedByS2);
    }

    @Test
    void storeWithoutEntriesBehavesLikeGlobal() {
        assertThat(index.forStore("unknown").findByGtin("590")).contains(global);
        assertThat(index.forStore(null).findAll()).containsExactly(global);
        assertThat(index.forStore(" ").findByGtin("591")).isEmpty();
    }

    @Test
    void blankOrNullKeysFindNothing() {
        assertThat(index.forStore("s1").findByGtinOrMpn(null, null)).isEmpty();
        assertThat(index.global().findByGtin(" ")).isEmpty();
        assertThat(index.global().findByPimId(null)).isEmpty();
        assertThat(index.forStore("s1").findByGtinOrMpn(null, "M-2")).contains(onlyInS1);
    }

    @Test
    void laterEntryWinsWithinAScope() {
        PimEntry first = entry("a", "600", "X", null);
        PimEntry second = entry("b", "600", "Y", null);
        PimEntryIndex dup = new PimEntryIndex(List.of(first, second), TRIM, TRIM);
        assertThat(dup.global().findByGtin("600")).contains(second);
    }

    @Test
    void keysAreNormalizedOnBuildAndOnLookup() {
        PimEntryIndex upper = new PimEntryIndex(List.of(entry("u", "700", "abc", null)),
                TRIM, v -> v == null ? null : v.trim().toUpperCase());
        assertThat(upper.global().findByMpn(" abc ")).isPresent();
    }

    @Test
    void emptyIndexFindsNothing() {
        assertThat(PimEntryIndex.empty().forStore("s1").findAll()).isEmpty();
        assertThat(PimEntryIndex.empty().size()).isZero();
    }
}
