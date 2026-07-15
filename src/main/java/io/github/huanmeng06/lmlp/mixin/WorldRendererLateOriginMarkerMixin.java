package io.github.huanmeng06.lmlp.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import io.github.huanmeng06.lmlp.gui.PlacementOriginMarker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererLateOriginMarkerMixin {
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V",
                    shift = At.Shift.AFTER))
    private void lmlp$renderOriginMarkerAfterFrameGraph(
            GraphicsResourceAllocator commandQueue,
            DeltaTracker tickCounter,
            boolean renderBlockOutline,
            CameraRenderState cameraState,
            Matrix4fc positionMatrix,
            GpuBufferSlice fogBuffer,
            Vector4f fogColor,
            boolean renderSky,
            ChunkSectionsToRender sectionsToRender,
            CallbackInfo ci) {
        PlacementOriginMarker.render(Minecraft.getInstance().gameRenderer.getMainCamera());
    }
}
