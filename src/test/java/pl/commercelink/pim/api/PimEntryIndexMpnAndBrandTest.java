package pl.commercelink.pim.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

class PimEntryIndexMpnAndBrandTest {

    private static final UnaryOperator<String> TRIM = v -> v == null ? null : v.trim();
    private static final UnaryOperator<String> WITHOUT_SPACES = v -> v == null ? null : v.replace(" ", "");

    private static final String SHORT_CODE = "23647";
    private static final String LONG_CODE = "SDSSDA-1T00";

    @Test
    void entryOfTheGivenBrandIsFoundUnderACodeSharedByTwoBrands() {
        // given
        PimEntry trust = entry("g1", "5900000000001", SHORT_CODE, "Trust", null);
        PimEntry genesis = entry("g2", "5900000000002", SHORT_CODE, "Genesis", null);
        PimView global = index(trust, genesis).global();

        // when / then
        assertThat(global.findByMpnAndBrand(SHORT_CODE, "Trust")).contains(trust);
        assertThat(global.findByMpnAndBrand(SHORT_CODE, "Genesis")).contains(genesis);
    }

    @Test
    void entryOfAnotherBrandIsNeverFound() {
        // given
        PimView global = index(
                entry("g1", "5900000000001", SHORT_CODE, "Trust", null),
                entry("g2", "5900000000002", LONG_CODE, "Western Digital", null)).global();

        // when / then
        assertThat(global.findByMpnAndBrand(SHORT_CODE, "Genesis")).isEmpty();
        assertThat(global.findByMpnAndBrand(LONG_CODE, "Genesis")).isEmpty();
    }

    @Test
    void brandIsComparedIgnoringCaseAndSurroundingSpaces() {
        // given
        PimEntry trust = entry("g1", "5900000000001", SHORT_CODE, "Trust", null);

        // when / then
        assertThat(index(trust).global().findByMpnAndBrand(SHORT_CODE, " TRUST ")).contains(trust);
    }

    @Test
    void missingBrandFindsAnEntryOnlyUnderALongCode() {
        // given
        PimEntry shortCoded = entry("g1", "5900000000001", SHORT_CODE, "Trust", null);
        PimEntry longCoded = entry("g2", "5900000000002", LONG_CODE, "Western Digital", null);
        PimView global = index(shortCoded, longCoded).global();

        // when / then
        assertThat(global.findByMpnAndBrand(LONG_CODE, null)).contains(longCoded);
        assertThat(global.findByMpnAndBrand(SHORT_CODE, null)).isEmpty();
        assertThat(global.findByMpnAndBrand(SHORT_CODE, " ")).isEmpty();
    }

    @Test
    void entryWithoutBrandIsFoundOnlyUnderALongCode() {
        // given
        PimEntry shortCoded = entry("g1", "5900000000001", SHORT_CODE, null, null);
        PimEntry longCoded = entry("g2", "5900000000002", LONG_CODE, " ", null);
        PimView global = index(shortCoded, longCoded).global();

        // when / then
        assertThat(global.findByMpnAndBrand(LONG_CODE, "Western Digital")).contains(longCoded);
        assertThat(global.findByMpnAndBrand(SHORT_CODE, "Trust")).isEmpty();
    }

    @Test
    void entryOfTheGivenBrandBeatsALaterEntryWithoutBrand() {
        // given
        PimEntry westernDigital = entry("g1", "5900000000001", LONG_CODE, "Western Digital", null);
        PimEntry withoutBrand = entry("g2", "5900000000002", LONG_CODE, null, null);

        // when / then
        assertThat(index(westernDigital, withoutBrand).global().findByMpnAndBrand(LONG_CODE, "Western Digital"))
                .contains(westernDigital);
    }

    @Test
    void laterEntryWinsAmongEntriesOfTheBrand() {
        // given
        PimEntry first = entry("g1", "5900000000001", SHORT_CODE, "Trust", null);
        PimEntry second = entry("g2", "5900000000002", SHORT_CODE, "Trust", null);
        PimEntry genesis = entry("g3", "5900000000003", SHORT_CODE, "Genesis", null);

        // when / then
        assertThat(index(first, second, genesis).global().findByMpnAndBrand(SHORT_CODE, "Trust")).contains(second);
    }

    @Test
    void codeLengthIsCountedOnTheNormalizedCode() {
        // given
        PimEntry entry = entry("g1", "5900000000001", "12 345 6", "Trust", null);
        PimView global = new PimEntryIndex(List.of(entry), TRIM, WITHOUT_SPACES).global();

        // when / then
        assertThat(global.findByMpnAndBrand(" 12 345 6 ", null)).isEmpty();
        assertThat(global.findByMpnAndBrand(" 12 345 6 ", "Trust")).contains(entry);
    }

