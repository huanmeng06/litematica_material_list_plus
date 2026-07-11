package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.materials.MaterialListEntry;
import io.github.huanmeng06.lmlp.access.MaterialListSourceAccess;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.MaterialListDataSource;
import io.github.huanmeng06.lmlp.material.MaterialListHudState;
import io.github.huanmeng06.lmlp.material.WaterBucketIceSubstitution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(targets = "fi.dy.masa.litematica.materials.MaterialListBase", remap = false)
public abstract class MaterialListBaseMixin implements MaterialListSourceAccess {
    @Unique
    private MaterialListDataSource lmlp$dataSource = MaterialListDataSource.UNKNOWN;

    @Override
    public MaterialListDataSource lmlp$getDataSource() {
        return this.lmlp$dataSource;
    }

    @Override
    public void lmlp$setDataSource(MaterialListDataSource dataSource) {
        this.lmlp$dataSource = dataSource;
    }

    @ModifyVariable(method = "setMaterialListEntries", at = @At("HEAD"), argsOnly = true)
    private List<MaterialListEntry> lmlp$applyMaterialSubstitutions(List<MaterialListEntry> entries) {
        return WaterBucketIceSubstitution.apply(entries);
    }

    @Inject(method = "setMaterialListEntries", at = @At("TAIL"))
    private void lmlp$rememberPlacementDemandCache(List<MaterialListEntry> entries, CallbackInfo ci) {
        if (ChunkMissingMaterialListCache.isApplyingSchematicCache()) {
            this.lmlp$setDataSource(ChunkMissingMaterialListCache.applyingCacheSource());
            return;
        }

        this.lmlp$setDataSource(MaterialListDataSource.WORLD_SCAN);
        ChunkMissingMaterialListCache.rememberIfPlacementList((fi.dy.masa.litematica.materials.MaterialListBase) (Object) this);
        ChunkMissingMaterialListCache.markLiveScanCompleted((fi.dy.masa.litematica.materials.MaterialListBase) (Object) this);
        MaterialListHudState.resyncAfterScan((fi.dy.masa.litematica.materials.MaterialListBase) (Object) this);
    }
}
