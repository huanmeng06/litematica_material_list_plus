package io.github.huanmeng06.lmlp.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Configs implements IConfigHandler {
    private static final String FILE_NAME = LitematicaMaterialListPlus.MOD_ID + ".json";
    private static final String GENERIC = "Generic";
    private static final String HOTKEYS = "Hotkeys";
    private static final String CONFIG_FORMS = "ConfigForms";
    private static final String PREFERRED_RECIPES = "PreferredRecipes";
    private static final String OPEN_CONFIG_HOTKEY_CURRENT_DEFAULT = "L,C";
    private static final Set<String> OPEN_CONFIG_HOTKEY_OLD_DEFAULTS = Set.of("M,L,C", "EQUAL,C", "RIGHT_SHIFT,O");
    private static final Map<String, String> preferredRecipes = new HashMap<>();
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
    private static final List<String> COLOR_PATTERN_SUFFIXES = List.of("dye", "wool", "carpet", "terracotta");
    private static final ImmutableList<String> DEFAULT_RECIPE_STOP_ITEMS = ImmutableList.of(
            "minecraft:iron_ingot",
            "minecraft:gold_ingot",
            "minecraft:slime_ball",
            "minecraft:quartz",
            "minecraft:honey_bottle",
            "minecraft:redstone",
            "minecraft:rail",
            "minecraft:powered_rail",
            "minecraft:detector_rail",
            "minecraft:activator_rail",
            "minecraft:{color}_dye",
            "minecraft:{color}_wool",
            "minecraft:{color}_carpet",
            "minecraft:{color}_terracotta"
    );

    public static final class Generic {
        public static final ConfigBoolean DISABLE_LITEMATICA_HOVER_TOOLTIP = new ConfigBoolean(
                "disableLitematicaHoverTooltip",
                true,
                "Disable Litematica's original material hover tooltip. Turn this off to show Litematica's original tooltip.",
                "lmlp.config.name.disable_litematica_hover_tooltip"
        );

        public static final ConfigOptionList COUNT_DISPLAY_STYLE = new ConfigOptionList(
                "countDisplayStyle",
                CountDisplayStyle.STYLE_1,
                "Style 1: boxes + stacks + items\nStyle 2: total = boxes + stacks + items\nStyle 3: A × SB + B × 64 (16) + C\nStyle 4: total (Litematica)",
                "lmlp.config.name.count_display_style"
        );

        public static final ConfigInteger ORIGIN_MARKER_TIME = new ConfigInteger(
                "originMarkerTime",
                120,
                1,
                600,
                true,
                "How many seconds a clicked placement origin highlight and beam stay visible."
        );

        public static final ConfigInteger ORIGIN_MARKER_TEXT_SCALE = new ConfigInteger(
                "originMarkerTextScale",
                2,
                1,
                5,
                true,
                "Text scale for the placement origin marker label. 1 is smallest, 5 is largest."
        );

        public static final ConfigInteger MISSING_PLACEMENT_BUTTON_RANGE = new ConfigInteger(
                "missingPlacementButtonRange",
                64,
                8,
                512,
                true,
                "How close (in blocks) the player must be to an offline-cached placement's recorded origin for the \"I can't find it\" button to appear on that row."
        );

        public static final ConfigStringList RECIPE_STOP_ITEMS = new ConfigStringList(
                "recipeStopItems",
                DEFAULT_RECIPE_STOP_ITEMS,
                "Items in this list are treated as base materials. Use {color} to match all 16 Minecraft colors, for example minecraft:{color}_wool."
        );

        public static final ConfigBoolean REPLACE_WATER_BUCKET_WITH_ICE = new ConfigBoolean(
                "replaceWaterBucketWithIce",
                false,
                "When enabled, water buckets required by the material list are shown and counted as ice instead, one ice per bucket.",
                "lmlp.config.name.replace_water_bucket_with_ice"
        );

        public static final List<IConfigBase> OPTIONS = ImmutableList.of(
                DISABLE_LITEMATICA_HOVER_TOOLTIP,
                COUNT_DISPLAY_STYLE,
                REPLACE_WATER_BUCKET_WITH_ICE,
                ORIGIN_MARKER_TIME,
                ORIGIN_MARKER_TEXT_SCALE,
                MISSING_PLACEMENT_BUTTON_RANGE
        );

        private Generic() {
        }
    }

    public static final class ConfigForms {
        public static final List<IConfigBase> OPTIONS = ImmutableList.of(
                Generic.RECIPE_STOP_ITEMS
        );

        private ConfigForms() {
        }
    }

    static {
        Generic.RECIPE_STOP_ITEMS.setValueChangeCallback(config -> MaterialListPlusState.clearRecipeCaches());
        Generic.REPLACE_WATER_BUCKET_WITH_ICE.setValueChangeCallback(config -> MaterialListPlusState.clearRecipeCaches());
    }

    public static void loadFromFile() {
        File file = new File(FileUtils.getConfigDirectory(), FILE_NAME);
        if (file.exists() && file.isFile() && file.canRead()) {
            JsonElement element = JsonUtils.parseJsonFile(file);
            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, GENERIC, Generic.OPTIONS);
                if (root.has(CONFIG_FORMS)) {
                    ConfigUtils.readConfigBase(root, CONFIG_FORMS, ConfigForms.OPTIONS);
                } else {
                    ConfigUtils.readConfigBase(root, GENERIC, ConfigForms.OPTIONS);
                }
                ConfigUtils.readConfigBase(root, HOTKEYS, Hotkeys.HOTKEY_LIST);
                readPreferredRecipes(root);
                migrateOpenConfigHotkeyDefault();
                migrateOriginMarkerTimeConfig(root);
                migrateDisableLitematicaHoverTooltipConfig(root);
                migrateDefaultRecipeStopItems();
                migrateRecipeStopColorPatterns();
            }
        }
    }

    public static void saveToFile() {
        File dir = FileUtils.getConfigDirectory();
        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, GENERIC, Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, CONFIG_FORMS, ConfigForms.OPTIONS);
            ConfigUtils.writeConfigBase(root, HOTKEYS, Hotkeys.HOTKEY_LIST);
            writePreferredRecipes(root);
            JsonUtils.writeJsonToFile(root, new File(dir, FILE_NAME));
        }
    }

    public static boolean shouldStopRecipeDecomposition(String itemId) {
        String normalizedItemId = normalizeItemId(itemId);
        for (String configuredId : Generic.RECIPE_STOP_ITEMS.getStrings()) {
            if (matchesRecipeStopItem(configuredId, normalizedItemId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesRecipeStopItem(String configuredId, String normalizedItemId) {
        String normalizedConfiguredId = normalizeItemId(configuredId);
        if (!normalizedConfiguredId.contains("{color}")) {
            return normalizedConfiguredId.equals(normalizedItemId);
        }

        for (String color : COLOR_NAMES) {
            if (normalizedConfiguredId.replace("{color}", color).equals(normalizedItemId)) {
                return true;
            }
        }

        return false;
    }

    public static String preferredRecipeId(String itemId) {
        return preferredRecipes.getOrDefault(normalizeItemId(itemId), "");
    }

    public static boolean isPreferredRecipe(String itemId, String recipeId) {
        String preferredRecipeId = preferredRecipeId(itemId);
        return !preferredRecipeId.isEmpty() && preferredRecipeId.equals(recipeId);
    }

    public static boolean togglePreferredRecipe(String itemId, String recipeId) {
        String normalizedItemId = normalizeItemId(itemId);
        String normalizedRecipeId = recipeId == null ? "" : recipeId.trim();
        if (normalizedItemId.isEmpty() || normalizedRecipeId.isEmpty()) {
            return false;
        }

        boolean pinned;
        if (normalizedRecipeId.equals(preferredRecipes.get(normalizedItemId))) {
            preferredRecipes.remove(normalizedItemId);
            pinned = false;
        } else {
            preferredRecipes.put(normalizedItemId, normalizedRecipeId);
            pinned = true;
        }

        MaterialListPlusState.applyRecipePreferences();
        saveToFile();
        return pinned;
    }

    private static String normalizeItemId(String itemId) {
        String trimmed = itemId == null ? "" : itemId.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty() || trimmed.contains(":") || trimmed.startsWith("#")) {
            return trimmed;
        }

        return "minecraft:" + trimmed;
    }

    private static void migrateRecipeStopColorPatterns() {
        List<String> values = new ArrayList<>(Generic.RECIPE_STOP_ITEMS.getStrings());
        boolean modified = false;
        for (String suffix : COLOR_PATTERN_SUFFIXES) {
            if (compactColorPattern(values, suffix)) {
                modified = true;
            }
        }

        if (modified) {
            Generic.RECIPE_STOP_ITEMS.setStrings(values);
        }
    }

    private static void migrateDefaultRecipeStopItems() {
        List<String> values = new ArrayList<>(Generic.RECIPE_STOP_ITEMS.getStrings());
        Set<String> normalizedValues = new HashSet<>();
        for (String value : values) {
            normalizedValues.add(normalizeItemId(value));
        }

        boolean modified = false;
        for (String defaultItem : DEFAULT_RECIPE_STOP_ITEMS) {
            String normalizedDefault = normalizeItemId(defaultItem);
            if (!normalizedValues.contains(normalizedDefault)) {
                values.add(defaultItem);
                normalizedValues.add(normalizedDefault);
                modified = true;
            }
        }

        if (modified) {
            Generic.RECIPE_STOP_ITEMS.setStrings(values);
        }
    }

    private static boolean compactColorPattern(List<String> values, String suffix) {
        String pattern = "minecraft:{color}_" + suffix;
        Set<String> colorIds = new HashSet<>();
        for (String color : COLOR_NAMES) {
            colorIds.add("minecraft:" + color + "_" + suffix);
        }

        Set<String> normalizedValues = new HashSet<>();
        boolean hasPattern = false;
        for (String value : values) {
            String normalized = normalizeItemId(value);
            normalizedValues.add(normalized);
            if (normalized.equals(pattern)) {
                hasPattern = true;
            }
        }

        if (!normalizedValues.containsAll(colorIds)) {
            return false;
        }

        int insertIndex = values.size();
        for (int index = 0; index < values.size(); index++) {
            if (colorIds.contains(normalizeItemId(values.get(index)))) {
                insertIndex = index;
                break;
            }
        }

        for (int index = values.size() - 1; index >= 0; index--) {
            if (colorIds.contains(normalizeItemId(values.get(index)))) {
                values.remove(index);
            }
        }

        if (!hasPattern) {
            values.add(Math.min(insertIndex, values.size()), pattern);
        }

        return true;
    }

    private static void readPreferredRecipes(JsonObject root) {
        preferredRecipes.clear();
        if (!root.has(PREFERRED_RECIPES) || !root.get(PREFERRED_RECIPES).isJsonObject()) {
            return;
        }

        JsonObject preferred = root.getAsJsonObject(PREFERRED_RECIPES);
        for (Entry<String, JsonElement> entry : preferred.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                String itemId = normalizeItemId(entry.getKey());
                String recipeId = entry.getValue().getAsString().trim();
                if (!itemId.isEmpty() && !recipeId.isEmpty()) {
                    preferredRecipes.put(itemId, recipeId);
                }
            }
        }
    }

    private static void writePreferredRecipes(JsonObject root) {
        if (preferredRecipes.isEmpty()) {
            return;
        }

        JsonObject preferred = new JsonObject();
        for (Entry<String, String> entry : preferredRecipes.entrySet()) {
            if (!entry.getKey().isEmpty() && !entry.getValue().isEmpty()) {
                preferred.addProperty(entry.getKey(), entry.getValue());
            }
        }
        root.add(PREFERRED_RECIPES, preferred);
    }

    private static void migrateDisableLitematicaHoverTooltipConfig(JsonObject root) {
        if (!root.has(GENERIC) || !root.get(GENERIC).isJsonObject()) {
            return;
        }

        JsonObject generic = root.getAsJsonObject(GENERIC);
        if (generic.has("disableLitematicaHoverTooltip")) {
            return;
        }

        if (generic.has("enableLmlpHoverTooltip")) {
            JsonElement previousValue = generic.get("enableLmlpHoverTooltip");
            if (previousValue.isJsonPrimitive()) {
                Generic.DISABLE_LITEMATICA_HOVER_TOOLTIP.setBooleanValue(disablesLitematicaHoverTooltip(previousValue.getAsString()));
            }
            return;
        }

        if (generic.has("hoverTooltipMode")) {
            JsonElement previousValue = generic.get("hoverTooltipMode");
            if (previousValue.isJsonPrimitive()) {
                Generic.DISABLE_LITEMATICA_HOVER_TOOLTIP.setBooleanValue(disablesLitematicaHoverTooltip(previousValue.getAsString()));
            }
        }
    }

    private static void migrateOriginMarkerTimeConfig(JsonObject root) {
        if (!root.has(GENERIC) || !root.get(GENERIC).isJsonObject()) {
            return;
        }

        JsonObject generic = root.getAsJsonObject(GENERIC);
        if (generic.has("originMarkerTime")) {
            return;
        }

        int previousValue = Math.max(
                readIntegerConfig(generic, "originHighlightTime"),
                readIntegerConfig(generic, "originBeamTime"));
        if (previousValue > 0) {
            Generic.ORIGIN_MARKER_TIME.setIntegerValue(previousValue);
        }
    }

    private static int readIntegerConfig(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return 0;
        }

        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static void migrateOpenConfigHotkeyDefault() {
        String keys = Hotkeys.OPEN_CONFIG_GUI.getStringValue();
        if (OPEN_CONFIG_HOTKEY_OLD_DEFAULTS.contains(keys)) {
            Hotkeys.OPEN_CONFIG_GUI.setValueFromString(OPEN_CONFIG_HOTKEY_CURRENT_DEFAULT);
        }
    }

    private static boolean disablesLitematicaHoverTooltip(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return !"false".equals(normalized) && !"litematica".equals(normalized);
    }

    @Override
    public void load() {
        loadFromFile();
    }

    @Override
    public void save() {
        saveToFile();
    }
}
