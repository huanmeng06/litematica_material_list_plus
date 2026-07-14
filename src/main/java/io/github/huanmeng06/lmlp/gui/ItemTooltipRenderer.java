package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.render.GuiContext;
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
        target.method_51446(textRenderer, stack, mouseX, mouseY);
        return true;
    }
}
