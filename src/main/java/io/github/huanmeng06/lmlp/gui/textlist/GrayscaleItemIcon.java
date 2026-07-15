package io.github.huanmeng06.lmlp.gui.textlist;

import net.minecraft.class_1799;
import net.minecraft.class_332;

/**
 * Renders the disabled state for an item icon.
 *
 * <p>Minecraft 1.21.10 moved GUI item drawing to the queued GUI renderer, so
 * the old synchronous framebuffer readback used by earlier versions can no
 * longer safely capture an icon. The neutral veil keeps disabled entries
 * visually distinct without reaching into the renderer's private queue.</p>
 */
final class GrayscaleItemIcon {
    private static final int DISABLED_VEIL = 0x99505050;

    private GrayscaleItemIcon() {
    }

    static void prewarm(String value) {
        // GUI item rendering is queued in 1.21.10; there is no cache to prewarm.
    }

    static boolean render(class_332 context, class_1799 stack, String cacheKey, int x, int y, int size) {
        if (context == null || stack == null || stack.method_7960()) {
            return false;
        }

        context.method_51427(stack, x, y);
        context.method_25294(x, y, x + size, y + size, DISABLED_VEIL);
        return true;
    }
}
