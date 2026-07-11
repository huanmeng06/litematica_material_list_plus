package io.github.huanmeng06.lmlp.gui.textlist;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTexture;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import net.minecraft.class_1011;
import net.minecraft.class_10366;
import net.minecraft.class_1043;
import net.minecraft.class_1799;
import net.minecraft.class_1921;
import net.minecraft.class_276;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_6367;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders a true black/white/gray copy of an item's vanilla GUI icon for disabled list entries.
 */
final class GrayscaleItemIcon {
    private static final int FB_SIZE = 64;
    private static final int ICON_SPACE = 16;
    private static final Map<String, CacheEntry> CACHE = new HashMap<>();
    private static int nextTextureId;

    private GrayscaleItemIcon() {
    }

    static void prewarm(String value) {
        for (ItemIdListIconResolver.Display display : ItemIdListIconResolver.allIcons(value)) {
            if (!display.stack().method_7960()) {
                ensureStarted(display.id(), display.stack(), null);
            }
        }
    }

    static boolean render(class_332 context, class_1799 stack, String cacheKey, int x, int y, int size) {
        CacheEntry entry = CACHE.get(cacheKey);
        if (entry == null) {
            ensureStarted(cacheKey, stack, context);
            return false;
        }

        if (entry.status != Status.READY || entry.textureId == null) {
            return false;
        }

        context.method_25293(class_1921::method_62277, entry.textureId, x, y, 0.0F, 0.0F,
                size, size, FB_SIZE, FB_SIZE, FB_SIZE, FB_SIZE, 0xFFFFFFFF);
        return true;
    }

    private static void ensureStarted(String cacheKey, class_1799 stack, class_332 outerContext) {
        if (CACHE.containsKey(cacheKey)) {
            return;
        }

        CacheEntry entry = new CacheEntry();
        CACHE.put(cacheKey, entry);
        try {
            startBuild(cacheKey, stack, outerContext, entry);
        } catch (RuntimeException exception) {
            entry.status = Status.FAILED;
        }
    }

    private static void startBuild(String cacheKey, class_1799 stack, class_332 outerContext, CacheEntry entry) {
        class_6367 framebuffer = new class_6367("lmlp grayscale item icon", FB_SIZE, FB_SIZE, true);
        GpuTexture colorTexture = framebuffer.method_30277();
        if (colorTexture == null) {
            framebuffer.method_1238();
            entry.status = Status.FAILED;
            return;
        }

        clear(framebuffer, colorTexture);
        renderVanillaIconTo(framebuffer, stack, outerContext);
        copyToImage(cacheKey, entry, framebuffer, colorTexture);
    }

