package io.github.huanmeng06.lmlp.recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.FamilyIconCycle;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;

public final class AlternativeItemDisplay {
    // How long each wood family stays on screen before the cycle advances to
    // the next family. A family's icons are subdivided evenly within this
    // window, so parallel lists (任意木板 with 1 icon/family, 任意原木 with N
    // icons/family) always sit on the SAME family at the same moment — the
    // planks icon and the log icon stay material-matched regardless of how
    // many log variants each family has.
    private static final long FAMILY_CYCLE_MILLIS = FamilyIconCycle.FAMILY_WINDOW_MILLIS;
    // Fallback period for non-wood groups (e.g. 沙子/红沙) that don't split by
    // family: cycle the whole list one icon per this interval.
    private static final long ICON_CYCLE_MILLIS = FamilyIconCycle.FALLBACK_STEP_MILLIS;
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
    private static final List<String> WOOD_FAMILIES = List.of(
            "dark_oak",
            "pale_oak",
            "oak",
            "spruce",
            "birch",
            "jungle",
            "acacia",
            "mangrove",
            "cherry",
            "bamboo",
            "crimson",
            "warped");

    private AlternativeItemDisplay() {
    }

    public static ItemStack icon(IngredientSummary ingredient) {
        return cyclingIcon(ingredient.icons(), ingredient.icon());
    }

    public static ItemStack icon(RecipeSlotSummary slot) {
        return cyclingIcon(slot.icons(), slot.icon());
    }

    public static ItemStack icon(MaterialTreeNode node) {
        return cyclingIcon(node.icons(), node.icon());
    }

    public static ItemStack icon(List<ItemStack> icons, ItemStack fallback) {
        return cyclingIcon(icons, fallback);
    }

