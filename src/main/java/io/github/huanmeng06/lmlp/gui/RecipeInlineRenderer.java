package io.github.huanmeng06.lmlp.gui;

import java.util.List;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import net.minecraft.class_332;

public final class RecipeInlineRenderer {
    private static final int INGREDIENT_HEIGHT = 22;
    private static final int PADDING = 8;

    private RecipeInlineRenderer() {
    }

    public static int getHeight(List<RecipeSummary> summaries) {
        if (summaries.isEmpty()) {
            return 48;
        }

        RecipeSummary summary = summaries.get(0);
        int height = 64 + summary.ingredients().size() * INGREDIENT_HEIGHT;
        if (summaries.size() > 1) {
            height += 22;
        }
        return height;
    }

    public static void render(WidgetBase widget, class_332 context, int x, int y, int width, List<RecipeSummary> summaries) {
        int height = getHeight(summaries);
        RenderUtils.drawOutlinedBox(x, y, Math.max(160, width), height, 0xDD000000, 0xFF777777);

        int panelWidth = Math.max(160, width);
        int textX = x + PADDING;

        if (summaries.isEmpty()) {
            widget.drawString(textX, y + 16, 0xFFFFCC66, StringUtils.translate("lmlp.label.recipe.none"), context);
            return;
        }

        RecipeSummary summary = summaries.get(0);
        int cursorY = y + PADDING;
        String itemName = GuiBase.TXT_BOLD + ItemStackTexts.name(summary.outputIcon());
        int titleWidth = 16 + 8 + widget.getStringWidth(itemName);
        int titleX = x + (panelWidth - titleWidth) / 2;
        context.method_51427(summary.outputIcon(), titleX, cursorY);
        widget.drawString(titleX + 24, cursorY + 1, 0xFFFFFFFF, itemName, context);
        cursorY += 24;

        widget.drawString(textX, cursorY, 0xFFFFFFFF, RecipeSummaryFormatter.header(summary, 1), context);
        cursorY += 18;

        int ingredientBoxY = cursorY;
        int ingredientBoxHeight = 18 + summary.ingredients().size() * INGREDIENT_HEIGHT;
        RenderUtils.drawRect(textX - 2, ingredientBoxY - 2, panelWidth - PADDING * 2 + 4, ingredientBoxHeight, 0x66000000);
        widget.drawString(textX, cursorY, 0xFFAAAAAA, StringUtils.translate("lmlp.label.recipe.ingredients_total"), context);
        cursorY += 18;

        for (IngredientSummary ingredient : summary.ingredients()) {
            RenderUtils.drawRect(textX, cursorY - 3, 18, 18, 0x30FFFFFF);
            context.method_51427(ingredient.icon(), textX + 1, cursorY - 2);
            String line = RecipeSummaryFormatter.ingredientName(ingredient) + ": " + GuiBase.TXT_GOLD + RecipeSummaryFormatter.totalCount(ingredient);
            if (ingredient.countMissing() != ingredient.countTotal()) {
                line += GuiBase.TXT_RST + " / " + GuiBase.TXT_RED + RecipeSummaryFormatter.missingCount(ingredient);
            }
            widget.drawString(textX + 26, cursorY + 2, 0xFFFFFFFF, line, context);
            cursorY += INGREDIENT_HEIGHT;
        }

        if (summaries.size() > 1) {
            widget.drawString(textX, y + height - 16, 0xFFFFFFFF, GuiBase.TXT_GOLD + StringUtils.translate("lmlp.label.recipe.more_hint"), context);
        }
    }
}
