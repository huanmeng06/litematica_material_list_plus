package io.github.huanmeng06.lmlp.gui;

import java.util.List;
import java.util.Set;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.MaterialTreeNode;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import net.minecraft.class_332;

public final class RecipeInlineRenderer {
    private static final int INGREDIENT_HEIGHT = 22;
    private static final int PADDING = 8;
    private static final int INNER_BOTTOM_PADDING = 4;
    private static final int ENTRY_BOTTOM_GAP = 4;
    private static final int INGREDIENT_TOGGLE_WIDTH = 10;
    private static final int INGREDIENT_ICON_OFFSET = 14;
    private static final int TREE_INDENT_WIDTH = 18;

    private RecipeInlineRenderer() {
    }

    public static int getHeight(List<RecipeSummary> summaries) {
        if (summaries.isEmpty()) {
            return 48;
        }

        RecipeSummary summary = summaries.get(0);
        int visibleRows = 0;
        for (IngredientSummary ingredient : summary.ingredients()) {
            visibleRows++;
            MaterialTreeNode root = MaterialListPlusState.getExpandedIngredientTree(ingredient);
            if (root != null) {
                visibleRows += visibleChildCount(root, MaterialListPlusState.getExpandedTreeNodes());
            }
        }

        int height = 64 + visibleRows * INGREDIENT_HEIGHT + INNER_BOTTOM_PADDING;
        if (summaries.size() > 1) {
            height += 22;
        }
        return height;
    }

    public static int getOuterHeight(List<RecipeSummary> summaries) {
        return getHeight(summaries) + ENTRY_BOTTOM_GAP;
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
        String itemName = ItemStackTexts.name(summary.outputIcon());
        context.method_51427(summary.outputIcon(), textX, cursorY);
        widget.drawString(textX + 24, cursorY + 4, 0xFFFFFFFF, GuiBase.TXT_BOLD + itemName, context);
        cursorY += 24;

        widget.drawString(textX, cursorY, 0xFFFFFFFF, RecipeSummaryFormatter.header(summary, 1), context);
        cursorY += 18;

        int ingredientBoxY = cursorY;
        int ingredientBoxHeight = 18 + visibleIngredientRows(summary) * INGREDIENT_HEIGHT;
        RenderUtils.drawRect(textX - 2, ingredientBoxY - 2, panelWidth - PADDING * 2 + 4, ingredientBoxHeight, 0x66000000);
        widget.drawString(textX, cursorY, 0xFFAAAAAA, StringUtils.translate("lmlp.label.recipe.ingredients_total"), context);
        cursorY += 18;

        for (IngredientSummary ingredient : summary.ingredients()) {
            renderIngredient(widget, context, textX, cursorY, ingredient);
            cursorY += INGREDIENT_HEIGHT;

            MaterialTreeNode root = MaterialListPlusState.getExpandedIngredientTree(ingredient);
            if (root != null) {
                cursorY = renderChildren(widget, context, textX, cursorY, root.children(), MaterialListPlusState.getExpandedTreeNodes(), 1);
            }
        }

        if (summaries.size() > 1) {
            widget.drawString(textX, y + height - 16, 0xFFFFFFFF, GuiBase.TXT_GOLD + StringUtils.translate("lmlp.label.recipe.more_hint"), context);
        }
    }

    public static ToggleTarget toggleAt(List<RecipeSummary> summaries, int x, int y, int width, int mouseX, int mouseY) {
        int panelWidth = Math.max(160, width);
        int height = getHeight(summaries);
        if (mouseX < x || mouseX >= x + panelWidth || mouseY < y || mouseY >= y + height || summaries.isEmpty()) {
            return ToggleTarget.NONE;
        }

        RecipeSummary summary = summaries.get(0);
        int textX = x + PADDING;
        int cursorY = y + PADDING + 24 + 18 + 18;
        for (IngredientSummary ingredient : summary.ingredients()) {
            if (isToggleHit(textX, cursorY, 0, mouseX, mouseY) && MaterialListPlusState.hasTree(ingredient)) {
                return ToggleTarget.ingredient(ingredient);
            }
            cursorY += INGREDIENT_HEIGHT;

            MaterialTreeNode root = MaterialListPlusState.getExpandedIngredientTree(ingredient);
            if (root != null) {
                ToggleScan scan = scanChildren(root.children(), MaterialListPlusState.getExpandedTreeNodes(), textX, cursorY, 1, mouseX, mouseY);
                if (scan.target() != ToggleTarget.NONE) {
                    return scan.target();
                }
                cursorY = scan.nextY();
            }
        }

        return ToggleTarget.NONE;
    }

