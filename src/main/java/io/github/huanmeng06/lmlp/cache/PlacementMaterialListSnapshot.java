package io.github.huanmeng06.lmlp.cache;

import java.util.List;

import com.google.gson.JsonObject;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

record PlacementMaterialListSnapshot(
        SchematicPlacement placement,
        String name,
        String title,
        JsonObject options,
        List<MaterialListEntry> entries) {
    static PlacementMaterialListSnapshot from(SchematicPlacement placement, MaterialListBase materialList) {
        return new PlacementMaterialListSnapshot(
                placement,
                materialList.getName(),
                materialList.getTitle(),
                PlacementMaterialListCache.copyOptions(materialList),
                PlacementMaterialListCache.copyEntries(materialList.getMaterialsAll()));
    }

    List<MaterialListEntry> createEntries() {
        return PlacementMaterialListCache.copyEntries(this.entries);
    }
}
