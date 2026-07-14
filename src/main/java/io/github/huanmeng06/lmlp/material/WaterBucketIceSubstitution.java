package io.github.huanmeng06.lmlp.material;

import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.litematica.materials.MaterialListEntry;
import io.github.huanmeng06.lmlp.config.Configs;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

public final class WaterBucketIceSubstitution {
    private static final String WATER_BUCKET_ID = "minecraft:water_bucket";
    private static final String ICE_ID = "minecraft:ice";

    private WaterBucketIceSubstitution() {
    }

    public static List<MaterialListEntry> apply(List<MaterialListEntry> entries) {
        if (entries == null || entries.isEmpty() || !Configs.Generic.REPLACE_WATER_BUCKET_WITH_ICE.getBooleanValue()) {
            return entries;
        }

        int waterBucketTotal = 0;
        int waterBucketMismatched = 0;
        int iceTotal = 0;
        int iceMismatched = 0;
        boolean hasWaterBucket = false;

        for (MaterialListEntry entry : entries) {
            String id = ItemStackTexts.id(entry.getStack());
            if (WATER_BUCKET_ID.equals(id)) {
                hasWaterBucket = true;
                waterBucketTotal += entry.getCountTotal();
                waterBucketMismatched += entry.getCountMismatched();
            } else if (ICE_ID.equals(id)) {
                iceTotal += entry.getCountTotal();
                iceMismatched += entry.getCountMismatched();
            }
        }

        if (!hasWaterBucket) {
            return entries;
        }

        class_1799 iceStack = iceStack();
        if (iceStack == null) {
            return entries;
        }

        int total = waterBucketTotal + iceTotal;
        int mismatched = waterBucketMismatched + iceMismatched;
        int available = Math.min(total, InventoryCounts.current().countAny(List.of(iceStack)));
        int missing = Math.max(0, total - available);
        MaterialListEntry merged = new MaterialListEntry(iceStack, total, missing, mismatched, available);

        List<MaterialListEntry> result = new ArrayList<>(entries.size());
        boolean inserted = false;
        for (MaterialListEntry entry : entries) {
            String id = ItemStackTexts.id(entry.getStack());
            if (WATER_BUCKET_ID.equals(id) || ICE_ID.equals(id)) {
                if (!inserted) {
                    result.add(merged);
                    inserted = true;
                }
            } else {
                result.add(entry);
            }
        }

        return result;
    }

    private static class_1799 iceStack() {
        class_1792 item = class_7923.field_41178.method_17966(class_2960.method_60654(ICE_ID)).orElse(null);
        return item == null ? null : new class_1799(item, 1);
    }
}
