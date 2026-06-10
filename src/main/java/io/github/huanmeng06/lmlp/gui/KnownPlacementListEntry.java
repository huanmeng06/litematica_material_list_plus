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

        int buttonX = this.x + this.width - 2;
        int buttonY = KnownPlacementRows.buttonY(this.y);
        buttonX = this.addRemoveButton(buttonX, buttonY);
        buttonX = this.addToggleButton(buttonX, buttonY);
        buttonX = this.addConfigureButton(buttonX, buttonY);

        this.buttonsStartX = buttonX;
    }

    private boolean canModifyPlacement() {
        return this.context != null && this.context.canEdit() && this.context.placement() != null;
    }

    private int addConfigureButton(int buttonX, int buttonY) {
        String label = StringUtils.translate("litematica.gui.button.schematic_placements.configure");
        ButtonGeneric button = new ButtonGeneric(buttonX, buttonY, -1, true, label);
        this.addButton(button, (clickedButton, mouseButton) -> {
            GuiPlacementConfiguration gui = new GuiPlacementConfiguration(this.placement);
            gui.setParent(this.parent.getParentGui());
            GuiBase.openGui(gui);
        });
        return button.getX() - 1;
    }

    private int addToggleButton(int buttonX, int buttonY) {
        ButtonOnOff button = new ButtonOnOff(
                buttonX,
                buttonY,
                -1,
                true,
                "litematica.gui.button.schematic_placements.placement_enabled",
                this.placement.isEnabled());
        this.addButton(button, (clickedButton, mouseButton) -> {
            this.placement.toggleEnabled();
            ChunkMissingMaterialListCache.rememberPlacementContext(this.placement, "known_placement.toggle_enabled");
            this.parent.refreshEntries();
        });
        return button.getX() - 2;
    }

    private int addRemoveButton(int buttonX, int buttonY) {
        String label = StringUtils.translate("litematica.gui.button.schematic_placements.remove");
        ButtonGeneric button = new ButtonGeneric(buttonX, buttonY, -1, true, label);
        IButtonActionListener listener = (clickedButton, mouseButton) -> {
            boolean allowCurrentDimensionRemoval = this.context != null && this.context.canEdit();
            if (ChunkMissingMaterialListCache.removeKnownPlacement(this.placement, allowCurrentDimensionRemoval, "known_placement.remove_button")) {
                this.parent.refreshEntries();
            }
        };
        this.addButton(button, listener);
        return button.getX() - 1;
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton) {
        return mouseX < this.buttonsStartX && super.canSelectAt(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isPlacementNameHovered(mouseX, mouseY) && this.context != null) {
            return MaterialListOpener.openContext(this.context.key(), "schematic_placements_list.name_click_legacy", this.parent.getParentGui());
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        boolean materialSelected = this.context != null && ChunkMissingMaterialListCache.isMaterialListContextSelected(this.context.key());
        if (selected || this.isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0707070);
        } else if (this.isOdd) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0101010);
        } else {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0303030);
        }

        if (materialSelected) {
            KnownPlacementRows.renderSelectedOutline(this);
        }

        String enabledColor = this.placement.isEnabled() ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        KnownPlacementContext context = this.context;
        PlacementLine line = KnownPlacementRows.placementLine(this, context, this.placement.getName(), KnownPlacementRows.contentRight(this, this.buttonsStartX));
        boolean nameHovered = line.nameHovered(this, mouseX, mouseY);
        if (nameHovered) {
            ClickableCursor.requestHand();
        }
        KnownPlacementRows.renderPlacementLine(this, this.zLevel, drawContext, line, enabledColor, nameHovered);

        this.drawSubWidgets(mouseX, mouseY, drawContext);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        if (this.isMouseOver(mouseX, mouseY) && mouseX < this.buttonsStartX && this.context != null) {
            PlacementLine line = KnownPlacementRows.placementLine(this, this.context, this.placement.getName(), KnownPlacementRows.contentRight(this, this.buttonsStartX));
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
        if (this.placement == null) {
            return false;
        }

        PlacementLine line = KnownPlacementRows.placementLine(this, this.context, this.placement.getName(), KnownPlacementRows.contentRight(this, this.buttonsStartX));
        return line.nameHovered(this, mouseX, mouseY);
    }
}
