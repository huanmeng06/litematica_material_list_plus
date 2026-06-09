package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.GuiListBase;
import io.github.huanmeng06.lmlp.gui.ClickableCursor;
import io.github.huanmeng06.lmlp.gui.MaterialListHotkeyMatcher;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiListBase.class, remap = false)
public abstract class GuiListBaseMixin {
    @Inject(method = "drawContents", at = @At("HEAD"))
    private void lmlp$beginClickableCursorFrame(class_332 drawContext, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if ((Object) this instanceof GuiMaterialList) {
            ClickableCursor.beginFrame();
        }
    }

    @Inject(method = "drawContents", at = @At("TAIL"))
    private void lmlp$applyClickableCursorFrame(class_332 drawContext, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if ((Object) this instanceof GuiMaterialList) {
            ClickableCursor.endFrame();
        }
    }

    @Inject(method = "method_25432", at = @At("HEAD"))
    private void lmlp$resetClickableCursor(CallbackInfo ci) {
        if ((Object) this instanceof GuiMaterialList) {
            ClickableCursor.reset();
        }
    }

    @Inject(method = "onCharTyped", at = @At("HEAD"), cancellable = true)
    private void lmlp$suppressOpeningHotkeyCharInput(char charIn, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof GuiMaterialList)) {
            return;
        }

        if (MaterialListHotkeyMatcher.consumeSuppressedCharInput()) {
            cir.setReturnValue(true);
        }
    }
}
