package io.github.huanmeng06.lmlp.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum GlazedTerracottaMaterial implements IConfigOptionListEntry {
    WHITE("white", "white_glazed_terracotta"),
    ORANGE("orange", "orange_glazed_terracotta"),
    MAGENTA("magenta", "magenta_glazed_terracotta"),
    LIGHT_BLUE("light_blue", "light_blue_glazed_terracotta"),
    YELLOW("yellow", "yellow_glazed_terracotta"),
    LIME("lime", "lime_glazed_terracotta"),
    PINK("pink", "pink_glazed_terracotta"),
    GRAY("gray", "gray_glazed_terracotta"),
    LIGHT_GRAY("light_gray", "light_gray_glazed_terracotta"),
    CYAN("cyan", "cyan_glazed_terracotta"),
    PURPLE("purple", "purple_glazed_terracotta"),
    BLUE("blue", "blue_glazed_terracotta"),
    BROWN("brown", "brown_glazed_terracotta"),
    GREEN("green", "green_glazed_terracotta"),
    RED("red", "red_glazed_terracotta"),
    BLACK("black", "black_glazed_terracotta");

    private final String id;
    private final String blockPath;

    GlazedTerracottaMaterial(String id, String blockPath) {
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
        GlazedTerracottaMaterial[] values = values();
        return values[Math.floorMod(this.ordinal() + (forward ? 1 : -1), values.length)];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (GlazedTerracottaMaterial material : values()) {
            if (material.id.equalsIgnoreCase(value)) {
                return material;
            }
        }
        return WHITE;
    }
}
