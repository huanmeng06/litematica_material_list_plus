package io.github.huanmeng06.lmlp.material;

import net.minecraft.class_1799;

public final class CountFormatter {
    private CountFormatter() {
    }

    public static String format(class_1799 stack, int count) {
        return format(count, stack.method_7914());
    }

    public static String format(int count, int maxStackSize) {
        if (count <= 0 || maxStackSize <= 1 || count <= maxStackSize) {
            return Integer.toString(count);
        }

        int groups = count / maxStackSize;
        int remainder = count % maxStackSize;

        if (remainder == 0) {
            return String.format("%d = %dx%d", count, groups, maxStackSize);
        }

        return String.format("%d = %dx%d+%d", count, groups, maxStackSize, remainder);
    }
}
