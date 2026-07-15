package io.github.huanmeng06.lmlp.recipe.jei;

import java.util.Optional;

import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

@JeiPlugin
public final class JeiRuntimeBridge implements IModPlugin {
    private static volatile IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("litematica_material_list_plus", "jei_bridge");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        MaterialListPlusState.clearRecipeCaches();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        MaterialListPlusState.clearRecipeCaches();
    }

    public static Optional<IJeiRuntime> runtime() {
        return Optional.ofNullable(runtime);
    }
}
