package io.github.huanmeng06.lmlp.gui;

import java.util.List;

import fi.dy.masa.malilib.gui.GuiScrollBar;
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
    private static final int BACK_BUTTON_WIDTH = 104;
    private static final int BACK_BUTTON_HEIGHT = 24;

    private final class_437 parent;
    private final class_1799 target;
    private final int totalCount;
    private final int missingCount;
    private final List<RecipeSummary> summaries;
    private final GuiScrollBar scrollBar = new GuiScrollBar();
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
        this.scrollBar.offsetValue(-(int) (amount * 24));
        return true;
    }

    @Override
    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isOverBackButton(mouseX, mouseY)) {
            this.method_25419();
            return true;
        }

        if (button == 0 && this.scrollBar.wasMouseOver()) {
            this.scrollBar.setIsDragging(true);
            this.draggingScrollbar = true;
            return true;
        }

        return super.method_25402(mouseX, mouseY, button);
    }

    @Override
    public boolean method_25403(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && this.draggingScrollbar) {
            return true;
        }

        return super.method_25403(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean method_25406(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.scrollBar.setIsDragging(false);
            this.draggingScrollbar = false;
        }

        return super.method_25406(mouseX, mouseY, button);
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        this.method_25420(context);

        int left = 24;
        int headerTop = 22;
        int width = this.field_22789 - 48;
        int headerHeight = 50;
        int contentTop = headerTop + headerHeight + 12;
        int contentBottom = this.field_22790 - 20;
        int viewportHeight = Math.max(0, contentBottom - contentTop);

        this.scrollBar.setMaxValue(Math.max(0, this.contentHeight() - viewportHeight));

        this.renderBackButton(context, mouseX, mouseY);
        this.renderTargetHeader(context, left, headerTop, Math.min(width, 600), headerHeight);

        int y = contentTop - this.scrollBar.getValue();
        context.method_44379(left, contentTop, left + width - 20, contentBottom);
        if (this.summaries.isEmpty()) {
            RenderUtils.drawOutlinedBox(left, y, width - 24, 46, 0xDD000000, 0xFF777777);
            context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.none"), left + 10, y + 17, 0xFFFFCC66, false);
        } else {
            int index = 1;
            for (RecipeSummary summary : this.summaries) {
                int boxHeight = recipeBoxHeight(summary);
                this.renderRecipeBox(context, summary, index, left, y, width - 24, boxHeight);
                y += boxHeight + 10;
                index++;
            }
        }
        context.method_44380();

        this.renderScrollbar(context, mouseX, mouseY, delta, contentTop, contentBottom);
    }

    private void renderTargetHeader(class_332 context, int left, int top, int width, int height) {
        RenderUtils.drawOutlinedBox(left, top, width, height, 0xDD000000, 0xFF888888);
        context.method_51427(this.target, left + 10, top + 9);
        context.method_51431(this.field_22793, this.target, left + 10, top + 9);
        context.method_51433(this.field_22793, ItemStackTexts.name(this.target), left + 36, top + 10, 0xFFFFFFFF, false);
        String counts = StringUtils.translate("lmlp.label.recipe.total_short") + ": " + CountFormatter.format(this.target, this.totalCount)
                + "    " + StringUtils.translate("lmlp.label.recipe.missing_short") + ": " + CountFormatter.format(this.target, this.missingCount);
        context.method_51433(this.field_22793, counts, left + 36, top + 28, 0xFFAAAAAA, false);
    }

    private void renderBackButton(class_332 context, int mouseX, int mouseY) {
        int x = this.backButtonX();
        int y = 10;
        boolean hovered = this.isOverBackButton(mouseX, mouseY);
        RenderUtils.drawOutlinedBox(x, y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, hovered ? 0xEE3A3A3A : 0xDD000000, hovered ? 0xFFFFFFFF : 0xFFAAAAAA);
        context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.back"), x + 11, y + 8, 0xFFFFFFFF, false);
    }

    private void renderRecipeBox(class_332 context, RecipeSummary summary, int index, int left, int y, int width, int boxHeight) {
        RenderUtils.drawOutlinedBox(left, y, width, boxHeight, 0xDD000000, 0xFF777777);
        context.method_51427(summary.outputIcon(), left + 10, y + 10);
        context.method_51433(this.field_22793, RecipeSummaryFormatter.header(summary, index), left + 34, y + 12, 0xFFFFFFFF, false);
        context.method_51433(this.field_22793, RecipeSummaryFormatter.recipeKind(summary), left + 34, y + 27, 0xFFAAAAAA, false);

        int gridX = left + 28;
        int gridY = y + 50;
        this.renderCraftingGrid(context, summary, gridX, gridY);

        int lineY = gridY + 96;
        context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.ingredients_total"), left + 14, lineY, 0xFFAAAAAA, false);
        lineY += 18;

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
        RenderUtils.drawOutlinedBox(x - 10, y - 10, 214, 86, 0xFFB8B8B8, 0xFF000000);

        List<RecipeSlotSummary> slots = summary.inputSlots();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slotIndex = row * 3 + column;
                int slotX = x + column * 19;
                int slotY = y + row * 19;
                drawSlot(context, slotX, slotY, slotIndex < slots.size() ? slots.get(slotIndex) : RecipeSlotSummary.EMPTY, true);
            }
        }

        context.method_51433(this.field_22793, "=>", x + 78, y + 24, 0xFF777777, false);

        int outputX = x + 122;
        int outputY = y + 19;
        drawSlot(context, outputX, outputY, new RecipeSlotSummary(summary.outputIcon(), List.of(ItemStackTexts.name(summary.outputIcon())), summary.outputCount()), false);
        if (summary.outputCount() > 1) {
            context.method_51433(this.field_22793, "x" + summary.outputCount(), outputX + 24, outputY + 6, 0xFFFFFFFF, false);
        }
    }

    private void drawSlot(class_332 context, int x, int y, RecipeSlotSummary slot, boolean drawCountInside) {
        context.method_25294(x, y, x + 18, y + 18, 0xFFE0E0E0);
        context.method_25294(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        if (!slot.isEmpty()) {
            class_1799 icon = slot.icon().method_7972();
            icon.method_7939(1);
            context.method_51427(icon, x + 1, y + 1);
            if (drawCountInside && slot.count() > 1) {
                context.method_51433(this.field_22793, Integer.toString(slot.count()), x + 11, y + 10, 0xFFFFFFFF, true);
            }
        }
    }

    private int contentHeight() {
        if (this.summaries.isEmpty()) {
            return 48;
        }

        int height = 0;
        for (RecipeSummary summary : this.summaries) {
            height += recipeBoxHeight(summary) + 10;
        }
        return height;
    }

    private static int recipeBoxHeight(RecipeSummary summary) {
        return 160 + summary.ingredients().size() * 20;
    }

    private void renderScrollbar(class_332 context, int mouseX, int mouseY, float delta, int top, int bottom) {
        if (this.scrollBar.getMaxValue() <= 0) {
            return;
        }

        this.scrollBar.render(mouseX, mouseY, delta, this.scrollbarX(), top, 8, bottom - top, this.contentHeight());
    }

    private boolean isOverBackButton(double mouseX, double mouseY) {
        int x = this.backButtonX();
        return mouseX >= x && mouseX <= x + BACK_BUTTON_WIDTH && mouseY >= 10 && mouseY <= 10 + BACK_BUTTON_HEIGHT;
    }

    private int scrollbarX() {
        return this.field_22789 - 18;
    }

    private int backButtonX() {
        return this.field_22789 - BACK_BUTTON_WIDTH - 16;
    }
}
