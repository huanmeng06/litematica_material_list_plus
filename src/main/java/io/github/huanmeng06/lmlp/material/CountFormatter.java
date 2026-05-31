package io.github.huanmeng06.lmlp.material;

import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.CountDisplayStyle;
import net.minecraft.class_1799;

public final class CountFormatter {
    private static final int STACKS_PER_SHULKER_BOX = 27;

    private CountFormatter() {
    }

    public static String format(class_1799 stack, int count) {
        return format(count, stack.method_7914());
    }

    public static String format(int count, int maxStackSize) {
        return format(count, maxStackSize, 0);
    }

    public static String formatAligned(class_1799 stack, int count, int rawDigits) {
        return format(count, stack.method_7914(), rawDigits);
    }

    public static String format(int count, int maxStackSize, int rawDigits) {
        CountDisplayStyle style = (CountDisplayStyle) Configs.Generic.COUNT_DISPLAY_STYLE.getOptionListValue();
        String raw = rawDigits > 0 ? String.format("%" + rawDigits + "d", count) : Integer.toString(count);
        if (style == CountDisplayStyle.STYLE_4) {
            return raw;
        }

        if (style == CountDisplayStyle.STYLE_1 && (count <= 0 || maxStackSize <= 1 || count <= maxStackSize)) {
            return raw;
        }

        CountParts parts = CountParts.of(count, maxStackSize);
        if (style == CountDisplayStyle.STYLE_3) {
            return parts.formula();
        }

        String grouped = parts.grouped();
        if (style == CountDisplayStyle.STYLE_2) {
            return grouped;
        }

        return raw + " = " + grouped;
    }

    public static String compact(int count, int maxStackSize) {
        CountDisplayStyle style = (CountDisplayStyle) Configs.Generic.COUNT_DISPLAY_STYLE.getOptionListValue();
        if (style == CountDisplayStyle.STYLE_4 || (style == CountDisplayStyle.STYLE_1 && (count <= 0 || maxStackSize <= 1 || count <= maxStackSize))) {
            return Integer.toString(count);
        }

        CountParts parts = CountParts.of(count, maxStackSize);
        if (style == CountDisplayStyle.STYLE_3) {
            return parts.formula();
        }

        return parts.grouped();
    }

    private static String countPart(String key, int count) {
        return StringUtils.translate(key, count);
    }

    private record CountParts(int count, int maxStackSize, int boxes, int remainingGroups, int remainder) {
        private static CountParts of(int count, int maxStackSize) {
            if (count <= 0 || maxStackSize <= 1) {
                return new CountParts(count, Math.max(1, maxStackSize), 0, 0, Math.max(0, count));
            }

            int groups = count / maxStackSize;
            int remainder = count % maxStackSize;
            int boxes = groups / STACKS_PER_SHULKER_BOX;
            int remainingGroups = groups % STACKS_PER_SHULKER_BOX;
            return new CountParts(count, maxStackSize, boxes, remainingGroups, remainder);
        }

        private String grouped() {
            if (this.count <= 0) {
                return Integer.toString(this.count);
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
                return Integer.toString(this.count);
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

            return builder.length() > 0 ? builder.toString() : Integer.toString(this.count);
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
