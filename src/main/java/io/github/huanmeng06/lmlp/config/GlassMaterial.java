package io.github.huanmeng06.lmlp.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum GlassMaterial implements IConfigOptionListEntry {
    CLEAR("clear", "glass", "glass_pane"),
    WHITE("white", "white_stained_glass", "white_stained_glass_pane"),
    ORANGE("orange", "orange_stained_glass", "orange_stained_glass_pane"),
    MAGENTA("magenta", "magenta_stained_glass", "magenta_stained_glass_pane"),
    LIGHT_BLUE("light_blue", "light_blue_stained_glass", "light_blue_stained_glass_pane"),
    YELLOW("yellow", "yellow_stained_glass", "yellow_stained_glass_pane"),
    LIME("lime", "lime_stained_glass", "lime_stained_glass_pane"),
    PINK("pink", "pink_stained_glass", "pink_stained_glass_pane"),
    GRAY("gray", "gray_stained_glass", "gray_stained_glass_pane"),
    LIGHT_GRAY("light_gray", "light_gray_stained_glass", "light_gray_stained_glass_pane"),
    CYAN("cyan", "cyan_stained_glass", "cyan_stained_glass_pane"),
    PURPLE("purple", "purple_stained_glass", "purple_stained_glass_pane"),
    BLUE("blue", "blue_stained_glass", "blue_stained_glass_pane"),
    BROWN("brown", "brown_stained_glass", "brown_stained_glass_pane"),
    GREEN("green", "green_stained_glass", "green_stained_glass_pane"),
    RED("red", "red_stained_glass", "red_stained_glass_pane"),
    BLACK("black", "black_stained_glass", "black_stained_glass_pane");

    private final String id;
    private final String blockPath;
    private final String panePath;

    GlassMaterial(String id, String blockPath, String panePath) {
        this.id = id;
        this.blockPath = blockPath;
        this.panePath = panePath;
    }

    public String blockId() {
        return "minecraft:" + this.blockPath;
    }

    public String paneId() {
        return "minecraft:" + this.panePath;
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
        GlassMaterial[] values = values();
        return values[Math.floorMod(this.ordinal() + (forward ? 1 : -1), values.length)];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (GlassMaterial material : values()) {
            if (material.id.equalsIgnoreCase(value)) {
                return material;
            }
        }
        return CLEAR;
    }
}
