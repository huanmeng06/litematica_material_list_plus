package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiConfigsBase.class, remap = false)
public abstract class ClearOriginHotkeyButtonMixin {
    @Shadow
    protected abstract void updateKeybindButtons();

    @Inject(method = "onKeyTyped", at = @At("RETURN"))
    private void lmlp$refreshResetAfterKeyInput(class_11908 keyInput, CallbackInfoReturnable<Boolean> cir) {
        this.updateKeybindButtons();
    }

    @Inject(method = "onMouseClicked", at = @At("RETURN"))
    private void lmlp$refreshResetAfterMouseInput(class_11909 mouseClick, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        this.updateKeybindButtons();
    }
}
