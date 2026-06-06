package io.github.huanmeng06.lmlp.gui;

import java.util.List;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import net.minecraft.class_1799;
import net.minecraft.class_332;

public final class MinimalSourceInlineRenderer {
    private static final int ROW_HEIGHT = 22;
    private static final int PADDING = 8;
    private static final int INNER_BOTTOM_PADDING = 4;
    private static final int ENTRY_BOTTOM_GAP = 4;
    private static final int SOURCE_ICON_BOX_SIZE = 18;
    private static final int SOURCE_ICON_BOX_Y_OFFSET = -3;
    private static final int COLUMN_GAP = 28;
    private static final int TWO_COLUMN_THRESHOLD = 5;
    private static final int THREE_COLUMN_THRESHOLD = 9;
    private static final int COLUMN_SEPARATOR_COLOR = 0xFF999999;

    private MinimalSourceInlineRenderer() {
    }

    public static int getHeight(class_1799 targetIcon, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll) {
        if (sources.isEmpty()) {
            return 48;
        }
        if (isSelfSource(targetIcon, sources)) {
            return 56;
        }

        return 50 + sourceRowCount(sources) * ROW_HEIGHT + INNER_BOTTOM_PADDING;
    }

    public static int getOuterHeight(class_1799 targetIcon, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll) {
        return getHeight(targetIcon, sources, showAll) + ENTRY_BOTTOM_GAP;
    }

    public static int getOuterHeight(class_1799 targetIcon, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, float progress) {
        return Math.round(getOuterHeight(targetIcon, sources, showAll) * progress);
    }

    public static void render(WidgetBase widget, class_332 context, int x, int y, int width, class_1799 targetIcon, String targetName, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, int visibleOuterHeight) {
        int height = getHeight(targetIcon, sources, showAll);
        int panelWidth = Math.max(160, width);
        int visibleHeight = Math.max(0, Math.min(height, visibleOuterHeight));
        if (visibleHeight <= 0) {
            return;
        }

        RenderUtils.drawRect(x, y, panelWidth, visibleHeight, 0xDD000000);
        int textX = x + PADDING;
        if (visibleHeight <= 2) {
            drawOutline(x, y, panelWidth, visibleHeight, 0xFF777777);
            return;
        }

        context.method_44379(x + 1, y + 1, x + panelWidth - 1, y + visibleHeight - 1);

        if (sources.isEmpty()) {
            widget.drawString(textX, y + 16, 0xFFFFCC66, StringUtils.translate("lmlp.label.minimal_sources.none"), context);
            context.method_44380();
            drawOutline(x, y, panelWidth, visibleHeight, 0xFF777777);
            return;
        }

        int cursorY = y + PADDING;
        context.method_51427(targetIcon, textX, cursorY);
        widget.drawString(textX + 24, cursorY + 4, 0xFFFFFFFF, GuiBase.TXT_BOLD + targetName, context);
        cursorY += 24;

        String boldTargetName = GuiBase.TXT_BOLD + targetName + GuiBase.TXT_RST;
        if (isSelfSource(targetIcon, sources)) {
            widget.drawString(textX, cursorY + 2, 0xFFFFFFFF, StringUtils.translate("lmlp.label.minimal_sources.self_material", boldTargetName), context);
            context.method_44380();
            drawOutline(x, y, panelWidth, visibleHeight, 0xFF777777);
            return;
        }

        widget.drawString(textX, cursorY, 0xFFFFFFFF, StringUtils.translate("lmlp.label.minimal_sources.header_named", boldTargetName), context);
        cursorY += 18;

        int boxY = cursorY;
        int visibleCount = visibleSourceCount(sources);
        int columns = sourceColumnCount(visibleCount);
        int rowCount = sourceRowCount(visibleCount, columns);
        int boxHeight = rowCount * ROW_HEIGHT;
        RenderUtils.drawRect(textX - 2, boxY - 2, panelWidth - PADDING * 2 + 4, boxHeight, 0x66000000);

        int columnGap = columns > 1 ? COLUMN_GAP : 0;
        int contentWidth = Math.max(1, panelWidth - PADDING * 2);
        int columnWidth = Math.max(1, (contentWidth - columnGap * (columns - 1)) / columns);
        if (columns > 1) {
            drawColumnSeparators(textX, boxY, rowCount, columns, columnWidth, columnGap);
        }

        for (int index = 0; index < visibleCount; index++) {
            MinimalSubMaterialListView.SourceContribution source = sources.get(index);
            int row = index / columns;
            int column = index % columns;
            int rowX = textX + column * (columnWidth + columnGap);
            int rowY = cursorY + row * ROW_HEIGHT;
            renderSourceRow(widget, context, rowX, rowY, source);
        }

        context.method_44380();
        drawOutline(x, y, panelWidth, visibleHeight, 0xFF777777);
    }

