package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the material list HUD for the minimal sub-material view, mirroring
 * Litematica's MaterialListHudRenderer layout but sourcing entries from
 * MinimalSubMaterialListView and formatting counts with the mod's style.
 */
public final class MinimalSubMaterialHudRenderer {
    private static final long UPDATE_INTERVAL_MS = 1000;
    private static final Map<MaterialListBase, CachedLines> LINE_CACHES = new WeakHashMap<>();

    private MinimalSubMaterialHudRenderer() {
    }

    public static int render(MaterialListBase materialList, int xOffset, int yOffset, HudAlignment alignment, GuiGraphicsExtractor drawContext) {
        GuiContext context = GuiContext.fromGuiGraphics(drawContext);
        Minecraft mc = Minecraft.getInstance();
        List<HudLine> lines = linesFor(materialList);

        if (lines.isEmpty()) {
            return 0;
        }

        final double scale = Configs.InfoOverlays.MATERIAL_LIST_HUD_SCALE.getDoubleValue();
        final int maxLines = Configs.InfoOverlays.MATERIAL_LIST_HUD_MAX_LINES.getIntegerValue();

        if (scale == 0d) {
            return 0;
        }

        Font font = mc.font;
        int bgMargin = 2;
        int lineHeight = 16;
        final int size = Math.min(lines.size(), maxLines);
        int contentHeight = (size * lineHeight) + bgMargin + 10;
        int maxTextLength = 0;
        int maxCountLength = 0;
        int posX = xOffset + bgMargin;
        int posY = yOffset + bgMargin;
        int bgColor = 0xA0000000;
        int textColor = 0xFFFFFFFF;

        for (int i = 0; i < size; i++) {
            HudLine line = lines.get(i);
            maxTextLength = Math.max(maxTextLength, font.width(line.name()));
            maxCountLength = Math.max(maxCountLength, font.width(line.countText()));
        }

        final int maxLineLength = maxTextLength + maxCountLength + 30;

        switch (alignment) {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                posX = (int) ((GuiUtils.getScaledWindowWidth() / scale) - maxLineLength - xOffset - bgMargin);
                break;
            case CENTER:
                posX = (int) ((GuiUtils.getScaledWindowWidth() / scale / 2) - (maxLineLength / 2) - xOffset);
                break;
            default:
        }

        if (scale != 1) {
            yOffset = (int) (yOffset / scale);
        }

        posY = RenderUtils.getHudPosY(posY, yOffset, contentHeight, scale, alignment);
        posY += RenderUtils.getHudOffsetForPotions(alignment, scale, mc.player);

        Matrix3x2fStack matrixStack = context.pose();

        if (scale != 1d) {
            matrixStack.pushMatrix();
            matrixStack.scale((float) scale, (float) scale);
        }

        int x1 = posX - bgMargin;
        int y1 = posY - bgMargin;
        int x2 = x1 + maxLineLength + bgMargin * 2;
        int y2 = y1 + contentHeight + bgMargin;
        context.fill(x1, y1, x2, y2, bgColor);

        int x = posX;
        int y = posY + 12;

        for (int i = 0; i < size; i++) {
            context.renderItem(lines.get(i).stack(), x, y);
            y += lineHeight;
        }

        String title = GuiBase.TXT_BOLD + StringUtils.translate("lmlp.hud.title.minimal_sub_material") + GuiBase.TXT_RST;
        context.drawString(font, title, posX + 2, posY + 2, textColor, false);

        final int itemCountTextColor = Configs.Colors.MATERIAL_LIST_HUD_ITEM_COUNTS.getIntegerValue();
        x = posX + 18;
        y = posY + 16;

        for (int i = 0; i < size; i++) {
            HudLine line = lines.get(i);
            int cntLen = font.width(line.countText());
            int cntPosX = posX + maxLineLength - cntLen - 2;

            context.drawString(font, line.name(), x, y, textColor, false);
            context.drawString(font, line.countText(), cntPosX, y, itemCountTextColor, false);

            y += lineHeight;
        }

        if (scale != 1d) {
            matrixStack.popMatrix();
        }

        return contentHeight + 4;
    }

    private static List<HudLine> linesFor(MaterialListBase materialList) {
        long currentTime = System.currentTimeMillis();
        CachedLines cached = LINE_CACHES.get(materialList);

        if (cached != null && currentTime - cached.updateTime() <= UPDATE_INTERVAL_MS) {
            return cached.lines();
        }

        List<MaterialListEntry> entries = MinimalSubMaterialListView.entries(materialList);
        List<HudLine> lines = new ArrayList<>();

        for (MaterialListEntry entry : entries) {
            int count = MinimalSubMaterialListView.netMissing(entry, materialList);
            if (count <= 0) {
                continue;
            }

            ItemStack stack = MinimalSubMaterialListView.displayStack(entry);
            String name = MinimalSubMaterialListView.displayName(entry);
            lines.add(new HudLine(stack, name, count, CountFormatter.format(count, stack.getMaxStackSize())));
        }

        lines.sort(Comparator.comparingInt(HudLine::count).reversed());
        LINE_CACHES.put(materialList, new CachedLines(lines, currentTime));
        return lines;
    }

    private record HudLine(ItemStack stack, String name, int count, String countText) {
    }

    private record CachedLines(List<HudLine> lines, long updateTime) {
    }
}
