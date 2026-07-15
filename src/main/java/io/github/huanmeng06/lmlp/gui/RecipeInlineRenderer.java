package io.github.huanmeng06.lmlp.gui;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.recipe.AlternativeItemDisplay;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.MaterialTreeNode;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import fi.dy.masa.malilib.render.GuiContext;

public final class RecipeInlineRenderer {
    private static final int INGREDIENT_HEIGHT = 22;
    private static final int PADDING = 8;
    private static final int INNER_BOTTOM_PADDING = 4;
    private static final int ENTRY_BOTTOM_GAP = 4;
    private static final int INGREDIENT_TOGGLE_WIDTH = 18;
    private static final int INGREDIENT_ICON_OFFSET = 20;
    private static final int TREE_INDENT_WIDTH = 18;
    private static final int HEADER_ROW_HEIGHT = 18;

    private RecipeInlineRenderer() {
    }

    public static int getHeight(List<RecipeSummary> summaries, int width) {
        MaterialListPlusState.pruneTreeAnimations();
        if (summaries.isEmpty()) {
            return 48;
        }

        RecipeSummary summary = summaries.get(0);
        int lineBudget = Math.max(160, width) - PADDING * 2;
        int height = 64 + headerExtraHeight(summary, lineBudget) + visibleIngredientHeight(summary) + INNER_BOTTOM_PADDING;
        return height;
    }

    public static int getOuterHeight(List<RecipeSummary> summaries, int width) {
        return getHeight(summaries, width) + ENTRY_BOTTOM_GAP;
    }

    public static int getOuterHeight(List<RecipeSummary> summaries, int width, float progress) {
        return Math.round(getOuterHeight(summaries, width) * progress);
    }

    public static int getTargetOuterHeight(List<RecipeSummary> summaries, int width) {
        return getTargetHeight(summaries, width) + ENTRY_BOTTOM_GAP;
    }

    public static void render(WidgetBase widget, GuiContext context, int x, int y, int width, List<RecipeSummary> summaries, int mouseX, int mouseY) {
        render(widget, context, x, y, width, summaries, getOuterHeight(summaries, width), mouseX, mouseY);
    }

