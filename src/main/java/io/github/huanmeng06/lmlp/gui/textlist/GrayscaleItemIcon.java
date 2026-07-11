package io.github.huanmeng06.lmlp.gui.textlist;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1799;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_10366;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_6367;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
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
    private static int textureSequence;

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

        context.method_25293(class_1921::method_62277, textureId, x, y, 0.0F, 0.0F, size, size, FB_SIZE, FB_SIZE, FB_SIZE, FB_SIZE, -1);
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
        class_6367 framebuffer = new class_6367(FB_SIZE, FB_SIZE, true);
        try {
            framebuffer.method_1236(0.0F, 0.0F, 0.0F, 0.0F);
            framebuffer.method_1230();
            framebuffer.method_1235(true);

            // Same projection/modelview setup vanilla uses for GUI rendering, with the
            // ortho sized to one item slot so the 16px icon fills the whole framebuffer.
            RenderSystem.backupProjectionMatrix();
            Matrix4f projection = new Matrix4f().setOrtho(0.0F, ICON_SPACE, ICON_SPACE, 0.0F, 1000.0F, 21000.0F);
            RenderSystem.setProjectionMatrix(projection, class_10366.field_54954);
            Matrix4fStack modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix();
            modelView.identity();
            modelView.translate(0.0F, 0.0F, -11000.0F);

            class_332 offscreenContext = new class_332(mc, mc.method_22940().method_23000());
            offscreenContext.method_51427(stack, 0, 0);
            offscreenContext.method_51452();

            class_1011 image = new class_1011(FB_SIZE, FB_SIZE, false);
            RenderSystem.bindTexture(framebuffer.method_30277());
            image.method_4327(0, false);
            image.method_4319();

            modelView.popMatrix();
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
            class_2960 textureId = class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "gray_icon/" + (textureSequence++));
            mc.method_1531().method_4616(textureId, new class_1043(image));
            return textureId;
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
                if ((image.method_61940(px, py) >>> 24) != 0) {
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
                // method_61940 returns ARGB (0xAARRGGBB); the old private accessor
                // returned ABGR, so red/blue occupy the swapped byte positions here.
                int color = image.method_61940(px, py);
                int r = (color >>> 16) & 0xFF;
                int g = (color >>> 8) & 0xFF;
                int b = color & 0xFF;
                int a = (color >>> 24) & 0xFF;
                int luminance = Math.round(0.299F * r + 0.587F * g + 0.114F * b);
                image.method_61941(px, py, (a << 24) | (luminance << 16) | (luminance << 8) | luminance);
            }
        }
    }
}
