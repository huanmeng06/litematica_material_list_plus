package io.github.huanmeng06.lmlp.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum BedMaterial implements IConfigOptionListEntry {
    WHITE("white"),
    ORANGE("orange"),
    MAGENTA("magenta"),
    LIGHT_BLUE("light_blue"),
    YELLOW("yellow"),
    LIME("lime"),
    PINK("pink"),
    GRAY("gray"),
    LIGHT_GRAY("light_gray"),
    CYAN("cyan"),
    PURPLE("purple"),
    BLUE("blue"),
    BROWN("brown"),
    GREEN("green"),
    RED("red"),
    BLACK("black");

    private final String id;

    BedMaterial(String id) {
        this.id = id;
    }

    public String blockId() {
        return "minecraft:" + this.id + "_bed";
    }

    @Override
    public String getStringValue() {
        return this.id;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate("block.minecraft." + this.id + "_bed");
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        BedMaterial[] values = values();
        return values[Math.floorMod(this.ordinal() + (forward ? 1 : -1), values.length)];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (BedMaterial material : values()) {
            if (material.id.equalsIgnoreCase(value)) {
                return material;
            }
        }
        return WHITE;
    }
}
