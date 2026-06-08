package io.github.huanmeng06.lmlp.material;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;

public final class MaterialCounts {
    private MaterialCounts() {
    }

    public static int total(MaterialListEntry entry, MaterialListBase materialList) {
        return multiplyClamped(entry.getCountTotal(), materialList.getMultiplier());
    }

    public static int missing(MaterialListEntry entry, MaterialListBase materialList) {
        int total = total(entry, materialList);
        return materialList.getMultiplier() > 1 ? total : entry.getCountMissing();
    }

    public static int netMissing(MaterialListEntry entry, MaterialListBase materialList) {
        return netMissing(entry, materialList.getMultiplier());
    }

    public static int netMissing(MaterialListEntry entry, int multiplier) {
        long total = (long) entry.getCountTotal() * Math.max(1, multiplier);
        long missing = multiplier > 1 ? total : entry.getCountMissing();
        return clampToInt(Math.max(0L, missing - entry.getCountAvailable()));
    }

    private static int multiplyClamped(int count, int multiplier) {
        return clampToInt((long) count * Math.max(1, multiplier));
    }

    private static int clampToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }
}
