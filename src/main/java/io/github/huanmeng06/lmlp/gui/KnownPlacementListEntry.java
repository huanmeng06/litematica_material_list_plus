package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicPlacements;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.ColumnLayout;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.PlacementLine;
import net.minecraft.class_332;

import java.util.ArrayList;
import java.util.List;

public class KnownPlacementListEntry extends WidgetSchematicPlacement {
    private static final int ROW_HEIGHT = KnownPlacementRows.ROW_HEIGHT;

    private final KnownPlacementContext context;

    public KnownPlacementListEntry(
            int x,
            int y,
            int width,
            boolean isOdd,
            SchematicPlacement placement,
            int listIndex,
            WidgetListSchematicPlacements parent) {
        super(x, y, width, ROW_HEIGHT, isOdd, placement, listIndex, parent);
        KnownPlacementContext knownContext = ChunkMissingMaterialListCache.knownContextFor(placement);
        this.context = knownContext;
        this.subWidgets.clear();
        this.createButtons();
    }

    public static int rowHeight() {
        return ROW_HEIGHT;
    }

    private void createButtons() {
        if (!this.canModifyPlacement()) {
            this.buttonsStartX = this.x + this.width;
            return;
        }

        ColumnLayout columns = KnownPlacementRows.computeColumns(this, KnownPlacementRows.PAGE_SCHEMATIC_PLACEMENTS);
        int buttonX = columns.contentRight();
        int buttonY = KnownPlacementRows.buttonY(this.y);
        buttonX = this.addRemoveButton(buttonX, buttonY);
        buttonX = this.addToggleButton(buttonX, buttonY);
        buttonX = this.addConfigureButton(buttonX, buttonY);

        this.buttonsStartX = columns.actionX();
    }

    private boolean canModifyPlacement() {
        return this.context != null && this.context.canEdit() && this.context.placement() != null;
    }

    private int addConfigureButton(int buttonX, int buttonY) {
        String label = StringUtils.translate("litematica.gui.button.schematic_placements.configure");
        ButtonGeneric button = new ButtonGeneric(buttonX, buttonY, KnownPlacementRows.configureButtonWidth(this), true, label);
        this.addButton(button, (clickedButton, mouseButton) -> {
            ChunkMissingMaterialListCache.selectEditablePlacement(this.placement, "known_placement.configure_button");
            GuiPlacementConfiguration gui = new GuiPlacementConfiguration(this.placement);
            gui.setParent(this.parent.getParentGui());
            GuiBase.openGui(gui);
        });
        return button.getX() - KnownPlacementRows.buttonGap();
    }

    private int addToggleButton(int buttonX, int buttonY) {
        ButtonOnOff button = new ButtonOnOff(
                buttonX,
                buttonY,
                KnownPlacementRows.toggleButtonWidth(this),
                true,
                "litematica.gui.button.schematic_placements.placement_enabled",
                this.placement.isEnabled());
        this.addButton(button, (clickedButton, mouseButton) -> {
            this.placement.toggleEnabled();
            ChunkMissingMaterialListCache.rememberPlacementContext(this.placement, "known_placement.toggle_enabled");
            this.parent.refreshEntries();
        });
        return button.getX() - KnownPlacementRows.buttonGap();
    }

    private int addRemoveButton(int buttonX, int buttonY) {
        String label = StringUtils.translate("litematica.gui.button.schematic_placements.remove");
        ButtonGeneric button = new ButtonGeneric(buttonX, buttonY, KnownPlacementRows.removeButtonWidth(this), true, label);
        IButtonActionListener listener = (clickedButton, mouseButton) -> {
            boolean allowCurrentDimensionRemoval = this.context != null && this.context.canEdit();
            if (ChunkMissingMaterialListCache.removeKnownPlacement(this.placement, allowCurrentDimensionRemoval, "known_placement.remove_button")) {
                this.parent.refreshEntries();
            }
        };
        this.addButton(button, listener);
        return button.getX() - KnownPlacementRows.buttonGap();
    }

    @Override
    public boolean canSelectAt(net.minecraft.class_11909 event) {
        return event.comp_4798() < this.buttonsStartX && super.canSelectAt(event);
    }

