package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.gui.widgets.WidgetListLoadedSchematics;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicEntry;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KnownLoadedSchematicsList extends WidgetListLoadedSchematics {
    private final List<KnownPlacementContext> visibleContexts = new ArrayList<>();

    public KnownLoadedSchematicsList(int x, int y, int width, int height) {
        super(x, y, width, height, null);
    }

    @Override
    protected Collection<LitematicaSchematic> getAllEntries() {
        List<LitematicaSchematic> schematics = new ArrayList<>();
        for (KnownPlacementContext context : ChunkMissingMaterialListCache.knownPlacementContexts()) {
            schematics.add(ChunkMissingMaterialListCache.schematicForPlacement(context.placement()));
        }
        return schematics;
    }

    @Override
    protected void refreshBrowserEntries() {
        this.listContents.clear();
        this.visibleContexts.clear();

        String filter = this.getFilterText();
        for (KnownPlacementContext context : ChunkMissingMaterialListCache.knownPlacementContexts()) {
            if (!filter.isEmpty() && !this.matchesFilter(this.filterStrings(context), filter)) {
                continue;
            }

            this.visibleContexts.add(context);
            this.listContents.add(ChunkMissingMaterialListCache.schematicForPlacement(context.placement()));
        }

        this.reCreateListEntryWidgets();
    }

    @Override
    protected List<String> getEntryStringsForFilter(LitematicaSchematic schematic) {
        return List.of();
    }

    @Override
    protected int getBrowserEntryHeightFor(LitematicaSchematic schematic) {
        return KnownLoadedSchematicEntry.rowHeight();
    }

    @Override
    protected WidgetSchematicEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, LitematicaSchematic schematic) {
        KnownPlacementContext context = listIndex >= 0 && listIndex < this.visibleContexts.size() ? this.visibleContexts.get(listIndex) : null;
        return new KnownLoadedSchematicEntry(x, y, this.browserEntryWidth, isOdd, schematic, context, listIndex, this);
    }

    @Override
    protected boolean onEntryClicked(LitematicaSchematic schematic, int index) {
        if (index >= 0 && index < this.visibleContexts.size()) {
            KnownPlacementContext context = this.visibleContexts.get(index);
            ChunkMissingMaterialListCache.selectMaterialListContext(context.key(), "loaded_schematics_list.click");
            this.refreshEntries();
        }
        return true;
    }

    private List<String> filterStrings(KnownPlacementContext context) {
        List<String> strings = new ArrayList<>();
        strings.add(context.name().toLowerCase());
        strings.add(context.displayDimension().toLowerCase());
        strings.add(context.dimension() == null ? "" : context.dimension().toLowerCase());
        strings.add(context.schematicPath().toLowerCase());
        return strings;
    }
}