    private static void renderIngredient(WidgetBase widget, class_332 context, int textX, int y, IngredientSummary ingredient) {
        boolean hasTree = MaterialListPlusState.hasTree(ingredient);
        boolean expanded = MaterialListPlusState.getExpandedIngredientTree(ingredient) != null;
        renderRow(widget, context, textX, y, 0, hasTree, expanded, ingredient.icon(), RecipeSummaryFormatter.ingredientName(ingredient), ingredient.countTotal(), ingredient.countMissing(), ingredient.maxStackSize());
    }

    private static int renderChildren(WidgetBase widget, class_332 context, int textX, int y, List<MaterialTreeNode> nodes, Set<String> expandedNodes, int depth) {
        int cursorY = y;
        for (MaterialTreeNode node : nodes) {
            boolean expanded = expandedNodes.contains(node.path());
            renderRow(widget, context, textX, cursorY, depth, node.hasChildren(), expanded, node.icon(), node.name(), node.totalCount(), node.missingCount(), node.maxStackSize());
            cursorY += INGREDIENT_HEIGHT;
            if (node.hasChildren() && expanded) {
                cursorY = renderChildren(widget, context, textX, cursorY, node.children(), expandedNodes, depth + 1);
            }
        }
        return cursorY;
    }

    private static void renderRow(WidgetBase widget, class_332 context, int textX, int y, int depth, boolean hasTree, boolean expanded, net.minecraft.class_1799 icon, String name, int totalCount, int missingCount, int maxStackSize) {
        int rowX = textX + depth * TREE_INDENT_WIDTH;
        if (hasTree) {
            widget.drawString(rowX + 2, y + 2, 0xFFFFFFFF, expanded ? "v" : ">", context);
        }

        int iconX = rowX + INGREDIENT_ICON_OFFSET;
        RenderUtils.drawRect(iconX, y - 3, 18, 18, 0x30FFFFFF);
        context.method_51427(icon, iconX + 1, y - 2);

        String line = name + ": " + GuiBase.TXT_GOLD + CountFormatter.format(totalCount, maxStackSize);
        if (missingCount != totalCount) {
            line += GuiBase.TXT_RST + " / " + GuiBase.TXT_RED + CountFormatter.format(missingCount, maxStackSize);
        }
        widget.drawString(rowX + INGREDIENT_ICON_OFFSET + 26, y + 2, 0xFFFFFFFF, line, context);
    }

    private static int visibleIngredientRows(RecipeSummary summary) {
        int rows = 0;
        for (IngredientSummary ingredient : summary.ingredients()) {
            rows++;
            MaterialTreeNode root = MaterialListPlusState.getExpandedIngredientTree(ingredient);
            if (root != null) {
                rows += visibleChildCount(root, MaterialListPlusState.getExpandedTreeNodes());
            }
        }
        return rows;
    }

    private static int visibleChildCount(MaterialTreeNode root, Set<String> expandedNodes) {
        if (!expandedNodes.contains(root.path())) {
            return 0;
        }

        return visibleNodeCount(root.children(), expandedNodes);
    }

    private static int visibleNodeCount(List<MaterialTreeNode> nodes, Set<String> expandedNodes) {
        int count = 0;
        for (MaterialTreeNode node : nodes) {
            count++;
            if (node.hasChildren() && expandedNodes.contains(node.path())) {
                count += visibleNodeCount(node.children(), expandedNodes);
            }
        }
        return count;
    }

    private static ToggleScan scanChildren(List<MaterialTreeNode> nodes, Set<String> expandedNodes, int textX, int y, int depth, int mouseX, int mouseY) {
        int cursorY = y;
        for (MaterialTreeNode node : nodes) {
            if (node.hasChildren() && isToggleHit(textX, cursorY, depth, mouseX, mouseY)) {
                return new ToggleScan(ToggleTarget.node(node.path()), cursorY + INGREDIENT_HEIGHT);
            }

            cursorY += INGREDIENT_HEIGHT;
            if (node.hasChildren() && expandedNodes.contains(node.path())) {
                ToggleScan scan = scanChildren(node.children(), expandedNodes, textX, cursorY, depth + 1, mouseX, mouseY);
                if (scan.target() != ToggleTarget.NONE) {
                    return scan;
                }
                cursorY = scan.nextY();
            }
        }

        return new ToggleScan(ToggleTarget.NONE, cursorY);
    }

    private static boolean isToggleHit(int textX, int y, int depth, int mouseX, int mouseY) {
        int toggleX = textX + depth * TREE_INDENT_WIDTH;
        return mouseX >= toggleX && mouseX < toggleX + INGREDIENT_TOGGLE_WIDTH && mouseY >= y - 2 && mouseY < y + INGREDIENT_HEIGHT - 2;
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

    private record ToggleScan(ToggleTarget target, int nextY) {
    }
}
