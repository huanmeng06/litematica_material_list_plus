package io.github.huanmeng06.lmlp;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.Hotkeys;
import io.github.huanmeng06.lmlp.event.InputHandler;
import io.github.huanmeng06.lmlp.gui.GuiConfigs;

public class InitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(LitematicaMaterialListPlus.MOD_ID, new Configs());
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        Hotkeys.OPEN_CONFIG_GUI.getKeybind().setCallback((action, key) -> {
            GuiBase.openGui(new GuiConfigs());
            return true;
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ChunkMissingMaterialListCache.onWorldJoined(client, "client_play.join"));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ChunkMissingMaterialListCache.onWorldDisconnected("client_play.disconnect"));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
                ChunkMissingMaterialListCache.onWorldDisconnected("client.lifecycle.stopping"));
    }
}
