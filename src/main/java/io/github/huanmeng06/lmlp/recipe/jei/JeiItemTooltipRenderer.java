package io.github.huanmeng06.lmlp.recipe.jei;

import java.util.List;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.gui.TooltipRenderer;
import mezz.jei.common.util.SafeIngredientUtil;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_332;

public final class JeiItemTooltipRenderer {
    private JeiItemTooltipRenderer() {
    }

    public static boolean render(class_332 context, class_1799 stack, int mouseX, int mouseY) {
        if (stack.method_7960()) return false;
        IJeiRuntime runtime = JeiRuntimeBridge.runtime().orElse(null);
        if (runtime == null) return false;
        IIngredientManager manager = runtime.getIngredientManager();
        return manager.createTypedIngredient(VanillaTypes.ITEM_STACK, stack)
                .map(ingredient -> renderTyped(context, runtime, manager, ingredient, mouseX, mouseY))
                .orElse(false);
    }

    private static <T> boolean renderTyped(class_332 context, IJeiRuntime runtime, IIngredientManager manager,
            ITypedIngredient<T> ingredient, int mouseX, int mouseY) {
        IIngredientRenderer<T> renderer = manager.getIngredientRenderer(ingredient.getType());
        List<class_2561> lines = SafeIngredientUtil.getTooltip(manager, renderer, ingredient);
        lines = runtime.getJeiHelpers().getModIdHelper().addModNameToIngredientTooltip(lines, ingredient);
        TooltipRenderer.drawHoveringText(context, lines, mouseX, mouseY, ingredient, renderer, manager);
        return true;
    }
}
