package pl.commercelink.pim.api;

import java.util.List;

/**
 * @param brand           the entry's brand as PIM stores it; null when the entry has none or the event was sent by a
 *                        PIM older than this field.
 * @param clonedFromPimId set only on the announcement of a NEW store clone of a global entry: the app then moves the
 *                        owner store's products from that entry to this one. Null on every other event, including
 *                        code-change events of a clone.
 */
public record PIMEntryAddedEvent(String pimId, String brand, List<String> eans, List<String> mfnCodes, String ownerStoreId, String clonedFromPimId) {
}
