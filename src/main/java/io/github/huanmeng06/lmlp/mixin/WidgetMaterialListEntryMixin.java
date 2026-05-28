package io.github.huanmeng06.lmlp.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntrySortable;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.access.WidgetListBoundsAccess;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import io.github.huanmeng06.lmlp.gui.RecipeDetailScreen;
import io.github.huanmeng06.lmlp.gui.RecipeInlineRenderer;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.MaterialCounts;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import net.minecraft.class_1799;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = WidgetMaterialListEntry.class, remap = false)
public abstract class WidgetMaterialListEntryMixin extends WidgetListEntrySortable<MaterialListEntry> {
    private static final int BASE_ENTRY_HEIGHT = 23;
    private static final int EXPANDED_PANEL_BOTTOM_PADDING = 8;
    private static final int FIXED_TOOLTIP_TOP = 14;
    private static final int FIXED_TOOLTIP_RIGHT_MARGIN = 226;
    private static final int FIXED_TOOLTIP_MIN_WIDTH = 420;
    private static final int FIXED_TOOLTIP_HEIGHT = 60;
    private static int lmlpMaxTotalDigits;
    private static int lmlpMaxMissingDigits;
    @Shadow
    private static int maxNameLength;
    @Shadow
    private static int maxCountLength1;
    @Shadow
    private static int maxCountLength2;
    @Shadow
    private static int maxCountLength3;

    @Shadow
    @Final
    private MaterialListBase materialList;
    @Shadow
    @Final
    private WidgetListMaterialList listWidget;
    @Shadow
    @Final
    private MaterialListEntry entry;
    @Shadow
    @Final
    private String header1;
    @Shadow
    @Final
    private String header2;
    @Shadow
    @Final
    private String header3;
    @Shadow
    @Final
    private String header4;
    @Shadow
    @Final
    private boolean isOdd;

    protected WidgetMaterialListEntryMixin(int x, int y, int width, int height, MaterialListEntry entry, int listIndex) {
        super(x, y, width, height, entry, listIndex);
    }

    /**
     * @author Huan_meeng
     * @reason Recalculate material list columns using grouped total and missing count text.
     */
    @Overwrite
    public static void setMaxNameLength(List<MaterialListEntry> entries, int multiplier) {
        maxNameLength = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.item") + GuiBase.TXT_RST);
        maxCountLength1 = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.total") + GuiBase.TXT_RST);
        maxCountLength2 = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.missing") + GuiBase.TXT_RST);
        maxCountLength3 = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.available") + GuiBase.TXT_RST);
        lmlpMaxTotalDigits = 1;
        lmlpMaxMissingDigits = 1;

        for (MaterialListEntry entry : entries) {
            int total = entry.getCountTotal() * multiplier;
            int missing = multiplier == 1 ? entry.getCountMissing() : total;
            lmlpMaxTotalDigits = Math.max(lmlpMaxTotalDigits, Integer.toString(total).length());
            lmlpMaxMissingDigits = Math.max(lmlpMaxMissingDigits, Integer.toString(missing).length());
        }

        for (MaterialListEntry entry : entries) {
            int total = entry.getCountTotal() * multiplier;
            int missing = multiplier == 1 ? entry.getCountMissing() : total;
            class_1799 stack = entry.getStack();

            maxNameLength = Math.max(maxNameLength, StringUtils.getStringWidth(stack.method_7964().getString()));
            maxCountLength1 = Math.max(maxCountLength1, StringUtils.getStringWidth(CountFormatter.formatAligned(stack, total, lmlpMaxTotalDigits)));
            maxCountLength2 = Math.max(maxCountLength2, StringUtils.getStringWidth(CountFormatter.formatAligned(stack, missing, lmlpMaxMissingDigits)));
            maxCountLength3 = Math.max(maxCountLength3, StringUtils.getStringWidth(Integer.toString(entry.getCountAvailable())));
        }
    }

    /**
     * @author Huan_meeng
     * @reason Space columns according to the longer grouped count strings.
     */
    @Overwrite
    protected int getColumnPosX(int column) {
        int xItem = this.x + 4;
        int xTotal = xItem + maxNameLength + 40;
        int xMissing = xTotal + maxCountLength1 + 24;
        int xAvailable = xMissing + maxCountLength2 + 24;

        return switch (column) {
            case 0 -> xItem;
            case 1 -> xTotal;
            case 2 -> xMissing;
            case 3 -> xAvailable;
            case 4 -> xAvailable + maxCountLength3 + 24;
            default -> xItem;
        };
    }

