package io.github.huanmeng06.lmlp.gui;

import org.joml.Quaternionf;

import net.minecraft.class_2960;
import net.minecraft.class_332;

final class ToggleArrowRenderer {
    private static final int ICON_WIDTH = 10;
    private static final int ICON_HEIGHT = 14;
    private static final float EXPANDED_ROTATION = (float) (Math.PI / 2.0D);
    private static final class_2960 TEXTURE = new class_2960("minecraft", "recipe_book/page_forward");
    private static final class_2960 HIGHLIGHTED_TEXTURE = new class_2960("minecraft", "recipe_book/page_forward_highlighted");

    private ToggleArrowRenderer() {
    }

    static void render(class_332 context, int slotX, int slotWidth, int centerY, float expandProgress, boolean hovered) {
        class_2960 texture = hovered ? HIGHLIGHTED_TEXTURE : TEXTURE;
        int centerX = slotX + slotWidth / 2;
        float rotation = EXPANDED_ROTATION * Math.max(0.0F, Math.min(1.0F, expandProgress));
        if (rotation <= 0.001F) {
            drawIcon(context, texture, centerX - ICON_WIDTH / 2, centerY - ICON_HEIGHT / 2);
            return;
        }

        context.method_51448().method_22903();
        context.method_51448().method_22904(centerX, centerY, 0.0D);
        context.method_51448().method_22907(new Quaternionf().rotateZ(rotation));
        drawIcon(context, texture, -ICON_WIDTH / 2, -ICON_HEIGHT / 2);
        context.method_51448().method_22909();
    }

    private static void drawIcon(class_332 context, class_2960 texture, int x, int y) {
        context.method_25290(texture, x, y, 0.0F, 0.0F, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);
    }
}