    public static void render(WidgetBase widget, GuiContext context, int x, int y, int width, List<RecipeSummary> summaries, int visibleOuterHeight, int mouseX, int mouseY) {
        int height = getHeight(summaries, width);
        int panelWidth = Math.max(160, width);
        int visibleHeight = Math.max(0, Math.min(height, visibleOuterHeight));
        if (visibleHeight <= 0) {
            return;
        }

        RenderUtils.drawRect(context, x, y, panelWidth, visibleHeight, 0xDD000000);
        int textX = x + PADDING;
        if (visibleHeight <= 2) {
            drawOutline(context, x, y, panelWidth, visibleHeight, 0xFF777777);
            return;
        }

        context.enableScissor(x + 1, y + 1, x + panelWidth - 1, y + visibleHeight - 1);

        if (summaries.isEmpty()) {
            widget.drawString(context, textX, y + 16, 0xFFFFCC66, StringUtils.translate("lmlp.label.recipe.none"));
            context.disableScissor();
            drawOutline(context, x, y, panelWidth, visibleHeight, 0xFF777777);
            return;
        }

        int lineBudget = panelWidth - PADDING * 2;
        int rightEdge = x + panelWidth - PADDING;

        RecipeSummary summary = summaries.get(0);
        int cursorY = y + PADDING;
        String multipleLabel = summaries.size() > 1
                ? StringUtils.translate("lmlp.label.recipe.multiple")
                : "";
        int multipleWidth = multipleLabel.isEmpty() ? 0 : StringUtils.getStringWidth(multipleLabel) + 4;
        String itemName = truncateToWidth(ItemStackTexts.name(summary.outputIcon()), lineBudget - 24 - multipleWidth);
        context.renderItem(summary.outputIcon(), textX, cursorY);
        int itemNameX = textX + 24;
        boolean titleHovered = isInside(mouseX, mouseY, itemNameX, cursorY + 2,
                StringUtils.getStringWidth(itemName), 14);
        String renderedItemName = titleHovered
                ? GuiBase.TXT_BOLD + GuiBase.TXT_UNDERLINE + itemName + GuiBase.TXT_RST
                : itemName;
        widget.drawString(context, itemNameX, cursorY + 4, 0xFFFFFFFF, renderedItemName);
        if (!multipleLabel.isEmpty()) {
            widget.drawString(
                    context,
                    itemNameX + StringUtils.getStringWidth(renderedItemName) + 4,
                    cursorY + 4,
                    0xFFFFAA00,
                    multipleLabel);
        }
        cursorY += 24;

        for (String headerLine : headerLines(summary, lineBudget)) {
            widget.drawString(context, textX, cursorY, 0xFFFFFFFF, headerLine);
            cursorY += HEADER_ROW_HEIGHT;
        }

        int ingredientBoxY = cursorY;
        int ingredientBoxHeight = 18 + visibleIngredientHeight(summary);
        RenderUtils.drawRect(context, textX - 2, ingredientBoxY - 2, panelWidth - PADDING * 2 + 4,
                ingredientBoxHeight, 0x66000000);
        String missingLabel = truncateToWidth(StringUtils.translate("lmlp.label.recipe.ingredients_missing"), lineBudget);
        widget.drawString(context, textX, cursorY, 0xFFAAAAAA, missingLabel);
        cursorY += 18;

        for (IngredientSummary ingredient : summary.ingredients()) {
            renderIngredient(widget, context, textX, cursorY, rightEdge, ingredient, mouseX, mouseY);
            cursorY += INGREDIENT_HEIGHT;

            MaterialTreeNode root = MaterialListPlusState.getVisibleIngredientTree(ingredient);
            if (root != null) {
                int fullHeight = visibleChildrenHeight(root.children());
                int childVisibleHeight = Math.min(fullHeight, Math.round(fullHeight * MaterialListPlusState.treeProgress(root.path())));
                if (childVisibleHeight > 0) {
                    context.enableScissor(textX, cursorY - 3, x + panelWidth - PADDING, cursorY + childVisibleHeight);
                    renderChildren(widget, context, textX, cursorY, root.children(), 1, childVisibleHeight, rightEdge, mouseX, mouseY);
                    context.disableScissor();
                    cursorY += childVisibleHeight;
                }
            }
        }

        context.disableScissor();
        drawOutline(context, x, y, panelWidth, visibleHeight, 0xFF777777);
    }

    public static ToggleTarget toggleAt(List<RecipeSummary> summaries, int x, int y, int width, int mouseX, int mouseY) {
        return toggleAt(summaries, x, y, width, getOuterHeight(summaries, width), mouseX, mouseY);
    }

    public static ToggleTarget toggleAt(List<RecipeSummary> summaries, int x, int y, int width, int visibleOuterHeight, int mouseX, int mouseY) {
        int panelWidth = Math.max(160, width);
        int height = Math.min(getHeight(summaries, width), Math.max(0, visibleOuterHeight));
        if (mouseX < x || mouseX >= x + panelWidth || mouseY < y || mouseY >= y + height || summaries.isEmpty()) {
            return ToggleTarget.NONE;
        }

        RecipeSummary summary = summaries.get(0);
        int textX = x + PADDING;
        int cursorY = y + PADDING + 24 + HEADER_ROW_HEIGHT * headerLines(summary, panelWidth - PADDING * 2).size() + 18;
        for (IngredientSummary ingredient : summary.ingredients()) {
            if (isToggleHit(textX, cursorY, 0, mouseX, mouseY) && MaterialListPlusState.hasTree(ingredient)) {
                return ToggleTarget.ingredient(ingredient);
            }
            cursorY += INGREDIENT_HEIGHT;

            MaterialTreeNode root = MaterialListPlusState.getVisibleIngredientTree(ingredient);
            if (root != null) {
                int fullHeight = visibleChildrenHeight(root.children());
                int visibleHeight = Math.min(fullHeight, Math.round(fullHeight * MaterialListPlusState.treeProgress(root.path())));
                ToggleScan scan = scanChildren(root.children(), textX, cursorY, 1, visibleHeight, mouseX, mouseY);
                if (scan.target() != ToggleTarget.NONE) {
                    return scan.target();
                }
                cursorY = scan.nextY();
            }
        }

        return ToggleTarget.NONE;
    }

