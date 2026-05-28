package io.github.huanmeng06.lmlp.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.recipe.AlternativeItemDisplay;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.MaterialTreeBuilder;
import io.github.huanmeng06.lmlp.recipe.MaterialTreeNode;
import io.github.huanmeng06.lmlp.recipe.RecipeSlotSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_437;

public class RecipeDetailScreen extends class_437 {
    private static final int BACK_BUTTON_WIDTH = 112;
    private static final int BACK_BUTTON_HEIGHT = 20;
    private static final int REI_PANEL_WIDTH = 254;
    private static final int REI_PANEL_HEIGHT = 104;
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
    private static final int INGREDIENT_TOGGLE_WIDTH = 10;
    private static final int INGREDIENT_ICON_OFFSET = 14;
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
    private final Map<String, Boolean> treeSupportCache = new HashMap<>();
    private final Map<String, MaterialTreeNode> treeCache = new HashMap<>();
    private final Set<String> expandedTreeNodes = new HashSet<>();
    private class_1799 hoveredStack = class_1799.field_8037;
    private int clipTop;
    private int clipBottom;
    private boolean draggingScrollbar;

    public RecipeDetailScreen(class_437 parent, class_1799 target, int totalCount, int missingCount, List<RecipeSummary> summaries) {
        super(class_2561.method_43470("lmlp.gui.recipe_detail.title"));
        this.parent = parent;
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
        if (area != null && this.nativeDisplayBridge.mouseScrolled(area.summary(), mouseX, mouseY, verticalAmount)) {
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

        NativeDisplayArea area = this.nativeDisplayAreaAt(mouseX, mouseY);
        if (area != null && this.nativeDisplayBridge.mouseClicked(area.summary(), mouseX, mouseY, button)) {
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
        if (area != null && this.nativeDisplayBridge.mouseDragged(area.summary(), mouseX, mouseY, button, deltaX, deltaY)) {
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
        if (area != null && this.nativeDisplayBridge.mouseReleased(area.summary(), mouseX, mouseY, button)) {
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
        this.nativeDisplayAreas.clear();
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
                int boxHeight = this.recipeBoxHeight(summary);
                this.renderRecipeBox(context, summary, index, layout.left(), y, layout.contentWidth(), boxHeight, mouseX, mouseY, delta);
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

    private void renderRecipeBox(class_332 context, RecipeSummary summary, int index, int left, int y, int width, int boxHeight, int mouseX, int mouseY, float delta) {
        RenderUtils.drawOutlinedBox(left, y, width, boxHeight, 0xDD000000, 0xFF777777);
        context.method_51427(summary.outputIcon(), left + 10, y + 10);
        this.captureHoveredStack(summary.outputIcon(), mouseX, mouseY, left + 10, y + 10, 16, 16);
        context.method_51433(this.field_22793, RecipeSummaryFormatter.header(summary, index), left + 34, y + 12, 0xFFFFFFFF, false);

        int panelWidth = this.displayPanelWidth(summary, width - 36);
        int panelHeight = this.displayPanelHeight(summary);
        int panelX = left + 18;
        int panelY = y + 38;
        if (!this.renderNativeDisplay(summary, context, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY, delta)) {
            this.renderCraftingGrid(context, summary, panelX, panelY, mouseX, mouseY);
        }

        int lineY = panelY + panelHeight + 16;
        context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.recipe.ingredients_total"), left + 14, lineY, 0xFFAAAAAA, false);
        lineY += 18;

        for (IngredientSummary ingredient : summary.ingredients()) {
            this.renderIngredientLine(context, left + 14, lineY, 0, ingredient, mouseX, mouseY);
            lineY += INGREDIENT_ROW_HEIGHT;

            MaterialTreeNode root = this.expandedTree(ingredient);
            if (root != null) {
                lineY = this.renderIngredientChildren(context, left + 14, lineY, root.children(), 1, mouseX, mouseY);
            }
        }
    }

    private void renderIngredientLine(class_332 context, int left, int y, int depth, IngredientSummary ingredient, int mouseX, int mouseY) {
        MaterialTreeNode root = this.expandedTree(ingredient);
        this.renderMaterialLine(
                context,
                left,
                y,
                depth,
                this.hasTree(ingredient),
                root != null,
                AlternativeItemDisplay.icon(ingredient),
                RecipeSummaryFormatter.ingredientName(ingredient),
                RecipeSummaryFormatter.totalCount(ingredient),
                RecipeSummaryFormatter.missingCount(ingredient),
                ingredient.countMissing() != ingredient.countTotal(),
                mouseX,
                mouseY);
    }

    private int renderIngredientChildren(class_332 context, int left, int y, List<MaterialTreeNode> nodes, int depth, int mouseX, int mouseY) {
        int lineY = y;
        for (MaterialTreeNode node : nodes) {
            boolean expanded = this.expandedTreeNodes.contains(node.path());
            this.renderMaterialLine(
                    context,
                    left,
                    lineY,
                    depth,
                    node.hasChildren(),
                    expanded,
                    AlternativeItemDisplay.icon(node),
                    node.name(),
                    CountFormatter.format(node.totalCount(), node.maxStackSize()),
                    CountFormatter.format(node.missingCount(), node.maxStackSize()),
                    node.missingCount() != node.totalCount(),
                    mouseX,
                    mouseY);
            lineY += INGREDIENT_ROW_HEIGHT;
            if (node.hasChildren() && expanded) {
                lineY = this.renderIngredientChildren(context, left, lineY, node.children(), depth + 1, mouseX, mouseY);
            }
        }

        return lineY;
    }

    private void renderMaterialLine(class_332 context, int left, int y, int depth, boolean hasTree, boolean expanded, class_1799 icon, String name, String totalText, String missingText, boolean showMissing, int mouseX, int mouseY) {
        int rowX = left + depth * INGREDIENT_TREE_INDENT_WIDTH;
        if (hasTree) {
            context.method_51433(this.field_22793, expanded ? "v" : ">", rowX + 2, y, 0xFFFFFFFF, false);
        }

        int iconX = rowX + INGREDIENT_ICON_OFFSET;
        context.method_51427(icon, iconX, y - 5);
        this.captureHoveredStack(icon, mouseX, mouseY, iconX, y - 5, 16, 16);

        String line = name + ": " + totalText;
        if (showMissing) {
            line += " / " + missingText;
        }
        context.method_51433(this.field_22793, line, rowX + INGREDIENT_ICON_OFFSET + 24, y, 0xFFFFFFFF, false);
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
        Layout layout = this.layout();
        int contentTop = layout.headerTop() + HEADER_HEIGHT + 12;
        int contentBottom = this.field_22790 - PAGE_BOTTOM_MARGIN;
        if (mouseY < contentTop || mouseY >= contentBottom) {
            return false;
        }

        int y = contentTop - this.scrollBar.getValue();
        for (RecipeSummary summary : this.summaries) {
            int boxHeight = this.recipeBoxHeight(summary);
            ToggleTarget target = this.toggleTargetAt(summary, layout.left(), y, (int) mouseX, (int) mouseY);
            if (!target.isNone()) {
                if (target.ingredient() != null) {
                    this.toggleIngredientTree(target.ingredient());
                } else {
                    this.toggleTreeNode(target.nodePath());
                }
                return true;
            }
            y += boxHeight + 10;
        }

        return false;
    }

    private ToggleTarget toggleTargetAt(RecipeSummary summary, int left, int boxY, int mouseX, int mouseY) {
        int panelHeight = this.displayPanelHeight(summary);
        int lineY = boxY + 38 + panelHeight + 16 + 18;

        for (IngredientSummary ingredient : summary.ingredients()) {
            if (this.hasTree(ingredient) && isToggleHit(left + 14, lineY, 0, mouseX, mouseY)) {
                return ToggleTarget.ingredient(ingredient);
            }
            lineY += INGREDIENT_ROW_HEIGHT;

            MaterialTreeNode root = this.expandedTree(ingredient);
            if (root != null) {
                ToggleScan scan = this.scanChildren(root.children(), left + 14, lineY, 1, mouseX, mouseY);
                if (!scan.target().isNone()) {
                    return scan.target();
                }
                lineY = scan.nextY();
            }
        }

        return ToggleTarget.NONE;
    }

    private ToggleScan scanChildren(List<MaterialTreeNode> nodes, int left, int y, int depth, int mouseX, int mouseY) {
        int lineY = y;
        for (MaterialTreeNode node : nodes) {
            if (node.hasChildren() && isToggleHit(left, lineY, depth, mouseX, mouseY)) {
                return new ToggleScan(ToggleTarget.node(node.path()), lineY + INGREDIENT_ROW_HEIGHT);
            }

            lineY += INGREDIENT_ROW_HEIGHT;
            if (node.hasChildren() && this.expandedTreeNodes.contains(node.path())) {
                ToggleScan scan = this.scanChildren(node.children(), left, lineY, depth + 1, mouseX, mouseY);
                if (!scan.target().isNone()) {
                    return scan;
                }
                lineY = scan.nextY();
            }
        }

        return new ToggleScan(ToggleTarget.NONE, lineY);
    }

    private int contentHeight() {
        if (this.summaries.isEmpty()) {
            return 48;
        }

        int height = 0;
        for (RecipeSummary summary : this.summaries) {
            height += this.recipeBoxHeight(summary) + 10;
        }
        return height;
    }

    private int recipeBoxHeight(RecipeSummary summary) {
        return 72 + this.displayPanelHeight(summary) + this.visibleIngredientRows(summary) * INGREDIENT_ROW_HEIGHT;
    }

    private int visibleIngredientRows(RecipeSummary summary) {
        int rows = 0;
        for (IngredientSummary ingredient : summary.ingredients()) {
            rows++;
            MaterialTreeNode root = this.expandedTree(ingredient);
            if (root != null) {
                rows += this.visibleChildRows(root);
            }
        }

        return rows;
    }

    private int visibleChildRows(MaterialTreeNode root) {
        if (!this.expandedTreeNodes.contains(root.path())) {
            return 0;
        }

        return this.visibleNodeRows(root.children());
    }

    private int visibleNodeRows(List<MaterialTreeNode> nodes) {
        int rows = 0;
        for (MaterialTreeNode node : nodes) {
            rows++;
            if (node.hasChildren() && this.expandedTreeNodes.contains(node.path())) {
                rows += this.visibleNodeRows(node.children());
            }
        }

        return rows;
    }

    private boolean hasTree(IngredientSummary ingredient) {
        String key = key(ingredient);
        Boolean cached = this.treeSupportCache.get(key);
        if (cached != null) {
            return cached;
        }

        boolean supported = MaterialTreeBuilder.hasChildren(ingredient.icon(), ingredient.countTotal(), ingredient.countMissing());
        this.treeSupportCache.put(key, supported);
        return supported;
    }

    private void toggleIngredientTree(IngredientSummary ingredient) {
        MaterialTreeNode root = this.treeFor(ingredient);
        if (!root.hasChildren()) {
            this.treeSupportCache.put(key(ingredient), false);
            return;
        }

        this.treeSupportCache.put(key(ingredient), true);
        if (this.expandedTreeNodes.contains(root.path())) {
            this.collapseTreeNode(root.path());
        } else {
            this.expandedTreeNodes.add(root.path());
        }
    }

    private MaterialTreeNode expandedTree(IngredientSummary ingredient) {
        MaterialTreeNode root = this.treeCache.get(key(ingredient));
        if (root == null || !this.expandedTreeNodes.contains(root.path())) {
            return null;
        }

        return root;
    }

    private MaterialTreeNode treeFor(IngredientSummary ingredient) {
        String key = key(ingredient);
        return this.treeCache.computeIfAbsent(key, ignored -> MaterialTreeBuilder.build(
                ingredient.icon(),
                RecipeSummaryFormatter.ingredientName(ingredient),
                ingredient.countTotal(),
                ingredient.countMissing(),
                "detail:" + key));
    }

    private void toggleTreeNode(String path) {
        if (this.expandedTreeNodes.contains(path)) {
            this.collapseTreeNode(path);
        } else {
            this.expandedTreeNodes.add(path);
        }
    }

    private void collapseTreeNode(String path) {
        this.expandedTreeNodes.removeIf(expandedPath -> expandedPath.equals(path) || expandedPath.startsWith(path + "/"));
    }

    private static boolean isToggleHit(int left, int y, int depth, int mouseX, int mouseY) {
        int toggleX = left + depth * INGREDIENT_TREE_INDENT_WIDTH;
        return mouseX >= toggleX && mouseX < toggleX + INGREDIENT_TOGGLE_WIDTH && mouseY >= y - 2 && mouseY < y + INGREDIENT_ROW_HEIGHT - 2;
    }

    private static String key(IngredientSummary ingredient) {
        return ItemStackTexts.id(ingredient.icon()) + "|" + ingredient.countTotal() + "|" + ingredient.countMissing();
    }

    private int displayPanelWidth(RecipeSummary summary, int maxWidth) {
        int boundedMaxWidth = Math.max(1, maxWidth);
        if (this.nativeDisplayBridge.canRender(summary)) {
            int fallbackWidth = Math.min(REI_PANEL_WIDTH, boundedMaxWidth);
            int nativeWidth = this.nativeDisplayBridge.getDisplayWidth(summary, fallbackWidth);
            return Math.min(Math.max(1, nativeWidth), boundedMaxWidth);
        }

        return Math.min(REI_PANEL_WIDTH, boundedMaxWidth);
    }

    private int displayPanelHeight(RecipeSummary summary) {
        if (this.nativeDisplayBridge.canRender(summary)) {
            return Math.max(1, this.nativeDisplayBridge.getDisplayHeight(summary, REI_PANEL_HEIGHT));
        }

        return REI_PANEL_HEIGHT;
    }

    private boolean renderNativeDisplay(RecipeSummary summary, class_332 context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
        if (!this.nativeDisplayBridge.canRender(summary)) {
            return false;
        }

        try {
            this.nativeDisplayBridge.render(summary, context, x, y, width, height, mouseX, mouseY, delta);
            this.nativeDisplayAreas.add(new NativeDisplayArea(summary, x, y, width, height));
            return true;
        } catch (Throwable throwable) {
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
        return area != null && this.nativeDisplayBridge.renderTooltip(area.summary(), context, this.field_22793, mouseX, mouseY);
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

    private record ToggleTarget(IngredientSummary ingredient, String nodePath) {
        private static final ToggleTarget NONE = new ToggleTarget(null, null);

        private static ToggleTarget ingredient(IngredientSummary ingredient) {
            return new ToggleTarget(ingredient, null);
        }

        private static ToggleTarget node(String nodePath) {
            return new ToggleTarget(null, nodePath);
        }

        private boolean isNone() {
            return this.ingredient == null && this.nodePath == null;
        }
    }

    private record ToggleScan(ToggleTarget target, int nextY) {
    }

    private record NativeDisplayArea(RecipeSummary summary, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private record Layout(int left, int headerTop, int contentWidth, int headerWidth, int backButtonX) {
    }
}
