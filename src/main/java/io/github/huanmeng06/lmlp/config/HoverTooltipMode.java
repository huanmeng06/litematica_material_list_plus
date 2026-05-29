package io.github.huanmeng06.lmlp.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum HoverTooltipMode implements IConfigOptionListEntry {
    LMLP("lmlp", "lmlp.config.hover_tooltip_mode.lmlp"),
    LITEMATICA("litematica", "lmlp.config.hover_tooltip_mode.litematica"),
    HIDDEN("hidden", "lmlp.config.hover_tooltip_mode.hidden");

    private final String value;
    private final String translationKey;

    HoverTooltipMode(String value, String translationKey) {
        this.value = value;
        this.translationKey = translationKey;
    }

    @Override
    public String getStringValue() {
        return this.value;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        HoverTooltipMode[] values = values();
        int offset = forward ? 1 : -1;
        return values[Math.floorMod(this.ordinal() + offset, values.length)];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (HoverTooltipMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }

        return LMLP;
    }
}
