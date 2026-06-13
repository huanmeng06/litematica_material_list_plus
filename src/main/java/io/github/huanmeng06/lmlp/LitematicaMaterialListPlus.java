package io.github.huanmeng06.lmlp;

import fi.dy.masa.malilib.event.InitializationHandler;
import io.github.huanmeng06.lmlp.gui.OriginMarkerHudLabelRenderer;
import net.fabricmc.api.ClientModInitializer;

public class LitematicaMaterialListPlus implements ClientModInitializer {
    public static final String MOD_ID = "litematica_material_list_plus";
    public static final String MOD_NAME = "Litematica Material List Plus";
    public static final String MOD_VERSION = "1.6.0+mc1.21.5";

    @Override
    public void onInitializeClient() {
        OriginMarkerHudLabelRenderer.register();
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
