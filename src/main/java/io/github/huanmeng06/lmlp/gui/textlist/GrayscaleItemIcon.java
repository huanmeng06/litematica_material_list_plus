package io.github.huanmeng06.lmlp.gui.textlist;

import net.minecraft.class_1799;
import net.minecraft.class_332;

/**
 * Renders the disabled state with the same neutral veil used by the modern
 * queued GUI renderer. Keeping the normal item render underneath preserves
 * its recognizable colors and model while clearly marking it as disabled.
 */
final class GrayscaleItemIcon {
    private static final int DISABLED_VEIL = 0x99505050;

    private GrayscaleItemIcon() {
    }

    static void prewarm(String value) {
        // The veil is drawn directly and does not require an offscreen cache.
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
