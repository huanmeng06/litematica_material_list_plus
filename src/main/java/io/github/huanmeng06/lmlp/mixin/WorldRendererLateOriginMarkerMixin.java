package io.github.huanmeng06.lmlp.mixin;

import io.github.huanmeng06.lmlp.gui.PlacementOriginMarker;
import net.minecraft.class_4184;
import net.minecraft.class_757;
import net.minecraft.class_761;
import net.minecraft.class_765;
import net.minecraft.class_9779;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_761.class)
public abstract class WorldRendererLateOriginMarkerMixin {
    @Inject(method = "method_22710", at = @At("TAIL"))
    private void lmlp$renderOriginMarkerLate(class_9779 tickCounter, boolean renderBlockOutline,
                                             class_4184 camera, class_757 gameRenderer,
                                             class_765 lightmapTextureManager, Matrix4f positionMatrix,
                                             Matrix4f projectionMatrix, CallbackInfo ci) {
        PlacementOriginMarker.render(camera);
    }
}
