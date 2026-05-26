package io.github.huanmeng06.lmlp.gui;

import java.util.List;

import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSlotSummary;
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
    private boolean draggingScrollbar;

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
        this.scrollY = clamp(this.scrollY - (int) (amount * 24), 0, this.maxScroll());
        return true;
    }

    @Override
    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverScrollbar(mouseX, mouseY)) {
            this.draggingScrollbar = true;
            this.setScrollFromMouse(mouseY);
            return true;
        }

        return super.method_25402(mouseX, mouseY, button);
    }

    @Override
    public boolean method_25403(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.draggingScrollbar) {
            this.setScrollFromMouse(mouseY);
            return true;
        }

        return super.method_25403(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean method_25406(double mouseX, double mouseY, int button) {
        this.draggingScrollbar = false;
        return super.method_25406(mouseX, mouseY, button);
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        this.method_25420(context);
        this.scrollY = clamp(this.scrollY, 0, this.maxScroll());

        int left = 24;
        int headerTop = 22;
        int width = this.field_22789 - 48;
        int headerHeight = 48;
        int contentTop = headerTop + headerHeight + 12;
        int contentBottom = this.field_22790 - 18;

        RenderUtils.drawOutlinedBox(left, headerTop, Math.min(width, 560), headerHeight, 0xDD000000, 0xFF777777);
        context.method_51427(this.target, left + 10, headerTop + 8);
        context.method_51431(this.field_22793, this.target, left + 10, headerTop + 8);
        context.method_51433(this.field_22793, ItemStackTexts.name(this.target), left + 34, headerTop + 9, 0xFFFFFFFF, false);
        context.method_51433(this.field_22793, CountFormatter.format(this.target, this.totalCount) + " / " + CountFormatter.format(this.target, this.missingCount), left + 34, headerTop + 25, 0xFFAAAAAA, false);
        context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.back"), this.field_22789 - 90, 12, 0xFFAAAAAA, false);

        int y = contentTop - this.scrollY;
        if (this.summaries.isEmpty()) {
            context.method_44379(left, contentTop, left + width - 18, contentBottom);
            RenderUtils.drawOutlinedBox(left, y, width - 22, 34, 0xDD000000, 0xFF777777);
            context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.none"), left + 8, y + 12, 0xFFFF5555, false);
            context.method_44380();
            this.renderScrollbar(context, contentTop, contentBottom);
            return;
        }

        context.method_44379(left, contentTop, left + width - 18, contentBottom);
        int index = 1;
        for (RecipeSummary summary : this.summaries) {
            int boxHeight = recipeBoxHeight(summary);
            this.renderRecipeBox(context, summary, index, left, y, width - 22, boxHeight);
            y += boxHeight + 10;
            index++;
        }
        context.method_44380();

        this.renderScrollbar(context, contentTop, contentBottom);
    }

    private void renderRecipeBox(class_332 context, RecipeSummary summary, int index, int left, int y, int width, int boxHeight) {
        RenderUtils.drawOutlinedBox(left, y, width, boxHeight, 0xDD000000, 0xFF777777);
        context.method_51427(summary.outputIcon(), left + 8, y + 8);
        context.method_51433(this.field_22793, RecipeSummaryFormatter.header(summary, index), left + 30, y + 10, 0xFFFFFFFF, false);
        String recipeId = summary.recipeId();
        if (summary.shapeless()) {
            recipeId += " / " + StringUtils.translate("lmlp.label.recipe.shapeless");
        }
        context.method_51433(this.field_22793, recipeId, left + 30, y + 23, 0xFF888888, false);

        int gridX = left + 18;
        int gridY = y + 48;
        this.renderCraftingGrid(context, summary, gridX, gridY);

        int lineY = gridY + 82;
        context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.ingredients_total"), left + 14, lineY, 0xFFAAAAAA, false);
        lineY += 16;

        for (IngredientSummary ingredient : summary.ingredients()) {
            context.method_51427(ingredient.icon(), left + 18, lineY - 5);
            String line = RecipeSummaryFormatter.ingredientName(ingredient) + ": " + RecipeSummaryFormatter.totalCount(ingredient);
            if (ingredient.countMissing() != ingredient.countTotal()) {
                line += " / " + RecipeSummaryFormatter.missingCount(ingredient);
            }
            context.method_51433(this.field_22793, line, left + 42, lineY, 0xFFFFFFFF, false);
            lineY += 20;
        }
    }

    private void renderCraftingGrid(class_332 context, RecipeSummary summary, int x, int y) {
        RenderUtils.drawOutlinedBox(x - 8, y - 8, 184, 74, 0xFFB8B8B8, 0xFF000000);

        List<RecipeSlotSummary> slots = summary.inputSlots();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slotIndex = row * 3 + column;
                int slotX = x + column * 19;
                int slotY = y + row * 19;
                drawSlot(context, slotX, slotY, slotIndex < slots.size() ? slots.get(slotIndex) : RecipeSlotSummary.EMPTY);
            }
        }

        context.method_51433(this.field_22793, "->", x + 72, y + 22, 0xFF777777, false);
        drawSlot(context, x + 112, y + 19, new RecipeSlotSummary(summary.outputIcon(), List.of(summary.outputIcon().method_7964().getString()), summary.outputCount()));
    }

    private void drawSlot(class_332 context, int x, int y, RecipeSlotSummary slot) {
        context.method_25294(x, y, x + 18, y + 18, 0xFFE0E0E0);
        context.method_25294(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        if (!slot.isEmpty()) {
            context.method_51427(slot.icon(), x + 1, y + 1);
            if (slot.count() > 1) {
                context.method_51433(this.field_22793, Integer.toString(slot.count()), x + 11, y + 10, 0xFFFFFFFF, true);
            }
        }
    }

    private int contentHeight() {
        if (this.summaries.isEmpty()) {
            return 36;
        }

        int height = 0;
        for (RecipeSummary summary : this.summaries) {
            height += recipeBoxHeight(summary) + 10;
        }
        return height;
    }

    private static int recipeBoxHeight(RecipeSummary summary) {
        return 150 + summary.ingredients().size() * 20;
    }

    private int maxScroll() {
        int contentTop = 22 + 48 + 12;
        int contentBottom = this.field_22790 - 18;
        return Math.max(0, this.contentHeight() - Math.max(0, contentBottom - contentTop));
    }

    private void renderScrollbar(class_332 context, int top, int bottom) {
        int maxScroll = this.maxScroll();
        if (maxScroll <= 0) {
            return;
        }

        int x = this.field_22789 - 16;
        int height = bottom - top;
        context.method_25294(x, top, x + 6, bottom, 0x66000000);

        int thumbHeight = Math.max(24, height * height / Math.max(height, this.contentHeight()));
        int thumbTop = top + (height - thumbHeight) * this.scrollY / maxScroll;
        context.method_25294(x, thumbTop, x + 6, thumbTop + thumbHeight, 0xFFAAAAAA);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int top = 22 + 48 + 12;
        int bottom = this.field_22790 - 18;
        int x = this.field_22789 - 18;
        return this.maxScroll() > 0 && mouseX >= x && mouseX <= x + 12 && mouseY >= top && mouseY <= bottom;
    }

    private void setScrollFromMouse(double mouseY) {
        int top = 22 + 48 + 12;
        int bottom = this.field_22790 - 18;
        int height = bottom - top;
        int maxScroll = this.maxScroll();
        if (height <= 0 || maxScroll <= 0) {
            this.scrollY = 0;
            return;
        }

        double ratio = (mouseY - top) / (double) height;
        this.scrollY = clamp((int) (ratio * maxScroll), 0, maxScroll);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
