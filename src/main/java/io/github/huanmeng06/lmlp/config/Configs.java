package io.github.huanmeng06.lmlp.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class Configs implements IConfigHandler {
    private static final String FILE_NAME = LitematicaMaterialListPlus.MOD_ID + ".json";
    private static final String GENERIC = "Generic";
    private static final String HOTKEYS = "Hotkeys";

    public static final class Generic {
        public static final ConfigOptionList HOVER_TOOLTIP_MODE = new ConfigOptionList(
                "hoverTooltipMode",
                HoverTooltipMode.LMLP,
                "Choose which material hover tooltip to show.",
                "lmlp.config.name.hover_tooltip_mode"
        );

        public static final ConfigStringList RECIPE_STOP_ITEMS = new ConfigStringList(
                "recipeStopItems",
                ImmutableList.of("minecraft:redstone"),
                "Items in this list are treated as base materials."
        );

        public static final List<IConfigBase> OPTIONS = ImmutableList.of(
                HOVER_TOOLTIP_MODE,
                RECIPE_STOP_ITEMS
        );

        private Generic() {
        }
    }

    static {
        Generic.RECIPE_STOP_ITEMS.setValueChangeCallback(config -> MaterialListPlusState.clearRecipeCaches());
    }

    public static void loadFromFile() {
        File file = new File(FileUtils.getConfigDirectory(), FILE_NAME);
        if (file.exists() && file.isFile() && file.canRead()) {
            JsonElement element = JsonUtils.parseJsonFile(file);
            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, GENERIC, Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, HOTKEYS, Hotkeys.HOTKEY_LIST);
                migrateLegacyHoverTooltipConfig(root);
            }
        }
    }

    public static void saveToFile() {
        File dir = FileUtils.getConfigDirectory();
        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, GENERIC, Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, HOTKEYS, Hotkeys.HOTKEY_LIST);
            JsonUtils.writeJsonToFile(root, new File(dir, FILE_NAME));
        }
    }

    public static boolean shouldStopRecipeDecomposition(String itemId) {
        String normalizedItemId = normalizeItemId(itemId);
        for (String configuredId : Generic.RECIPE_STOP_ITEMS.getStrings()) {
            if (normalizeItemId(configuredId).equals(normalizedItemId)) {
                return true;
            }
        }

        return false;
    }

    private static String normalizeItemId(String itemId) {
        String trimmed = itemId == null ? "" : itemId.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty() || trimmed.contains(":") || trimmed.startsWith("#")) {
            return trimmed;
        }

        return "minecraft:" + trimmed;
    }

    private static void migrateLegacyHoverTooltipConfig(JsonObject root) {
        if (!root.has(GENERIC) || !root.get(GENERIC).isJsonObject()) {
            return;
        }

        JsonObject generic = root.getAsJsonObject(GENERIC);
        if (generic.has("hoverTooltipMode") || !generic.has("enableLmlpHoverTooltip")) {
            return;
        }

        JsonElement legacyValue = generic.get("enableLmlpHoverTooltip");
        if (legacyValue.isJsonPrimitive() && legacyValue.getAsJsonPrimitive().isBoolean()) {
            Generic.HOVER_TOOLTIP_MODE.setOptionListValue(legacyValue.getAsBoolean() ? HoverTooltipMode.LMLP : HoverTooltipMode.LITEMATICA);
        }
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
