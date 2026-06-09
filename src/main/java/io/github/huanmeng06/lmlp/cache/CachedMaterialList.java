package io.github.huanmeng06.lmlp.cache;

import java.util.List;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.class_310;

public class CachedMaterialList extends MaterialListBase {
    private final PlacementMaterialListSnapshot snapshot;

    CachedMaterialList(PlacementMaterialListSnapshot snapshot) {
        this.snapshot = snapshot;
        this.fromJson(snapshot.options());
        this.setMaterialListEntries(snapshot.createEntries());
        this.refreshAvailableCounts();
    }

    public SchematicPlacement placement() {
        return this.snapshot.placement();
    }

    public boolean matchesCurrentPlacementState() {
        return this.snapshot.matchesCurrentState();
    }

    public boolean canRefreshLive() {
        return this.matchesCurrentPlacementState() && PlacementMaterialListCache.arePlacementChunksLoaded(this.snapshot.placement());
    }

    @Override
    public String getName() {
        return this.snapshot.name();
    }

    @Override
    public String getTitle() {
        return this.snapshot.title();
    }

    @Override
    public boolean supportsRenderLayers() {
        return false;
    }

    @Override
    public void reCreateMaterialList() {
        if (!this.matchesCurrentPlacementState()) {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "lmlp.message.material_list_cache.no_cache_unloaded");
            return;
        }

        if (this.canRefreshLive()) {
            PlacementMaterialListCache.refreshLive(this.snapshot.placement(), this);
            return;
        }

        this.refreshAvailableCounts();
        InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "lmlp.message.material_list_cache.refresh_requires_loaded_chunks");
    }

    private void refreshAvailableCounts() {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 != null) {
            List<MaterialListEntry> entries = this.getMaterialsAll();
            MaterialListUtils.updateAvailableCounts(entries, mc.field_1724);
            this.refreshPreFilteredList();
            this.recreateFilteredList();
            this.updateCounts();
        }
    }
}
