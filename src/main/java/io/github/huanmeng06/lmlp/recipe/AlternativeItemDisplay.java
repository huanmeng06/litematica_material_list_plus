package io.github.huanmeng06.lmlp.recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import net.minecraft.class_1799;

public final class AlternativeItemDisplay {
    private static final long ICON_CYCLE_MILLIS = 900L;
    private static final Map<String, String> COMMON_GROUP_KEYS = createCommonGroupKeys();
    private static final List<String> COMMON_PREFIXES = List.of(
            "light_gray_",
            "light_blue_",
            "dark_oak_",
            "pale_oak_",
            "stripped_",
            "white_",
            "orange_",
            "magenta_",
            "yellow_",
            "lime_",
            "pink_",
            "gray_",
            "cyan_",
            "purple_",
            "blue_",
            "brown_",
            "green_",
            "red_",
            "black_",
            "oak_",
            "spruce_",
            "birch_",
            "jungle_",
            "acacia_",
            "mangrove_",
            "cherry_",
            "bamboo_",
            "crimson_",
            "warped_");

    private AlternativeItemDisplay() {
    }

    public static class_1799 icon(IngredientSummary ingredient) {
        return cyclingIcon(ingredient.icons(), ingredient.icon());
    }

    public static class_1799 icon(RecipeSlotSummary slot) {
        return cyclingIcon(slot.icons(), slot.icon());
    }

    public static class_1799 icon(MaterialTreeNode node) {
        return cyclingIcon(node.icons(), node.icon());
    }

    public static String name(List<class_1799> icons, List<String> alternatives) {
        if (alternatives.isEmpty()) {
            return "";
        }

        if (alternatives.size() == 1) {
            return alternatives.get(0);
        }

        String directName = directAlternativeName(icons, alternatives);
        if (!directName.isEmpty()) {
            return directName;
        }

        String groupKey = commonGroupKey(icons);
        if (!groupKey.isEmpty()) {
            return StringUtils.translate(groupKey);
        }

        String commonSuffix = commonNameSuffix(alternatives);
        if (commonSuffix.length() >= 2) {
            return StringUtils.translate("lmlp.label.recipe.any_short", commonSuffix);
        }

        return "";
    }

    private static String directAlternativeName(List<class_1799> icons, List<String> alternatives) {
        if (!isSandPair(icons)) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(" / ");
        int limit = Math.min(alternatives.size(), 4);
        for (int i = 0; i < limit; i++) {
            joiner.add(alternatives.get(i));
        }

        if (alternatives.size() > limit) {
            joiner.add("...");
        }

        return joiner.toString();
    }

    private static boolean isSandPair(List<class_1799> icons) {
        boolean hasSand = false;
        boolean hasRedSand = false;
        for (class_1799 icon : icons) {
            String id = ItemStackTexts.id(icon);
            if (id.equals("minecraft:sand")) {
                hasSand = true;
            } else if (id.equals("minecraft:red_sand")) {
                hasRedSand = true;
            } else {
                return false;
            }
        }

        return hasSand && hasRedSand;
    }

    private static class_1799 cyclingIcon(List<class_1799> icons, class_1799 fallback) {
        if (icons.isEmpty()) {
            return fallback;
        }

        int index = (int) ((System.currentTimeMillis() / ICON_CYCLE_MILLIS) % icons.size());
        class_1799 icon = icons.get(index);
        return icon.method_7960() ? fallback : icon;
    }

    private static String commonGroupKey(List<class_1799> icons) {
        if (icons.size() < 2) {
            return "";
        }

        String suffix = commonIdSuffix(icons);
        if (suffix.isEmpty()) {
            suffix = commonWoodLogSuffix(icons);
        }

        return suffix.isEmpty() ? "" : COMMON_GROUP_KEYS.getOrDefault(suffix, "");
    }

