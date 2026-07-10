package pl.commercelink.pim.api;

public record PimCategory(String id, String parentId, String namePl, String nameEn) {

    public boolean topLevel() {
        return parentId == null;
    }
}
