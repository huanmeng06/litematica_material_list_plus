package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SchematicPlacement.class, remap = false)
public abstract class SchematicPlacementMixin {
    @Shadow
    private MaterialListBase materialList;

    @Inject(method = "getMaterialList", at = @At("HEAD"), cancellable = true)
    private void lmlp$getChunkMissingMaterialList(CallbackInfoReturnable<MaterialListBase> cir) {
        SchematicPlacement placement = (SchematicPlacement) (Object) this;
        if (ChunkMissingMaterialListCache.shouldUseSchematicCache(placement, this.materialList)) {
            cir.setReturnValue(ChunkMissingMaterialListCache.getOrCreate(placement, this.materialList));
        }
    }
}
