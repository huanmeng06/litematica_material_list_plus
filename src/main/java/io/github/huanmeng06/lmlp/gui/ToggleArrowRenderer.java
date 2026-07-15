package io.github.huanmeng06.lmlp.gui;

import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import net.minecraft.resources.Identifier;
import fi.dy.masa.malilib.render.GuiContext;

final class ToggleArrowRenderer {
    private static final int ICON_WIDTH = 10;
    private static final int ICON_HEIGHT = 14;
    private static final float EXPANDED_ROTATION = (float) (Math.PI / 2.0D);
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(LitematicaMaterialListPlus.MOD_ID, "recipe_book/page_forward");
    private static final Identifier HIGHLIGHTED_TEXTURE = Identifier.fromNamespaceAndPath(LitematicaMaterialListPlus.MOD_ID, "recipe_book/page_forward_highlighted");

    private ToggleArrowRenderer() {
    }

    static void render(GuiContext context, int slotX, int slotWidth, int centerY, float expandProgress, boolean hovered) {
        Identifier texture = hovered ? HIGHLIGHTED_TEXTURE : TEXTURE;
        int centerX = slotX + slotWidth / 2;
        float rotation = EXPANDED_ROTATION * Math.max(0.0F, Math.min(1.0F, expandProgress));
        if (rotation <= 0.001F) {
            context.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, centerX - ICON_WIDTH / 2, centerY - ICON_HEIGHT / 2, ICON_WIDTH, ICON_HEIGHT);
            return;
        }

        context.pose().pushMatrix();
        context.pose().translate(centerX, centerY);
        context.pose().rotate(rotation);
        context.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, -ICON_WIDTH / 2, -ICON_HEIGHT / 2, ICON_WIDTH, ICON_HEIGHT);
        context.pose().popMatrix();
    }
}
