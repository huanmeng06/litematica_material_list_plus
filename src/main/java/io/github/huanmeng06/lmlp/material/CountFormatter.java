package io.github.huanmeng06.lmlp.material;

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
        String raw = rawDigits > 0 ? String.format("%" + rawDigits + "d", count) : Integer.toString(count);

        if (count <= 0 || maxStackSize <= 1 || count <= maxStackSize) {
            return raw;
        }

        int groups = count / maxStackSize;
        int remainder = count % maxStackSize;
        int boxes = groups / STACKS_PER_SHULKER_BOX;
        int remainingGroups = groups % STACKS_PER_SHULKER_BOX;

        StringBuilder builder = new StringBuilder(raw).append(" = ");
        boolean hasPrevious = false;
        if (boxes > 0) {
            builder.append(boxes).append(" 盒");
            hasPrevious = true;
        }
        if (remainingGroups > 0) {
            if (hasPrevious) {
                builder.append(" + ");
            }
            builder.append(remainingGroups).append(" 组");
            hasPrevious = true;
        }
        if (remainder > 0) {
            if (hasPrevious) {
                builder.append(" + ");
            }
            builder.append(remainder).append(" 个");
        }

        return builder.toString();
    }

    public static String compact(int count, int maxStackSize) {
        if (count <= 0 || maxStackSize <= 1 || count <= maxStackSize) {
            return Integer.toString(count);
        }

        int groups = count / maxStackSize;
        int remainder = count % maxStackSize;
        int boxes = groups / STACKS_PER_SHULKER_BOX;
        int remainingGroups = groups % STACKS_PER_SHULKER_BOX;

        StringBuilder builder = new StringBuilder();
        boolean hasPrevious = false;
        if (boxes > 0) {
            builder.append(boxes).append(" 盒");
            hasPrevious = true;
        }
        if (remainingGroups > 0) {
            if (hasPrevious) {
                builder.append(" + ");
            }
            builder.append(remainingGroups).append(" 组");
            hasPrevious = true;
        }
        if (remainder > 0) {
            if (hasPrevious) {
                builder.append(" + ");
            }
            builder.append(remainder).append(" 个");
        }

        return builder.toString();
    }
}
