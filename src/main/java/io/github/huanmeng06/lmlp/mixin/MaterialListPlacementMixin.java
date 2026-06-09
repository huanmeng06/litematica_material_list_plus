package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MaterialListPlacement.class, remap = false)
public abstract class MaterialListPlacementMixin implements MaterialListPlacementAccess {
    @Shadow
    @Final
    private SchematicPlacement placement;

    @Override
    public SchematicPlacement lmlp$getPlacement() {
        return this.placement;
    }

    @Inject(method = "reCreateMaterialList", at = @At("HEAD"), cancellable = true)
    private void lmlp$useSchematicCacheWhenChunksMissing(CallbackInfo ci) {
        if (ChunkMissingMaterialListCache.shouldUseSchematicCache(this.placement, (fi.dy.masa.litematica.materials.MaterialListBase) (Object) this)) {
            ChunkMissingMaterialListCache.refreshPlacementList(this.placement, (fi.dy.masa.litematica.materials.MaterialListBase) (Object) this);
            ci.cancel();
        }
    }
}
