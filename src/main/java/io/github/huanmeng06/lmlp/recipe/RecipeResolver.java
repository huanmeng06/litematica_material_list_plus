package io.github.huanmeng06.lmlp.recipe;

import java.util.List;

import net.minecraft.class_1799;

public interface RecipeResolver {
    List<RecipeSummary> findRecipes(class_1799 target, int totalCount, int missingCount);
}
