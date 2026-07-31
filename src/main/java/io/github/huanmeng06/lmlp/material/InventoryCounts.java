package io.github.huanmeng06.lmlp.material;

import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.malilib.util.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.class_1799;
import net.minecraft.class_310;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class InventoryCounts {
    private static final long SNAPSHOT_MAX_AGE_NANOS = 50_000_000L;
    private static Snapshot cachedSnapshot;
    private static long lastCaptureNanos;

    private InventoryCounts() {
    }

    public static Snapshot current() {
        Snapshot snapshot = cachedSnapshot;
        long now = System.nanoTime();
        if (snapshot == null || now - lastCaptureNanos >= SNAPSHOT_MAX_AGE_NANOS) {
            snapshot = captureAndPublish(now);
        }
        return snapshot;
    }

    public static void refresh() {
        captureAndPublish(System.nanoTime());
    }

    public static void clear() {
        cachedSnapshot = null;
        lastCaptureNanos = 0L;
    }

    private static Snapshot captureAndPublish(long capturedAtNanos) {
        Snapshot snapshot = capture();
        cachedSnapshot = snapshot;
        lastCaptureNanos = capturedAtNanos;
        WaterBucketIceSubstitution.refreshAvailableCounts(snapshot);
        return snapshot;
    }

    private static Snapshot capture() {
        try {
            class_310 client = class_310.method_1551();
            if (client == null || client.field_1724 == null) {
                return Snapshot.EMPTY;
            }

            return new Snapshot(MaterialListUtils.getInventoryItemCounts(client.field_1724.method_31548()));
        } catch (Throwable throwable) {
            return Snapshot.EMPTY;
        }
    }

    public record Snapshot(Object2IntOpenHashMap<ItemType> counts, String signature) {
        private static final Snapshot EMPTY = new Snapshot(new Object2IntOpenHashMap<>());

        private Snapshot(Object2IntOpenHashMap<ItemType> counts) {
            this(counts, buildSignature(counts));
        }

        public int count(class_1799 stack) {
            if (stack.method_7960()) {
                return 0;
            }

            return this.counts.getInt(new ItemType(stack, true, false));
        }

        public int countAny(List<class_1799> stacks) {
            int total = 0;
            Set<String> seen = new HashSet<>();
            for (class_1799 stack : stacks) {
                if (!stack.method_7960() && seen.add(ItemStackTexts.id(stack))) {
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
