package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import io.github.huanmeng06.lmlp.access.ButtonBaseAccess;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import io.github.huanmeng06.lmlp.cache.CachedMaterialList;
import io.github.huanmeng06.lmlp.cache.PlacementMaterialListCache;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "fi.dy.masa.litematica.gui.GuiMaterialList$ButtonListener", remap = false)
public abstract class GuiMaterialListButtonListenerMixin {
    @Shadow
    @Final
    private GuiMaterialList parent;

    @Inject(method = "actionPerformedWithButton", at = @At("HEAD"), cancellable = true)
    private void lmlp$handleCachedRefresh(ButtonBase button, int mouseButton, CallbackInfo ci) {
        MaterialListBase materialList = this.parent.getMaterialList();
        String label = ((ButtonBaseAccess) button).lmlp$getDisplayString();
        String refreshLabel = fi.dy.masa.malilib.util.StringUtils.translate("litematica.gui.button.material_list.refresh_list");
        if (!refreshLabel.equals(label)) {
            return;
        }

        if (materialList instanceof CachedMaterialList cachedList) {
            this.lmlp$refreshCachedList(cachedList);
            ci.cancel();
            return;
        }

        if (materialList instanceof MaterialListPlacement && materialList instanceof MaterialListPlacementAccess access) {
            if (PlacementMaterialListCache.arePlacementChunksLoaded(access.lmlp$getPlacement())) {
                return;
            }

            MaterialListBase cachedList = PlacementMaterialListCache.getCachedOrShowMissing(access.lmlp$getPlacement());
            if (cachedList != null) {
                GuiBase.openGui(new GuiMaterialList(cachedList));
                PlacementMaterialListCache.showUsingCachedMessage();
            }
            ci.cancel();
        }
    }

    @Redirect(
            method = "actionPerformedWithButton",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/materials/MaterialListBase;setMaterialListType(Lfi/dy/masa/litematica/util/BlockInfoListType;)V"))
    private void lmlp$cycleMaterialListDisplayType(MaterialListBase materialList, BlockInfoListType ignoredType, ButtonBase button, int mouseButton) {
        MinimalSubMaterialListView.cycle(materialList, mouseButton == 0);
    }

    private void lmlp$refreshCachedList(CachedMaterialList cachedList) {
        if (cachedList.canRefreshLive()) {
            MaterialListBase liveList = PlacementMaterialListCache.refreshLive(cachedList.placement(), cachedList);
            GuiBase.openGui(new GuiMaterialList(liveList));
        } else {
            cachedList.reCreateMaterialList();
            this.parent.initGui();
        }
    }
}
