package io.github.huanmeng06.lmlp.gui;

import net.minecraft.class_327;
import net.minecraft.class_1799;
import net.minecraft.class_332;

public interface RecipeTooltipBridge {
    RecipeTooltipBridge DISABLED = new RecipeTooltipBridge() {
    };

    default boolean renderTooltip(class_332 context, class_327 textRenderer, class_1799 stack, int mouseX, int mouseY) {
        return false;
    }
}
