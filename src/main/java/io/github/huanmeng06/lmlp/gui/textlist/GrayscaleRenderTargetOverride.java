package io.github.huanmeng06.lmlp.gui.textlist;

import net.minecraft.class_276;

public final class GrayscaleRenderTargetOverride {
    private static class_276 target;

    private GrayscaleRenderTargetOverride() {
    }

    public static class_276 get() {
        return target;
    }

    static void set(class_276 framebuffer) {
        target = framebuffer;
    }

    static void clear(class_276 framebuffer) {
        if (target == framebuffer) {
            target = null;
        }
    }
}
