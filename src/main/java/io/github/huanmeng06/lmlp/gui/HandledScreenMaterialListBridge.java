package io.github.huanmeng06.lmlp.gui;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class HandledScreenMaterialListBridge {
    private static AbstractContainerScreen<?> preservedScreen;

    private HandledScreenMaterialListBridge() {
    }

    public static void preserveOnce(AbstractContainerScreen<?> screen) {
        preservedScreen = screen;
    }

    public static boolean consumePreserveClose(AbstractContainerScreen<?> screen) {
        if (preservedScreen != screen) {
            return false;
        }

        preservedScreen = null;
        return true;
    }
}
