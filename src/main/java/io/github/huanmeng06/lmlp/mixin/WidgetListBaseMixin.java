package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import io.github.huanmeng06.lmlp.access.WidgetMaterialListAccess;
import io.github.huanmeng06.lmlp.access.WidgetListBoundsAccess;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.CountDisplayStyle;
import io.github.huanmeng06.lmlp.gui.MaterialListColumnLayout;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import io.github.huanmeng06.lmlp.gui.RecipeInlineRenderer;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = WidgetListBase.class, remap = false)
public abstract class WidgetListBaseMixin implements WidgetListBoundsAccess {
    private int lmlp$lastLayoutMultiplier = Integer.MIN_VALUE;
    private int lmlp$lastLayoutEntryCount = -1;
    private String lmlp$lastLayoutSignature = "";
    private CountDisplayStyle lmlp$lastLayoutCountDisplayStyle;
    private boolean lmlp$lastLayoutMinimalSubMaterialView;
    private long lmlp$lastLayoutMinimalSubMaterialRevision = Long.MIN_VALUE;

    @Shadow
    protected int totalWidth;
    @Shadow
    protected int browserEntriesStartY;
    @Shadow
    protected int browserHeight;
    @Shadow
    protected int browserWidth;
    @Shadow
    protected int browserEntryWidth;
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
    @Shadow
    public abstract void refreshEntries();

    @Inject(method = "drawContents", at = @At("HEAD"))
    private void lmlp$refreshAnimatedRecipeExpansion(class_332 drawContext, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!((Object) this instanceof WidgetListMaterialList)) {
            return;
        }

        if ((Object) this instanceof WidgetMaterialListAccess access && MinimalSubMaterialListView.tick(access.lmlp$getMaterialList())) {
            this.refreshEntries();
        }

        this.lmlp$refreshMaterialListColumnLayoutIfNeeded(true);

        boolean hadActiveAnimations = MaterialListPlusState.hasActiveAnimations();
        boolean hadActiveColumnAnimation = MaterialListColumnLayout.hasActiveAnimation();
        MaterialListPlusState.pruneAnimations();
        if (hadActiveAnimations || MaterialListPlusState.hasActiveAnimations() || hadActiveColumnAnimation || MaterialListColumnLayout.hasActiveAnimation()) {
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

    @Inject(
            method = "refreshBrowserEntries",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/gui/widgets/WidgetListBase;reCreateListEntryWidgets()V"))
    private void lmlp$fitMaterialListColumnsBeforeRecreate(CallbackInfo ci) {
        if (!((Object) this instanceof WidgetListMaterialList)) {
            return;
        }

        this.lmlp$refreshMaterialListColumnLayoutIfNeeded(false);
    }

    @Inject(method = "reCreateListEntryWidgets", at = @At("TAIL"))
    private void lmlp$updateVariableHeightScrollbarMax(CallbackInfo ci) {
        if (!((Object) this instanceof WidgetListMaterialList)) {
            return;
        }

        this.scrollBar.setMaxValue(this.lmlp$getDynamicScrollbarMaxValue());
    }

    @Inject(method = "reCreateListEntryWidgets", at = @At("HEAD"))
    private void lmlp$expandMaterialListWidth(CallbackInfo ci) {
        if (!((Object) this instanceof WidgetListMaterialList)) {
            return;
        }

        this.browserWidth = Math.max(this.totalWidth, MaterialListColumnLayout.requiredEntryWidth() + 14);
        this.browserEntryWidth = this.browserWidth - 14;
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

    private void lmlp$refreshMaterialListColumnLayoutIfNeeded(boolean recreateAfterUpdate) {
        if (!((Object) this instanceof WidgetMaterialListAccess access)) {
            return;
        }

        int multiplier = access.lmlp$getMaterialList().getMultiplier();
        boolean minimalSubMaterialView = MinimalSubMaterialListView.isActive(access.lmlp$getMaterialList());
        long minimalSubMaterialRevision = MinimalSubMaterialListView.layoutRevision();
        CountDisplayStyle style = (CountDisplayStyle) Configs.Generic.COUNT_DISPLAY_STYLE.getOptionListValue();
        List<MaterialListEntry> entries = this.lmlp$currentMaterialEntries();
        int entryCount = entries.size();
        String signature = this.lmlp$layoutSignature(entries);
        if (multiplier == this.lmlp$lastLayoutMultiplier
                && entryCount == this.lmlp$lastLayoutEntryCount
                && signature.equals(this.lmlp$lastLayoutSignature)
                && style == this.lmlp$lastLayoutCountDisplayStyle
                && minimalSubMaterialView == this.lmlp$lastLayoutMinimalSubMaterialView
                && minimalSubMaterialRevision == this.lmlp$lastLayoutMinimalSubMaterialRevision) {
            return;
        }

        MaterialListColumnLayout.setAnimateShrinkForNextUpdate(this.lmlp$shouldAnimateColumnLayout(multiplier, style, minimalSubMaterialView));
        WidgetMaterialListEntry.setMaxNameLength(entries, multiplier);
        this.lmlp$lastLayoutMultiplier = multiplier;
        this.lmlp$lastLayoutEntryCount = entryCount;
        this.lmlp$lastLayoutSignature = signature;
        this.lmlp$lastLayoutCountDisplayStyle = style;
        this.lmlp$lastLayoutMinimalSubMaterialView = minimalSubMaterialView;
        this.lmlp$lastLayoutMinimalSubMaterialRevision = minimalSubMaterialRevision;
        if (recreateAfterUpdate) {
            this.reCreateListEntryWidgets();
        }
    }

    private boolean lmlp$shouldAnimateColumnLayout(int multiplier, CountDisplayStyle style, boolean minimalSubMaterialView) {
        return this.lmlp$lastLayoutEntryCount > 0
                && multiplier == this.lmlp$lastLayoutMultiplier
                && style == this.lmlp$lastLayoutCountDisplayStyle
                && minimalSubMaterialView == this.lmlp$lastLayoutMinimalSubMaterialView;
    }

    private List<MaterialListEntry> lmlp$currentMaterialEntries() {
        List<MaterialListEntry> entries = new ArrayList<>();
        for (Object entry : this.listContents) {
            if (entry instanceof MaterialListEntry materialEntry) {
                entries.add(materialEntry);
            }
        }
        return entries;
    }

    private String lmlp$layoutSignature(List<MaterialListEntry> entries) {
        StringBuilder builder = new StringBuilder();
        for (MaterialListEntry entry : entries) {
            builder.append(MinimalSubMaterialListView.widestDisplayName(entry))
                    .append(':')
                    .append(entry.getCountTotal())
                    .append(':')
                    .append(entry.getCountMissing())
                    .append(':')
                    .append(entry.getCountAvailable())
                    .append('|');
        }
        return builder.toString();
    }
}
