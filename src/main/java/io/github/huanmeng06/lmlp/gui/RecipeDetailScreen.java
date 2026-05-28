package io.github.huanmeng06.lmlp.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.recipe.AlternativeItemDisplay;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeResolvers;
import io.github.huanmeng06.lmlp.recipe.RecipeSlotSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import net.minecraft.class_1799;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_465;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Button;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.category.ButtonArea;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRenderer;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;

public class RecipeDetailScreen extends class_437 {
    private static final Logger LOGGER = LoggerFactory.getLogger(LitematicaMaterialListPlus.MOD_ID);
    private static final int BACK_BUTTON_WIDTH = 112;
    private static final int BACK_BUTTON_HEIGHT = 20;
    private static final int REI_PANEL_WIDTH = 254;
    private static final int REI_PANEL_HEIGHT = 104;
    private static final int INGREDIENT_TEXT_COLOR = 0xFFFFFFFF;
    private static final int INGREDIENT_TOTAL_COLOR = 0xFFFFAA00;
    private static final int INGREDIENT_MISSING_COLOR = 0xFFFF5555;
    private static final int NATIVE_RENDER_CLIP_PADDING = 24;
    private static final int MAX_NATIVE_DISPLAYS_PER_FRAME = 64;
    private static final int MAX_NATIVE_DISPLAY_DEPTH = 3;
    private static final int OUTLINE_CLIP_PADDING = 2;
    private static final int PAGE_MARGIN_X = 24;
    private static final int PAGE_TOP = 22;
    private static final int PAGE_BOTTOM_MARGIN = 20;
    private static final int CONTENT_RIGHT_INSET = 24;
    private static final int HEADER_MAX_WIDTH = 600;
    private static final int HEADER_HEIGHT = 50;
    private static final int HEADER_BUTTON_GAP = 8;
    private static final int INGREDIENT_ROW_HEIGHT = 20;
    private static final int INGREDIENT_TREE_INDENT_WIDTH = 18;
    private static final int INGREDIENT_TOGGLE_WIDTH = 18;
    private static final int INGREDIENT_ICON_OFFSET = 20;
    private static final int NESTED_RECIPE_GAP = 8;
    private static final int NESTED_RECIPE_INDENT = 24;
    private static final int MAX_NESTED_DEPTH = 3;
    private static final class_2960 REI_DISPLAY_TEXTURE = new class_2960("roughlyenoughitems", "textures/gui/display.png");

    private final class_437 parent;
    private final class_1799 target;
    private final int totalCount;
    private final int missingCount;
    private final List<RecipeSummary> summaries;
    private final GuiScrollBar scrollBar = new GuiScrollBar();
    private final ButtonGeneric backButton = new ButtonGeneric(0, 0, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, "");
    private final RecipeNativeDisplayBridge nativeDisplayBridge = createNativeDisplayBridge();
    private final RecipeTooltipBridge tooltipBridge = createTooltipBridge();
    private final List<NativeDisplayArea> nativeDisplayAreas = new ArrayList<>();
    private final List<TransferButtonEntry> transferButtons = new ArrayList<>();
    private final List<ToggleArea> toggleAreas = new ArrayList<>();
    private final Map<String, List<RecipeSummary>> nestedRecipeCache = new HashMap<>();
    private final Set<String> expandedNestedRecipes = new HashSet<>();
    private final Set<String> disabledNativeDisplayCategories = new HashSet<>();
    private class_1799 hoveredStack = class_1799.field_8037;
    private int clipTop;
    private int clipBottom;
    private int nativeDisplaysRenderedThisFrame;
    private boolean nativeDisplayLimitLoggedThisFrame;
    private boolean draggingScrollbar;
    private final class_465<?> transferContainerScreen;

    public RecipeDetailScreen(class_437 parent, class_1799 target, int totalCount, int missingCount, List<RecipeSummary> summaries) {
        super(class_2561.method_43470("lmlp.gui.recipe_detail.title"));
        this.parent = parent;
        this.transferContainerScreen = findHandledParent(parent);
        this.target = target.method_7972();
        this.totalCount = totalCount;
        this.missingCount = missingCount;
        this.summaries = List.copyOf(summaries);
        this.backButton.setDisplayString(StringUtils.translate("lmlp.label.recipe.back"));
        this.backButton.setTextCentered(true);
        this.backButton.setActionListener((button, mouseButton) -> this.method_25419());
    }

    @Override
    public void method_25419() {
        this.field_22787.method_1507(this.parent);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        if (this.dispatchNativeDisplay(area, "mouse scroll", summary -> this.nativeDisplayBridge.mouseScrolled(summary, mouseX, mouseY, verticalAmount))) {
            return true;
        }

        this.scrollBar.offsetValue(-(int) (verticalAmount * 24));
        return true;
    }

