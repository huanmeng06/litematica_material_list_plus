package io.github.huanmeng06.lmlp.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum CandleMaterial implements IConfigOptionListEntry {
    PLAIN("plain", "candle"),
    WHITE("white", "white_candle"),
    ORANGE("orange", "orange_candle"),
    MAGENTA("magenta", "magenta_candle"),
    LIGHT_BLUE("light_blue", "light_blue_candle"),
    YELLOW("yellow", "yellow_candle"),
    LIME("lime", "lime_candle"),
    PINK("pink", "pink_candle"),
    GRAY("gray", "gray_candle"),
    LIGHT_GRAY("light_gray", "light_gray_candle"),
    CYAN("cyan", "cyan_candle"),
    PURPLE("purple", "purple_candle"),
    BLUE("blue", "blue_candle"),
    BROWN("brown", "brown_candle"),
    GREEN("green", "green_candle"),
    RED("red", "red_candle"),
    BLACK("black", "black_candle");

    private final String id;
    private final String blockPath;

    CandleMaterial(String id, String blockPath) {
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
        CandleMaterial[] values = values();
        return values[Math.floorMod(this.ordinal() + (forward ? 1 : -1), values.length)];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (CandleMaterial material : values()) {
            if (material.id.equalsIgnoreCase(value)) {
                return material;
            }
        }
        return PLAIN;
    }
}
