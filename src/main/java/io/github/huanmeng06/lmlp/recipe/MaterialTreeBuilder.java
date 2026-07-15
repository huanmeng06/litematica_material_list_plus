package io.github.huanmeng06.lmlp.recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;

public final class MaterialTreeBuilder {
    private static final int MAX_DEPTH = 3;

    private MaterialTreeBuilder() {
    }

    public static boolean hasChildren(ItemStack stack, int totalCount, int missingCount) {
        if (Configs.shouldStopRecipeDecomposition(ItemStackTexts.id(stack))) {
            return false;
        }

        List<RecipeSummary> summaries = RecipeResolvers.findRecipes(stack, totalCount, missingCount);
        return !summaries.isEmpty()
                && !summaries.get(0).ingredients().isEmpty()
                && !RecipeResolvers.leadsBackTo(ItemStackTexts.id(stack), summaries.get(0), MAX_DEPTH);
    }

    public static MaterialTreeNode build(ItemStack stack, int totalCount, int missingCount) {
        return build(stack, ItemStackTexts.name(stack), totalCount, missingCount, "root", 0, new HashSet<>());
    }

    // Decompose a choice-group ("任意X") ingredient: instead of resolving only
    // the representative item (which would show "橡木原木"), resolve the
    // representative recipe (oak_planks -> oak_log) and, for each child slot,
    // union the equivalent child across every parent alternative's recipe so
    // the child reads as a choice group of its own ("任意原木"). Choice-group
    // children recurse the same way (任意台阶 -> 任意木板 -> 任意原木), and
    // single-item children fall back to the normal decomposition. Quantities
    // follow the representative recipe's yield, matching the single-item path.
    public static MaterialTreeNode buildChoiceGroup(IngredientSummary ingredient, String path) {
        List<ItemStack> icons = ingredient.icons().isEmpty() ? List.of(ingredient.icon()) : ingredient.icons();
        return buildChoiceGroupNode(
                ingredient.icon().copy(),
                icons,
                ingredient.alternatives(),
                RecipeSummaryFormatter.ingredientName(ingredient),
                ingredient.countTotal(),
                ingredient.countMissing(),
                path,
                0,
                new HashSet<>());
    }

    private static MaterialTreeNode buildChoiceGroupNode(ItemStack icon, List<ItemStack> icons, List<String> alternatives, String name, int totalCount, int missingCount, String path, int depth, Set<String> seenItems) {
        String itemId = ItemStackTexts.id(icon);
        List<MaterialTreeNode> children = List.of();

        if (depth < MAX_DEPTH && !seenItems.contains(itemId) && !Configs.shouldStopRecipeDecomposition(itemId)) {
            Set<String> childSeenItems = new HashSet<>(seenItems);
            childSeenItems.add(itemId);

            List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, totalCount, missingCount);
            if (!summaries.isEmpty()
                    && !RecipeResolvers.leadsBackTo(itemId, summaries.get(0), MAX_DEPTH - depth)) {
                children = buildChoiceGroupChildren(icons, summaries.get(0), totalCount, missingCount, path, depth + 1, childSeenItems);
            }
        }

