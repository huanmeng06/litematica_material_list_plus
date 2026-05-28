package io.github.huanmeng06.lmlp.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.recipe.MaterialTreeNode;
import net.minecraft.class_332;

public final class NestedMaterialTreeRenderer {
    private static final int PADDING = 8;
    private static final int HEADER_HEIGHT = 16;
    private static final int ROW_HEIGHT = 22;
    private static final int ENTRY_BOTTOM_GAP = 4;
    private static final int INDENT_WIDTH = 14;
    private static final int TOGGLE_WIDTH = 10;
    private static final int ICON_SIZE = 16;
    private static final int MIN_WIDTH = 240;

    private NestedMaterialTreeRenderer() {
    }

    public static int getHeight(MaterialTreeNode root, Set<String> expandedNodes) {
        int rowCount = visibleNodes(root, expandedNodes).size();
        return PADDING * 2 + HEADER_HEIGHT + Math.max(1, rowCount) * ROW_HEIGHT;
    }

    public static int getOuterHeight(MaterialTreeNode root, Set<String> expandedNodes) {
        return getHeight(root, expandedNodes) + ENTRY_BOTTOM_GAP;
    }

    public static void render(WidgetBase widget, class_332 context, int x, int y, int width, MaterialTreeNode root, Set<String> expandedNodes) {
        int panelWidth = Math.max(MIN_WIDTH, width);
        int height = getHeight(root, expandedNodes);
        RenderUtils.drawOutlinedBox(x, y, panelWidth, height, 0xDD000000, 0xFF777777);

        Columns columns = columns(x, panelWidth);
        int headerY = y + PADDING;
        widget.drawString(columns.totalX(), headerY, 0xFFAAAAAA, StringUtils.translate("lmlp.label.recipe.total_short"), context);
        widget.drawString(columns.missingX(), headerY, 0xFFAAAAAA, StringUtils.translate("lmlp.label.recipe.missing_short"), context);

        List<MaterialTreeNode> nodes = visibleNodes(root, expandedNodes);
        int rowY = y + PADDING + HEADER_HEIGHT;
        if (nodes.isEmpty()) {
            widget.drawString(x + PADDING, rowY + 5, 0xFFFFCC66, StringUtils.translate("lmlp.label.recipe.none"), context);
            return;
        }

        for (MaterialTreeNode node : nodes) {
            renderNode(widget, context, node, expandedNodes, x + PADDING, rowY, columns);
            rowY += ROW_HEIGHT;
        }
    }

    public static String nodeToggleAt(MaterialTreeNode root, Set<String> expandedNodes, int x, int y, int width, int mouseX, int mouseY) {
        int panelWidth = Math.max(MIN_WIDTH, width);
        int height = getHeight(root, expandedNodes);
        if (mouseX < x || mouseX >= x + panelWidth || mouseY < y || mouseY >= y + height) {
            return null;
        }

        int rowY = y + PADDING + HEADER_HEIGHT;
        for (MaterialTreeNode node : visibleNodes(root, expandedNodes)) {
            if (node.hasChildren()) {
                int toggleX = x + PADDING + node.depth() * INDENT_WIDTH;
                if (mouseX >= toggleX && mouseX < toggleX + TOGGLE_WIDTH && mouseY >= rowY + 6 && mouseY < rowY + 16) {
                    return node.path();
                }
            }
            rowY += ROW_HEIGHT;
        }

        return null;
    }

    private static void renderNode(WidgetBase widget, class_332 context, MaterialTreeNode node, Set<String> expandedNodes, int left, int y, Columns columns) {
        int rowX = left + node.depth() * INDENT_WIDTH;
        if (node.hasChildren()) {
            widget.drawString(rowX + 2, y + 7, 0xFFFFFFFF, expandedNodes.contains(node.path()) ? "v" : ">", context);
        }

        int iconX = rowX + TOGGLE_WIDTH + 2;
        RenderUtils.drawRect(iconX - 1, y + 2, ICON_SIZE + 2, ICON_SIZE + 2, 0x20FFFFFF);
        context.method_51427(node.icon(), iconX, y + 3);

        int nameX = iconX + ICON_SIZE + 6;
        int nameWidth = Math.max(12, columns.totalX() - nameX - 8);
        widget.drawString(nameX, y + 7, 0xFFFFFFFF, fit(node.name(), nameWidth), context);
        widget.drawString(columns.totalX(), y + 7, 0xFFFFFFFF, CountFormatter.format(node.totalCount(), node.maxStackSize()), context);
        widget.drawString(columns.missingX(), y + 7, missingColor(node.missingCount()), CountFormatter.format(node.missingCount(), node.maxStackSize()), context);
    }

    private static List<MaterialTreeNode> visibleNodes(MaterialTreeNode root, Set<String> expandedNodes) {
        List<MaterialTreeNode> nodes = new ArrayList<>();
        if (root == null) {
            return nodes;
        }

        collectVisible(root, expandedNodes, nodes);
        return nodes;
    }

    private static void collectVisible(MaterialTreeNode node, Set<String> expandedNodes, List<MaterialTreeNode> nodes) {
        nodes.add(node);
        if (!node.hasChildren() || !expandedNodes.contains(node.path())) {
            return;
        }

        for (MaterialTreeNode child : node.children()) {
            collectVisible(child, expandedNodes, nodes);
        }
    }

    private static String fit(String text, int maxWidth) {
        if (StringUtils.getStringWidth(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int suffixWidth = StringUtils.getStringWidth(suffix);
        StringBuilder builder = new StringBuilder(text);
        while (builder.length() > 0 && StringUtils.getStringWidth(builder.toString()) + suffixWidth > maxWidth) {
            builder.setLength(builder.length() - 1);
        }

        return builder + suffix;
    }

    private static int missingColor(int missingCount) {
        return missingCount <= 0 ? 0xFF55FF55 : 0xFFFF5555;
    }

    private static Columns columns(int x, int width) {
        int countColumnWidth = Math.max(72, Math.min(130, width / 4));
        int missingX = x + width - PADDING - countColumnWidth;
        int totalX = missingX - countColumnWidth - 8;
        return new Columns(totalX, missingX);
    }

    private record Columns(int totalX, int missingX) {
    }
}
