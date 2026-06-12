package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.gui.MaterialListOpener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "fi.dy.masa.litematica.event.KeyCallbacks$KeyCallbackHotkeys", remap = false)
public abstract class KeyCallbackHotkeysMixin {
    @Inject(method = "onKeyAction", at = @At("HEAD"), cancellable = true)
    private void lmlp$openMaterialList(KeyAction action, IKeybind key, CallbackInfoReturnable<Boolean> cir) {
        if (key == Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeybind()) {
            ChunkMissingMaterialListCache.rememberCurrentPlacements("litematica_hotkey.before_open");
            cir.setReturnValue(MaterialListOpener.open());
        }
    }
}
