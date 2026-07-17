package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicPlacements;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.render.RenderUtils;
import io.github.huanmeng06.lmlp.access.MinimalChoiceTooltipAccess;
import io.github.huanmeng06.lmlp.access.WidgetMaterialListAccess;
import io.github.huanmeng06.lmlp.access.WidgetListBoundsAccess;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.CountDisplayStyle;
import io.github.huanmeng06.lmlp.gui.MaterialListColumnLayout;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import io.github.huanmeng06.lmlp.gui.MinimalSourceInlineRenderer;
import io.github.huanmeng06.lmlp.gui.KnownPlacementListEntry;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.KnownPlacementRow;
import io.github.huanmeng06.lmlp.gui.RecipeInlineRenderer;
import io.github.huanmeng06.lmlp.material.InventoryCounts;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import fi.dy.masa.malilib.render.GuiContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(value = WidgetListBase.class, remap = false)
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class WidgetListBaseMixin implements WidgetListBoundsAccess {
    private static final int WHEEL_SCROLL_PIXELS = 36;
    private static final int BROWSER_BOTTOM_INSET = 8;
    private static final int MINIMAL_SOURCE_PANEL_SIDE_WIDTH = 50;
    private static final int SEARCH_ICON_RESERVE = 20;

    private int lmlp$lastLayoutMultiplier = Integer.MIN_VALUE;
    private int lmlp$lastLayoutEntryCount = -1;
    private String lmlp$lastLayoutSignature = "";
    private CountDisplayStyle lmlp$lastLayoutCountDisplayStyle;
    private boolean lmlp$lastLayoutMinimalSubMaterialView;
    private long lmlp$lastLayoutMinimalSubMaterialRevision = Long.MIN_VALUE;
    private String lmlp$lastMaterialDataSignature = "";
    private double lmlp$scrollRemainder;
    // Frame-scoped memo of per-entry scroll-target heights. An entry's target
    // height is constant within a single frame (it depends on expansion state /
    // panel width, which only change via input that also re-lays the list), so
    // caching it lets the several O(N) height passes per frame (totalEntryHeight
    // twice in drawContents, plus firstVisibleEntryIndex/entryTop/loop during a
    // rebuild) compute each entry once instead of 2-5x. Cleared at the top of
    // every height-loop entry point so it never carries stale values across a
    // relayout.
    private final java.util.Map<Object, Integer> lmlp$frameEntryHeights = new java.util.IdentityHashMap<>();

    @Shadow
    @Final
    protected int posX;
    @Shadow
    @Final
    protected int posY;
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
    protected int maxVisibleBrowserEntries;
    @Shadow
    protected int lastScrollbarPosition;
    @Shadow
    protected int lastSelectedEntryIndex;
    @Shadow
    protected boolean allowKeyboardNavigation;
    @Shadow
    protected boolean allowMultiSelection;
    @Shadow
    protected WidgetSearchBar widgetSearchBar;
    @Shadow
    @Final
    protected GuiScrollBar scrollBar;
    @Shadow
    @Final
    protected List listContents;
    @Shadow
    @Final
    protected List listWidgets;
    @Shadow
    @Final
    protected Set selectedEntries;

    @Shadow
    protected abstract int getBrowserEntryHeightFor(Object entry);
    @Shadow
    protected abstract WidgetListEntryBase<?> createHeaderWidget(int x, int y, int startIndex, int listHeight, int currentHeight);
    @Shadow
    protected abstract WidgetListEntryBase<?> createListEntryWidget(int x, int y, int listIndex, boolean isOdd, Object entry);
    @Shadow
    public abstract Object getLastSelectedEntry();
    @Shadow
    public abstract void setLastSelectedEntry(Object entry, int index);
    @Shadow
    public abstract void refreshEntries();

    private void lmlp$refreshAnimatedRecipeExpansion() {
        if ((Object) this instanceof WidgetListSchematicPlacements) {
            boolean hadActiveGroupAnimations = KnownPlacementRows.hasActiveGroupAnimations();
            KnownPlacementRows.pruneGroupAnimations();
            boolean hasActiveGroupAnimations = KnownPlacementRows.hasActiveGroupAnimations();
            if (hadActiveGroupAnimations && !hasActiveGroupAnimations) {
                this.refreshEntries();
            } else if (hadActiveGroupAnimations || hasActiveGroupAnimations) {
                this.reCreateListEntryWidgets();
            }
            return;
        }

        if (!((Object) this instanceof WidgetListMaterialList)) {
            return;
        }

        if (this.lmlp$refreshMaterialDataIfNeeded()) {
            return;
        }

        if ((Object) this instanceof WidgetMaterialListAccess access && MinimalSubMaterialListView.tick(access.lmlp$getMaterialList())) {
            this.refreshEntries();
        }

        this.lmlp$refreshMaterialListColumnLayoutIfNeeded(true);

        boolean hadActiveAnimations = MaterialListPlusState.hasActiveAnimations();
        boolean hadActiveSourceAnimations = MinimalSubMaterialListView.hasActiveSourceAnimations();
        boolean hadActiveColumnAnimation = MaterialListColumnLayout.hasActiveAnimation();
        MaterialListPlusState.pruneAnimations();
        MinimalSubMaterialListView.pruneSourceAnimations();
        if (hadActiveAnimations
                || MaterialListPlusState.hasActiveAnimations()
                || hadActiveSourceAnimations
                || MinimalSubMaterialListView.hasActiveSourceAnimations()
                || hadActiveColumnAnimation
                || MaterialListColumnLayout.hasActiveAnimation()) {
            this.reCreateListEntryWidgets();
        }
    }

    private boolean lmlp$refreshMaterialDataIfNeeded() {
        if (!((Object) this instanceof WidgetMaterialListAccess access)) {
            return false;
        }

        String signature = this.lmlp$materialDataSignature(access);
        if (signature.equals(this.lmlp$lastMaterialDataSignature)) {
            return false;
        }

        this.lmlp$lastMaterialDataSignature = signature;
        this.refreshEntries();
        return true;
    }

    private String lmlp$materialDataSignature(WidgetMaterialListAccess access) {
        StringBuilder builder = new StringBuilder();
        builder.append(access.lmlp$getMaterialList().getMaterialListType().getStringValue())
                .append('|')
                .append(access.lmlp$getMaterialList().getHideAvailable())
                .append('|')
                .append(access.lmlp$getMaterialList().getMultiplier())
                .append('|')
                .append(InventoryCounts.current().signature());
        for (MaterialListEntry entry : access.lmlp$getMaterialList().getMaterialsFiltered(true)) {
            builder.append('|')
                    .append(ItemStackTexts.id(entry.getStack()))
                    .append(':')
                    .append(entry.getCountTotal())
                    .append(':')
                    .append(entry.getCountMissing())
                    .append(':')
                    .append(entry.getCountAvailable());
        }
        return builder.toString();
    }

    private void lmlp$renderPanelTooltipAfterList(GuiContext drawContext, int mouseX, int mouseY) {
        if (!((Object) this instanceof WidgetMaterialListAccess)) {
            return;
        }

        for (Object listWidget : this.listWidgets) {
            if (listWidget instanceof WidgetListEntryBase<?> widget
                    && widget.isMouseOver(mouseX, mouseY)
                    && listWidget instanceof MinimalChoiceTooltipAccess tooltip) {
                tooltip.lmlp$renderMinimalChoiceTooltip(drawContext, mouseX, mouseY);
                return;
            }
        }
    }

    @Inject(method = "onMouseScrolled", at = @At("HEAD"), cancellable = true)
    private void lmlp$scrollByPixels(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!this.lmlp$isInsideBrowser((int) mouseX, (int) mouseY)) {
            return;
        }

        double target = this.lmlp$scrollRemainder - verticalAmount * WHEEL_SCROLL_PIXELS;
        int pixels = (int) target;
        this.lmlp$scrollRemainder = target - pixels;
        if (pixels == 0 && verticalAmount != 0.0D) {
            pixels = verticalAmount > 0.0D ? -1 : 1;
            this.lmlp$scrollRemainder = 0.0D;
        }

        if (pixels != 0) {
            this.scrollBar.offsetValue(pixels);
            this.lastScrollbarPosition = this.scrollBar.getValue();
            this.reCreateListEntryWidgets();
        }
        cir.setReturnValue(true);
    }

    /**
     * @author Huan_meeng
     * @reason Treat WidgetListBase scrollbar values as pixel offsets so lists scroll continuously instead of by row index.
     */
    @Overwrite
    public void drawContents(GuiContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.lmlp$refreshAnimatedRecipeExpansion();
        this.lmlp$frameEntryHeights.clear();

        WidgetBase hovered = null;
        int viewportHeight = this.lmlp$getListViewportHeight();
        int contentHeight = Math.max(this.lmlp$totalEntryHeight(), this.lmlp$getListViewportHeight());
        int scrollbarX = this.posX + this.browserWidth - 9;
        int scrollbarY = this.browserEntriesStartY + this.browserEntriesOffsetY;
        this.scrollBar.setMaxValue(this.lmlp$getPixelScrollbarMaxValue(0));
        this.scrollBar.render(drawContext, mouseX, mouseY, partialTicks, scrollbarX, scrollbarY, 8, this.lmlp$getListViewportHeight(), contentHeight);

        if (this.scrollBar.getValue() != this.lastScrollbarPosition) {
            this.lastScrollbarPosition = this.scrollBar.getValue();
            this.reCreateListEntryWidgets();
        }

        int entriesClipTop = this.lmlp$getEntriesClipTop();
        int clipBottom = this.posY + 4 + this.browserEntriesOffsetY + viewportHeight;
        for (Object listWidget : this.listWidgets) {
            if (!(listWidget instanceof WidgetListEntryBase<?> widget)) {
                continue;
            }

            Object entry = widget.getEntry();
            boolean selected = this.allowMultiSelection
                    ? this.selectedEntries.contains(entry)
                    : entry != null && entry.equals(this.getLastSelectedEntry());
            if (entry == null) {
                widget.render(drawContext, mouseX, mouseY, selected);
            } else if (clipBottom > entriesClipTop) {
                drawContext.method_44379(this.posX, entriesClipTop, this.posX + this.browserWidth, clipBottom);
                widget.render(drawContext, mouseX, mouseY, selected);
                drawContext.method_44380();
            }

            if (widget.isMouseOver(mouseX, mouseY)) {
                hovered = widget;
            }
        }

        if (this.widgetSearchBar != null) {
            this.widgetSearchBar.render(drawContext, mouseX, mouseY, false);
        }

        if (hovered == null && this.widgetSearchBar != null && this.widgetSearchBar.isMouseOver(mouseX, mouseY)) {
            hovered = this.widgetSearchBar;
        }

        ((GuiBaseHoverAccess) this).lmlp$setHoveredWidget(hovered);
        this.lmlp$renderPanelTooltipAfterList(drawContext, mouseX, mouseY);
    }

    @Inject(method = "getBrowserEntryHeightFor", at = @At("HEAD"), cancellable = true)
    private void lmlp$getBrowserEntryHeightFor(Object entry, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof WidgetListSchematicPlacements && entry instanceof KnownPlacementRow row) {
            cir.setReturnValue(KnownPlacementRows.rowHeight(row));
            return;
        }

        if ((Object) this instanceof WidgetListSchematicPlacements && entry instanceof SchematicPlacement) {
            cir.setReturnValue(KnownPlacementListEntry.rowHeight());
            return;
        }

        if (!((Object) this instanceof WidgetListMaterialList) || !(entry instanceof MaterialListEntry materialEntry)) {
            return;
        }

        boolean minimalSubMaterialView = (Object) this instanceof WidgetMaterialListAccess access && MinimalSubMaterialListView.isActive(access.lmlp$getMaterialList());
        if (minimalSubMaterialView && MinimalSubMaterialListView.isSourcesVisible(materialEntry)) {
            boolean showAllSources = MinimalSubMaterialListView.isSourcesFull(materialEntry);
            int total = (Object) this instanceof WidgetMaterialListAccess access ? MinimalSubMaterialListView.total(materialEntry, access.lmlp$getMaterialList()) : materialEntry.getCountTotal();
            int missing = (Object) this instanceof WidgetMaterialListAccess access ? MinimalSubMaterialListView.netMissing(materialEntry, access.lmlp$getMaterialList()) : materialEntry.getCountMissing();
            int visibleOuterHeight = MinimalSourceInlineRenderer.getOuterHeight(
                    MinimalSubMaterialListView.displayStack(materialEntry),
                    MinimalSubMaterialListView.sourceRequirements(materialEntry, total, missing),
                    MinimalSubMaterialListView.sourceContributions(materialEntry),
                    showAllSources,
                    this.lmlp$getMinimalSourcePanelWidthForHeight(),
                    MinimalSubMaterialListView.sourceProgress(materialEntry));
            cir.setReturnValue(23 + visibleOuterHeight);
        } else if (MaterialListPlusState.isRecipeVisible(materialEntry)) {
            int visibleOuterHeight = RecipeInlineRenderer.getOuterHeight(MaterialListPlusState.getCachedSummaries(materialEntry), this.lmlp$getRecipePanelWidthForHeight(), MaterialListPlusState.recipeProgress(materialEntry));
            cir.setReturnValue(23 + visibleOuterHeight);
        }
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

    /**
     * @author Huan_meeng
     * @reason Build visible row widgets from a pixel scroll offset, allowing partially visible top and bottom rows.
     */
    @Overwrite
    protected void reCreateListEntryWidgets() {
        if (!((Object) this instanceof WidgetListMaterialList)) {
            this.lmlp$reCreateListEntryWidgetsByPixels();
            return;
        }

        // Reserve room for the search icon floating over the header row's
        // top-right corner so a fully-fit column layout doesn't sit under it.
        MaterialListColumnLayout.updateAvailableEntryWidth(this.totalWidth - 14 - SEARCH_ICON_RESERVE);
        // Cap the expanded minimal-source panel's width demand at the viewport:
        // growing browserWidth past totalWidth pushes the whole list (and the
        // vertical scrollbar at browserWidth-9) off the window's right edge —
        // the scrollbar got clipped at small/high-GUI-scale widths. The panel
        // now wraps its upstream requirement to fit narrower widths, so it no
        // longer needs the browser wider than the viewport.
        int minimalSourceWidth = Math.min(this.totalWidth, this.lmlp$getRequiredMinimalSourceBrowserWidth());
        this.browserWidth = Math.max(this.totalWidth, Math.max(MaterialListColumnLayout.requiredEntryWidth() + 14, minimalSourceWidth));
        this.browserEntryWidth = this.browserWidth - 14;
        this.lmlp$reCreateListEntryWidgetsByPixels();
    }

    // Litematica creates the search bar once in the WidgetListMaterialList
    // constructor with the width at that moment; on window resize GuiListBase
    // only calls setSize, so the search icon would stay anchored to the old
    // right edge. Rebuild it with the new geometry, keeping text and state.
    @Inject(method = "setSize", at = @At("TAIL"))
    private void lmlp$repositionSearchBarOnResize(int width, int height, CallbackInfo ci) {
        if (!((Object) this instanceof WidgetListMaterialList) || this.widgetSearchBar == null) {
            return;
        }

        WidgetSearchBar old = this.widgetSearchBar;
        WidgetSearchBar fresh = new WidgetSearchBar(this.posX + 2, this.posY + 8, this.totalWidth - 16, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.RIGHT);
        fresh.setZLevel(1);
        fresh.setSearchOpen(old.isSearchOpen());
        String text = ((WidgetSearchBarAccessor) old).lmlp$getSearchBox().method_1882();
        ((WidgetSearchBarAccessor) fresh).lmlp$getSearchBox().method_1852(text);
        this.widgetSearchBar = fresh;
    }

    /**
     * @author Huan_meeng
     * @reason Keep keyboard navigation compatible with pixel-based scrollbar values.
     */
    @Overwrite
    protected void offsetSelectionOrScrollbar(int amount, boolean moveSelection) {
        if (!moveSelection) {
            this.scrollBar.offsetValue(amount * WHEEL_SCROLL_PIXELS / 3);
            this.reCreateListEntryWidgets();
            return;
        }

        int size = this.listContents.size();
        if (size <= 0) {
            return;
        }

        int selectedIndex = this.lastSelectedEntryIndex;
        if (selectedIndex < 0 || selectedIndex >= size) {
            selectedIndex = this.lmlp$firstVisibleEntryIndex(this.scrollBar.getValue());
        }

        int nextIndex = Math.max(0, Math.min(size - 1, selectedIndex + amount));
        this.setLastSelectedEntry(this.listContents.get(nextIndex), nextIndex);
        this.lmlp$scrollEntryIndexIntoView(nextIndex, 0);
        this.reCreateListEntryWidgets();
    }

    @Override
    public int lmlp$getEntryWidth() {
        return this.browserEntryWidth;
    }

    @Override
    public int lmlp$getVisibleTop() {
        return this.lmlp$getEntriesClipTop();
    }

    @Override
    public int lmlp$getVisibleBottom() {
        return this.lmlp$getVisibleTop() + this.lmlp$getEntryViewportHeight(0);
    }

    @Override
    public void lmlp$scrollEntryIntoView(Object entry, int bottomPadding) {
        if (!((Object) this instanceof WidgetListMaterialList)) {
            return;
        }

        int targetIndex = this.lmlp$findEntryIndex(entry);
        if (targetIndex < 0) {
            return;
        }

        int previousScroll = this.scrollBar.getValue();
        this.lmlp$scrollEntryIndexIntoView(targetIndex, bottomPadding);
        if (this.scrollBar.getValue() != previousScroll) {
            this.reCreateListEntryWidgets();
        }
    }

    private int lmlp$findEntryIndex(Object entry) {
        int size = this.listContents.size();
        for (int index = 0; index < size; index++) {
            if (this.listContents.get(index) == entry) {
                return index;
            }
        }

        if (entry instanceof MaterialListEntry materialEntry && MinimalSubMaterialListView.isMinimalEntry(materialEntry)) {
            String stableKey = MinimalSubMaterialListView.debugStableKey(materialEntry);
            for (int index = 0; index < size; index++) {
                Object candidate = this.listContents.get(index);
                if (candidate instanceof MaterialListEntry candidateEntry
                        && MinimalSubMaterialListView.isMinimalEntry(candidateEntry)
                        && stableKey.equals(MinimalSubMaterialListView.debugStableKey(candidateEntry))) {
                    return index;
                }
            }
        }

        return this.listContents.indexOf(entry);
    }

    private void lmlp$reCreateListEntryWidgetsByPixels() {
        this.lmlp$frameEntryHeights.clear();
        this.listWidgets.clear();
        this.maxVisibleBrowserEntries = 0;

        int size = this.listContents.size();
        int viewportHeight = this.lmlp$getListViewportHeight();
        int x = this.posX + 2;
        int top = this.posY + 4 + this.browserEntriesOffsetY;
        int currentHeight = 0;
        WidgetListEntryBase<?> header = this.createHeaderWidget(x, top, this.lmlp$firstVisibleEntryIndex(this.scrollBar.getValue()), viewportHeight, currentHeight);
        if (header != null) {
            this.listWidgets.add(header);
            currentHeight += header.getHeight();
        }

        int entryViewportHeight = Math.max(1, viewportHeight - currentHeight);
        this.scrollBar.setMaxValue(Math.max(0, this.lmlp$totalEntryHeight() - entryViewportHeight));
        int scrollPixels = this.scrollBar.getValue();
        int firstIndex = this.lmlp$firstVisibleEntryIndex(scrollPixels);
        int firstTop = this.lmlp$entryTop(firstIndex);
        int y = top + currentHeight - Math.max(0, scrollPixels - firstTop);
        int clipTop = top + currentHeight;
        int clipBottom = top + viewportHeight;

        for (int index = firstIndex; index < size && y < clipBottom; index++) {
            Object entry = this.listContents.get(index);
            int entryHeight = Math.max(1, this.lmlp$getScrollTargetEntryHeightFor(entry));
            if (y + entryHeight > clipTop) {
                WidgetListEntryBase<?> widget = this.createListEntryWidget(x, y, index, (index & 1) != 0, entry);
                if (widget != null) {
                    this.listWidgets.add(widget);
                    this.maxVisibleBrowserEntries++;
                }
            }
            y += entryHeight;
        }

        this.lastScrollbarPosition = this.scrollBar.getValue();
    }

    private void lmlp$scrollEntryIndexIntoView(int targetIndex, int bottomPadding) {
        this.lmlp$frameEntryHeights.clear();
        int viewportHeight = this.lmlp$getEntryViewportHeight(bottomPadding);
        int top = this.lmlp$entryTop(targetIndex);
        int bottom = top + Math.max(1, this.lmlp$getScrollTargetEntryHeightFor(this.listContents.get(targetIndex)));
        int scroll = this.scrollBar.getValue();
        if (top < scroll) {
            this.scrollBar.setValue(top);
        } else if (bottom > scroll + viewportHeight) {
            this.scrollBar.setValue(bottom - viewportHeight);
        }
        this.lastScrollbarPosition = this.scrollBar.getValue();
    }

    private int lmlp$getPixelScrollbarMaxValue(int bottomPadding) {
        return Math.max(0, this.lmlp$totalEntryHeight() - this.lmlp$getEntryViewportHeight(bottomPadding));
    }

    private int lmlp$getListViewportHeight() {
        return Math.max(1, this.browserHeight - this.browserEntriesOffsetY - BROWSER_BOTTOM_INSET);
    }

    private int lmlp$getEntryViewportHeight(int bottomPadding) {
        return Math.max(1, this.lmlp$getListViewportHeight() - this.lmlp$getHeaderHeight() - bottomPadding);
    }

    private int lmlp$getEntriesClipTop() {
        return this.posY + 4 + this.browserEntriesOffsetY + this.lmlp$getHeaderHeight();
    }

    private int lmlp$totalEntryHeight() {
        int height = 0;
        for (Object entry : this.listContents) {
            height += Math.max(1, this.lmlp$getScrollTargetEntryHeightFor(entry));
        }
        return height;
    }

    private int lmlp$entryTop(int targetIndex) {
        int top = 0;
        int size = Math.min(targetIndex, this.listContents.size());
        for (int index = 0; index < size; index++) {
            top += Math.max(1, this.lmlp$getScrollTargetEntryHeightFor(this.listContents.get(index)));
        }
        return top;
    }

    private int lmlp$firstVisibleEntryIndex(int scrollPixels) {
        int top = 0;
        for (int index = 0; index < this.listContents.size(); index++) {
            int height = Math.max(1, this.lmlp$getScrollTargetEntryHeightFor(this.listContents.get(index)));
            if (top + height > scrollPixels) {
                return index;
            }
            top += height;
        }
        return Math.max(0, this.listContents.size() - 1);
    }

    private boolean lmlp$isInsideBrowser(int mouseX, int mouseY) {
        return mouseX >= this.posX
                && mouseX <= this.posX + this.browserWidth
                && mouseY >= this.posY
                && mouseY <= this.posY + this.browserHeight;
    }

    private int lmlp$getScrollTargetEntryHeightFor(Object entry) {
        Integer cached = this.lmlp$frameEntryHeights.get(entry);
        if (cached != null) {
            return cached;
        }

        int computed = this.lmlp$computeScrollTargetEntryHeightFor(entry);
        this.lmlp$frameEntryHeights.put(entry, computed);
        return computed;
    }

    private int lmlp$computeScrollTargetEntryHeightFor(Object entry) {
        if (entry instanceof MaterialListEntry materialEntry) {
            boolean minimalSubMaterialView = (Object) this instanceof WidgetMaterialListAccess access && MinimalSubMaterialListView.isActive(access.lmlp$getMaterialList());
            if (minimalSubMaterialView && MinimalSubMaterialListView.isSourcesExpanded(materialEntry)) {
                int total = (Object) this instanceof WidgetMaterialListAccess access ? MinimalSubMaterialListView.total(materialEntry, access.lmlp$getMaterialList()) : materialEntry.getCountTotal();
                int missing = (Object) this instanceof WidgetMaterialListAccess access ? MinimalSubMaterialListView.netMissing(materialEntry, access.lmlp$getMaterialList()) : materialEntry.getCountMissing();
                return 23 + MinimalSourceInlineRenderer.getOuterHeight(
                        MinimalSubMaterialListView.displayStack(materialEntry),
                        MinimalSubMaterialListView.sourceRequirements(materialEntry, total, missing),
                        MinimalSubMaterialListView.sourceContributions(materialEntry),
                        MinimalSubMaterialListView.isSourcesFull(materialEntry),
                        this.lmlp$getMinimalSourcePanelWidthForHeight());
            }
            if (MaterialListPlusState.isRecipeExpanded(materialEntry)) {
                return 23 + RecipeInlineRenderer.getTargetOuterHeight(MaterialListPlusState.getCachedSummaries(materialEntry), this.lmlp$getRecipePanelWidthForHeight());
            }
        }

        return this.getBrowserEntryHeightFor(entry);
    }

    private int lmlp$getHeaderHeight() {
        if (!this.listWidgets.isEmpty() && this.listWidgets.get(0) instanceof WidgetListEntryBase<?> widget && widget.getEntry() == null) {
            return widget.getHeight();
        }

        return 0;
    }

    private int lmlp$getMinimalSourcePanelWidthForHeight() {
        return Math.max(180, this.browserEntryWidth - 36);
    }

    private int lmlp$getRecipePanelWidthForHeight() {
        return Math.max(180, this.browserEntryWidth - 64);
    }

    private int lmlp$getRequiredMinimalSourceBrowserWidth() {
        if (!((Object) this instanceof WidgetMaterialListAccess access) || !MinimalSubMaterialListView.isActive(access.lmlp$getMaterialList())) {
            return 0;
        }

        var materialList = access.lmlp$getMaterialList();
        int requiredWidth = 0;
        for (Object entry : this.listContents) {
            if (entry instanceof MaterialListEntry materialEntry && MinimalSubMaterialListView.isSourcesExpanded(materialEntry)) {
                int total = MinimalSubMaterialListView.total(materialEntry, materialList);
                int missing = MinimalSubMaterialListView.netMissing(materialEntry, materialList);
                List<MinimalSubMaterialListView.RequirementContribution> requirements =
                        MinimalSubMaterialListView.sourceRequirements(materialEntry, total, missing);
                requiredWidth = Math.max(requiredWidth, MinimalSourceInlineRenderer.getRequiredPanelWidth(
                        requirements, MinimalSubMaterialListView.sourceContributions(materialEntry)));
            }
        }

        return requiredWidth <= 0 ? 0 : requiredWidth + MINIMAL_SOURCE_PANEL_SIDE_WIDTH;
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
