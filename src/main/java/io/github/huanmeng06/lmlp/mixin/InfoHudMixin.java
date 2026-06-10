package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.scheduler.tasks.TaskProcessChunkBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InfoHud.class, remap = false)
public abstract class InfoHudMixin {
    @Inject(method = "addInfoHudRenderer", at = @At("HEAD"), cancellable = true)
    private void lmlp$hideMissingChunkHud(IInfoHudRenderer renderer, boolean enabled, CallbackInfo ci) {
        if (renderer instanceof TaskProcessChunkBase) {
            ci.cancel();
        }
    }
}
