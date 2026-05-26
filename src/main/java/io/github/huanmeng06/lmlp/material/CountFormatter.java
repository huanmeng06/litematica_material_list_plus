package io.github.huanmeng06.lmlp.material;

import net.minecraft.class_1799;

public final class CountFormatter {
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
        String raw = rawDigits > 0 ? String.format("%" + rawDigits + "d", count) : Integer.toString(count);

        if (count <= 0 || maxStackSize <= 1 || count <= maxStackSize) {
            return raw;
        }

        int groups = count / maxStackSize;
        int remainder = count % maxStackSize;

        if (remainder == 0) {
            return String.format("%s = %d x %d", raw, groups, maxStackSize);
        }

        return String.format("%s = %d x %d + %d", raw, groups, maxStackSize, remainder);
    }
}