    /** Clickable names in the inline summary: the output title opens the detail
     * screen normally, while a top-level ingredient name opens it focused on
     * that ingredient. Toggle arrows remain a separate hit region. */
    public static NavigationTarget navigationTargetAt(List<RecipeSummary> summaries, int x, int y, int width,
            int visibleOuterHeight, int mouseX, int mouseY) {
        int panelWidth = Math.max(160, width);
        int height = Math.min(getHeight(summaries, width), Math.max(0, visibleOuterHeight));
        if (mouseX < x || mouseX >= x + panelWidth || mouseY < y || mouseY >= y + height || summaries.isEmpty()) {
            return NavigationTarget.NONE;
        }

        RecipeSummary summary = summaries.get(0);
        int lineBudget = panelWidth - PADDING * 2;
        int textX = x + PADDING;
        int cursorY = y + PADDING;
        String multipleLabel = summaries.size() > 1
                ? StringUtils.translate("lmlp.label.recipe.multiple")
                : "";
        int multipleWidth = multipleLabel.isEmpty() ? 0 : StringUtils.getStringWidth(multipleLabel) + 4;
        String title = truncateToWidth(ItemStackTexts.name(summary.outputIcon()), lineBudget - 24 - multipleWidth);
        int titleX = textX + 24;
        if (isInside(mouseX, mouseY, titleX, cursorY + 2, StringUtils.getStringWidth(title), 14)) {
            return NavigationTarget.title(summary.recipeId());
        }

        cursorY += 24 + HEADER_ROW_HEIGHT * headerLines(summary, lineBudget).size() + 18;
        int rightEdge = x + panelWidth - PADDING;
        for (IngredientSummary ingredient : summary.ingredients()) {
            int visibleRowHeight = Math.min(INGREDIENT_HEIGHT, y + height - cursorY);
            if (visibleRowHeight <= 0) {
                break;
            }

            String count = CountFormatter.format(ingredient.countMissing(), ingredient.maxStackSize());
            int nameX = textX + INGREDIENT_ICON_OFFSET + 26;
            int nameBudget = rightEdge - nameX - StringUtils.getStringWidth(": " + count);
            String name = truncateToWidth(RecipeSummaryFormatter.ingredientName(ingredient), nameBudget);
            if (isInside(mouseX, mouseY, nameX, cursorY - 1, StringUtils.getStringWidth(name),
                    Math.min(18, visibleRowHeight))) {
                return NavigationTarget.ingredient(summary.recipeId(), ItemStackTexts.id(ingredient.icon()));
            }

            cursorY += INGREDIENT_HEIGHT;
            MaterialTreeNode root = MaterialListPlusState.getVisibleIngredientTree(ingredient);
            if (root != null) {
                cursorY += Math.round(visibleChildrenHeight(root.children())
                        * MaterialListPlusState.treeProgress(root.path()));
            }
        }

        return NavigationTarget.NONE;
    }

