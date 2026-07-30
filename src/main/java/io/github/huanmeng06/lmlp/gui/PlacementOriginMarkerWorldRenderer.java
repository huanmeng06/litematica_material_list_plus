package io.github.huanmeng06.lmlp.gui;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import fi.dy.masa.malilib.interfaces.IRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public final class PlacementOriginMarkerWorldRenderer implements IRenderer {
    public static final PlacementOriginMarkerWorldRenderer INSTANCE = new PlacementOriginMarkerWorldRenderer();

    private PlacementOriginMarkerWorldRenderer() {
    }

    @Override
    public void onExtractWorldLast(DeltaTracker tickCounter,
                                   Camera camera,
                                   float tickDelta,
                                   ProfilerFiller profiler) {
        PlacementOriginMarker.extractWorldLabel(camera);
    }

    @Override
    public void onRenderWorldLast(RenderTarget renderTarget,
                                  Matrix4fc positionMatrix,
                                  CameraRenderState cameraState,
                                  Frustum frustum,
                                  RenderBuffers renderBuffers,
                                  GpuBufferSlice fogBuffer,
                                  Vector4f fogColor,
                                  ProfilerFiller profiler) {
        PlacementOriginMarker.renderWorldLabel(cameraState);
    }
}
