package io.github.huanmeng06.lmlp.recipe;

import java.util.List;

import net.minecraft.class_1799;

public final class RecipeSummary {
    private final String category;
    private final String recipeId;
    private final class_1799 outputIcon;
    private final int outputCount;
    private final int craftsTotal;
    private final int craftsMissing;
    private final List<IngredientSummary> ingredients;

    public RecipeSummary(String category, String recipeId, class_1799 outputIcon, int outputCount, int craftsTotal, int craftsMissing, List<IngredientSummary> ingredients) {
        this.category = category;
        this.recipeId = recipeId;
        this.outputIcon = outputIcon;
        this.outputCount = outputCount;
        this.craftsTotal = craftsTotal;
        this.craftsMissing = craftsMissing;
        this.ingredients = List.copyOf(ingredients);
    }

    public String category() {
        return this.category;
    }

    public String recipeId() {
        return this.recipeId;
    }

    public class_1799 outputIcon() {
        return this.outputIcon;
    }

    public int outputCount() {
        return this.outputCount;
    }

    public int craftsTotal() {
        return this.craftsTotal;
    }

    public int craftsMissing() {
        return this.craftsMissing;
    }

    public List<IngredientSummary> ingredients() {
        return this.ingredients;
    }
}
