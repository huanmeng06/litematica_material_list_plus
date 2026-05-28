package io.github.huanmeng06.lmlp.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
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
    private static MaterialListEntry treeEntry;
    private static String treeOwnerKey = "";
    private static MaterialTreeNode treeRoot;
    private static final Set<String> expandedTreeNodes = new HashSet<>();
    private static final Map<String, Boolean> treeSupportCache = new HashMap<>();
    private static final Map<String, MaterialTreeNode> treeCache = new HashMap<>();

    private MaterialListPlusState() {
    }

    public static boolean isExpanded(MaterialListEntry entry) {
        return isRecipeExpanded(entry) || isTreeExpanded(entry);
    }

    public static boolean isRecipeExpanded(MaterialListEntry entry) {
        return expandedEntry != null && expandedEntry.equals(entry);
    }

    public static boolean isTreeExpanded(MaterialListEntry entry) {
        return treeEntry != null && treeEntry.equals(entry);
    }

    public static void toggle(MaterialListEntry entry, MaterialListBase materialList) {
        if (isRecipeExpanded(entry)) {
            clearRecipe();
        } else {
            open(entry, materialList);
        }
    }

    public static void open(MaterialListEntry entry, MaterialListBase materialList) {
        clearTree();
        expandedEntry = entry;
        expandedKey = key(entry, materialList);
        expandedSummaries = RecipeResolvers.findRecipes(entry.getStack(), MaterialCounts.total(entry, materialList), MaterialCounts.missing(entry, materialList));
    }

    public static void clear() {
        clearRecipe();
        clearTree();
    }

    public static boolean hasTree(IngredientSummary ingredient) {
        String key = key(ingredient);
        Boolean cached = treeSupportCache.get(key);
        if (cached != null) {
            return cached;
        }

        boolean supported = MaterialTreeBuilder.hasChildren(ingredient.icon(), ingredient.countTotal(), ingredient.countMissing());
        treeSupportCache.put(key, supported);
        return supported;
    }

    public static void openTree(MaterialListEntry entry, MaterialListBase materialList, IngredientSummary ingredient) {
        String ownerKey = key(entry, materialList);
        String key = key(ingredient);
        MaterialTreeNode root = treeCache.computeIfAbsent(key, ignored -> MaterialTreeBuilder.build(ingredient.icon(), RecipeSummaryFormatter.ingredientName(ingredient), ingredient.countTotal(), ingredient.countMissing()));
        if (!root.hasChildren()) {
            treeSupportCache.put(key, false);
            clearTree();
            return;
        }

        clearRecipe();
        treeSupportCache.put(key, true);
        treeEntry = entry;
        treeOwnerKey = ownerKey;
        treeRoot = root;
        expandedTreeNodes.clear();
        expandedTreeNodes.add(root.path());
    }

    public static MaterialTreeNode getTreeRoot(MaterialListEntry entry, MaterialListBase materialList) {
        if (!isTreeExpanded(entry)) {
            return null;
        }

        String ownerKey = key(entry, materialList);
        if (!ownerKey.equals(treeOwnerKey)) {
            clearTree();
            return null;
        }

        return treeRoot;
    }

    public static MaterialTreeNode getCachedTreeRoot(MaterialListEntry entry) {
        if (!isTreeExpanded(entry)) {
            return null;
        }

        return treeRoot;
    }

    public static Set<String> getExpandedTreeNodes() {
        return Collections.unmodifiableSet(expandedTreeNodes);
    }

    public static void toggleTreeNode(String path) {
        if (expandedTreeNodes.contains(path)) {
            expandedTreeNodes.remove(path);
        } else {
            expandedTreeNodes.add(path);
        }
    }

    private static void clearRecipe() {
        expandedEntry = null;
        expandedKey = "";
        expandedSummaries = Collections.emptyList();
    }

    private static void clearTree() {
        treeEntry = null;
        treeOwnerKey = "";
        treeRoot = null;
        expandedTreeNodes.clear();
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
