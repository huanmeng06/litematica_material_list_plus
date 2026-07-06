package io.github.huanmeng06.lmlp.recipe.rei;

import java.util.ArrayList;
import java.util.List;

import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.common.util.EntryStacks;
import io.github.huanmeng06.lmlp.gui.RecipeTooltipBridge;
import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_1799;
import net.minecraft.class_1836;
import net.minecraft.class_332;

public final class ReiTooltipBridge implements RecipeTooltipBridge {
    @Override
    public boolean renderTooltip(class_332 context, class_327 textRenderer, class_1799 stack, int mouseX, int mouseY) {
        if (stack.method_7960()) {
            return false;
        }

        Tooltip tooltip = EntryStacks.of(stack).getTooltip(TooltipContext.of(new Point(mouseX, mouseY), class_1836.field_41070), false);
        if (tooltip == null) {
            return false;
        }

        return renderTooltip(context, textRenderer, tooltip, mouseX, mouseY);
    }

    static boolean renderTooltip(class_332 context, class_327 textRenderer, Tooltip tooltip, int mouseX, int mouseY) {
        List<class_2561> lines = new ArrayList<>();
        for (Tooltip.Entry entry : tooltip.entries()) {
            if (entry.isText()) {
                lines.add(entry.getAsText());
            }
        }

        if (lines.isEmpty()) {
            return false;
        }

        context.method_51434(textRenderer, lines, mouseX, mouseY);
        return true;
    }
}
