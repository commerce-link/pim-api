package pl.commercelink.pim.api;

import java.util.List;

/**
 * @param clonedFromPimId set only on the announcement of a NEW store clone of a global entry: the app then moves the
 *                        owner store's products from that entry to this one. Null on every other event, including
 *                        code-change events of a clone.
 */
public record PIMEntryAddedEvent(String pimId, List<String> eans, List<String> mfnCodes, String ownerStoreId,
                                 String clonedFromPimId) {

    public PIMEntryAddedEvent(String pimId, List<String> eans, List<String> mfnCodes, String ownerStoreId) {
        this(pimId, eans, mfnCodes, ownerStoreId, null);
    }

    public PIMEntryAddedEvent(String pimId, List<String> eans, List<String> mfnCodes) {
        this(pimId, eans, mfnCodes, null, null);
    }
}
