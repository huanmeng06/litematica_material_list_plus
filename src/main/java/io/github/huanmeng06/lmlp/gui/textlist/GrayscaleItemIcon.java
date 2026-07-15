package io.github.huanmeng06.lmlp.gui.textlist;

import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the disabled state for an item icon.
 *
 * <p>Minecraft 1.21.11 uses the queued GUI renderer, so
 * the old synchronous framebuffer readback used by earlier versions can no
 * longer safely capture an icon. The neutral veil keeps disabled entries
 * visually distinct without reaching into the renderer's private queue.</p>
 */
final class GrayscaleItemIcon {
    private static final int DISABLED_VEIL = 0x99505050;

    private GrayscaleItemIcon() {
    }

    static void prewarm(String value) {
        // GUI item rendering is queued in 1.21.11; there is no cache to prewarm.
    }

    static boolean render(GuiContext context, ItemStack stack, String cacheKey, int x, int y, int size) {
        if (context == null || stack == null || stack.isEmpty()) {
            return false;
        }

        context.renderItem(stack, x, y);
        context.fill(x, y, x + size, y + size, DISABLED_VEIL);
        return true;
    }
}
