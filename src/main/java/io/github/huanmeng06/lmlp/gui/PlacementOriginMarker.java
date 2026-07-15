package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.config.Configs;
import net.minecraft.class_238;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_3532;
import net.minecraft.class_638;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_7833;
import net.minecraft.class_9848;
import net.minecraft.class_11661;
import net.minecraft.class_11682;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final class_11661 BEAM_COMMANDS = new class_11661();
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

    public static void render(net.minecraft.class_4184 camera) {
        Marker current = activeMarker();
        class_310 client = class_310.method_1551();
        if (current == null || camera == null || client.field_1687 == null) {
            return;
        }

        RenderTarget target = resolveRenderTarget(current.sourceDimension, current.sourcePos, currentDimensionId(client.field_1687));
        if (target == null) {
            return;
        }

        if (client.field_1724 != null && client.field_1724.method_5707(target.pos.method_46558()) <= ARRIVAL_DISTANCE_SQUARED) {
            clear();
            return;
        }

        class_243 cameraPos = camera.method_71156();
        drawBeam(client.field_1687, target.pos, cameraPos);
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

    private static void drawBeam(class_638 world, class_2338 pos, class_243 cameraPos) {
        class_4587 matrices = new class_4587();
        matrices.method_22904(
                pos.method_10263() + 0.5D - cameraPos.field_1352,
                pos.method_10264() + 0.5D - cameraPos.field_1351,
                pos.method_10260() + 0.5D - cameraPos.field_1350);
        matrices.method_22904(-0.5D, -0.5D, -0.5D);

        float tickDelta = class_310.method_1551().method_61966().method_60637(true);
        float animationTime = Math.floorMod(world.method_8510(), 40L) + tickDelta;
        renderOmmcBeam(
                matrices,
                animationTime,
                -pos.method_10264() - 64,
                BEAM_HEIGHT,
                BEAM_COLOR,
                BEAM_INNER_RADIUS,
                BEAM_OUTER_RADIUS);

        class_4597.class_4598 consumers = class_310.method_1551().method_22940().method_23000();
        new class_11682().method_72999(BEAM_COMMANDS.method_73531(0), consumers);
        BEAM_COMMANDS.method_72954();
        consumers.method_22993();
    }

    private static void renderOmmcBeam(class_4587 matrices,
                                       float animationTime,
                                       int yOffset,
                                       int height,
                                       int color,
                                       float innerRadius,
                                       float outerRadius) {
        int maxY = yOffset + height;
        matrices.method_22903();
        matrices.method_22904(0.5D, 0.0D, 0.5D);
        float direction = height < 0 ? animationTime : -animationTime;
        float scroll = class_3532.method_22450(direction * 0.2F - class_3532.method_15375(direction * 0.1F));

        matrices.method_22903();
        matrices.method_22907(class_7833.field_40716.rotationDegrees(animationTime * 2.25F - 45.0F));
        float innerV1 = -1.0F + scroll;
        float innerV2 = height * (0.5F / innerRadius) + innerV1;
        BEAM_COMMANDS.method_73483(
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
        matrices.method_22909();

        float outerV1 = -1.0F + scroll;
        float outerV2 = height + outerV1;
        int translucentColor = class_9848.method_61330(32, color);
        BEAM_COMMANDS.method_73483(
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
        matrices.method_22909();
    }

    private static void drawBeamFaces(class_4587.class_4665 entry,
                                      class_4588 buffer,
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

    private static void drawBeamFace(class_4587.class_4665 entry,
                                     class_4588 buffer,
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

    private static void beamVertex(class_4587.class_4665 entry,
                                   class_4588 buffer,
                                   int color,
                                   int y,
                                   float x, float z,
                                   float u, float v) {
        buffer.method_56824(entry, x, y, z)
                .method_39415(color)
                .method_22913(u, v)
                .method_22922(class_4608.field_21444)
                .method_60803(LABEL_LIGHT)
                .method_60831(entry, 0.0F, 1.0F, 0.0F);
    }

    private static void drawTargetInfo(net.minecraft.class_4184 camera,
                                       class_310 client,
                                       Marker current,
                                       class_2338 pos,
                                       class_243 cameraPos) {
        class_327 textRenderer = client.field_1772;
        if (textRenderer == null) {
            return;
        }

        double distance = distanceToCamera(cameraPos, pos);
        if (distance <= 2.0D) {
            return;
        }

        double baseX = pos.method_10263() + 0.5D - cameraPos.field_1352;
        double baseY = pos.method_10264() + 0.5D - cameraPos.field_1351;
        double baseZ = pos.method_10260() + 0.5D - cameraPos.field_1350;
        double maxDistance = client.field_1690.method_42503().method_41753() * 16.0D;
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            baseX = baseX / distance * maxDistance;
            baseY = baseY / distance * maxDistance;
            baseZ = baseZ / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        class_4587 matrices = new class_4587();
        matrices.method_22904(baseX, baseY, baseZ);
        matrices.method_34425(new Matrix4f().rotation(camera.method_23767()));
        float scale = (float) ((adjustedDistance > 8.0D ? adjustedDistance - 8.0D : 0.0D) * 0.2D + 1.0D) * LABEL_SCALE_BASE;
        matrices.method_22905(scale, -scale, -scale);

        float alpha = fade(distance);
        drawTargetTexture(matrices.method_23760().method_23761(), alpha);

        if (isPointedAt(pos, cameraPos, distance, camera.method_19335())) {
            matrices.method_22903();
            float configuredScale = originMarkerTextScale();
            matrices.method_22905(configuredScale, configuredScale, 1.0F);

            String title = current.name == null || current.name.isEmpty() ? "Placement" : current.name;
            String coordinate = String.format("[%d, %d, %d]",
                    pos.method_10263(),
                    pos.method_10264(),
                    pos.method_10260());
            String distanceText = String.format("%dm", (int) distance);
            drawLabel(matrices.method_23760().method_23761(), textRenderer, title, coordinate, distanceText, alpha);
            matrices.method_22909();
        }
    }

    private static void drawTargetTexture(Matrix4f matrix, float alpha) {
        class_4597.class_4598 consumers = class_310.method_1551().method_22940().method_23000();
        class_4588 buffer = consumers.method_73477(OriginMarkerRenderLayers.ICON);
        iconVertex(buffer, matrix, -TARGET_HALF_SIZE, -TARGET_HALF_SIZE, 0.0F, 0.0F, alpha);
        iconVertex(buffer, matrix, -TARGET_HALF_SIZE, TARGET_HALF_SIZE, 0.0F, 1.0F, alpha);
        iconVertex(buffer, matrix, TARGET_HALF_SIZE, TARGET_HALF_SIZE, 1.0F, 1.0F, alpha);
        iconVertex(buffer, matrix, TARGET_HALF_SIZE, -TARGET_HALF_SIZE, 1.0F, 0.0F, alpha);
        consumers.method_22993();
    }

    private static void iconVertex(class_4588 buffer,
                                   Matrix4f matrix,
                                   float x, float y,
                                   float u, float v,
                                   float alpha) {
        buffer.method_22918(matrix, x, y, 0.0F)
                .method_22913(u, v)
                .method_22915(1.0F, 0.0F, 0.0F, alpha);
    }

    private static void drawLabel(Matrix4f matrix,
                                  class_327 textRenderer,
                                  String title,
                                  String coordinate,
                                  String distanceText,
                                  float alpha) {
        int titleWidth = textRenderer.method_1727(title);
        int coordinateWidth = textRenderer.method_1727(coordinate);
        int distanceWidth = textRenderer.method_1727(distanceText);
        int halfWidth = Math.max(Math.max(titleWidth, coordinateWidth), distanceWidth) / 2;
        int x1 = -halfWidth - LABEL_PADDING_X;
        int x2 = halfWidth + LABEL_PADDING_X;
        int y1 = LABEL_ELEVATE_BY - 2;
        int y2 = LABEL_ELEVATE_BY + LABEL_LINE_HEIGHT * 3;

        class_4597.class_4598 consumers = class_310.method_1551().method_22940().method_23000();
        class_4588 background = consumers.method_73477(OriginMarkerRenderLayers.FILL);
        Matrix4f backgroundMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, 0.02F);
        rectangle(background, backgroundMatrix, x1, y1, x2, y2, 1.0F, 0.0F, 0.0F, 0.6F * alpha);
        rectangle(background, new Matrix4f(backgroundMatrix).translate(0.0F, 0.0F, 0.005F),
                x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0.0F, 0.0F, 0.0F, 0.15F * alpha);
        consumers.method_22993();

        Matrix4f textMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, 0.03F);
        int color = (((int) (255.0F * alpha)) << 24) | (LABEL_TEXT_COLOR & 0x00FFFFFF);
        drawTextLine(textRenderer, consumers, textMatrix, title, -titleWidth / 2.0F, LABEL_ELEVATE_BY, color);
        drawTextLine(textRenderer, consumers, textMatrix, coordinate, -coordinateWidth / 2.0F, LABEL_ELEVATE_BY + LABEL_LINE_HEIGHT, color);
        drawTextLine(textRenderer, consumers, textMatrix, distanceText, -distanceWidth / 2.0F, LABEL_ELEVATE_BY + LABEL_LINE_HEIGHT * 2, color);
        consumers.method_22993();
    }

    private static void drawTextLine(class_327 textRenderer,
                                     class_4597.class_4598 consumers,
                                     Matrix4f matrix,
                                     String text,
                                     float x,
                                     int y,
                                     int color) {
        textRenderer.method_27521(
                text,
                x,
                y,
                color,
                false,
                matrix,
                consumers,
                class_327.class_6415.field_33994,
                0,
                LABEL_LIGHT);
    }

    private static void rectangle(class_4588 buffer,
                                  Matrix4f matrix,
                                  int x1, int y1, int x2, int y2,
                                  float r, float g, float b, float a) {
        buffer.method_22918(matrix, x1, y1, 0.0F).method_22915(r, g, b, a);
        buffer.method_22918(matrix, x1, y2, 0.0F).method_22915(r, g, b, a);
        buffer.method_22918(matrix, x2, y2, 0.0F).method_22915(r, g, b, a);
        buffer.method_22918(matrix, x2, y1, 0.0F).method_22915(r, g, b, a);
    }

    private static double distanceToCamera(class_243 cameraPos, class_2338 pos) {
        double dx = pos.method_10263() + 0.5D - cameraPos.field_1352;
        double dy = pos.method_10264() + 0.5D - cameraPos.field_1351;
        double dz = pos.method_10260() + 0.5D - cameraPos.field_1350;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static boolean isPointedAt(class_2338 pos, class_243 cameraPos, double distance, Vector3fc cameraDirection) {
        if (cameraDirection == null || distance <= 0.0D) {
            return false;
        }
        double degrees = 5.0D + Math.min(5.0D / distance, 5.0D);
        double angle = Math.toRadians(degrees);
        double size = Math.sin(angle) * distance;
        class_243 end = new class_243(
                cameraPos.field_1352 + cameraDirection.x() * distance,
                cameraPos.field_1351 + cameraDirection.y() * distance,
                cameraPos.field_1350 + cameraDirection.z() * distance);
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

    static float originMarkerTextScale() {
        int configuredScale = Configs.Generic.ORIGIN_MARKER_TEXT_SCALE.getIntegerValue();
        int clampedScale = Math.max(1, Math.min(5, configuredScale));
        return clampedScale / 3.0F;
    }

    private static boolean hasValidOrigin(KnownPlacementContext context) {
        return context != null && parseOrigin(context.originPosition()) != null;
    }

    static class_2338 parseOrigin(String origin) {
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