    public static String name(List<ItemStack> icons, List<String> alternatives) {
        if (alternatives.isEmpty()) {
            return "";
        }

        if (alternatives.size() == 1) {
            return alternatives.get(0);
        }

        String familyGroupName = woodFamilyGroupName(icons);
        if (!familyGroupName.isEmpty()) {
            return familyGroupName;
        }

        String directName = directAlternativeName(icons, alternatives);
        if (!directName.isEmpty()) {
            return directName;
        }

        String familyLogName = sameWoodFamilyLogName(icons, alternatives);
        if (!familyLogName.isEmpty()) {
            return familyLogName;
        }

        String familyPlanksName = sameWoodFamilyPlanksName(icons, alternatives);
        if (!familyPlanksName.isEmpty()) {
            return familyPlanksName;
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

    private static String directAlternativeName(List<ItemStack> icons, List<String> alternatives) {
        String coalCharcoalName = coalCharcoalName(icons, alternatives);
        if (!coalCharcoalName.isEmpty()) {
            return coalCharcoalName;
        }

        if (isSoulFireBasePair(icons)) {
            return StringUtils.translate("lmlp.label.recipe.any.soul_sand_soil");
        }

        if (isPurpurPair(icons)) {
            return StringUtils.translate("lmlp.label.recipe.any.purpur");
        }

        if (isSandPair(icons)) {
            return StringUtils.translate("lmlp.label.recipe.any.sand");
        }

        return "";
    }

    private static String coalCharcoalName(List<ItemStack> icons, List<String> alternatives) {
        if (icons.size() != 2 || icons.size() != alternatives.size()) {
            return "";
        }

        String coal = "";
        String charcoal = "";
        for (int index = 0; index < icons.size(); index++) {
            String id = ItemStackTexts.id(icons.get(index));
            if (id.equals("minecraft:coal")) {
                coal = alternatives.get(index);
            } else if (id.equals("minecraft:charcoal")) {
                charcoal = alternatives.get(index);
            } else {
                return "";
            }
        }

        return coal.isEmpty() || charcoal.isEmpty()
                ? ""
                : StringUtils.translate("lmlp.label.recipe.any.coal");
    }

    private static boolean isSoulFireBasePair(List<ItemStack> icons) {
        if (icons.size() != 2) {
            return false;
        }

        boolean hasSoulSand = false;
        boolean hasSoulSoil = false;
        for (ItemStack icon : icons) {
            String id = ItemStackTexts.id(icon);
            if (id.equals("minecraft:soul_sand")) {
                hasSoulSand = true;
            } else if (id.equals("minecraft:soul_soil")) {
                hasSoulSoil = true;
            } else {
                return false;
            }
        }

        return hasSoulSand && hasSoulSoil;
    }

    private static boolean isSandPair(List<ItemStack> icons) {
        boolean hasSand = false;
        boolean hasRedSand = false;
        for (ItemStack icon : icons) {
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

    private static boolean isPurpurPair(List<ItemStack> icons) {
        boolean hasPurpurBlock = false;
        boolean hasPurpurPillar = false;
        for (ItemStack icon : icons) {
            String id = ItemStackTexts.id(icon);
            if (id.equals("minecraft:purpur_block")) {
                hasPurpurBlock = true;
            } else if (id.equals("minecraft:purpur_pillar")) {
                hasPurpurPillar = true;
            } else {
                return false;
            }
        }

        return hasPurpurBlock && hasPurpurPillar;
    }

    private static String sameWoodFamilyLogName(List<ItemStack> icons, List<String> alternatives) {
        if (icons.size() < 2 || icons.size() != alternatives.size()) {
            return "";
        }

        String family = "";
        String preferredName = "";
        for (int index = 0; index < icons.size(); index++) {
            String path = itemPath(icons.get(index));
            if (!isLogLike(path)) {
                return "";
            }

            String currentFamily = woodFamily(path);
            if (currentFamily.isEmpty()) {
                return "";
            }
            if (family.isEmpty()) {
                family = currentFamily;
            } else if (!family.equals(currentFamily)) {
                return "";
            }

            if (preferredName.isEmpty() && (path.endsWith("_log") || path.endsWith("_stem"))) {
                preferredName = alternatives.get(index);
            }
        }

        return preferredName.isEmpty() ? alternatives.get(0) : preferredName;
    }

    private static String sameWoodFamilyPlanksName(List<ItemStack> icons, List<String> alternatives) {
        if (icons.size() < 2 || icons.size() != alternatives.size()) {
            return "";
        }

        String family = "";
        String preferredName = "";
        for (int index = 0; index < icons.size(); index++) {
            String path = itemPath(icons.get(index));
            if (!path.endsWith("_planks")) {
                return "";
            }

            String currentFamily = woodFamily(path);
            if (currentFamily.isEmpty()) {
                return "";
            }
            if (family.isEmpty()) {
                family = currentFamily;
            } else if (!family.equals(currentFamily)) {
                return "";
            }

            if (preferredName.isEmpty() && path.equals(family + "_planks")) {
                preferredName = alternatives.get(index);
            }
        }

        return preferredName.isEmpty() ? alternatives.get(0) : preferredName;
    }

    private static ItemStack cyclingIcon(List<ItemStack> icons, ItemStack fallback) {
        if (icons.isEmpty()) {
            return fallback;
        }

        ItemStack icon = FamilyIconCycle.pick(icons, System.currentTimeMillis(), FAMILY_CYCLE_MILLIS, ICON_CYCLE_MILLIS);
        return icon.isEmpty() ? fallback : icon;
    }

    private static String commonGroupKey(List<ItemStack> icons) {
        if (icons.size() < 2) {
            return "";
        }

        String suffix = commonIdSuffix(icons);
        if (suffix.isEmpty()) {
            suffix = commonWoodLogSuffix(icons);
        }

        return suffix.isEmpty() ? "" : COMMON_GROUP_KEYS.getOrDefault(suffix, "");
    }

    private static String commonIdSuffix(List<ItemStack> icons) {
        String suffix = null;
        for (ItemStack icon : icons) {
            String id = ItemStackTexts.id(icon);
            String path = itemPath(id);
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

    private static String commonWoodLogSuffix(List<ItemStack> icons) {
        boolean hasLog = false;
        boolean hasWood = false;
        for (ItemStack icon : icons) {
            String id = ItemStackTexts.id(icon);
            String path = itemPath(id);
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

    // Group a union of icons that spans several wood families into a single
    // "任意原木"/"任意木板" label, mirroring the minimal sub-material page's
    // family check. This must run before the per-family / common-suffix
    // heuristics, which only fire when every icon shares one family (e.g.
    // oak_log + oak_wood) and would otherwise fall back to a single item name
    // like "橡木原木" for a cross-family union.
    private static String woodFamilyGroupName(List<ItemStack> icons) {
        if (icons.size() < 2) {
            return "";
        }

        if (allIconsMatch(icons, AlternativeItemDisplay::isLogLike) && hasMultipleWoodFamilies(icons)) {
            return StringUtils.translate("lmlp.label.recipe.any.log");
        }

        if (allIconsMatch(icons, path -> path.endsWith("_planks")) && hasMultipleWoodFamilies(icons)) {
            return StringUtils.translate("lmlp.label.recipe.any.planks");
        }

        return "";
    }

    private static boolean hasMultipleWoodFamilies(List<ItemStack> icons) {
        String firstFamily = "";
        for (ItemStack icon : icons) {
            String family = woodFamily(itemPath(icon));
            if (family.isEmpty()) {
                return true;
            }
            if (firstFamily.isEmpty()) {
                firstFamily = family;
            } else if (!firstFamily.equals(family)) {
                return true;
            }
        }

        return false;
    }

    private static boolean allIconsMatch(List<ItemStack> icons, java.util.function.Predicate<String> predicate) {
        if (icons.isEmpty()) {
            return false;
        }

        for (ItemStack icon : icons) {
            if (icon.isEmpty() || !predicate.test(itemPath(icon))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isLogLike(String path) {
        return path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_stem")
                || path.endsWith("_hyphae")
                || path.equals("bamboo_block")
                || path.equals("stripped_bamboo_block");
    }

    private static String woodFamily(String path) {
        if (path.startsWith("stripped_")) {
            path = path.substring("stripped_".length());
        }

        for (String family : WOOD_FAMILIES) {
            if (path.equals(family) || path.startsWith(family + "_")) {
                return family;
            }
        }

        return "";
    }

    private static String itemPath(ItemStack stack) {
        return itemPath(ItemStackTexts.id(stack));
    }

    private static String itemPath(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
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
