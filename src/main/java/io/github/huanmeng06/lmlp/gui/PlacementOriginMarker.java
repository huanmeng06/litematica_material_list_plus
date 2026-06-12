package io.github.huanmeng06.lmlp.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.config.Configs;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.class_1297;
import net.minecraft.class_238;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_638;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_757;
import org.joml.Matrix4f;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlacementOriginMarker {
    public static final int ORIGIN_TEXT_COLOR = 0xFF55FF55;

    private static final class_2960 TARGET_TEXTURE = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/origin_target.png");
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
    private static final int LABEL_ELEVATE_BY = -19;
    private static final int LABEL_PADDING_X = 2;
    private static final int LABEL_LINE_HEIGHT = 9;
    private static final int LABEL_TEXT_COLOR = 0xFFCCCCCC;
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
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(class_757::method_34540);

        drawBeam(target.pos, cameraPos);
        if (target.sameDimension && client.field_1724 != null) {
            drawTargetInfo(context.matrixStack(), context.camera(), client, current, target.pos);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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

    private static void drawBeam(class_2338 pos, class_243 cameraPos) {
        double centerX = pos.method_10263() + 0.5D - cameraPos.field_1352;
        double centerZ = pos.method_10260() + 0.5D - cameraPos.field_1350;
        double minX = centerX - BEAM_RADIUS;
        double maxX = centerX + BEAM_RADIUS;
        double minZ = centerZ - BEAM_RADIUS;
        double maxZ = centerZ + BEAM_RADIUS;
        double minY = pos.method_10264() - BEAM_HALF_HEIGHT - cameraPos.field_1351;
        double maxY = pos.method_10264() + BEAM_HALF_HEIGHT - cameraPos.field_1351;

        class_289 tessellator = class_289.method_1348();
        class_287 buffer = tessellator.method_1349();
        buffer.method_1328(class_293.class_5596.field_27382, class_290.field_1576);
        RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, BEAM_FILL, buffer);
        tessellator.method_1350();

        RenderSystem.lineWidth(1.6F);
        buffer.method_1328(class_293.class_5596.field_29345, class_290.field_1576);
        RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, BEAM_OUTLINE, buffer);
        tessellator.method_1350();
    }

    private static void drawTargetInfo(class_4587 matrices, net.minecraft.class_4184 camera, class_310 client, Marker current, class_2338 pos) {
        class_1297 player = client.field_1724;
        class_327 textRenderer = client.field_1772;
        if (player == null || textRenderer == null) {
            return;
        }

        class_243 cameraPos = camera.method_19326();
        double distance = distanceToEntity(player, pos);
        float fade = fade(distance);
        boolean pointedAt = isPointedAt(pos, distance, player, client.method_1488());

        String title = current.name == null || current.name.isEmpty() ? "Placement" : current.name;
        String coordinate = String.format("[%d, %d, %d] (%dm)",
                pos.method_10263(),
                pos.method_10264(),
                pos.method_10260(),
                (int) distance);

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
        matrices.method_22905(-scale, -scale, scale);

        Matrix4f matrix = matrices.method_23760().method_23761();
        drawTargetTexture(matrix, fade);
        if (pointedAt) {
            drawLabel(matrix, textRenderer, title, coordinate, fade);
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

    private static void drawTargetTexture(Matrix4f matrix, float alpha) {
        class_289 tessellator = class_289.method_1348();
        class_287 buffer = tessellator.method_1349();
        RenderSystem.setShader(class_757::method_34542);
        RenderSystem.setShaderTexture(0, TARGET_TEXTURE);
        buffer.method_1328(class_293.class_5596.field_27382, class_290.field_1575);
        vertex(buffer, matrix, -TARGET_HALF_SIZE, -TARGET_HALF_SIZE, 0.0F, 0.0F, alpha);
        vertex(buffer, matrix, -TARGET_HALF_SIZE, TARGET_HALF_SIZE, 0.0F, 1.0F, alpha);
        vertex(buffer, matrix, TARGET_HALF_SIZE, TARGET_HALF_SIZE, 1.0F, 1.0F, alpha);
        vertex(buffer, matrix, TARGET_HALF_SIZE, -TARGET_HALF_SIZE, 1.0F, 0.0F, alpha);
        tessellator.method_1350();
    }

    private static void drawLabel(Matrix4f matrix, class_327 textRenderer, String title, String coordinate, float alpha) {
        int titleWidth = textRenderer.method_1727(title);
        int coordinateWidth = textRenderer.method_1727(coordinate);
        int halfWidth = Math.max(titleWidth, coordinateWidth) / 2;
        int x1 = -halfWidth - LABEL_PADDING_X;
        int x2 = halfWidth + LABEL_PADDING_X;
        int y1 = LABEL_ELEVATE_BY - 2;
        int y2 = LABEL_ELEVATE_BY + LABEL_LINE_HEIGHT * 2;

        RenderSystem.enablePolygonOffset();
        RenderSystem.setShader(class_757::method_34540);
        class_289 tessellator = class_289.method_1348();
        class_287 buffer = tessellator.method_1349();
        RenderSystem.polygonOffset(1.0F, 11.0F);
        buffer.method_1328(class_293.class_5596.field_27382, class_290.field_1576);
        rectangle(buffer, matrix, x1, y1, x2, y2, 3.0F, 0.0F, 0.0F, 0.6F * alpha);
        tessellator.method_1350();

        RenderSystem.polygonOffset(1.0F, 9.0F);
        buffer.method_1328(class_293.class_5596.field_27382, class_290.field_1576);
        rectangle(buffer, matrix, x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0.0F, 0.0F, 0.0F, 0.15F * alpha);
        tessellator.method_1350();
        RenderSystem.disablePolygonOffset();

        int color = (((int) (255.0F * alpha)) << 24) | (LABEL_TEXT_COLOR & 0x00FFFFFF);
        class_4597.class_4598 immediate = class_4597.method_22991(class_289.method_1348().method_1349());
        RenderSystem.disableDepthTest();
        textRenderer.method_27522(title, -titleWidth / 2.0F, LABEL_ELEVATE_BY, color, false, matrix, immediate, class_327.class_6415.field_33993, 0, LABEL_LIGHT, false);
        textRenderer.method_27522(coordinate, -coordinateWidth / 2.0F, LABEL_ELEVATE_BY + LABEL_LINE_HEIGHT, color, false, matrix, immediate, class_327.class_6415.field_33993, 0, LABEL_LIGHT, false);
        immediate.method_22993();
    }

    private static void rectangle(class_287 buffer, Matrix4f matrix, int x1, int y1, int x2, int y2, float r, float g, float b, float a) {
        colorVertex(buffer, matrix, x1, y1, r, g, b, a);
        colorVertex(buffer, matrix, x1, y2, r, g, b, a);
        colorVertex(buffer, matrix, x2, y2, r, g, b, a);
        colorVertex(buffer, matrix, x2, y1, r, g, b, a);
    }

    private static void colorVertex(class_287 buffer, Matrix4f matrix, float x, float y, float r, float g, float b, float a) {
        buffer.method_22918(matrix, x, y, 0.0F).method_22915(r, g, b, a).method_1344();
    }

    private static void vertex(class_287 buffer, Matrix4f matrix, float x, float y, float u, float v, float alpha) {
        buffer.method_22918(matrix, x, y, 0.0F).method_22913(u, v).method_22915(1.0F, 0.0F, 0.0F, alpha).method_1344();
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
