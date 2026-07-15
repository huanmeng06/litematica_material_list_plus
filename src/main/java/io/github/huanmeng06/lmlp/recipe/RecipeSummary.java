package io.github.huanmeng06.lmlp.recipe;

import java.util.List;

import net.minecraft.class_1799;

public final class RecipeSummary {
    private final String category;
    private final String recipeId;
    private final class_1799 outputIcon;
    private final List<class_1799> outputIcons;
    private final int outputCount;
    private final int craftsTotal;
    private final int craftsMissing;
    private final List<IngredientSummary> ingredients;
    private final List<RecipeSlotSummary> inputSlots;
    private final int gridWidth;
    private final int gridHeight;
    private final boolean shapeless;
    private final Object nativeDisplay;

    public RecipeSummary(String category, String recipeId, class_1799 outputIcon, int outputCount, int craftsTotal, int craftsMissing, List<IngredientSummary> ingredients, List<RecipeSlotSummary> inputSlots, int gridWidth, int gridHeight, boolean shapeless) {
        this(category, recipeId, outputIcon, outputCount, craftsTotal, craftsMissing, ingredients, inputSlots, gridWidth, gridHeight, shapeless, null);
    }

    public RecipeSummary(String category, String recipeId, class_1799 outputIcon, int outputCount, int craftsTotal, int craftsMissing, List<IngredientSummary> ingredients, List<RecipeSlotSummary> inputSlots, int gridWidth, int gridHeight, boolean shapeless, Object nativeDisplay) {
        this(category, recipeId, outputIcon, List.of(outputIcon), outputCount, craftsTotal, craftsMissing, ingredients,
                inputSlots, gridWidth, gridHeight, shapeless, nativeDisplay);
    }

    public RecipeSummary(String category, String recipeId, class_1799 outputIcon, List<class_1799> outputIcons,
            int outputCount, int craftsTotal, int craftsMissing, List<IngredientSummary> ingredients,
            List<RecipeSlotSummary> inputSlots, int gridWidth, int gridHeight, boolean shapeless, Object nativeDisplay) {
        this.category = category;
        this.recipeId = recipeId;
        this.outputIcon = outputIcon;
        this.outputIcons = outputIcons.stream()
                .filter(icon -> !icon.method_7960())
                .map(class_1799::method_7972)
                .toList();
        this.outputCount = outputCount;
        this.craftsTotal = craftsTotal;
        this.craftsMissing = craftsMissing;
        this.ingredients = List.copyOf(ingredients);
        this.inputSlots = List.copyOf(inputSlots);
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.shapeless = shapeless;
        this.nativeDisplay = nativeDisplay;
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

    public List<class_1799> outputIcons() {
        return this.outputIcons;
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

    public List<RecipeSlotSummary> inputSlots() {
        return this.inputSlots;
    }

    public int gridWidth() {
        return this.gridWidth;
    }

    public int gridHeight() {
        return this.gridHeight;
    }

    public boolean shapeless() {
        return this.shapeless;
    }

    public Object nativeDisplay() {
        return this.nativeDisplay;
    }
}
