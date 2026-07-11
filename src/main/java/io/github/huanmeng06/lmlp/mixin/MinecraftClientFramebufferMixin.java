package io.github.huanmeng06.lmlp.mixin;

import io.github.huanmeng06.lmlp.gui.textlist.GrayscaleRenderTargetOverride;
import net.minecraft.class_276;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_310.class)
public abstract class MinecraftClientFramebufferMixin {
    @Inject(method = "method_1522", at = @At("HEAD"), cancellable = true)
    private void lmlp$useGrayscaleIconTarget(CallbackInfoReturnable<class_276> cir) {
        class_276 target = GrayscaleRenderTargetOverride.get();
        if (target != null) {
            cir.setReturnValue(target);
        }
    }
}
