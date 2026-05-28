package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import io.github.huanmeng06.lmlp.access.WidgetListBoundsAccess;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import io.github.huanmeng06.lmlp.gui.NestedMaterialTreeRenderer;
import io.github.huanmeng06.lmlp.gui.RecipeInlineRenderer;
import io.github.huanmeng06.lmlp.recipe.MaterialTreeNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WidgetListBase.class, remap = false)
public abstract class WidgetListBaseMixin implements WidgetListBoundsAccess {
    @Shadow
    protected int browserEntriesStartY;
    @Shadow
    protected int browserHeight;

    @Inject(method = "getBrowserEntryHeightFor", at = @At("HEAD"), cancellable = true)
    private void lmlp$getBrowserEntryHeightFor(Object entry, CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof WidgetListMaterialList) || !(entry instanceof MaterialListEntry materialEntry) || !MaterialListPlusState.isExpanded(materialEntry)) {
            return;
        }

        if (MaterialListPlusState.isRecipeExpanded(materialEntry)) {
            cir.setReturnValue(23 + RecipeInlineRenderer.getOuterHeight(MaterialListPlusState.getCachedSummaries(materialEntry)));
            return;
        }

        MaterialTreeNode root = MaterialListPlusState.getCachedTreeRoot(materialEntry);
        if (root != null) {
            cir.setReturnValue(23 + NestedMaterialTreeRenderer.getOuterHeight(root, MaterialListPlusState.getExpandedTreeNodes()));
        }
    }

    @Override
    public int lmlp$getVisibleBottom() {
        return this.browserEntriesStartY + this.browserHeight;
    }
}
