package pl.commercelink.pim.api;

public record BrandMapping(
        String alias,
        String canonicalName,
        int strength
) {
}
