package io.github.huanmeng06.lmlp.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
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
        public static final ConfigBoolean ENABLE_LMLP_HOVER_TOOLTIP = new ConfigBoolean(
                "enableLmlpHoverTooltip",
                true,
                "Use the LMLP compact/detail material hover tooltip. Disable to use the original Litematica hover tooltip.",
                "lmlp.config.name.enable_lmlp_hover_tooltip"
        );

        public static final ConfigStringList RECIPE_STOP_ITEMS = new ConfigStringList(
                "recipeStopItems",
                ImmutableList.of("minecraft:redstone"),
                "Item ids that should be treated as base materials and not recursively decomposed into recipes."
        );

        public static final List<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_LMLP_HOVER_TOOLTIP,
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

    @Override
    public void load() {
        loadFromFile();
    }

    @Override
    public void save() {
        saveToFile();
    }
}
