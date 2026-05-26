package io.github.huanmeng06.lmlp.material;

import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

public final class ItemStackTexts {
    private ItemStackTexts() {
    }

    public static String id(class_1799 stack) {
        class_2960 id = class_7923.field_41178.method_10221(stack.method_7909());
        return id == null ? "unknown" : id.toString();
    }

    public static String name(class_1799 stack) {
        return stack.method_7964().getString();
    }
}
