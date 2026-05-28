package io.github.huanmeng06.lmlp.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

import java.util.List;

public final class Hotkeys {
    public static final ConfigHotkey SHOW_HOVER_DETAILS = new ConfigHotkey(
            "showHoverDetails",
            "LEFT_ALT",
            KeybindSettings.MODIFIER_GUI,
            "Hold to show the detailed material hover tooltip.",
            "lmlp.config.name.show_hover_details"
    );

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            SHOW_HOVER_DETAILS
    );

    private Hotkeys() {
    }
}
