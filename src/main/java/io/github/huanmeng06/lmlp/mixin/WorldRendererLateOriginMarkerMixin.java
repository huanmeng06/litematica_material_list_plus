package io.github.huanmeng06.lmlp.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import io.github.huanmeng06.lmlp.gui.PlacementOriginMarker;
import net.minecraft.class_4184;
import net.minecraft.class_761;
import net.minecraft.class_9779;
import net.minecraft.class_9922;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_761.class)
public abstract class WorldRendererLateOriginMarkerMixin {
    @Inject(
            method = "method_22710",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_9909;method_61910(Lnet/minecraft/class_9922;Lnet/minecraft/class_9909$class_9912;)V",
                    shift = At.Shift.AFTER))
    private void lmlp$renderOriginMarkerAfterFrameGraph(
            class_9922 commandQueue,
            class_9779 tickCounter,
            boolean renderBlockOutline,
            class_4184 camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            Matrix4f modelViewMatrix,
            GpuBufferSlice fogBuffer,
            Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci) {
        PlacementOriginMarker.render(camera);
    }
}
