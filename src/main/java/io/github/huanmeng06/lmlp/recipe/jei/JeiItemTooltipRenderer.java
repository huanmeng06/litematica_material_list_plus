package io.github.huanmeng06.lmlp.recipe.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.gui.JeiTooltip;
import mezz.jei.common.util.SafeIngredientUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Renders standalone LMLP item icons with the same rich ingredient tooltip
 * pipeline that JEI uses for recipe slots.
 */
public final class JeiItemTooltipRenderer {
    private JeiItemTooltipRenderer() {
    }

    public static boolean render(GuiGraphicsExtractor context, ItemStack stack, int mouseX, int mouseY) {
        if (stack.isEmpty()) {
            return false;
        }

        IJeiRuntime runtime = JeiRuntimeBridge.runtime().orElse(null);
        if (runtime == null) {
            return false;
        }

        IIngredientManager ingredientManager = runtime.getIngredientManager();
        return ingredientManager.createTypedIngredient(VanillaTypes.ITEM_STACK, stack, false)
                .map(ingredient -> renderTyped(context, ingredientManager, ingredient, mouseX, mouseY))
                .orElse(false);
    }

    private static <T> boolean renderTyped(GuiGraphicsExtractor context, IIngredientManager ingredientManager,
            ITypedIngredient<T> ingredient, int mouseX, int mouseY) {
        IIngredientRenderer<T> renderer = ingredientManager.getIngredientRenderer(ingredient.getType());
        JeiTooltip tooltip = new JeiTooltip();
        SafeIngredientUtil.getRichTooltip(tooltip, ingredientManager, renderer, ingredient);
        tooltip.draw(context, mouseX, mouseY);
        return true;
    }
}
