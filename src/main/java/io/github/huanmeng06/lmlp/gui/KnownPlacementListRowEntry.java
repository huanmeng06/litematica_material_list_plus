package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicPlacements;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.interfaces.IConfirmationListener;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.ColumnLayout;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.PlacementLine;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.KnownPlacementRow;
import net.minecraft.class_437;
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
        if (KnownPlacementRows.shouldShowOfflineMissingButton(context)) {
            ColumnLayout columns = KnownPlacementRows.computeColumns(this, this.row.pageId());
            this.addOfflineMissingButton(columns, KnownPlacementRows.buttonY(this.y), context);
            this.buttonsStartX = columns.actionX();
            return;
        }

        if (!canModifyPlacement(context)) {
            return;
        }

        ColumnLayout columns = KnownPlacementRows.computeColumns(this, this.row.pageId());
        int buttonX = columns.contentRight();
        int buttonY = KnownPlacementRows.buttonY(this.y);
        buttonX = this.addRemoveButton(buttonX, buttonY, context);
        buttonX = this.addToggleButton(buttonX, buttonY, context.placement());
        buttonX = this.addConfigureButton(buttonX, buttonY, context.placement());

        this.buttonsStartX = columns.actionX();
    }

    private static boolean canModifyPlacement(KnownPlacementContext context) {
        return context != null && context.canEdit() && context.placement() != null;
    }

    private int addConfigureButton(int buttonX, int buttonY, SchematicPlacement placement) {
        String label = StringUtils.translate("litematica.gui.button.schematic_placements.configure");
        ButtonGeneric button = new ButtonGeneric(buttonX, buttonY, KnownPlacementRows.configureButtonWidth(this), true, label);
        this.addButton(button, (clickedButton, mouseButton) -> {
            GuiPlacementConfiguration gui = new GuiPlacementConfiguration(placement);
            gui.setParent(this.parent.getParentGui());
            GuiBase.openGui(gui);
        });
        return button.getX() - KnownPlacementRows.buttonGap();
    }

    private int addToggleButton(int buttonX, int buttonY, SchematicPlacement placement) {
        ButtonOnOff button = new ButtonOnOff(
                buttonX,
                buttonY,
                KnownPlacementRows.toggleButtonWidth(this),
                true,
                "litematica.gui.button.schematic_placements.placement_enabled",
                placement.isEnabled());
        this.addButton(button, (clickedButton, mouseButton) -> {
            placement.toggleEnabled();
            ChunkMissingMaterialListCache.rememberPlacementContext(placement, "known_placement.toggle_enabled");
            this.parent.refreshEntries();
        });
        return button.getX() - KnownPlacementRows.buttonGap();
    }

    private int addRemoveButton(int buttonX, int buttonY, KnownPlacementContext context) {
        ButtonGeneric button = new ButtonGeneric(
                buttonX,
                buttonY,
                KnownPlacementRows.removeButtonWidth(this),
                true,
                StringUtils.translate("litematica.gui.button.schematic_placements.remove"));
        this.addButton(button, (clickedButton, mouseButton) -> {
            boolean allowCurrentDimensionRemoval = context.canEdit();
            if (ChunkMissingMaterialListCache.removeKnownPlacementContext(context.key(), allowCurrentDimensionRemoval, "known_placement.remove_button")) {
                this.parent.refreshEntries();
            }
        });
        return button.getX() - KnownPlacementRows.buttonGap();
    }

    private void addOfflineMissingButton(ColumnLayout columns, int buttonY, KnownPlacementContext context) {
        ButtonGeneric button = new ButtonGeneric(
                columns.actionX(),
                buttonY,
                columns.actionWidth(),
                false,
                "lmlp.gui.button.offline_cache_not_found");
        this.addButton(button, (clickedButton, mouseButton) -> this.openOfflineMissingConfirm(context));
    }

    private void openOfflineMissingConfirm(KnownPlacementContext context) {
        OfflineCacheMissingConfirmGui gui = new OfflineCacheMissingConfirmGui(context, new IConfirmationListener() {
            @Override
            public boolean onActionConfirmed() {
                if (ChunkMissingMaterialListCache.removeKnownPlacementContext(context.key(), false, "known_placement.offline_cache_not_found")) {
                    KnownPlacementListRowEntry.this.parent.refreshEntries();
                }
                return true;
            }

            @Override
            public boolean onActionCancelled() {
                return true;
            }
        }, this.parent.getParentGui());
        GuiBase.openGui(gui);
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

        if (this.row.isPlacement()) {
            KnownPlacementContext context = this.row.context();
            PlacementLine line = KnownPlacementRows.placementLine(this, context, context.name(), this.row.pageId());
            if (PlacementOriginMarker.originHovered(context, line, this, mouseX, mouseY)) {
                return PlacementOriginMarker.handleOriginClick(context);
            }
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
        PlacementLine line = KnownPlacementRows.placementLine(this, context, context.name(), this.row.pageId());
        boolean nameHovered = line.nameHovered(this, mouseX, mouseY);
        boolean originHovered = PlacementOriginMarker.originHovered(context, line, this, mouseX, mouseY);
        if (nameHovered || originHovered) {
            ClickableCursor.requestHand();
        }
        KnownPlacementRows.renderPlacementLine(this, this.zLevel, drawContext, line, color, nameHovered, context, originHovered);

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
            PlacementLine line = KnownPlacementRows.placementLine(this, context, context.name(), this.row.pageId());
            List<String> lines = new ArrayList<>();
            if (line.statusHovered(this, mouseX, mouseY)) {
                lines.addAll(line.status().tooltipLines());
            } else if (line.fileHovered(this, mouseX, mouseY) && !line.fileHoverText().isEmpty()) {
                lines.add(line.fileHoverText());
            } else if (PlacementOriginMarker.originHovered(context, line, this, mouseX, mouseY)) {
                lines.add(StringUtils.translate(PlacementOriginMarker.hasHighlight(context)
                        ? "lmlp.gui.known_placement.origin_beam_hint"
                        : "lmlp.gui.known_placement.origin_highlight_hint"));
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
        PlacementLine line = KnownPlacementRows.placementLine(this, context, context.name(), this.row.pageId());
        return line.nameHovered(this, mouseX, mouseY);
    }
}

class OfflineCacheMissingConfirmGui extends GuiConfirmAction {
    OfflineCacheMissingConfirmGui(KnownPlacementContext context, IConfirmationListener listener, class_437 parent) {
        super(
                360,
                "lmlp.gui.title.offline_cache_not_found",
                listener,
                parent,
                "lmlp.gui.confirm.offline_cache_not_found",
                GuiBase.TXT_BOLD + KnownPlacementRows.displayName(context.dimension()) + GuiBase.TXT_RST,
                GuiBase.TXT_BOLD + originText(context) + GuiBase.TXT_RST);
    }

    @Override
    protected int getButtonWidth() {
        int confirmWidth = this.getStringWidth(StringUtils.translate("lmlp.gui.button.confirm_offline_cache_not_found")) + 10;
        int cancelWidth = this.getStringWidth(StringUtils.translate("lmlp.gui.button.keep_looking")) + 10;
        return Math.max(confirmWidth, cancelWidth);
    }

    @Override
    protected void createButton(int x, int y, int width, ButtonType type) {
        String labelKey = type == ButtonType.OK
                ? "lmlp.gui.button.confirm_offline_cache_not_found"
                : "lmlp.gui.button.keep_looking";
        String color = type == ButtonType.OK ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, color + StringUtils.translate(labelKey) + GuiBase.TXT_RST);
        this.addButton(button, this.createActionListener(type));
    }

    private static String originText(KnownPlacementContext context) {
        if (context == null || context.originPosition() == null || context.originPosition().isEmpty()) {
            return StringUtils.translate("lmlp.gui.known_placement.origin_unknown");
        }

        return context.originPosition();
    }
}
