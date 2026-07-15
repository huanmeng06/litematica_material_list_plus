package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.render.GuiContext;
import io.github.huanmeng06.lmlp.recipe.jei.JeiItemTooltipRenderer;
import net.minecraft.class_1799;
import net.minecraft.class_327;
import net.minecraft.class_332;

public final class ItemTooltipRenderer {
    private ItemTooltipRenderer() {
    }

    public static boolean render(class_332 context, class_327 textRenderer, class_1799 stack, int mouseX, int mouseY) {
        if (stack.method_7960()) {
            return false;
        }

        class_332 target = context instanceof GuiContext guiContext ? guiContext.getGuiGraphics() : context;
        try {
            if (JeiItemTooltipRenderer.render(target, stack, mouseX, mouseY)) {
                return true;
            }
        } catch (Throwable ignored) {
            // Keep item tooltips available if JEI's runtime or tooltip internals
            // are temporarily unavailable during screen transitions.
        }

        target.method_51446(textRenderer, stack, mouseX, mouseY);
        return true;
    }
}
