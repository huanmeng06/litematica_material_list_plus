package io.github.huanmeng06.lmlp.gui.textlist;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import net.minecraft.class_6367;
import net.minecraft.class_8251;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders a true black/white/gray copy of an item's icon for disabled list entries.
 * Vanilla item rendering resets its own shader color before drawing, so tinting can
 * only darken the icon, and sampling the item's particle sprite loses the 3D look of
 * block items. Instead the item is rendered once, exactly as the GUI would draw it,
 * into an offscreen framebuffer; the pixels are read back, converted to luminance,
 * and re-uploaded as a cached texture that gets drawn in place of the colored icon.
 */
final class GrayscaleItemIcon {
    // Render at 4x the on-screen icon size so the result stays crisp at higher GUI scales.
    private static final int FB_SIZE = 64;
    private static final int ICON_SPACE = 16;
    private static final Map<String, class_2960> CACHE = new HashMap<>();

    private GrayscaleItemIcon() {
    }

    /**
     * Builds the grayscale textures for every icon the given entry value can display
     * (wildcard values cycle through several items). Call this outside the render
     * pass -- e.g. when the row widget is created -- so the framebuffer/matrix
     * switching never happens mid-frame, which showed up as a visible flicker.
     */
    static void prewarm(String value) {
        for (ItemIdListIconResolver.Display display : ItemIdListIconResolver.allIcons(value)) {
            if (!display.stack().method_7960() && !CACHE.containsKey(display.id())) {
                CACHE.put(display.id(), buildGrayscaleTexture(null, display.stack()));
            }
        }
    }

    static boolean render(class_332 context, class_1799 stack, String cacheKey, int x, int y, int size) {
        if (!CACHE.containsKey(cacheKey)) {
            CACHE.put(cacheKey, buildGrayscaleTexture(context, stack));
        }

        class_2960 textureId = CACHE.get(cacheKey);
        if (textureId == null) {
            return false;
        }

        context.method_25293(textureId, x, y, size, size, 0.0F, 0.0F, FB_SIZE, FB_SIZE, FB_SIZE, FB_SIZE);
        return true;
    }

    private static class_2960 buildGrayscaleTexture(class_332 outerContext, class_1799 stack) {
        class_310 mc = class_310.method_1551();
        // The outer GUI shares the immediate vertex consumers; flush its pending
        // geometry before retargeting the framebuffer and matrices. Null when
        // called from prewarm(), outside any render pass.
        if (outerContext != null) {
            outerContext.method_51452();
        }
        // The surrounding list widget may have a GL scissor active; its box is in
        // window coordinates, so it would clip the whole offscreen framebuffer away.
        boolean scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (scissorWasEnabled) {
            GlStateManager._disableScissorTest();
        }
        class_6367 framebuffer = new class_6367(FB_SIZE, FB_SIZE, true, class_310.field_1703);
        try {
            framebuffer.method_1236(0.0F, 0.0F, 0.0F, 0.0F);
            framebuffer.method_1230(class_310.field_1703);
            framebuffer.method_1235(true);

            // Same projection/modelview setup vanilla uses for GUI rendering, with the
            // ortho sized to one item slot so the 16px icon fills the whole framebuffer.
            RenderSystem.backupProjectionMatrix();
            Matrix4f projection = new Matrix4f().setOrtho(0.0F, ICON_SPACE, ICON_SPACE, 0.0F, 1000.0F, 21000.0F);
            RenderSystem.setProjectionMatrix(projection, class_8251.field_43361);
            class_4587 modelView = RenderSystem.getModelViewStack();
            modelView.method_22903();
            modelView.method_34426();
            modelView.method_46416(0.0F, 0.0F, -11000.0F);
            RenderSystem.applyModelViewMatrix();

            class_332 offscreenContext = new class_332(mc, mc.method_22940().method_23000());
            offscreenContext.method_51427(stack, 0, 0);
            offscreenContext.method_51452();

            class_1011 image = new class_1011(FB_SIZE, FB_SIZE, false);
            RenderSystem.bindTexture(framebuffer.method_30277());
            image.method_4327(0, false);
            image.method_4319();

            modelView.method_22909();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            mc.method_1522().method_1235(true);

            if (isFullyTransparent(image)) {
                // Offscreen pass produced nothing (unexpected GL state); treat as
                // failure so the caller falls back to the colored icon instead of
                // drawing an invisible one.
                image.close();
                return null;
            }

            toGrayscale(image);
            return mc.method_1531().method_4617("lmlp_gray_icon", new class_1043(image));
        } catch (RuntimeException exception) {
            mc.method_1522().method_1235(true);
            return null;
        } finally {
            framebuffer.method_1238();
            if (scissorWasEnabled) {
                GlStateManager._enableScissorTest();
            }
        }
    }

    private static boolean isFullyTransparent(class_1011 image) {
        int width = image.method_4307();
        int height = image.method_4323();
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                if ((image.method_4315(px, py) >>> 24) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void toGrayscale(class_1011 image) {
        int width = image.method_4307();
        int height = image.method_4323();
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int color = image.method_4315(px, py);
                int r = color & 0xFF;
                int g = (color >>> 8) & 0xFF;
                int b = (color >>> 16) & 0xFF;
                int a = (color >>> 24) & 0xFF;
                int luminance = Math.round(0.299F * r + 0.587F * g + 0.114F * b);
                image.method_4305(px, py, (a << 24) | (luminance << 16) | (luminance << 8) | luminance);
            }
        }
    }
}