        return new MaterialTreeNode(
                path,
                icon,
                icons,
                alternatives,
                name,
                totalCount,
                missingCount,
                Math.max(1, icon.getMaxStackSize()),
                depth,
                children);
    }

    private static List<MaterialTreeNode> buildChoiceGroupChildren(List<ItemStack> parentIcons, RecipeSummary representative, int totalCount, int missingCount, String parentPath, int depth, Set<String> seenItems) {
        // Resolve each parent alternative's recipe once so slot i can be
        // unioned across all alternatives (oak_log + spruce_log + ... ->
        // "任意原木") rather than only the representative's slot. All
        // alternatives share the same yield ratio, so the same total/missing
        // count is passed and the representative's per-slot counts are used.
        List<List<IngredientSummary>> perAlternative = new ArrayList<>();
        for (ItemStack alternativeIcon : parentIcons) {
            if (alternativeIcon.isEmpty()) {
                perAlternative.add(List.of());
                continue;
            }
            List<RecipeSummary> summaries = RecipeResolvers.findRecipes(alternativeIcon, totalCount, missingCount);
            perAlternative.add(summaries.isEmpty() ? List.of() : summaries.get(0).ingredients());
        }

        List<MaterialTreeNode> children = new ArrayList<>();
        List<IngredientSummary> repIngredients = representative.ingredients();
        for (int index = 0; index < repIngredients.size(); index++) {
            IngredientSummary repChild = repIngredients.get(index);
            if (repChild.countTotal() <= 0 && repChild.countMissing() <= 0) {
                continue;
            }

            Map<String, ItemStack> unionIcons = new LinkedHashMap<>();
            List<String> unionNames = new ArrayList<>();
            IngredientUnion.addIngredient(repChild, unionIcons, unionNames);
            for (List<IngredientSummary> altIngredients : perAlternative) {
                if (index < altIngredients.size()) {
                    IngredientUnion.addIngredient(altIngredients.get(index), unionIcons, unionNames);
                }
            }

            List<ItemStack> icons = new ArrayList<>(unionIcons.values());
            if (icons.isEmpty()) {
                icons = List.of(repChild.icon().copy());
            }
            ItemStack icon = icons.get(0);
            boolean choiceGroup = icons.size() > 1 || unionNames.size() > 1;
            String name = choiceGroup ? AlternativeItemDisplay.name(icons, unionNames) : "";
            if (name.isEmpty()) {
                name = RecipeSummaryFormatter.ingredientName(repChild);
            }

            String childPath = parentPath + "/" + index + ":" + ItemStackTexts.id(icon);
            if (choiceGroup) {
                children.add(buildChoiceGroupNode(
                        icon,
                        icons,
                        unionNames,
                        name,
                        repChild.countTotal(),
                        repChild.countMissing(),
                        childPath,
                        depth,
                        seenItems));
            } else {
                children.add(build(
                        icon,
                        icons,
                        name,
                        repChild.countTotal(),
                        repChild.countMissing(),
                        childPath,
                        depth,
                        seenItems));
            }
        }

        return children;
    }

    public static MaterialTreeNode build(ItemStack stack, String name, int totalCount, int missingCount) {
        return build(stack, name, totalCount, missingCount, "root", 0, new HashSet<>());
    }

    public static MaterialTreeNode build(ItemStack stack, String name, int totalCount, int missingCount, String path) {
        return build(stack, List.of(stack), name, totalCount, missingCount, path, 0, new HashSet<>());
    }

    private static MaterialTreeNode build(ItemStack stack, String name, int totalCount, int missingCount, String path, int depth, Set<String> seenItems) {
        return build(stack, List.of(stack), name, totalCount, missingCount, path, depth, seenItems);
    }

    private static MaterialTreeNode build(ItemStack stack, List<ItemStack> icons, String name, int totalCount, int missingCount, String path, int depth, Set<String> seenItems) {
        ItemStack icon = stack.copy();
        String itemId = ItemStackTexts.id(icon);
        List<MaterialTreeNode> children = List.of();

        if (depth < MAX_DEPTH && !seenItems.contains(itemId) && !Configs.shouldStopRecipeDecomposition(itemId)) {
            Set<String> childSeenItems = new HashSet<>(seenItems);
            childSeenItems.add(itemId);

            List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, totalCount, missingCount);
            if (!summaries.isEmpty()
                    && !RecipeResolvers.leadsBackTo(itemId, summaries.get(0), MAX_DEPTH - depth)) {
                children = buildChildren(summaries.get(0), path, depth + 1, childSeenItems);
            }
        }

        return new MaterialTreeNode(path, icon, icons, name, totalCount, missingCount, Math.max(1, icon.getMaxStackSize()), depth, children);
    }

    private static List<MaterialTreeNode> buildChildren(RecipeSummary summary, String parentPath, int depth, Set<String> seenItems) {
        List<MaterialTreeNode> children = new ArrayList<>();
        int index = 0;
        for (IngredientSummary ingredient : summary.ingredients()) {
            if (ingredient.countTotal() <= 0 && ingredient.countMissing() <= 0) {
                continue;
            }

            ItemStack icon = ingredient.icon().copy();
            String path = parentPath + "/" + index + ":" + ItemStackTexts.id(icon);
            if (ingredient.isChoiceGroup()) {
                // A choice group reached through a plain item's recipe (e.g.
                // chest -> "任意木板") must keep decomposing as a choice group
                // (任意木板 -> 任意原木), not dead-end as a leaf. Delegate to the
                // same recursive builder the top-level choice-group path uses.
                children.add(buildChoiceGroupNode(
                        icon,
                        ingredient.icons().isEmpty() ? List.of(icon) : ingredient.icons(),
                        ingredient.alternatives(),
                        RecipeSummaryFormatter.ingredientName(ingredient),
                        ingredient.countTotal(),
                        ingredient.countMissing(),
                        path,
                        depth,
                        seenItems));
            } else {
                children.add(build(
                        icon,
                        ingredient.icons(),
                        RecipeSummaryFormatter.ingredientName(ingredient),
                        ingredient.countTotal(),
                        ingredient.countMissing(),
                        path,
                        depth,
                        seenItems));
            }
            index++;
        }

        return children;
    }
}
