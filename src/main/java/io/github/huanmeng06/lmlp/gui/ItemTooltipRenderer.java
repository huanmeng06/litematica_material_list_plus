package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.render.GuiContext;
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
        target.setTooltipForNextFrame(textRenderer, stack, mouseX, mouseY);
        return true;
    }
}