    /**
     * @author Huan_meeng
     * @reason Use row clicks for inline recipe expansion and shift-click for the detail screen.
     */
    @Overwrite
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton) {
        if (super.onMouseClickedImpl(mouseX, mouseY, mouseButton)) {
            return true;
        }

        if (this.entry != null) {
            if (mouseButton == 0 && this.isMouseOver(mouseX, mouseY)) {
                if (!GuiBase.isShiftDown()) {
                    if (this.handleRecipePanelClick(mouseX, mouseY)) {
                        return true;
                    }
                }

                if (GuiBase.isShiftDown()) {
                    List<RecipeSummary> summaries = MaterialListPlusState.resolveFor(this.entry, this.materialList);
                    this.mc.method_1507(new RecipeDetailScreen(GuiUtils.getCurrentScreen(), this.entry.getStack(), MaterialCounts.total(this.entry, this.materialList), MaterialCounts.missing(this.entry, this.materialList), summaries));
                } else {
                    boolean wasExpanded = MaterialListPlusState.isRecipeExpanded(this.entry);
                    int rowTopY = this.y;
                    if (wasExpanded) {
                        MaterialListPlusState.clear();
                    } else {
                        MaterialListPlusState.open(this.entry, this.materialList);
                    }

                    this.listWidget.refreshEntries();
                    if (!wasExpanded) {
                        this.scrollExpandedEntryIntoView(rowTopY);
                    }
                }
                return true;
            }

            return false;
        }

        int column = this.getMouseOverColumn(mouseX, mouseY);
        switch (column) {
            case 0 -> this.materialList.setSortCriteria(MaterialListBase.SortCriteria.NAME);
            case 1 -> this.materialList.setSortCriteria(MaterialListBase.SortCriteria.COUNT_TOTAL);
            case 2 -> this.materialList.setSortCriteria(MaterialListBase.SortCriteria.COUNT_MISSING);
            case 3 -> this.materialList.setSortCriteria(MaterialListBase.SortCriteria.COUNT_AVAILABLE);
            default -> {
                return false;
            }
        }

        this.listWidget.refreshEntries();
        return true;
    }

    /**
     * @author Huan_meeng
     * @reason Preserve original non-selectable rows.
     */
    @Overwrite
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    /**
     * @author Huan_meeng
     * @reason Draw grouped counts and optional inline recipe summaries.
     */
    @Overwrite
    public void render(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        if (this.header1 == null && (selected || this.isMouseOver(mouseX, mouseY))) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0707070);
        } else if (this.isOdd) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0101010);
        } else {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0303030);
        }

        int xItem = this.getColumnPosX(0);
        int xTotal = this.getColumnPosX(1);
        int xMissing = this.getColumnPosX(2);
        int xAvailable = this.getColumnPosX(3);
        int yText = this.y + 7;

        if (this.header1 != null) {
            if (!this.listWidget.getSearchBarWidget().isSearchOpen()) {
                this.drawString(xItem, yText, -1, this.header1, drawContext);
                this.drawString(xTotal, yText, -1, this.header2, drawContext);
                this.drawString(xMissing, yText, -1, this.header3, drawContext);
                this.drawString(xAvailable, yText, -1, this.header4, drawContext);
                this.renderColumnHeader(mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP);
            }

            super.render(mouseX, mouseY, selected, drawContext);
            return;
        }

        if (this.entry == null) {
            super.render(mouseX, mouseY, selected, drawContext);
            return;
        }

        class_1799 stack = this.entry.getStack();
        int total = MaterialCounts.total(this.entry, this.materialList);
        int missing = MaterialCounts.missing(this.entry, this.materialList);
        int available = this.entry.getCountAvailable();

        int iconX = xItem;
        this.drawString(xItem + 20, yText, -1, stack.method_7964().getString(), drawContext);
        this.drawString(xTotal, yText, -1, CountFormatter.formatAligned(stack, total, lmlpMaxTotalDigits), drawContext);
        this.drawString(xMissing, yText, -1, missingColor(missing, available) + CountFormatter.formatAligned(stack, missing, lmlpMaxMissingDigits), drawContext);
        this.drawString(xAvailable, yText, -1, availableColor(available, missing) + available, drawContext);

        drawContext.method_51448().method_22903();
        RenderUtils.enableDiffuseLightingGui3D();
        int iconY = this.y + 3;
        RenderUtils.drawRect(iconX, iconY, 16, 16, 0x20FFFFFF);
        drawContext.method_51427(stack, iconX, iconY);
        RenderSystem.disableBlend();
        RenderUtils.disableDiffuseLighting();
        drawContext.method_51448().method_22909();

        if (MaterialListPlusState.isRecipeExpanded(this.entry)) {
            List<RecipeSummary> summaries = MaterialListPlusState.getSummaries(this.entry, this.materialList);
            int panelY = this.y + 23;
            RecipeInlineRenderer.render(this, drawContext, this.x + 28, panelY, Math.max(180, this.width - 64), summaries, mouseX, mouseY);
        }

        super.render(mouseX, mouseY, selected, drawContext);
    }

    /**
     * @author Huan_meeng
     * @reason Pin material hover details to the top-right HUD area instead of following the mouse.
     */
    @Overwrite
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        if (this.entry == null) {
            return;
        }

        this.renderFixedHoverPanel(drawContext);
    }

    private void renderFixedHoverPanel(class_332 drawContext) {
        class_1799 stack = this.entry.getStack();
        String itemLabel = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.item") + GuiBase.TXT_RST;
        String totalLabel = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.total") + GuiBase.TXT_RST;
        String missingLabel = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.missing") + GuiBase.TXT_RST;
        String itemText = stack.method_7964().getString();
        String totalText = CountFormatter.format(stack, MaterialCounts.total(this.entry, this.materialList));
        String missingText = CountFormatter.format(stack, MaterialCounts.missing(this.entry, this.materialList));

        int labelWidth = Math.max(this.getStringWidth(itemLabel), Math.max(this.getStringWidth(totalLabel), this.getStringWidth(missingLabel)));
        int valueWidth = Math.max(this.getStringWidth(itemText) + 20, Math.max(this.getStringWidth(totalText), this.getStringWidth(missingText)));
        int panelWidth = Math.max(FIXED_TOOLTIP_MIN_WIDTH, labelWidth + valueWidth + 60);
        int maxPanelWidth = Math.max(FIXED_TOOLTIP_MIN_WIDTH, this.mc.method_22683().method_4486() - FIXED_TOOLTIP_RIGHT_MARGIN - 20);
        panelWidth = Math.min(panelWidth, maxPanelWidth);

        int panelX = Math.max(20, this.mc.method_22683().method_4486() - FIXED_TOOLTIP_RIGHT_MARGIN - panelWidth);
        int panelY = FIXED_TOOLTIP_TOP;
        int labelX = panelX + 10;
        int valueX = labelX + labelWidth + 20;
        int lineY = panelY + 10;

        drawContext.method_51448().method_22903();
        drawContext.method_51448().method_46416(0.0F, 0.0F, 200.0F);
        RenderUtils.drawOutlinedBox(panelX, panelY, panelWidth, FIXED_TOOLTIP_HEIGHT, 0xF0000000, 0xFF999999);

        this.drawString(labelX, lineY, 0xFFFFFFFF, itemLabel, drawContext);
        RenderUtils.drawRect(valueX, lineY - 4, 16, 16, 0x20FFFFFF);
        RenderUtils.enableDiffuseLightingGui3D();
        drawContext.method_51427(stack, valueX, lineY - 4);
        RenderUtils.disableDiffuseLighting();
        this.drawString(valueX + 24, lineY, 0xFFFFFFFF, itemText, drawContext);

        lineY += 16;
        this.drawString(labelX, lineY, 0xFFFFFFFF, totalLabel, drawContext);
        this.drawString(valueX, lineY, 0xFFFFFFFF, totalText, drawContext);

        lineY += 16;
        this.drawString(labelX, lineY, 0xFFFFFFFF, missingLabel, drawContext);
        this.drawString(valueX, lineY, 0xFFFFFFFF, missingText, drawContext);

        drawContext.method_51448().method_22909();
    }

    private boolean handleRecipePanelClick(int mouseX, int mouseY) {
        if (!MaterialListPlusState.isRecipeExpanded(this.entry)) {
            return false;
        }

        List<RecipeSummary> summaries = MaterialListPlusState.getSummaries(this.entry, this.materialList);
        int panelX = this.x + 28;
        int panelY = this.y + 23;
        int panelWidth = Math.max(180, this.width - 64);
        RecipeInlineRenderer.ToggleTarget target = RecipeInlineRenderer.toggleAt(summaries, panelX, panelY, panelWidth, mouseX, mouseY);
        if (target.isNone()) {
            return false;
        }

        if (target.ingredient() != null) {
            MaterialListPlusState.toggleIngredientTree(target.ingredient());
        } else {
            MaterialListPlusState.toggleTreeNode(target.nodePath());
        }

        this.listWidget.refreshEntries();
        this.scrollExpandedEntryIntoView(this.y);
        return true;
    }

    private void scrollExpandedEntryIntoView(int rowTopY) {
        if (!(this.listWidget instanceof WidgetListBoundsAccess access)) {
            return;
        }

        int visibleBottom = access.lmlp$getVisibleBottom() - EXPANDED_PANEL_BOTTOM_PADDING;
        int expandedEntryHeight = BASE_ENTRY_HEIGHT + RecipeInlineRenderer.getOuterHeight(MaterialListPlusState.getCachedSummaries(this.entry));
        int overflow = rowTopY + expandedEntryHeight - visibleBottom;
        if (overflow <= 0) {
            return;
        }

        int rows = (overflow + BASE_ENTRY_HEIGHT - 1) / BASE_ENTRY_HEIGHT;
        this.listWidget.getScrollbar().offsetValue(rows);
        this.listWidget.refreshEntries();
    }

    private static String missingColor(int missing, int available) {
        if (missing == 0) {
            return GuiBase.TXT_GREEN;
        }
        if (available >= missing) {
            return GuiBase.TXT_GOLD;
        }
        return GuiBase.TXT_RED;
    }

    private static String availableColor(int available, int missing) {
        if (available >= missing) {
            return GuiBase.TXT_GREEN;
        }
        return GuiBase.TXT_RED;
    }
}
