package pl.commercelink.pim.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
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
        assertThat(index.forStore("s2").findAll()).containsExactly(ownedByS2, global);
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

    @Test
    void storeFindAllListsOwnEntriesFirstAndHidesShadowedGlobalOnes() {
        PimEntry unshadowed = entry("g2", "800", "M-8", null);
        PimEntryIndex idx = new PimEntryIndex(List.of(global, unshadowed, ownedByS1, onlyInS1), TRIM, TRIM);

        assertThat(idx.forStore("s1").findAll()).containsExactly(ownedByS1, onlyInS1, unshadowed);
        assertThat(idx.forStore("s2").findAll()).containsExactly(global, unshadowed);
        assertThat(idx.global().findAll()).containsExactly(global, unshadowed);
    }

    @Test
    void globalEntrySharingOnlyAnMpnWithAStoreEntryIsShadowedInThatStore() {
        PimEntry ownMpnOnly = entry("s1-9", "999", " M-1 ", "s1");
        PimEntryIndex idx = new PimEntryIndex(List.of(global, ownMpnOnly), TRIM, TRIM);

        assertThat(idx.forStore("s1").findAll()).containsExactly(ownMpnOnly);
    }

    @Test
    void ownMpnLosesToGlobalGtinInGtinOrMpnLookup() {
        PimEntry ownMpn = entry("s1-7", "777", "M-1", "s1");
        PimEntryIndex idx = new PimEntryIndex(List.of(global, ownMpn), TRIM, TRIM);

        assertThat(idx.forStore("s1").findByGtinOrMpn("590", "M-1")).contains(global);
    }

    @Test
    void compositeLookupPrefersOwnPimIdEvenWhenGtinsHitElsewhere() {
        assertThat(index.forStore("s1").findByPimIdOrGtinsOrMpns("s1-2", List.of("590"), List.of()))
                .contains(onlyInS1);
    }

    @Test
    void compositeLookupPrefersOwnIdentifierOverGlobalPimId() {
        assertThat(index.forStore("s1").findByPimIdOrGtinsOrMpns("g1", List.of("590"), List.of()))
                .contains(ownedByS1);
    }

    @Test
    void compositeLookupPrefersOwnHitOnLaterGtinOverGlobalHitOnEarlierOne() {
        PimEntry globalOnly = entry("g3", "300", "M-30", null);
        PimEntryIndex idx = new PimEntryIndex(List.of(globalOnly, global, onlyInS1), TRIM, TRIM);

        assertThat(idx.forStore("s1").findByPimIdOrGtinsOrMpns(null, List.of("300", "591"), List.of()))
                .contains(onlyInS1);
        assertThat(idx.forStore("s1").findByPimIdOrGtinsOrMpns(null, List.of("300"), List.of("M-2")))
                .contains(onlyInS1);
    }

    @Test
    void compositeLookupFallsBackToGlobalPimIdThenGlobalGtinsThenGlobalMpns() {
        PimEntry globalOnly = entry("g3", "300", "M-30", null);
        PimEntryIndex idx = new PimEntryIndex(List.of(globalOnly, global, onlyInS1), TRIM, TRIM);
        PimLookup s1 = idx.forStore("s1");

        assertThat(s1.findByPimIdOrGtinsOrMpns("g1", List.of("300"), List.of())).contains(global);
        assertThat(s1.findByPimIdOrGtinsOrMpns("missing", List.of("300"), List.of("M-1"))).contains(globalOnly);
        assertThat(s1.findByPimIdOrGtinsOrMpns(null, List.of("nope"), List.of("M-30"))).contains(globalOnly);
        assertThat(s1.findByPimIdOrGtinsOrMpns("s2-1", List.of(), List.of())).isEmpty();
    }

    @Test
    void compositeLookupToleratesNullAndBlankArguments() {
        PimLookup s1 = index.forStore("s1");
        assertThat(s1.findByPimIdOrGtinsOrMpns(null, null, null)).isEmpty();
        assertThat(s1.findByPimIdOrGtinsOrMpns(" ", Arrays.asList(null, " ", "591"), Arrays.asList((String) null)))
                .contains(onlyInS1);
        assertThat(index.global().findByPimIdOrGtinsOrMpns(null, null, Arrays.asList(null, "M-1"))).contains(global);
    }

    @Test
    void globalViewKeepsPimIdFirstInCompositeLookup() {
        PimEntry other = entry("g4", "400", "M-40", null);
        PimEntryIndex idx = new PimEntryIndex(List.of(global, other), TRIM, TRIM);
        assertThat(idx.global().findByPimIdOrGtinsOrMpns("g4", List.of("590"), List.of())).contains(other);
    }

    @Test
    void nullEntriesAndMalformedIdentifiersAreSkipped() {
        List<PimIdentifier> ids = new ArrayList<>();
        ids.add(null);
        ids.add(new PimIdentifier("123", null));
        ids.add(new PimIdentifier("124", PimIdentifierType.GTIN));
        PimEntry odd = new PimEntry("odd", ids, "b", "n", "c", "s", true, null, null, null, "s1");
        PimEntry noIds = new PimEntry("none", null, "b", "n", "c", "s", true, null, null, null, null);
        List<PimEntry> entries = new ArrayList<>(List.of(global, odd, noIds));
        entries.add(1, null);

        PimEntryIndex idx = new PimEntryIndex(entries, TRIM, TRIM);

        assertThat(idx.size()).isEqualTo(3);
        assertThat(idx.forStore("s1").findByGtin("124")).contains(odd);
        assertThat(idx.forStore("s1").findByMpn("123")).isEmpty();
        assertThat(idx.forStore("s1").findAll()).containsExactly(odd, global, noIds);
        assertThat(odd.gtins()).containsExactly("124");
        assertThat(noIds.gtins()).isEmpty();
        assertThat(noIds.mpns()).isEmpty();
    }
}
