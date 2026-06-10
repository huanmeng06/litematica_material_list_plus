package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.gui.widgets.WidgetSchematicEntry;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.PlacementLine;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.KnownPlacementRow;
import net.minecraft.class_332;

import java.util.ArrayList;
import java.util.List;

public class KnownLoadedSchematicEntry extends WidgetSchematicEntry {
    private final KnownPlacementRow row;
    private final KnownLoadedSchematicsList listParent;
    private final boolean isOdd;
    private int buttonsStartX;

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
        this.buttonsStartX = this.x + this.width;
        this.subWidgets.clear();
        this.createButtons();
    }

    private void createButtons() {
        if (this.row == null || !this.row.isPlacement()) {
            return;
        }

        KnownPlacementContext context = this.row.context();
        if (context == null || KnownPlacementRows.readStatus(context) == KnownPlacementRows.ReadStatus.LIVE) {
            return;
        }

        int buttonX = this.x + this.width - 2;
        int buttonY = KnownPlacementRows.buttonY(this.y);
        this.buttonsStartX = this.addClearCacheButton(buttonX, buttonY, context);
    }

    private int addClearCacheButton(int buttonX, int buttonY, KnownPlacementContext context) {
        ButtonGeneric button = new ButtonGeneric(buttonX, buttonY, -1, true, StringUtils.translate("lmlp.gui.button.clear_cache"));
        this.addButton(button, (clickedButton, mouseButton) -> {
            if (ChunkMissingMaterialListCache.clearKnownPlacementCache(context.key(), "loaded_schematics.clear_cache_button")) {
                this.listParent.refreshEntries();
            }
        });
        return button.getX() - 1;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
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
        boolean materialSelected = context != null && context.selected();
        if (this.isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0707070);
        } else if (this.isOdd) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0101010);
        } else {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0303030);
        }

        if (materialSelected) {
            KnownPlacementRows.renderSelectedOutline(this);
        }

        String name = context == null ? "" : context.name();
        PlacementLine line = KnownPlacementRows.placementLine(this, context, name, KnownPlacementRows.contentRight(this, this.buttonsStartX));
        boolean nameHovered = line.nameHovered(this, mouseX, mouseY);
        if (nameHovered) {
            ClickableCursor.requestHand();
        }
        KnownPlacementRows.renderPlacementLine(this, this.zLevel, drawContext, line, "", nameHovered);
        this.drawSubWidgets(mouseX, mouseY, drawContext);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.isMouseOver(mouseX, mouseY) || mouseButton != 0) {
            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        if (super.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }

        if (mouseButton == 0 && this.row != null && this.row.isTableHeader()
                && KnownPlacementRows.clickTableHeader(this, this.row, mouseX, mouseY)) {
            this.listParent.refreshEntries();
            return true;
        }

        if (mouseButton == 0 && this.row != null && this.row.isPlacement() && this.isPlacementNameHovered(mouseX, mouseY)) {
            KnownPlacementContext context = this.row.context();
            return MaterialListOpener.openContext(context.key(), "loaded_schematics_list.name_click", this.listParent.getParentGui());
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        if (this.row == null || !this.isMouseOver(mouseX, mouseY)) {
            return;
        }

        if (this.row.isTableHeader()) {
            return;
        }

        if (this.row.isHeader()) {
            List<String> lines = new ArrayList<>();
            lines.add(this.row.displayName());
            RenderUtils.drawHoverText(mouseX, mouseY, lines, drawContext);
            return;
        }

        KnownPlacementContext context = this.row.context();
        PlacementLine line = KnownPlacementRows.placementLine(this, context, context == null ? "" : context.name(), KnownPlacementRows.contentRight(this, this.buttonsStartX));
        List<String> lines = new ArrayList<>();
        if (line.statusHovered(this, mouseX, mouseY)) {
            lines.addAll(line.status().tooltipLines());
        } else if (line.fileHovered(this, mouseX, mouseY) && !line.fileHoverText().isEmpty()) {
            lines.add(line.fileHoverText());
        }

        if (!lines.isEmpty()) {
            RenderUtils.drawHoverText(mouseX, mouseY, lines, drawContext);
        }

        this.drawHoveredSubWidget(mouseX, mouseY, drawContext);
    }

    private boolean isPlacementNameHovered(int mouseX, int mouseY) {
        if (this.row == null || !this.row.isPlacement() || this.row.context() == null) {
            return false;
        }

        KnownPlacementContext context = this.row.context();
        PlacementLine line = KnownPlacementRows.placementLine(this, context, context.name(), KnownPlacementRows.contentRight(this, this.buttonsStartX));
        return line.nameHovered(this, mouseX, mouseY);
    }
}
