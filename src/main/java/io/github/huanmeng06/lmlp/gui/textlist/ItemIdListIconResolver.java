package io.github.huanmeng06.lmlp.gui.textlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class ItemIdListIconResolver {
    private static final long DISPLAY_CYCLE_MS = 900L;
    private static final Display EMPTY_DISPLAY = new Display(ItemStack.EMPTY, "");
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
    // Mirrors Configs.WOOD_NAMES / MinimalSubMaterialListView.WOOD_FAMILIES.
    private static final List<String> WOOD_NAMES = List.of(
            "oak",
            "spruce",
            "birch",
            "jungle",
            "acacia",
            "dark_oak",
            "mangrove",
            "cherry",
            "bamboo",
            "crimson",
            "warped",
            "pale_oak"
    );
    private static final Map<String, List<Display>> CACHE = new HashMap<>();

    private ItemIdListIconResolver() {
    }

    static List<Display> allIcons(String value) {
        return displaysFor(value);
    }

    static Display currentIcon(String value) {
        List<Display> displays = displaysFor(value);
        if (displays.isEmpty()) {
            return EMPTY_DISPLAY;
        }

        int index = displays.size() == 1 ? 0 : (int) ((System.currentTimeMillis() / DISPLAY_CYCLE_MS) % displays.size());
        return displays.get(index);
    }

    private static List<Display> displaysFor(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return List.of();
        }

        return CACHE.computeIfAbsent(normalized, ItemIdListIconResolver::resolveDisplays);
    }

    private static List<Display> resolveDisplays(String normalized) {
        List<String> ids = List.of(normalized);
        if (normalized.contains("{color}")) {
            ids = expandWildcard(ids, "{color}", COLOR_NAMES);
        }
        if (normalized.contains("{wood}")) {
            ids = expandWildcard(ids, "{wood}", WOOD_NAMES);
        }

        if (ids.size() == 1 && ids.get(0).equals(normalized)) {
            Display display = resolveDisplay(normalized);
            return display.stack().isEmpty() ? List.of() : List.of(display);
        }

        Map<String, Display> displays = new LinkedHashMap<>();
        for (String id : ids) {
            Display display = resolveDisplay(id);
            if (!display.stack().isEmpty()) {
                displays.putIfAbsent(id, display);
            }
        }

        return displays.isEmpty() ? List.of() : List.copyOf(new ArrayList<>(displays.values()));
    }

    private static List<String> expandWildcard(List<String> ids, String token, List<String> names) {
        List<String> expanded = new ArrayList<>(ids.size() * names.size());
        for (String id : ids) {
            for (String name : names) {
                expanded.add(id.replace(token, name));
            }
        }
        return expanded;
    }

    private static Display resolveDisplay(String id) {
        ItemStack stack = resolveStack(id);
        return stack.isEmpty() ? EMPTY_DISPLAY : new Display(stack, id);
    }

    private static ItemStack resolveStack(String id) {
        if (id.isEmpty() || id.startsWith("#")) {
            return ItemStack.EMPTY;
        }

        try {
            Identifier identifier = Identifier.parse(id);
            if (!BuiltInRegistries.ITEM.containsKey(identifier)) {
                return ItemStack.EMPTY;
            }

            Item item = BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
            if (item == null) {
                return ItemStack.EMPTY;
            }

            ItemStack stack = item.getDefaultInstance();
            return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack;
        } catch (RuntimeException exception) {
            return ItemStack.EMPTY;
        }
    }

    private static String normalize(String value) {
        String trimmed = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty() || trimmed.contains(":") || trimmed.startsWith("#")) {
            return trimmed;
        }

        return "minecraft:" + trimmed;
    }

    record Display(ItemStack stack, String id) {
    }
}
