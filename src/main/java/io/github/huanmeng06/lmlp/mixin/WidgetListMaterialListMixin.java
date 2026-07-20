package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import io.github.huanmeng06.lmlp.access.WidgetMaterialListAccess;
import io.github.huanmeng06.lmlp.gui.IgnoredMaterialRegistry;
import io.github.huanmeng06.lmlp.gui.MaterialListSortState;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Comparator;

@Mixin(value = WidgetListMaterialList.class, remap = false)
public abstract class WidgetListMaterialListMixin implements WidgetMaterialListAccess {
    @Shadow
    @Final
    private GuiMaterialList gui;

    @Override
    public MaterialListBase lmlp$getMaterialList() {
        return this.gui.getMaterialList();
    }

    @Inject(method = "getAllEntries", at = @At("HEAD"), cancellable = true)
    private void lmlp$useMinimalSubMaterialEntries(CallbackInfoReturnable<Collection<MaterialListEntry>> cir) {
        MaterialListBase materialList = this.gui.getMaterialList();
        if (MinimalSubMaterialListView.isActive(materialList)) {
            cir.setReturnValue(MinimalSubMaterialListView.entries(materialList));
        }
    }

    @Inject(method = "getAllEntries", at = @At("RETURN"), cancellable = true)
    private void lmlp$filterStableIgnoredMaterials(CallbackInfoReturnable<Collection<MaterialListEntry>> cir) {
        MaterialListBase materialList = this.gui.getMaterialList();
        if (MinimalSubMaterialListView.isActive(materialList)) {
            return;
        }

        Collection<MaterialListEntry> entries = cir.getReturnValue();
        if (entries == null || entries.isEmpty()) {
            return;
        }

        Collection<MaterialListEntry> filtered = IgnoredMaterialRegistry.filter(materialList, entries);
        if (filtered != entries) {
            cir.setReturnValue(filtered);
        }
    }

    // Vanilla's MaterialListSorter compares raw entry.getCountMissing(), a
    // value frozen when the list was built. The "缺少" column actually shown
    // to the player is netMissing (total*multiplier - available), which
    // keeps shrinking as items are collected. Sorting by the stale raw value
    // while displaying the live net value makes rows appear out of order
    // once any progress has been made, so mirror vanilla's COUNT_MISSING
    // comparator logic but drive it off the same net value that is rendered.
    @Inject(method = "getComparator", at = @At("HEAD"), cancellable = true)
    private void lmlp$fixNetMissingSortComparator(CallbackInfoReturnable<Comparator<MaterialListEntry>> cir) {
        MaterialListBase materialList = this.gui.getMaterialList();
        if (MinimalSubMaterialListView.isActive(materialList)
                && MaterialListSortState.isCompatibleSort(materialList)) {
            boolean reverse = materialList.getSortInReverse();
            cir.setReturnValue((a, b) -> {
                int compatibleA = MinimalSubMaterialListView.compatibleCount(a);
                int compatibleB = MinimalSubMaterialListView.compatibleCount(b);
                if (compatibleA != compatibleB) {
                    int result = Integer.compare(compatibleB, compatibleA);
                    return reverse ? -result : result;
                }

                return a.getStack().getHoverName().getString()
                        .compareTo(b.getStack().getHoverName().getString());
            });
            return;
        }

        if (materialList.getSortCriteria() != MaterialListBase.SortCriteria.COUNT_MISSING) {
            return;
        }

        boolean reverse = materialList.getSortInReverse();
        cir.setReturnValue((a, b) -> {
            int missingA = MinimalSubMaterialListView.netMissing(a, materialList);
            int missingB = MinimalSubMaterialListView.netMissing(b, materialList);
            int nameCmp = a.getStack().getHoverName().getString().compareTo(b.getStack().getHoverName().getString());
            if (missingA == missingB) {
                return nameCmp;
            }

            boolean aGreater = missingA > missingB;
            return aGreater == reverse ? 1 : -1;
        });
    }
}
