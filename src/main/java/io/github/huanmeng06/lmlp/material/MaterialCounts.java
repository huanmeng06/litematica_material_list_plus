package io.github.huanmeng06.lmlp.material;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;

public final class MaterialCounts {
    private MaterialCounts() {
    }

    public static int total(MaterialListEntry entry, MaterialListBase materialList) {
        return entry.getCountTotal() * materialList.getMultiplier();
    }

    public static int missing(MaterialListEntry entry, MaterialListBase materialList) {
        int total = total(entry, materialList);
        return materialList.getMultiplier() > 1 ? total : entry.getCountMissing();
    }
}
