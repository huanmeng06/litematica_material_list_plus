package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.materials.MaterialListEntry;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(targets = "fi.dy.masa.litematica.materials.MaterialListBase", remap = false)
public abstract class MaterialListBaseMixin {
    @Inject(method = "setMaterialListEntries", at = @At("TAIL"))
    private void lmlp$rememberPlacementDemandCache(List<MaterialListEntry> entries, CallbackInfo ci) {
        ChunkMissingMaterialListCache.rememberIfPlacementList((fi.dy.masa.litematica.materials.MaterialListBase) (Object) this);
    }
}
