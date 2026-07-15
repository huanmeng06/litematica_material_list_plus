package io.github.huanmeng06.lmlp.gui;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class ClickableCursor {
    private static long handCursor;
    private static boolean handActive;
    private static boolean handRequested;

    private ClickableCursor() {
    }

    public static void beginFrame() {
        handRequested = false;
    }

    public static void requestHand() {
        handRequested = true;
    }

    public static void endFrame() {
        setHand(handRequested);
    }

    public static void reset() {
        handRequested = false;
        setHand(false);
    }

    private static void setHand(boolean hand) {
        if (handActive == hand) {
            return;
        }

        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.getWindow() == null) {
                return;
            }

            long window = client.getWindow().handle();
            if (hand) {
                if (handCursor == 0L) {
                    handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
                }
                if (handCursor != 0L) {
                    GLFW.glfwSetCursor(window, handCursor);
                    handActive = true;
                }
            } else {
                GLFW.glfwSetCursor(window, 0L);
                handActive = false;
            }
        } catch (Throwable throwable) {
            handActive = false;
        }
    }
}
