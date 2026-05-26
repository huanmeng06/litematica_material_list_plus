package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import io.github.huanmeng06.lmlp.gui.RecipeInlineRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WidgetListBase.class, remap = false)
public abstract class WidgetListBaseMixin {
    @Inject(method = "getBrowserEntryHeightFor", at = @At("HEAD"), cancellable = true)
    private void lmlp$getBrowserEntryHeightFor(Object entry, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof WidgetListMaterialList && entry instanceof MaterialListEntry materialEntry && MaterialListPlusState.isExpanded(materialEntry)) {
            cir.setReturnValue(22 + RecipeInlineRenderer.getHeight(MaterialListPlusState.getCachedSummaries(materialEntry)) + 4);
        }
    }
}
