package io.github.huanmeng06.lmlp.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.huanmeng06.lmlp.gui.PlacementOriginMarker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererLateOriginMarkerMixin {
    @Inject(
            method = "submitBlockEntities",
            at = @At("TAIL"))
    private void lmlp$submitOriginMarker(
            PoseStack matrices,
            LevelRenderState renderState,
            SubmitNodeCollector collector,
            CallbackInfo ci) {
        PlacementOriginMarker.submit(Minecraft.getInstance().gameRenderer.mainCamera(), collector);
    }
}
