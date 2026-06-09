package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "fi.dy.masa.litematica.gui.GuiMaterialList$ButtonListener", remap = false)
public abstract class GuiMaterialListButtonListenerMixin {
    @Redirect(
            method = "actionPerformedWithButton",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/materials/MaterialListBase;setMaterialListType(Lfi/dy/masa/litematica/util/BlockInfoListType;)V"))
    private void lmlp$cycleMaterialListDisplayType(MaterialListBase materialList, BlockInfoListType ignoredType, ButtonBase button, int mouseButton) {
        MinimalSubMaterialListView.cycle(materialList, mouseButton == 0);
    }

    @Redirect(
            method = "actionPerformedWithButton",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/materials/MaterialListBase;reCreateMaterialList()V"))
    private void lmlp$refreshChunkMissingMaterialList(MaterialListBase materialList) {
        if (ChunkMissingMaterialListCache.refreshWithLiveScanIfLoaded(materialList)) {
            return;
        }

        materialList.reCreateMaterialList();
    }
}
