package io.github.huanmeng06.lmlp.material;

import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.CountDisplayStyle;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

public final class CountFormatter {
    private static final int STACKS_PER_SHULKER_BOX = 27;
    private static final int CACHE_LIMIT = 512;

    // Counts don't change every frame but the material list reformats them every
    // frame, so the same (count, stackSize, digits) pair produces the same
    // string repeatedly. Memoize the pure formatting to cut per-frame string /
    // StringBuilder / translate garbage. Style and language are NOT part of the
    // key; instead the cache is cleared when the display-style config changes
    // (Configs static block) and when the material list GUI (re)opens
    // (GuiMaterialListMixin#initGui) — a language switch forces a GUI reopen —
    // so a hit is always byte-identical to recomputing. Client-thread only.
    private static final Map<FormatKey, String> FORMAT_CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<FormatKey, String> eldest) {
            return size() > CACHE_LIMIT;
        }
    };
    private static final Map<CompactKey, String> COMPACT_CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CompactKey, String> eldest) {
            return size() > CACHE_LIMIT;
        }
    };

    private CountFormatter() {
    }

    public static void clearCache() {
        FORMAT_CACHE.clear();
        COMPACT_CACHE.clear();
    }

    public static String format(ItemStack stack, int count) {
        return format(stack, (long) count);
    }

    public static String format(ItemStack stack, long count) {
        return format(count, stack.getMaxStackSize());
    }

    public static String format(int count, int maxStackSize) {
        return format(count, maxStackSize, 0);
    }

    public static String format(long count, int maxStackSize) {
        return format(count, maxStackSize, 0);
    }

    public static String formatAligned(ItemStack stack, int count, int rawDigits) {
        return format(count, stack.getMaxStackSize(), rawDigits);
    }

    public static String format(int count, int maxStackSize, int rawDigits) {
        return format((long) count, maxStackSize, rawDigits);
    }

    public static String format(long count, int maxStackSize, int rawDigits) {
        FormatKey key = new FormatKey(count, maxStackSize, rawDigits);
        String cached = FORMAT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        String result = computeFormat(count, maxStackSize, rawDigits);
        FORMAT_CACHE.put(key, result);
        return result;
    }

    private static String computeFormat(long count, int maxStackSize, int rawDigits) {
        CountDisplayStyle style = (CountDisplayStyle) Configs.Generic.COUNT_DISPLAY_STYLE.getOptionListValue();
        String raw = rawDigits > 0 ? String.format("%" + rawDigits + "d", count) : Long.toString(count);
        if (style == CountDisplayStyle.STYLE_4) {
            return raw;
        }
        if (count == 0) {
            return countPart("lmlp.label.count.items", 0);
        }

        if (style == CountDisplayStyle.STYLE_2 && (count < 0 || maxStackSize <= 1 || count <= maxStackSize)) {
            return raw;
        }

        CountParts parts = CountParts.of(count, maxStackSize);
        if (style == CountDisplayStyle.STYLE_3) {
            return parts.formula();
        }

        String grouped = parts.grouped();
        if (style == CountDisplayStyle.STYLE_1) {
            return grouped;
        }

        return raw + " = " + grouped;
    }

    public static String compact(int count, int maxStackSize) {
        return compact((long) count, maxStackSize);
    }

    public static String compact(long count, int maxStackSize) {
        CompactKey key = new CompactKey(count, maxStackSize);
        String cached = COMPACT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        String result = computeCompact(count, maxStackSize);
        COMPACT_CACHE.put(key, result);
        return result;
    }

    private static String computeCompact(long count, int maxStackSize) {
        CountDisplayStyle style = (CountDisplayStyle) Configs.Generic.COUNT_DISPLAY_STYLE.getOptionListValue();
        if (count == 0 && style != CountDisplayStyle.STYLE_4) {
            return countPart("lmlp.label.count.items", 0);
        }
        if (style == CountDisplayStyle.STYLE_4 || (style == CountDisplayStyle.STYLE_2 && (count <= 0 || maxStackSize <= 1 || count <= maxStackSize))) {
            return Long.toString(count);
        }

        CountParts parts = CountParts.of(count, maxStackSize);
        if (style == CountDisplayStyle.STYLE_3) {
            return parts.formula();
        }

        return parts.grouped();
    }

    private static String countPart(String key, long count) {
        return StringUtils.translate(key, count);
    }

    private record FormatKey(long count, int maxStackSize, int rawDigits) {
    }

    private record CompactKey(long count, int maxStackSize) {
    }

    private record CountParts(long count, int maxStackSize, long boxes, long remainingGroups, long remainder) {
        private static CountParts of(long count, int maxStackSize) {
            if (count <= 0 || maxStackSize <= 1) {
                return new CountParts(count, Math.max(1, maxStackSize), 0, 0, Math.max(0, count));
            }

            long groups = count / maxStackSize;
            long remainder = count % maxStackSize;
            long boxes = groups / STACKS_PER_SHULKER_BOX;
            long remainingGroups = groups % STACKS_PER_SHULKER_BOX;
            return new CountParts(count, maxStackSize, boxes, remainingGroups, remainder);
        }

        private String grouped() {
            if (this.count <= 0) {
                return this.count == 0 ? countPart("lmlp.label.count.items", 0) : Long.toString(this.count);
            }

            if (this.maxStackSize <= 1) {
                return countPart("lmlp.label.count.items", this.count);
            }

            StringBuilder builder = new StringBuilder();
            this.appendGroupedParts(builder);
            return builder.length() > 0 ? builder.toString() : countPart("lmlp.label.count.items", this.count);
        }

        private String formula() {
            if (this.count <= 0 || this.maxStackSize <= 1) {
                return this.count == 0 ? countPart("lmlp.label.count.items", 0) : Long.toString(this.count);
            }

            StringBuilder builder = new StringBuilder();
            boolean hasPrevious = false;
            if (this.boxes > 0) {
                builder.append(this.boxes).append(" x SB");
                hasPrevious = true;
            }
            if (this.remainingGroups > 0) {
                if (hasPrevious) {
                    builder.append(" + ");
                }
                builder.append(this.remainingGroups).append(" x ").append(this.maxStackSize);
                hasPrevious = true;
            }
            if (this.remainder > 0) {
                if (hasPrevious) {
                    builder.append(" + ");
                }
                builder.append(this.remainder);
            }

            return builder.length() > 0 ? builder.toString() : Long.toString(this.count);
        }

        private void appendGroupedParts(StringBuilder builder) {
            boolean hasPrevious = false;
            if (this.boxes > 0) {
                builder.append(countPart("lmlp.label.count.shulker_boxes", this.boxes));
                hasPrevious = true;
            }
            if (this.remainingGroups > 0) {
                if (hasPrevious) {
                    builder.append(" + ");
                }
                builder.append(countPart("lmlp.label.count.stacks", this.remainingGroups));
                hasPrevious = true;
            }
            if (this.remainder > 0) {
                if (hasPrevious) {
                    builder.append(" + ");
                }
                builder.append(countPart("lmlp.label.count.items", this.remainder));
            }
        }
    }
}
