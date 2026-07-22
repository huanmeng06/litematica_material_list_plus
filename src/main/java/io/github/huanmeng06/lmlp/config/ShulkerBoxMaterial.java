package io.github.huanmeng06.lmlp.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum ShulkerBoxMaterial implements IConfigOptionListEntry {
    PLAIN("plain", "shulker_box"),
    WHITE("white", "white_shulker_box"),
    ORANGE("orange", "orange_shulker_box"),
    MAGENTA("magenta", "magenta_shulker_box"),
    LIGHT_BLUE("light_blue", "light_blue_shulker_box"),
    YELLOW("yellow", "yellow_shulker_box"),
    LIME("lime", "lime_shulker_box"),
    PINK("pink", "pink_shulker_box"),
    GRAY("gray", "gray_shulker_box"),
    LIGHT_GRAY("light_gray", "light_gray_shulker_box"),
    CYAN("cyan", "cyan_shulker_box"),
    PURPLE("purple", "purple_shulker_box"),
    BLUE("blue", "blue_shulker_box"),
    BROWN("brown", "brown_shulker_box"),
    GREEN("green", "green_shulker_box"),
    RED("red", "red_shulker_box"),
    BLACK("black", "black_shulker_box");

    private final String id;
    private final String blockPath;

    ShulkerBoxMaterial(String id, String blockPath) {
        this.id = id;
        this.blockPath = blockPath;
    }

    public String blockId() {
        return "minecraft:" + this.blockPath;
    }

    @Override
    public String getStringValue() {
        return this.id;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate("block.minecraft." + this.blockPath);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        ShulkerBoxMaterial[] values = values();
        return values[Math.floorMod(this.ordinal() + (forward ? 1 : -1), values.length)];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (ShulkerBoxMaterial material : values()) {
            if (material.id.equalsIgnoreCase(value)) {
                return material;
            }
        }
        return PLAIN;
    }
}
