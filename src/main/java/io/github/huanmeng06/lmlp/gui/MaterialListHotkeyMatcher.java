package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import net.minecraft.class_310;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class MaterialListHotkeyMatcher {
    private static int suppressedCharInputs;
    private static long suppressCharInputUntilNanos;

    private MaterialListHotkeyMatcher() {
    }

    public static boolean matches(int keyCode) {
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

    public static boolean isConfiguredKey(int keyCode) {
        IKeybind keybind = Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeybind();
        return keybind.getKeys().contains(keyCode);
    }

    public static void suppressNextCharInput() {
        suppressedCharInputs++;
        suppressCharInputUntilNanos = System.nanoTime() + 250_000_000L;
    }

    public static boolean consumeSuppressedCharInput() {
        if (suppressedCharInputs <= 0) {
            return false;
        }

        if (System.nanoTime() > suppressCharInputUntilNanos) {
            suppressedCharInputs = 0;
            return false;
        }

        suppressedCharInputs--;
        return true;
    }
}
