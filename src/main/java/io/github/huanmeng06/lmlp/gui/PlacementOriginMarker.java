package io.github.huanmeng06.lmlp.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.config.Configs;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_757;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlacementOriginMarker {
    public static final int ORIGIN_TEXT_COLOR = 0xFF55FF55;

    private static final Pattern ORIGIN_PATTERN = Pattern.compile("^\\s*\\[?\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*]?\\s*$");
    private static final double ARRIVAL_DISTANCE_SQUARED = 4.0D;
    private static final double BEAM_RADIUS = 0.18D;
    private static final int BEAM_HALF_HEIGHT = 512;
    private static final Color4f HIGHLIGHT_FILL = new Color4f(1.0F, 0.05F, 0.05F, 0.20F);
    private static final Color4f HIGHLIGHT_OUTLINE = new Color4f(1.0F, 0.05F, 0.05F, 0.95F);
    private static final Color4f BEAM_FILL = new Color4f(1.0F, 0.05F, 0.05F, 0.16F);
    private static final Color4f BEAM_OUTLINE = new Color4f(1.0F, 0.05F, 0.05F, 0.55F);

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

        if (!isSameDimension(current.sourceDimension, currentDimensionId(context.world()))) {
            return;
        }

        class_310 client = class_310.method_1551();
        if (client.field_1724 != null && client.field_1724.method_5707(current.sourcePos.method_46558()) <= ARRIVAL_DISTANCE_SQUARED) {
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

        drawHighlight(current.sourcePos, cameraPos);
        drawBeam(current.sourcePos, cameraPos);

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

    private static void drawHighlight(class_2338 pos, class_243 cameraPos) {
        double expansion = 0.006D;
        double minX = pos.method_10263() - expansion - cameraPos.field_1352;
        double minY = pos.method_10264() - expansion - cameraPos.field_1351;
        double minZ = pos.method_10260() - expansion - cameraPos.field_1350;
        double maxX = pos.method_10263() + 1.0D + expansion - cameraPos.field_1352;
        double maxY = pos.method_10264() + 1.0D + expansion - cameraPos.field_1351;
        double maxZ = pos.method_10260() + 1.0D + expansion - cameraPos.field_1350;
        class_289 tessellator = class_289.method_1348();
        class_287 buffer = tessellator.method_1349();
        buffer.method_1328(class_293.class_5596.field_27382, class_290.field_1576);
        RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, HIGHLIGHT_FILL, buffer);
        tessellator.method_1350();

        RenderSystem.lineWidth(2.0F);
        buffer.method_1328(class_293.class_5596.field_29345, class_290.field_1576);
        RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(pos, cameraPos, HIGHLIGHT_OUTLINE, 0.01D, buffer);
        tessellator.method_1350();
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

    private static String currentDimensionId(class_638 world) {
        return world == null ? null : world.method_27983().method_29177().toString();
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
