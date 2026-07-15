package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.malilib.config.gui.ConfigOptionChangeListenerKeybind;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonKeybind;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import io.github.huanmeng06.lmlp.gui.KeybindResetRefreshRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = ConfigOptionChangeListenerKeybind.class, remap = false)
public abstract class KeybindResetStateMixin {
    @Shadow
    @Final
    private ButtonGeneric button;

    @Shadow
    @Final
    private IKeybind keybind;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lmlp$registerResetButton(IKeybind keybind,
                                           ConfigButtonKeybind hotkeyButton,
                                           ButtonGeneric resetButton,
                                           IKeybindConfigGui host,
                                           CallbackInfo ci) {
        KeybindResetRefreshRegistry.register(keybind, resetButton);
    }

    @Inject(method = "updateButtons", at = @At("TAIL"))
    private void lmlp$matchLitematicaResetState(CallbackInfo ci) {
        this.button.setEnabled(!Objects.equals(
                this.keybind.getStringValue(),
                this.keybind.getDefaultStringValue()));
    }
}
