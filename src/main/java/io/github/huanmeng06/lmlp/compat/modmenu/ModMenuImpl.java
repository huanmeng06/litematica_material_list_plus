package io.github.huanmeng06.lmlp.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.huanmeng06.lmlp.gui.GuiConfigs;

public class ModMenuImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new GuiConfigs().setParent(parent);
    }
}
