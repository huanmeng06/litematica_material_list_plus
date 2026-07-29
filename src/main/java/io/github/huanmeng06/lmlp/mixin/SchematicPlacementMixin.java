package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import io.github.huanmeng06.lmlp.access.SchematicPlacementMaterialListAccess;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SchematicPlacement.class, remap = false)
public abstract class SchematicPlacementMixin implements SchematicPlacementMaterialListAccess {
    @Shadow
    private MaterialListBase materialList;

    @Unique
    private boolean lmlp$enabledBeforeSet;

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

    @Inject(method = "setEnabled", at = @At("HEAD"))
    private void lmlp$captureEnabledState(boolean enabled, CallbackInfo ci) {
        this.lmlp$enabledBeforeSet = ((SchematicPlacement) (Object) this).isEnabled();
    }

    @Inject(method = "setEnabled", at = @At("TAIL"))
    private void lmlp$rebuildWhenEnabled(boolean enabled, CallbackInfo ci) {
        if (!enabled || this.lmlp$enabledBeforeSet) {
            return;
        }

        SchematicPlacement placement = (SchematicPlacement) (Object) this;
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        if (manager != null && manager.getAllSchematicsPlacements().contains(placement)) {
            manager.markChunksForRebuild(placement);
            manager.setVisibleSubChunksNeedsUpdate();
        }
    }
}
