package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import io.github.huanmeng06.lmlp.gui.MaterialListHotkeyMatcher;
import io.github.huanmeng06.lmlp.gui.MaterialListOpener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiListBase.class, remap = false)
public abstract class GuiListBaseMixin {
    @Inject(method = "onKeyTyped", at = @At("HEAD"), cancellable = true)
    private void lmlp$toggleMaterialListFromHandledParent(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof GuiMaterialList) || !MaterialListHotkeyMatcher.matches(keyCode)) {
            return;
        }

        if (MaterialListOpener.closeToHandledScreenParent((GuiBase) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
