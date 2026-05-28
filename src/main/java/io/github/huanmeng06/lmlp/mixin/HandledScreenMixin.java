package io.github.huanmeng06.lmlp.mixin;

import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import io.github.huanmeng06.lmlp.gui.MaterialListOverlay;
import net.minecraft.class_332;
import net.minecraft.class_310;
import net.minecraft.class_465;

@Mixin(class_465.class)
public abstract class HandledScreenMixin {
    @Inject(method = "method_25394", at = @At("TAIL"), remap = false)
    private void lmlp$renderMaterialListOverlay(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MaterialListOverlay.render((class_465<?>) (Object) this, context, mouseX, mouseY, delta, screenWidth(), screenHeight());
    }

    @Inject(method = "method_25404", at = @At("HEAD"), cancellable = true, remap = false)
    private void lmlp$openMaterialListFromHandledScreen(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (matchesMaterialListHotkey(keyCode)) {
            cir.setReturnValue(MaterialListOverlay.toggle((class_465<?>) (Object) this, screenWidth(), screenHeight()));
            return;
        }

        if (MaterialListOverlay.keyPressed((class_465<?>) (Object) this, keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25400", at = @At("HEAD"), cancellable = true, remap = false)
    private void lmlp$typeMaterialListOverlayChar(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (MaterialListOverlay.charTyped((class_465<?>) (Object) this, chr, modifiers)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true, remap = false)
    private void lmlp$clickMaterialListOverlay(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (MaterialListOverlay.mouseClicked((class_465<?>) (Object) this, mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25406", at = @At("HEAD"), cancellable = true, remap = false)
    private void lmlp$releaseMaterialListOverlay(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (MaterialListOverlay.mouseReleased((class_465<?>) (Object) this, mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25401", at = @At("HEAD"), cancellable = true, remap = false)
    private void lmlp$scrollMaterialListOverlay(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (MaterialListOverlay.mouseScrolled((class_465<?>) (Object) this, mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
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

    private static int screenWidth() {
        return class_310.method_1551().method_22683().method_4486();
    }

    private static int screenHeight() {
        return class_310.method_1551().method_22683().method_4502();
    }
}
