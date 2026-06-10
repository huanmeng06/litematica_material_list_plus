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
import net.minecraft.class_332;

import java.io.File;
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
        int buttonX = this.x + this.width - 2;
        int buttonY = KnownPlacementRows.buttonY(this.y);
        buttonX = this.addRemoveButton(buttonX, buttonY);
        if (this.context != null && this.context.canEdit()) {
            buttonX = this.addToggleButton(buttonX, buttonY);
            buttonX = this.addConfigureButton(buttonX, buttonY);
        }

        this.buttonsStartX = buttonX;
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
    public void render(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        boolean materialSelected = this.context != null && this.context.selected();
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
        String name = enabledColor + this.placement.getName() + GuiBase.TXT_RST;
        int textY = KnownPlacementRows.textY(this);
        this.drawString(this.x + KnownPlacementRows.PLACEMENT_INDENT, textY, 0xFFFFFFFF, name, drawContext);

        if (this.context != null && !this.context.canEdit()) {
            this.drawString(this.x + KnownPlacementRows.STATUS_X, textY, 0xFFAAAAAA, StringUtils.translate("lmlp.gui.known_placement.cache_only"), drawContext);
        }

        this.drawSubWidgets(mouseX, mouseY, drawContext);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        if (this.isMouseOver(mouseX, mouseY) && mouseX < this.buttonsStartX) {
            List<String> lines = new ArrayList<>();
            lines.add(StringUtils.translate("lmlp.gui.known_placement.dimension", this.context == null ? "?" : KnownPlacementRows.displayName(this.context.dimension())));
            lines.add(StringUtils.translate("lmlp.gui.known_placement.schematic", this.schematicName()));
            if (this.context != null && !this.context.canEdit()) {
                lines.add(StringUtils.translate("lmlp.gui.known_placement.cache_only_hint"));
            }
            RenderUtils.drawHoverText(mouseX, mouseY, lines, drawContext);
        }

        super.postRenderHovered(mouseX, mouseY, selected, drawContext);
    }

    private String schematicName() {
        if (this.context == null || this.context.schematicPath().isEmpty()) {
            return StringUtils.translate("litematica.gui.label.schematic_placement.in_memory");
        }

        return new File(this.context.schematicPath()).getName();
    }
}
