package pl.commercelink.pim.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(index.forStore("unknown").findAll()).containsExactly(global);
        assertThat(index.global().findAll()).containsExactly(global);
    }

    @Test
    void storeViewWithoutStoreIdFailsInsteadOfFallingBackToGlobal() {
        assertThatThrownBy(() -> index.forStore(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.forStore(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankOrNullKeysFindNothing() {
        assertThat(index.forStore("s1").findByGtinOrMpnAndBrand(null, null, "b")).isEmpty();
        assertThat(index.global().findByGtin(" ")).isEmpty();
        assertThat(index.global().findByPimId(null)).isEmpty();
        assertThat(index.forStore("s1").findByGtinOrMpnAndBrand(null, "M-2", "b")).contains(onlyInS1);
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
        assertThat(upper.global().findByMpnAndBrand(" abc ", "b")).isPresent();
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
    void globalEntrySharingOnlyAnMpnWithAStoreEntryStaysVisibleInThatStore() {
        PimEntry ownMpnOnly = entry("s1-9", "999", " M-1 ", "s1");
        PimEntryIndex idx = new PimEntryIndex(List.of(global, ownMpnOnly), TRIM, TRIM);

        assertThat(idx.forStore("s1").findAll()).containsExactly(ownMpnOnly, global);
    }

    @Test
    void ownEntrySharingOnlyAnMpnDoesNotTakeOverTheGlobalPimIdOfAKey() {
        // the same code under another product (another brand, another size) must not turn the key into the store's entry
        PimEntry ownMpnOnly = entry("s1-7", "777", "M-1", "s1");
        PimView s1 = new PimEntryIndex(List.of(global, ownMpnOnly), TRIM, TRIM).forStore("s1");

        assertThat(s1.findByPimIdOrGtins("g1", List.of("590"))).contains(global);
    }

    @Test
    void gtinStillBeatsMpnWithinOneScope() {
        PimEntry byGtin = entry("g5", "500", "M-50", null);
        PimEntry byMpn = entry("g6", "600", "M-60", null);
        PimEntryIndex idx = new PimEntryIndex(List.of(byGtin, byMpn), TRIM, TRIM);

        assertThat(idx.forStore("s1").findByGtinOrMpnAndBrand("500", "M-60", "b")).contains(byGtin);
        assertThat(idx.global().findByGtinOrMpnAndBrand("500", "M-60", "b")).contains(byGtin);
    }

    @Test
    void globalPimIdOfAKeyYieldsOnlyToTheOwnEntryShadowingThatEntry() {
        // two global groups; the store entry shares the first group's GTIN and, by bad feed data, another GTIN of the
        // second key: only the first group may resolve to the store entry, or one pimId would stand for two products
        PimEntry first = entry("g7", "701", "M-71", null);
        PimEntry second = entry("g8", "702", "M-72", null);
        PimEntry own = new PimEntry("s1-8", List.of(new PimIdentifier("701", PimIdentifierType.GTIN),
                new PimIdentifier("703", PimIdentifierType.GTIN)), "b", "n", "c", "s", true, null, null, null, "s1");
        PimView s1 = new PimEntryIndex(List.of(first, second, own), TRIM, TRIM).forStore("s1");

        assertThat(s1.findByPimIdOrGtins("g7", List.of("701"))).contains(own);
        assertThat(s1.findByPimIdOrGtins("g8", List.of("702", "703"))).contains(second);
        assertThat(s1.findByPimIdOrGtins(null, List.of("702", "703"))).contains(own);
    }

    @Test
    void compositeLookupPrefersOwnPimIdEvenWhenGtinsHitElsewhere() {
        assertThat(index.forStore("s1").findByPimIdOrGtins("s1-2", List.of("590"))).contains(onlyInS1);
    }

    @Test
    void compositeLookupPrefersOwnIdentifierOverGlobalPimId() {
        assertThat(index.forStore("s1").findByPimIdOrGtins("g1", List.of("590"))).contains(ownedByS1);
    }

    @Test
    void compositeLookupPrefersOwnHitOnLaterGtinOverGlobalHitOnEarlierOne() {
        PimEntry globalOnly = entry("g3", "300", "M-30", null);
        PimEntryIndex idx = new PimEntryIndex(List.of(globalOnly, global, onlyInS1), TRIM, TRIM);

        assertThat(idx.forStore("s1").findByPimIdOrGtins(null, List.of("300", "591"))).contains(onlyInS1);
    }

    @Test
    void compositeLookupFallsBackToGlobalPimIdThenGlobalGtins() {
        PimEntry globalOnly = entry("g3", "300", "M-30", null);
        PimEntryIndex idx = new PimEntryIndex(List.of(globalOnly, global, onlyInS1), TRIM, TRIM);
        PimView s1 = idx.forStore("s1");

        assertThat(s1.findByPimIdOrGtins("g1", List.of("300"))).contains(global);
        assertThat(s1.findByPimIdOrGtins("missing", List.of("300"))).contains(globalOnly);
        assertThat(s1.findByPimIdOrGtins("s2-1", List.of())).isEmpty();
    }

    @Test
    void compositeLookupToleratesNullAndBlankArguments() {
        PimView s1 = index.forStore("s1");
        assertThat(s1.findByPimIdOrGtins(null, null)).isEmpty();
        assertThat(s1.findByPimIdOrGtins(" ", Arrays.asList(null, " ", "591"))).contains(onlyInS1);
        assertThat(index.global().findByPimIdOrGtins(null, Arrays.asList(null, "590"))).contains(global);
    }

    @Test
    void globalViewKeepsPimIdFirstInCompositeLookup() {
        PimEntry other = entry("g4", "400", "M-40", null);
        PimEntryIndex idx = new PimEntryIndex(List.of(global, other), TRIM, TRIM);
        assertThat(idx.global().findByPimIdOrGtins("g4", List.of("590"))).contains(other);
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
        assertThat(idx.forStore("s1").findByMpnAndBrand("123", "b")).isEmpty();
        assertThat(idx.forStore("s1").findAll()).containsExactly(odd, global, noIds);
        assertThat(odd.gtins()).containsExactly("124");
        assertThat(noIds.gtins()).isEmpty();
        assertThat(noIds.mpns()).isEmpty();
    }

    private static PimEntry clone(String pimId, String gtin, String mpn, String owner, String source) {
        return new PimEntry(pimId, List.of(new PimIdentifier(gtin, PimIdentifierType.GTIN),
                new PimIdentifier(mpn, PimIdentifierType.MPN)), "b", "n", "c", "s", true, null, null, null, owner, source);
    }

    @Test
    void storeViewFindsTheCloneOfASource() {
        PimEntry clone = clone("c1", "590", "M-1", "s1", "g1");
        PimEntryIndex idx = new PimEntryIndex(List.of(global, clone), TRIM, TRIM);
        assertThat(idx.forStore("s1").findCloneOf("g1")).contains(clone);
        assertThat(idx.forStore("s1").findCloneOf("other")).isEmpty();
        assertThat(idx.forStore("s1").findCloneOf(" ")).isEmpty();
        assertThat(idx.forStore("s1").findCloneOf(null)).isEmpty();
    }

    @Test
    void globalViewFindsNoClone() {
        PimEntryIndex idx = new PimEntryIndex(List.of(global, clone("c1", "590", "M-1", "s1", "g1")), TRIM, TRIM);
        assertThat(idx.global().findCloneOf("g1")).isEmpty();
    }

    @Test
    void otherStoreViewFindsNoClone() {
        PimEntryIndex idx = new PimEntryIndex(List.of(global, clone("c1", "590", "M-1", "s1", "g1")), TRIM, TRIM);
        assertThat(idx.forStore("s2").findCloneOf("g1")).isEmpty();
        assertThat(idx.forStore("unknown").findCloneOf("g1")).isEmpty();
    }

    @Test
    void cloneWithChangedCodesIsStillFoundBySource() {
        PimEntry clone = clone("c1", "999", "M-99", "s1", "g1");
        PimEntryIndex idx = new PimEntryIndex(List.of(global, clone), TRIM, TRIM);
        assertThat(idx.forStore("s1").findCloneOf("g1")).contains(clone);
        assertThat(idx.forStore("s1").findByGtin("590")).contains(global);
    }

    @Test
    void storeViewShadowsTheSourceWithItsCloneByCode() {
        PimEntry clone = clone("c1", "590", "M-1", "s1", "g1");
        PimEntryIndex idx = new PimEntryIndex(List.of(global, clone), TRIM, TRIM);
        assertThat(idx.forStore("s1").findByGtin("590")).contains(clone);
        assertThat(idx.forStore("s1").findAll()).containsExactly(clone);
        assertThat(idx.forStore("s2").findByGtin("590")).contains(global);
    }

    @Test
    void storeViewListsOnlyItsOwnClones() {
        PimEntry clone = clone("c1", "590", "M-1", "s1", "g1");
        PimEntryIndex idx = new PimEntryIndex(List.of(global, clone, onlyInS1, ownedByS2), TRIM, TRIM);
        assertThat(idx.forStore("s1").findOwnClones()).containsExactly(clone);
        assertThat(idx.forStore("s2").findOwnClones()).isEmpty();
        assertThat(idx.forStore("unknown").findOwnClones()).isEmpty();
    }

    @Test
    void globalViewListsNoClones() {
        PimEntryIndex idx = new PimEntryIndex(List.of(global, clone("c1", "590", "M-1", "s1", "g1")), TRIM, TRIM);
        assertThat(idx.global().findOwnClones()).isEmpty();
    }
}
