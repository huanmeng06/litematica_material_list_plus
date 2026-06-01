package io.github.huanmeng06.lmlp.gui;

import org.joml.Quaternionf;

import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import net.minecraft.class_2960;
import net.minecraft.class_332;

final class ToggleArrowRenderer {
    private static final int ICON_WIDTH = 10;
    private static final int ICON_HEIGHT = 14;
    private static final float EXPANDED_ROTATION = (float) (Math.PI / 2.0D);
    private static final class_2960 TEXTURE = new class_2960(LitematicaMaterialListPlus.MOD_ID, "recipe_book/page_forward");
    private static final class_2960 HIGHLIGHTED_TEXTURE = new class_2960(LitematicaMaterialListPlus.MOD_ID, "recipe_book/page_forward_highlighted");

    private ToggleArrowRenderer() {
    }

    static void render(class_332 context, int slotX, int slotWidth, int centerY, float expandProgress, boolean hovered) {
        class_2960 texture = hovered ? HIGHLIGHTED_TEXTURE : TEXTURE;
        int centerX = slotX + slotWidth / 2;
        float rotation = EXPANDED_ROTATION * Math.max(0.0F, Math.min(1.0F, expandProgress));
        if (rotation <= 0.001F) {
            context.method_52706(texture, centerX - ICON_WIDTH / 2, centerY - ICON_HEIGHT / 2, ICON_WIDTH, ICON_HEIGHT);
            return;
        }

        context.method_51448().method_22903();
        context.method_51448().method_22904(centerX, centerY, 0.0D);
        context.method_51448().method_22907(new Quaternionf().rotateZ(rotation));
        context.method_52706(texture, -ICON_WIDTH / 2, -ICON_HEIGHT / 2, ICON_WIDTH, ICON_HEIGHT);
        context.method_51448().method_22909();
    }
}
