package io.github.huanmeng06.lmlp.gui;

import net.minecraft.class_1799;
import net.minecraft.class_327;
import net.minecraft.class_332;

public final class ItemTooltipRenderer {
    private static final RecipeTooltipBridge TOOLTIP_BRIDGE = createTooltipBridge();

    private ItemTooltipRenderer() {
    }

    public static boolean render(class_332 context, class_327 textRenderer, class_1799 stack, int mouseX, int mouseY) {
        if (stack.method_7960()) {
            return false;
        }

        if (TOOLTIP_BRIDGE.renderTooltip(context, textRenderer, stack, mouseX, mouseY)) {
            return true;
        }

        context.method_51446(textRenderer, stack, mouseX, mouseY);
        return true;
    }

    private static RecipeTooltipBridge createTooltipBridge() {
        try {
            Class<?> bridgeClass = Class.forName("io.github.huanmeng06.lmlp.recipe.rei.ReiTooltipBridge");
            return (RecipeTooltipBridge) bridgeClass.getDeclaredConstructor().newInstance();
        } catch (Throwable throwable) {
            return RecipeTooltipBridge.DISABLED;
        }
    }
}
