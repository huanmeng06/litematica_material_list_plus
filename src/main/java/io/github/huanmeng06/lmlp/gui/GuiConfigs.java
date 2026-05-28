package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.config.Hotkeys;

import java.util.Collection;
import java.util.List;

public class GuiConfigs extends GuiConfigsBase {
    private static ConfigGuiTab currentTab = ConfigGuiTab.HOTKEYS;

    public GuiConfigs() {
        super(
                10,
                50,
                LitematicaMaterialListPlus.MOD_ID,
                null,
                "lmlp.gui.title.configs",
                LitematicaMaterialListPlus.MOD_VERSION
        );
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;
        for (ConfigGuiTab tab : ConfigGuiTab.values()) {
            x += this.createButton(x, y, -1, tab);
        }
    }

    @Override
    protected boolean useKeybindSearch() {
        return currentTab == ConfigGuiTab.HOTKEYS;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        Collection<? extends IConfigBase> configs = switch (currentTab) {
            case HOTKEYS -> Hotkeys.HOTKEY_LIST;
        };
        return ConfigOptionWrapper.createFor(configs);
    }

    private int createButton(int x, int y, int width, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(currentTab != tab);
        this.addButton(button, new ButtonListener(tab, this));
        return button.getWidth() + 2;
    }

    private enum ConfigGuiTab {
        HOTKEYS("lmlp.gui.button.config_gui.hotkeys");

        private final String translationKey;

        ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
        }
    }

    private static class ButtonListener implements IButtonActionListener {
        private final ConfigGuiTab tab;
        private final GuiConfigs parent;

        private ButtonListener(ConfigGuiTab tab, GuiConfigs parent) {
            this.tab = tab;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            currentTab = this.tab;
            this.parent.reCreateListWidget();
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }
}
