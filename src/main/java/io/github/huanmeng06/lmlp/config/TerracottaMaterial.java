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
    BLACK("black", "black_terracotta");

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
