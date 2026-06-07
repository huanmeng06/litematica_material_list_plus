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
    private static final String UNDERLINE = "\u00A7n";
    private static final int ROW_HEIGHT = 22;
    private static final int PADDING = 8;
    private static final int INNER_BOTTOM_PADDING = 4;
    private static final int ENTRY_BOTTOM_GAP = 4;
    private static final int SOURCE_ICON_BOX_SIZE = 18;
    private static final int SOURCE_ICON_BOX_Y_OFFSET = -3;
    private static final int SECTION_GAP = 6;
    private static final int TEXT_HOVER_HEIGHT = 12;
    private static final int UPSTREAM_GAP = 18;
    private static final int UPSTREAM_ARROW_WIDTH = 18;
    private static final int COLUMN_GAP = 28;
    private static final int TWO_COLUMN_THRESHOLD = 4;
    private static final int THREE_COLUMN_THRESHOLD = 7;
    private static final int COLUMN_SEPARATOR_COLOR = 0xFF999999;
    private static final long ICON_CYCLE_MILLIS = 900L;

    private MinimalSourceInlineRenderer() {
    }

    public static int getHeight(class_1799 targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll) {
        if (sources.isEmpty()) {
            return 48;
        }
        if (isSelfSource(targetIcon, sources)) {
            return 56;
        }

        return 50 + requirementHeight(requirements) + sourceRowCount(sources) * ROW_HEIGHT + INNER_BOTTOM_PADDING;
    }

    public static int getOuterHeight(class_1799 targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll) {
        return getHeight(targetIcon, requirements, sources, showAll) + ENTRY_BOTTOM_GAP;
    }

    public static int getOuterHeight(class_1799 targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, float progress) {
        return Math.round(getOuterHeight(targetIcon, requirements, sources, showAll) * progress);
    }

    public static boolean isTargetNameHovered(int x, int y, class_1799 targetIcon, int targetNameWidth, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, int visibleOuterHeight, int mouseX, int mouseY) {
        int visibleHeight = Math.max(0, visibleOuterHeight - ENTRY_BOTTOM_GAP);
        int textX = x + PADDING + 24;
        int textY = y + PADDING + 4;
        if (isVisibleTextHovered(textX, textY, targetNameWidth, y, visibleHeight, mouseX, mouseY)) {
            return true;
        }

        if (sources.isEmpty()) {
            return false;
        }

        int cursorY = y + PADDING + 24;
        textX = x + PADDING;
        if (isSelfSource(targetIcon, sources)) {
            return isVisibleTextHovered(textX, cursorY + 2, targetNameWidth, y, visibleHeight, mouseX, mouseY);
        }

        if (!requirements.isEmpty()) {
            if (isVisibleTextHovered(textX, cursorY, targetNameWidth, y, visibleHeight, mouseX, mouseY)) {
                return true;
            }
            cursorY += 18 + requirements.size() * ROW_HEIGHT + SECTION_GAP;
        }

        return isVisibleTextHovered(textX, cursorY, targetNameWidth, y, visibleHeight, mouseX, mouseY);
    }

    public static MinimalSubMaterialListView.SourceContribution sourceAt(int x, int y, int width, class_1799 targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, int visibleOuterHeight, int mouseX, int mouseY) {
        if (sources.isEmpty() || isSelfSource(targetIcon, sources)) {
            return null;
        }

        int panelWidth = Math.max(160, width);
        int visibleHeight = Math.max(0, Math.min(getHeight(targetIcon, requirements, sources, showAll), visibleOuterHeight));
        int cursorY = y + PADDING + 24;
        if (!requirements.isEmpty()) {
            cursorY += 18 + requirements.size() * ROW_HEIGHT + SECTION_GAP;
        }
        cursorY += 18;

        int visibleCount = visibleSourceCount(sources);
        int columns = sourceColumnCount(visibleCount);
        int rowCount = sourceRowCount(visibleCount, columns);
        int contentWidth = Math.max(1, panelWidth - PADDING * 2);
        int columnStride = Math.max(1, contentWidth / columns);
        int contentX = x + PADDING;

        for (int index = 0; index < visibleCount; index++) {
            MinimalSubMaterialListView.SourceContribution source = sources.get(index);
            int column = index / rowCount;
            int row = index % rowCount;
            int rowX = contentX + column * columnStride + (column == 0 ? 0 : COLUMN_GAP / 2);
            int rowY = cursorY + row * ROW_HEIGHT;
            int lineX = rowX + 26;
            int lineY = rowY + 2;
            int lineWidth = StringUtils.getStringWidth(countLine(source.name(), source.totalCount(), source.maxStackSize()));
            if (isVisibleTextHovered(lineX, lineY, lineWidth, y, visibleHeight, mouseX, mouseY)) {
                return source;
            }
        }

        return null;
    }

    public static void render(WidgetBase widget, class_332 context, int x, int y, int width, class_1799 targetIcon, String targetName, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, int visibleOuterHeight, int mouseX, int mouseY) {
        int height = getHeight(targetIcon, requirements, sources, showAll);
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

        if (!requirements.isEmpty()) {
            widget.drawString(textX, cursorY, 0xFFFFFFFF, StringUtils.translate("lmlp.label.minimal_sources.requires_named", boldTargetName), context);
            cursorY += 18;

            int requirementBoxY = cursorY;
            int requirementHeight = requirements.size() * ROW_HEIGHT;
            RenderUtils.drawRect(textX - 2, requirementBoxY - 2, panelWidth - PADDING * 2 + 4, requirementHeight, 0x66000000);
            for (int index = 0; index < requirements.size(); index++) {
                MinimalSubMaterialListView.RequirementContribution requirement = requirements.get(index);
                renderRequirementRow(widget, context, textX, cursorY + index * ROW_HEIGHT, requirement);
            }
            cursorY += requirementHeight + SECTION_GAP;
        }

        widget.drawString(textX, cursorY, 0xFFFFFFFF, StringUtils.translate("lmlp.label.minimal_sources.header_named", boldTargetName), context);
        cursorY += 18;

        int boxY = cursorY;
        int visibleCount = visibleSourceCount(sources);
        int columns = sourceColumnCount(visibleCount);
        int rowCount = sourceRowCount(visibleCount, columns);
        int boxHeight = rowCount * ROW_HEIGHT;
        RenderUtils.drawRect(textX - 2, boxY - 2, panelWidth - PADDING * 2 + 4, boxHeight, 0x66000000);

        int contentWidth = Math.max(1, panelWidth - PADDING * 2);
        int columnStride = Math.max(1, contentWidth / columns);
        MinimalSubMaterialListView.SourceContribution hoveredSource = GuiBase.isShiftDown()
                ? sourceAt(x, y, width, targetIcon, requirements, sources, showAll, visibleOuterHeight, mouseX, mouseY)
                : null;
        if (columns > 1) {
            drawColumnSeparators(textX, boxY, rowCount, columns, contentWidth);
        }

        for (int index = 0; index < visibleCount; index++) {
            MinimalSubMaterialListView.SourceContribution source = sources.get(index);
            int column = index / rowCount;
            int row = index % rowCount;
            int rowX = textX + column * columnStride + (column == 0 ? 0 : COLUMN_GAP / 2);
            int rowY = cursorY + row * ROW_HEIGHT;
            renderSourceRow(widget, context, rowX, rowY, source, source == hoveredSource);
        }

        context.method_44380();
        drawOutline(x, y, panelWidth, visibleHeight, 0xFF777777);
    }

    private static int visibleSourceCount(List<MinimalSubMaterialListView.SourceContribution> sources) {
        return sources.size();
    }

    private static int requirementHeight(List<MinimalSubMaterialListView.RequirementContribution> requirements) {
        return requirements.isEmpty() ? 0 : 18 + requirements.size() * ROW_HEIGHT + SECTION_GAP;
    }

    private static boolean isVisibleTextHovered(int textX, int textY, int textWidth, int panelY, int visibleHeight, int mouseX, int mouseY) {
        return textWidth > 0
                && mouseX >= textX
                && mouseX < textX + textWidth
                && mouseY >= textY
                && mouseY < textY + TEXT_HOVER_HEIGHT
                && mouseY >= panelY
                && mouseY < panelY + visibleHeight;
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

    private static void renderSourceRow(WidgetBase widget, class_332 context, int textX, int y, MinimalSubMaterialListView.SourceContribution source, boolean underlined) {
        renderCountRow(widget, context, textX, y, source.icon(), source.name(), source.totalCount(), source.missingCount(), source.maxStackSize(), underlined);
    }

    private static void renderRequirementRow(WidgetBase widget, class_332 context, int textX, int y, MinimalSubMaterialListView.RequirementContribution requirement) {
        String line = renderCountRow(widget, context, textX, y, cyclingIcon(requirement.icons(), requirement.icon()), MinimalSubMaterialListView.requirementDisplayName(requirement), requirement.totalCount(), requirement.missingCount(), requirement.maxStackSize());
        MinimalSubMaterialListView.UpstreamRequirement upstream = requirement.upstream();
        if (upstream == null) {
            return;
        }

        int arrowX = textX + 26 + StringUtils.getStringWidth(line) + UPSTREAM_GAP;
        int centerY = y + SOURCE_ICON_BOX_Y_OFFSET + SOURCE_ICON_BOX_SIZE / 2;
        ToggleArrowRenderer.render(context, arrowX, UPSTREAM_ARROW_WIDTH, centerY, 0.0F, false);

        int upstreamIconX = arrowX + UPSTREAM_ARROW_WIDTH + 8;
        renderCountRow(
                widget,
                context,
                upstreamIconX,
                y,
                cyclingIcon(upstream.icons(), upstream.icon()),
                MinimalSubMaterialListView.upstreamDisplayName(upstream),
                upstream.totalCount(),
                upstream.missingCount(),
                upstream.maxStackSize());
    }

    private static void renderNameRow(WidgetBase widget, class_332 context, int textX, int y, class_1799 icon, String name) {
        RenderUtils.drawRect(textX, y + SOURCE_ICON_BOX_Y_OFFSET, SOURCE_ICON_BOX_SIZE, SOURCE_ICON_BOX_SIZE, 0x30FFFFFF);
        context.method_51427(icon, textX + 1, y + SOURCE_ICON_BOX_Y_OFFSET + 1);

        widget.drawString(textX + 26, y + 2, 0xFFFFFFFF, name, context);
    }

    private static String renderCountRow(WidgetBase widget, class_332 context, int textX, int y, class_1799 icon, String name, int totalCount, int missingCount, int maxStackSize) {
        return renderCountRow(widget, context, textX, y, icon, name, totalCount, missingCount, maxStackSize, false);
    }

    private static String renderCountRow(WidgetBase widget, class_332 context, int textX, int y, class_1799 icon, String name, int totalCount, int missingCount, int maxStackSize, boolean underlined) {
        RenderUtils.drawRect(textX, y + SOURCE_ICON_BOX_Y_OFFSET, SOURCE_ICON_BOX_SIZE, SOURCE_ICON_BOX_SIZE, 0x30FFFFFF);
        context.method_51427(icon, textX + 1, y + SOURCE_ICON_BOX_Y_OFFSET + 1);

        String count = CountFormatter.format(totalCount, maxStackSize);
        String line = countLine(name, totalCount, maxStackSize);
        String renderedLine = underlined
                ? UNDERLINE + name + ": " + GuiBase.TXT_GOLD + UNDERLINE + count + GuiBase.TXT_RST
                : name + ": " + GuiBase.TXT_GOLD + count;
        widget.drawString(textX + 26, y + 2, 0xFFFFFFFF, renderedLine, context);
        return line;
    }

    private static String countLine(String name, int totalCount, int maxStackSize) {
        return name + ": " + CountFormatter.format(totalCount, maxStackSize);
    }

    private static class_1799 cyclingIcon(List<class_1799> icons, class_1799 fallback) {
        if (icons.isEmpty()) {
            return fallback;
        }

        int index = (int) ((System.currentTimeMillis() / ICON_CYCLE_MILLIS) % icons.size());
        class_1799 icon = icons.get(index);
        return icon.method_7960() ? fallback : icon;
    }

    private static void drawColumnSeparators(int contentX, int boxY, int rowCount, int columns, int contentWidth) {
        int separatorY = boxY + SOURCE_ICON_BOX_Y_OFFSET;
        int separatorHeight = Math.max(0, (rowCount - 1) * ROW_HEIGHT + SOURCE_ICON_BOX_SIZE);
        for (int separator = 1; separator < columns; separator++) {
            int x = contentX + Math.round(contentWidth * (separator / (float) columns));
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
