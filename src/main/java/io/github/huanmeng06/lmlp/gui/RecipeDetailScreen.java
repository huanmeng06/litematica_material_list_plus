package io.github.huanmeng06.lmlp.gui;

import java.util.List;

import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;

public class RecipeDetailScreen extends class_437 {
    private final class_437 parent;
    private final class_1799 target;
    private final int totalCount;
    private final int missingCount;
    private final List<RecipeSummary> summaries;
    private int scrollY;

    public RecipeDetailScreen(class_437 parent, class_1799 target, int totalCount, int missingCount, List<RecipeSummary> summaries) {
        super(class_2561.method_43470("lmlp.gui.recipe_detail.title"));
        this.parent = parent;
        this.target = target.method_7972();
        this.totalCount = totalCount;
        this.missingCount = missingCount;
        this.summaries = List.copyOf(summaries);
    }

    @Override
    public void method_25419() {
        this.field_22787.method_1507(this.parent);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double amount) {
        this.scrollY = Math.max(0, this.scrollY - (int) (amount * 18));
        return true;
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        this.method_25420(context);

        int left = 24;
        int top = 24 - this.scrollY;
        int width = this.field_22789 - 48;

        context.method_51427(this.target, left, top);
        context.method_51431(this.field_22793, this.target, left, top);
        context.method_51433(this.field_22793, ItemStackTexts.name(this.target), left + 24, top + 4, 0xFFFFFFFF, false);
        context.method_51433(this.field_22793, CountFormatter.format(this.target, this.totalCount) + " / " + CountFormatter.format(this.target, this.missingCount), left + 24, top + 16, 0xFFAAAAAA, false);
        context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.back"), this.field_22789 - 90, 12, 0xFFAAAAAA, false);

        int y = top + 42;
        if (this.summaries.isEmpty()) {
            RenderUtils.drawOutlinedBox(left, y, width, 34, 0xDD000000, 0xFF777777);
            context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.none"), left + 8, y + 12, 0xFFFF5555, false);
            return;
        }

        int index = 1;
        for (RecipeSummary summary : this.summaries) {
            int boxHeight = 44 + summary.ingredients().size() * 20;
            RenderUtils.drawOutlinedBox(left, y, width, boxHeight, 0xDD000000, 0xFF777777);
            context.method_51427(summary.outputIcon(), left + 8, y + 8);
            context.method_51433(this.field_22793, RecipeSummaryFormatter.header(summary, index), left + 30, y + 10, 0xFFFFFFFF, false);
            context.method_51433(this.field_22793, summary.recipeId(), left + 30, y + 22, 0xFF888888, false);

            int lineY = y + 42;
            for (IngredientSummary ingredient : summary.ingredients()) {
                context.method_51427(ingredient.icon(), left + 12, lineY - 4);
                String line = RecipeSummaryFormatter.ingredientName(ingredient) + ": " + RecipeSummaryFormatter.totalCount(ingredient);
                if (ingredient.countMissing() != ingredient.countTotal()) {
                    line += " / " + RecipeSummaryFormatter.missingCount(ingredient);
                }
                context.method_51433(this.field_22793, line, left + 34, lineY, 0xFFFFFFFF, false);
                lineY += 20;
            }

            y += boxHeight + 8;
            index++;
        }
    }
}
