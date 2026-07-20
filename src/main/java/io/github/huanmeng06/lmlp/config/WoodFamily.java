package io.github.huanmeng06.lmlp.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum WoodFamily implements IConfigOptionListEntry {
    OAK("oak"),
    SPRUCE("spruce"),
    BIRCH("birch"),
    JUNGLE("jungle"),
    ACACIA("acacia"),
    DARK_OAK("dark_oak"),
    MANGROVE("mangrove"),
    CHERRY("cherry"),
    PALE_OAK("pale_oak"),
    BAMBOO("bamboo"),
    CRIMSON("crimson"),
    WARPED("warped");

    private final String id;

    WoodFamily(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    @Override
    public String getStringValue() {
        return this.id;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate("lmlp.wood_family." + this.id);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        WoodFamily[] values = values();
        return values[Math.floorMod(this.ordinal() + (forward ? 1 : -1), values.length)];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (WoodFamily family : values()) {
            if (family.id.equalsIgnoreCase(value)) {
                return family;
            }
        }
        return OAK;
    }
}
