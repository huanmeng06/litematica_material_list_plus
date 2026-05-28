package io.github.huanmeng06.lmlp;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.event.InputHandler;

public class InitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(LitematicaMaterialListPlus.MOD_ID, new Configs());
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
    }
}