    private static int visibleSourceCount(List<MinimalSubMaterialListView.SourceContribution> sources) {
        return sources.size();
    }

    private static int sourceColumnCount(int visibleCount) {
        if (visibleCount >= THREE_COLUMN_THRESHOLD) {
            return 3;
        }
        if (visibleCount >= TWO_COLUMN_THRESHOLD) {
            return 2;
        }
        return 1;
    }

    private static int sourceRowCount(List<MinimalSubMaterialListView.SourceContribution> sources) {
        int visibleCount = visibleSourceCount(sources);
        return sourceRowCount(visibleCount, sourceColumnCount(visibleCount));
    }

    private static int sourceRowCount(int visibleCount, int columns) {
        return (visibleCount + columns - 1) / columns;
    }

    private static boolean isSelfSource(class_1799 targetIcon, List<MinimalSubMaterialListView.SourceContribution> sources) {
        return sources.size() == 1 && ItemStackTexts.id(targetIcon).equals(ItemStackTexts.id(sources.get(0).icon()));
    }

    private static void renderSourceRow(WidgetBase widget, class_332 context, int textX, int y, MinimalSubMaterialListView.SourceContribution source) {
        RenderUtils.drawRect(textX, y + SOURCE_ICON_BOX_Y_OFFSET, SOURCE_ICON_BOX_SIZE, SOURCE_ICON_BOX_SIZE, 0x30FFFFFF);
        context.method_51427(source.icon(), textX + 1, y + SOURCE_ICON_BOX_Y_OFFSET + 1);

        String line = source.name() + ": " + GuiBase.TXT_GOLD + CountFormatter.format(source.totalCount(), source.maxStackSize());
        if (source.missingCount() != source.totalCount()) {
            line += GuiBase.TXT_RST + " / " + GuiBase.TXT_RED + CountFormatter.format(source.missingCount(), source.maxStackSize());
        }
        widget.drawString(textX + 26, y + 2, 0xFFFFFFFF, line, context);
    }

    private static void drawColumnSeparators(int contentX, int boxY, int rowCount, int columns, int columnWidth, int columnGap) {
        int separatorY = boxY + SOURCE_ICON_BOX_Y_OFFSET;
        int separatorHeight = Math.max(0, (rowCount - 1) * ROW_HEIGHT + SOURCE_ICON_BOX_SIZE);
        for (int separator = 1; separator < columns; separator++) {
            int x = contentX + separator * columnWidth + (separator - 1) * columnGap + columnGap / 2;
            RenderUtils.drawRect(x, separatorY, 1, separatorHeight, COLUMN_SEPARATOR_COLOR);
        }
    }

    private static void drawOutline(int x, int y, int width, int height, int color) {
        if (height <= 0) {
            return;
        }

        RenderUtils.drawRect(x, y, width, 1, color);
        RenderUtils.drawRect(x, y + height - 1, width, 1, color);
        RenderUtils.drawRect(x, y, 1, height, color);
        RenderUtils.drawRect(x + width - 1, y, 1, height, color);
    }
}
