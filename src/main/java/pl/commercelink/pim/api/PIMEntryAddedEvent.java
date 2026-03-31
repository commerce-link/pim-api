package pl.commercelink.pim.api;

import java.util.List;

public record PIMEntryAddedEvent(String pimId, List<String> eans, List<String> mfnCodes) {
}
