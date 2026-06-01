package io.github.huanmeng06.lmlp.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1799;

public final class RecipeResolvers {
    private static RecipeResolver resolver;

    private RecipeResolvers() {
    }

    public static List<RecipeSummary> findRecipes(class_1799 target, int totalCount, int missingCount) {
        if (Configs.shouldStopRecipeDecomposition(ItemStackTexts.id(target))) {
            return Collections.emptyList();
        }

        try {
            return applyPreferredOrder(getResolver().findRecipes(target, totalCount, missingCount));
        } catch (Throwable throwable) {
            return Collections.emptyList();
        }
    }

    public static List<RecipeSummary> applyPreferredOrder(List<RecipeSummary> summaries) {
        if (summaries.size() <= 1) {
            return summaries;
        }

        String itemId = ItemStackTexts.id(summaries.get(0).outputIcon());
        String preferredRecipeId = Configs.preferredRecipeId(itemId);
        if (preferredRecipeId.isEmpty() || preferredRecipeId.equals(summaries.get(0).recipeId())) {
            return summaries;
        }

        int preferredIndex = -1;
        for (int index = 1; index < summaries.size(); index++) {
            if (preferredRecipeId.equals(summaries.get(index).recipeId())) {
                preferredIndex = index;
                break;
            }
        }

        if (preferredIndex < 0) {
            return summaries;
        }

        List<RecipeSummary> sorted = new ArrayList<>(summaries.size());
        sorted.add(summaries.get(preferredIndex));
        for (int index = 0; index < summaries.size(); index++) {
            if (index != preferredIndex) {
                sorted.add(summaries.get(index));
            }
        }
        return List.copyOf(sorted);
    }

    private static RecipeResolver getResolver() throws ReflectiveOperationException {
        if (resolver != null) {
            return resolver;
        }

        if (FabricLoader.getInstance().isModLoaded("roughlyenoughitems")) {
            Class<?> resolverClass = Class.forName("io.github.huanmeng06.lmlp.recipe.rei.ReiRecipeResolver");
            resolver = (RecipeResolver) resolverClass.getDeclaredConstructor().newInstance();
        } else {
            resolver = (target, totalCount, missingCount) -> Collections.emptyList();
        }

        return resolver;
    }
}
