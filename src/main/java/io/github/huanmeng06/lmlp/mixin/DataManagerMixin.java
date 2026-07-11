package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import io.github.huanmeng06.lmlp.material.MaterialListHudState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DataManager.class, remap = false)
public abstract class DataManagerMixin {
    /**
     * Litematica's setMaterialList force-toggles the previous list's HUD off
     * whenever the current material list is replaced (without even checking
     * that the list actually changed). This mod's cache layer calls
     * setMaterialList on every GUI open, page switch and refresh, so that
     * cleanup would turn the HUD off constantly. Returning false here skips
     * the cleanup branch entirely; the HUD toggle only changes on user input,
     * and the TAIL inject below keeps InfoHud registered correctly.
     */
    @Redirect(
            method = "setMaterialList",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/materials/MaterialListHudRenderer;getShouldRenderCustom()Z"))
    private static boolean lmlp$skipHudForceOff(MaterialListHudRenderer renderer) {
        return false;
    }

    @Inject(method = "setMaterialList", at = @At("TAIL"))
    private static void lmlp$syncHudRendererToCurrentList(MaterialListBase materialList, CallbackInfo ci) {
        MaterialListHudState.syncCurrentList(materialList);
    }
}
