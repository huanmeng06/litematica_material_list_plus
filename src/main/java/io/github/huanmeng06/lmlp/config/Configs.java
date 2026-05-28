package io.github.huanmeng06.lmlp.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;

import java.io.File;

public class Configs implements IConfigHandler {
    private static final String FILE_NAME = LitematicaMaterialListPlus.MOD_ID + ".json";
    private static final String HOTKEYS = "Hotkeys";

    public static void loadFromFile() {
        File file = new File(FileUtils.getConfigDirectory(), FILE_NAME);
        if (file.exists() && file.isFile() && file.canRead()) {
            JsonElement element = JsonUtils.parseJsonFile(file);
            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, HOTKEYS, Hotkeys.HOTKEY_LIST);
            }
        }
    }

    public static void saveToFile() {
        File dir = FileUtils.getConfigDirectory();
        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, HOTKEYS, Hotkeys.HOTKEY_LIST);
            JsonUtils.writeJsonToFile(root, new File(dir, FILE_NAME));
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
