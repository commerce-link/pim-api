package pl.commercelink.pim.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PimCategories {

    private final List<PimCategory> categories;
    private final Map<String, List<PimCategory>> childrenByParentId = new HashMap<>();

    public PimCategories(List<PimCategory> categories) {
        this.categories = List.copyOf(categories);
        for (PimCategory category : this.categories) {
            if (!category.topLevel()) {
                childrenByParentId.computeIfAbsent(category.parentId(), k -> new ArrayList<>()).add(category);
            }
        }
    }

    public List<PimCategory> all() {
        return categories;
    }

    public List<PimCategory> topLevels() {
        return categories.stream().filter(PimCategory::topLevel).toList();
    }

    public List<PimCategory> childrenOf(String id) {
        return List.copyOf(childrenByParentId.getOrDefault(id, List.of()));
    }

    public List<PimCategory> leavesUnder(String id) {
        PimCategory root = categories.stream().filter(c -> c.id().equals(id)).findFirst().orElse(null);
        if (root == null) {
            return List.of();
        }
        List<PimCategory> leaves = new ArrayList<>();
        collectLeaves(root, leaves);
        return leaves;
    }

    private void collectLeaves(PimCategory node, List<PimCategory> leaves) {
        List<PimCategory> children = childrenByParentId.getOrDefault(node.id(), List.of());
        if (children.isEmpty()) {
            leaves.add(node);
            return;
        }
        for (PimCategory child : children) {
            collectLeaves(child, leaves);
        }
    }
}