    @Test
    void blankCodeFindsNothing() {
        // given
        PimView global = index(entry("g1", "5900000000001", LONG_CODE, null, null)).global();

        // when / then
        assertThat(global.findByMpnAndBrand(null, null)).isEmpty();
        assertThat(global.findByMpnAndBrand(" ", "Trust")).isEmpty();
    }

    @Test
    void storeLookupTakesItsOwnEntryOfTheBrandFirst() {
        // given
        PimEntry global = entry("g1", "5900000000001", SHORT_CODE, "Trust", null);
        PimEntry own = entry("s1-1", "5900000000009", SHORT_CODE, "Trust", "s1");

        // when / then
        assertThat(index(global, own).forStore("s1").findByMpnAndBrand(SHORT_CODE, "Trust")).contains(own);
    }

    @Test
    void ownEntryOfAnotherBrandDoesNotHideTheGlobalEntryOfTheBrand() {
        // given
        PimEntry global = entry("g1", "5900000000001", SHORT_CODE, "Trust", null);
        PimEntry own = entry("s1-1", "5900000000009", SHORT_CODE, "Genesis", "s1");

        // when / then
        assertThat(index(global, own).forStore("s1").findByMpnAndBrand(SHORT_CODE, "Trust")).contains(global);
    }

    @Test
    void lookupsWithoutBrandStillGiveTheLaterEntryUnderASharedCode() {
        // given
        PimEntry trust = entry("g1", "5900000000001", SHORT_CODE, "Trust", null);
        PimEntry genesis = entry("g2", "5900000000002", SHORT_CODE, "Genesis", null);
        PimView global = index(trust, genesis).global();

        // when / then
        assertThat(global.findByMpn(SHORT_CODE)).contains(genesis);
        assertThat(global.findByGtinOrMpn(null, SHORT_CODE)).contains(genesis);
    }

    @Test
    void gtinHitWinsRegardlessOfBrand() {
        // given
        PimEntry trust = entry("g1", "5900000000001", SHORT_CODE, "Trust", null);

        // when / then
        assertThat(index(trust).global().findByGtinOrMpnAndBrand("5900000000001", SHORT_CODE, "Genesis"))
                .contains(trust);
    }

    @Test
    void codeHitWithoutGtinHitNeedsTheBrand() {
        // given
        PimEntry trust = entry("g1", "5900000000001", SHORT_CODE, "Trust", null);
        PimView global = index(trust).global();

        // when / then
        assertThat(global.findByGtinOrMpnAndBrand("5900000000009", SHORT_CODE, "Trust")).contains(trust);
        assertThat(global.findByGtinOrMpnAndBrand("5900000000009", SHORT_CODE, "Genesis")).isEmpty();
        assertThat(global.findByGtinOrMpnAndBrand(null, SHORT_CODE, null)).isEmpty();
    }

    @Test
    void gtinBeatsAnEntryOfTheBrandUnderTheCodeWithinOneScope() {
        // given
        PimEntry byGtin = entry("g1", "5900000000001", "OTHER-CODE", "Genesis", null);
        PimEntry byCode = entry("g2", "5900000000002", SHORT_CODE, "Trust", null);

        // when / then
        assertThat(index(byGtin, byCode).global().findByGtinOrMpnAndBrand("5900000000001", SHORT_CODE, "Trust"))
                .contains(byGtin);
    }

    @Test
    void ownEntryByEitherCodeBeatsGlobalEntries() {
        // given
        PimEntry global = entry("g1", "5900000000001", "OTHER-CODE", "Genesis", null);
        PimEntry own = entry("s1-1", "5900000000009", SHORT_CODE, "Trust", "s1");
        PimEntryIndex index = index(global, own);

        // when / then
        assertThat(index.forStore("s1").findByGtinOrMpnAndBrand("5900000000001", SHORT_CODE, "Trust")).contains(own);
        assertThat(index.forStore("s2").findByGtinOrMpnAndBrand("5900000000001", SHORT_CODE, "Trust")).contains(global);
    }

    @Test
    void blankArgumentsFindNothingByGtinOrCode() {
        // given
        PimView global = index(entry("g1", "5900000000001", LONG_CODE, "Trust", null)).global();

        // when / then
        assertThat(global.findByGtinOrMpnAndBrand(null, null, null)).isEmpty();
        assertThat(global.findByGtinOrMpnAndBrand(" ", " ", "Trust")).isEmpty();
    }

    private static PimEntryIndex index(PimEntry... entries) {
        return new PimEntryIndex(List.of(entries), TRIM, TRIM);
    }

    private static PimEntry entry(String pimId, String gtin, String mpn, String brand, String owner) {
        return new PimEntry(pimId, List.of(new PimIdentifier(gtin, PimIdentifierType.GTIN),
                new PimIdentifier(mpn, PimIdentifierType.MPN)), brand, "n", "c", "s", true, null, null, null, owner);
    }
}
