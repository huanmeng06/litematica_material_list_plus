package io.github.huanmeng06.lmlp.gui;

import java.util.List;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import net.minecraft.class_1799;
import net.minecraft.class_332;

public final class MinimalSourceInlineRenderer {
    private static final int ROW_HEIGHT = 22;
    private static final int PADDING = 8;
    private static final int INNER_BOTTOM_PADDING = 4;
    private static final int ENTRY_BOTTOM_GAP = 4;

    private MinimalSourceInlineRenderer() {
    }

    public static int getHeight(List<MinimalSubMaterialListView.SourceContribution> sources) {
        if (sources.isEmpty()) {
            return 48;
        }

        return 64 + sources.size() * ROW_HEIGHT + INNER_BOTTOM_PADDING;
    }

    public static int getOuterHeight(List<MinimalSubMaterialListView.SourceContribution> sources) {
        return getHeight(sources) + ENTRY_BOTTOM_GAP;
    }

    public static int getOuterHeight(List<MinimalSubMaterialListView.SourceContribution> sources, float progress) {
        return Math.round(getOuterHeight(sources) * progress);
    }

    public static void render(WidgetBase widget, class_332 context, int x, int y, int width, class_1799 targetIcon, String targetName, List<MinimalSubMaterialListView.SourceContribution> sources, int visibleOuterHeight) {
        int height = getHeight(sources);
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

        widget.drawString(textX, cursorY, 0xFFFFFFFF, StringUtils.translate("lmlp.label.minimal_sources.header"), context);
        cursorY += 18;

        int boxY = cursorY;
        int boxHeight = 18 + sources.size() * ROW_HEIGHT;
        RenderUtils.drawRect(textX - 2, boxY - 2, panelWidth - PADDING * 2 + 4, boxHeight, 0x66000000);
        widget.drawString(textX, cursorY, 0xFFAAAAAA, StringUtils.translate("lmlp.label.minimal_sources.source_materials"), context);
        cursorY += 18;

        for (MinimalSubMaterialListView.SourceContribution source : sources) {
            renderSourceRow(widget, context, textX, cursorY, source);
            cursorY += ROW_HEIGHT;
        }

        context.method_44380();
        drawOutline(x, y, panelWidth, visibleHeight, 0xFF777777);
    }

    private static void renderSourceRow(WidgetBase widget, class_332 context, int textX, int y, MinimalSubMaterialListView.SourceContribution source) {
        RenderUtils.drawRect(textX, y - 3, 18, 18, 0x30FFFFFF);
        context.method_51427(source.icon(), textX + 1, y - 2);

        String line = source.name() + ": " + GuiBase.TXT_GOLD + CountFormatter.format(source.totalCount(), source.maxStackSize());
        if (source.missingCount() != source.totalCount()) {
            line += GuiBase.TXT_RST + " / " + GuiBase.TXT_RED + CountFormatter.format(source.missingCount(), source.maxStackSize());
        }
        widget.drawString(textX + 26, y + 2, 0xFFFFFFFF, line, context);
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
