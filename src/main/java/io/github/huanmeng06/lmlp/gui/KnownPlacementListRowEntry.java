package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicPlacements;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.PlacementLine;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.KnownPlacementRow;
import net.minecraft.class_332;

import java.util.ArrayList;
import java.util.List;

public class KnownPlacementListRowEntry extends WidgetListEntryBase<KnownPlacementRow> {
    private final KnownPlacementRow row;
    private final WidgetListSchematicPlacements parent;
    private final boolean isOdd;
    private int buttonsStartX;

    public KnownPlacementListRowEntry(
            int x,
            int y,
            int width,
            boolean isOdd,
            KnownPlacementRow row,
            int listIndex,
            WidgetListSchematicPlacements parent) {
        super(x, y, width, KnownPlacementRows.ROW_HEIGHT, row, listIndex);
        this.row = row;
        this.parent = parent;
        this.isOdd = isOdd;
        this.buttonsStartX = this.x + this.width;
        this.createButtons();
    }

    private void createButtons() {
        if (this.row == null || !this.row.isPlacement()) {
            return;
        }

        KnownPlacementContext context = this.row.context();
        if (!canModifyPlacement(context)) {
            return;
        }

        int buttonX = this.x + this.width - 2;
        int buttonY = KnownPlacementRows.buttonY(this.y);
        buttonX = this.addRemoveButton(buttonX, buttonY, context);
        buttonX = this.addToggleButton(buttonX, buttonY, context.placement());
        buttonX = this.addConfigureButton(buttonX, buttonY, context.placement());

        this.buttonsStartX = buttonX;
    }

    private static boolean canModifyPlacement(KnownPlacementContext context) {
        return context != null && context.canEdit() && context.placement() != null;
    }

    private int addConfigureButton(int buttonX, int buttonY, SchematicPlacement placement) {
        String label = StringUtils.translate("litematica.gui.button.schematic_placements.configure");
        ButtonGeneric button = new ButtonGeneric(buttonX, buttonY, -1, true, label);
        this.addButton(button, (clickedButton, mouseButton) -> {
            GuiPlacementConfiguration gui = new GuiPlacementConfiguration(placement);
            gui.setParent(this.parent.getParentGui());
            GuiBase.openGui(gui);
        });
        return button.getX() - 1;
    }

    private int addToggleButton(int buttonX, int buttonY, SchematicPlacement placement) {
        ButtonOnOff button = new ButtonOnOff(
                buttonX,
                buttonY,
                -1,
                true,
                "litematica.gui.button.schematic_placements.placement_enabled",
                placement.isEnabled());
        this.addButton(button, (clickedButton, mouseButton) -> {
            placement.toggleEnabled();
            ChunkMissingMaterialListCache.rememberPlacementContext(placement, "known_placement.toggle_enabled");
            this.parent.refreshEntries();
        });
        return button.getX() - 2;
    }

    private int addRemoveButton(int buttonX, int buttonY, KnownPlacementContext context) {
        ButtonGeneric button = new ButtonGeneric(buttonX, buttonY, -1, true, StringUtils.translate("litematica.gui.button.schematic_placements.remove"));
        this.addButton(button, (clickedButton, mouseButton) -> {
            boolean allowCurrentDimensionRemoval = context.canEdit();
            if (ChunkMissingMaterialListCache.removeKnownPlacementContext(context.key(), allowCurrentDimensionRemoval, "known_placement.remove_button")) {
                this.parent.refreshEntries();
            }
        });
        return button.getX() - 1;
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.isMouseOver(mouseX, mouseY) || mouseButton != 0) {
            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        if (super.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }

        if (this.row == null) {
            return false;
        }

        if (this.row.isTableHeader()) {
            if (KnownPlacementRows.clickTableHeader(this, this.row, mouseX, mouseY)) {
                this.parent.refreshEntries();
            }
            return true;
        }

        if (this.row.isHeader()) {
            KnownPlacementRows.toggle(this.row.pageId(), this.row.dimension());
            this.parent.refreshEntries();
            return true;
        }

        if (this.row.isPlacement() && this.isPlacementNameHovered(mouseX, mouseY)) {
            KnownPlacementContext context = this.row.context();
            return MaterialListOpener.openContext(context.key(), "schematic_placements_list.name_click", this.parent.getParentGui());
        }

        if (this.row.isPlacement() && mouseX < this.buttonsStartX) {
            KnownPlacementContext context = this.row.context();
            ChunkMissingMaterialListCache.selectMaterialListContext(context.key(), "schematic_placements_list.row_click");
            return true;
        }

        return false;
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
        if (this.isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0707070);
        } else if (this.isOdd) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0101010);
        } else {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0303030);
        }

        if (ChunkMissingMaterialListCache.isMaterialListContextSelected(context.key())) {
            KnownPlacementRows.renderSelectedOutline(this);
        }

        String color = context.placement() == null
                ? GuiBase.TXT_GRAY
                : context.placement().isEnabled() ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        PlacementLine line = KnownPlacementRows.placementLine(this, context, context.name(), KnownPlacementRows.contentRight(this, this.buttonsStartX));
        boolean nameHovered = line.nameHovered(this, mouseX, mouseY);
        if (nameHovered) {
            ClickableCursor.requestHand();
        }
        KnownPlacementRows.renderPlacementLine(this, this.zLevel, drawContext, line, color, nameHovered);

        this.drawSubWidgets(mouseX, mouseY, drawContext);
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
        } else if (this.row.isPlacement() && mouseX < this.buttonsStartX) {
            KnownPlacementContext context = this.row.context();
            PlacementLine line = KnownPlacementRows.placementLine(this, context, context.name(), KnownPlacementRows.contentRight(this, this.buttonsStartX));
            List<String> lines = new ArrayList<>();
            if (line.statusHovered(this, mouseX, mouseY)) {
                lines.addAll(line.status().tooltipLines());
            } else if (line.fileHovered(this, mouseX, mouseY) && !line.fileHoverText().isEmpty()) {
                lines.add(line.fileHoverText());
            }

            if (!lines.isEmpty()) {
                RenderUtils.drawHoverText(mouseX, mouseY, lines, drawContext);
            }
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
