package io.github.huanmeng06.lmlp.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum TerracottaMaterial implements IConfigOptionListEntry {
    PLAIN("plain", "terracotta"),
    WHITE("white", "white_terracotta"),
    ORANGE("orange", "orange_terracotta"),
    MAGENTA("magenta", "magenta_terracotta"),
    LIGHT_BLUE("light_blue", "light_blue_terracotta"),
    YELLOW("yellow", "yellow_terracotta"),
    LIME("lime", "lime_terracotta"),
    PINK("pink", "pink_terracotta"),
    GRAY("gray", "gray_terracotta"),
    LIGHT_GRAY("light_gray", "light_gray_terracotta"),
    CYAN("cyan", "cyan_terracotta"),
    PURPLE("purple", "purple_terracotta"),
    BLUE("blue", "blue_terracotta"),
    BROWN("brown", "brown_terracotta"),
    GREEN("green", "green_terracotta"),
    RED("red", "red_terracotta"),
    BLACK("black", "black_terracotta"),
    WHITE_GLAZED("white_glazed", "white_glazed_terracotta"),
    ORANGE_GLAZED("orange_glazed", "orange_glazed_terracotta"),
    MAGENTA_GLAZED("magenta_glazed", "magenta_glazed_terracotta"),
    LIGHT_BLUE_GLAZED("light_blue_glazed", "light_blue_glazed_terracotta"),
    YELLOW_GLAZED("yellow_glazed", "yellow_glazed_terracotta"),
    LIME_GLAZED("lime_glazed", "lime_glazed_terracotta"),
    PINK_GLAZED("pink_glazed", "pink_glazed_terracotta"),
    GRAY_GLAZED("gray_glazed", "gray_glazed_terracotta"),
    LIGHT_GRAY_GLAZED("light_gray_glazed", "light_gray_glazed_terracotta"),
    CYAN_GLAZED("cyan_glazed", "cyan_glazed_terracotta"),
    PURPLE_GLAZED("purple_glazed", "purple_glazed_terracotta"),
    BLUE_GLAZED("blue_glazed", "blue_glazed_terracotta"),
    BROWN_GLAZED("brown_glazed", "brown_glazed_terracotta"),
    GREEN_GLAZED("green_glazed", "green_glazed_terracotta"),
    RED_GLAZED("red_glazed", "red_glazed_terracotta"),
    BLACK_GLAZED("black_glazed", "black_glazed_terracotta");

    private final String id;
    private final String blockPath;

    TerracottaMaterial(String id, String blockPath) {
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
        TerracottaMaterial[] values = values();
        return values[Math.floorMod(this.ordinal() + (forward ? 1 : -1), values.length)];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (TerracottaMaterial material : values()) {
            if (material.id.equalsIgnoreCase(value)) {
                return material;
            }
        }
        return PLAIN;
    }
}
