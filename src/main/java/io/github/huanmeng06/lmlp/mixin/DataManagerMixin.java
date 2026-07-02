package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import io.github.huanmeng06.lmlp.material.MaterialListHudState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DataManager.class, remap = false)
public abstract class DataManagerMixin {
    @Inject(method = "setMaterialList", at = @At("TAIL"))
    private static void lmlp$syncHudRendererToCurrentList(MaterialListBase materialList, CallbackInfo ci) {
        MaterialListHudState.syncCurrentList(materialList);
    }
}
