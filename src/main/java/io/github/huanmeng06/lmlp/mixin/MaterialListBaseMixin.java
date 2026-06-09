package io.github.huanmeng06.lmlp.mixin;

import java.util.List;

import fi.dy.masa.litematica.materials.MaterialListEntry;
import io.github.huanmeng06.lmlp.cache.PlacementMaterialListCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = fi.dy.masa.litematica.materials.MaterialListBase.class, remap = false)
public abstract class MaterialListBaseMixin {
    @Inject(method = "setMaterialListEntries", at = @At("TAIL"))
    private void lmlp$rememberSuccessfulPlacementList(List<MaterialListEntry> list, CallbackInfo ci) {
        PlacementMaterialListCache.rememberIfPlacementList((fi.dy.masa.litematica.materials.MaterialListBase) (Object) this);
    }
}
