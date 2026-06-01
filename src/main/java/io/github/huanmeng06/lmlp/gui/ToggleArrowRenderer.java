package io.github.huanmeng06.lmlp.gui;

import org.joml.Quaternionf;

import net.minecraft.class_2960;
import net.minecraft.class_332;

final class ToggleArrowRenderer {
    private static final int ICON_WIDTH = 10;
    private static final int ICON_HEIGHT = 14;
    private static final int TEXTURE_SIZE = 256;
    private static final int NORMAL_U = 0;
    private static final int NORMAL_V = 208;
    private static final int HIGHLIGHTED_U = 0;
    private static final int HIGHLIGHTED_V = 224;
    private static final float EXPANDED_ROTATION = (float) (Math.PI / 2.0D);
    private static final class_2960 TEXTURE = new class_2960("minecraft", "textures/gui/recipe_book.png");

    private ToggleArrowRenderer() {
    }

    static void render(class_332 context, int slotX, int slotWidth, int centerY, float expandProgress, boolean hovered) {
        int textureU = hovered ? HIGHLIGHTED_U : NORMAL_U;
        int textureV = hovered ? HIGHLIGHTED_V : NORMAL_V;
        int centerX = slotX + slotWidth / 2;
        float rotation = EXPANDED_ROTATION * Math.max(0.0F, Math.min(1.0F, expandProgress));
        if (rotation <= 0.001F) {
            drawIcon(context, centerX - ICON_WIDTH / 2, centerY - ICON_HEIGHT / 2, textureU, textureV);
            return;
        }

        context.method_51448().method_22903();
        context.method_51448().method_22904(centerX, centerY, 0.0D);
        context.method_51448().method_22907(new Quaternionf().rotateZ(rotation));
        drawIcon(context, -ICON_WIDTH / 2, -ICON_HEIGHT / 2, textureU, textureV);
        context.method_51448().method_22909();
    }

    private static void drawIcon(class_332 context, int x, int y, int textureU, int textureV) {
        context.method_25290(TEXTURE, x, y, textureU, textureV, ICON_WIDTH, ICON_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
