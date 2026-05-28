package io.github.huanmeng06.lmlp.mixin;

import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import io.github.huanmeng06.lmlp.gui.MaterialListOpener;
import net.minecraft.class_310;
import net.minecraft.class_465;

@Mixin(class_465.class)
public abstract class HandledScreenMixin {
    @Inject(method = "method_25404", at = @At("HEAD"), cancellable = true, remap = false)
    private void lmlp$openMaterialListFromHandledScreen(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (matchesMaterialListHotkey(keyCode)) {
            cir.setReturnValue(MaterialListOpener.open());
        }
    }

    private static boolean matchesMaterialListHotkey(int keyCode) {
        IKeybind keybind = Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeybind();
        List<Integer> keys = keybind.getKeys();
        if (keys.isEmpty() || !keys.contains(keyCode)) {
            return false;
        }

        long handle = class_310.method_1551().method_22683().method_4490();
        for (int key : keys) {
            if (key == keyCode) {
                continue;
            }

            if (key < 0 || GLFW.glfwGetKey(handle, key) != GLFW.GLFW_PRESS) {
                return false;
            }
        }

        return true;
    }
}
