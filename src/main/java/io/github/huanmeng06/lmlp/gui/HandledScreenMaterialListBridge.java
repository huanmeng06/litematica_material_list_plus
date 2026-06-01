package io.github.huanmeng06.lmlp.gui;

import net.minecraft.class_465;

public final class HandledScreenMaterialListBridge {
    private static class_465<?> preservedScreen;

    private HandledScreenMaterialListBridge() {
    }

    public static void preserveOnce(class_465<?> screen) {
        preservedScreen = screen;
    }

    public static boolean consumePreserveClose(class_465<?> screen) {
        if (preservedScreen != screen) {
            return false;
        }

        preservedScreen = null;
        return true;
    }
}
