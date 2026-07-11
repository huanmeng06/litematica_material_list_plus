package io.github.huanmeng06.lmlp.gui.textlist;

import net.minecraft.class_1799;
import net.minecraft.class_332;

/**
 * On other Minecraft versions this renders a true black/white/gray copy of an
 * item's icon for disabled list entries, by drawing the item into an offscreen
 * framebuffer, reading the pixels back synchronously, converting them to
 * luminance, and re-uploading the result as a cached texture.
 *
 * <p>mc1.21.5 replaced the immediate GL pipeline with the GpuDevice/RenderPass
 * API, and texture read-back is now asynchronous (CommandEncoder
 * #copyTextureToBuffer completes via a Runnable callback into a GpuBuffer).
 * The synchronous "render -&gt; read pixels -&gt; grayscale -&gt; re-upload"
 * flow this feature relies on can no longer run inline during a GUI frame, so
 * the grayscale icon is disabled on this version. The caller falls back to the
 * normal coloured icon, which is the same fallback used when the offscreen pass
 * fails on any other version.
 */
final class GrayscaleItemIcon {
    private GrayscaleItemIcon() {
    }

    /** No-op on mc1.21.5; see the class comment. */
    static void prewarm(String value) {
    }

    /**
     * Always returns {@code false} on mc1.21.5 so the caller draws the normal
     * coloured icon; see the class comment for why grayscale is unavailable here.
     */
    static boolean render(class_332 context, class_1799 stack, String cacheKey, int x, int y, int size) {
        return false;
    }
}