    @Override
    public boolean onMouseClicked(net.minecraft.class_11909 event, boolean doubleClick) {
        int mouseX = (int) event.comp_4798();
        int mouseY = (int) event.comp_4799();
        int mouseButton = event.comp_4800().comp_4801();
        if (mouseButton == 0 && this.context != null) {
            PlacementLine line = KnownPlacementRows.placementLine(this, this.context, this.placement.getName(), KnownPlacementRows.PAGE_SCHEMATIC_PLACEMENTS);
            if (PlacementOriginMarker.originHovered(this.context, line, this, mouseX, mouseY)) {
                return PlacementOriginMarker.handleOriginClick(this.context);
            }
        }

        if (mouseButton == 0 && this.isPlacementNameHovered(mouseX, mouseY) && this.context != null) {
            return MaterialListOpener.openContext(this.context.key(), "schematic_placements_list.name_click_legacy", this.parent.getParentGui());
        }

        if (mouseButton == 0 && this.context != null && mouseX < this.buttonsStartX) {
            ChunkMissingMaterialListCache.selectEditablePlacement(this.placement, "schematic_placements_list.row_click_legacy");
        }

        return super.onMouseClicked(event, doubleClick);
    }

    @Override
    public void render(class_332 drawContext, int mouseX, int mouseY, boolean selected) {
        boolean materialSelected = this.context != null && ChunkMissingMaterialListCache.isMaterialListContextSelected(this.context.key());
        if (selected || this.isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawRect(drawContext, this.x, this.y, this.width, this.height, 0xA0707070);
        } else if (this.isOdd) {
            RenderUtils.drawRect(drawContext, this.x, this.y, this.width, this.height, 0xA0101010);
        } else {
            RenderUtils.drawRect(drawContext, this.x, this.y, this.width, this.height, 0xA0303030);
        }

        if (materialSelected) {
            KnownPlacementRows.renderSelectedOutline(this, drawContext);
        }

        String enabledColor = this.placement.isEnabled() ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        KnownPlacementContext context = this.context;
        PlacementLine line = KnownPlacementRows.placementLine(this, context, this.placement.getName(), KnownPlacementRows.PAGE_SCHEMATIC_PLACEMENTS);
        boolean nameHovered = line.nameHovered(this, mouseX, mouseY);
        boolean originHovered = PlacementOriginMarker.originHovered(context, line, this, mouseX, mouseY);
        if (nameHovered || originHovered) {
            ClickableCursor.requestHand();
        }
        KnownPlacementRows.renderPlacementLine(this, this.zLevel, drawContext, line, enabledColor, nameHovered, context, originHovered);

        this.drawSubWidgets(drawContext, mouseX, mouseY);
    }

    @Override
    public void postRenderHovered(class_332 drawContext, int mouseX, int mouseY, boolean selected) {
        if (this.isMouseOver(mouseX, mouseY) && mouseX < this.buttonsStartX && this.context != null) {
            PlacementLine line = KnownPlacementRows.placementLine(this, this.context, this.placement.getName(), KnownPlacementRows.PAGE_SCHEMATIC_PLACEMENTS);
            List<String> lines = new ArrayList<>();
            if (line.statusHovered(this, mouseX, mouseY)) {
                lines.addAll(line.status().tooltipLines());
            } else if (line.nameTooltipHovered(this, mouseX, mouseY)) {
                lines.add(line.nameHoverText());
            } else if (line.fileHovered(this, mouseX, mouseY) && !line.fileHoverText().isEmpty()) {
                line.fileHoverText().lines().forEach(lines::add);
            } else if (PlacementOriginMarker.originHovered(this.context, line, this, mouseX, mouseY)) {
                lines.add(StringUtils.translate("lmlp.gui.known_placement.origin_highlight_hint"));
            } else if (PlacementOriginMarker.disabledOriginHovered(this.context, line, this, mouseX, mouseY)) {
                lines.add(StringUtils.translate("lmlp.gui.known_placement.origin_wrong_dimension_hint"));
            }

            if (!lines.isEmpty()) {
                RenderUtils.drawHoverText(drawContext, mouseX, mouseY, lines);
            }
        }

        this.drawHoveredSubWidget(drawContext, mouseX, mouseY);
    }

    private boolean isPlacementNameHovered(int mouseX, int mouseY) {
        if (this.placement == null) {
            return false;
        }

        PlacementLine line = KnownPlacementRows.placementLine(this, this.context, this.placement.getName(), KnownPlacementRows.PAGE_SCHEMATIC_PLACEMENTS);
        return line.nameHovered(this, mouseX, mouseY);
    }
}
