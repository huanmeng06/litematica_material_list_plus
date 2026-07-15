package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.render.GuiContext;
import io.github.huanmeng06.lmlp.recipe.jei.JeiItemTooltipRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class ItemTooltipRenderer {
    private ItemTooltipRenderer() {
    }

    public static boolean render(GuiGraphicsExtractor context, Font textRenderer, ItemStack stack, int mouseX, int mouseY) {
        if (stack.isEmpty()) {
            return false;
        }

        GuiGraphicsExtractor target = context instanceof GuiContext guiContext ? guiContext.getGuiGraphics() : context;
        try {
            if (JeiItemTooltipRenderer.render(target, stack, mouseX, mouseY)) {
                return true;
            }
        } catch (Throwable ignored) {
            // Keep item tooltips available if JEI's runtime or tooltip internals
            // are temporarily unavailable during screen transitions.
        }

        target.setTooltipForNextFrame(textRenderer, stack, mouseX, mouseY);
        return true;
    }
}
