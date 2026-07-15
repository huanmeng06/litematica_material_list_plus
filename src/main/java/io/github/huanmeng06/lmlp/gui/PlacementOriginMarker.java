package io.github.huanmeng06.lmlp.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.config.Configs;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PlacementOriginMarker {
    public static final int ORIGIN_TEXT_COLOR = 0xFF55FF55;

    private static final Pattern ORIGIN_PATTERN = Pattern.compile("^\\s*\\[?\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*]?\\s*$");
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";
    private static final double ARRIVAL_DISTANCE_SQUARED = 4.0D;
    private static final int BEAM_COLOR = 0xFF0000;
    private static final int BEAM_HEIGHT = 2048;
    private static final float BEAM_INNER_RADIUS = 0.2F;
    private static final float BEAM_OUTER_RADIUS = 0.25F;
    private static final SubmitNodeStorage BEAM_COMMANDS = new SubmitNodeStorage();
    private static final float LABEL_SCALE_BASE = 0.0265F;
    private static final int TARGET_HALF_SIZE = 10;
    static final int LABEL_ELEVATE_BY = -28;
    static final int LABEL_PADDING_X = 2;
    static final int LABEL_LINE_HEIGHT = 9;
    static final int LABEL_TEXT_COLOR = 0xFFCCCCCC;
    private static final int LABEL_LIGHT = 0x00F000F0;

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

        BlockPos sourcePos = parseOrigin(context.originPosition());
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

    public static void render(net.minecraft.client.Camera camera) {
        Marker current = activeMarker();
        Minecraft client = Minecraft.getInstance();
        if (current == null || camera == null || client.level == null) {
            return;
        }

        RenderTarget target = resolveRenderTarget(current.sourceDimension, current.sourcePos, currentDimensionId(client.level));
        if (target == null) {
            return;
        }

        if (client.player != null && client.player.distanceToSqr(target.pos.getCenter()) <= ARRIVAL_DISTANCE_SQUARED) {
            clear();
            return;
        }

        Vec3 cameraPos = camera.position();
        drawBeam(client.level, target.pos, cameraPos);
        if (target.sameDimension) {
            drawTargetInfo(camera, client, current, target.pos, cameraPos);
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

    private static void drawBeam(ClientLevel world, BlockPos pos, Vec3 cameraPos) {
        PoseStack matrices = new PoseStack();
        matrices.translate(
                pos.getX() + 0.5D - cameraPos.x,
                pos.getY() + 0.5D - cameraPos.y,
                pos.getZ() + 0.5D - cameraPos.z);
        matrices.translate(-0.5D, -0.5D, -0.5D);

        float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float animationTime = Math.floorMod(world.getGameTime(), 40L) + tickDelta;
        renderOmmcBeam(
                matrices,
                animationTime,
                -pos.getY() - 64,
                BEAM_HEIGHT,
                BEAM_COLOR,
                BEAM_INNER_RADIUS,
                BEAM_OUTER_RADIUS);

        MultiBufferSource.BufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();
        CustomFeatureRenderer renderer = new CustomFeatureRenderer();
        renderer.renderSolid(BEAM_COMMANDS.order(0), consumers);
        renderer.renderTranslucent(BEAM_COMMANDS.order(0), consumers);
        BEAM_COMMANDS.endFrame();
        consumers.endBatch();
    }

    private static void renderOmmcBeam(PoseStack matrices,
                                       float animationTime,
                                       int yOffset,
                                       int height,
                                       int color,
                                       float innerRadius,
                                       float outerRadius) {
        int maxY = yOffset + height;
        matrices.pushPose();
        matrices.translate(0.5D, 0.0D, 0.5D);
        float direction = height < 0 ? animationTime : -animationTime;
        float scroll = Mth.frac(direction * 0.2F - Mth.floor(direction * 0.1F));

        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotationDegrees(animationTime * 2.25F - 45.0F));
        float innerV1 = -1.0F + scroll;
        float innerV2 = height * (0.5F / innerRadius) + innerV1;
        BEAM_COMMANDS.submitCustomGeometry(
                matrices,
                OriginMarkerRenderLayers.BEAM_OPAQUE,
                (entry, buffer) -> drawBeamFaces(
                        entry,
                        buffer,
                        color,
                        yOffset,
                        maxY,
                        0.0F, innerRadius,
                        innerRadius, 0.0F,
                        -innerRadius, 0.0F,
                        0.0F, -innerRadius,
                        0.0F, 1.0F,
                        innerV2, innerV1));
        matrices.popPose();

        float outerV1 = -1.0F + scroll;
        float outerV2 = height + outerV1;
        int translucentColor = ARGB.color(32, color);
        BEAM_COMMANDS.submitCustomGeometry(
                matrices,
                OriginMarkerRenderLayers.BEAM_TRANSLUCENT,
                (entry, buffer) -> drawBeamFaces(
                        entry,
                        buffer,
                        translucentColor,
                        yOffset,
                        maxY,
                        -outerRadius, -outerRadius,
                        outerRadius, -outerRadius,
                        -outerRadius, outerRadius,
                        outerRadius, outerRadius,
                        0.0F, 1.0F,
                        outerV2, outerV1));
        matrices.popPose();
    }

    private static void drawBeamFaces(PoseStack.Pose entry,
                                      VertexConsumer buffer,
                                      int color,
                                      int minY,
                                      int maxY,
                                      float x1, float z1,
                                      float x2, float z2,
                                      float x3, float z3,
                                      float x4, float z4,
                                      float minU, float maxU,
                                      float maxV, float minV) {
        drawBeamFace(entry, buffer, color, minY, maxY, x1, z1, x2, z2, minU, maxU, maxV, minV);
        drawBeamFace(entry, buffer, color, minY, maxY, x4, z4, x3, z3, minU, maxU, maxV, minV);
        drawBeamFace(entry, buffer, color, minY, maxY, x2, z2, x4, z4, minU, maxU, maxV, minV);
        drawBeamFace(entry, buffer, color, minY, maxY, x3, z3, x1, z1, minU, maxU, maxV, minV);
    }

    private static void drawBeamFace(PoseStack.Pose entry,
                                     VertexConsumer buffer,
                                     int color,
                                     int minY,
                                     int maxY,
                                     float x1, float z1,
                                     float x2, float z2,
                                     float minU, float maxU,
                                     float maxV, float minV) {
        beamVertex(entry, buffer, color, maxY, x1, z1, maxU, maxV);
        beamVertex(entry, buffer, color, minY, x1, z1, maxU, minV);
        beamVertex(entry, buffer, color, minY, x2, z2, minU, minV);
        beamVertex(entry, buffer, color, maxY, x2, z2, minU, maxV);
    }

    private static void beamVertex(PoseStack.Pose entry,
                                   VertexConsumer buffer,
                                   int color,
                                   int y,
                                   float x, float z,
                                   float u, float v) {
        buffer.addVertex(entry, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LABEL_LIGHT)
                .setNormal(entry, 0.0F, 1.0F, 0.0F);
    }

    private static void drawTargetInfo(net.minecraft.client.Camera camera,
                                       Minecraft client,
                                       Marker current,
                                       BlockPos pos,
                                       Vec3 cameraPos) {
        Font textRenderer = client.font;
        if (textRenderer == null) {
            return;
        }

        double distance = distanceToCamera(cameraPos, pos);
        if (distance <= 2.0D) {
            return;
        }

        double baseX = pos.getX() + 0.5D - cameraPos.x;
        double baseY = pos.getY() + 0.5D - cameraPos.y;
        double baseZ = pos.getZ() + 0.5D - cameraPos.z;
        double maxDistance = client.options.renderDistance().get() * 16.0D;
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            baseX = baseX / distance * maxDistance;
            baseY = baseY / distance * maxDistance;
            baseZ = baseZ / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        PoseStack matrices = new PoseStack();
        matrices.translate(baseX, baseY, baseZ);
        matrices.mulPose(new Matrix4f().rotation(camera.rotation()));
        float scale = (float) ((adjustedDistance > 8.0D ? adjustedDistance - 8.0D : 0.0D) * 0.2D + 1.0D) * LABEL_SCALE_BASE;
        matrices.scale(scale, -scale, -scale);

        float alpha = fade(distance);
        drawTargetTexture(matrices.last().pose(), alpha);

        if (isPointedAt(pos, cameraPos, distance, camera.forwardVector())) {
            matrices.pushPose();
            float configuredScale = originMarkerTextScale();
            matrices.scale(configuredScale, configuredScale, 1.0F);

            String title = current.name == null || current.name.isEmpty() ? "Placement" : current.name;
            String coordinate = String.format("[%d, %d, %d]",
                    pos.getX(),
                    pos.getY(),
                    pos.getZ());
            String distanceText = String.format("%dm", (int) distance);
            drawLabel(matrices.last().pose(), textRenderer, title, coordinate, distanceText, alpha);
            matrices.popPose();
        }
    }

    private static void drawTargetTexture(Matrix4f matrix, float alpha) {
        MultiBufferSource.BufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = consumers.getBuffer(OriginMarkerRenderLayers.ICON);
        iconVertex(buffer, matrix, -TARGET_HALF_SIZE, -TARGET_HALF_SIZE, 0.0F, 0.0F, alpha);
        iconVertex(buffer, matrix, -TARGET_HALF_SIZE, TARGET_HALF_SIZE, 0.0F, 1.0F, alpha);
        iconVertex(buffer, matrix, TARGET_HALF_SIZE, TARGET_HALF_SIZE, 1.0F, 1.0F, alpha);
        iconVertex(buffer, matrix, TARGET_HALF_SIZE, -TARGET_HALF_SIZE, 1.0F, 0.0F, alpha);
        consumers.endBatch();
    }

    private static void iconVertex(VertexConsumer buffer,
                                   Matrix4f matrix,
                                   float x, float y,
                                   float u, float v,
                                   float alpha) {
        buffer.addVertex(matrix, x, y, 0.0F)
                .setUv(u, v)
                .setColor(1.0F, 0.0F, 0.0F, alpha);
    }

    private static void drawLabel(Matrix4f matrix,
                                  Font textRenderer,
                                  String title,
                                  String coordinate,
                                  String distanceText,
                                  float alpha) {
        int titleWidth = textRenderer.width(title);
        int coordinateWidth = textRenderer.width(coordinate);
        int distanceWidth = textRenderer.width(distanceText);
        int halfWidth = Math.max(Math.max(titleWidth, coordinateWidth), distanceWidth) / 2;
        int x1 = -halfWidth - LABEL_PADDING_X;
        int x2 = halfWidth + LABEL_PADDING_X;
        int y1 = LABEL_ELEVATE_BY - 2;
        int y2 = LABEL_ELEVATE_BY + LABEL_LINE_HEIGHT * 3;

        MultiBufferSource.BufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer background = consumers.getBuffer(OriginMarkerRenderLayers.FILL);
        Matrix4f backgroundMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, 0.02F);
        rectangle(background, backgroundMatrix, x1, y1, x2, y2, 1.0F, 0.0F, 0.0F, 0.6F * alpha);
        rectangle(background, new Matrix4f(backgroundMatrix).translate(0.0F, 0.0F, 0.005F),
                x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0.0F, 0.0F, 0.0F, 0.15F * alpha);
        consumers.endBatch();

        Matrix4f textMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, 0.03F);
        int color = (((int) (255.0F * alpha)) << 24) | (LABEL_TEXT_COLOR & 0x00FFFFFF);
        drawTextLine(textRenderer, consumers, textMatrix, title, -titleWidth / 2.0F, LABEL_ELEVATE_BY, color);
        drawTextLine(textRenderer, consumers, textMatrix, coordinate, -coordinateWidth / 2.0F, LABEL_ELEVATE_BY + LABEL_LINE_HEIGHT, color);
        drawTextLine(textRenderer, consumers, textMatrix, distanceText, -distanceWidth / 2.0F, LABEL_ELEVATE_BY + LABEL_LINE_HEIGHT * 2, color);
        consumers.endBatch();
    }

    private static void drawTextLine(Font textRenderer,
                                     MultiBufferSource.BufferSource consumers,
                                     Matrix4f matrix,
                                     String text,
                                     float x,
                                     int y,
                                     int color) {
        textRenderer.drawInBatch(
                text,
                x,
                y,
                color,
                false,
                matrix,
                consumers,
                Font.DisplayMode.SEE_THROUGH,
                0,
                LABEL_LIGHT);
    }

    private static void rectangle(VertexConsumer buffer,
                                  Matrix4f matrix,
                                  int x1, int y1, int x2, int y2,
                                  float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, 0.0F).setColor(r, g, b, a);
        buffer.addVertex(matrix, x1, y2, 0.0F).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, 0.0F).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y1, 0.0F).setColor(r, g, b, a);
    }

    private static double distanceToCamera(Vec3 cameraPos, BlockPos pos) {
        double dx = pos.getX() + 0.5D - cameraPos.x;
        double dy = pos.getY() + 0.5D - cameraPos.y;
        double dz = pos.getZ() + 0.5D - cameraPos.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static boolean isPointedAt(BlockPos pos, Vec3 cameraPos, double distance, Vector3fc cameraDirection) {
        if (cameraDirection == null || distance <= 0.0D) {
            return false;
        }
        double degrees = 5.0D + Math.min(5.0D / distance, 5.0D);
        double angle = Math.toRadians(degrees);
        double size = Math.sin(angle) * distance;
        Vec3 end = new Vec3(
                cameraPos.x + cameraDirection.x() * distance,
                cameraPos.y + cameraDirection.y() * distance,
                cameraPos.z + cameraDirection.z() * distance);
        AABB box = new AABB(
                pos.getX() + 0.5D - size,
                pos.getY() + 0.5D - size,
                pos.getZ() + 0.5D - size,
                pos.getX() + 0.5D + size,
                pos.getY() + 0.5D + size,
                pos.getZ() + 0.5D + size);
        Optional<Vec3> raycastResult = box.clip(cameraPos, end);
        return box.contains(cameraPos) ? distance >= 1.0D : raycastResult.isPresent();
    }

    private static float fade(double distance) {
        float fade = distance < 5.0D ? 1.0F : (float) distance / 5.0F;
        return Math.min(fade, 1.0F);
    }

    static float originMarkerTextScale() {
        int configuredScale = Configs.Generic.ORIGIN_MARKER_TEXT_SCALE.getIntegerValue();
        int clampedScale = Math.max(1, Math.min(5, configuredScale));
        return clampedScale / 3.0F;
    }

    private static boolean hasValidOrigin(KnownPlacementContext context) {
        return context != null && parseOrigin(context.originPosition()) != null;
    }

    static BlockPos parseOrigin(String origin) {
        if (origin == null || origin.isEmpty()) {
            return null;
        }

        Matcher matcher = ORIGIN_PATTERN.matcher(origin);
        if (!matcher.matches()) {
            return null;
        }

        try {
            return new BlockPos(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isCurrentDimension(String sourceDimension) {
        return isSameDimension(sourceDimension, currentDimensionId(Minecraft.getInstance().level));
    }

    private static boolean isSameDimension(String sourceDimension, String currentDimension) {
        String source = KnownPlacementRows.normalizedDimension(sourceDimension);
        String current = KnownPlacementRows.normalizedDimension(currentDimension);
        return !"unknown".equals(source) && source.equals(current);
    }

    private static RenderTarget resolveRenderTarget(String sourceDimension, BlockPos sourcePos, String currentDimension) {
        String source = KnownPlacementRows.normalizedDimension(sourceDimension);
        String current = KnownPlacementRows.normalizedDimension(currentDimension);
        if ("unknown".equals(source) || "unknown".equals(current)) {
            return null;
        }

        if (source.equals(current)) {
            return new RenderTarget(sourcePos, true);
        }

        if (OVERWORLD.equals(source) && NETHER.equals(current)) {
            return new RenderTarget(new BlockPos(
                    Math.floorDiv(sourcePos.getX(), 8),
                    sourcePos.getY(),
                    Math.floorDiv(sourcePos.getZ(), 8)), false);
        }

        if (NETHER.equals(source) && OVERWORLD.equals(current)) {
            return new RenderTarget(new BlockPos(
                    multiplyCoordinate(sourcePos.getX(), 8),
                    sourcePos.getY(),
                    multiplyCoordinate(sourcePos.getZ(), 8)), false);
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

    private static String currentDimensionId(ClientLevel world) {
        return world == null ? null : world.dimension().identifier().toString();
    }

    private record RenderTarget(BlockPos pos, boolean sameDimension) {
    }

    private record Marker(
            String key,
            String name,
            String sourceDimension,
            BlockPos sourcePos,
            long expiresAt) {
        private static Marker show(String key, String name, String sourceDimension, BlockPos sourcePos, long now) {
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
