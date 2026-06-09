package io.github.huanmeng06.lmlp.cache;

import java.util.List;

import com.google.gson.JsonObject;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

record PlacementMaterialListSnapshot(
        SchematicPlacement placement,
        String signature,
        String name,
        String title,
        JsonObject options,
        List<MaterialListEntry> entries) {
    static PlacementMaterialListSnapshot from(SchematicPlacement placement, MaterialListBase materialList) {
        return new PlacementMaterialListSnapshot(
                placement,
                PlacementMaterialListCache.signature(placement),
                materialList.getName(),
                materialList.getTitle(),
                PlacementMaterialListCache.copyOptions(materialList),
                PlacementMaterialListCache.copyEntries(materialList.getMaterialsAll()));
    }

    boolean matchesCurrentState() {
        return this.signature.equals(PlacementMaterialListCache.signature(this.placement));
    }

    List<MaterialListEntry> createEntries() {
        return PlacementMaterialListCache.copyEntries(this.entries);
    }
}
