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
                if (GuiBase.isShiftDown()) {
                    List<RecipeSummary> summaries = MaterialListPlusState.resolveFor(this.entry, this.materialList);
                    this.mc.method_1507(new RecipeDetailScreen(GuiUtils.getCurrentScreen(), this.entry.getStack(), MaterialCounts.total(this.entry, this.materialList), MaterialCounts.missing(this.entry, this.materialList), summaries));
                } else {
                    MaterialListPlusState.toggle(this.entry, this.materialList);
                    this.listWidget.refreshEntries();
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

        this.drawString(xItem + 20, yText, -1, stack.method_7964().getString(), drawContext);
        this.drawString(xTotal, yText, -1, CountFormatter.formatAligned(stack, total, lmlpMaxTotalDigits), drawContext);
        this.drawString(xMissing, yText, -1, missingColor(missing, available) + CountFormatter.formatAligned(stack, missing, lmlpMaxMissingDigits), drawContext);
        this.drawString(xAvailable, yText, -1, availableColor(available, missing) + available, drawContext);

        drawContext.method_51448().method_22903();
        RenderUtils.enableDiffuseLightingGui3D();
        int iconY = this.y + 3;
        RenderUtils.drawRect(xItem, iconY, 16, 16, 0x20FFFFFF);
        drawContext.method_51427(stack, xItem, iconY);
        RenderSystem.disableBlend();
        RenderUtils.disableDiffuseLighting();
        drawContext.method_51448().method_22909();

        if (MaterialListPlusState.isExpanded(this.entry)) {
            List<RecipeSummary> summaries = MaterialListPlusState.getSummaries(this.entry, this.materialList);
            int panelHeight = RecipeInlineRenderer.getHeight(summaries);
            int panelY = this.y + 23;
            int visibleBottom = this.listWidget instanceof io.github.huanmeng06.lmlp.mixin.access.WidgetListBoundsAccess access ? access.lmlp$getVisibleBottom() : this.y + this.height;
            if (panelY + panelHeight > visibleBottom) {
                panelY = Math.max(this.y - panelHeight - 2, 24);
            }
            RecipeInlineRenderer.render(this, drawContext, this.x + 28, panelY, Math.max(180, this.width - 64), summaries);
        }

        super.render(mouseX, mouseY, selected, drawContext);
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
