package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.KnownPlacementRow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiSchematicPlacementsList.class, remap = false)
public abstract class GuiSchematicPlacementsListMixin {
    @Inject(method = "onSelectionChange(Ljava/lang/Object;)V", at = @At("HEAD"), cancellable = true)
    private void lmlp$selectMaterialListRow(Object selected, CallbackInfo ci) {
        if (!(selected instanceof KnownPlacementRow row)) {
            return;
        }

        if (row.isHeader()) {
            KnownPlacementRows.toggle(row.pageId(), row.dimension());
        } else if (row.isPlacement()) {
            ChunkMissingMaterialListCache.selectMaterialListContext(row.context().key(), "schematic_placements_list.selection_row");
        }
        ci.cancel();
    }

    @Inject(method = "onSelectionChange(Lfi/dy/masa/litematica/schematic/placement/SchematicPlacement;)V", at = @At("HEAD"), cancellable = true)
    private void lmlp$selectMaterialListPlacement(SchematicPlacement placement, CallbackInfo ci) {
        if (placement == null) {
            ci.cancel();
            return;
        }

        ChunkMissingMaterialListCache.selectMaterialListPlacement(placement, "schematic_placements_list.click");
        ci.cancel();
    }
}
