package io.github.huanmeng06.lmlp.gui;

import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2960;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class OriginMarkerHudLabelRenderer {
    private static final class_2960 LAYER_ID = class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "origin_marker_label");
    private static FrameState lastWorldFrame;
    private static boolean registered;

    private OriginMarkerHudLabelRenderer() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        HudLayerRegistrationCallback.EVENT.register(layers ->
                layers.attachLayerAfter(IdentifiedLayer.SUBTITLES, IdentifiedLayer.of(LAYER_ID, OriginMarkerHudLabelRenderer::render)));
    }

    // 1.21.5+ world-space text cannot reliably out-rank Litematica projection; HUD keeps the label topmost while still anchoring it to the world position.
    public static void captureWorldFrame(WorldRenderContext context) {
        if (context == null || context.camera() == null || context.positionMatrix() == null || context.projectionMatrix() == null) {
            return;
        }

        lastWorldFrame = new FrameState(
                context.camera().method_19326(),
                new org.joml.Vector3f(context.camera().method_19335()),
                new Matrix4f(context.positionMatrix()),
                new Matrix4f(context.projectionMatrix()));
    }

    public static void clearWorldFrame() {
        lastWorldFrame = null;
    }

    public static ScreenProjection projectWorldToScreen(class_2338 pos, int screenWidth, int screenHeight) {
        FrameState frame = lastWorldFrame;
        if (frame == null || pos == null || screenWidth <= 0 || screenHeight <= 0) {
            return null;
        }

        double worldX = pos.method_10263() + 0.5D;
        double worldY = pos.method_10264() + 0.5D;
        double worldZ = pos.method_10260() + 0.5D;
        float relativeX = (float) (worldX - frame.cameraPos.field_1352);
        float relativeY = (float) (worldY - frame.cameraPos.field_1351);
        float relativeZ = (float) (worldZ - frame.cameraPos.field_1350);
        if (relativeX * frame.cameraForward.x + relativeY * frame.cameraForward.y + relativeZ * frame.cameraForward.z <= 0.0F) {
            return new ScreenProjection(0.0F, 0.0F, false, true);
        }

        Vector4f clip = new Vector4f(
                relativeX,
                relativeY,
                relativeZ,
                1.0F);

        new Matrix4f(frame.projectionMatrix).mul(frame.positionMatrix).transform(clip);
        if (Math.abs(clip.w()) < 1.0E-5F) {
            return new ScreenProjection(0.0F, 0.0F, false, false);
        }

        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        if (ndcX < -1.0F || ndcX > 1.0F || ndcY < -1.0F || ndcY > 1.0F) {
            return new ScreenProjection(0.0F, 0.0F, false, false);
        }

        float screenX = (ndcX * 0.5F + 0.5F) * screenWidth;
        float screenY = (0.5F - ndcY * 0.5F) * screenHeight;
        return new ScreenProjection(screenX, screenY, true, false);
    }

    public static void drawHudLabel(class_332 context,
                                    class_327 textRenderer,
                                    String title,
                                    String coordinate,
                                    String distanceText,
                                    float alpha,
                                    float screenX,
                                    float screenY,
                                    float configuredScale) {
        if (context == null || textRenderer == null) {
            return;
        }

        int titleWidth = textRenderer.method_1727(title);
        int coordinateWidth = textRenderer.method_1727(coordinate);
        int distanceWidth = textRenderer.method_1727(distanceText);
        int halfWidth = Math.max(Math.max(titleWidth, coordinateWidth), distanceWidth) / 2;
        int x1 = -halfWidth - PlacementOriginMarker.LABEL_PADDING_X;
        int x2 = halfWidth + PlacementOriginMarker.LABEL_PADDING_X;
        int y1 = PlacementOriginMarker.LABEL_ELEVATE_BY - 2;
        int y2 = PlacementOriginMarker.LABEL_ELEVATE_BY + PlacementOriginMarker.LABEL_LINE_HEIGHT * 3;

        float scale = hudTextScale(configuredScale);
        class_4587 matrices = context.method_51448();
        matrices.method_22903();
        matrices.method_22904(screenX, screenY, 0.0D);
        matrices.method_22905(scale, scale, 1.0F);
        try {
            context.method_25294(x1, y1, x2, y2, argb(0.6F * alpha, 0xFF0000));
            context.method_25294(x1 + 1, y1 + 1, x2 - 1, y2 - 1, argb(0.15F * alpha, 0x000000));

            int color = argb(alpha, PlacementOriginMarker.LABEL_TEXT_COLOR);
            context.method_51433(textRenderer, title, -titleWidth / 2, PlacementOriginMarker.LABEL_ELEVATE_BY, color, false);
            context.method_51433(textRenderer, coordinate, -coordinateWidth / 2, PlacementOriginMarker.LABEL_ELEVATE_BY + PlacementOriginMarker.LABEL_LINE_HEIGHT, color, false);
            context.method_51433(textRenderer, distanceText, -distanceWidth / 2, PlacementOriginMarker.LABEL_ELEVATE_BY + PlacementOriginMarker.LABEL_LINE_HEIGHT * 2, color, false);
        } finally {
            matrices.method_22909();
        }
        context.method_51452();
    }

    private static void render(class_332 context, net.minecraft.class_9779 tickCounter) {
        PlacementOriginMarker.renderHudLabel(context, tickCounter);
    }

    private static float hudTextScale(float configuredScale) {
        return 0.5F + Math.max(1.0F / 3.0F, Math.min(5.0F / 3.0F, configuredScale)) * 0.5F;
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private record FrameState(class_243 cameraPos, org.joml.Vector3f cameraForward, Matrix4f positionMatrix, Matrix4f projectionMatrix) {
    }

    public record ScreenProjection(float screenX, float screenY, boolean visible, boolean behindCamera) {
    }
}
