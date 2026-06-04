package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import io.github.huanmeng06.lmlp.access.WidgetMaterialListAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = WidgetListMaterialList.class, remap = false)
public abstract class WidgetListMaterialListMixin implements WidgetMaterialListAccess {
    @Shadow
    @Final
    private GuiMaterialList gui;

    @Override
    public MaterialListBase lmlp$getMaterialList() {
        return this.gui.getMaterialList();
    }
}
