package pl.commercelink.pim.api;

public record PimIdentifier(String value, PimIdentifierType type, boolean primary) {

    public PimIdentifier(String value, PimIdentifierType type) {
        this(value, type, false);
    }
}
