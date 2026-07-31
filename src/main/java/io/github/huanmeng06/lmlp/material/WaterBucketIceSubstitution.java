package io.github.huanmeng06.lmlp.material;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
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
    private static final List<WeakReference<MaterialListEntry>> SUBSTITUTED_ICE_ENTRIES = new ArrayList<>();

    private WaterBucketIceSubstitution() {
    }

    public static List<MaterialListEntry> apply(List<MaterialListEntry> entries) {
        if (entries == null || entries.isEmpty() || !Configs.Generic.REPLACE_WATER_BUCKET_WITH_ICE.getBooleanValue()) {
            return entries;
        }

        int waterBucketTotal = 0;
        int waterBucketMissing = 0;
        int waterBucketMismatched = 0;
        int iceTotal = 0;
        int iceMissing = 0;
        int iceMismatched = 0;
        boolean hasWaterBucket = false;

        for (MaterialListEntry entry : entries) {
            String id = ItemStackTexts.id(entry.getStack());
            if (WATER_BUCKET_ID.equals(id)) {
                hasWaterBucket = true;
                waterBucketTotal += entry.getCountTotal();
                waterBucketMissing += entry.getCountMissing();
                waterBucketMismatched += entry.getCountMismatched();
            } else if (ICE_ID.equals(id)) {
                iceTotal += entry.getCountTotal();
                iceMissing += entry.getCountMissing();
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
        int missing = waterBucketMissing + iceMissing;
        int mismatched = waterBucketMismatched + iceMismatched;
        int available = InventoryCounts.current().countAny(List.of(iceStack));
        MaterialListEntry merged = new MaterialListEntry(iceStack, total, missing, mismatched, available);
        track(merged);

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

    public static void refreshAvailableCounts(InventoryCounts.Snapshot inventory) {
        class_1799 iceStack = iceStack();
        if (iceStack == null) {
            return;
        }

        int inventoryCount = inventory.countAny(List.of(iceStack));
        Iterator<WeakReference<MaterialListEntry>> iterator = SUBSTITUTED_ICE_ENTRIES.iterator();
        while (iterator.hasNext()) {
            MaterialListEntry entry = iterator.next().get();
            if (entry == null) {
                iterator.remove();
            } else {
                entry.setCountAvailable(inventoryCount);
            }
        }
    }

    /**
     * Returns the live inventory count for an ice row. Ice is a normal inventory item;
     * water-bucket substitution must not cap its visible count at the schematic demand.
     */
    public static int availableForDisplay(MaterialListEntry entry) {
        if (entry == null || !ICE_ID.equals(ItemStackTexts.id(entry.getStack()))) {
            return entry == null ? 0 : entry.getCountAvailable();
        }

        class_1799 iceStack = iceStack();
        return iceStack == null ? entry.getCountAvailable() : InventoryCounts.current().count(iceStack);
    }

    private static void track(MaterialListEntry entry) {
        SUBSTITUTED_ICE_ENTRIES.removeIf(reference -> reference.get() == null);
        SUBSTITUTED_ICE_ENTRIES.add(new WeakReference<>(entry));
    }

    private static class_1799 iceStack() {
        try {
            class_1792 item = class_7923.field_41178.method_10223(new class_2960(ICE_ID));
            return item == null ? null : new class_1799(item, 1);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
