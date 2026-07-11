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

        int preferredIndex = preferredIndex(summaries);
        if (preferredIndex <= 0) {
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

    // Which recipe to decompose first. A user-pinned preference always wins; when
    // none is pinned, fall back to a built-in default so ambiguous items resolve
    // deterministically. Returns 0 when the current head is already preferred and
    // -1 when there's nothing to prefer.
    private static int preferredIndex(List<RecipeSummary> summaries) {
        String itemId = ItemStackTexts.id(summaries.get(0).outputIcon());
        String userPin = Configs.preferredRecipeId(itemId);
        if (!userPin.isEmpty()) {
            for (int index = 0; index < summaries.size(); index++) {
                if (userPin.equals(summaries.get(index).recipeId())) {
                    return index;
                }
            }
            return -1;
        }

        // Default: prefer a recipe whose every ingredient is planks. This keeps
        // wooden intermediates like sticks (craftable from planks OR bamboo)
        // decomposing along the planks -> logs chain, matching how slabs behave,
        // instead of depending on REI's non-deterministic enumeration order.
        for (int index = 0; index < summaries.size(); index++) {
            if (isAllPlanksRecipe(summaries.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isAllPlanksRecipe(RecipeSummary summary) {
        if (summary.ingredients().isEmpty()) {
            return false;
        }
        for (IngredientSummary ingredient : summary.ingredients()) {
            if (ingredient.icons().isEmpty()) {
                return false;
            }
            for (class_1799 icon : ingredient.icons()) {
                if (!itemPath(ItemStackTexts.id(icon)).endsWith("_planks")) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String itemPath(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
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
