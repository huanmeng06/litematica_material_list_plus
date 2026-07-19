package io.github.huanmeng06.lmlp.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum StoneMaterialFamily implements IConfigOptionListEntry {
    STONE("stone", "minecraft:stone"),
    COBBLESTONE("cobblestone", "minecraft:cobblestone"),
    SMOOTH_STONE("smooth_stone", "minecraft:smooth_stone"),
    GRANITE("granite", "minecraft:granite"),
    DIORITE("diorite", "minecraft:diorite"),
    ANDESITE("andesite", "minecraft:andesite"),
    DEEPSLATE("deepslate", "minecraft:cobbled_deepslate"),
    BLACKSTONE("blackstone", "minecraft:blackstone"),
    TUFF("tuff", "minecraft:tuff"),
    QUARTZ("quartz", "minecraft:quartz_block");

    private final String id;
    private final String representativeBlockId;

    StoneMaterialFamily(String id, String representativeBlockId) {
        this.id = id;
        this.representativeBlockId = representativeBlockId;
    }

    public String representativeBlockId() {
        return this.representativeBlockId;
    }

    @Override
    public String getStringValue() {
        return this.id;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate("lmlp.stone_family." + this.id);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        StoneMaterialFamily[] values = values();
        return values[Math.floorMod(this.ordinal() + (forward ? 1 : -1), values.length)];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (StoneMaterialFamily family : values()) {
            if (family.id.equalsIgnoreCase(value)) {
                return family;
            }
        }
        return SMOOTH_STONE;
    }
}
