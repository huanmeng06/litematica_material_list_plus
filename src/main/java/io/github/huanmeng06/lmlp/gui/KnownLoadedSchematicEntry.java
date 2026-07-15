package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.gui.widgets.WidgetSchematicEntry;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.PlacementLine;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.KnownPlacementRow;
import fi.dy.masa.malilib.render.GuiContext;

import java.util.ArrayList;
import java.util.List;

public class KnownLoadedSchematicEntry extends WidgetSchematicEntry {
    private final KnownPlacementRow row;
    private final KnownLoadedSchematicsList listParent;
    private final boolean isOdd;

    public KnownLoadedSchematicEntry(
            int x,
            int y,
            int width,
            boolean isOdd,
            LitematicaSchematic schematic,
            KnownPlacementRow row,
            int listIndex,
            KnownLoadedSchematicsList parent) {
        super(x, y, width, KnownPlacementRows.ROW_HEIGHT, isOdd, schematic, listIndex, parent);
        this.row = row;
        this.listParent = parent;
        this.isOdd = isOdd;
        this.subWidgets.clear();
    }

    @Override
    public void render(GuiContext drawContext, int mouseX, int mouseY, boolean selected) {
        if (this.row == null) {
            return;
        }

        if (this.row.isTableHeader()) {
            KnownPlacementRows.renderTableHeader(this, this.row, mouseX, mouseY, drawContext);
            return;
        }

        if (this.row.isHeader()) {
            KnownPlacementRows.renderHeader(this, this.row, mouseX, mouseY, drawContext);
            return;
        }

        KnownPlacementContext context = this.row.context();
        boolean materialSelected = context != null && ChunkMissingMaterialListCache.isMaterialListContextSelected(context.key());
        if (this.isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawRect(drawContext, this.x, this.y, this.width, this.height, 0xA0707070);
        } else if (this.isOdd) {
            RenderUtils.drawRect(drawContext, this.x, this.y, this.width, this.height, 0xA0101010);
        } else {
            RenderUtils.drawRect(drawContext, this.x, this.y, this.width, this.height, 0xA0303030);
        }

        if (materialSelected) {
            KnownPlacementRows.renderSelectedOutline(this, drawContext);
        }

        String name = context == null ? "" : context.name();
        PlacementLine line = KnownPlacementRows.placementLine(this, context, name, this.row.pageId());
        boolean nameHovered = line.nameHovered(this, mouseX, mouseY);
        boolean originHovered = PlacementOriginMarker.originHovered(context, line, this, mouseX, mouseY);
        if (nameHovered || originHovered) {
            ClickableCursor.requestHand();
        }
        KnownPlacementRows.renderPlacementLine(this, this.zLevel, drawContext, line, "", nameHovered, context, originHovered);
        this.drawSubWidgets(drawContext, mouseX, mouseY);
    }

    @Override
    public boolean onMouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int mouseButton = event.buttonInfo().button();
        if (!this.isMouseOver(mouseX, mouseY) || mouseButton != 0) {
            return super.onMouseClicked(event, doubleClick);
        }

        if (mouseButton == 0 && this.row != null && this.row.isTableHeader()
                && KnownPlacementRows.clickTableHeader(this, this.row, mouseX, mouseY)) {
            this.listParent.refreshEntries();
            return true;
        }

        if (mouseButton == 0 && this.row != null && this.row.isPlacement()) {
            KnownPlacementContext context = this.row.context();
            PlacementLine line = KnownPlacementRows.placementLine(this, context, context == null ? "" : context.name(), this.row.pageId());
            if (PlacementOriginMarker.originHovered(context, line, this, mouseX, mouseY)) {
                return PlacementOriginMarker.handleOriginClick(context);
            }
        }

        if (mouseButton == 0 && this.row != null && this.row.isPlacement() && this.isPlacementNameHovered(mouseX, mouseY)) {
            KnownPlacementContext context = this.row.context();
            return MaterialListOpener.openContext(context.key(), "loaded_schematics_list.name_click", this.listParent.getParentGui());
        }

        return super.onMouseClicked(event, doubleClick);
    }

    @Override
    public void postRenderHovered(GuiContext drawContext, int mouseX, int mouseY, boolean selected) {
        if (this.row == null || !this.isMouseOver(mouseX, mouseY)) {
            return;
        }

        if (this.row.isTableHeader()) {
            return;
        }

        if (this.row.isHeader()) {
            List<String> lines = new ArrayList<>();
            lines.add(this.row.displayName());
            RenderUtils.drawHoverText(drawContext, mouseX, mouseY, lines);
            return;
        }

        KnownPlacementContext context = this.row.context();
        PlacementLine line = KnownPlacementRows.placementLine(this, context, context == null ? "" : context.name(), this.row.pageId());
        List<String> lines = new ArrayList<>();
        if (line.statusHovered(this, mouseX, mouseY)) {
            lines.addAll(line.status().tooltipLines());
        } else if (line.nameTooltipHovered(this, mouseX, mouseY)) {
            lines.add(line.nameHoverText());
        } else if (line.fileHovered(this, mouseX, mouseY) && !line.fileHoverText().isEmpty()) {
            line.fileHoverText().lines().forEach(lines::add);
        } else if (PlacementOriginMarker.originHovered(context, line, this, mouseX, mouseY)) {
            lines.add(StringUtils.translate("lmlp.gui.known_placement.origin_highlight_hint"));
        } else if (PlacementOriginMarker.disabledOriginHovered(context, line, this, mouseX, mouseY)) {
            lines.add(StringUtils.translate("lmlp.gui.known_placement.origin_wrong_dimension_hint"));
        }

        if (!lines.isEmpty()) {
            RenderUtils.drawHoverText(drawContext, mouseX, mouseY, lines);
        }

        this.drawHoveredSubWidget(drawContext, mouseX, mouseY);
    }

    private boolean isPlacementNameHovered(int mouseX, int mouseY) {
        if (this.row == null || !this.row.isPlacement() || this.row.context() == null) {
            return false;
        }

        KnownPlacementContext context = this.row.context();
        PlacementLine line = KnownPlacementRows.placementLine(this, context, context.name(), this.row.pageId());
        return line.nameHovered(this, mouseX, mouseY);
    }
}
