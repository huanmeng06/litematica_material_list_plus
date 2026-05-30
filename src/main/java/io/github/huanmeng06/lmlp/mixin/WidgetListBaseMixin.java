package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import io.github.huanmeng06.lmlp.access.WidgetListBoundsAccess;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import io.github.huanmeng06.lmlp.gui.RecipeInlineRenderer;
import net.minecraft.class_332;
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
    @Shadow
    protected abstract void reCreateListEntryWidgets();

    @Inject(method = "drawContents", at = @At("HEAD"))
    private void lmlp$refreshAnimatedRecipeExpansion(class_332 drawContext, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!((Object) this instanceof WidgetListMaterialList)) {
            return;
        }

        boolean hadActiveAnimations = MaterialListPlusState.hasActiveAnimations();
        MaterialListPlusState.pruneAnimations();
        if (hadActiveAnimations || MaterialListPlusState.hasActiveAnimations()) {
            this.reCreateListEntryWidgets();
        }
    }

    @Inject(method = "getBrowserEntryHeightFor", at = @At("HEAD"), cancellable = true)
    private void lmlp$getBrowserEntryHeightFor(Object entry, CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof WidgetListMaterialList) || !(entry instanceof MaterialListEntry materialEntry) || !MaterialListPlusState.isRecipeVisible(materialEntry)) {
            return;
        }

        int visibleOuterHeight = RecipeInlineRenderer.getOuterHeight(MaterialListPlusState.getCachedSummaries(materialEntry), MaterialListPlusState.recipeProgress(materialEntry));
        cir.setReturnValue(23 + visibleOuterHeight);
    }

    @Inject(method = "reCreateListEntryWidgets", at = @At("TAIL"))
    private void lmlp$updateVariableHeightScrollbarMax(CallbackInfo ci) {
        if (!((Object) this instanceof WidgetListMaterialList)) {
            return;
        }

        this.scrollBar.setMaxValue(this.lmlp$getDynamicScrollbarMaxValue());
    }

    @Override
    public int lmlp$getVisibleTop() {
        return this.browserEntriesStartY + this.browserEntriesOffsetY;
    }

    @Override
    public int lmlp$getVisibleBottom() {
        return this.lmlp$getVisibleTop() + Math.max(0, this.browserHeight - this.browserPaddingY - this.browserEntriesOffsetY);
    }

    @Override
    public void lmlp$scrollEntryIntoView(Object entry, int bottomPadding) {
        if (!((Object) this instanceof WidgetListMaterialList)) {
            return;
        }

        int targetIndex = this.listContents.indexOf(entry);
        if (targetIndex < 0) {
            return;
        }

        int viewportHeight = this.lmlp$getContentViewportHeight(bottomPadding);
        if (this.lmlp$isEntryBottomVisible(targetIndex, viewportHeight)) {
            return;
        }

        int startIndex = this.lmlp$getBestStartIndexFor(targetIndex, viewportHeight);
        this.scrollBar.setMaxValue(this.lmlp$getDynamicScrollbarMaxValue());
        this.scrollBar.setValue(startIndex);
    }

    private int lmlp$getDynamicScrollbarMaxValue() {
        int size = this.listContents.size();
        if (size <= 0) {
            return 0;
        }

        int viewportHeight = this.lmlp$getContentViewportHeight(0);
        int height = 0;
        for (int index = size - 1; index >= 0; index--) {
            height += Math.max(1, this.lmlp$getScrollTargetEntryHeightFor(this.listContents.get(index)));
            if (height > viewportHeight) {
                return Math.min(size - 1, index + 1);
            }
        }

        return 0;
    }

    private boolean lmlp$isEntryBottomVisible(int targetIndex, int viewportHeight) {
        int currentIndex = this.scrollBar.getValue();
        if (currentIndex > targetIndex) {
            return false;
        }

        int height = 0;
        for (int index = currentIndex; index <= targetIndex; index++) {
            height += Math.max(1, this.lmlp$getScrollTargetEntryHeightFor(this.listContents.get(index)));
            if (height > viewportHeight) {
                return false;
            }
        }

        return true;
    }

    private int lmlp$getBestStartIndexFor(int targetIndex, int viewportHeight) {
        int height = Math.max(1, this.lmlp$getScrollTargetEntryHeightFor(this.listContents.get(targetIndex)));
        int startIndex = targetIndex;

        if (height >= viewportHeight) {
            return startIndex;
        }

        for (int index = targetIndex - 1; index >= 0; index--) {
            int candidateHeight = height + Math.max(1, this.lmlp$getScrollTargetEntryHeightFor(this.listContents.get(index)));
            if (candidateHeight > viewportHeight) {
                break;
            }

            height = candidateHeight;
            startIndex = index;
        }

        return startIndex;
    }

    private int lmlp$getContentViewportHeight(int bottomPadding) {
        return Math.max(1, this.browserHeight - this.browserPaddingY - this.browserEntriesOffsetY - this.lmlp$getHeaderHeight() - bottomPadding);
    }

    private int lmlp$getScrollTargetEntryHeightFor(Object entry) {
        if (entry instanceof MaterialListEntry materialEntry && MaterialListPlusState.isRecipeExpanded(materialEntry)) {
            return 23 + RecipeInlineRenderer.getTargetOuterHeight(MaterialListPlusState.getCachedSummaries(materialEntry));
        }

        return this.getBrowserEntryHeightFor(entry);
    }

    private int lmlp$getHeaderHeight() {
        if (!this.listWidgets.isEmpty() && this.listWidgets.get(0) instanceof WidgetListEntryBase<?> widget && widget.getEntry() == null) {
            return widget.getHeight();
        }

        return 0;
    }
}
