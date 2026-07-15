package io.github.huanmeng06.lmlp.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public interface RecipeResolver {
    List<RecipeSummary> findRecipes(ItemStack target, int totalCount, int missingCount);
}
