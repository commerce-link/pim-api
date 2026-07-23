package pl.commercelink.pim.api;

public record CategoryMatchedEvent(String ean, String mfn, String category,
                                   String categoryId, Double confidence, String source) {
}
