package io.github.huanmeng06.lmlp.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

import java.util.List;

public final class Hotkeys {
    public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey(
            "openConfigGui",
            "M,L,C",
            KeybindSettings.EXCLUSIVE,
            "Open the Litematica Material List Plus config screen.",
            "lmlp.config.name.open_config_gui"
    );

    public static final ConfigHotkey CLEAR_ORIGIN_MARKER = new ConfigHotkey(
            "clearOriginMarker",
            "",
            KeybindSettings.EXCLUSIVE,
            "Clear the active placement origin highlight and beam.",
            "lmlp.config.name.clear_origin_marker"
    );

    public static final ConfigHotkey SHOW_HOVER_DETAILS = new ConfigHotkey(
            "showHoverDetails",
            "LEFT_ALT",
            KeybindSettings.MODIFIER_GUI,
            "Hold to show the detailed material hover tooltip.",
            "lmlp.config.name.show_hover_details"
    );

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_CONFIG_GUI,
            CLEAR_ORIGIN_MARKER,
            SHOW_HOVER_DETAILS
    );

    private Hotkeys() {
    }
}
