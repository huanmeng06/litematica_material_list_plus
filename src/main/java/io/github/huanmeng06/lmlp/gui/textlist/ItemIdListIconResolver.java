package io.github.huanmeng06.lmlp.gui.textlist;

import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ItemIdListIconResolver {
    private static final long DISPLAY_CYCLE_MS = 900L;
    private static final List<String> COLOR_NAMES = List.of(
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black"
    );
    private static final Map<String, List<class_1799>> CACHE = new HashMap<>();

    private ItemIdListIconResolver() {
    }

    static class_1799 currentStack(String value) {
        List<class_1799> stacks = stacksFor(value);
        if (stacks.isEmpty()) {
            return class_1799.field_8037;
        }

        int index = stacks.size() == 1 ? 0 : (int) ((System.currentTimeMillis() / DISPLAY_CYCLE_MS) % stacks.size());
        return stacks.get(index).method_7972();
    }

    private static List<class_1799> stacksFor(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return List.of();
        }

        return CACHE.computeIfAbsent(normalized, ItemIdListIconResolver::resolveStacks);
    }

    private static List<class_1799> resolveStacks(String normalized) {
        if (!normalized.contains("{color}")) {
            class_1799 stack = resolveStack(normalized);
            return stack.method_7960() ? List.of() : List.of(stack);
        }

        Map<String, class_1799> stacks = new LinkedHashMap<>();
        for (String color : COLOR_NAMES) {
            String id = normalized.replace("{color}", color);
            class_1799 stack = resolveStack(id);
            if (!stack.method_7960()) {
                stacks.putIfAbsent(id, stack);
            }
        }

        return stacks.isEmpty() ? List.of() : List.copyOf(new ArrayList<>(stacks.values()));
    }

    private static class_1799 resolveStack(String id) {
        if (id.isEmpty() || id.startsWith("#")) {
            return class_1799.field_8037;
        }

        try {
            class_2960 identifier = new class_2960(id);
            if (!class_7923.field_41178.method_10250(identifier)) {
                return class_1799.field_8037;
            }

            class_1792 item = class_7923.field_41178.method_10223(identifier);
            if (item == null) {
                return class_1799.field_8037;
            }

            class_1799 stack = item.method_7854();
            return stack == null || stack.method_7960() ? class_1799.field_8037 : stack;
        } catch (RuntimeException exception) {
            return class_1799.field_8037;
        }
    }

    private static String normalize(String value) {
        String trimmed = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty() || trimmed.contains(":") || trimmed.startsWith("#")) {
            return trimmed;
        }

        return "minecraft:" + trimmed;
    }
}