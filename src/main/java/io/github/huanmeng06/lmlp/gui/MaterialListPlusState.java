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
    private static MaterialListEntry visibleRecipeEntry;
    private static String visibleRecipeKey = "";
    private static List<RecipeSummary> visibleRecipeSummaries = Collections.emptyList();
    private static final Set<String> expandedTreeNodes = new HashSet<>();
    private static final Map<String, Boolean> treeSupportCache = new HashMap<>();
    private static final Map<String, MaterialTreeNode> treeCache = new HashMap<>();
    private static final ExpandAnimationTracker recipeAnimations = new ExpandAnimationTracker();
    private static final ExpandAnimationTracker treeAnimations = new ExpandAnimationTracker();

    private MaterialListPlusState() {
    }

    public static boolean isExpanded(MaterialListEntry entry) {
        return isRecipeExpanded(entry);
    }

    public static boolean isRecipeExpanded(MaterialListEntry entry) {
        return expandedEntry != null && expandedEntry.equals(entry);
    }

    public static boolean isRecipeVisible(MaterialListEntry entry) {
        return visibleRecipeEntry != null && visibleRecipeEntry.equals(entry);
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
        String key = key(entry, materialList);
        float startProgress = visibleRecipeEntry != null && visibleRecipeEntry.equals(entry) ? recipeProgress(entry) : 0.0F;
        List<RecipeSummary> summaries = RecipeResolvers.findRecipes(entry.getStack(), MaterialCounts.total(entry, materialList), MaterialCounts.missing(entry, materialList));
        expandedEntry = entry;
        expandedKey = key;
        expandedSummaries = summaries;
        visibleRecipeEntry = entry;
        visibleRecipeKey = key;
        visibleRecipeSummaries = summaries;
        recipeAnimations.start(key, startProgress, 1.0F);
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
            treeAnimations.remove("ingredient:" + key);
            treeAnimations.removeDescendants("ingredient:" + key);
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
        float startProgress = treeProgress(root.path());
        if (expandedTreeNodes.contains(root.path())) {
            collapseTreeNode(root.path());
            treeAnimations.removeDescendants(root.path());
            treeAnimations.start(root.path(), startProgress, 0.0F);
        } else {
            expandedTreeNodes.add(root.path());
            treeAnimations.start(root.path(), startProgress, 1.0F);
        }
    }

    public static MaterialTreeNode getExpandedIngredientTree(IngredientSummary ingredient) {
        MaterialTreeNode root = treeCache.get(key(ingredient));
        if (root == null || !expandedTreeNodes.contains(root.path())) {
            return null;
        }

        return root;
    }

    public static MaterialTreeNode getVisibleIngredientTree(IngredientSummary ingredient) {
        MaterialTreeNode root = treeCache.get(key(ingredient));
        if (root == null) {
            return null;
        }

        return expandedTreeNodes.contains(root.path()) || treeProgress(root.path()) > 0.0F ? root : null;
    }

    public static float treeProgress(String path) {
        return treeAnimations.progress(path, expandedTreeNodes.contains(path));
    }

    public static void pruneTreeAnimations() {
        treeAnimations.prune();
    }

    public static void pruneAnimations() {
        boolean hadRecipeAnimation = recipeAnimations.isActive();
        recipeAnimations.prune();
        treeAnimations.prune();
        if (hadRecipeAnimation && visibleRecipeEntry != null && !isRecipeExpanded(visibleRecipeEntry) && recipeProgress(visibleRecipeEntry) <= 0.0F) {
            visibleRecipeEntry = null;
            visibleRecipeKey = "";
            visibleRecipeSummaries = Collections.emptyList();
            recipeAnimations.clear();
        }
    }

    public static boolean hasActiveTreeAnimations() {
        return treeAnimations.isActive();
    }

    public static boolean hasActiveAnimations() {
        return recipeAnimations.isActive() || treeAnimations.isActive();
    }

    public static float recipeProgress(MaterialListEntry entry) {
        if (!isRecipeVisible(entry)) {
            return 0.0F;
        }

        return recipeAnimations.progress(visibleRecipeKey, isRecipeExpanded(entry));
    }

    public static Set<String> getExpandedTreeNodes() {
        return Collections.unmodifiableSet(expandedTreeNodes);
    }

    public static void toggleTreeNode(String path) {
        float startProgress = treeProgress(path);
        if (expandedTreeNodes.contains(path)) {
            collapseTreeNode(path);
            treeAnimations.removeDescendants(path);
            treeAnimations.start(path, startProgress, 0.0F);
        } else {
            expandedTreeNodes.add(path);
            treeAnimations.start(path, startProgress, 1.0F);
        }
    }

    private static void clearRecipe() {
        if (expandedEntry != null) {
            visibleRecipeEntry = expandedEntry;
            visibleRecipeKey = expandedKey;
            visibleRecipeSummaries = expandedSummaries;
            recipeAnimations.start(visibleRecipeKey, recipeProgress(expandedEntry), 0.0F);
        }
        expandedEntry = null;
        expandedKey = "";
        expandedSummaries = Collections.emptyList();
    }

    private static void clearIngredientTrees() {
        expandedTreeNodes.clear();
        treeAnimations.clear();
    }

    public static void clearRecipeCaches() {
        clearIngredientTrees();
        treeSupportCache.clear();
        treeCache.clear();
    }

    public static void applyRecipePreferences() {
        expandedSummaries = RecipeResolvers.applyPreferredOrder(expandedSummaries);
        visibleRecipeSummaries = RecipeResolvers.applyPreferredOrder(visibleRecipeSummaries);
        clearRecipeCaches();
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
        if (!isRecipeVisible(entry)) {
            return Collections.emptyList();
        }

        return visibleRecipeSummaries;
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
