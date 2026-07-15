package io.github.huanmeng06.lmlp.material;

import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.malilib.util.data.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class InventoryCounts {
    private static Snapshot cachedSnapshot;

    private InventoryCounts() {
    }

    public static Snapshot current() {
        Snapshot snapshot = cachedSnapshot;
        if (snapshot == null) {
            snapshot = capture();
            cachedSnapshot = snapshot;
        }
        return snapshot;
    }

    public static void refresh() {
        cachedSnapshot = capture();
    }

    public static void clear() {
        cachedSnapshot = null;
    }

    private static Snapshot capture() {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.player == null) {
                return Snapshot.EMPTY;
            }

            return new Snapshot(MaterialListUtils.getInventoryItemCounts(client.player.getInventory()));
        } catch (Throwable throwable) {
            return Snapshot.EMPTY;
        }
    }

    public record Snapshot(Object2IntOpenHashMap<ItemType> counts, String signature) {
        private static final Snapshot EMPTY = new Snapshot(new Object2IntOpenHashMap<>());

        private Snapshot(Object2IntOpenHashMap<ItemType> counts) {
            this(counts, buildSignature(counts));
        }

        public int count(ItemStack stack) {
            if (stack.isEmpty()) {
                return 0;
            }

            return this.counts.getInt(new ItemType(stack, true, false));
        }

        public int countAny(List<ItemStack> stacks) {
            int total = 0;
            Set<String> seen = new HashSet<>();
            for (ItemStack stack : stacks) {
                if (!stack.isEmpty() && seen.add(ItemStackTexts.id(stack))) {
                    total += this.count(stack);
                }
            }
            return total;
        }

        private static String buildSignature(Object2IntOpenHashMap<ItemType> counts) {
            if (counts.isEmpty()) {
                return "";
            }

            List<String> parts = new ArrayList<>();
            for (ItemType type : counts.keySet()) {
                int count = counts.getInt(type);
                if (count > 0) {
                    parts.add(type.toString() + '=' + count);
                }
            }

            Collections.sort(parts);
            return String.join("|", parts);
        }
    }
}
