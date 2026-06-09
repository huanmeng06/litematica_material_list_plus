package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import io.github.huanmeng06.lmlp.access.SchematicPlacementMaterialListAccess;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SchematicPlacement.class, remap = false)
public abstract class SchematicPlacementMixin implements SchematicPlacementMaterialListAccess {
    @Shadow
    private MaterialListBase materialList;

    @Override
    public MaterialListBase lmlp$getMaterialList() {
        return this.materialList;
    }

    @Override
    public void lmlp$setMaterialList(MaterialListBase materialList) {
        this.materialList = materialList;
    }

    @Inject(method = "getMaterialList", at = @At("HEAD"), cancellable = true)
    private void lmlp$getChunkMissingMaterialList(CallbackInfoReturnable<MaterialListBase> cir) {
        SchematicPlacement placement = (SchematicPlacement) (Object) this;
        if (ChunkMissingMaterialListCache.shouldUseSchematicCache(placement, this.materialList)) {
            cir.setReturnValue(ChunkMissingMaterialListCache.getOrCreate(placement, this.materialList));
        } else if (this.materialList == null) {
            this.materialList = new MaterialListPlacement(placement);
            cir.setReturnValue(this.materialList);
        }
    }
}
