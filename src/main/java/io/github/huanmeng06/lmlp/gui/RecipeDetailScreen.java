package io.github.huanmeng06.lmlp.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import io.github.huanmeng06.lmlp.recipe.IngredientUnion;
import io.github.huanmeng06.lmlp.recipe.RecipeResolvers;
import io.github.huanmeng06.lmlp.recipe.RecipeSlotSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_465;
import net.minecraft.class_490;

public class RecipeDetailScreen extends class_437 {
    private static final Logger LOGGER = LoggerFactory.getLogger(LitematicaMaterialListPlus.MOD_ID);
    private static final int BACK_BUTTON_WIDTH = 112;
    private static final int BACK_BUTTON_HEIGHT = 20;
    private static final int RECIPE_PANEL_WIDTH = 254;
    private static final int RECIPE_PANEL_HEIGHT = 104;
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
    private static final int CATEGORY_TAB_SIZE = 28;
    private static final int INGREDIENT_ROW_HEIGHT = 20;
    private static final int INGREDIENT_TREE_INDENT_WIDTH = 18;
    private static final int INGREDIENT_TOGGLE_WIDTH = 18;
    private static final int INGREDIENT_ICON_OFFSET = 20;
    private static final int NESTED_RECIPE_GAP = 8;
    private static final int NESTED_RECIPE_INDENT = 24;
    private static final int MAX_NESTED_DEPTH = 3;
    private static final int WHEEL_SCROLL_PIXELS = 36;
    private static final int HOVER_TOOLTIP_MARGIN = 8;
    private static final int HOVER_TOOLTIP_CURSOR_OFFSET = 12;
    private static final int HOVER_TOOLTIP_PADDING = 6;
    private static final int HOVER_TOOLTIP_LINE_HEIGHT = 12;
    private static final int HOVER_TOOLTIP_HEADER_GAP = 3;
    private static final int HOVER_TOOLTIP_ICON_GAP = 6;
    private static final int HOVER_TOOLTIP_ICON_SIZE = 16;
    private static final int CHOICE_TOOLTIP_COLUMN_GAP = 14;
    private static final int CHOICE_TOOLTIP_ROW_HEIGHT = 18;
    private static final String ROOT_RECIPE_LIST = "root";
    private static final class_2960 TRANSFER_BUTTON_TEXTURE = class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID,
            "textures/gui/rei_button.png");
    private static final class_2960 PREFERRED_WIDGETS_TEXTURE = class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID,
            "textures/gui/gui_widgets.png");

    private final class_437 parent;
    private final class_1799 target;
    private final int totalCount;
    private final int missingCount;
    private List<RecipeSummary> summaries;
    private final GuiScrollBar scrollBar = new GuiScrollBar();
    private final ButtonGeneric backButton = new ButtonGeneric(0, 0, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, "");
    private final RecipeNativeDisplayBridge nativeDisplayBridge = createNativeDisplayBridge();
    private final RecipeTransferBridge transferBridge = createTransferBridge();
    private final List<NativeDisplayArea> nativeDisplayAreas = new ArrayList<>();
    private final List<TransferButtonEntry> transferButtons = new ArrayList<>();
    private final List<PreferredRecipeButtonArea> preferredRecipeButtons = new ArrayList<>();
    private final List<CategoryTabArea> categoryTabs = new ArrayList<>();
    private final List<ToggleArea> toggleAreas = new ArrayList<>();
    private final List<ChoiceGroupNameArea> choiceGroupNameAreas = new ArrayList<>();
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
    private List<class_2561> hoveredTransferTooltip = List.of();
    private TransferButtonEntry hoveredTransferEntry;
    private List<class_2561> hoveredPreferredRecipeTooltip = List.of();
    private List<class_2561> hoveredCategoryTooltip = List.of();
    private String pendingFocusPath;
    private int pendingFocusContentY = Integer.MIN_VALUE;

    public RecipeDetailScreen(class_437 parent, class_1799 target, int totalCount, int missingCount,
            List<RecipeSummary> summaries) {
        this(parent, target, totalCount, missingCount, summaries, "", "");
    }

    public RecipeDetailScreen(class_437 parent, class_1799 target, int totalCount, int missingCount,
            List<RecipeSummary> summaries, String focusRecipeId, String focusItemId) {
        super(class_2561.method_43471("lmlp.gui.recipe_detail.title"));
        this.parent = parent;
        this.transferContainerScreen = findTransferContainer(parent);
        this.target = target.method_7972();
        this.totalCount = totalCount;
        this.missingCount = missingCount;
        this.summaries = RecipeResolvers.applyPreferredOrder(List.copyOf(summaries));
        this.prepareInitialFocus(focusRecipeId, focusItemId);
        this.backButton.setDisplayString(StringUtils.translate("lmlp.label.recipe.back"));
        this.backButton.setTextCentered(true);
        this.backButton.setActionListener((button, mouseButton) -> this.method_25419());
    }

    private void prepareInitialFocus(String focusRecipeId, String focusItemId) {
        if (focusRecipeId == null || focusRecipeId.isEmpty() || focusItemId == null || focusItemId.isEmpty()) {
            return;
        }

        for (int summaryIndex = 0; summaryIndex < this.summaries.size(); summaryIndex++) {
            RecipeSummary summary = this.summaries.get(summaryIndex);
            if (!focusRecipeId.equals(summary.recipeId())) {
                continue;
            }

            for (int ingredientIndex = 0; ingredientIndex < summary.ingredients().size(); ingredientIndex++) {
                IngredientSummary ingredient = summary.ingredients().get(ingredientIndex);
                if (!focusItemId.equals(ItemStackTexts.id(ingredient.icon()))) {
                    continue;
                }

                String recipePath = "recipe/" + (summaryIndex + 1) + ":" + summary.recipeId();
                String ingredientPath = recipePath + "/ingredient/" + ingredientIndex + ":" + key(ingredient);
                this.expandedNestedRecipes.add(ingredientPath);
                this.pendingFocusPath = ingredientPath;
                return;
            }
        }
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
    public void method_25393() {
        super.method_25393();
        Set<Object> tickedDisplays = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (NativeDisplayArea area : this.nativeDisplayAreas) {
            Object nativeDisplay = area.summary().nativeDisplay();
            if (nativeDisplay != null && tickedDisplays.add(nativeDisplay)) {
                this.nativeDisplayBridge.tick(area.summary());
            }
        }
    }

    @Override
    public boolean method_25404(net.minecraft.class_11908 event) {
        int keyCode = event.comp_4795();
        if (MaterialListHotkeyMatcher.matches(keyCode) && this.transferContainerScreen != null) {
            MaterialListOpener.rememberHandledScreenOverlay(this);
            this.field_22787.method_1507(this.transferContainerScreen);
            return true;
        }

        return super.method_25404(event);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        if (this.dispatchNativeDisplay(area, "mouse scroll",
                summary -> this.nativeDisplayBridge.mouseScrolled(summary, mouseX, mouseY, verticalAmount))) {
            return true;
        }

        this.offsetScroll(verticalAmount);
        return true;
    }

    @Override
    public boolean method_25402(net.minecraft.class_11909 event, boolean doubleClick) {
        double mouseX = event.comp_4798();
        double mouseY = event.comp_4799();
        int button = event.comp_4800().comp_4801();
        if (button == 0 && this.backButton.onMouseClicked(event, doubleClick)) {
            return true;
        }

        if (button == 0 && this.handleTransferButtonClick(mouseX, mouseY)) {
            return true;
        }

        if (button == 0 && this.handlePreferredRecipeButtonClick(mouseX, mouseY)) {
            return true;
        }

        if (button == 0 && this.handleCategoryTabClick(mouseX, mouseY)) {
            return true;
        }

        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        if (this.dispatchNativeDisplay(area, "mouse click",
                summary -> this.nativeDisplayBridge.mouseClicked(summary, mouseX, mouseY, button))) {
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

        return super.method_25402(event, doubleClick);
    }

    @Override
    public boolean method_25403(net.minecraft.class_11909 event, double deltaX, double deltaY) {
        double mouseX = event.comp_4798();
        double mouseY = event.comp_4799();
        int button = event.comp_4800().comp_4801();
        if (button == 0 && this.draggingScrollbar) {
            return true;
        }

        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        if (this.dispatchNativeDisplay(area, "mouse drag",
                summary -> this.nativeDisplayBridge.mouseDragged(summary, mouseX, mouseY, button, deltaX, deltaY))) {
            return true;
        }

        return super.method_25403(event, deltaX, deltaY);
    }

    @Override
    public boolean method_25406(net.minecraft.class_11909 event) {
        double mouseX = event.comp_4798();
        double mouseY = event.comp_4799();
        int button = event.comp_4800().comp_4801();
        if (button == 0) {
            this.scrollBar.setIsDragging(false);
            this.draggingScrollbar = false;
        }

        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        if (this.dispatchNativeDisplay(area, "mouse release",
                summary -> this.nativeDisplayBridge.mouseReleased(summary, mouseX, mouseY, button))) {
            return true;
        }

        return super.method_25406(event);
    }

    @Override
    public void method_25394(class_332 drawContext, int mouseX, int mouseY, float delta) {
        GuiContext context = GuiContext.fromGuiGraphics(drawContext);
        RenderUtils.drawRect(context, 0, 0, this.field_22789, this.field_22790, SCREEN_BACKGROUND_COLOR);
        this.nestedRecipeAnimations.prune();
        this.recipeReorderAnimations.entrySet().removeIf(entry -> entry.getValue().isFinished());

        Layout layout = this.layout();
        int contentTop = layout.headerTop() + HEADER_HEIGHT + 12;
        int contentBottom = this.field_22790 - PAGE_BOTTOM_MARGIN;
        int viewportHeight = Math.max(0, contentBottom - contentTop);

        this.hoveredStack = class_1799.field_8037;
        this.hoveredTransferTooltip = List.of();
        this.hoveredTransferEntry = null;
        this.hoveredPreferredRecipeTooltip = List.of();
        this.hoveredCategoryTooltip = List.of();
        this.clipTop = contentTop;
        this.clipBottom = contentBottom;
        this.activeClipTop = contentTop;
        this.activeClipBottom = contentBottom;
        this.nativeDisplaysRenderedThisFrame = 0;
        this.nativeDisplayLimitLoggedThisFrame = false;
        this.nativeDisplayAreas.clear();
        this.transferButtons.clear();
        this.preferredRecipeButtons.clear();
        this.categoryTabs.clear();
        this.toggleAreas.clear();
        this.choiceGroupNameAreas.clear();
        this.recipeListSnapshots.clear();
        if (this.pendingFocusPath != null) {
            this.pendingFocusContentY = Integer.MIN_VALUE;
        }
        this.scrollBar.setMaxValue(Math.max(0, this.contentHeight() - viewportHeight));
        this.updateBackButtonPosition(layout);

        this.renderTitle(context, layout.left());
        this.renderBackButton(context, mouseX, mouseY);
        this.renderTargetHeader(context, layout.left(), layout.headerTop(), layout.headerWidth(), HEADER_HEIGHT, mouseX,
                mouseY);

        int y = contentTop - this.scrollBar.getValue();
        context.method_44379(layout.left() - OUTLINE_CLIP_PADDING, contentTop - OUTLINE_CLIP_PADDING,
                layout.left() + layout.contentWidth() + OUTLINE_CLIP_PADDING, contentBottom);
        if (this.summaries.isEmpty()) {
            RenderUtils.drawOutlinedBox(context, layout.left(), y, layout.contentWidth(), 46, 0xDD000000, 0xFF777777);
            context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.none"), layout.left() + 10,
                    y + 17, 0xFFFFCC66, false);
        } else {
            this.captureRecipeListSnapshot(ROOT_RECIPE_LIST, this.summaries, 0);
            int index = 1;
            for (RecipeSummary summary : this.summaries) {
                String path = "recipe/" + index + ":" + summary.recipeId();
                int boxHeight = this.recipeBoxHeight(summary, path, 0);
                int animatedY = this.animatedRecipeY(ROOT_RECIPE_LIST, summary.recipeId(), y);
                this.renderRecipeBox(context, summary, index, path, ROOT_RECIPE_LIST, 0, layout.left(), animatedY,
                        layout.contentWidth(), boxHeight, mouseX, mouseY, delta);
                y += boxHeight + 10;
                index++;
            }
        }
        context.method_44380();

        this.applyPendingFocus(contentTop);

        this.renderScrollbar(context, mouseX, mouseY, delta, contentTop, contentBottom);
        if (this.renderTransferTooltip(context, mouseX, mouseY)) {
            return;
        }

        if (this.renderPreferredRecipeTooltip(context, mouseX, mouseY)) {
            return;
        }

        if (this.renderCategoryTooltip(context, mouseX, mouseY)) {
            return;
        }

        if (this.renderNativeTooltip(context, mouseX, mouseY)) {
            return;
        }

        if (this.renderChoiceGroupTooltip(context, mouseX, mouseY)) {
            return;
        }

        if (!this.hoveredStack.method_7960()) {
            ItemTooltipRenderer.render(context, this.field_22793, this.hoveredStack, mouseX, mouseY);
        }
    }

    private void renderTitle(GuiContext context, int left) {
        context.method_51433(
                this.field_22793,
                StringUtils.translate("lmlp.gui.title.recipe_detail_header", LitematicaMaterialListPlus.MOD_VERSION),
                left,
                TITLE_Y,
                0xFFFFFFFF,
                false);
    }

    private void renderTargetHeader(GuiContext context, int left, int top, int width, int height, int mouseX,
            int mouseY) {
        RenderUtils.drawOutlinedBox(context, left, top, width, height, 0xDD000000, 0xFF888888);
        context.method_51427(this.target, left + 10, top + 9);
        if (isInside(mouseX, mouseY, left + 10, top + 9, 16, 16)) {
            this.hoveredStack = this.target;
        }
        int textX = left + 36;
        int textRight = Math.max(textX + 1, left + width - 6);
        context.method_44379(textX, top + 4, textRight, top + height - 4);
        String targetName = ItemStackTexts.name(this.target);
        context.method_51433(this.field_22793, targetName, textX, top + 8, 0xFFFFFFFF, false);

        String total = StringUtils.translate("lmlp.label.recipe.total_short") + ": "
                + CountFormatter.format(this.target, this.totalCount);
        String missing = StringUtils.translate("lmlp.label.recipe.missing_short") + ": "
                + CountFormatter.format(this.target, this.missingCount);
        String counts = total + "    " + missing;
        if (this.field_22793.method_1727(counts) <= textRight - textX) {
            context.method_51433(this.field_22793, counts, textX, top + 28, 0xFFAAAAAA, false);
        } else {
            context.method_51433(this.field_22793, total, textX, top + 23, 0xFFAAAAAA, false);
            context.method_51433(this.field_22793, missing, textX, top + 35, 0xFFAAAAAA, false);
        }
        context.method_44380();
    }

    private void renderBackButton(GuiContext context, int mouseX, int mouseY) {
        this.backButton.render(context, mouseX, mouseY, false);
    }

    private void renderRecipeBox(GuiContext context, RecipeSummary summary, int index, String path, String listPath,
            int depth, int left, int y, int width, int boxHeight, int mouseX, int mouseY, float delta) {
        this.renderRecipeBox(context, summary, index, path, listPath, depth, left, y, width, boxHeight, boxHeight,
                mouseX, mouseY, delta);
    }

    private void renderRecipeBox(GuiContext context, RecipeSummary summary, int index, String path, String listPath,
            int depth, int left, int y, int width, int boxHeight, int visibleHeight, int mouseX, int mouseY,
            float delta) {
        int clippedVisibleHeight = Math.max(0, Math.min(boxHeight, visibleHeight));
        if (clippedVisibleHeight <= 0) {
            return;
        }

        RenderUtils.drawRect(context, left, y, width, clippedVisibleHeight, 0xDD000000);

        int previousClipTop = this.activeClipTop;
        int previousClipBottom = this.activeClipBottom;
        this.activeClipTop = Math.max(this.activeClipTop, y + 1);
        this.activeClipBottom = Math.min(this.activeClipBottom, y + clippedVisibleHeight - 1);
        if (this.activeClipBottom > this.activeClipTop) {
            context.method_44379(left + 1, this.activeClipTop, left + width - 1, this.activeClipBottom);
            this.renderRecipeBoxContents(context, summary, index, path, listPath, depth, left, y, width, boxHeight,
                    mouseX, mouseY, delta);
            context.method_44380();
        }
        this.activeClipTop = previousClipTop;
        this.activeClipBottom = previousClipBottom;

        drawOutline(context, left, y, width, clippedVisibleHeight, 0xFF777777);
    }

    private void renderRecipeBoxContents(GuiContext context, RecipeSummary summary, int index, String path,
            String listPath, int depth, int left, int y, int width, int boxHeight, int mouseX, int mouseY,
            float delta) {
        class_1799 displayOutput = displayOutputIcon(summary);
        context.method_51427(displayOutput, left + 10, y + 10);
        this.captureHoveredStack(displayOutput, mouseX, mouseY, left + 10, y + 10, 16, 16);
        context.method_51433(this.field_22793, RecipeSummaryFormatter.header(summary, index), left + 34, y + 12,
                0xFFFFFFFF, false);
        this.renderPreferredRecipeButton(context, summary, listPath, left, y, width, mouseX, mouseY);

        int panelWidth = this.displayPanelWidth(summary, width - 36, depth);
        int panelHeight = this.displayPanelHeight(summary, depth);
        int panelX = left + 18;
        int panelY = y + 38;
        if (this.isDisplayPanelVisible(panelY, panelHeight) && !this.renderNativeDisplay(summary, context, panelX,
                panelY, panelWidth, panelHeight, mouseX, mouseY, delta, path, depth)) {
            this.renderCraftingGrid(context, summary, panelX, panelY, mouseX, mouseY);
        }
        this.renderTransferButton(context, summary, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY, delta);
        this.renderCategoryTab(context, summary, panelX, panelY, panelWidth, mouseX, mouseY);

        int lineY = panelY + panelHeight + 16;
        context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.ingredients_total"), left + 14,
                lineY, 0xFFAAAAAA, false);
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
                    this.renderNestedRecipes(context, ingredient, ingredientPath, depth + 1, left + 14, lineY,
                            width - 28, visibleHeight, mouseX, mouseY, delta);
                    this.activeClipTop = previousClipTop;
                    this.activeClipBottom = previousClipBottom;
                }
                lineY += visibleHeight;
            }
        }
    }

    private void renderIngredientLine(GuiContext context, int left, int y, int depth, String path,
            IngredientSummary ingredient, int mouseX, int mouseY) {
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
                ingredient.isChoiceGroup(),
                ingredient.icons(),
                ingredient.alternatives(),
                RecipeSummaryFormatter.totalCount(ingredient),
                RecipeSummaryFormatter.missingCount(ingredient),
                ingredient.countMissing(),
                true,
                mouseX,
                mouseY);
    }

    private int renderNestedRecipes(GuiContext context, IngredientSummary ingredient, String parentPath, int depth,
            int left, int y, int width, int visibleHeight, int mouseX, int mouseY, float delta) {
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
            this.renderRecipeBox(context, recipe, recipeIndex + 1, path, parentPath, depth, nestedLeft, animatedY,
                    nestedWidth, height, visibleBoxHeight, mouseX, mouseY, delta);
            lineY += visibleBoxHeight;
            remainingHeight -= visibleBoxHeight;

            int visibleGap = Math.min(NESTED_RECIPE_GAP, remainingHeight);
            lineY += visibleGap;
            remainingHeight -= visibleGap;
        }

        return lineY;
    }

    private void renderMaterialLine(GuiContext context, int left, int y, int depth, String path, boolean hasRecipes,
            class_1799 icon, String name, boolean choiceGroup, List<class_1799> choiceIcons,
            List<String> choiceAlternatives, String totalText, String missingText, int missingCount,
            boolean showMissing, int mouseX, int mouseY) {
        if (path.equals(this.pendingFocusPath)) {
            this.pendingFocusContentY = y + this.scrollBar.getValue();
        }
        int rowX = left + depth * INGREDIENT_TREE_INDENT_WIDTH;
        int iconX = rowX + INGREDIENT_ICON_OFFSET;
        int iconY = y - 5;
        if (hasRecipes) {
            boolean hovered = isInside(mouseX, mouseY, rowX, y - 2, INGREDIENT_TOGGLE_WIDTH, INGREDIENT_ROW_HEIGHT);
            ToggleArrowRenderer.render(context, rowX, INGREDIENT_TOGGLE_WIDTH, iconY + 8, this.expandProgress(path),
                    hovered);
            if (this.isVisibleInActiveClip(y - 2, INGREDIENT_ROW_HEIGHT)) {
                this.toggleAreas.add(new ToggleArea(path, rowX, y - 2, INGREDIENT_TOGGLE_WIDTH, INGREDIENT_ROW_HEIGHT));
            }
        }

        context.method_51427(icon, iconX, iconY);
        this.captureHoveredStack(icon, mouseX, mouseY, iconX, iconY, 16, 16);

        int textX = rowX + INGREDIENT_ICON_OFFSET + 24;
        String namePart = choiceGroup
                ? GuiBase.TXT_YELLOW + name + GuiBase.TXT_RST
                : name;
        context.method_51433(this.field_22793, namePart, textX, y, INGREDIENT_TEXT_COLOR, false);
        int namePartWidth = this.field_22793.method_1727(namePart);
        if (choiceGroup && !choiceIcons.isEmpty() && this.isVisibleInActiveClip(y - 2, INGREDIENT_ROW_HEIGHT)) {
            this.choiceGroupNameAreas.add(new ChoiceGroupNameArea(icon, name, choiceIcons, choiceAlternatives, textX,
                    y - 2, namePartWidth, INGREDIENT_ROW_HEIGHT));
        }
        textX += namePartWidth;
        context.method_51433(this.field_22793, ": ", textX, y, INGREDIENT_TEXT_COLOR, false);
        textX += this.field_22793.method_1727(": ");
        context.method_51433(this.field_22793, totalText, textX, y, INGREDIENT_TOTAL_COLOR, false);
        textX += this.field_22793.method_1727(totalText);
        if (showMissing) {
            context.method_51433(this.field_22793, " / ", textX, y, INGREDIENT_TEXT_COLOR, false);
            textX += this.field_22793.method_1727(" / ");
            int missingColor = missingCount == 0 ? 0xFF55FF55 : INGREDIENT_MISSING_COLOR;
            context.method_51433(this.field_22793, missingText, textX, y, missingColor, false);
        }
    }

    private void renderCraftingGrid(GuiContext context, RecipeSummary summary, int x, int y, int mouseX, int mouseY) {
        RenderUtils.drawRect(context, x, y, RECIPE_PANEL_WIDTH, RECIPE_PANEL_HEIGHT, 0xFFB8B8B8);
        RenderUtils.drawOutlinedBox(context, x, y, RECIPE_PANEL_WIDTH, RECIPE_PANEL_HEIGHT, 0x00FFFFFF, 0xFF000000);
        RenderUtils.drawRect(context, x + 2, y + 2, RECIPE_PANEL_WIDTH - 4, 1, 0xFFFFFFFF);
        RenderUtils.drawRect(context, x + 2, y + 2, 1, RECIPE_PANEL_HEIGHT - 4, 0xFFFFFFFF);

        List<RecipeSlotSummary> slots = summary.inputSlots();
        int gridX = x + 34;
        int gridY = y + 24;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slotIndex = row * 3 + column;
                int slotX = gridX + column * 18;
                int slotY = gridY + row * 18;
                drawInputSlot(context, slotX, slotY);
                drawSlotItem(context, slotX + 1, slotY + 1,
                        slotIndex < slots.size() ? slots.get(slotIndex) : RecipeSlotSummary.EMPTY, mouseX, mouseY, 18,
                        18);
            }
        }

        drawCraftingArrow(context, x + 122, y + 44);

        int outputX = x + 166;
        int outputY = y + 35;
        drawOutputSlot(context, outputX, outputY);
        class_1799 displayOutput = displayOutputIcon(summary);
        drawSlotItem(
                context, outputX + 5, outputY + 5, new RecipeSlotSummary(displayOutput,
                        List.of(ItemStackTexts.name(displayOutput)), summary.outputCount()),
                mouseX, mouseY, 16, 16);
    }

    private static void drawInputSlot(GuiContext context, int x, int y) {
        context.method_25294(x, y, x + 18, y + 18, 0xFF373737);
        context.method_25294(x + 1, y + 1, x + 18, y + 2, 0xFFFFFFFF);
        context.method_25294(x + 1, y + 1, x + 2, y + 18, 0xFFFFFFFF);
        context.method_25294(x + 2, y + 2, x + 17, y + 17, 0xFF8B8B8B);
    }

    private static void drawCraftingArrow(GuiContext context, int x, int y) {
        int color = 0xFF555555;
        context.method_25294(x, y + 5, x + 16, y + 10, color);
        context.method_25294(x + 12, y + 2, x + 18, y + 13, color);
        context.method_25294(x + 18, y + 5, x + 22, y + 10, color);
    }

    private void drawOutputSlot(GuiContext context, int x, int y) {
        context.method_25294(x, y, x + 26, y + 26, 0xFFFFFFFF);
        context.method_25294(x + 1, y + 1, x + 25, y + 25, 0xFFE0E0E0);
        context.method_25294(x + 2, y + 2, x + 24, y + 24, 0xFFB8B8B8);
    }

    private void drawSlotItem(GuiContext context, int x, int y, RecipeSlotSummary slot, int mouseX, int mouseY,
            int hoverWidth, int hoverHeight) {
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
            if (entry.contains(mouseX, mouseY) && entry.state().enabled()) {
                // Material-list transfers should fill the crafting grid with as many
                // craftable batches as the player's inventory can supply.
                if (this.transferBridge.transfer(entry.summary(), this.transferContainerScreen, true)) {
                    this.field_22787.method_1507(this.transferContainerScreen);
                }
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

    private boolean handleCategoryTabClick(double mouseX, double mouseY) {
        for (int i = this.categoryTabs.size() - 1; i >= 0; i--) {
            CategoryTabArea area = this.categoryTabs.get(i);
            if (area.contains(mouseX, mouseY)) {
                return this.nativeDisplayBridge.openCategory(area.summary());
            }
        }
        return false;
    }

    private void renderCategoryTab(GuiContext context, RecipeSummary summary, int panelX, int panelY, int panelWidth,
            int mouseX, int mouseY) {
        int x = panelX + panelWidth + 1;
        int y = panelY - 5;
        if (!this.isVisibleInActiveClip(y, CATEGORY_TAB_SIZE)) {
            return;
        }
        boolean hovered = isInside(mouseX, mouseY, x, y, CATEGORY_TAB_SIZE, CATEGORY_TAB_SIZE);
        if (this.nativeDisplayBridge.renderCategoryTab(summary, context, x, y, hovered)) {
            this.categoryTabs.add(new CategoryTabArea(summary, x, y, CATEGORY_TAB_SIZE, CATEGORY_TAB_SIZE));
            if (hovered) {
                this.hoveredCategoryTooltip = this.nativeDisplayBridge.getCategoryTooltip(summary);
                class_1799 ingredient = this.nativeDisplayBridge.getCategoryIngredient(summary);
                if (!ingredient.method_7960()) {
                    this.hoveredStack = ingredient;
                }
            }
        }
    }

    private void renderPreferredRecipeButton(GuiContext context, RecipeSummary summary, String listPath, int left, int y,
            int width, int mouseX, int mouseY) {
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
        context.method_25290(net.minecraft.class_10799.field_56883, PREFERRED_WIDGETS_TEXTURE, starX, starY, (float) textureU, (float) textureV,
                PREFERRED_ICON_SIZE, PREFERRED_ICON_SIZE, PREFERRED_WIDGET_TEXTURE_SIZE, PREFERRED_WIDGET_TEXTURE_SIZE);
        if (hovered) {
            this.drawPreferredRecipeHoverBorder(context, buttonX, buttonY);
            this.hoveredPreferredRecipeTooltip = List.of(class_2561.method_43470(label));
        }

        if (this.isVisibleInActiveClip(buttonY, PREFERRED_BUTTON_SIZE)) {
            this.preferredRecipeButtons.add(new PreferredRecipeButtonArea(itemId, summary.recipeId(), listPath, buttonX,
                    buttonY, PREFERRED_BUTTON_SIZE, PREFERRED_BUTTON_SIZE));
        }
    }

    private void drawPreferredRecipeHoverBorder(GuiContext context, int x, int y) {
        int right = x + PREFERRED_BUTTON_SIZE;
        int bottom = y + PREFERRED_BUTTON_SIZE;
        context.method_25294(x, y, right, y + 1, 0xFFFFFFFF);
        context.method_25294(x, bottom - 1, right, bottom, 0xFFFFFFFF);
        context.method_25294(x, y, x + 1, bottom, 0xFFFFFFFF);
        context.method_25294(right - 1, y, right, bottom, 0xFFFFFFFF);
    }

    private void renderTransferButton(GuiContext context, RecipeSummary summary, int panelX, int panelY, int panelWidth,
            int panelHeight, int mouseX, int mouseY, float delta) {
        if (this.transferContainerScreen == null) {
            return;
        }

        RecipeTransferBridge.TransferState state = this.transferBridge.evaluate(summary, this.transferContainerScreen);
        if (!state.supported()) {
            return;
        }

        RecipeTransferBridge.Bounds bounds = this.transferBridge.buttonBounds(summary, panelX, panelY, panelWidth,
                panelHeight);
        if (!this.isVisibleInActiveClip(bounds.y(), bounds.height())) {
            return;
        }

        int visibleTop = Math.max(bounds.y(), this.activeClipTop);
        int visibleBottom = Math.min(bounds.y() + bounds.height(), this.activeClipBottom);
        int visibleHeight = Math.max(0, visibleBottom - visibleTop);
        boolean hovered = isInside(mouseX, mouseY, bounds.x(), visibleTop, bounds.width(), visibleHeight);
        renderTransferButtonBackground(context, bounds, state.enabled() ? (hovered ? 4 : 1) : 0);
        if (state.tinted()) {
            context.method_25296(
                    bounds.x() + 1,
                    bounds.y() + 1,
                    bounds.x() + bounds.width() - 1,
                    bounds.y() + bounds.height() - 1,
                    state.tint(),
                    state.tint());
        }
        int textColor = state.enabled() ? (hovered ? 0xFFFFFFA0 : 0xFFE0E0E0) : 0xFFA0A0A0;
        context.method_27534(
                this.field_22793,
                class_2561.method_43470(state.label()),
                bounds.x() + bounds.width() / 2,
                bounds.y() + (bounds.height() - 8) / 2,
                textColor);
        if (hovered) {
            this.hoveredTransferTooltip = state.tooltip();
        }
        TransferButtonEntry entry = new TransferButtonEntry(summary, state, bounds.x(), visibleTop, bounds.width(),
                visibleHeight);
        this.transferButtons.add(entry);
        if (hovered) {
            this.hoveredTransferEntry = entry;
        }
    }

    private static void renderTransferButtonBackground(GuiContext context, RecipeTransferBridge.Bounds bounds,
            int textureId) {
        int x = bounds.x();
        int y = bounds.y();
        int width = bounds.width();
        int height = bounds.height();
        float textureV = textureId * 80.0F;
        context.method_25291(net.minecraft.class_10799.field_56883, TRANSFER_BUTTON_TEXTURE, x, y, 0.0F, textureV,
                8, 8, 256, 512, 0xFFFFFFFF);
        context.method_25291(net.minecraft.class_10799.field_56883, TRANSFER_BUTTON_TEXTURE, x + width - 8, y,
                248.0F, textureV, 8, 8, 256, 512, 0xFFFFFFFF);
        context.method_25291(net.minecraft.class_10799.field_56883, TRANSFER_BUTTON_TEXTURE, x, y + height - 8,
                0.0F, textureV + 72.0F, 8, 8, 256, 512, 0xFFFFFFFF);
        context.method_25291(net.minecraft.class_10799.field_56883, TRANSFER_BUTTON_TEXTURE, x + width - 8, y + height - 8, 248.0F, textureV + 72.0F, 8, 8,
                256, 512, 0xFFFFFFFF);
    }

    private boolean renderTransferTooltip(GuiContext context, int mouseX, int mouseY) {
        if (this.hoveredTransferEntry == null) {
            return false;
        }

        class_332 drawContext = context.getGuiGraphics();
        // 1.21.11 batches GUI elements by stratum. Put JEI's missing-slot
        // highlights above the already queued recipe background and item icons.
        drawContext.method_71048();
        this.transferBridge.renderError(
                this.hoveredTransferEntry.summary(),
                this.hoveredTransferEntry.state(),
                drawContext,
                mouseX,
                mouseY);
        if (!this.hoveredTransferTooltip.isEmpty()) {
            drawContext.method_51434(this.field_22793, this.hoveredTransferTooltip, mouseX, mouseY);
        }
        return true;
    }

    private boolean renderPreferredRecipeTooltip(GuiContext context, int mouseX, int mouseY) {
        if (this.hoveredPreferredRecipeTooltip.isEmpty()) {
            return false;
        }

        context.getGuiGraphics().method_51434(this.field_22793, this.hoveredPreferredRecipeTooltip, mouseX, mouseY);
        return true;
    }

    private boolean renderCategoryTooltip(GuiContext context, int mouseX, int mouseY) {
        if (this.hoveredCategoryTooltip.isEmpty()) {
            return false;
        }
        context.getGuiGraphics().method_51434(this.field_22793, this.hoveredCategoryTooltip, mouseX, mouseY);
        return true;
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
        this.recipeListSnapshots.put(listPath,
                new RecipeListSnapshot(List.copyOf(recipes), depth, this.recipePositions(recipes, depth)));
    }

    private void startRecipeReorderAnimations() {
        this.recipeReorderAnimations.clear();
        for (Map.Entry<String, RecipeListSnapshot> entry : this.recipeListSnapshots.entrySet()) {
            String listPath = entry.getKey();
            RecipeListSnapshot snapshot = entry.getValue();
            List<RecipeSummary> recipes = ROOT_RECIPE_LIST.equals(listPath) ? this.summaries
                    : RecipeResolvers.applyPreferredOrder(snapshot.recipes());
            RecipeReorderAnimation animation = RecipeReorderAnimation.start(snapshot.positions(),
                    this.recipePositions(recipes, snapshot.depth()));
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
        if (Configs.shouldStopRecipeDecomposition(ItemStackTexts.id(ingredient.icon()))
                || isTerminalLogGroup(ingredient)) {
            return false;
        }

        return !this.recipesFor(ingredient).isEmpty();
    }

    private List<RecipeSummary> recipesFor(IngredientSummary ingredient) {
        if (Configs.shouldStopRecipeDecomposition(ItemStackTexts.id(ingredient.icon()))
                || isTerminalLogGroup(ingredient)) {
            return List.of();
        }

        String key = key(ingredient);
        return this.nestedRecipeCache.computeIfAbsent(key, ignored -> resolveRecipes(ingredient));
    }

    private static List<RecipeSummary> resolveRecipes(IngredientSummary ingredient) {
        if (ingredient.isChoiceGroup()) {
            List<RecipeSummary> union = unionChoiceGroupRecipes(ingredient);
            if (!union.isEmpty()) {
                return union;
            }
        }

        return RecipeResolvers.findRecipes(ingredient.icon(), ingredient.countTotal(), ingredient.countMissing());
    }

    // Decompose a choice-group ("任意X") ingredient by resolving EVERY
    // alternative's own recipe (oak_planks -> oak_log, spruce_planks ->
    // spruce_log, ...) and unioning each ingredient/slot across them, instead
    // of resolving only the representative icon (which would show a single
    // "橡木原木" row with no cycling). Mirrors MaterialTreeBuilder's
    // buildChoiceGroupChildren, but returns a RecipeSummary so it fits this
    // screen's own recipe-box rendering. Falls back to the representative-only
    // recipe (via resolveRecipes's caller) when any alternative lacks a
    // recipe, since a partial union can't be resolved safely.
    private static List<RecipeSummary> unionChoiceGroupRecipes(IngredientSummary ingredient) {
        List<class_1799> icons = ingredient.icons();
        if (icons.size() < 2) {
            return List.of();
        }

        List<RecipeSummary> perAlternative = new ArrayList<>(icons.size());
        for (class_1799 icon : icons) {
            List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, ingredient.countTotal(),
                    ingredient.countMissing());
            if (summaries.isEmpty() || summaries.get(0).ingredients().isEmpty()) {
                return List.of();
            }
            perAlternative.add(summaries.get(0));
        }

        RecipeSummary representative = perAlternative.get(0);
        List<IngredientSummary> repIngredients = representative.ingredients();
        List<IngredientSummary> unionIngredients = new ArrayList<>(repIngredients.size());
        for (int index = 0; index < repIngredients.size(); index++) {
            IngredientSummary repChild = repIngredients.get(index);
            Map<String, class_1799> unionIcons = new LinkedHashMap<>();
            List<String> unionNames = new ArrayList<>();
            IngredientUnion.addIngredient(repChild, unionIcons, unionNames);
            for (RecipeSummary alt : perAlternative) {
                if (index < alt.ingredients().size()) {
                    IngredientUnion.addIngredient(alt.ingredients().get(index), unionIcons, unionNames);
                }
            }

            List<class_1799> mergedIcons = new ArrayList<>(unionIcons.values());
            unionIngredients.add(new IngredientSummary(
                    repChild.icon(),
                    mergedIcons.isEmpty() ? List.of(repChild.icon()) : mergedIcons,
                    unionNames,
                    repChild.countPerCraft(),
                    repChild.countTotal(),
                    repChild.countMissing(),
                    repChild.maxStackSize()));
        }

        // Keep the representative's native layout for geometry and interaction,
        // while carrying every alternative output and input slot so the bridge
        // can display one synchronized wood family across the whole recipe tree.
        return List.of(new RecipeSummary(
                representative.category(),
                representative.recipeId(),
                representative.outputIcon(),
                perAlternative.stream().map(RecipeSummary::outputIcon).toList(),
                representative.outputCount(),
                representative.craftsTotal(),
                representative.craftsMissing(),
                unionIngredients,
                unionSlots(perAlternative, representative.inputSlots()),
                representative.gridWidth(),
                representative.gridHeight(),
                representative.shapeless(),
                representative.nativeDisplay()));
    }

    private static class_1799 displayOutputIcon(RecipeSummary summary) {
        return AlternativeItemDisplay.icon(summary.outputIcons(), summary.outputIcon());
    }

    private static List<RecipeSlotSummary> unionSlots(List<RecipeSummary> perAlternative,
            List<RecipeSlotSummary> repSlots) {
        List<RecipeSlotSummary> merged = new ArrayList<>(repSlots.size());
        for (int index = 0; index < repSlots.size(); index++) {
            RecipeSlotSummary repSlot = repSlots.get(index);
            if (repSlot.isEmpty()) {
                merged.add(repSlot);
                continue;
            }

            Map<String, class_1799> unionIcons = new LinkedHashMap<>();
            List<String> unionNames = new ArrayList<>();
            IngredientUnion.addSlot(repSlot, unionIcons, unionNames);
            for (RecipeSummary alt : perAlternative) {
                if (index < alt.inputSlots().size()) {
                    IngredientUnion.addSlot(alt.inputSlots().get(index), unionIcons, unionNames);
                }
            }

            List<class_1799> mergedIcons = new ArrayList<>(unionIcons.values());
            merged.add(new RecipeSlotSummary(
                    repSlot.icon(),
                    mergedIcons.isEmpty() ? List.of(repSlot.icon()) : mergedIcons,
                    unionNames,
                    repSlot.count()));
        }

        return merged;
    }

    // Logs (任意原木) are the terminal raw-gatherable resource — they never
    // decompose further. Without this guard, a stripping-axe "recipe" (log ->
    // stripped_log) some candidates carry gets mistaken for a real
    // decomposition step, producing a bogus nested "配方" box under 任意原木
    // itself (same root cause as the v1.6.76 minimal-list fix).
    private static boolean isTerminalLogGroup(IngredientSummary ingredient) {
        List<class_1799> icons = ingredient.icons().isEmpty() ? List.of(ingredient.icon()) : ingredient.icons();
        if (icons.isEmpty()) {
            return false;
        }

        for (class_1799 icon : icons) {
            if (icon.method_7960() || !isLogLike(icon)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isLogLike(class_1799 icon) {
        String path = itemPath(ItemStackTexts.id(icon));
        return path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_stem")
                || path.endsWith("_hyphae")
                || path.equals("bamboo_block")
                || path.equals("stripped_bamboo_block");
    }

    private static String itemPath(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
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
        this.expandedNestedRecipes
                .removeIf(expandedPath -> expandedPath.equals(path) || expandedPath.startsWith(path + "/"));
        this.nestedRecipeAnimations.removeDescendants(path);
    }

    private static String key(IngredientSummary ingredient) {
        return ItemStackTexts.id(ingredient.icon()) + "|" + ingredient.countTotal() + "|" + ingredient.countMissing();
    }

    private int displayPanelWidth(RecipeSummary summary, int maxWidth, int depth) {
        int boundedMaxWidth = Math.max(1, maxWidth);
        if (this.canUseNativeLayout(summary, depth)) {
            int fallbackWidth = Math.min(RECIPE_PANEL_WIDTH, boundedMaxWidth);
            try {
                int nativeWidth = this.nativeDisplayBridge.getDisplayWidth(summary, fallbackWidth);
                return Math.min(Math.max(1, nativeWidth), boundedMaxWidth);
            } catch (Throwable throwable) {
                this.disableNativeDisplay(summary, "display width", throwable);
            }
        }

        return Math.min(RECIPE_PANEL_WIDTH, boundedMaxWidth);
    }

    private int displayPanelHeight(RecipeSummary summary, int depth) {
        if (this.canUseNativeLayout(summary, depth)) {
            try {
                return Math.max(1, this.nativeDisplayBridge.getDisplayHeight(summary, RECIPE_PANEL_HEIGHT));
            } catch (Throwable throwable) {
                this.disableNativeDisplay(summary, "display height", throwable);
            }
        }

        return RECIPE_PANEL_HEIGHT;
    }

    private boolean renderNativeDisplay(RecipeSummary summary, GuiContext context, int x, int y, int width, int height,
            int mouseX, int mouseY, float delta, String path, int depth) {
        if (!this.shouldRenderNativeDisplay(summary, depth)) {
            return false;
        }

        int displayIndex = this.nativeDisplaysRenderedThisFrame + 1;
        LOGGER.debug(
                "Rendering native recipe display recipe={} category={} path={} depth={} bounds={}x{} at {},{} frameDisplay={}/{}",
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
                this.nativeDisplayAreas
                        .add(new NativeDisplayArea(summary, x, y, width, height, visibleTop, visibleBottom));
            }
            return true;
        } catch (Throwable throwable) {
            this.disableNativeDisplay(summary, "render", throwable);
            return false;
        }
    }

    private void renderScrollbar(GuiContext context, int mouseX, int mouseY, float delta, int top, int bottom) {
        if (this.scrollBar.getMaxValue() <= 0) {
            return;
        }

        this.scrollBar.render(context, mouseX, mouseY, delta, this.scrollbarX(), top, 8, bottom - top, this.contentHeight());
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

    private void applyPendingFocus(int contentTop) {
        if (this.pendingFocusPath == null || this.pendingFocusContentY == Integer.MIN_VALUE) {
            return;
        }

        // Put the requested material row near the top of the viewport, leaving
        // a small visual margin. The scrollbar clamps the value to its range.
        this.scrollBar.setValue(this.pendingFocusContentY - contentTop - 12);
        this.scrollRemainder = 0.0D;
        this.pendingFocusPath = null;
        this.pendingFocusContentY = Integer.MIN_VALUE;
    }

    private void captureHoveredStack(class_1799 stack, int mouseX, int mouseY, int x, int y, int width, int height) {
        if (mouseY >= this.activeClipTop && mouseY <= this.activeClipBottom
                && isInside(mouseX, mouseY, x, y, width, height)) {
            this.hoveredStack = stack;
        }
    }

    private boolean renderNativeTooltip(GuiContext context, int mouseX, int mouseY) {
        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        return this.dispatchNativeDisplay(area, "tooltip", summary -> this.nativeDisplayBridge.renderTooltip(
                summary,
                context,
                this.field_22793,
                area.x(),
                area.y(),
                area.width(),
                area.height(),
                mouseX,
                mouseY));
    }

    // Hovering a choice-group ("任意X") name shows the concrete items it
    // stands for, distinct from hovering its item icon (which shows the
    // vanilla single-item tooltip via hoveredStack instead). Same icon+name
    // grid style as WidgetMaterialListEntryMixin#lmlp$renderChoiceGrid, used
    // for the equivalent hover in the minimal material list / inline recipe
    // panel, so the two choice-group tooltips look identical everywhere.
    private boolean renderChoiceGroupTooltip(GuiContext context, int mouseX, int mouseY) {
        ChoiceGroupNameArea area = this.choiceGroupNameAreaAt(mouseX, mouseY);
        if (area == null) {
            return false;
        }

        List<MinimalSubMaterialListView.TooltipCandidate> candidates = choiceGroupCandidates(area);
        if (candidates.isEmpty()) {
            return false;
        }

        return this.renderChoiceGrid(context, mouseX, mouseY, area.icon(),
                MinimalSubMaterialListView.emphasizeChoiceGroupName(area.name()), candidates);
    }

    private static List<MinimalSubMaterialListView.TooltipCandidate> choiceGroupCandidates(ChoiceGroupNameArea area) {
        List<class_1799> icons = area.icons();
        List<String> names = area.alternatives();
        if (icons.isEmpty()) {
            return List.of();
        }

        boolean namesParallel = names.size() == icons.size();
        List<MinimalSubMaterialListView.TooltipCandidate> candidates = new ArrayList<>(icons.size());
        for (int index = 0; index < icons.size(); index++) {
            class_1799 icon = icons.get(index);
            if (icon.method_7960()) {
                continue;
            }
            String name = namesParallel ? names.get(index) : ItemStackTexts.name(icon);
            candidates.add(new MinimalSubMaterialListView.TooltipCandidate(icon.method_7972(), name));
        }
        return candidates;
    }

    // Ported from WidgetMaterialListEntryMixin#lmlp$renderChoiceGrid so both
    // choice-group hover tooltips (minimal list / inline recipe panel, and
    // this full recipe-detail screen) render pixel-identical icon+name grids.
    private boolean renderChoiceGrid(GuiContext context, int mouseX, int mouseY, class_1799 headerStack,
            String headerName, List<MinimalSubMaterialListView.TooltipCandidate> candidates) {
        int maxPanelWidth = Math.max(120, this.field_22789 - HOVER_TOOLTIP_MARGIN * 2);
        int maxContentWidth = Math.max(80, maxPanelWidth - HOVER_TOOLTIP_PADDING * 2);
        int maxCandidateNameWidth = 0;
        for (MinimalSubMaterialListView.TooltipCandidate candidate : candidates) {
            maxCandidateNameWidth = Math.max(maxCandidateNameWidth, this.field_22793.method_1727(candidate.name()));
        }

        int naturalColumnWidth = HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP + maxCandidateNameWidth;
        int minColumnWidth = HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP + 60;
        int maxRows = Configs.Generic.HOVER_PANEL_MAX_ROWS.getIntegerValue();
        int requestedColumns = Math.max(1, (candidates.size() + maxRows - 1) / maxRows);
        int columns = requestedColumns;
        while (columns > 1
                && maxContentWidth < minColumnWidth * columns + CHOICE_TOOLTIP_COLUMN_GAP * (columns - 1)) {
            columns--;
        }
        int columnGap = columns > 1 ? CHOICE_TOOLTIP_COLUMN_GAP : 0;
        int columnWidth = columns > 1
                ? Math.min(naturalColumnWidth, Math.max(minColumnWidth,
                        (maxContentWidth - columnGap * (columns - 1)) / columns))
                : Math.min(naturalColumnWidth, maxContentWidth);
        // The title/header is separate: fill up to maxRows candidate items in
        // each column before moving right. Only rebalance when screen width
        // forces fewer columns than requested.
        int rowsPerColumn = columns == requestedColumns
                ? Math.min(maxRows, candidates.size())
                : (candidates.size() + columns - 1) / columns;
        int candidateContentWidth = columns * columnWidth + columnGap * (columns - 1);

        String headerText = this.truncateToWidth(
                headerName,
                Math.max(20, maxContentWidth - HOVER_TOOLTIP_ICON_SIZE - HOVER_TOOLTIP_ICON_GAP));
        int headerWidth = HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP + this.field_22793.method_1727(headerText);
        int contentWidth = Math.min(maxContentWidth, Math.max(headerWidth, candidateContentWidth));
        int panelWidth = contentWidth + HOVER_TOOLTIP_PADDING * 2;
        int panelHeight = HOVER_TOOLTIP_PADDING * 2
                + Math.max(HOVER_TOOLTIP_ICON_SIZE, HOVER_TOOLTIP_LINE_HEIGHT)
                + HOVER_TOOLTIP_HEADER_GAP
                + rowsPerColumn * CHOICE_TOOLTIP_ROW_HEIGHT;

        PanelBounds bounds = this.hoverTooltipBounds(mouseX, mouseY, panelWidth, panelHeight);
        int panelX = bounds.x();
        int panelY = bounds.y();
        int contentX = panelX + HOVER_TOOLTIP_PADDING;
        int headerY = panelY + HOVER_TOOLTIP_PADDING + 4;
        int rowsY = headerY + Math.max(HOVER_TOOLTIP_ICON_SIZE, HOVER_TOOLTIP_LINE_HEIGHT) + HOVER_TOOLTIP_HEADER_GAP;

        // Native JEI layouts queue item and text vertices. Flush the existing
        // recipe batches first so they cannot be submitted over this tooltip.
        context.method_51448().pushMatrix();
        // Match vanilla's own tooltip Z offset (see GuiContext's drawTooltip),
        // not the +200 this was ported with. Now that the recipe box behind
        // this panel can render a recipe viewer's native display, its item
        // slots sit at a Z high enough to paint over a +200 panel — the log
        // icons and even the unrelated root recipe's output icon bled through
        // on top of this tooltip. +400 is the same headroom vanilla tooltips
        // rely on to stay above arbitrary GUI content.
        drawTooltipBox(context, panelX, panelY, panelWidth, panelHeight, 0xF0000000, 0xFF999999);

        context.method_25294(contentX, headerY - 4, contentX + HOVER_TOOLTIP_ICON_SIZE,
                headerY - 4 + HOVER_TOOLTIP_ICON_SIZE, 0x20FFFFFF);
        context.method_51427(headerStack, contentX, headerY - 4);
        context.method_51433(this.field_22793, headerText, contentX + HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP,
                headerY, 0xFFFFFFFF, false);

        for (int index = 0; index < candidates.size(); index++) {
            MinimalSubMaterialListView.TooltipCandidate candidate = candidates.get(index);
            int column = index / rowsPerColumn;
            int row = index % rowsPerColumn;
            int rowX = contentX + column * (columnWidth + columnGap);
            int rowTop = rowsY + row * CHOICE_TOOLTIP_ROW_HEIGHT;
            int textX = rowX + HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP;
            int maxNameWidth = Math.max(20, columnWidth - HOVER_TOOLTIP_ICON_SIZE - HOVER_TOOLTIP_ICON_GAP);
            String itemName = this.truncateToWidth(candidate.name(), maxNameWidth);

            context.method_25294(rowX, rowTop + 1, rowX + HOVER_TOOLTIP_ICON_SIZE,
                    rowTop + 1 + HOVER_TOOLTIP_ICON_SIZE, 0x20FFFFFF);
            context.method_51427(candidate.icon(), rowX, rowTop + 1);
            context.method_51433(this.field_22793, itemName, textX, rowTop + 5, 0xFFFFFFFF, false);
        }

        context.method_51448().popMatrix();
        return true;
    }

    private static void drawTooltipBox(GuiContext context, int x, int y, int width, int height,
            int background, int border) {
        context.method_25294(x, y, x + width, y + height, background);
        context.method_25294(x, y, x + width, y + 1, border);
        context.method_25294(x, y + height - 1, x + width, y + height, border);
        context.method_25294(x, y, x + 1, y + height, border);
        context.method_25294(x + width - 1, y, x + width, y + height, border);
    }

    private PanelBounds hoverTooltipBounds(int mouseX, int mouseY, int panelWidth, int panelHeight) {
        int screenWidth = this.field_22789;
        int screenHeight = this.field_22790;
        int minX = HOVER_TOOLTIP_MARGIN;
        int minY = HOVER_TOOLTIP_MARGIN;
        int maxX = Math.max(minX, screenWidth - panelWidth - HOVER_TOOLTIP_MARGIN);
        int maxY = Math.max(minY, screenHeight - panelHeight - HOVER_TOOLTIP_MARGIN);
        int x = mouseX + HOVER_TOOLTIP_CURSOR_OFFSET;
        int y = mouseY + HOVER_TOOLTIP_CURSOR_OFFSET;

        if (x > maxX) {
            x = mouseX - HOVER_TOOLTIP_CURSOR_OFFSET - panelWidth;
        }
        if (y > maxY) {
            y = mouseY - HOVER_TOOLTIP_CURSOR_OFFSET - panelHeight;
        }

        x = clamp(x, minX, maxX);
        y = clamp(y, minY, maxY);
        return new PanelBounds(x, y);
    }

    private String truncateToWidth(String text, int maxWidth) {
        if (this.field_22793.method_1727(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int suffixWidth = this.field_22793.method_1727(suffix);
        int end = text.length();
        while (end > 0 && this.field_22793.method_1727(text.substring(0, end)) + suffixWidth > maxWidth) {
            end--;
        }

        return end > 0 ? text.substring(0, end) + suffix : suffix;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private ChoiceGroupNameArea choiceGroupNameAreaAt(double mouseX, double mouseY) {
        for (int i = this.choiceGroupNameAreas.size() - 1; i >= 0; i--) {
            ChoiceGroupNameArea area = this.choiceGroupNameAreas.get(i);
            if (area.contains(mouseX, mouseY)) {
                return area;
            }
        }

        return null;
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
                LOGGER.debug("Native recipe display frame limit reached; falling back after {} displays.",
                        MAX_NATIVE_DISPLAYS_PER_FRAME);
            }
            return false;
        }

        return true;
    }

    private boolean isDisplayPanelVisible(int y, int height) {
        return y + height >= this.activeClipTop - NATIVE_RENDER_CLIP_PADDING
                && y <= this.activeClipBottom + NATIVE_RENDER_CLIP_PADDING;
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
            LOGGER.warn("Disabling native recipe display rendering for category {} after {} failed on recipe {}.",
                    category, action, summary.recipeId(), throwable);
        }
    }

    private static String nativeDisplayCategory(RecipeSummary summary) {
        return summary.category() == null ? "<unknown>" : summary.category();
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
            Class<?> bridgeClass = Class.forName("io.github.huanmeng06.lmlp.recipe.jei.JeiNativeDisplayBridge");
            return (RecipeNativeDisplayBridge) bridgeClass.getDeclaredConstructor().newInstance();
        } catch (Throwable throwable) {
            return RecipeNativeDisplayBridge.DISABLED;
        }
    }

    private static RecipeTransferBridge createTransferBridge() {
        try {
            Class<?> bridgeClass = Class.forName("io.github.huanmeng06.lmlp.recipe.jei.JeiRecipeTransferBridge");
            return (RecipeTransferBridge) bridgeClass.getDeclaredConstructor().newInstance();
        } catch (Throwable throwable) {
            return RecipeTransferBridge.DISABLED;
        }
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void drawOutline(GuiContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }

        RenderUtils.drawRect(context, x, y, width, 1, color);
        if (height > 1) {
            RenderUtils.drawRect(context, x, y + height - 1, width, 1, color);
            RenderUtils.drawRect(context, x, y, 1, height, color);
            RenderUtils.drawRect(context, x + width - 1, y, 1, height, color);
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

    private static class_465<?> findTransferContainer(class_437 screen) {
        class_465<?> handledParent = findHandledParent(screen);
        if (handledParent != null) {
            return handledParent;
        }

        net.minecraft.class_310 client = net.minecraft.class_310.method_1551();
        return client != null && client.field_1724 != null
                ? new class_490(client.field_1724)
                : null;
    }

    private record ToggleArea(String path, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y
                    && mouseY < this.y + this.height;
        }
    }

    private record ChoiceGroupNameArea(class_1799 icon, String name, List<class_1799> icons, List<String> alternatives,
            int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y
                    && mouseY < this.y + this.height;
        }
    }

    private record PanelBounds(int x, int y) {
    }

    private record NativeDisplayArea(RecipeSummary summary, int x, int y, int width, int height, int visibleTop,
            int visibleBottom) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.visibleTop
                    && mouseY < this.visibleBottom;
        }
    }

    private record TransferButtonEntry(RecipeSummary summary, RecipeTransferBridge.TransferState state, int x, int y,
            int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y
                    && mouseY < this.y + this.height;
        }
    }

    private record PreferredRecipeButtonArea(String itemId, String recipeId, String listPath, int x, int y, int width,
            int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y
                    && mouseY < this.y + this.height;
        }
    }

    private record CategoryTabArea(RecipeSummary summary, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y
                    && mouseY < this.y + this.height;
        }
    }

    private record RecipeListSnapshot(List<RecipeSummary> recipes, int depth, Map<String, Integer> positions) {
    }

    private record RecipeReorderAnimation(long startTimeMs, Map<String, Integer> startOffsets) {
        private static final RecipeReorderAnimation NONE = new RecipeReorderAnimation(0L, Map.of());

        private static RecipeReorderAnimation start(Map<String, Integer> oldPositions,
                Map<String, Integer> newPositions) {
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

            float elapsed = (float) (System.currentTimeMillis() - this.startTimeMs)
                    / (float) ExpandAnimationTracker.DURATION_MS;
            float remaining = 1.0F - ExpandAnimationTracker.easeOutCubic(elapsed);
            return targetY + Math.round(offset * remaining);
        }

        private boolean isFinished() {
            return this.startOffsets.isEmpty()
                    || System.currentTimeMillis() - this.startTimeMs >= ExpandAnimationTracker.DURATION_MS;
        }
    }

    @FunctionalInterface
    private interface NativeDisplayAction {
        boolean apply(RecipeSummary summary);
    }

    private record Layout(int left, int headerTop, int contentWidth, int headerWidth, int backButtonX) {
    }
}
