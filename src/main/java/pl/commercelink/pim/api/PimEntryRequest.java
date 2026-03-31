package pl.commercelink.pim.api;

public record PimEntryRequest(String ean, String mfn, String brand, int priority) {
}
