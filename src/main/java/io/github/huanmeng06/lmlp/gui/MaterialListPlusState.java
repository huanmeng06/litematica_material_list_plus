package io.github.huanmeng06.lmlp.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.material.MaterialCounts;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.MaterialTreeBuilder;
import io.github.huanmeng06.lmlp.recipe.MaterialTreeNode;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import io.github.huanmeng06.lmlp.recipe.RecipeResolvers;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;

public final class MaterialListPlusState {
    private static MaterialListEntry expandedEntry;
    private static String expandedKey = "";
    private static List<RecipeSummary> expandedSummaries = Collections.emptyList();
    private static final Set<String> expandedTreeNodes = new HashSet<>();
    private static final Map<String, Boolean> treeSupportCache = new HashMap<>();
    private static final Map<String, MaterialTreeNode> treeCache = new HashMap<>();

    private MaterialListPlusState() {
    }

    public static boolean isExpanded(MaterialListEntry entry) {
        return isRecipeExpanded(entry);
    }

    public static boolean isRecipeExpanded(MaterialListEntry entry) {
        return expandedEntry != null && expandedEntry.equals(entry);
    }

    public static void toggle(MaterialListEntry entry, MaterialListBase materialList) {
        if (isRecipeExpanded(entry)) {
            clearRecipe();
        } else {
            open(entry, materialList);
        }
    }

    public static void open(MaterialListEntry entry, MaterialListBase materialList) {
        clearIngredientTrees();
        expandedEntry = entry;
        expandedKey = key(entry, materialList);
        expandedSummaries = RecipeResolvers.findRecipes(entry.getStack(), MaterialCounts.total(entry, materialList), MaterialCounts.missing(entry, materialList));
    }

    public static void clear() {
        clearRecipe();
        clearIngredientTrees();
    }

    public static boolean hasTree(IngredientSummary ingredient) {
        String key = key(ingredient);
        if (Configs.shouldStopRecipeDecomposition(ItemStackTexts.id(ingredient.icon()))) {
            treeSupportCache.put(key, false);
            treeCache.remove(key);
            collapseTreeNode("ingredient:" + key);
            return false;
        }

        Boolean cached = treeSupportCache.get(key);
        if (cached != null) {
            return cached;
        }

        boolean supported = MaterialTreeBuilder.hasChildren(ingredient.icon(), ingredient.countTotal(), ingredient.countMissing());
        treeSupportCache.put(key, supported);
        return supported;
    }

    public static void toggleIngredientTree(IngredientSummary ingredient) {
        MaterialTreeNode root = treeFor(ingredient);
        if (!root.hasChildren()) {
            treeSupportCache.put(key(ingredient), false);
            return;
        }

        treeSupportCache.put(key(ingredient), true);
        if (expandedTreeNodes.contains(root.path())) {
            collapseTreeNode(root.path());
        } else {
            expandedTreeNodes.add(root.path());
        }
    }

    public static MaterialTreeNode getExpandedIngredientTree(IngredientSummary ingredient) {
        MaterialTreeNode root = treeCache.get(key(ingredient));
        if (root == null || !expandedTreeNodes.contains(root.path())) {
            return null;
        }

        return root;
    }

    public static Set<String> getExpandedTreeNodes() {
        return Collections.unmodifiableSet(expandedTreeNodes);
    }

    public static void toggleTreeNode(String path) {
        if (expandedTreeNodes.contains(path)) {
            collapseTreeNode(path);
        } else {
            expandedTreeNodes.add(path);
        }
    }

    private static void clearRecipe() {
        expandedEntry = null;
        expandedKey = "";
        expandedSummaries = Collections.emptyList();
    }

    private static void clearIngredientTrees() {
        expandedTreeNodes.clear();
    }

    public static void clearRecipeCaches() {
        clearIngredientTrees();
        treeSupportCache.clear();
        treeCache.clear();
    }

    private static MaterialTreeNode treeFor(IngredientSummary ingredient) {
        String key = key(ingredient);
        if (Configs.shouldStopRecipeDecomposition(ItemStackTexts.id(ingredient.icon()))) {
            treeSupportCache.put(key, false);
            treeCache.remove(key);
        }

        return treeCache.computeIfAbsent(key, ignored -> MaterialTreeBuilder.build(
                ingredient.icon(),
                RecipeSummaryFormatter.ingredientName(ingredient),
                ingredient.countTotal(),
                ingredient.countMissing(),
                "ingredient:" + key));
    }

    private static void collapseTreeNode(String path) {
        expandedTreeNodes.removeIf(expandedPath -> expandedPath.equals(path) || expandedPath.startsWith(path + "/"));
    }

    public static List<RecipeSummary> getSummaries(MaterialListEntry entry, MaterialListBase materialList) {
        if (!isRecipeExpanded(entry)) {
            return Collections.emptyList();
        }

        String key = key(entry, materialList);
        if (!key.equals(expandedKey)) {
            open(entry, materialList);
        }

        return expandedSummaries;
    }

    public static List<RecipeSummary> getCachedSummaries(MaterialListEntry entry) {
        if (!isRecipeExpanded(entry)) {
            return Collections.emptyList();
        }

        return expandedSummaries;
    }

    public static List<RecipeSummary> resolveFor(MaterialListEntry entry, MaterialListBase materialList) {
        return RecipeResolvers.findRecipes(entry.getStack(), MaterialCounts.total(entry, materialList), MaterialCounts.missing(entry, materialList));
    }

    private static String key(MaterialListEntry entry, MaterialListBase materialList) {
        return key(entry.getStack(), MaterialCounts.total(entry, materialList), MaterialCounts.missing(entry, materialList));
    }

    private static String key(IngredientSummary ingredient) {
        return key(ingredient.icon(), ingredient.countTotal(), ingredient.countMissing());
    }

    private static String key(net.minecraft.class_1799 stack, int totalCount, int missingCount) {
        return ItemStackTexts.id(stack) + "|" + totalCount + "|" + missingCount;
    }
}
