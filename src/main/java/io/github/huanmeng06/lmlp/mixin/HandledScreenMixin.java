package io.github.huanmeng06.lmlp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.huanmeng06.lmlp.gui.HandledScreenMaterialListBridge;
import io.github.huanmeng06.lmlp.gui.MaterialListHotkeyMatcher;
import io.github.huanmeng06.lmlp.gui.MaterialListOpener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {
    @Inject(method = "removed", at = @At("HEAD"), cancellable = true, remap = false)
    private void lmlp$keepContainerOpenForMaterialList(CallbackInfo ci) {
        if (HandledScreenMaterialListBridge.consumePreserveClose((AbstractContainerScreen<?>) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    private void lmlp$openMaterialListFromHandledScreen(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (MaterialListHotkeyMatcher.matches(event.key())) {
            cir.setReturnValue(MaterialListOpener.openFromHandledScreen((AbstractContainerScreen<?>) (Object) this));
        }
    }
}
