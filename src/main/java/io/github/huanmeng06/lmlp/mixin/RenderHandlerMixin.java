package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.event.RenderHandler;
import io.github.huanmeng06.lmlp.gui.PlacementOriginMarker;
import net.minecraft.class_276;
import net.minecraft.class_310;
import net.minecraft.class_3695;
import net.minecraft.class_4184;
import net.minecraft.class_4599;
import net.minecraft.class_4604;
import net.minecraft.class_9958;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderHandler.class, remap = false)
public abstract class RenderHandlerMixin {
    @Inject(method = "onRenderWorldLastAdvanced", at = @At("TAIL"))
    private void lmlp$renderOriginMarkerLabelAfterLitematica(
            class_276 framebuffer,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            class_4604 frustum,
            class_4184 camera,
            class_9958 fog,
            class_4599 vertexSorter,
            class_3695 profiler,
            CallbackInfo ci) {
        PlacementOriginMarker.renderLabelOverlayAfterLitematica(modelViewMatrix, camera, class_310.method_1551());
    }
}
