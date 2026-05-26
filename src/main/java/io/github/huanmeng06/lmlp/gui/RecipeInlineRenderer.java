package io.github.huanmeng06.lmlp.gui;

import java.util.List;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import net.minecraft.class_332;

public final class RecipeInlineRenderer {
    private static final int HEADER_HEIGHT = 14;
    private static final int INGREDIENT_HEIGHT = 18;
    private static final int PADDING = 6;

    private RecipeInlineRenderer() {
    }

    public static int getHeight(List<RecipeSummary> summaries) {
        if (summaries.isEmpty()) {
            return 24;
        }

        int height = PADDING * 2;
        for (RecipeSummary summary : summaries) {
            height += HEADER_HEIGHT + 12 + summary.ingredients().size() * INGREDIENT_HEIGHT;
        }
        return Math.min(height, 220);
    }

    public static void render(WidgetBase widget, class_332 context, int x, int y, int width, List<RecipeSummary> summaries) {
        int height = getHeight(summaries);
        RenderUtils.drawOutlinedBox(x, y, Math.max(160, width), height, 0xDD000000, 0xFF777777);

        int textX = x + PADDING;
        int cursorY = y + PADDING;

        if (summaries.isEmpty()) {
            widget.drawString(textX, cursorY + 4, 0xFFFF5555, StringUtils.translate("lmlp.label.recipe.none"), context);
            return;
        }

        int recipeIndex = 1;
        int bottom = y + height - PADDING;
        for (RecipeSummary summary : summaries) {
            if (cursorY + HEADER_HEIGHT >= bottom) {
                widget.drawString(textX, cursorY, 0xFFAAAAAA, "...", context);
                return;
            }

            context.method_51427(summary.outputIcon(), textX, cursorY - 3);
            widget.drawString(textX + 20, cursorY, 0xFFFFFFFF, RecipeSummaryFormatter.header(summary, recipeIndex), context);
            cursorY += HEADER_HEIGHT;

            widget.drawString(textX + 20, cursorY, 0xFFAAAAAA, StringUtils.translate("lmlp.label.recipe.ingredients_total"), context);
            cursorY += 12;

            for (IngredientSummary ingredient : summary.ingredients()) {
                if (cursorY + INGREDIENT_HEIGHT >= bottom) {
                    widget.drawString(textX, cursorY, 0xFFAAAAAA, "...", context);
                    return;
                }

                RenderUtils.drawRect(textX, cursorY - 3, 16, 16, 0x30FFFFFF);
                context.method_51427(ingredient.icon(), textX, cursorY - 3);
                String line = RecipeSummaryFormatter.ingredientName(ingredient) + ": " + GuiBase.TXT_GOLD + RecipeSummaryFormatter.totalCount(ingredient);
                if (ingredient.countMissing() != ingredient.countTotal()) {
                    line += GuiBase.TXT_RST + " / " + GuiBase.TXT_RED + RecipeSummaryFormatter.missingCount(ingredient);
                }
                widget.drawString(textX + 22, cursorY, 0xFFFFFFFF, line, context);
                cursorY += INGREDIENT_HEIGHT;
            }

            recipeIndex++;
        }
    }
}
