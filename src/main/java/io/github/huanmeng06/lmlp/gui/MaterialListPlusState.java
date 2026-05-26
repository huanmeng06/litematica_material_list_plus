package io.github.huanmeng06.lmlp.gui;

import java.util.Collections;
import java.util.List;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.material.MaterialCounts;
import io.github.huanmeng06.lmlp.recipe.RecipeResolvers;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;

public final class MaterialListPlusState {
    private static MaterialListEntry expandedEntry;
    private static String expandedKey = "";
    private static List<RecipeSummary> expandedSummaries = Collections.emptyList();

    private MaterialListPlusState() {
    }

    public static boolean isExpanded(MaterialListEntry entry) {
        return expandedEntry != null && expandedEntry.equals(entry);
    }

    public static void toggle(MaterialListEntry entry, MaterialListBase materialList) {
        if (isExpanded(entry)) {
            clear();
        } else {
            open(entry, materialList);
        }
    }

    public static void open(MaterialListEntry entry, MaterialListBase materialList) {
        expandedEntry = entry;
        expandedKey = key(entry, materialList);
        expandedSummaries = RecipeResolvers.findRecipes(entry.getStack(), MaterialCounts.total(entry, materialList), MaterialCounts.missing(entry, materialList));
    }

    public static void clear() {
        expandedEntry = null;
        expandedKey = "";
        expandedSummaries = Collections.emptyList();
    }

    public static List<RecipeSummary> getSummaries(MaterialListEntry entry, MaterialListBase materialList) {
        if (!isExpanded(entry)) {
            return Collections.emptyList();
        }

        String key = key(entry, materialList);
        if (!key.equals(expandedKey)) {
            open(entry, materialList);
        }

        return expandedSummaries;
    }

    public static List<RecipeSummary> getCachedSummaries(MaterialListEntry entry) {
        if (!isExpanded(entry)) {
            return Collections.emptyList();
        }

        return expandedSummaries;
    }

    public static List<RecipeSummary> resolveFor(MaterialListEntry entry, MaterialListBase materialList) {
        return RecipeResolvers.findRecipes(entry.getStack(), MaterialCounts.total(entry, materialList), MaterialCounts.missing(entry, materialList));
    }

    private static String key(MaterialListEntry entry, MaterialListBase materialList) {
        return ItemStackTexts.id(entry.getStack()) + "|" + MaterialCounts.total(entry, materialList) + "|" + MaterialCounts.missing(entry, materialList);
    }
}