    private static String commonIdSuffix(List<class_1799> icons) {
        String suffix = null;
        for (class_1799 icon : icons) {
            String id = ItemStackTexts.id(icon);
            int separator = id.indexOf(':');
            String path = separator >= 0 ? id.substring(separator + 1) : id;
            String currentSuffix = removeCommonPrefix(path);
            if (currentSuffix.equals(path) || currentSuffix.isEmpty()) {
                return "";
            }
            if (suffix == null) {
                suffix = currentSuffix;
            } else if (!suffix.equals(currentSuffix)) {
                return "";
            }
        }

        return suffix == null ? "" : suffix;
    }

    private static String commonWoodLogSuffix(List<class_1799> icons) {
        boolean hasLog = false;
        boolean hasWood = false;
        for (class_1799 icon : icons) {
            String id = ItemStackTexts.id(icon);
            int separator = id.indexOf(':');
            String path = separator >= 0 ? id.substring(separator + 1) : id;
            String suffix = removeCommonPrefix(path);
            if (suffix.equals("log")) {
                hasLog = true;
            } else if (suffix.equals("wood")) {
                hasWood = true;
            } else {
                return "";
            }
        }

        return hasLog && hasWood ? "log" : "";
    }

    private static String removeCommonPrefix(String path) {
        String value = path;
        boolean changed;
        do {
            changed = false;
            for (String prefix : COMMON_PREFIXES) {
                if (value.startsWith(prefix) && value.length() > prefix.length()) {
                    value = value.substring(prefix.length());
                    changed = true;
                    break;
                }
            }
        } while (changed);

        int underscore = value.indexOf('_');
        if (value.equals(path) && underscore >= 0 && underscore < value.length() - 1) {
            return value.substring(underscore + 1);
        }

        return value;
    }

    private static String commonNameSuffix(List<String> alternatives) {
        String suffix = alternatives.get(0);
        for (int i = 1; i < alternatives.size(); i++) {
            suffix = commonSuffix(suffix, alternatives.get(i));
            if (suffix.isEmpty()) {
                return "";
            }
        }

        return suffix.strip();
    }

    private static String commonSuffix(String first, String second) {
        int firstIndex = first.length() - 1;
        int secondIndex = second.length() - 1;
        while (firstIndex >= 0 && secondIndex >= 0 && first.charAt(firstIndex) == second.charAt(secondIndex)) {
            firstIndex--;
            secondIndex--;
        }

        return first.substring(firstIndex + 1);
    }

    private static Map<String, String> createCommonGroupKeys() {
        Map<String, String> keys = new HashMap<>();
        keys.put("planks", "lmlp.label.recipe.any.planks");
        keys.put("log", "lmlp.label.recipe.any.log");
        keys.put("wood", "lmlp.label.recipe.any.wood");
        keys.put("leaves", "lmlp.label.recipe.any.leaves");
        keys.put("sapling", "lmlp.label.recipe.any.sapling");
        keys.put("slab", "lmlp.label.recipe.any.slab");
        keys.put("stairs", "lmlp.label.recipe.any.stairs");
        keys.put("fence", "lmlp.label.recipe.any.fence");
        keys.put("fence_gate", "lmlp.label.recipe.any.fence_gate");
        keys.put("door", "lmlp.label.recipe.any.door");
        keys.put("trapdoor", "lmlp.label.recipe.any.trapdoor");
        keys.put("button", "lmlp.label.recipe.any.button");
        keys.put("pressure_plate", "lmlp.label.recipe.any.pressure_plate");
        keys.put("dye", "lmlp.label.recipe.any.dye");
        keys.put("wool", "lmlp.label.recipe.any.wool");
        keys.put("carpet", "lmlp.label.recipe.any.carpet");
        keys.put("bed", "lmlp.label.recipe.any.bed");
        keys.put("banner", "lmlp.label.recipe.any.banner");
        keys.put("stained_glass", "lmlp.label.recipe.any.stained_glass");
        keys.put("stained_glass_pane", "lmlp.label.recipe.any.stained_glass_pane");
        keys.put("terracotta", "lmlp.label.recipe.any.terracotta");
        keys.put("glazed_terracotta", "lmlp.label.recipe.any.glazed_terracotta");
        keys.put("concrete", "lmlp.label.recipe.any.concrete");
        keys.put("concrete_powder", "lmlp.label.recipe.any.concrete_powder");
        return keys;
    }
}