    private static void clear(class_276 framebuffer, GpuTexture colorTexture) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        GpuTexture depthTexture = framebuffer.field_1478 ? framebuffer.method_30278() : null;
        if (depthTexture != null) {
            encoder.clearColorAndDepthTextures(colorTexture, 0x00000000, depthTexture, 1.0D);
        } else {
            encoder.clearColorTexture(colorTexture, 0x00000000);
        }
    }

    private static void renderVanillaIconTo(class_276 framebuffer, class_1799 stack, class_332 outerContext) {
        class_310 mc = class_310.method_1551();
        if (outerContext != null) {
            outerContext.method_51452();
        }

        ScissorSnapshot scissor = ScissorSnapshot.capture();
        RenderSystem.disableScissor();
        RenderSystem.backupProjectionMatrix();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        GrayscaleRenderTargetOverride.set(framebuffer);
        try {
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0.0F, ICON_SPACE, ICON_SPACE, 0.0F, 1000.0F, 21000.0F),
                    class_10366.field_54954);
            modelView.identity();
            modelView.translate(0.0F, 0.0F, -11000.0F);

            class_332 offscreenContext = new class_332(mc, mc.method_22940().method_23000());
            offscreenContext.method_51427(stack, 0, 0);
            offscreenContext.method_51452();
        } finally {
            GrayscaleRenderTargetOverride.clear(framebuffer);
            modelView.popMatrix();
            RenderSystem.restoreProjectionMatrix();
            scissor.restore();
        }
    }

    private static void copyToImage(String cacheKey, CacheEntry entry, class_276 framebuffer, GpuTexture colorTexture) {
        GpuDevice device = RenderSystem.getDevice();
        int pixelSize = colorTexture.getFormat().pixelSize();
        GpuBuffer buffer = device.createBuffer(() -> "lmlp grayscale icon readback",
                BufferType.PIXEL_PACK, BufferUsage.STATIC_READ, FB_SIZE * FB_SIZE * pixelSize);
        CommandEncoder readEncoder = device.createCommandEncoder();
        device.createCommandEncoder().copyTextureToBuffer(colorTexture, buffer, 0,
                () -> finishReadback(cacheKey, entry, framebuffer, colorTexture, buffer, readEncoder), 0);
    }

    private static void finishReadback(
            String cacheKey,
            CacheEntry entry,
            class_276 framebuffer,
            GpuTexture colorTexture,
            GpuBuffer buffer,
            CommandEncoder readEncoder
    ) {
        class_1011 image = null;
        try (GpuBuffer.ReadView view = readEncoder.readBuffer(buffer)) {
            image = new class_1011(FB_SIZE, FB_SIZE, false);
            ByteBuffer data = view.data();
            int pixelSize = colorTexture.getFormat().pixelSize();
            for (int y = 0; y < FB_SIZE; y++) {
                for (int x = 0; x < FB_SIZE; x++) {
                    int color = data.getInt((x + y * FB_SIZE) * pixelSize);
                    image.method_4305(x, FB_SIZE - y - 1, color);
                }
            }
        } catch (RuntimeException exception) {
            if (image != null) {
                image.close();
            }
            entry.status = Status.FAILED;
            return;
        } finally {
            buffer.close();
            framebuffer.method_1238();
        }

        if (isFullyTransparent(image)) {
            image.close();
            entry.status = Status.FAILED;
            return;
        }

        toGrayscale(image);
        class_1011 readyImage = image;
        class_310 mc = class_310.method_1551();
        Runnable upload = () -> upload(cacheKey, entry, readyImage);
        if (RenderSystem.isOnRenderThread()) {
            upload.run();
        } else {
            mc.method_18859(upload);
        }
    }

    private static void upload(String cacheKey, CacheEntry entry, class_1011 image) {
        try {
            class_2960 textureId = class_2960.method_60655(
                    LitematicaMaterialListPlus.MOD_ID,
                    "dynamic/grayscale_item_icon_" + nextTextureId++);
            class_310.method_1551().method_1531().method_4616(textureId, new class_1043(() -> cacheKey, image));
            entry.textureId = textureId;
            entry.status = Status.READY;
        } catch (RuntimeException exception) {
            image.close();
            entry.status = Status.FAILED;
        }
    }

    private static boolean isFullyTransparent(class_1011 image) {
        for (int py = 0; py < image.method_4323(); py++) {
            for (int px = 0; px < image.method_4307(); px++) {
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
                int color = image.method_61940(px, py);
                int a = (color >>> 24) & 0xFF;
                int r = (color >>> 16) & 0xFF;
                int g = (color >>> 8) & 0xFF;
                int b = color & 0xFF;
                int luminance = Math.round(0.299F * r + 0.587F * g + 0.114F * b);
                image.method_61941(px, py, (a << 24) | (luminance << 16) | (luminance << 8) | luminance);
            }
        }
    }

    private enum Status {
        PENDING,
        READY,
        FAILED
    }

    private static final class CacheEntry {
        private volatile Status status = Status.PENDING;
        private volatile class_2960 textureId;
    }

    private static final class ScissorSnapshot {
        private final boolean enabled;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private ScissorSnapshot(boolean enabled, int x, int y, int width, int height) {
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private static ScissorSnapshot capture() {
            ScissorState state = RenderSystem.SCISSOR_STATE;
            return new ScissorSnapshot(state.isEnabled(), state.getX(), state.getY(), state.getWidth(), state.getHeight());
        }

        private void restore() {
            if (this.enabled) {
                RenderSystem.enableScissor(this.x, this.y, this.width, this.height);
            } else {
                RenderSystem.disableScissor();
            }
        }
    }
}
