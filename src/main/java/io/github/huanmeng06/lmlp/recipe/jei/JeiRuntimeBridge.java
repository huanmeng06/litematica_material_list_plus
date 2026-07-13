package io.github.huanmeng06.lmlp.recipe.jei;

import java.util.Optional;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.class_2960;

@JeiPlugin
public final class JeiRuntimeBridge implements IModPlugin {
    private static volatile IJeiRuntime runtime;

    @Override
    public class_2960 getPluginUid() {
        return new class_2960("litematica_material_list_plus", "jei_bridge");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static Optional<IJeiRuntime> runtime() {
        return Optional.ofNullable(runtime);
    }
}
