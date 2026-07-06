package io.github.huanmeng06.lmlp.recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    // Decompose a choice-group ("任意X") ingredient one level: instead of
    // resolving only the representative item (which would show "橡木原木"),
    // resolve the representative recipe for the leading alternative
    // (oak_planks -> oak_log) and, for each child slot, union the equivalent
    // child across every parent alternative's recipe so the child reads as a
    // choice group of its own ("任意原木"). Quantities follow the
    // representative recipe's yield, matching how the single-item path counts.
    public static MaterialTreeNode buildChoiceGroup(IngredientSummary ingredient, String path) {
        class_1799 icon = ingredient.icon().method_7972();
        List<class_1799> icons = ingredient.icons().isEmpty() ? List.of(icon) : ingredient.icons();
        List<MaterialTreeNode> children = List.of();

        if (!Configs.shouldStopRecipeDecomposition(ItemStackTexts.id(icon))) {
            List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, ingredient.countTotal(), ingredient.countMissing());
            if (!summaries.isEmpty()) {
                children = buildChoiceGroupChildren(ingredient, summaries.get(0), path);
            }
        }

        return new MaterialTreeNode(
                path,
                icon,
                icons,
                ingredient.alternatives(),
                RecipeSummaryFormatter.ingredientName(ingredient),
                ingredient.countTotal(),
                ingredient.countMissing(),
                Math.max(1, icon.method_7914()),
                0,
                children);
    }

    private static List<MaterialTreeNode> buildChoiceGroupChildren(IngredientSummary parent, RecipeSummary representative, String parentPath) {
        // Resolve each parent alternative's recipe once so slot i can be
        // unioned across all alternatives (oak_log + spruce_log + ... ->
        // "任意原木") rather than only the representative's slot.
        List<List<IngredientSummary>> perAlternative = new ArrayList<>();
        for (class_1799 alternativeIcon : parent.icons()) {
            if (alternativeIcon.method_7960()) {
                perAlternative.add(List.of());
                continue;
            }
            List<RecipeSummary> summaries = RecipeResolvers.findRecipes(alternativeIcon, parent.countTotal(), parent.countMissing());
            perAlternative.add(summaries.isEmpty() ? List.of() : summaries.get(0).ingredients());
        }

        List<MaterialTreeNode> children = new ArrayList<>();
        List<IngredientSummary> repIngredients = representative.ingredients();
        for (int index = 0; index < repIngredients.size(); index++) {
            IngredientSummary repChild = repIngredients.get(index);
            if (repChild.countTotal() <= 0 && repChild.countMissing() <= 0) {
                continue;
            }

            Map<String, class_1799> unionIcons = new LinkedHashMap<>();
            List<String> unionNames = new ArrayList<>();
            addChildToUnion(repChild, unionIcons, unionNames);
            for (List<IngredientSummary> altIngredients : perAlternative) {
                if (index < altIngredients.size()) {
                    addChildToUnion(altIngredients.get(index), unionIcons, unionNames);
                }
            }

            List<class_1799> icons = new ArrayList<>(unionIcons.values());
            class_1799 icon = icons.isEmpty() ? repChild.icon().method_7972() : icons.get(0);
            boolean choiceGroup = icons.size() > 1 || unionNames.size() > 1;
            String name = choiceGroup ? AlternativeItemDisplay.name(icons, unionNames) : "";
            if (name.isEmpty()) {
                name = RecipeSummaryFormatter.ingredientName(repChild);
            }

            String childPath = parentPath + "/" + index + ":" + ItemStackTexts.id(icon);
            children.add(new MaterialTreeNode(
                    childPath,
                    icon,
                    icons.isEmpty() ? List.of(icon) : icons,
                    choiceGroup ? unionNames : List.of(),
                    name,
                    repChild.countTotal(),
                    repChild.countMissing(),
                    Math.max(1, icon.method_7914()),
                    1,
                    List.of()));
        }

        return children;
    }

    private static void addChildToUnion(IngredientSummary child, Map<String, class_1799> icons, List<String> names) {
        List<class_1799> childIcons = child.icons().isEmpty() ? List.of(child.icon()) : child.icons();
        for (class_1799 stack : childIcons) {
            if (stack.method_7960()) {
                continue;
            }
            icons.putIfAbsent(ItemStackTexts.id(stack), stack.method_7972());
        }

        List<String> childNames = child.alternatives();
        if (childNames.isEmpty()) {
            String name = ItemStackTexts.name(child.icon());
            if (!name.isEmpty() && !names.contains(name)) {
                names.add(name);
            }
            return;
        }

        for (String name : childNames) {
            if (!names.contains(name)) {
                names.add(name);
            }
        }
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
            if (ingredient.isChoiceGroup()) {
                children.add(new MaterialTreeNode(
                        path,
                        icon,
                        ingredient.icons().isEmpty() ? List.of(icon) : ingredient.icons(),
                        ingredient.alternatives(),
                        RecipeSummaryFormatter.ingredientName(ingredient),
                        ingredient.countTotal(),
                        ingredient.countMissing(),
                        Math.max(1, icon.method_7914()),
                        depth,
                        List.of()));
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
