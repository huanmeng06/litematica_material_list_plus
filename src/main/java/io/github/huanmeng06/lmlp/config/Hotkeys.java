package io.github.huanmeng06.lmlp.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

import java.util.List;

public final class Hotkeys {
    public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey(
            "openConfigGui",
            "L,C",
            KeybindSettings.EXCLUSIVE,
            "lmlp.config.comment.open_config_gui",
            "lmlp.config.name.open_config_gui",
            "lmlp.config.name.open_config_gui"
    );

    public static final ConfigHotkey CLEAR_ORIGIN_MARKER = new ConfigHotkey(
            "clearOriginMarker",
            "",
            "lmlp.config.comment.clear_origin_marker",
            "lmlp.config.name.clear_origin_marker",
            "lmlp.config.name.clear_origin_marker"
    );

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_CONFIG_GUI,
            CLEAR_ORIGIN_MARKER
    );

    private Hotkeys() {
    }
}
