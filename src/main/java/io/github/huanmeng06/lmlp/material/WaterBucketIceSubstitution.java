package io.github.huanmeng06.lmlp.material;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import io.github.huanmeng06.lmlp.config.Configs;

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

        ItemStack iceStack = iceStack();
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

    private static ItemStack iceStack() {
        Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(ICE_ID)).orElse(null);
        return item == null ? null : new ItemStack(item, 1);
    }
}
