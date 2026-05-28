package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.config.gui.GuiModConfigs;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.config.Hotkeys;

public class GuiConfigs extends GuiModConfigs {
    public GuiConfigs() {
        super(
                LitematicaMaterialListPlus.MOD_ID,
                Hotkeys.HOTKEY_LIST,
                "lmlp.gui.title.configs",
                LitematicaMaterialListPlus.MOD_VERSION
        );
    }
}
