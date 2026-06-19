package io.github.huanmeng06.lmlp;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.Hotkeys;
import io.github.huanmeng06.lmlp.event.InputHandler;
import io.github.huanmeng06.lmlp.gui.GuiConfigs;
import io.github.huanmeng06.lmlp.gui.PlacementOriginMarker;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class InitHandler implements IInitializationHandler {
    private static boolean openConfigHotkeyWasDown;
    private static boolean clearOriginMarkerHotkeyWasDown;

    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(LitematicaMaterialListPlus.MOD_ID, new Configs());
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        Hotkeys.OPEN_CONFIG_GUI.getKeybind().setCallback((action, key) -> {
            GuiBase.openGui(new GuiConfigs());
            return true;
        });
        Hotkeys.CLEAR_ORIGIN_MARKER.getKeybind().setCallback((action, key) -> {
            PlacementOriginMarker.clear();
            return true;
        });
        ClientTickEvents.END_CLIENT_TICK.register(InitHandler::handleHotkeyFallback);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ChunkMissingMaterialListCache.onWorldJoined(client, "client_play.join"));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PlacementOriginMarker.clear();
            ChunkMissingMaterialListCache.onWorldDisconnected("client_play.disconnect");
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            PlacementOriginMarker.clear();
            ChunkMissingMaterialListCache.onWorldDisconnected("client.lifecycle.stopping");
        });
        WorldRenderEvents.LAST.register(PlacementOriginMarker::render);
    }
    private static void handleHotkeyFallback(net.minecraft.class_310 client) {
        boolean openConfigDown = isHotkeyDown(client, Hotkeys.OPEN_CONFIG_GUI.getKeybind().getKeys());
        if (openConfigDown && !openConfigHotkeyWasDown) {
            GuiBase.openGui(new GuiConfigs());
        }
        openConfigHotkeyWasDown = openConfigDown;

        boolean clearMarkerDown = isHotkeyDown(client, Hotkeys.CLEAR_ORIGIN_MARKER.getKeybind().getKeys());
        if (clearMarkerDown && !clearOriginMarkerHotkeyWasDown) {
            PlacementOriginMarker.clear();
        }
        clearOriginMarkerHotkeyWasDown = clearMarkerDown;
    }

    private static boolean isHotkeyDown(net.minecraft.class_310 client, List<Integer> keys) {
        if (client == null || client.method_22683() == null || keys == null || keys.isEmpty()) {
            return false;
        }

        long handle = client.method_22683().method_4490();
        for (int key : keys) {
            if (key <= 0 || GLFW.glfwGetKey(handle, key) != GLFW.GLFW_PRESS) {
                return false;
            }
        }
        return true;
    }
}
