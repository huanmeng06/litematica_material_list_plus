package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.Hotkeys;
import net.minecraft.class_11908;
import net.minecraft.class_11909;

import java.util.Collection;
import java.util.List;

public class GuiConfigs extends GuiConfigsBase {
    private static ConfigGuiTab currentTab = ConfigGuiTab.GENERIC;

    public GuiConfigs() {
        super(
                10,
                50,
                LitematicaMaterialListPlus.MOD_ID,
                null,
                "lmlp.gui.title.configs",
                String.format("%s", LitematicaMaterialListPlus.MOD_VERSION)
        );
    }

    @Override
    public void initGui() {
        this.clearOptions();
        super.initGui();

        int x = 10;
        int y = 26;
        for (ConfigGuiTab tab : ConfigGuiTab.values()) {
            x += this.createButton(x, y, -1, tab);
        }
        this.updateKeybindButtons();
    }

    @Override
    public boolean onKeyTyped(class_11908 keyInput) {
        boolean handled = super.onKeyTyped(keyInput);
        this.updateKeybindButtons();
        return handled;
    }

    @Override
    public boolean onMouseClicked(class_11909 mouseClick, boolean doubleClick) {
        boolean preferredWoodWasEnabled = Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue();
        boolean preferredStoneWasEnabled = Configs.ConfigForms.PREFERRED_STONE_ENABLED.getBooleanValue();
        boolean preferredGlassWasEnabled = Configs.ConfigForms.PREFERRED_GLASS_ENABLED.getBooleanValue();
        boolean preferredCarpetWasEnabled = Configs.ConfigForms.PREFERRED_CARPET_ENABLED.getBooleanValue();
        boolean preferredTerracottaWasEnabled = Configs.ConfigForms.PREFERRED_TERRACOTTA_ENABLED.getBooleanValue();
        boolean preferredGlazedTerracottaWasEnabled = Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_ENABLED.getBooleanValue();
        boolean handled = super.onMouseClicked(mouseClick, doubleClick);
        boolean preferredWoodIsEnabled = Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue();
        boolean preferredStoneIsEnabled = Configs.ConfigForms.PREFERRED_STONE_ENABLED.getBooleanValue();
        boolean preferredGlassIsEnabled = Configs.ConfigForms.PREFERRED_GLASS_ENABLED.getBooleanValue();
        boolean preferredCarpetIsEnabled = Configs.ConfigForms.PREFERRED_CARPET_ENABLED.getBooleanValue();
        boolean preferredTerracottaIsEnabled = Configs.ConfigForms.PREFERRED_TERRACOTTA_ENABLED.getBooleanValue();
        boolean preferredGlazedTerracottaIsEnabled = Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_ENABLED.getBooleanValue();

        if (currentTab == ConfigGuiTab.PREFERENCE_FORM
                && this.getListWidget() instanceof PreferenceWidgetListConfigOptions preferenceList) {
            if (preferredWoodWasEnabled != preferredWoodIsEnabled) {
                preferenceList.setGroupExpanded(Configs.ConfigForms.PREFERRED_WOOD_ENABLED, preferredWoodIsEnabled);
            }
            if (preferredStoneWasEnabled != preferredStoneIsEnabled) {
                preferenceList.setGroupExpanded(Configs.ConfigForms.PREFERRED_STONE_ENABLED, preferredStoneIsEnabled);
            }
            if (preferredGlassWasEnabled != preferredGlassIsEnabled) {
                preferenceList.setGroupExpanded(Configs.ConfigForms.PREFERRED_GLASS_ENABLED, preferredGlassIsEnabled);
            }
            if (preferredCarpetWasEnabled != preferredCarpetIsEnabled) {
                preferenceList.setGroupExpanded(Configs.ConfigForms.PREFERRED_CARPET_ENABLED, preferredCarpetIsEnabled);
            }
            if (preferredTerracottaWasEnabled != preferredTerracottaIsEnabled) {
                preferenceList.setGroupExpanded(Configs.ConfigForms.PREFERRED_TERRACOTTA_ENABLED, preferredTerracottaIsEnabled);
            }
            if (preferredGlazedTerracottaWasEnabled != preferredGlazedTerracottaIsEnabled) {
                preferenceList.setGroupExpanded(
                        Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_ENABLED,
                        preferredGlazedTerracottaIsEnabled);
            }
        }

        this.updateKeybindButtons();
        return handled;
    }

    @Override
    protected WidgetListConfigOptions createListWidget(int x, int y) {
        if (currentTab == ConfigGuiTab.PREFERENCE_FORM) {
            return new PreferenceWidgetListConfigOptions(
                    x,
                    y,
                    this.getBrowserWidth(),
                    this.getBrowserHeight(),
                    this.getConfigWidth(),
                    0.0F,
                    this.useKeybindSearch(),
                    this
            );
        }

        return super.createListWidget(x, y);
    }

    @Override
    protected boolean useKeybindSearch() {
        return true;
    }

    @Override
    protected int getConfigWidth() {
        if (currentTab == ConfigGuiTab.GENERIC
                || currentTab == ConfigGuiTab.CONFIG_FORMS
                || currentTab == ConfigGuiTab.PREFERENCE_FORM
                || currentTab == ConfigGuiTab.HOTKEYS) {
            return 140;
        }

        return super.getConfigWidth();
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        Collection<? extends IConfigBase> configs = switch (currentTab) {
            case GENERIC -> Configs.Generic.OPTIONS;
            case CONFIG_FORMS -> Configs.ConfigForms.OPTIONS;
            case PREFERENCE_FORM -> Configs.ConfigForms.PREFERENCE_OPTIONS;
            case HOTKEYS -> Hotkeys.HOTKEY_LIST;
        };
        return ConfigOptionWrapper.createFor(configs);
    }

    private int createButton(int x, int y, int width, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName(), new String[0]);
        button.setEnabled(currentTab != tab);
        this.addButton(button, new ButtonListener(tab, this));
        return button.getWidth() + 2;
    }

    private enum ConfigGuiTab {
        GENERIC("lmlp.gui.button.config_gui.generic"),
        CONFIG_FORMS("lmlp.gui.button.config_gui.config_forms"),
        PREFERENCE_FORM("lmlp.gui.button.config_gui.preferred_replacement_form"),
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
