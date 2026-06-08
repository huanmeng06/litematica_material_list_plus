package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import io.github.huanmeng06.lmlp.access.WidgetMaterialListAccess;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

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
}
