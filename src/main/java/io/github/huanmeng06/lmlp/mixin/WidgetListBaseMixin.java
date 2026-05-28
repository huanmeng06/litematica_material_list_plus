package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import io.github.huanmeng06.lmlp.access.WidgetListBoundsAccess;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import io.github.huanmeng06.lmlp.gui.RecipeInlineRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = WidgetListBase.class, remap = false)
public abstract class WidgetListBaseMixin implements WidgetListBoundsAccess {
    @Shadow
    protected int browserEntriesStartY;
    @Shadow
    protected int browserHeight;
    @Shadow
    protected int browserEntriesOffsetY;
    @Shadow
    protected int browserPaddingY;
    @Shadow
    @Final
    protected GuiScrollBar scrollBar;
    @Shadow
    @Final
    protected List<?> listContents;
    @Shadow
    @Final
    protected List<?> listWidgets;

    @Shadow
    protected abstract int getBrowserEntryHeightFor(Object entry);

    @Inject(method = "getBrowserEntryHeightFor", at = @At("HEAD"), cancellable = true)
    private void lmlp$getBrowserEntryHeightFor(Object entry, CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof WidgetListMaterialList) || !(entry instanceof MaterialListEntry materialEntry) || !MaterialListPlusState.isRecipeExpanded(materialEntry)) {
            return;
        }

        cir.setReturnValue(23 + RecipeInlineRenderer.getOuterHeight(MaterialListPlusState.getCachedSummaries(materialEntry)));
    }

    @Inject(method = "reCreateListEntryWidgets", at = @At("TAIL"))
    private void lmlp$updateVariableHeightScrollbarMax(CallbackInfo ci) {
        if (!((Object) this instanceof WidgetListMaterialList)) {
            return;
        }

        this.scrollBar.setMaxValue(this.lmlp$getDynamicScrollbarMaxValue());
    }

    @Override
    public int lmlp$getVisibleBottom() {
        return this.browserEntriesStartY + this.browserEntriesOffsetY + Math.max(0, this.browserHeight - this.browserPaddingY - this.browserEntriesOffsetY);
    }

    private int lmlp$getDynamicScrollbarMaxValue() {
        int size = this.listContents.size();
        if (size <= 0) {
            return 0;
        }

        int viewportHeight = Math.max(1, this.browserHeight - this.browserPaddingY - this.browserEntriesOffsetY - this.lmlp$getHeaderHeight());
        int height = 0;
        for (int index = size - 1; index >= 0; index--) {
            height += Math.max(1, this.getBrowserEntryHeightFor(this.listContents.get(index)));
            if (height > viewportHeight) {
                return Math.min(size - 1, index + 1);
            }
        }

        return 0;
    }

    private int lmlp$getHeaderHeight() {
        if (!this.listWidgets.isEmpty() && this.listWidgets.get(0) instanceof WidgetListEntryBase<?> widget && widget.getEntry() == null) {
            return widget.getHeight();
        }

        return 0;
    }
}
