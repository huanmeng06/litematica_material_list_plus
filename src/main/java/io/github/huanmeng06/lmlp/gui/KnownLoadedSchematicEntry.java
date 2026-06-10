package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.gui.widgets.WidgetListLoadedSchematics;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicEntry;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.KnownPlacementRow;
import net.minecraft.class_332;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KnownLoadedSchematicEntry extends WidgetSchematicEntry {
    private final KnownPlacementRow row;
    private final boolean isOdd;

    public KnownLoadedSchematicEntry(
            int x,
            int y,
            int width,
            boolean isOdd,
            LitematicaSchematic schematic,
            KnownPlacementRow row,
            int listIndex,
            WidgetListLoadedSchematics parent) {
        super(x, y, width, KnownPlacementRows.ROW_HEIGHT, isOdd, schematic, listIndex, parent);
        this.row = row;
        this.isOdd = isOdd;
        this.subWidgets.clear();
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
        this.drawString(this.x + KnownPlacementRows.PLACEMENT_INDENT, this.y + 8, 0xFFFFFFFF, name, drawContext);

        if (context != null && context.offlineCache()) {
            this.drawString(this.x + 180, this.y + 8, 0xFFFFAA66, StringUtils.translate("lmlp.gui.known_placement.offline_cache"), drawContext);
        } else if (context != null && !context.canEdit()) {
            this.drawString(this.x + 180, this.y + 8, 0xFFAAAAAA, StringUtils.translate("lmlp.gui.known_placement.cache_only"), drawContext);
        }
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        if (this.row == null || !this.isMouseOver(mouseX, mouseY)) {
            return;
        }

        List<String> lines = new ArrayList<>();
        if (this.row.isHeader()) {
            lines.add(this.row.displayName());
            lines.add(this.row.dimension());
        } else {
            KnownPlacementContext context = this.row.context();
            lines.add(StringUtils.translate("lmlp.gui.known_placement.dimension", context == null ? "?" : KnownPlacementRows.displayName(context.dimension())));
            lines.add(StringUtils.translate("lmlp.gui.known_placement.schematic", this.schematicName(context)));
            if (context != null && context.offlineCache()) {
                lines.add(StringUtils.translate("lmlp.gui.known_placement.offline_cache_hint"));
                if (context.schematicMissing()) {
                    lines.add(StringUtils.translate("lmlp.gui.known_placement.schematic_missing"));
                }
                if (!context.hasMaterialCache()) {
                    lines.add(StringUtils.translate("lmlp.gui.known_placement.offline_cache_empty"));
                }
            } else if (context != null && !context.canEdit()) {
                lines.add(StringUtils.translate("lmlp.gui.known_placement.cache_only_hint"));
            }
        }

        RenderUtils.drawHoverText(mouseX, mouseY, lines, drawContext);
    }

    private String schematicName(KnownPlacementContext context) {
        if (context == null || context.schematicPath().isEmpty()) {
            return context != null && !context.schematicName().isEmpty()
                    ? context.schematicName()
                    : StringUtils.translate("litematica.gui.label.schematic_placement.in_memory");
        }

        return new File(context.schematicPath()).getName();
    }
}
