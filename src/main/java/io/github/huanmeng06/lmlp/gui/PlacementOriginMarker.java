package io.github.huanmeng06.lmlp.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.config.Configs;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.class_10789;
import net.minecraft.class_1297;
import net.minecraft.class_276;
import net.minecraft.class_238;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_638;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_9801;
import net.minecraft.class_9799;
import org.joml.Matrix4f;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlacementOriginMarker {
    public static final int ORIGIN_TEXT_COLOR = 0xFF55FF55;

    private static final class_2960 TARGET_TEXTURE = class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "textures/gui/origin_target.png");
    private static final Pattern ORIGIN_PATTERN = Pattern.compile("^\\s*\\[?\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*]?\\s*$");
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";
    private static final double ARRIVAL_DISTANCE_SQUARED = 4.0D;
    private static final double BEAM_RADIUS = 0.18D;
    private static final int BEAM_HALF_HEIGHT = 512;
    private static final Color4f BEAM_FILL = new Color4f(1.0F, 0.05F, 0.05F, 0.16F);
    private static final Color4f BEAM_OUTLINE = new Color4f(1.0F, 0.05F, 0.05F, 0.55F);
    private static final float LABEL_SCALE_BASE = 0.0266F;
    private static final int TARGET_HALF_SIZE = 10;
    private static final int LABEL_ELEVATE_BY = -28;
    private static final int LABEL_PADDING_X = 2;
    private static final int LABEL_LINE_HEIGHT = 9;
    private static final int LABEL_TEXT_COLOR = 0xFFCCCCCC;
    private static final int LABEL_LIGHT = 0x00F000F0;
    private static final float TARGET_ICON_LAYER_Z = 0.01F;
    private static final float LABEL_BACKGROUND_LAYER_Z = 0.02F;
    private static final float LABEL_TEXT_LAYER_Z = 0.03F;
    private static final RenderPipeline POSITION_COLOR_OVERLAY_PIPELINE = RenderPipeline.builder()
            .withLocation(class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_position_color_overlay"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withUniform("ModelViewMat", class_10789.field_56747)
            .withUniform("ProjMat", class_10789.field_56747)
            .withUniform("ColorModulator", class_10789.field_56746)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(class_290.field_1576, VertexFormat.class_5596.field_27382)
            .build();
    private static final RenderPipeline POSITION_COLOR_LINES_OVERLAY_PIPELINE = RenderPipeline.builder()
            .withLocation(class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_position_color_lines_overlay"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withUniform("ModelViewMat", class_10789.field_56747)
            .withUniform("ProjMat", class_10789.field_56747)
            .withUniform("ColorModulator", class_10789.field_56746)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(class_290.field_1576, VertexFormat.class_5596.field_29345)
            .build();
    private static final RenderPipeline POSITION_TEX_COLOR_OVERLAY_PIPELINE = RenderPipeline.builder()
            .withLocation(class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_position_tex_color_overlay"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withUniform("ModelViewMat", class_10789.field_56747)
            .withUniform("ProjMat", class_10789.field_56747)
            .withUniform("ColorModulator", class_10789.field_56746)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(class_290.field_1575, VertexFormat.class_5596.field_27382)
            .build();

    private static Marker marker;

    private PlacementOriginMarker() {
    }

    public static void clear() {
        marker = null;
    }

    public static boolean handleOriginClick(KnownPlacementContext context) {
        if (!canHighlightOrigin(context)) {
            return false;
        }

        class_2338 sourcePos = parseOrigin(context.originPosition());
        if (sourcePos == null) {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "lmlp.message.origin_marker_origin_missing");
            return true;
        }

        long now = System.currentTimeMillis();
        marker = Marker.show(context.key(), context.name(), context.dimension(), sourcePos, now);
        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "lmlp.message.origin_marker_highlight_added");
        return true;
    }

    public static boolean originHovered(KnownPlacementContext context, KnownPlacementRows.PlacementLine line, WidgetBase widget, int mouseX, int mouseY) {
        return canHighlightOrigin(context) && line.originHovered(widget, mouseX, mouseY);
    }

    public static boolean disabledOriginHovered(KnownPlacementContext context, KnownPlacementRows.PlacementLine line, WidgetBase widget, int mouseX, int mouseY) {
        return hasValidOrigin(context) && !canHighlightOrigin(context) && line.originHovered(widget, mouseX, mouseY);
    }

    public static boolean canHighlightOrigin(KnownPlacementContext context) {
        return hasValidOrigin(context) && isCurrentDimension(context.dimension());
    }

    public static boolean hasHighlight(KnownPlacementContext context) {
        return hasActiveMarker(context);
    }

    public static boolean hasBeam(KnownPlacementContext context) {
        return hasActiveMarker(context);
    }

    public static void render(WorldRenderContext context) {
        Marker current = activeMarker();
        if (current == null || context == null || context.camera() == null || context.world() == null) {
            return;
        }

        RenderTarget target = resolveRenderTarget(current.sourceDimension, current.sourcePos, currentDimensionId(context.world()));
        if (target == null) {
            return;
        }

        class_310 client = class_310.method_1551();
        if (client.field_1724 != null && client.field_1724.method_5707(target.pos.method_46558()) <= ARRIVAL_DISTANCE_SQUARED) {
            clear();
            return;
        }

        class_243 cameraPos = context.camera().method_19326();
        beginOverlayState();

        try {
            drawBeam(context.matrixStack(), target.pos, cameraPos);
            if (target.sameDimension && client.field_1724 != null) {
                drawTargetInfo(context.matrixStack(), context.camera(), client, current, target.pos, true, false);
            }
        } finally {
            endOverlayState();
        }
    }

    public static void renderLabelOverlayAfterLitematica(Matrix4f worldRenderMatrix, net.minecraft.class_4184 camera, class_310 client) {
        Marker current = activeMarker();
        if (current == null || worldRenderMatrix == null || camera == null || client == null || client.field_1687 == null) {
            return;
        }

        RenderTarget target = resolveRenderTarget(current.sourceDimension, current.sourcePos, currentDimensionId(client.field_1687));
        if (target == null || !target.sameDimension || client.field_1724 == null || client.field_1772 == null) {
            return;
        }

        class_4587 matrices = new class_4587();
        matrices.method_34425(worldRenderMatrix);
        beginOverlayState();
        try {
            drawTargetInfo(matrices, camera, client, current, target.pos, false, true);
        } finally {
            endOverlayState();
        }
    }

    private static boolean hasActiveMarker(KnownPlacementContext context) {
        Marker current = activeMarker();
        return context != null
                && current != null
                && Objects.equals(current.key, context.key())
                && current.active(System.currentTimeMillis())
                && isCurrentDimension(current.sourceDimension);
    }

    private static Marker activeMarker() {
        long now = System.currentTimeMillis();
        if (marker != null && !marker.active(now)) {
            marker = null;
        }

        return marker;
    }

    private static void beginOverlayState() {
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(770, 771, 1, 0);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void endOverlayState() {
        GlStateManager._enableCull();
        GlStateManager._depthMask(true);
        GlStateManager._enableDepthTest();
        GlStateManager._disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.lineWidth(1.0F);
    }

    private static void drawBeam(class_4587 matrices, class_2338 pos, class_243 cameraPos) {
        beginOverlayState();

        double centerX = pos.method_10263() + 0.5D - cameraPos.field_1352;
        double centerY = pos.method_10264() + 0.5D - cameraPos.field_1351;
        double centerZ = pos.method_10260() + 0.5D - cameraPos.field_1350;
        float minX = (float) -BEAM_RADIUS;
        float maxX = (float) BEAM_RADIUS;
        float minZ = (float) -BEAM_RADIUS;
        float maxZ = (float) BEAM_RADIUS;
        float minY = -BEAM_HALF_HEIGHT;
        float maxY = BEAM_HALF_HEIGHT;

        matrices.method_22903();
        matrices.method_22904(centerX, centerY, centerZ);
        Matrix4f matrix = matrices.method_23760().method_23761();

        class_289 tessellator = class_289.method_1348();
        class_287 buffer = tessellator.method_60827(VertexFormat.class_5596.field_27382, class_290.field_1576);
        drawBoxQuads(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, BEAM_FILL);
        drawBuiltBuffer(buffer.method_60794(), POSITION_COLOR_OVERLAY_PIPELINE, null);

        RenderSystem.lineWidth(1.6F);
        buffer = tessellator.method_60827(VertexFormat.class_5596.field_29345, class_290.field_1576);
        drawBoxLines(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, BEAM_OUTLINE);
        drawBuiltBuffer(buffer.method_60794(), POSITION_COLOR_LINES_OVERLAY_PIPELINE, null);
        RenderSystem.lineWidth(1.0F);
        matrices.method_22909();
    }

    private static void drawBoxQuads(class_287 buffer, Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color) {
        quad(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, color);
        quad(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, color);
        quad(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, color);
        quad(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        quad(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        quad(buffer, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ, color);
    }

    private static void quad(class_287 buffer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             Color4f color) {
        colorVertex(buffer, matrix, x1, y1, z1, color);
        colorVertex(buffer, matrix, x2, y2, z2, color);
        colorVertex(buffer, matrix, x3, y3, z3, color);
        colorVertex(buffer, matrix, x4, y4, z4, color);
    }

    private static void drawBoxLines(class_287 buffer, Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color) {
        line(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, color);
        line(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, color);
        line(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, color);
        line(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, color);
        line(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, color);
        line(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        line(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        line(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, color);
        line(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, color);
        line(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, color);
        line(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        line(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, color);
    }

    private static void line(class_287 buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, Color4f color) {
        colorVertex(buffer, matrix, x1, y1, z1, color);
        colorVertex(buffer, matrix, x2, y2, z2, color);
    }

    private static void drawTargetInfo(class_4587 matrices, net.minecraft.class_4184 camera, class_310 client, Marker current, class_2338 pos, boolean drawIcon, boolean drawLabel) {
        class_1297 player = client.field_1724;
        class_327 textRenderer = client.field_1772;
        if (player == null || textRenderer == null) {
            return;
        }

        class_243 cameraPos = camera.method_19326();
        double distance = distanceToEntity(player, pos);
        float fade = fade(distance);
        boolean pointedAt = isPointedAt(pos, distance, player, client.method_61966().method_60637(true));

        String title = current.name == null || current.name.isEmpty() ? "Placement" : current.name;
        String coordinate = String.format("[%d, %d, %d]",
                pos.method_10263(),
                pos.method_10264(),
                pos.method_10260());
        String distanceText = String.format("%dm", (int) distance);

        double baseX = pos.method_10263() + 0.5D - cameraPos.field_1352;
        double baseY = pos.method_10264() + 0.5D - cameraPos.field_1351;
        double baseZ = pos.method_10260() + 0.5D - cameraPos.field_1350;
        double maxDistance = client.field_1690.method_42503().method_41753();
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            baseX = baseX / distance * maxDistance;
            baseY = baseY / distance * maxDistance;
            baseZ = baseZ / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        float scale = (float) (adjustedDistance * 0.1F + 1.0F) * LABEL_SCALE_BASE;
        matrices.method_22903();
        matrices.method_22904(baseX, baseY, baseZ);
        matrices.method_22907(camera.method_23767());
        matrices.method_22905(scale, -scale, scale);

        Matrix4f matrix = matrices.method_23760().method_23761();
        if (drawIcon) {
            drawTargetTexture(new Matrix4f(matrix).translate(0.0F, 0.0F, TARGET_ICON_LAYER_Z), fade);
        }
        if (drawLabel && pointedAt) {
            matrices.method_22903();
            float textScale = originMarkerTextScale();
            matrices.method_22905(textScale, textScale, 1.0F);
            drawLabel(matrices.method_23760().method_23761(), textRenderer, title, coordinate, distanceText, fade);
            matrices.method_22909();
        }
        matrices.method_22909();
    }

    private static double distanceToEntity(class_1297 entity, class_2338 pos) {
        double dx = pos.method_10263() + 0.5D - entity.method_23317();
        double dy = pos.method_10264() + 0.5D - entity.method_23318();
        double dz = pos.method_10260() + 0.5D - entity.method_23321();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static boolean isPointedAt(class_2338 pos, double distance, class_1297 cameraEntity, float tickDelta) {
        class_243 cameraPos = cameraEntity.method_33571();
        double degrees = 5.0D + Math.min(5.0D / distance, 5.0D);
        double angle = degrees * 0.0174533D;
        double size = Math.sin(angle) * distance;
        class_243 direction = cameraEntity.method_5828(tickDelta);
        class_243 end = new class_243(
                cameraPos.field_1352 + direction.field_1352 * distance,
                cameraPos.field_1351 + direction.field_1351 * distance,
                cameraPos.field_1350 + direction.field_1350 * distance);
        class_238 box = new class_238(
                pos.method_10263() + 0.5D - size,
                pos.method_10264() + 0.5D - size,
                pos.method_10260() + 0.5D - size,
                pos.method_10263() + 0.5D + size,
                pos.method_10264() + 0.5D + size,
                pos.method_10260() + 0.5D + size);
        Optional<class_243> raycastResult = box.method_992(cameraPos, end);
        return box.method_1006(cameraPos) ? distance >= 1.0D : raycastResult.isPresent();
    }

    private static float fade(double distance) {
        float fade = distance < 5.0D ? 1.0F : (float) distance / 5.0F;
        return Math.min(fade, 1.0F);
    }

    private static float originMarkerTextScale() {
        int configuredScale = Configs.Generic.ORIGIN_MARKER_TEXT_SCALE.getIntegerValue();
        int clampedScale = Math.max(1, Math.min(5, configuredScale));
        return clampedScale / 3.0F;
    }

    private static void drawTargetTexture(Matrix4f matrix, float alpha) {
        class_289 tessellator = class_289.method_1348();
        beginOverlayState();
        RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, alpha);
        class_287 buffer = tessellator.method_60827(VertexFormat.class_5596.field_27382, class_290.field_1575);
        vertex(buffer, matrix, -TARGET_HALF_SIZE, -TARGET_HALF_SIZE, 0.0F, 0.0F, alpha);
        vertex(buffer, matrix, -TARGET_HALF_SIZE, TARGET_HALF_SIZE, 0.0F, 1.0F, alpha);
        vertex(buffer, matrix, TARGET_HALF_SIZE, TARGET_HALF_SIZE, 1.0F, 1.0F, alpha);
        vertex(buffer, matrix, TARGET_HALF_SIZE, -TARGET_HALF_SIZE, 1.0F, 0.0F, alpha);
        GpuTexture texture = class_310.method_1551().method_1531().method_4619(TARGET_TEXTURE).method_68004();
        drawBuiltBuffer(buffer.method_60794(), POSITION_TEX_COLOR_OVERLAY_PIPELINE, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawLabel(Matrix4f matrix, class_327 textRenderer, String title, String coordinate, String distanceText, float alpha) {
        int titleWidth = textRenderer.method_1727(title);
        int coordinateWidth = textRenderer.method_1727(coordinate);
        int distanceWidth = textRenderer.method_1727(distanceText);
        int halfWidth = Math.max(Math.max(titleWidth, coordinateWidth), distanceWidth) / 2;
        int x1 = -halfWidth - LABEL_PADDING_X;
        int x2 = halfWidth + LABEL_PADDING_X;
        int y1 = LABEL_ELEVATE_BY - 2;
        int y2 = LABEL_ELEVATE_BY + LABEL_LINE_HEIGHT * 3;

        Matrix4f backgroundMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, LABEL_BACKGROUND_LAYER_Z);
        Matrix4f textMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, LABEL_TEXT_LAYER_Z);

        beginOverlayState();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        class_289 tessellator = class_289.method_1348();
        class_287 buffer = tessellator.method_60827(VertexFormat.class_5596.field_27382, class_290.field_1576);
        rectangle(buffer, backgroundMatrix, x1, y1, x2, y2, 3.0F, 0.0F, 0.0F, 0.6F * alpha);
        drawBuiltBuffer(buffer.method_60794(), POSITION_COLOR_OVERLAY_PIPELINE, null);

        buffer = tessellator.method_60827(VertexFormat.class_5596.field_27382, class_290.field_1576);
        rectangle(buffer, backgroundMatrix, x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0.0F, 0.0F, 0.0F, 0.15F * alpha);
        drawBuiltBuffer(buffer.method_60794(), POSITION_COLOR_OVERLAY_PIPELINE, null);

        int color = (((int) (255.0F * alpha)) << 24) | (LABEL_TEXT_COLOR & 0x00FFFFFF);
        try (class_9799 textAllocator = new class_9799(786432)) {
            class_4597.class_4598 immediate = class_4597.method_22991(textAllocator);
            beginOverlayState();
            textRenderer.method_27522(class_2561.method_30163(title), -titleWidth / 2.0F, LABEL_ELEVATE_BY, color, false, textMatrix, immediate, class_327.class_6415.field_33994, 0, LABEL_LIGHT);
            textRenderer.method_27522(class_2561.method_30163(coordinate), -coordinateWidth / 2.0F, LABEL_ELEVATE_BY + LABEL_LINE_HEIGHT, color, false, textMatrix, immediate, class_327.class_6415.field_33994, 0, LABEL_LIGHT);
            textRenderer.method_27522(class_2561.method_30163(distanceText), -distanceWidth / 2.0F, LABEL_ELEVATE_BY + LABEL_LINE_HEIGHT * 2, color, false, textMatrix, immediate, class_327.class_6415.field_33994, 0, LABEL_LIGHT);
            immediate.method_22993();
        }
        beginOverlayState();
    }

    private static void drawBuiltBuffer(class_9801 builtBuffer, RenderPipeline pipeline, GpuTexture texture) {
        if (builtBuffer == null) {
            return;
        }

        try (builtBuffer) {
            GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(builtBuffer.method_60818());
            GpuBuffer indexBuffer;
            VertexFormat.class_5595 indexType;
            if (builtBuffer.method_60821() == null) {
                RenderSystem.class_5590 sequentialBuffer = RenderSystem.getSequentialBuffer(builtBuffer.method_60822().comp_752());
                indexBuffer = sequentialBuffer.method_68274(builtBuffer.method_60822().comp_751());
                indexType = sequentialBuffer.method_31924();
            } else {
                indexBuffer = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.method_60821());
                indexType = builtBuffer.method_60822().comp_753();
            }

            class_276 framebuffer = class_310.method_1551().method_1522();
            try (RenderPass pass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(framebuffer.method_30277(), OptionalInt.empty())) {
                pass.setPipeline(pipeline);
                if (texture != null) {
                    pass.bindSampler("Sampler0", texture);
                }
                pass.setVertexBuffer(0, vertexBuffer);
                pass.setIndexBuffer(indexBuffer, indexType);
                pass.drawIndexed(0, builtBuffer.method_60822().comp_751());
            }
        }
    }

    private static void rectangle(class_287 buffer, Matrix4f matrix, int x1, int y1, int x2, int y2, float r, float g, float b, float a) {
        colorVertex(buffer, matrix, x1, y1, r, g, b, a);
        colorVertex(buffer, matrix, x1, y2, r, g, b, a);
        colorVertex(buffer, matrix, x2, y2, r, g, b, a);
        colorVertex(buffer, matrix, x2, y1, r, g, b, a);
    }

    private static void colorVertex(class_287 buffer, Matrix4f matrix, float x, float y, float r, float g, float b, float a) {
        buffer.method_22918(matrix, x, y, 0.0F).method_22915(r, g, b, a);
    }

    private static void colorVertex(class_287 buffer, Matrix4f matrix, float x, float y, float z, Color4f color) {
        buffer.method_22918(matrix, x, y, z).method_22915(color.r, color.g, color.b, color.a);
    }

    private static void vertex(class_287 buffer, Matrix4f matrix, float x, float y, float u, float v, float alpha) {
        buffer.method_22918(matrix, x, y, 0.0F).method_22913(u, v).method_22915(1.0F, 0.0F, 0.0F, alpha);
    }

    private static boolean hasValidOrigin(KnownPlacementContext context) {
        return context != null && parseOrigin(context.originPosition()) != null;
    }

    private static class_2338 parseOrigin(String origin) {
        if (origin == null || origin.isEmpty()) {
            return null;
        }

        Matcher matcher = ORIGIN_PATTERN.matcher(origin);
        if (!matcher.matches()) {
            return null;
        }

        try {
            return new class_2338(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isCurrentDimension(String sourceDimension) {
        return isSameDimension(sourceDimension, currentDimensionId(class_310.method_1551().field_1687));
    }

    private static boolean isSameDimension(String sourceDimension, String currentDimension) {
        String source = KnownPlacementRows.normalizedDimension(sourceDimension);
        String current = KnownPlacementRows.normalizedDimension(currentDimension);
        return !"unknown".equals(source) && source.equals(current);
    }

    private static RenderTarget resolveRenderTarget(String sourceDimension, class_2338 sourcePos, String currentDimension) {
        String source = KnownPlacementRows.normalizedDimension(sourceDimension);
        String current = KnownPlacementRows.normalizedDimension(currentDimension);
        if ("unknown".equals(source) || "unknown".equals(current)) {
            return null;
        }

        if (source.equals(current)) {
            return new RenderTarget(sourcePos, true);
        }

        if (OVERWORLD.equals(source) && NETHER.equals(current)) {
            return new RenderTarget(new class_2338(
                    Math.floorDiv(sourcePos.method_10263(), 8),
                    sourcePos.method_10264(),
                    Math.floorDiv(sourcePos.method_10260(), 8)), false);
        }

        if (NETHER.equals(source) && OVERWORLD.equals(current)) {
            return new RenderTarget(new class_2338(
                    multiplyCoordinate(sourcePos.method_10263(), 8),
                    sourcePos.method_10264(),
                    multiplyCoordinate(sourcePos.method_10260(), 8)), false);
        }

        return null;
    }

    private static int multiplyCoordinate(int value, int scale) {
        long result = (long) value * scale;
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (result < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }

        return (int) result;
    }

    private static String currentDimensionId(class_638 world) {
        return world == null ? null : world.method_27983().method_29177().toString();
    }

    private record RenderTarget(class_2338 pos, boolean sameDimension) {
    }

    private record Marker(
            String key,
            String name,
            String sourceDimension,
            class_2338 sourcePos,
            long expiresAt) {
        private static Marker show(String key, String name, String sourceDimension, class_2338 sourcePos, long now) {
            return new Marker(
                    key,
                    name,
                    sourceDimension,
                    sourcePos,
                    now + Configs.Generic.ORIGIN_MARKER_TIME.getIntegerValue() * 1000L);
        }

        private boolean active(long now) {
            return this.expiresAt >= now;
        }
    }
}