    public static ItemStack hoveredStackAt(List<RecipeSummary> summaries, int x, int y, int width, int visibleOuterHeight, int mouseX, int mouseY) {
        int panelWidth = Math.max(160, width);
        int height = Math.min(getHeight(summaries, width), Math.max(0, visibleOuterHeight));
        if (mouseX < x || mouseX >= x + panelWidth || mouseY < y || mouseY >= y + height || summaries.isEmpty()) {
            return ItemStack.EMPTY;
        }

        RecipeSummary summary = summaries.get(0);
        int textX = x + PADDING;
        int cursorY = y + PADDING;
        if (isInside(mouseX, mouseY, textX, cursorY, 16, 16)) {
            return summary.outputIcon();
        }

        cursorY += 24 + HEADER_ROW_HEIGHT * headerLines(summary, panelWidth - PADDING * 2).size() + 18;
        ItemStack stack = hoveredIngredientStackAt(summary.ingredients(), textX, cursorY, 0, Integer.MAX_VALUE, mouseX, mouseY);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    // When the mouse is over a choice-group row's NAME text (not its item
    // icon), return the concrete items the "任意X" tag stands for (icon +
    // name pairs) plus the group's own label, so it can be shown as a rich
    // hover grid. null when not over a choice-group name.
    public static ChoiceGroupHover hoveredChoiceGroup(List<RecipeSummary> summaries, int x, int y, int width, int visibleOuterHeight, int mouseX, int mouseY) {
        int panelWidth = Math.max(160, width);
        int height = Math.min(getHeight(summaries, width), Math.max(0, visibleOuterHeight));
        if (mouseX < x || mouseX >= x + panelWidth || mouseY < y || mouseY >= y + height || summaries.isEmpty()) {
            return null;
        }

        RecipeSummary summary = summaries.get(0);
        int textX = x + PADDING;
        int cursorY = y + PADDING + 24 + HEADER_ROW_HEIGHT * headerLines(summary, panelWidth - PADDING * 2).size() + 18;
        return hoveredIngredientChoiceGroup(summary.ingredients(), textX, cursorY, 0, Integer.MAX_VALUE, mouseX, mouseY);
    }

    private static ChoiceGroupHover hoveredIngredientChoiceGroup(List<IngredientSummary> ingredients, int textX, int y, int depth, int visibleHeight, int mouseX, int mouseY) {
        int cursorY = y;
        int remainingHeight = visibleHeight;
        for (IngredientSummary ingredient : ingredients) {
            if (remainingHeight <= 0) {
                break;
            }

            int visibleRowHeight = Math.min(INGREDIENT_HEIGHT, remainingHeight);
            if (ingredient.isChoiceGroup() && isNameRegionHovered(textX, cursorY, depth, RecipeSummaryFormatter.ingredientName(ingredient), visibleRowHeight, mouseX, mouseY)) {
                return new ChoiceGroupHover(AlternativeItemDisplay.icon(ingredient), RecipeSummaryFormatter.ingredientName(ingredient), ingredient.icons(), ingredient.alternatives());
            }

            cursorY += INGREDIENT_HEIGHT;
            remainingHeight -= visibleRowHeight;
            MaterialTreeNode root = MaterialListPlusState.getVisibleIngredientTree(ingredient);
            if (root != null && remainingHeight > 0) {
                int fullChildrenHeight = visibleChildrenHeight(root.children());
                int childVisibleHeight = Math.min(fullChildrenHeight, Math.round(fullChildrenHeight * MaterialListPlusState.treeProgress(root.path())));
                childVisibleHeight = Math.min(childVisibleHeight, remainingHeight);
                if (childVisibleHeight > 0) {
                    ChoiceGroupHover childHit = hoveredNodeChoiceGroup(root.children(), textX, cursorY, depth + 1, childVisibleHeight, mouseX, mouseY);
                    if (childHit != null) {
                        return childHit;
                    }
                    cursorY += childVisibleHeight;
                    remainingHeight -= childVisibleHeight;
                }
            }
        }

        return null;
    }

    private static ChoiceGroupHover hoveredNodeChoiceGroup(List<MaterialTreeNode> nodes, int textX, int y, int depth, int visibleHeight, int mouseX, int mouseY) {
        int cursorY = y;
        int remainingHeight = visibleHeight;
        for (MaterialTreeNode node : nodes) {
            if (remainingHeight <= 0) {
                break;
            }

            int visibleRowHeight = Math.min(INGREDIENT_HEIGHT, remainingHeight);
            if (node.isChoiceGroup() && isNameRegionHovered(textX, cursorY, depth, node.name(), visibleRowHeight, mouseX, mouseY)) {
                return new ChoiceGroupHover(AlternativeItemDisplay.icon(node), node.name(), node.icons(), node.alternatives());
            }

            cursorY += INGREDIENT_HEIGHT;
            remainingHeight -= visibleRowHeight;
            if (node.hasChildren() && remainingHeight > 0) {
                int fullChildrenHeight = visibleChildrenHeight(node.children());
                int childVisibleHeight = Math.min(fullChildrenHeight, Math.round(fullChildrenHeight * MaterialListPlusState.treeProgress(node.path())));
                childVisibleHeight = Math.min(childVisibleHeight, remainingHeight);
                if (childVisibleHeight > 0) {
                    ChoiceGroupHover childHit = hoveredNodeChoiceGroup(node.children(), textX, cursorY, depth + 1, childVisibleHeight, mouseX, mouseY);
                    if (childHit != null) {
                        return childHit;
                    }
                    cursorY += childVisibleHeight;
                    remainingHeight -= childVisibleHeight;
                }
            }
        }

        return null;
    }

    // A hovered choice-group: the group's icon + label, and the concrete
    // items (icon + name, kept parallel) it stands for.
    public record ChoiceGroupHover(ItemStack icon, String name, List<ItemStack> icons, List<String> alternatives) {
    }

    // Only the yellow name text is hoverable, not the whole row. Stop the hit
    // region before the trailing ": count".
    private static boolean isNameRegionHovered(int textX, int y, int depth, String name, int visibleRowHeight, int mouseX, int mouseY) {
        int rowX = textX + depth * TREE_INDENT_WIDTH;
        int nameStartX = rowX + INGREDIENT_ICON_OFFSET + 26;
        int nameWidth = StringUtils.getStringWidth(name);
        return visibleRowHeight > 0
                && mouseX >= nameStartX
                && mouseX < nameStartX + nameWidth
                && mouseY >= y - 3
                && mouseY < y - 3 + Math.min(18, visibleRowHeight);
    }

    private static void renderIngredient(WidgetBase widget, GuiContext context, int textX, int y, int rightEdge, IngredientSummary ingredient, int mouseX, int mouseY) {
        boolean hasTree = MaterialListPlusState.hasTree(ingredient);
        MaterialTreeNode root = MaterialListPlusState.getVisibleIngredientTree(ingredient);
        float expandProgress = root == null ? 0.0F : MaterialListPlusState.treeProgress(root.path());
        renderRow(widget, context, textX, y, 0, hasTree, expandProgress, AlternativeItemDisplay.icon(ingredient), RecipeSummaryFormatter.ingredientName(ingredient), ingredient.isChoiceGroup(), ingredient.countTotal(), ingredient.countMissing(), ingredient.maxStackSize(), rightEdge, true, mouseX, mouseY);
    }

    private static int renderChildren(WidgetBase widget, GuiContext context, int textX, int y, List<MaterialTreeNode> nodes, int depth, int visibleHeight, int rightEdge, int mouseX, int mouseY) {
        int cursorY = y;
        int remainingHeight = visibleHeight;
        for (MaterialTreeNode node : nodes) {
            if (remainingHeight <= 0) {
                break;
            }

            float expandProgress = node.hasChildren() ? MaterialListPlusState.treeProgress(node.path()) : 0.0F;
            renderRow(widget, context, textX, cursorY, depth, node.hasChildren(), expandProgress, AlternativeItemDisplay.icon(node), node.name(), node.isChoiceGroup(), node.totalCount(), node.missingCount(), node.maxStackSize(), rightEdge, false, mouseX, mouseY);

            int visibleRowHeight = Math.min(INGREDIENT_HEIGHT, remainingHeight);
            cursorY += INGREDIENT_HEIGHT;
            remainingHeight -= visibleRowHeight;
            if (node.hasChildren() && remainingHeight > 0) {
                int fullChildrenHeight = visibleChildrenHeight(node.children());
                int visibleChildrenHeight = Math.min(fullChildrenHeight, Math.round(fullChildrenHeight * MaterialListPlusState.treeProgress(node.path())));
                visibleChildrenHeight = Math.min(visibleChildrenHeight, remainingHeight);
                if (visibleChildrenHeight > 0) {
                    cursorY = renderChildren(widget, context, textX, cursorY, node.children(), depth + 1, visibleChildrenHeight, rightEdge, mouseX, mouseY);
                    remainingHeight -= visibleChildrenHeight;
                }
            }
        }
        return cursorY;
    }

    private static ItemStack hoveredIngredientStackAt(List<IngredientSummary> ingredients, int textX, int y, int depth, int visibleHeight, int mouseX, int mouseY) {
        int cursorY = y;
        int remainingHeight = visibleHeight;
        for (IngredientSummary ingredient : ingredients) {
            if (remainingHeight <= 0) {
                break;
            }

            int visibleRowHeight = Math.min(INGREDIENT_HEIGHT, remainingHeight);
            ItemStack icon = AlternativeItemDisplay.icon(ingredient);
            String name = RecipeSummaryFormatter.ingredientName(ingredient);
            if (isRowStackHovered(textX, cursorY, depth, name, visibleRowHeight, mouseX, mouseY)) {
                return icon;
            }

            cursorY += INGREDIENT_HEIGHT;
            remainingHeight -= visibleRowHeight;
            MaterialTreeNode root = MaterialListPlusState.getVisibleIngredientTree(ingredient);
            if (root != null && remainingHeight > 0) {
                int fullChildrenHeight = visibleChildrenHeight(root.children());
                int childVisibleHeight = Math.min(fullChildrenHeight, Math.round(fullChildrenHeight * MaterialListPlusState.treeProgress(root.path())));
                childVisibleHeight = Math.min(childVisibleHeight, remainingHeight);
                if (childVisibleHeight > 0) {
                    ItemStack childStack = hoveredNodeStackAt(root.children(), textX, cursorY, depth + 1, childVisibleHeight, mouseX, mouseY);
                    if (childStack != null) {
                        return childStack;
                    }
                    cursorY += childVisibleHeight;
                    remainingHeight -= childVisibleHeight;
                }
            }
        }

        return null;
    }

    private static ItemStack hoveredNodeStackAt(List<MaterialTreeNode> nodes, int textX, int y, int depth, int visibleHeight, int mouseX, int mouseY) {
        int cursorY = y;
        int remainingHeight = visibleHeight;
        for (MaterialTreeNode node : nodes) {
            if (remainingHeight <= 0) {
                break;
            }

            int visibleRowHeight = Math.min(INGREDIENT_HEIGHT, remainingHeight);
            ItemStack icon = AlternativeItemDisplay.icon(node);
            if (isRowStackHovered(textX, cursorY, depth, node.name(), visibleRowHeight, mouseX, mouseY)) {
                return icon;
            }

            cursorY += INGREDIENT_HEIGHT;
            remainingHeight -= visibleRowHeight;
            if (node.hasChildren() && remainingHeight > 0) {
                int fullChildrenHeight = visibleChildrenHeight(node.children());
                int childVisibleHeight = Math.min(fullChildrenHeight, Math.round(fullChildrenHeight * MaterialListPlusState.treeProgress(node.path())));
                childVisibleHeight = Math.min(childVisibleHeight, remainingHeight);
                if (childVisibleHeight > 0) {
                    ItemStack childStack = hoveredNodeStackAt(node.children(), textX, cursorY, depth + 1, childVisibleHeight, mouseX, mouseY);
                    if (childStack != null) {
                        return childStack;
                    }
                    cursorY += childVisibleHeight;
                    remainingHeight -= childVisibleHeight;
                }
            }
        }

        return null;
    }

    private static boolean isRowStackHovered(int textX, int y, int depth, String name, int visibleRowHeight, int mouseX, int mouseY) {
        int rowX = textX + depth * TREE_INDENT_WIDTH;
        int iconX = rowX + INGREDIENT_ICON_OFFSET;
        int rowRight = iconX + 18;
        return visibleRowHeight > 0
                && mouseX >= iconX
                && mouseX < rowRight
                && mouseY >= y - 3
                && mouseY < y - 3 + Math.min(18, visibleRowHeight);
    }

    private static void renderRow(WidgetBase widget, GuiContext context, int textX, int y, int depth, boolean hasTree, float expandProgress, net.minecraft.world.item.ItemStack icon, String name, boolean choiceGroup, int totalCount, int missingCount, int maxStackSize, int rightEdge, boolean clickableName, int mouseX, int mouseY) {
        int rowX = textX + depth * TREE_INDENT_WIDTH;
        int iconX = rowX + INGREDIENT_ICON_OFFSET;
        int iconY = y - 2;
        if (hasTree) {
            boolean hovered = isToggleHit(textX, y, depth, mouseX, mouseY);
            ToggleArrowRenderer.render(context, rowX, INGREDIENT_TOGGLE_WIDTH, iconY + 8, expandProgress, hovered);
        }

        RenderUtils.drawRect(context, iconX, y - 3, 18, 18, 0x30FFFFFF);
        context.renderItem(icon, iconX + 1, iconY);

        String countColor = missingCount == 0 ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        String count = CountFormatter.format(missingCount, maxStackSize);
        int textStartX = rowX + INGREDIENT_ICON_OFFSET + 26;
        // Never truncate the count itself (the number matters more than the
        // name), only shrink the plain name in front of it.
        int nameBudget = rightEdge - textStartX - StringUtils.getStringWidth(": " + count);
        String shownName = truncateToWidth(name, nameBudget);
        boolean nameHovered = clickableName && isInside(mouseX, mouseY, textStartX, y - 1,
                StringUtils.getStringWidth(shownName), 18);
        // Choice-group ("任意X" / tag) names are yellow so
        // they read as "any of a category" rather than a specific item. Wrap
        // AFTER truncating the plain text so width math and the "..." are
        // computed on the raw name, not the format codes.
        if (choiceGroup || nameHovered) {
            shownName = (choiceGroup ? GuiBase.TXT_YELLOW : "")
                    + (nameHovered ? GuiBase.TXT_BOLD + GuiBase.TXT_UNDERLINE : "")
                    + shownName
                    + GuiBase.TXT_RST;
        }
        String line = shownName + ": " + countColor + count;
        widget.drawString(context, textStartX, y + 2, 0xFFFFFFFF, line);
    }

    private static int visibleIngredientHeight(RecipeSummary summary) {
        int height = 0;
        for (IngredientSummary ingredient : summary.ingredients()) {
            height += INGREDIENT_HEIGHT;
            MaterialTreeNode root = MaterialListPlusState.getVisibleIngredientTree(ingredient);
            if (root != null) {
                int fullHeight = visibleChildrenHeight(root.children());
                height += Math.round(fullHeight * MaterialListPlusState.treeProgress(root.path()));
            }
        }
        return height;
    }

    private static int getTargetHeight(List<RecipeSummary> summaries, int width) {
        if (summaries.isEmpty()) {
            return 48;
        }

        RecipeSummary summary = summaries.get(0);
        int lineBudget = Math.max(160, width) - PADDING * 2;
        int height = 64 + headerExtraHeight(summary, lineBudget) + targetIngredientHeight(summary) + INNER_BOTTOM_PADDING;
        return height;
    }

    private static int targetIngredientHeight(RecipeSummary summary) {
        int height = 0;
        for (IngredientSummary ingredient : summary.ingredients()) {
            height += INGREDIENT_HEIGHT;
            MaterialTreeNode root = MaterialListPlusState.getVisibleIngredientTree(ingredient);
            if (root != null && MaterialListPlusState.getExpandedTreeNodes().contains(root.path())) {
                height += targetChildrenHeight(root.children());
            }
        }
        return height;
    }

    private static int targetChildrenHeight(List<MaterialTreeNode> nodes) {
        int height = 0;
        for (MaterialTreeNode node : nodes) {
            height += INGREDIENT_HEIGHT;
            if (node.hasChildren() && MaterialListPlusState.getExpandedTreeNodes().contains(node.path())) {
                height += targetChildrenHeight(node.children());
            }
        }
        return height;
    }

    private static int visibleChildrenHeight(List<MaterialTreeNode> nodes) {
        int height = 0;
        for (MaterialTreeNode node : nodes) {
            height += INGREDIENT_HEIGHT;
            if (node.hasChildren()) {
                height += Math.round(visibleChildrenHeight(node.children()) * MaterialListPlusState.treeProgress(node.path()));
            }
        }
        return height;
    }

    private static ToggleScan scanChildren(List<MaterialTreeNode> nodes, int textX, int y, int depth, int visibleHeight, int mouseX, int mouseY) {
        int cursorY = y;
        int remainingHeight = visibleHeight;
        for (MaterialTreeNode node : nodes) {
            if (remainingHeight <= 0) {
                break;
            }

            int visibleRowHeight = Math.min(INGREDIENT_HEIGHT, remainingHeight);
            if (node.hasChildren() && mouseY < cursorY + visibleRowHeight && isToggleHit(textX, cursorY, depth, mouseX, mouseY)) {
                return new ToggleScan(ToggleTarget.node(node.path()), cursorY + INGREDIENT_HEIGHT);
            }

            cursorY += INGREDIENT_HEIGHT;
            remainingHeight -= visibleRowHeight;
            if (node.hasChildren() && remainingHeight > 0) {
                int fullChildrenHeight = visibleChildrenHeight(node.children());
                int visibleChildrenHeight = Math.min(fullChildrenHeight, Math.round(fullChildrenHeight * MaterialListPlusState.treeProgress(node.path())));
                visibleChildrenHeight = Math.min(visibleChildrenHeight, remainingHeight);
                ToggleScan scan = scanChildren(node.children(), textX, cursorY, depth + 1, visibleChildrenHeight, mouseX, mouseY);
                if (scan.target() != ToggleTarget.NONE) {
                    return scan;
                }
                cursorY = scan.nextY();
                remainingHeight -= visibleChildrenHeight;
            }
        }

        return new ToggleScan(ToggleTarget.NONE, cursorY);
    }

    private static boolean isToggleHit(int textX, int y, int depth, int mouseX, int mouseY) {
        int toggleX = textX + depth * TREE_INDENT_WIDTH;
        return mouseX >= toggleX && mouseX < toggleX + INGREDIENT_TOGGLE_WIDTH && mouseY >= y - 2 && mouseY < y + INGREDIENT_HEIGHT - 2;
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    // The " / missing" suffix in RecipeSummaryFormatter.header() carries
    // color codes as one unit, so it's never safe to cut mid-string — wrap
    // it onto its own line instead of truncating it away. Only the plain
    // base text (no color codes) ever gets character-truncated, and only if
    // it alone still doesn't fit on one line.
    private static List<String> headerLines(RecipeSummary summary, int lineBudget) {
        String full = RecipeSummaryFormatter.header(summary, 1);
        if (StringUtils.getStringWidth(full) <= lineBudget) {
            return List.of(full);
        }

        String base = RecipeSummaryFormatter.headerBase(summary, 1);
        if (StringUtils.getStringWidth(base) <= lineBudget) {
            return List.of(base, RecipeSummaryFormatter.headerMissing(summary));
        }

        return List.of(truncateToWidth(base, lineBudget));
    }

    private static int headerExtraHeight(RecipeSummary summary, int lineBudget) {
        return (headerLines(summary, lineBudget).size() - 1) * HEADER_ROW_HEIGHT;
    }

    private static String truncateToWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }

        if (StringUtils.getStringWidth(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int suffixWidth = StringUtils.getStringWidth(suffix);
        if (suffixWidth > maxWidth) {
            return "";
        }

        int end = text.length();
        while (end > 0 && StringUtils.getStringWidth(text.substring(0, end)) + suffixWidth > maxWidth) {
            end--;
        }

        return end > 0 ? text.substring(0, end) + suffix : suffix;
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

    public record ToggleTarget(IngredientSummary ingredient, String nodePath) {
        public static final ToggleTarget NONE = new ToggleTarget(null, null);

        public static ToggleTarget ingredient(IngredientSummary ingredient) {
            return new ToggleTarget(ingredient, null);
        }

        public static ToggleTarget node(String nodePath) {
            return new ToggleTarget(null, nodePath);
        }

        public boolean isNone() {
            return this.ingredient == null && this.nodePath == null;
        }
    }

    public record NavigationTarget(boolean title, String recipeId, String itemId) {
        public static final NavigationTarget NONE = new NavigationTarget(false, "", "");

        public static NavigationTarget title(String recipeId) {
            return new NavigationTarget(true, recipeId, "");
        }

        public static NavigationTarget ingredient(String recipeId, String itemId) {
            return new NavigationTarget(false, recipeId, itemId);
        }

        public boolean isNone() {
            return this.recipeId.isEmpty();
        }
    }

    private record ToggleScan(ToggleTarget target, int nextY) {
    }
}
