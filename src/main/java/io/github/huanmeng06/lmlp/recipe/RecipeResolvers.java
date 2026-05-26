package io.github.huanmeng06.lmlp.recipe;

import java.util.Collections;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1799;

public final class RecipeResolvers {
    private static RecipeResolver resolver;

    private RecipeResolvers() {
    }

    public static List<RecipeSummary> findRecipes(class_1799 target, int totalCount, int missingCount) {
        try {
            return getResolver().findRecipes(target, totalCount, missingCount);
        } catch (Throwable throwable) {
            return Collections.emptyList();
        }
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
