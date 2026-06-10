package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiSchematicPlacementsList.class, remap = false)
public abstract class GuiSchematicPlacementsListMixin {
    @Shadow
    @Final
    public SchematicPlacementManager manager;

    @Inject(method = "onSelectionChange(Lfi/dy/masa/litematica/schematic/placement/SchematicPlacement;)V", at = @At("HEAD"), cancellable = true)
    private void lmlp$selectMaterialListPlacement(SchematicPlacement placement, CallbackInfo ci) {
        if (placement == null) {
            ci.cancel();
            return;
        }

        ChunkMissingMaterialListCache.selectMaterialListPlacement(placement, "schematic_placements_list.click");
        if (ChunkMissingMaterialListCache.canEditPlacement(placement)) {
            this.manager.setSelectedSchematicPlacement(
                    placement != this.manager.getSelectedSchematicPlacement() ? placement : null);
        }
        ci.cancel();
    }
}