    @Override
    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (button == 0 && this.backButton.onMouseClicked((int) mouseX, (int) mouseY, button)) {
            return true;
        }

        if (button == 0 && this.handleTransferButtonClick(mouseX, mouseY)) {
            return true;
        }

        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        if (this.dispatchNativeDisplay(area, "mouse click", summary -> this.nativeDisplayBridge.mouseClicked(summary, mouseX, mouseY, button))) {
            return true;
        }

        if (button == 0 && this.handleIngredientTreeClick(mouseX, mouseY)) {
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

        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        if (this.dispatchNativeDisplay(area, "mouse drag", summary -> this.nativeDisplayBridge.mouseDragged(summary, mouseX, mouseY, button, deltaX, deltaY))) {
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

        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        if (this.dispatchNativeDisplay(area, "mouse release", summary -> this.nativeDisplayBridge.mouseReleased(summary, mouseX, mouseY, button))) {
            return true;
        }

        return super.method_25406(mouseX, mouseY, button);
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        this.method_25420(context, mouseX, mouseY, delta);

        Layout layout = this.layout();
        int contentTop = layout.headerTop() + HEADER_HEIGHT + 12;
        int contentBottom = this.field_22790 - PAGE_BOTTOM_MARGIN;
        int viewportHeight = Math.max(0, contentBottom - contentTop);

        this.hoveredStack = class_1799.field_8037;
        this.clipTop = contentTop;
        this.clipBottom = contentBottom;
        this.nativeDisplaysRenderedThisFrame = 0;
        this.nativeDisplayLimitLoggedThisFrame = false;
        this.nativeDisplayAreas.clear();
        this.transferButtons.clear();
        this.toggleAreas.clear();
        this.scrollBar.setMaxValue(Math.max(0, this.contentHeight() - viewportHeight));
        this.updateBackButtonPosition(layout);

        this.renderBackButton(context, mouseX, mouseY);
        this.renderTargetHeader(context, layout.left(), layout.headerTop(), layout.headerWidth(), HEADER_HEIGHT, mouseX, mouseY);

        int y = contentTop - this.scrollBar.getValue();
        context.method_44379(layout.left() - OUTLINE_CLIP_PADDING, contentTop - OUTLINE_CLIP_PADDING, layout.left() + layout.contentWidth() + OUTLINE_CLIP_PADDING, contentBottom);
        if (this.summaries.isEmpty()) {
            RenderUtils.drawOutlinedBox(layout.left(), y, layout.contentWidth(), 46, 0xDD000000, 0xFF777777);
            context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.none"), layout.left() + 10, y + 17, 0xFFFFCC66, false);
        } else {
            int index = 1;
            for (RecipeSummary summary : this.summaries) {
                String path = "recipe/" + index + ":" + summary.recipeId();
                int boxHeight = this.recipeBoxHeight(summary, path, 0);
                this.renderRecipeBox(context, summary, index, path, 0, layout.left(), y, layout.contentWidth(), boxHeight, mouseX, mouseY, delta);
                y += boxHeight + 10;
                index++;
            }
        }
        context.method_44380();

        this.renderScrollbar(context, mouseX, mouseY, delta, contentTop, contentBottom);
        if (this.renderNativeTooltip(context, mouseX, mouseY)) {
            return;
        }

        if (!this.hoveredStack.method_7960()) {
            if (!this.tooltipBridge.renderTooltip(context, this.field_22793, this.hoveredStack, mouseX, mouseY)) {
                context.method_51446(this.field_22793, this.hoveredStack, mouseX, mouseY);
            }
        }
    }

    private void renderTargetHeader(class_332 context, int left, int top, int width, int height, int mouseX, int mouseY) {
        RenderUtils.drawOutlinedBox(left, top, width, height, 0xDD000000, 0xFF888888);
        context.method_51427(this.target, left + 10, top + 9);
        if (isInside(mouseX, mouseY, left + 10, top + 9, 16, 16)) {
            this.hoveredStack = this.target;
        }
        int textX = left + 36;
        int textRight = Math.max(textX + 1, left + width - 6);
        context.method_44379(textX, top + 4, textRight, top + height - 4);
        context.method_51433(this.field_22793, ItemStackTexts.name(this.target), textX, top + 8, 0xFFFFFFFF, false);

        String total = StringUtils.translate("lmlp.label.recipe.total_short") + ": " + CountFormatter.format(this.target, this.totalCount);
        String missing = StringUtils.translate("lmlp.label.recipe.missing_short") + ": " + CountFormatter.format(this.target, this.missingCount);
        String counts = total + "    " + missing;
        if (this.field_22793.method_1727(counts) <= textRight - textX) {
            context.method_51433(this.field_22793, counts, textX, top + 28, 0xFFAAAAAA, false);
        } else {
            context.method_51433(this.field_22793, total, textX, top + 23, 0xFFAAAAAA, false);
            context.method_51433(this.field_22793, missing, textX, top + 35, 0xFFAAAAAA, false);
        }
        context.method_44380();
    }

    private void renderBackButton(class_332 context, int mouseX, int mouseY) {
        this.backButton.render(mouseX, mouseY, false, context);
    }

    private void renderRecipeBox(class_332 context, RecipeSummary summary, int index, String path, int depth, int left, int y, int width, int boxHeight, int mouseX, int mouseY, float delta) {
        RenderUtils.drawOutlinedBox(left, y, width, boxHeight, 0xDD000000, 0xFF777777);
        context.method_51427(summary.outputIcon(), left + 10, y + 10);
        this.captureHoveredStack(summary.outputIcon(), mouseX, mouseY, left + 10, y + 10, 16, 16);
        context.method_51433(this.field_22793, RecipeSummaryFormatter.header(summary, index), left + 34, y + 12, 0xFFFFFFFF, false);

        int panelWidth = this.displayPanelWidth(summary, width - 36, depth);
        int panelHeight = this.displayPanelHeight(summary, depth);
        int panelX = left + 18;
        int panelY = y + 38;
        if (this.isDisplayPanelVisible(panelY, panelHeight) && !this.renderNativeDisplay(summary, context, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY, delta, path, depth)) {
            this.renderCraftingGrid(context, summary, panelX, panelY, mouseX, mouseY);
        }
        this.renderTransferButton(context, summary, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY, delta);

        int lineY = panelY + panelHeight + 16;
        context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.ingredients_total"), left + 14, lineY, 0xFFAAAAAA, false);
        lineY += 18;

        for (int ingredientIndex = 0; ingredientIndex < summary.ingredients().size(); ingredientIndex++) {
            IngredientSummary ingredient = summary.ingredients().get(ingredientIndex);
            String ingredientPath = path + "/ingredient/" + ingredientIndex + ":" + key(ingredient);
            this.renderIngredientLine(context, left + 14, lineY, depth, ingredientPath, ingredient, mouseX, mouseY);
            lineY += INGREDIENT_ROW_HEIGHT;

            if (this.expandedNestedRecipes.contains(ingredientPath)) {
                lineY = this.renderNestedRecipes(context, ingredient, ingredientPath, depth + 1, left + 14, lineY, width - 28, mouseX, mouseY, delta);
            }
        }
    }

    private void renderIngredientLine(class_332 context, int left, int y, int depth, String path, IngredientSummary ingredient, int mouseX, int mouseY) {
        boolean hasRecipes = depth < MAX_NESTED_DEPTH && this.hasRecipes(ingredient);
        boolean expanded = this.expandedNestedRecipes.contains(path);
        this.renderMaterialLine(
                context,
                left,
                y,
                0,
                path,
                hasRecipes,
                expanded,
                AlternativeItemDisplay.icon(ingredient),
                RecipeSummaryFormatter.ingredientName(ingredient),
                RecipeSummaryFormatter.totalCount(ingredient),
                RecipeSummaryFormatter.missingCount(ingredient),
                ingredient.countMissing() != ingredient.countTotal(),
                mouseX,
                mouseY);
    }

    private int renderNestedRecipes(class_332 context, IngredientSummary ingredient, String parentPath, int depth, int left, int y, int width, int mouseX, int mouseY, float delta) {
        int lineY = y;
        List<RecipeSummary> recipes = this.recipesFor(ingredient);
        int nestedLeft = left + NESTED_RECIPE_INDENT;
        int nestedWidth = Math.max(180, width - NESTED_RECIPE_INDENT);
        for (int recipeIndex = 0; recipeIndex < recipes.size(); recipeIndex++) {
            RecipeSummary recipe = recipes.get(recipeIndex);
            String path = parentPath + "/recipe/" + (recipeIndex + 1) + ":" + recipe.recipeId();
            int height = this.recipeBoxHeight(recipe, path, depth);
            this.renderRecipeBox(context, recipe, recipeIndex + 1, path, depth, nestedLeft, lineY, nestedWidth, height, mouseX, mouseY, delta);
            lineY += height + NESTED_RECIPE_GAP;
        }

        return lineY;
    }

    private void renderMaterialLine(class_332 context, int left, int y, int depth, String path, boolean hasRecipes, boolean expanded, class_1799 icon, String name, String totalText, String missingText, boolean showMissing, int mouseX, int mouseY) {
        int rowX = left + depth * INGREDIENT_TREE_INDENT_WIDTH;
        int iconX = rowX + INGREDIENT_ICON_OFFSET;
        int iconY = y - 5;
        if (hasRecipes) {
            boolean hovered = isInside(mouseX, mouseY, rowX, y - 2, INGREDIENT_TOGGLE_WIDTH, INGREDIENT_ROW_HEIGHT);
            ToggleArrowRenderer.render(context, rowX, INGREDIENT_TOGGLE_WIDTH, iconY + 8, expanded, hovered);
            this.toggleAreas.add(new ToggleArea(path, rowX, y - 2, INGREDIENT_TOGGLE_WIDTH, INGREDIENT_ROW_HEIGHT));
        }

        context.method_51427(icon, iconX, iconY);
        this.captureHoveredStack(icon, mouseX, mouseY, iconX, iconY, 16, 16);

        int textX = rowX + INGREDIENT_ICON_OFFSET + 24;
        String prefix = name + ": ";
        context.method_51433(this.field_22793, prefix, textX, y, INGREDIENT_TEXT_COLOR, false);
        textX += this.field_22793.method_1727(prefix);
        context.method_51433(this.field_22793, totalText, textX, y, INGREDIENT_TOTAL_COLOR, false);
        textX += this.field_22793.method_1727(totalText);
        if (showMissing) {
            context.method_51433(this.field_22793, " / ", textX, y, INGREDIENT_TEXT_COLOR, false);
            textX += this.field_22793.method_1727(" / ");
            context.method_51433(this.field_22793, missingText, textX, y, INGREDIENT_MISSING_COLOR, false);
        }
    }

    private void renderCraftingGrid(class_332 context, RecipeSummary summary, int x, int y, int mouseX, int mouseY) {
        RenderUtils.drawRect(x, y, REI_PANEL_WIDTH, REI_PANEL_HEIGHT, 0xFFB8B8B8);
        RenderUtils.drawOutlinedBox(x, y, REI_PANEL_WIDTH, REI_PANEL_HEIGHT, 0x00FFFFFF, 0xFF000000);
        RenderUtils.drawRect(x + 2, y + 2, REI_PANEL_WIDTH - 4, 1, 0xFFFFFFFF);
        RenderUtils.drawRect(x + 2, y + 2, 1, REI_PANEL_HEIGHT - 4, 0xFFFFFFFF);

        List<RecipeSlotSummary> slots = summary.inputSlots();
        int gridX = x + 34;
        int gridY = y + 24;
        context.method_25290(REI_DISPLAY_TEXTURE, gridX, gridY, 0.0F, 0.0F, 54, 54, 256, 256);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slotIndex = row * 3 + column;
                int slotX = gridX + column * 18;
                int slotY = gridY + row * 18;
                drawSlotItem(context, slotX + 1, slotY + 1, slotIndex < slots.size() ? slots.get(slotIndex) : RecipeSlotSummary.EMPTY, mouseX, mouseY, 18, 18);
            }
        }

