package pl.commercelink.pim.api;

public record CategoryMatchedEvent(String ean, String mfn, String category,
                                   String icecatCategoryId, String icecatCategoryName,
                                   Double confidence, String source) {
}
