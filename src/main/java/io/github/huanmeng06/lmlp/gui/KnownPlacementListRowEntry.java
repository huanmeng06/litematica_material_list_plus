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
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.KnownPlacementRow;
import net.minecraft.class_332;

import java.io.File;
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
        int buttonX = this.x + this.width - 2;
        int buttonY = KnownPlacementRows.buttonY(this.y);
        buttonX = this.addRemoveButton(buttonX, buttonY, context);
        if (context.canEdit() && context.placement() != null) {
            buttonX = this.addToggleButton(buttonX, buttonY, context.placement());
            buttonX = this.addConfigureButton(buttonX, buttonY, context.placement());
        }

        this.buttonsStartX = buttonX;
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
        String labelKey = context.offlineCache() ? "lmlp.gui.button.delete_cache" : "litematica.gui.button.schematic_placements.remove";
        ButtonGeneric button = new ButtonGeneric(buttonX, buttonY, -1, true, StringUtils.translate(labelKey));
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

        if (this.row.isHeader()) {
            KnownPlacementRows.toggle(this.row.pageId(), this.row.dimension());
            this.parent.refreshEntries();
            return true;
        }

        if (this.row.isPlacement() && this.isPlacementNameHovered(mouseX, mouseY)) {
            KnownPlacementContext context = this.row.context();
            return MaterialListOpener.openContext(context.key(), "schematic_placements_list.name_click");
        }

        if (this.row.isPlacement() && mouseX < this.buttonsStartX) {
            KnownPlacementContext context = this.row.context();
            ChunkMissingMaterialListCache.selectMaterialListContext(context.key(), "schematic_placements_list.row_click");
            if (context.canEdit() && context.placement() != null) {
                SchematicPlacement selected = this.parent.getParentGui().manager.getSelectedSchematicPlacement();
                this.parent.getParentGui().manager.setSelectedSchematicPlacement(context.placement() != selected ? context.placement() : null);
            }
            this.parent.refreshEntries();
            return true;
        }

        return false;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        if (this.row == null) {
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

        if (context.selected()) {
            KnownPlacementRows.renderSelectedOutline(this);
        }

        int textY = KnownPlacementRows.textY(this);
        String color = context.placement() == null
                ? GuiBase.TXT_GRAY
                : context.placement().isEnabled() ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        boolean nameHovered = this.isPlacementNameHovered(mouseX, mouseY);
        if (nameHovered) {
            ClickableCursor.requestHand();
        }
        KnownPlacementRows.renderPlacementIcon(this, this.zLevel, drawContext);
        this.drawString(this.x + KnownPlacementRows.PLACEMENT_INDENT, textY, 0xFFFFFFFF,
                color + (nameHovered ? KnownPlacementRows.UNDERLINE : "") + context.name() + GuiBase.TXT_RST, drawContext);

        if (context.offlineCache()) {
            this.drawString(this.x + KnownPlacementRows.STATUS_X, textY, 0xFFFFAA66, StringUtils.translate("lmlp.gui.known_placement.offline_cache"), drawContext);
        } else if (!context.canEdit()) {
            this.drawString(this.x + KnownPlacementRows.STATUS_X, textY, 0xFFAAAAAA, StringUtils.translate("lmlp.gui.known_placement.cache_only"), drawContext);
        }

        this.drawSubWidgets(mouseX, mouseY, drawContext);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        if (this.row == null || !this.isMouseOver(mouseX, mouseY)) {
            return;
        }

        List<String> lines = new ArrayList<>();
        if (this.row.isHeader()) {
            lines.add(this.row.displayName());
        } else if (mouseX < this.buttonsStartX) {
            KnownPlacementContext context = this.row.context();
            lines.add(StringUtils.translate("lmlp.gui.known_placement.dimension", KnownPlacementRows.displayName(context.dimension())));
            lines.add(StringUtils.translate("lmlp.gui.known_placement.schematic", this.schematicName(context)));
            if (context.offlineCache()) {
                KnownPlacementRows.addTranslatedTooltipLines(lines, "lmlp.gui.known_placement.offline_cache_hint");
                lines.add(StringUtils.translate("lmlp.gui.known_placement.offline_cache_delete_hint"));
                if (context.schematicMissing()) {
                    lines.add(StringUtils.translate("lmlp.gui.known_placement.schematic_missing"));
                }
            } else if (!context.canEdit()) {
                lines.add(StringUtils.translate("lmlp.gui.known_placement.cache_only_hint"));
            }
        }

        if (!lines.isEmpty()) {
            RenderUtils.drawHoverText(mouseX, mouseY, lines, drawContext);
        }

        this.drawHoveredSubWidget(mouseX, mouseY, drawContext);
    }

    private String schematicName(KnownPlacementContext context) {
        if (context == null || context.schematicPath().isEmpty()) {
            return context != null && !context.schematicName().isEmpty()
                    ? context.schematicName()
                    : StringUtils.translate("litematica.gui.label.schematic_placement.in_memory");
        }

        return new File(context.schematicPath()).getName();
    }

    private boolean isPlacementNameHovered(int mouseX, int mouseY) {
        if (this.row == null || !this.row.isPlacement() || this.row.context() == null) {
            return false;
        }

        return KnownPlacementRows.isPlacementNameHovered(this, this.getStringWidth(this.row.context().name()), mouseX, mouseY);
    }
}
