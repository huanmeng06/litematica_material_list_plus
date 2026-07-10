package io.github.huanmeng06.lmlp;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;

public class LitematicaMaterialListPlus implements ClientModInitializer {
    public static final String MOD_ID = "litematica_material_list_plus";
    public static final String MOD_NAME = "Litematica Material List Plus";
    public static final String MOD_VERSION = "1.6.107+mc1.21.1";

    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
