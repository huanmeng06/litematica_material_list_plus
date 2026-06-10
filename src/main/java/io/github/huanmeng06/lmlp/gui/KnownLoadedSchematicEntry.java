package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.gui.widgets.WidgetListLoadedSchematics;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicEntry;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import net.minecraft.class_332;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KnownLoadedSchematicEntry extends WidgetSchematicEntry {
    private static final int ROW_HEIGHT = 26;

    private final KnownPlacementContext context;
    private final boolean isOdd;

    public KnownLoadedSchematicEntry(
            int x,
            int y,
            int width,
            boolean isOdd,
            LitematicaSchematic schematic,
            KnownPlacementContext context,
            int listIndex,
            WidgetListLoadedSchematics parent) {
        super(x, y, width, ROW_HEIGHT, isOdd, schematic, listIndex, parent);
        this.context = context;
        this.isOdd = isOdd;
        this.subWidgets.clear();
    }

    public static int rowHeight() {
        return ROW_HEIGHT;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        boolean materialSelected = this.context != null && this.context.selected();
        if (this.isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0707070);
        } else if (this.isOdd) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0101010);
        } else {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0303030);
        }

        if (materialSelected) {
            RenderUtils.drawOutline(this.x + 1, this.y + 1, this.width - 2, this.height - 2, 0xFFFFAA00);
        }

        String marker = materialSelected ? "[x]" : "[ ]";
        String name = this.context == null ? "" : this.context.name();
        this.drawString(this.x + 4, this.y + 8, 0xFFFFFFFF, marker, drawContext);
        this.drawString(this.x + 32, this.y + 8, 0xFFFFFFFF, name, drawContext);

        if (this.context != null && this.context.offlineCache()) {
            this.drawString(this.x + 190, this.y + 8, 0xFFFFAA66, StringUtils.translate("lmlp.gui.known_placement.offline_cache"), drawContext);
        } else if (this.context != null && !this.context.canEdit()) {
            this.drawString(this.x + 190, this.y + 8, 0xFFAAAAAA, StringUtils.translate("lmlp.gui.known_placement.cache_only"), drawContext);
        }
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        if (this.context == null || !this.isMouseOver(mouseX, mouseY)) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add(StringUtils.translate("lmlp.gui.known_placement.dimension", this.context.displayDimension()));
        lines.add(StringUtils.translate("lmlp.gui.known_placement.schematic", this.schematicName()));
        if (this.context.offlineCache()) {
            lines.add(StringUtils.translate("lmlp.gui.known_placement.offline_cache_hint"));
            if (this.context.schematicMissing()) {
                lines.add(StringUtils.translate("lmlp.gui.known_placement.schematic_missing"));
            }
            if (!this.context.hasMaterialCache()) {
                lines.add(StringUtils.translate("lmlp.gui.known_placement.offline_cache_empty"));
            }
        } else if (!this.context.canEdit()) {
            lines.add(StringUtils.translate("lmlp.gui.known_placement.cache_only_hint"));
        }

        RenderUtils.drawHoverText(mouseX, mouseY, lines, drawContext);
    }

    private String schematicName() {
        if (this.context == null || this.context.schematicPath().isEmpty()) {
            return this.context != null && !this.context.schematicName().isEmpty()
                    ? this.context.schematicName()
                    : StringUtils.translate("litematica.gui.label.schematic_placement.in_memory");
        }

        return new File(this.context.schematicPath()).getName();
    }
}