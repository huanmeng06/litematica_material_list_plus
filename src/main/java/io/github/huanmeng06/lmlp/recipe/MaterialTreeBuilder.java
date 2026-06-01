package io.github.huanmeng06.lmlp.recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import net.minecraft.class_1799;

public final class MaterialTreeBuilder {
    private static final int MAX_DEPTH = 3;

    private MaterialTreeBuilder() {
    }

    public static boolean hasChildren(class_1799 stack, int totalCount, int missingCount) {
        if (Configs.shouldStopRecipeDecomposition(ItemStackTexts.id(stack))) {
            return false;
        }

        List<RecipeSummary> summaries = RecipeResolvers.findRecipes(stack, totalCount, missingCount);
        return !summaries.isEmpty() && !summaries.get(0).ingredients().isEmpty();
    }

    public static MaterialTreeNode build(class_1799 stack, int totalCount, int missingCount) {
        return build(stack, ItemStackTexts.name(stack), totalCount, missingCount, "root", 0, new HashSet<>());
    }

    public static MaterialTreeNode build(class_1799 stack, String name, int totalCount, int missingCount) {
        return build(stack, name, totalCount, missingCount, "root", 0, new HashSet<>());
    }

    public static MaterialTreeNode build(class_1799 stack, String name, int totalCount, int missingCount, String path) {
        return build(stack, List.of(stack), name, totalCount, missingCount, path, 0, new HashSet<>());
    }

    private static MaterialTreeNode build(class_1799 stack, String name, int totalCount, int missingCount, String path, int depth, Set<String> seenItems) {
        return build(stack, List.of(stack), name, totalCount, missingCount, path, depth, seenItems);
    }

    private static MaterialTreeNode build(class_1799 stack, List<class_1799> icons, String name, int totalCount, int missingCount, String path, int depth, Set<String> seenItems) {
        class_1799 icon = stack.method_7972();
        String itemId = ItemStackTexts.id(icon);
        List<MaterialTreeNode> children = List.of();

        if (depth < MAX_DEPTH && !seenItems.contains(itemId) && !Configs.shouldStopRecipeDecomposition(itemId)) {
            Set<String> childSeenItems = new HashSet<>(seenItems);
            childSeenItems.add(itemId);

            List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, totalCount, missingCount);
            if (!summaries.isEmpty()) {
                children = buildChildren(summaries.get(0), path, depth + 1, childSeenItems);
            }
        }

        return new MaterialTreeNode(path, icon, icons, name, totalCount, missingCount, Math.max(1, icon.method_7914()), depth, children);
    }

    private static List<MaterialTreeNode> buildChildren(RecipeSummary summary, String parentPath, int depth, Set<String> seenItems) {
        List<MaterialTreeNode> children = new ArrayList<>();
        int index = 0;
        for (IngredientSummary ingredient : summary.ingredients()) {
            if (ingredient.countTotal() <= 0 && ingredient.countMissing() <= 0) {
                continue;
            }

            class_1799 icon = ingredient.icon().method_7972();
            String path = parentPath + "/" + index + ":" + ItemStackTexts.id(icon);
            children.add(build(
                    icon,
                    ingredient.icons(),
                    RecipeSummaryFormatter.ingredientName(ingredient),
                    ingredient.countTotal(),
                    ingredient.countMissing(),
                    path,
                    depth,
                    seenItems));
            index++;
        }

        return children;
    }
}
