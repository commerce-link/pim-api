package pl.commercelink.pim.api;

public record PimCategory(String id, String parentId, String name, String lang) {

    public boolean topLevel() {
        return parentId == null;
    }
}