        context.method_25290(REI_DISPLAY_TEXTURE, x + 122, y + 44, 60.0F, 18.0F, 24, 15, 256, 256);

        int outputX = x + 166;
        int outputY = y + 35;
        drawOutputSlot(context, outputX, outputY);
        drawSlotItem(context, outputX + 5, outputY + 5, new RecipeSlotSummary(summary.outputIcon(), List.of(ItemStackTexts.name(summary.outputIcon())), summary.outputCount()), mouseX, mouseY, 26, 26);
    }

    private void drawOutputSlot(class_332 context, int x, int y) {
        context.method_25294(x, y, x + 26, y + 26, 0xFFFFFFFF);
        context.method_25294(x + 1, y + 1, x + 25, y + 25, 0xFFE0E0E0);
        context.method_25294(x + 2, y + 2, x + 24, y + 24, 0xFFB8B8B8);
    }

    private void drawSlotItem(class_332 context, int x, int y, RecipeSlotSummary slot, int mouseX, int mouseY, int hoverWidth, int hoverHeight) {
        if (!slot.isEmpty()) {
            class_1799 icon = AlternativeItemDisplay.icon(slot).method_7972();
            icon.method_7939(Math.max(1, slot.count()));
            context.method_51427(icon, x + 1, y + 1);
            context.method_51431(this.field_22793, icon, x + 1, y + 1);
            this.captureHoveredStack(icon, mouseX, mouseY, x, y, hoverWidth, hoverHeight);
        }
    }

    private boolean handleIngredientTreeClick(double mouseX, double mouseY) {
        for (int i = this.toggleAreas.size() - 1; i >= 0; i--) {
            ToggleArea area = this.toggleAreas.get(i);
            if (area.contains(mouseX, mouseY)) {
                this.toggleNestedRecipes(area.path());
                return true;
            }
        }

        return false;
    }

    private boolean handleTransferButtonClick(double mouseX, double mouseY) {
        for (int i = this.transferButtons.size() - 1; i >= 0; i--) {
            TransferButtonEntry entry = this.transferButtons.get(i);
            if (entry.button().method_25402(mouseX, mouseY, 0)) {
                return true;
            }
        }

        return false;
    }

    private void renderTransferButton(class_332 context, RecipeSummary summary, int panelX, int panelY, int panelWidth, int panelHeight, int mouseX, int mouseY, float delta) {
        if (!this.canTransfer(summary)) {
            return;
        }

        Display display = displayFor(summary);
        Rectangle displayBounds = new Rectangle(panelX, panelY, panelWidth, panelHeight);
        Optional<ButtonArea> buttonArea = this.transferButtonArea(display);
        if (buttonArea.isEmpty()) {
            return;
        }

        Rectangle buttonBounds = buttonArea.get().get(displayBounds);
        if (buttonBounds.getMaxY() < this.clipTop || buttonBounds.y > this.clipBottom) {
            return;
        }

        AutoCraftingState state = this.evaluateTransfer(display, false, false);
        Button button = Widgets.createButton(buttonBounds, state.hasApplicable() ? class_2561.method_43470(buttonArea.get().getButtonText()) : class_2561.method_43470("!"))
                .focusable(false)
                .onClick(ignored -> this.transferRecipe(summary, GuiBase.isShiftDown()));
        button.setEnabled(state.successful());
        button.setTint(state.tint());

        if (state.hasApplicable()
                && state.renderer() != null
                && (isInside(mouseX, mouseY, buttonBounds.x, buttonBounds.y, buttonBounds.width, buttonBounds.height) || button.method_25370())) {
            state.renderer().render(context, mouseX, mouseY, delta, this.setupDisplayWidgets(display, displayBounds), displayBounds, display);
        }

        button.method_25394(context, mouseX, mouseY, delta);
        if (isInside(mouseX, mouseY, buttonBounds.x, buttonBounds.y, buttonBounds.width, buttonBounds.height) && !button.method_25370()) {
            Tooltip.from(new me.shedaniel.math.Point(mouseX, mouseY), state.tooltip()).queue();
        }
        this.transferButtons.add(new TransferButtonEntry(button, state));
    }

    private boolean canTransfer(RecipeSummary summary) {
        return this.transferContainerScreen != null && displayFor(summary) != null;
    }

    private void transferRecipe(RecipeSummary summary, boolean stackedCrafting) {
        Display display = displayFor(summary);
        if (display == null || this.transferContainerScreen == null) {
            return;
        }

        AutoCraftingState state = this.evaluateTransfer(display, true, stackedCrafting);
        if (state.successful()) {
            this.field_22787.method_1507(this.transferContainerScreen);
        }
    }

    private AutoCraftingState evaluateTransfer(Display display, boolean actuallyCrafting, boolean stackedCrafting) {
        List<TransferHandler.Result> errors = new ArrayList<>();
        List<Tooltip.Entry> tooltip = new ArrayList<>();
        TransferHandler.Result successfulResult = null;
        boolean successful = false;
        boolean hasApplicable = false;
        int tint = 0;
        TransferHandlerRenderer renderer = null;
        TransferHandler.Context context = TransferHandler.Context.create(actuallyCrafting, stackedCrafting, this.transferContainerScreen, display);

        for (TransferHandler handler : TransferHandlerRegistry.getInstance()) {
            TransferHandler.Result result;
            try {
                TransferHandler.ApplicabilityResult applicability = handler.checkApplicable(context);
                if (!applicability.isApplicable()) {
                    continue;
                }

                result = applicability.isSuccessful() ? handler.handle(context) : applicability.getError();
            } catch (Throwable throwable) {
                LOGGER.warn("REI transfer evaluation failed for display {}.", display.getDisplayLocation().map(class_2960::toString).orElse("<unknown>"), throwable);
                continue;
            }

            if (result == null) {
                continue;
            }

            if (result.isBlocking() && actuallyCrafting) {
                if (result.isReturningToScreen()) {
                    this.field_22787.method_1507(this.transferContainerScreen);
                }
                break;
            }

            if (result.isApplicable()) {
                hasApplicable = true;
                tint = result.getColor();
                TransferHandlerRenderer resultRenderer = result.getRenderer(handler, context);
                if (resultRenderer != null) {
                    renderer = resultRenderer;
                }

                if (result.isSuccessful()) {
                    successful = true;
                    successfulResult = result;
                    errors.clear();
                    break;
                }

                errors.add(result);
                if (result.isBlocking()) {
                    break;
                }
            }
        }

        if (!hasApplicable) {
            tooltip.add(Tooltip.entry(class_2561.method_43471("error.rei.not.supported.move.items").method_27692(class_124.field_1061)));
        } else if (errors.isEmpty()) {
            tooltip.add(Tooltip.entry(class_2561.method_43471("text.auto_craft.move_items")));
            if (successfulResult != null) {
                successfulResult.fillTooltip(tooltip);
            }
        } else {
            for (TransferHandler.Result error : errors) {
                error.fillTooltip(tooltip);
            }
        }

        this.addRecipeIdTooltip(display, tooltip);
        return new AutoCraftingState(successful, hasApplicable, tint, renderer, tooltip);
    }

    private void addRecipeIdTooltip(Display display, List<Tooltip.Entry> tooltip) {
        if (this.field_22787 == null || this.field_22787.field_1690 == null || !this.field_22787.field_1690.field_1827) {
            return;
        }

        for (class_2960 id : display.provideInternalDisplayIds()) {
            tooltip.add(Tooltip.entry(class_2561.method_43469("text.rei.recipe_id",
                    class_2561.method_43470(id.method_12836()).method_27692(class_124.field_1080),
                    class_2561.method_43470(id.method_12832()).method_27692(class_124.field_1080))));
        }
    }

    private Optional<ButtonArea> transferButtonArea(Display display) {
        try {
            CategoryIdentifier<?> categoryId = display.getCategoryIdentifier();
            return CategoryRegistry.getInstance().tryGet(categoryId.cast())
                    .flatMap(CategoryRegistry.CategoryConfiguration::getPlusButtonArea)
                    .or(() -> Optional.of(ButtonArea.defaultArea()));
        } catch (Throwable throwable) {
            return Optional.of(ButtonArea.defaultArea());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Widget> setupDisplayWidgets(Display display, Rectangle displayBounds) {
        try {
            DisplayCategory category = CategoryRegistry.getInstance().get(display.getCategoryIdentifier().cast()).getCategory();
            return category.setupDisplay(display, displayBounds);
        } catch (Throwable throwable) {
            return List.of();
        }
    }

    private int contentHeight() {
        if (this.summaries.isEmpty()) {
            return 48;
        }

        int height = 0;
        int index = 1;
        for (RecipeSummary summary : this.summaries) {
            height += this.recipeBoxHeight(summary, "recipe/" + index + ":" + summary.recipeId(), 0) + 10;
            index++;
        }
        return height;
    }

    private int recipeBoxHeight(RecipeSummary summary, String path, int depth) {
        return 72 + this.displayPanelHeight(summary, depth) + this.ingredientsHeight(summary, path, depth);
    }

    private int ingredientsHeight(RecipeSummary summary, String path, int depth) {
        int height = 0;
        for (int ingredientIndex = 0; ingredientIndex < summary.ingredients().size(); ingredientIndex++) {
            IngredientSummary ingredient = summary.ingredients().get(ingredientIndex);
            String ingredientPath = path + "/ingredient/" + ingredientIndex + ":" + key(ingredient);
            height += INGREDIENT_ROW_HEIGHT;
            if (this.expandedNestedRecipes.contains(ingredientPath)) {
                height += this.nestedRecipesHeight(ingredient, ingredientPath, depth + 1);
            }
        }

        return height;
    }

    private int nestedRecipesHeight(IngredientSummary ingredient, String parentPath, int depth) {
        int height = 0;
        List<RecipeSummary> recipes = this.recipesFor(ingredient);
        for (int recipeIndex = 0; recipeIndex < recipes.size(); recipeIndex++) {
            RecipeSummary recipe = recipes.get(recipeIndex);
            String path = parentPath + "/recipe/" + (recipeIndex + 1) + ":" + recipe.recipeId();
            height += this.recipeBoxHeight(recipe, path, depth) + NESTED_RECIPE_GAP;
        }

        return height;
    }

    private boolean hasRecipes(IngredientSummary ingredient) {
        if (Configs.shouldStopRecipeDecomposition(ItemStackTexts.id(ingredient.icon()))) {
            return false;
        }

        return !this.recipesFor(ingredient).isEmpty();
    }

    private List<RecipeSummary> recipesFor(IngredientSummary ingredient) {
        if (Configs.shouldStopRecipeDecomposition(ItemStackTexts.id(ingredient.icon()))) {
            return List.of();
        }

        String key = key(ingredient);
        return this.nestedRecipeCache.computeIfAbsent(key, ignored -> RecipeResolvers.findRecipes(ingredient.icon(), ingredient.countTotal(), ingredient.countMissing()));
    }

    private void toggleNestedRecipes(String path) {
        if (this.expandedNestedRecipes.contains(path)) {
            this.collapseNestedRecipes(path);
        } else {
            this.expandedNestedRecipes.add(path);
        }
    }

    private void collapseNestedRecipes(String path) {
        this.expandedNestedRecipes.removeIf(expandedPath -> expandedPath.equals(path) || expandedPath.startsWith(path + "/"));
    }

    private static String key(IngredientSummary ingredient) {
        return ItemStackTexts.id(ingredient.icon()) + "|" + ingredient.countTotal() + "|" + ingredient.countMissing();
    }

    private int displayPanelWidth(RecipeSummary summary, int maxWidth, int depth) {
        int boundedMaxWidth = Math.max(1, maxWidth);
        if (this.canUseNativeLayout(summary, depth)) {
            int fallbackWidth = Math.min(REI_PANEL_WIDTH, boundedMaxWidth);
            try {
                int nativeWidth = this.nativeDisplayBridge.getDisplayWidth(summary, fallbackWidth);
                return Math.min(Math.max(1, nativeWidth), boundedMaxWidth);
            } catch (Throwable throwable) {
                this.disableNativeDisplay(summary, "display width", throwable);
            }
        }

        return Math.min(REI_PANEL_WIDTH, boundedMaxWidth);
    }

    private int displayPanelHeight(RecipeSummary summary, int depth) {
        if (this.canUseNativeLayout(summary, depth)) {
            try {
                return Math.max(1, this.nativeDisplayBridge.getDisplayHeight(summary, REI_PANEL_HEIGHT));
            } catch (Throwable throwable) {
                this.disableNativeDisplay(summary, "display height", throwable);
            }
        }

        return REI_PANEL_HEIGHT;
    }

    private boolean renderNativeDisplay(RecipeSummary summary, class_332 context, int x, int y, int width, int height, int mouseX, int mouseY, float delta, String path, int depth) {
        if (!this.shouldRenderNativeDisplay(summary, depth)) {
            return false;
        }

        int displayIndex = this.nativeDisplaysRenderedThisFrame + 1;
        LOGGER.debug("Rendering native REI display recipe={} category={} path={} depth={} bounds={}x{} at {},{} frameDisplay={}/{}",
                summary.recipeId(),
                summary.category(),
                path,
                depth,
                width,
                height,
                x,
                y,
                displayIndex,
                MAX_NATIVE_DISPLAYS_PER_FRAME);
        try {
            this.nativeDisplayBridge.render(summary, context, x, y, width, height, mouseX, mouseY, delta);
            this.nativeDisplaysRenderedThisFrame = displayIndex;
            this.nativeDisplayAreas.add(new NativeDisplayArea(summary, x, y, width, height));
            return true;
        } catch (Throwable throwable) {
            this.disableNativeDisplay(summary, "render", throwable);
            return false;
        }
    }

    private void renderScrollbar(class_332 context, int mouseX, int mouseY, float delta, int top, int bottom) {
        if (this.scrollBar.getMaxValue() <= 0) {
            return;
        }

        this.scrollBar.render(mouseX, mouseY, delta, this.scrollbarX(), top, 8, bottom - top, this.contentHeight());
    }

    private void captureHoveredStack(class_1799 stack, int mouseX, int mouseY, int x, int y, int width, int height) {
        if (mouseY >= this.clipTop && mouseY <= this.clipBottom && isInside(mouseX, mouseY, x, y, width, height)) {
            this.hoveredStack = stack;
        }
    }

    private boolean renderNativeTooltip(class_332 context, int mouseX, int mouseY) {
        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        return this.dispatchNativeDisplay(area, "tooltip", summary -> this.nativeDisplayBridge.renderTooltip(summary, context, this.field_22793, mouseX, mouseY));
    }

    private boolean canUseNativeLayout(RecipeSummary summary, int depth) {
        if (depth > MAX_NATIVE_DISPLAY_DEPTH || this.isNativeDisplayDisabled(summary)) {
            return false;
        }

        try {
            return this.nativeDisplayBridge.canRender(summary);
        } catch (Throwable throwable) {
            this.disableNativeDisplay(summary, "availability check", throwable);
            return false;
        }
    }

    private boolean shouldRenderNativeDisplay(RecipeSummary summary, int depth) {
        if (!this.canUseNativeLayout(summary, depth)) {
            return false;
        }

        if (this.nativeDisplaysRenderedThisFrame >= MAX_NATIVE_DISPLAYS_PER_FRAME) {
            if (!this.nativeDisplayLimitLoggedThisFrame) {
                this.nativeDisplayLimitLoggedThisFrame = true;
                LOGGER.debug("Native REI display frame limit reached; falling back after {} displays.", MAX_NATIVE_DISPLAYS_PER_FRAME);
            }
            return false;
        }

        return true;
    }

    private boolean isDisplayPanelVisible(int y, int height) {
        return y + height >= this.clipTop - NATIVE_RENDER_CLIP_PADDING && y <= this.clipBottom + NATIVE_RENDER_CLIP_PADDING;
    }

    private boolean dispatchNativeDisplay(NativeDisplayArea area, String action, NativeDisplayAction displayAction) {
        if (area == null || this.isNativeDisplayDisabled(area.summary())) {
            return false;
        }

        try {
            return displayAction.apply(area.summary());
        } catch (Throwable throwable) {
            this.disableNativeDisplay(area.summary(), action, throwable);
            return false;
        }
    }

    private boolean isNativeDisplayDisabled(RecipeSummary summary) {
        return this.disabledNativeDisplayCategories.contains(nativeDisplayCategory(summary));
    }

    private void disableNativeDisplay(RecipeSummary summary, String action, Throwable throwable) {
        String category = nativeDisplayCategory(summary);
        if (this.disabledNativeDisplayCategories.add(category)) {
            LOGGER.warn("Disabling native REI display rendering for category {} after {} failed on recipe {}.", category, action, summary.recipeId(), throwable);
        }
    }

    private static String nativeDisplayCategory(RecipeSummary summary) {
        return summary.category() == null ? "<unknown>" : summary.category();
    }

    private static Display displayFor(RecipeSummary summary) {
        Object nativeDisplay = summary.nativeDisplay();
        return nativeDisplay instanceof Display display ? display : null;
    }

    private int scrollbarX() {
        return this.field_22789 - 18;
    }

    private Layout layout() {
        int left = PAGE_MARGIN_X;
        int availableWidth = Math.max(1, this.field_22789 - PAGE_MARGIN_X * 2);
        int contentWidth = Math.max(1, availableWidth - CONTENT_RIGHT_INSET);
        int contentRight = left + contentWidth;
        int backButtonX = Math.max(left, contentRight - BACK_BUTTON_WIDTH);
        int headerWidth = Math.min(HEADER_MAX_WIDTH, Math.max(1, backButtonX - left - HEADER_BUTTON_GAP));
        return new Layout(left, PAGE_TOP, contentWidth, headerWidth, backButtonX);
    }

    private void updateBackButtonPosition(Layout layout) {
        this.backButton.setPosition(layout.backButtonX(), layout.headerTop());
    }

    private NativeDisplayArea nativeDisplayAreaAt(double mouseX, double mouseY) {
        for (int i = this.nativeDisplayAreas.size() - 1; i >= 0; i--) {
            NativeDisplayArea area = this.nativeDisplayAreas.get(i);
            if (area.contains(mouseX, mouseY)) {
                return area;
            }
        }

        return null;
    }

    private static RecipeNativeDisplayBridge createNativeDisplayBridge() {
        try {
            Class<?> bridgeClass = Class.forName("io.github.huanmeng06.lmlp.recipe.rei.ReiNativeDisplayBridge");
            return (RecipeNativeDisplayBridge) bridgeClass.getDeclaredConstructor().newInstance();
        } catch (Throwable throwable) {
            return RecipeNativeDisplayBridge.DISABLED;
        }
    }

    private static RecipeTooltipBridge createTooltipBridge() {
        try {
            Class<?> bridgeClass = Class.forName("io.github.huanmeng06.lmlp.recipe.rei.ReiTooltipBridge");
            return (RecipeTooltipBridge) bridgeClass.getDeclaredConstructor().newInstance();
        } catch (Throwable throwable) {
            return RecipeTooltipBridge.DISABLED;
        }
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static class_465<?> findHandledParent(class_437 screen) {
        class_437 current = screen;
        while (current != null) {
            if (current instanceof class_465<?> handledScreen) {
                return handledScreen;
            }

            if (current instanceof GuiBase gui) {
                current = gui.getParent();
            } else {
                return null;
            }
        }

        return null;
    }

    private record ToggleArea(String path, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private record NativeDisplayArea(RecipeSummary summary, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private record TransferButtonEntry(Button button, AutoCraftingState state) {
    }

    private record AutoCraftingState(boolean successful, boolean hasApplicable, int tint, TransferHandlerRenderer renderer, List<Tooltip.Entry> tooltip) {
    }
    @FunctionalInterface
    private interface NativeDisplayAction {
        boolean apply(RecipeSummary summary);
    }

    private record Layout(int left, int headerTop, int contentWidth, int headerWidth, int backButtonX) {
    }
}
