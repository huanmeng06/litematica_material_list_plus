package io.github.huanmeng06.lmlp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.huanmeng06.lmlp.gui.HandledScreenMaterialListBridge;
import io.github.huanmeng06.lmlp.gui.MaterialListHotkeyMatcher;
import io.github.huanmeng06.lmlp.gui.MaterialListOpener;
import net.minecraft.class_11908;
import net.minecraft.class_465;

@Mixin(class_465.class)
public abstract class HandledScreenMixin {
    @Inject(method = "method_25432", at = @At("HEAD"), cancellable = true, remap = false)
    private void lmlp$keepContainerOpenForMaterialList(CallbackInfo ci) {
        if (HandledScreenMaterialListBridge.consumePreserveClose((class_465<?>) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "method_25404", at = @At("HEAD"), cancellable = true, remap = false)
    private void lmlp$openMaterialListFromHandledScreen(class_11908 event, CallbackInfoReturnable<Boolean> cir) {
        if (MaterialListHotkeyMatcher.matches(event.comp_4795())) {
            cir.setReturnValue(MaterialListOpener.openFromHandledScreen((class_465<?>) (Object) this));
        }
    }
}
