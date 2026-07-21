package pl.commercelink.pim.api;

public record CategoryMatchRequest(String supplier, String ean, String mfn,
                                   String brand, String name, String rawCategory) {
}
