package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.gui.widgets.WidgetListLoadedSchematics;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicEntry;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.KnownPlacementRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KnownLoadedSchematicsList extends WidgetListLoadedSchematics {
    private static final String PAGE_ID = "loaded_schematics";

    private final List<KnownPlacementRow> visibleRows = new ArrayList<>();

    public KnownLoadedSchematicsList(int x, int y, int width, int height) {
        super(x, y, width, height, null);
    }

    @Override
    protected Collection<LitematicaSchematic> getAllEntries() {
        List<LitematicaSchematic> schematics = new ArrayList<>();
        for (KnownPlacementRow row : KnownPlacementRows.rows(PAGE_ID)) {
            schematics.add(row.isPlacement() ? ChunkMissingMaterialListCache.schematicForPlacement(row.context().placement()) : null);
        }
        return schematics;
    }

    @Override
    protected void refreshBrowserEntries() {
        this.listContents.clear();
        this.visibleRows.clear();

        String filter = this.getFilterText();
        for (KnownPlacementRow row : KnownPlacementRows.rows(PAGE_ID)) {
            if (!filter.isEmpty() && !this.matchesFilter(KnownPlacementRows.filterStrings(row), filter)) {
                continue;
            }

            this.visibleRows.add(row);
            this.listContents.add(row.isPlacement() ? ChunkMissingMaterialListCache.schematicForPlacement(row.context().placement()) : null);
        }

        this.reCreateListEntryWidgets();
    }

    @Override
    protected List<String> getEntryStringsForFilter(LitematicaSchematic schematic) {
        return List.of();
    }

    @Override
    protected int getBrowserEntryHeightFor(LitematicaSchematic schematic) {
        return KnownPlacementRows.ROW_HEIGHT;
    }

    @Override
    protected WidgetSchematicEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, LitematicaSchematic schematic) {
        KnownPlacementRow row = listIndex >= 0 && listIndex < this.visibleRows.size() ? this.visibleRows.get(listIndex) : null;
        return new KnownLoadedSchematicEntry(x, y, this.browserEntryWidth, isOdd, schematic, row, listIndex, this);
    }

    @Override
    protected boolean onEntryClicked(LitematicaSchematic schematic, int index) {
        if (index < 0 || index >= this.visibleRows.size()) {
            return true;
        }

        KnownPlacementRow row = this.visibleRows.get(index);
        if (row.isHeader()) {
            KnownPlacementRows.toggle(PAGE_ID, row.dimension());
        } else if (row.isPlacement()) {
            KnownPlacementContext context = row.context();
            ChunkMissingMaterialListCache.selectMaterialListContext(context.key(), "loaded_schematics_list.click");
        }
        this.refreshEntries();
        return true;
    }
}
