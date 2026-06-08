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
    private static final int SCREEN_BACKGROUND_COLOR = 0xB0000000;
    private static final int NATIVE_RENDER_CLIP_PADDING = 24;
    private static final int MAX_NATIVE_DISPLAYS_PER_FRAME = 64;
    private static final int MAX_NATIVE_DISPLAY_DEPTH = 3;
    private static final int OUTLINE_CLIP_PADDING = 2;
    private static final int TITLE_Y = 10;
    private static final int TITLE_HEIGHT = 9;
    private static final int TITLE_CONTENT_GAP = 10;
    private static final int CONTENT_X = 20;
    private static final int PAGE_MARGIN_X = CONTENT_X;
    private static final int PAGE_TOP = TITLE_Y + TITLE_HEIGHT + TITLE_CONTENT_GAP;
    private static final int PAGE_BOTTOM_MARGIN = 20;
    private static final int CONTENT_RIGHT_INSET = 24;
    private static final int HEADER_MAX_WIDTH = 600;
    private static final int HEADER_HEIGHT = 50;
    private static final int HEADER_BUTTON_GAP = 8;
    private static final int PREFERRED_BUTTON_SIZE = 18;
    private static final int PREFERRED_ICON_SIZE = 16;
    private static final int PREFERRED_WIDGET_TEXTURE_SIZE = 256;
    private static final int INGREDIENT_ROW_HEIGHT = 20;
    private static final int INGREDIENT_TREE_INDENT_WIDTH = 18;
    private static final int INGREDIENT_TOGGLE_WIDTH = 18;
    private static final int INGREDIENT_ICON_OFFSET = 20;
    private static final int NESTED_RECIPE_GAP = 8;
    private static final int NESTED_RECIPE_INDENT = 24;
    private static final int MAX_NESTED_DEPTH = 3;
    private static final int WHEEL_SCROLL_PIXELS = 36;
    private static final String ROOT_RECIPE_LIST = "root";
    private static final class_2960 REI_DISPLAY_TEXTURE = new class_2960("roughlyenoughitems", "textures/gui/display.png");
    private static final class_2960 PREFERRED_WIDGETS_TEXTURE = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/gui_widgets.png");

    private final class_437 parent;
    private final class_1799 target;
    private final int totalCount;
    private final int missingCount;
    private List<RecipeSummary> summaries;
    private final GuiScrollBar scrollBar = new GuiScrollBar();
    private final ButtonGeneric backButton = new ButtonGeneric(0, 0, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, "");
    private final RecipeNativeDisplayBridge nativeDisplayBridge = createNativeDisplayBridge();
    private final List<NativeDisplayArea> nativeDisplayAreas = new ArrayList<>();
    private final List<TransferButtonEntry> transferButtons = new ArrayList<>();
    private final List<PreferredRecipeButtonArea> preferredRecipeButtons = new ArrayList<>();
    private final List<ToggleArea> toggleAreas = new ArrayList<>();
    private final Map<String, List<RecipeSummary>> nestedRecipeCache = new HashMap<>();
    private final Map<String, RecipeListSnapshot> recipeListSnapshots = new HashMap<>();
    private final Set<String> expandedNestedRecipes = new HashSet<>();
    private final ExpandAnimationTracker nestedRecipeAnimations = new ExpandAnimationTracker();
    private final Map<String, RecipeReorderAnimation> recipeReorderAnimations = new HashMap<>();
    private final Set<String> disabledNativeDisplayCategories = new HashSet<>();
    private class_1799 hoveredStack = class_1799.field_8037;
    private int clipTop;
    private int clipBottom;
    private int activeClipTop = Integer.MIN_VALUE;
    private int activeClipBottom = Integer.MAX_VALUE;
    private int nativeDisplaysRenderedThisFrame;
    private boolean nativeDisplayLimitLoggedThisFrame;
    private boolean draggingScrollbar;
    private double scrollRemainder;
    private final class_465<?> transferContainerScreen;
    private List<Tooltip.Entry> hoveredTransferTooltip = List.of();
    private List<class_2561> hoveredPreferredRecipeTooltip = List.of();

    public RecipeDetailScreen(class_437 parent, class_1799 target, int totalCount, int missingCount, List<RecipeSummary> summaries) {
        super(class_2561.method_43471("lmlp.gui.recipe_detail.title"));
        this.parent = parent;
        this.transferContainerScreen = findHandledParent(parent);
        this.target = target.method_7972();
        this.totalCount = totalCount;
        this.missingCount = missingCount;
        this.summaries = RecipeResolvers.applyPreferredOrder(List.copyOf(summaries));
        this.backButton.setDisplayString(StringUtils.translate("lmlp.label.recipe.back"));
        this.backButton.setTextCentered(true);
        this.backButton.setActionListener((button, mouseButton) -> this.method_25419());
    }

    @Override
    public void method_25419() {
        MaterialListOpener.forgetHandledScreenOverlay(this);
        this.field_22787.method_1507(this.parent);
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    @Override
    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (MaterialListHotkeyMatcher.matches(keyCode) && this.transferContainerScreen != null) {
            MaterialListOpener.rememberHandledScreenOverlay(this);
            this.field_22787.method_1507(this.transferContainerScreen);
            return true;
        }

        return super.method_25404(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        if (this.dispatchNativeDisplay(area, "mouse scroll", summary -> this.nativeDisplayBridge.mouseScrolled(summary, mouseX, mouseY, verticalAmount))) {
            return true;
        }

        this.offsetScroll(verticalAmount);
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

        if (button == 0 && this.handlePreferredRecipeButtonClick(mouseX, mouseY)) {
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
        RenderUtils.drawRect(0, 0, this.field_22789, this.field_22790, SCREEN_BACKGROUND_COLOR);
        this.nestedRecipeAnimations.prune();
        this.recipeReorderAnimations.entrySet().removeIf(entry -> entry.getValue().isFinished());

        Layout layout = this.layout();
        int contentTop = layout.headerTop() + HEADER_HEIGHT + 12;
        int contentBottom = this.field_22790 - PAGE_BOTTOM_MARGIN;
        int viewportHeight = Math.max(0, contentBottom - contentTop);

        this.hoveredStack = class_1799.field_8037;
        this.hoveredTransferTooltip = List.of();
        this.hoveredPreferredRecipeTooltip = List.of();
        this.clipTop = contentTop;
        this.clipBottom = contentBottom;
        this.activeClipTop = contentTop;
        this.activeClipBottom = contentBottom;
        this.nativeDisplaysRenderedThisFrame = 0;
        this.nativeDisplayLimitLoggedThisFrame = false;
        this.nativeDisplayAreas.clear();
        this.transferButtons.clear();
        this.preferredRecipeButtons.clear();
        this.toggleAreas.clear();
        this.recipeListSnapshots.clear();
        this.scrollBar.setMaxValue(Math.max(0, this.contentHeight() - viewportHeight));
        this.updateBackButtonPosition(layout);

        this.renderTitle(context, layout.left());
        this.renderBackButton(context, mouseX, mouseY);
        this.renderTargetHeader(context, layout.left(), layout.headerTop(), layout.headerWidth(), HEADER_HEIGHT, mouseX, mouseY);

        int y = contentTop - this.scrollBar.getValue();
        context.method_44379(layout.left() - OUTLINE_CLIP_PADDING, contentTop - OUTLINE_CLIP_PADDING, layout.left() + layout.contentWidth() + OUTLINE_CLIP_PADDING, contentBottom);
        if (this.summaries.isEmpty()) {
            RenderUtils.drawOutlinedBox(layout.left(), y, layout.contentWidth(), 46, 0xDD000000, 0xFF777777);
            context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.none"), layout.left() + 10, y + 17, 0xFFFFCC66, false);
        } else {
            this.captureRecipeListSnapshot(ROOT_RECIPE_LIST, this.summaries, 0);
            int index = 1;
            for (RecipeSummary summary : this.summaries) {
                String path = "recipe/" + index + ":" + summary.recipeId();
                int boxHeight = this.recipeBoxHeight(summary, path, 0);
                int animatedY = this.animatedRecipeY(ROOT_RECIPE_LIST, summary.recipeId(), y);
                this.renderRecipeBox(context, summary, index, path, ROOT_RECIPE_LIST, 0, layout.left(), animatedY, layout.contentWidth(), boxHeight, mouseX, mouseY, delta);
                y += boxHeight + 10;
                index++;
            }
        }
        context.method_44380();

        this.renderScrollbar(context, mouseX, mouseY, delta, contentTop, contentBottom);
        if (this.renderTransferTooltip(context, mouseX, mouseY)) {
            return;
        }

        if (this.renderPreferredRecipeTooltip(context, mouseX, mouseY)) {
            return;
        }

        if (this.renderNativeTooltip(context, mouseX, mouseY)) {
            return;
        }

        if (!this.hoveredStack.method_7960()) {
            ItemTooltipRenderer.render(context, this.field_22793, this.hoveredStack, mouseX, mouseY);
        }
    }

    private void renderTitle(class_332 context, int left) {
        context.method_51433(
                this.field_22793,
                StringUtils.translate("lmlp.gui.title.recipe_detail_header", LitematicaMaterialListPlus.MOD_VERSION),
                left,
                TITLE_Y,
                0xFFFFFFFF,
                false);
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
        String targetName = ItemStackTexts.name(this.target);
        context.method_51433(this.field_22793, targetName, textX, top + 8, 0xFFFFFFFF, false);
        if (isInside(mouseX, mouseY, textX, top + 8, this.field_22793.method_1727(targetName), 12)) {
            this.hoveredStack = this.target;
        }

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

    private void renderRecipeBox(class_332 context, RecipeSummary summary, int index, String path, String listPath, int depth, int left, int y, int width, int boxHeight, int mouseX, int mouseY, float delta) {
        this.renderRecipeBox(context, summary, index, path, listPath, depth, left, y, width, boxHeight, boxHeight, mouseX, mouseY, delta);
    }

    private void renderRecipeBox(class_332 context, RecipeSummary summary, int index, String path, String listPath, int depth, int left, int y, int width, int boxHeight, int visibleHeight, int mouseX, int mouseY, float delta) {
        int clippedVisibleHeight = Math.max(0, Math.min(boxHeight, visibleHeight));
        if (clippedVisibleHeight <= 0) {
            return;
        }

        RenderUtils.drawRect(left, y, width, clippedVisibleHeight, 0xDD000000);

        int previousClipTop = this.activeClipTop;
        int previousClipBottom = this.activeClipBottom;
        this.activeClipTop = Math.max(this.activeClipTop, y + 1);
        this.activeClipBottom = Math.min(this.activeClipBottom, y + clippedVisibleHeight - 1);
        if (this.activeClipBottom > this.activeClipTop) {
            context.method_44379(left + 1, this.activeClipTop, left + width - 1, this.activeClipBottom);
            this.renderRecipeBoxContents(context, summary, index, path, listPath, depth, left, y, width, boxHeight, mouseX, mouseY, delta);
            context.method_44380();
        }
        this.activeClipTop = previousClipTop;
        this.activeClipBottom = previousClipBottom;

        drawOutline(left, y, width, clippedVisibleHeight, 0xFF777777);
    }

    private void renderRecipeBoxContents(class_332 context, RecipeSummary summary, int index, String path, String listPath, int depth, int left, int y, int width, int boxHeight, int mouseX, int mouseY, float delta) {
        context.method_51427(summary.outputIcon(), left + 10, y + 10);
        this.captureHoveredStack(summary.outputIcon(), mouseX, mouseY, left + 10, y + 10, 16, 16);
        context.method_51433(this.field_22793, RecipeSummaryFormatter.header(summary, index), left + 34, y + 12, 0xFFFFFFFF, false);
        this.renderPreferredRecipeButton(context, summary, listPath, left, y, width, mouseX, mouseY);

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

            float expandProgress = this.expandProgress(ingredientPath);
            if (expandProgress > 0.0F) {
                int fullHeight = this.nestedRecipesHeight(ingredient, ingredientPath, depth + 1);
                int visibleHeight = Math.min(fullHeight, Math.round(fullHeight * expandProgress));
                if (visibleHeight > 0) {
                    int previousClipTop = this.activeClipTop;
                    int previousClipBottom = this.activeClipBottom;
                    this.activeClipTop = Math.max(this.activeClipTop, lineY);
                    this.activeClipBottom = Math.min(this.activeClipBottom, lineY + visibleHeight);
                    this.renderNestedRecipes(context, ingredient, ingredientPath, depth + 1, left + 14, lineY, width - 28, visibleHeight, mouseX, mouseY, delta);
                    this.activeClipTop = previousClipTop;
                    this.activeClipBottom = previousClipBottom;
                }
                lineY += visibleHeight;
            }
        }
    }

    private void renderIngredientLine(class_332 context, int left, int y, int depth, String path, IngredientSummary ingredient, int mouseX, int mouseY) {
        boolean hasRecipes = depth < MAX_NESTED_DEPTH && this.hasRecipes(ingredient);
        this.renderMaterialLine(
                context,
                left,
                y,
                0,
                path,
                hasRecipes,
                AlternativeItemDisplay.icon(ingredient),
                RecipeSummaryFormatter.ingredientName(ingredient),
                RecipeSummaryFormatter.totalCount(ingredient),
                RecipeSummaryFormatter.missingCount(ingredient),
                ingredient.countMissing(),
                true,
                mouseX,
                mouseY);
    }

    private int renderNestedRecipes(class_332 context, IngredientSummary ingredient, String parentPath, int depth, int left, int y, int width, int visibleHeight, int mouseX, int mouseY, float delta) {
        int lineY = y;
        int remainingHeight = visibleHeight;
        List<RecipeSummary> recipes = this.recipesFor(ingredient);
        this.captureRecipeListSnapshot(parentPath, recipes, depth);
        int nestedLeft = left + NESTED_RECIPE_INDENT;
        int nestedWidth = Math.max(180, width - NESTED_RECIPE_INDENT);
        for (int recipeIndex = 0; recipeIndex < recipes.size(); recipeIndex++) {
            if (remainingHeight <= 0) {
                break;
            }

            RecipeSummary recipe = recipes.get(recipeIndex);
            String path = parentPath + "/recipe/" + (recipeIndex + 1) + ":" + recipe.recipeId();
            int height = this.recipeBoxHeight(recipe, path, depth);
            int visibleBoxHeight = Math.min(height, remainingHeight);
            int animatedY = this.animatedRecipeY(parentPath, recipe.recipeId(), lineY);
            this.renderRecipeBox(context, recipe, recipeIndex + 1, path, parentPath, depth, nestedLeft, animatedY, nestedWidth, height, visibleBoxHeight, mouseX, mouseY, delta);
            lineY += visibleBoxHeight;
            remainingHeight -= visibleBoxHeight;

            int visibleGap = Math.min(NESTED_RECIPE_GAP, remainingHeight);
            lineY += visibleGap;
            remainingHeight -= visibleGap;
        }

        return lineY;
    }

    private void renderMaterialLine(class_332 context, int left, int y, int depth, String path, boolean hasRecipes, class_1799 icon, String name, String totalText, String missingText, int missingCount, boolean showMissing, int mouseX, int mouseY) {
        int rowX = left + depth * INGREDIENT_TREE_INDENT_WIDTH;
        int iconX = rowX + INGREDIENT_ICON_OFFSET;
        int iconY = y - 5;
        if (hasRecipes) {
            boolean hovered = isInside(mouseX, mouseY, rowX, y - 2, INGREDIENT_TOGGLE_WIDTH, INGREDIENT_ROW_HEIGHT);
            ToggleArrowRenderer.render(context, rowX, INGREDIENT_TOGGLE_WIDTH, iconY + 8, this.expandProgress(path), hovered);
            if (this.isVisibleInActiveClip(y - 2, INGREDIENT_ROW_HEIGHT)) {
                this.toggleAreas.add(new ToggleArea(path, rowX, y - 2, INGREDIENT_TOGGLE_WIDTH, INGREDIENT_ROW_HEIGHT));
            }
        }

        context.method_51427(icon, iconX, iconY);
        this.captureHoveredStack(icon, mouseX, mouseY, iconX, iconY, 16, 16);

        int textX = rowX + INGREDIENT_ICON_OFFSET + 24;
        int lineStartX = iconX;
        String prefix = name + ": ";
        context.method_51433(this.field_22793, prefix, textX, y, INGREDIENT_TEXT_COLOR, false);
        textX += this.field_22793.method_1727(prefix);
        context.method_51433(this.field_22793, totalText, textX, y, INGREDIENT_TOTAL_COLOR, false);
        textX += this.field_22793.method_1727(totalText);
        if (showMissing) {
            context.method_51433(this.field_22793, " / ", textX, y, INGREDIENT_TEXT_COLOR, false);
            textX += this.field_22793.method_1727(" / ");
            int missingColor = missingCount == 0 ? 0xFF55FF55 : INGREDIENT_MISSING_COLOR;
            context.method_51433(this.field_22793, missingText, textX, y, missingColor, false);
            textX += this.field_22793.method_1727(missingText);
        }
        if (isInside(mouseX, mouseY, lineStartX, y - 5, Math.max(16, textX - lineStartX), INGREDIENT_ROW_HEIGHT)) {
            this.captureHoveredStack(icon, mouseX, mouseY, lineStartX, y - 5, Math.max(16, textX - lineStartX), INGREDIENT_ROW_HEIGHT);
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
            if (entry.contains(mouseX, mouseY) && entry.button().method_25402(mouseX, mouseY, 0)) {
                return true;
            }
        }

        return false;
    }

    private boolean handlePreferredRecipeButtonClick(double mouseX, double mouseY) {
        for (int i = this.preferredRecipeButtons.size() - 1; i >= 0; i--) {
            PreferredRecipeButtonArea area = this.preferredRecipeButtons.get(i);
            if (area.contains(mouseX, mouseY)) {
                Configs.togglePreferredRecipe(area.itemId(), area.recipeId());
                this.summaries = RecipeResolvers.applyPreferredOrder(this.summaries);
                this.startRecipeReorderAnimations();
                this.nestedRecipeCache.clear();
                return true;
            }
        }

        return false;
    }

    private void renderPreferredRecipeButton(class_332 context, RecipeSummary summary, String listPath, int left, int y, int width, int mouseX, int mouseY) {
        String itemId = ItemStackTexts.id(summary.outputIcon());
        boolean preferred = Configs.isPreferredRecipe(itemId, summary.recipeId());
        String label = StringUtils.translate("lmlp.label.recipe.preferred_pin");
        int buttonX = left + width - PREFERRED_BUTTON_SIZE - 8;
        int buttonY = y + 8;
        if (buttonX <= left + 34) {
            return;
        }

        boolean hovered = isInside(mouseX, mouseY, buttonX, buttonY, PREFERRED_BUTTON_SIZE, PREFERRED_BUTTON_SIZE);
        int starX = buttonX + (PREFERRED_BUTTON_SIZE - PREFERRED_ICON_SIZE) / 2;
        int starY = buttonY + (PREFERRED_BUTTON_SIZE - PREFERRED_ICON_SIZE) / 2;
        int textureU = hovered ? PREFERRED_ICON_SIZE : 0;
        int textureV = preferred ? 0 : PREFERRED_ICON_SIZE;
        context.method_25290(PREFERRED_WIDGETS_TEXTURE, starX, starY, (float) textureU, (float) textureV, PREFERRED_ICON_SIZE, PREFERRED_ICON_SIZE, PREFERRED_WIDGET_TEXTURE_SIZE, PREFERRED_WIDGET_TEXTURE_SIZE);
        if (hovered) {
            this.drawPreferredRecipeHoverBorder(context, buttonX, buttonY);
            this.hoveredPreferredRecipeTooltip = List.of(class_2561.method_43470(label));
        }

        if (this.isVisibleInActiveClip(buttonY, PREFERRED_BUTTON_SIZE)) {
            this.preferredRecipeButtons.add(new PreferredRecipeButtonArea(itemId, summary.recipeId(), listPath, buttonX, buttonY, PREFERRED_BUTTON_SIZE, PREFERRED_BUTTON_SIZE));
        }
    }

    private void drawPreferredRecipeHoverBorder(class_332 context, int x, int y) {
        int right = x + PREFERRED_BUTTON_SIZE;
        int bottom = y + PREFERRED_BUTTON_SIZE;
        context.method_25294(x, y, right, y + 1, 0xFFFFFFFF);
        context.method_25294(x, bottom - 1, right, bottom, 0xFFFFFFFF);
        context.method_25294(x, y, x + 1, bottom, 0xFFFFFFFF);
        context.method_25294(right - 1, y, right, bottom, 0xFFFFFFFF);
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
        if (!this.isVisibleInActiveClip(buttonBounds.y, buttonBounds.height)) {
            return;
        }

        AutoCraftingState state = this.evaluateTransfer(display, false, false);
        Button button = Widgets.createButton(buttonBounds, state.hasApplicable() ? class_2561.method_43470(buttonArea.get().getButtonText()) : class_2561.method_43470("!"))
                .focusable(false)
                .onClick(ignored -> this.transferRecipe(summary, GuiBase.isShiftDown()));
        button.setEnabled(state.successful());
        button.setTint(state.tint());

        int visibleTop = Math.max(buttonBounds.y, this.activeClipTop);
        int visibleBottom = Math.min(buttonBounds.getMaxY(), this.activeClipBottom);
        boolean mouseInVisibleButton = isInside(mouseX, mouseY, buttonBounds.x, visibleTop, buttonBounds.width, visibleBottom - visibleTop);

        if (state.hasApplicable()
                && state.renderer() != null
                && (mouseInVisibleButton || button.method_25370())) {
            state.renderer().render(context, mouseX, mouseY, delta, this.setupDisplayWidgets(display, displayBounds), displayBounds, display);
        }

        button.method_25394(context, mouseX, mouseY, delta);
        if (mouseInVisibleButton && !button.method_25370()) {
            this.hoveredTransferTooltip = state.tooltip();
        }
        this.transferButtons.add(new TransferButtonEntry(button, state, buttonBounds.x, visibleTop, buttonBounds.width, visibleBottom - visibleTop));
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
        return new AutoCraftingState(successful, hasApplicable, tint, renderer, filterTransferTooltip(tooltip));
    }

    private boolean renderTransferTooltip(class_332 context, int mouseX, int mouseY) {
        if (this.hoveredTransferTooltip.isEmpty()) {
            return false;
        }

        List<class_2561> lines = new ArrayList<>();
        for (Tooltip.Entry entry : this.hoveredTransferTooltip) {
            if (entry.isText()) {
                lines.add(entry.getAsText());
            }
        }

        if (lines.isEmpty()) {
            return false;
        }

        context.method_51434(this.field_22793, lines, mouseX, mouseY);
        return true;
    }

    private boolean renderPreferredRecipeTooltip(class_332 context, int mouseX, int mouseY) {
        if (this.hoveredPreferredRecipeTooltip.isEmpty()) {
            return false;
        }

        context.method_51434(this.field_22793, this.hoveredPreferredRecipeTooltip, mouseX, mouseY);
        return true;
    }

    private static List<Tooltip.Entry> filterTransferTooltip(List<Tooltip.Entry> tooltip) {
        return tooltip.stream()
                .filter(entry -> !isFavoriteHint(entry))
                .toList();
    }

    private static boolean isFavoriteHint(Tooltip.Entry entry) {
        if (!entry.isText()) {
            return false;
        }

        String text = entry.getAsText().getString();
        String lowerText = text.toLowerCase(java.util.Locale.ROOT);
        return text.contains("收藏") || lowerText.contains("favorite") || lowerText.contains("save it for later");
    }

    private void addRecipeIdTooltip(Display display, List<Tooltip.Entry> tooltip) {
        if (this.field_22787 == null || this.field_22787.field_1690 == null || !this.field_22787.field_1690.field_1827) {
            return;
        }

        for (class_2960 id : display.provideInternalDisplayIds()) {
            tooltip.add(Tooltip.entry(class_2561.method_43470(StringUtils.translate("lmlp.label.recipe.recipe_id") + ": " + id).method_27692(class_124.field_1080)));
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

    private void captureRecipeListSnapshot(String listPath, List<RecipeSummary> recipes, int depth) {
        this.recipeListSnapshots.put(listPath, new RecipeListSnapshot(List.copyOf(recipes), depth, this.recipePositions(recipes, depth)));
    }

    private void startRecipeReorderAnimations() {
        this.recipeReorderAnimations.clear();
        for (Map.Entry<String, RecipeListSnapshot> entry : this.recipeListSnapshots.entrySet()) {
            String listPath = entry.getKey();
            RecipeListSnapshot snapshot = entry.getValue();
            List<RecipeSummary> recipes = ROOT_RECIPE_LIST.equals(listPath) ? this.summaries : RecipeResolvers.applyPreferredOrder(snapshot.recipes());
            RecipeReorderAnimation animation = RecipeReorderAnimation.start(snapshot.positions(), this.recipePositions(recipes, snapshot.depth()));
            if (!animation.isFinished()) {
                this.recipeReorderAnimations.put(listPath, animation);
            }
        }
    }

    private int animatedRecipeY(String listPath, String recipeId, int targetY) {
        RecipeReorderAnimation animation = this.recipeReorderAnimations.get(listPath);
        return animation == null ? targetY : animation.y(recipeId, targetY);
    }

    private Map<String, Integer> recipePositions(List<RecipeSummary> recipes, int depth) {
        Map<String, Integer> positions = new HashMap<>();
        int y = 0;
        int index = 1;
        for (RecipeSummary summary : recipes) {
            positions.put(summary.recipeId(), y);
            y += this.recipeBoxHeight(summary, "recipe/" + index + ":" + summary.recipeId(), depth) + 10;
            index++;
        }

        return positions;
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
            float expandProgress = this.expandProgress(ingredientPath);
            if (expandProgress > 0.0F) {
                height += Math.round(this.nestedRecipesHeight(ingredient, ingredientPath, depth + 1) * expandProgress);
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
        float startProgress = this.expandProgress(path);
        if (this.expandedNestedRecipes.contains(path)) {
            this.collapseNestedRecipes(path);
            this.nestedRecipeAnimations.start(path, startProgress, 0.0F);
        } else {
            this.expandedNestedRecipes.add(path);
            this.nestedRecipeAnimations.start(path, startProgress, 1.0F);
        }
    }

    private void collapseNestedRecipes(String path) {
        this.expandedNestedRecipes.removeIf(expandedPath -> expandedPath.equals(path) || expandedPath.startsWith(path + "/"));
        this.nestedRecipeAnimations.removeDescendants(path);
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
            int visibleTop = Math.max(y, this.activeClipTop);
            int visibleBottom = Math.min(y + height, this.activeClipBottom);
            if (visibleBottom > visibleTop) {
                this.nativeDisplayAreas.add(new NativeDisplayArea(summary, x, visibleTop, width, visibleBottom - visibleTop));
            }
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

    private void offsetScroll(double verticalAmount) {
        double target = this.scrollRemainder - verticalAmount * WHEEL_SCROLL_PIXELS;
        int pixels = (int) target;
        this.scrollRemainder = target - pixels;
        if (pixels == 0 && verticalAmount != 0.0D) {
            pixels = verticalAmount > 0.0D ? -1 : 1;
            this.scrollRemainder = 0.0D;
        }
        this.scrollBar.offsetValue(pixels);
    }

    private void captureHoveredStack(class_1799 stack, int mouseX, int mouseY, int x, int y, int width, int height) {
        if (mouseY >= this.activeClipTop && mouseY <= this.activeClipBottom && isInside(mouseX, mouseY, x, y, width, height)) {
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
        return y + height >= this.activeClipTop - NATIVE_RENDER_CLIP_PADDING && y <= this.activeClipBottom + NATIVE_RENDER_CLIP_PADDING;
    }

    private boolean isVisibleInActiveClip(int y, int height) {
        return y + height > this.activeClipTop && y < this.activeClipBottom;
    }

    private float expandProgress(String path) {
        return this.nestedRecipeAnimations.progress(path, this.expandedNestedRecipes.contains(path));
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

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void drawOutline(int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }

        RenderUtils.drawRect(x, y, width, 1, color);
        if (height > 1) {
            RenderUtils.drawRect(x, y + height - 1, width, 1, color);
            RenderUtils.drawRect(x, y, 1, height, color);
            RenderUtils.drawRect(x + width - 1, y, 1, height, color);
        }
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

    private record TransferButtonEntry(Button button, AutoCraftingState state, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private record PreferredRecipeButtonArea(String itemId, String recipeId, String listPath, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private record RecipeListSnapshot(List<RecipeSummary> recipes, int depth, Map<String, Integer> positions) {
    }

    private record RecipeReorderAnimation(long startTimeMs, Map<String, Integer> startOffsets) {
        private static final RecipeReorderAnimation NONE = new RecipeReorderAnimation(0L, Map.of());

        private static RecipeReorderAnimation start(Map<String, Integer> oldPositions, Map<String, Integer> newPositions) {
            Map<String, Integer> offsets = new HashMap<>();
            for (Map.Entry<String, Integer> entry : newPositions.entrySet()) {
                Integer oldY = oldPositions.get(entry.getKey());
                if (oldY == null) {
                    continue;
                }

                int offset = oldY - entry.getValue();
                if (offset != 0) {
                    offsets.put(entry.getKey(), offset);
                }
            }

            if (offsets.isEmpty()) {
                return NONE;
            }

            return new RecipeReorderAnimation(System.currentTimeMillis(), Map.copyOf(offsets));
        }

        private int y(String recipeId, int targetY) {
            Integer offset = this.startOffsets.get(recipeId);
            if (offset == null) {
                return targetY;
            }

            float elapsed = (float) (System.currentTimeMillis() - this.startTimeMs) / (float) ExpandAnimationTracker.DURATION_MS;
            float remaining = 1.0F - ExpandAnimationTracker.easeOutCubic(elapsed);
            return targetY + Math.round(offset * remaining);
        }

        private boolean isFinished() {
            return this.startOffsets.isEmpty() || System.currentTimeMillis() - this.startTimeMs >= ExpandAnimationTracker.DURATION_MS;
        }
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
