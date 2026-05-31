package io.github.huanmeng06.lmlp.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum CountDisplayStyle implements IConfigOptionListEntry {
    STYLE_1("style1", "lmlp.config.count_display_style.style1"),
    STYLE_2("style2", "lmlp.config.count_display_style.style2"),
    STYLE_3("style3", "lmlp.config.count_display_style.style3"),
    STYLE_4("style4", "lmlp.config.count_display_style.style4");

    private final String value;
    private final String translationKey;

    CountDisplayStyle(String value, String translationKey) {
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
        CountDisplayStyle[] values = values();
        int offset = forward ? 1 : -1;
        return values[Math.floorMod(this.ordinal() + offset, values.length)];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (CountDisplayStyle style : values()) {
            if (style.value.equalsIgnoreCase(value)) {
                return style;
            }
        }

        return STYLE_1;
    }
}
