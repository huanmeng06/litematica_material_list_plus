package io.github.huanmeng06.lmlp.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntrySortable;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.access.MinimalChoiceTooltipAccess;
import io.github.huanmeng06.lmlp.access.WidgetListBoundsAccess;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.gui.MaterialListColumnLayout;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import io.github.huanmeng06.lmlp.gui.ClickableCursor;
import io.github.huanmeng06.lmlp.gui.ItemTooltipRenderer;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import io.github.huanmeng06.lmlp.gui.MinimalSourceInlineRenderer;
import io.github.huanmeng06.lmlp.gui.RecipeDetailScreen;
import io.github.huanmeng06.lmlp.gui.RecipeInlineRenderer;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.MaterialCounts;
import io.github.huanmeng06.lmlp.recipe.RecipeResolvers;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import net.minecraft.class_1799;
import net.minecraft.class_437;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = WidgetMaterialListEntry.class, remap = false)
public abstract class WidgetMaterialListEntryMixin extends WidgetListEntrySortable<MaterialListEntry> implements MinimalChoiceTooltipAccess {
    private static final int EXPANDED_PANEL_BOTTOM_PADDING = 8;
    private static final int HOVER_TOOLTIP_MARGIN = 8;
    private static final int HOVER_TOOLTIP_CURSOR_OFFSET = 12;
    private static final int HOVER_TOOLTIP_PADDING = 6;
    private static final int HOVER_TOOLTIP_LINE_HEIGHT = 12;
    private static final int HOVER_TOOLTIP_HEADER_GAP = 3;
    private static final int HOVER_TOOLTIP_ICON_GAP = 6;
    private static final int HOVER_TOOLTIP_ICON_SIZE = 16;
    private static final int HOVER_TEXT_HEIGHT = 12;
    private static final int TOOLTIP_STACK_GAP = 8;
    private static final int CHOICE_TOOLTIP_COLUMN_GAP = 14;
    private static final int CHOICE_TOOLTIP_ROW_HEIGHT = 18;
    private static final int VANILLA_TOOLTIP_HEIGHT = 68;
    private static final int VANILLA_TOOLTIP_MARGIN = 10;
    private static final int VANILLA_TOOLTIP_LABEL_VALUE_GAP = 20;
    private static final int VANILLA_TOOLTIP_PADDING_EXTRA = 60;
    private static final int VANILLA_TOOLTIP_LINE_HEIGHT = 16;
    private static final int VANILLA_TOOLTIP_HEADER_GAP = 8;
    private static final int COUNT_COLUMN_SAFETY_PADDING = 32;
    private static int lmlpMaxTotalDigits;
    private static int lmlpMaxMissingDigits;
    private static int lmlpMaxAvailableDigits;
    @Shadow
    private static int maxNameLength;
    @Shadow
    private static int maxCountLength1;
    @Shadow
    private static int maxCountLength2;
    @Shadow
    private static int maxCountLength3;

    @Shadow
    @Final
    private MaterialListBase materialList;
    @Shadow
    @Final
    private WidgetListMaterialList listWidget;
    @Shadow
    @Final
    private MaterialListEntry entry;
    @Shadow
    @Final
    private String header1;
    @Shadow
    @Final
    private String header2;
    @Shadow
    @Final
    private String header3;
    @Shadow
    @Final
    private String header4;
    @Shadow
    @Final
    private boolean isOdd;

    protected WidgetMaterialListEntryMixin(int x, int y, int width, int height, MaterialListEntry entry, int listIndex) {
        super(x, y, width, height, entry, listIndex);
    }

    /**
     * @author Huan_meeng
     * @reason Recalculate material list columns using grouped total and missing count text.
     */
    @Overwrite
    public static void setMaxNameLength(List<MaterialListEntry> entries, int multiplier) {
        maxNameLength = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.item") + GuiBase.TXT_RST);
        maxCountLength1 = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.total") + GuiBase.TXT_RST);
        maxCountLength2 = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.missing") + GuiBase.TXT_RST);
        maxCountLength3 = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.available") + GuiBase.TXT_RST);
        lmlpMaxTotalDigits = 1;
        lmlpMaxMissingDigits = 1;
        lmlpMaxAvailableDigits = 1;

        for (MaterialListEntry entry : entries) {
            int entryMultiplier = MinimalSubMaterialListView.isMinimalEntry(entry) ? 1 : multiplier;
            int total = entry.getCountTotal() * entryMultiplier;
            int missing = MinimalSubMaterialListView.netMissing(entry, multiplier);
            int available = entry.getCountAvailable();
            lmlpMaxTotalDigits = Math.max(lmlpMaxTotalDigits, Integer.toString(total).length());
            lmlpMaxMissingDigits = Math.max(lmlpMaxMissingDigits, Integer.toString(missing).length());
            lmlpMaxAvailableDigits = Math.max(lmlpMaxAvailableDigits, Integer.toString(available).length());
        }

        for (MaterialListEntry entry : entries) {
            int entryMultiplier = MinimalSubMaterialListView.isMinimalEntry(entry) ? 1 : multiplier;
            int total = entry.getCountTotal() * entryMultiplier;
            int missing = MinimalSubMaterialListView.netMissing(entry, multiplier);
            int available = entry.getCountAvailable();
            String name = MinimalSubMaterialListView.widestDisplayName(entry);

            maxNameLength = Math.max(maxNameLength, StringUtils.getStringWidth(name));
            for (class_1799 stack : MinimalSubMaterialListView.displayStacks(entry)) {
                maxCountLength1 = Math.max(maxCountLength1, StringUtils.getStringWidth(CountFormatter.formatAligned(stack, total, lmlpMaxTotalDigits)));
                maxCountLength2 = Math.max(maxCountLength2, StringUtils.getStringWidth(CountFormatter.formatAligned(stack, missing, lmlpMaxMissingDigits)));
                maxCountLength3 = Math.max(maxCountLength3, StringUtils.getStringWidth(CountFormatter.formatAligned(stack, available, lmlpMaxAvailableDigits)));
            }
        }

        MaterialListColumnLayout.updateRequiredEntryWidth(
                maxNameLength,
                maxCountLength1 + COUNT_COLUMN_SAFETY_PADDING,
                maxCountLength2 + COUNT_COLUMN_SAFETY_PADDING,
                maxCountLength3 + COUNT_COLUMN_SAFETY_PADDING);
    }

    /**
     * @author Huan_meeng
     * @reason Space columns according to the longer grouped count strings.
     */
    @Overwrite
    protected int getColumnPosX(int column) {
        boolean totalVisible = MaterialListColumnLayout.isTotalVisible();
        boolean missingVisible = MaterialListColumnLayout.isMissingVisible();
        boolean availableVisible = MaterialListColumnLayout.isAvailableVisible();

        int xItem = this.x + 4;
        int cursor = xItem + MaterialListColumnLayout.nameWidth();

        int xTotal = cursor;
        if (totalVisible) {
            cursor += MaterialListColumnLayout.nameToTotalGap();
            xTotal = cursor;
            cursor += MaterialListColumnLayout.totalWidth();
        }

        int xMissing = cursor;
        if (missingVisible) {
            cursor += totalVisible ? MaterialListColumnLayout.countColumnGap() : MaterialListColumnLayout.nameToTotalGap();
            xMissing = cursor;
            cursor += MaterialListColumnLayout.missingWidth();
        }

        int xAvailable = cursor;
        if (availableVisible) {
            cursor += (totalVisible || missingVisible) ? MaterialListColumnLayout.countColumnGap() : MaterialListColumnLayout.nameToTotalGap();
            xAvailable = cursor;
            cursor += MaterialListColumnLayout.availableWidth();
        }

        // The trailing gap only applies while the available column is shown;
        // once it is hidden the row's sortable content ends at the last
        // visible column, so the ignore-button boundary starts right there.
        int xEnd = availableVisible
                ? cursor + MaterialListColumnLayout.countColumnGap()
                : cursor;

        // Collapse hidden columns rightward onto the next visible position so
        // malilib's renderColumnHeader (which draws a box from getColumnPosX(i)
        // to getColumnPosX(i+1) for every column index) draws zero-width boxes
        // for them instead of leftover empty frames spanning the gaps.
        if (!availableVisible) {
            xAvailable = xEnd;
        }
        if (!missingVisible) {
            xMissing = xAvailable;
        }
        if (!totalVisible) {
            xTotal = xMissing;
        }

        return switch (column) {
            case 0 -> xItem;
            case 1 -> xTotal;
            case 2 -> xMissing;
            case 3 -> xAvailable;
            case 4 -> xEnd;
            default -> xItem;
        };
    }

    /**
     * @author Huan_meeng
     * @reason Use row clicks for inline expansion and direct name clicks for details.
     */
    @Overwrite
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton) {
        if (this.entry == null) {
            if (this.header1 != null && this.listWidget.getSearchBarWidget().isSearchOpen()) {
                return false;
            }

            int column = this.getMouseOverColumn(mouseX, mouseY);
            switch (column) {
                case 0 -> this.materialList.setSortCriteria(MaterialListBase.SortCriteria.NAME);
                case 1 -> this.materialList.setSortCriteria(MaterialListBase.SortCriteria.COUNT_TOTAL);
                case 2 -> this.materialList.setSortCriteria(MaterialListBase.SortCriteria.COUNT_MISSING);
                case 3 -> this.materialList.setSortCriteria(MaterialListBase.SortCriteria.COUNT_AVAILABLE);
                default -> {
                    return false;
                }
            }

            this.listWidget.refreshEntries();
            return true;
        }

        if (MinimalSubMaterialListView.isActive(this.materialList)) {
            if (mouseButton == 0 && this.isMouseOver(mouseX, mouseY)) {
                if (this.toggleMinimalSourceSort(mouseX, mouseY)) {
                    this.listWidget.refreshEntries();
                    return true;
                }

                if (this.openMinimalSourceRecipe(mouseX, mouseY)) {
                    return true;
                }

                boolean wasExpanded = MinimalSubMaterialListView.isSourcesExpanded(this.entry);
                MinimalSubMaterialListView.toggleSources(this.entry, false);
                this.listWidget.refreshEntries();
                if (!wasExpanded) {
                    this.scrollExpandedEntryIntoView();
                }
                return true;
            }
            return false;
        }

        if (super.onMouseClickedImpl(mouseX, mouseY, mouseButton)) {
            return true;
        }

        if (mouseButton == 0 && this.isMouseOver(mouseX, mouseY)) {
            if (this.lmlp$isMaterialNameHovered(mouseX, mouseY)) {
                List<RecipeSummary> summaries = MaterialListPlusState.resolveFor(this.entry, this.materialList);
                this.mc.method_1507(new RecipeDetailScreen(
                        GuiUtils.getCurrentScreen(),
                        this.entry.getStack(),
                        MaterialCounts.total(this.entry, this.materialList),
                        MaterialCounts.netMissing(this.entry, this.materialList),
                        summaries));
                return true;
            }

            if (this.openRecipePanelNavigation(mouseX, mouseY)) {
                return true;
            }

            if (this.handleRecipePanelClick(mouseX, mouseY)) {
                return true;
            }

            boolean wasExpanded = MaterialListPlusState.isRecipeExpanded(this.entry);
            if (wasExpanded) {
                MaterialListPlusState.clear();
            } else {
                MaterialListPlusState.open(this.entry, this.materialList);
            }

            this.listWidget.refreshEntries();
            if (!wasExpanded) {
                this.scrollExpandedEntryIntoView();
            }
            return true;
        }

        return false;
    }

    /**
     * @author Huan_meeng
     * @reason Preserve original non-selectable rows.
     */
    @Overwrite
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    /**
     * @author Huan_meeng
     * @reason Draw grouped counts and optional inline recipe summaries.
     */
    @Overwrite
    public void render(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        if (this.header1 == null && (selected || this.isMouseOver(mouseX, mouseY))) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0707070);
        } else if (this.isOdd) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0101010);
        } else {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0303030);
        }

        int xItem = this.getColumnPosX(0);
        int xTotal = this.getColumnPosX(1);
        int xMissing = this.getColumnPosX(2);
        int xAvailable = this.getColumnPosX(3);
        int yText = this.y + 7;

        if (this.header1 != null) {
            if (!this.listWidget.getSearchBarWidget().isSearchOpen()) {
                this.drawString(xItem, yText, -1, this.header1, drawContext);
                if (MaterialListColumnLayout.isTotalVisible()) {
                    this.drawString(xTotal, yText, -1, this.header2, drawContext);
                }
                if (MaterialListColumnLayout.isMissingVisible()) {
                    this.drawString(xMissing, yText, -1, this.header3, drawContext);
                }
                if (MaterialListColumnLayout.isAvailableVisible()) {
                    this.drawString(xAvailable, yText, -1, this.header4, drawContext);
                }
                this.renderColumnHeader(mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP);
            }

            return;
        }

        if (this.entry == null) {
            return;
        }

        class_1799 stack = MinimalSubMaterialListView.displayStack(this.entry);
        String name = MinimalSubMaterialListView.displayName(this.entry);
        int total = MinimalSubMaterialListView.total(this.entry, this.materialList);
        int rawMissing = MinimalSubMaterialListView.missing(this.entry, this.materialList);
        int missing = MinimalSubMaterialListView.netMissing(this.entry, this.materialList);
        int available = this.entry.getCountAvailable();
        boolean minimalSubMaterialView = MinimalSubMaterialListView.isActive(this.materialList);

        int iconX = xItem;
        String renderedName = this.truncateToWidth(name, this.lmlp$nameTextLimit());
        if (!minimalSubMaterialView && this.lmlp$isMaterialNameHovered(mouseX, mouseY)) {
            ClickableCursor.requestHand();
            renderedName = GuiBase.TXT_BOLD + GuiBase.TXT_UNDERLINE + renderedName + GuiBase.TXT_RST;
        }
        this.drawString(xItem + 20, yText, -1, renderedName, drawContext);
        if (MaterialListColumnLayout.isTotalVisible()) {
            this.drawString(xTotal, yText, -1, CountFormatter.formatAligned(stack, total, lmlpMaxTotalDigits), drawContext);
        }
        if (MaterialListColumnLayout.isMissingVisible()) {
            this.drawString(xMissing, yText, -1, netMissingColor(missing) + CountFormatter.formatAligned(stack, missing, lmlpMaxMissingDigits), drawContext);
        }
        if (MaterialListColumnLayout.isAvailableVisible()) {
            this.drawString(xAvailable, yText, -1, availableColor(available, rawMissing) + CountFormatter.formatAligned(stack, available, lmlpMaxAvailableDigits), drawContext);
        }

        drawContext.method_51448().method_22903();
        RenderUtils.enableDiffuseLightingGui3D();
        int iconY = this.y + 3;
        RenderUtils.drawRect(iconX, iconY, 16, 16, 0x20FFFFFF);
        drawContext.method_51427(stack, iconX, iconY);
        RenderSystem.disableBlend();
        RenderUtils.disableDiffuseLighting();
        drawContext.method_51448().method_22909();

        if (MaterialListPlusState.isRecipeVisible(this.entry)) {
            List<RecipeSummary> summaries = MaterialListPlusState.getSummaries(this.entry, this.materialList);
            if (summaries.isEmpty()) {
                summaries = MaterialListPlusState.getCachedSummaries(this.entry);
            }
            int panelY = this.y + 23;
            int panelWidth = Math.max(180, this.width - 64);
            int visibleOuterHeight = RecipeInlineRenderer.getOuterHeight(summaries, panelWidth, MaterialListPlusState.recipeProgress(this.entry));
            RecipeInlineRenderer.render(this, drawContext, this.x + 28, panelY, panelWidth, summaries, visibleOuterHeight, mouseX, mouseY);
            if (!RecipeInlineRenderer.navigationTargetAt(summaries, this.x + 28, panelY, panelWidth,
                    visibleOuterHeight, mouseX, mouseY).isNone()) {
                ClickableCursor.requestHand();
            }
        }

        if (minimalSubMaterialView && MinimalSubMaterialListView.isSourcesVisible(this.entry)) {
            List<MinimalSubMaterialListView.RequirementContribution> requirements = MinimalSubMaterialListView.sourceRequirements(this.entry, total, missing);
            List<MinimalSubMaterialListView.SourceContribution> sources = MinimalSubMaterialListView.sourceContributions(this.entry);
            boolean showAllSources = MinimalSubMaterialListView.isSourcesFull(this.entry);
            int panelY = this.y + 23;
            int panelWidth = this.minimalSourcePanelWidth();
            int visibleOuterHeight = MinimalSourceInlineRenderer.getOuterHeight(stack, requirements, sources, showAllSources, panelWidth, MinimalSubMaterialListView.sourceProgress(this.entry));
            MinimalSourceInlineRenderer.render(this, drawContext, this.x + 28, panelY, panelWidth, stack, name, requirements, sources, showAllSources, visibleOuterHeight, mouseX, mouseY);
        }

        if (minimalSubMaterialView) {
            this.drawSubWidgets(mouseX, mouseY, drawContext);
        } else {
            super.render(mouseX, mouseY, selected, drawContext);
        }
    }

    private boolean lmlp$isMaterialNameHovered(int mouseX, int mouseY) {
        if (this.entry == null || MinimalSubMaterialListView.isActive(this.materialList)) {
            return false;
        }
        int nameX = this.getColumnPosX(0) + 20;
        int nameY = this.y + 7;
        String name = this.truncateToWidth(MinimalSubMaterialListView.displayName(this.entry), this.lmlp$nameTextLimit());
        int nameWidth = StringUtils.getStringWidth(name);
        return mouseX >= nameX && mouseX < nameX + nameWidth && mouseY >= nameY && mouseY < nameY + 10;
    }

    /**
     * @author Huan_meeng
     * @reason Optionally suppress Litematica's original material hover tooltip.
     */
    @Overwrite
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, class_332 drawContext) {
        if (this.entry == null) {
            return;
        }

        if (MinimalSubMaterialListView.isActive(this.materialList) && this.minimalChoiceTooltipTarget(mouseX, mouseY) != null) {
            return;
        }

        if (this.lmlp$renderTruncatedNameTooltip(drawContext, mouseX, mouseY)) {
            return;
        }

        if (this.lmlp$renderRecipeChoiceGroupTooltip(drawContext, mouseX, mouseY)) {
            return;
        }

        if (this.lmlp$renderPanelItemTooltip(drawContext, mouseX, mouseY)) {
            return;
        }

        if (Configs.Generic.DISABLE_LITEMATICA_HOVER_TOOLTIP.getBooleanValue()) {
            return;
        }

        this.renderVanillaMaterialHoverTooltip(drawContext, mouseX, mouseY);
    }

    // The narrow-window name clamp truncates the name column text; show the
    // full name as a tooltip when hovering a truncated name.
    private int lmlp$nameTextLimit() {
        // The name text starts 20px into the name column (after the item
        // icon); the raw column width never truncates because it was measured
        // from the widest name, so this only bites once the clamp kicks in.
        return MaterialListColumnLayout.nameWidth() + 20;
    }

    private boolean lmlp$renderTruncatedNameTooltip(class_332 drawContext, int mouseX, int mouseY) {
        if (this.entry == null || this.header1 != null) {
            return false;
        }

        String fullName = MinimalSubMaterialListView.displayName(this.entry);
        String shownName = this.truncateToWidth(fullName, this.lmlp$nameTextLimit());
        if (shownName.equals(fullName)) {
            return false;
        }

        int nameX = this.getColumnPosX(0) + 20;
        if (mouseX < nameX
                || mouseX >= nameX + this.getStringWidth(shownName)
                || mouseY < this.y + 1
                || mouseY >= this.y + Math.min(this.height, 22)) {
            return false;
        }

        RenderUtils.drawHoverText(mouseX, mouseY, List.of(fullName), drawContext);
        return true;
    }

    // Hovering a choice-group ("任意X") name in the recipe panel shows the
    // concrete items it stands for, distinct from hovering its item icon
    // (which still shows the vanilla item tooltip via lmlp$renderPanelItemTooltip).
    private boolean lmlp$renderRecipeChoiceGroupTooltip(class_332 drawContext, int mouseX, int mouseY) {
        if (this.entry == null || !MaterialListPlusState.isRecipeVisible(this.entry)) {
            return false;
        }

        List<RecipeSummary> summaries = MaterialListPlusState.getSummaries(this.entry, this.materialList);
        if (summaries.isEmpty()) {
            summaries = MaterialListPlusState.getCachedSummaries(this.entry);
        }
        int panelX = this.x + 28;
        int panelY = this.y + 23;
        int panelWidth = Math.max(180, this.width - 64);
        int visibleOuterHeight = RecipeInlineRenderer.getOuterHeight(summaries, panelWidth, MaterialListPlusState.recipeProgress(this.entry));
        RecipeInlineRenderer.ChoiceGroupHover hover = RecipeInlineRenderer.hoveredChoiceGroup(summaries, panelX, panelY, panelWidth, visibleOuterHeight, mouseX, mouseY);
        if (hover == null) {
            return false;
        }

        List<MinimalSubMaterialListView.TooltipCandidate> candidates = lmlp$choiceGroupCandidates(hover);
        if (candidates.isEmpty()) {
            return false;
        }

        return this.lmlp$renderChoiceGrid(drawContext, mouseX, mouseY, hover.icon(), MinimalSubMaterialListView.emphasizeChoiceGroupName(hover.name()), candidates);
    }

    // Build the grid candidates for a hovered choice group. The names list can
    // drift out of parallel with the icons (unioned children dedupe icons by id
    // separately from names), so only trust names when it lines up 1:1;
    // otherwise derive each row's label from its own icon to guarantee the
    // icon and name always match.
    private static List<MinimalSubMaterialListView.TooltipCandidate> lmlp$choiceGroupCandidates(RecipeInlineRenderer.ChoiceGroupHover hover) {
        List<class_1799> icons = hover.icons();
        List<String> names = hover.alternatives();
        if (icons.isEmpty()) {
            return List.of();
        }

        boolean namesParallel = names.size() == icons.size();
        List<MinimalSubMaterialListView.TooltipCandidate> candidates = new java.util.ArrayList<>(icons.size());
        for (int index = 0; index < icons.size(); index++) {
            class_1799 icon = icons.get(index);
            if (icon.method_7960()) {
                continue;
            }
            String name = namesParallel ? names.get(index) : ItemStackTexts.name(icon);
            candidates.add(new MinimalSubMaterialListView.TooltipCandidate(icon.method_7972(), name));
        }
        return candidates;
    }

    @Override
    public boolean lmlp$renderMinimalChoiceTooltip(class_332 drawContext, int mouseX, int mouseY) {
        if (this.entry == null || !MinimalSubMaterialListView.isActive(this.materialList)) {
            return false;
        }
        ChoiceTooltipTarget target = this.minimalChoiceTooltipTarget(mouseX, mouseY);
        if (target == null) {
            return false;
        }

        List<MinimalSubMaterialListView.TooltipCandidate> candidates = target.candidates();
        if (candidates.isEmpty()) {
            return false;
        }

        return this.lmlp$renderChoiceGrid(drawContext, mouseX, mouseY, target.icon(), target.name(), candidates);
    }

    // Shared rich choice-group grid (icon + name, up to four columns) used by both
    // the minimal sub-material page and the recipe panel's "任意X" hover.
    private boolean lmlp$renderChoiceGrid(class_332 drawContext, int mouseX, int mouseY, class_1799 headerStack, String headerName, List<MinimalSubMaterialListView.TooltipCandidate> candidates) {
        int maxPanelWidth = Math.max(120, this.mc.method_22683().method_4486() - HOVER_TOOLTIP_MARGIN * 2);
        int maxContentWidth = Math.max(80, maxPanelWidth - HOVER_TOOLTIP_PADDING * 2);
        int maxCandidateNameWidth = 0;
        for (MinimalSubMaterialListView.TooltipCandidate candidate : candidates) {
            maxCandidateNameWidth = Math.max(maxCandidateNameWidth, this.getStringWidth(candidate.name()));
        }

        int naturalColumnWidth = HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP + maxCandidateNameWidth;
        int minColumnWidth = HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP + 60;
        int maxRows = Configs.Generic.HOVER_PANEL_MAX_ROWS.getIntegerValue();
        int requestedColumns = Math.max(1, (candidates.size() + maxRows - 1) / maxRows);
        int columns = requestedColumns;
        while (columns > 1
                && maxContentWidth < minColumnWidth * columns + CHOICE_TOOLTIP_COLUMN_GAP * (columns - 1)) {
            columns--;
        }
        int columnGap = columns > 1 ? CHOICE_TOOLTIP_COLUMN_GAP : 0;
        int columnWidth = columns > 1
                ? Math.min(naturalColumnWidth, Math.max(minColumnWidth,
                        (maxContentWidth - columnGap * (columns - 1)) / columns))
                : Math.min(naturalColumnWidth, maxContentWidth);
        // Fill each column to the configured row limit before starting the
        // next one. The separate title/header never consumes one of these rows.
        // If the screen is too narrow for the requested number of columns,
        // balance the rows across the reduced column count instead.
        int rowsPerColumn = columns == requestedColumns
                ? Math.min(maxRows, candidates.size())
                : (candidates.size() + columns - 1) / columns;
        int candidateContentWidth = columns * columnWidth + columnGap * (columns - 1);

        String headerText = this.truncateToWidth(
                headerName,
                Math.max(20, maxContentWidth - HOVER_TOOLTIP_ICON_SIZE - HOVER_TOOLTIP_ICON_GAP));
        int headerWidth = HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP + this.getStringWidth(headerText);
        int contentWidth = Math.min(maxContentWidth, Math.max(headerWidth, candidateContentWidth));
        int panelWidth = contentWidth + HOVER_TOOLTIP_PADDING * 2;
        int panelHeight = HOVER_TOOLTIP_PADDING * 2
                + Math.max(HOVER_TOOLTIP_ICON_SIZE, HOVER_TOOLTIP_LINE_HEIGHT)
                + HOVER_TOOLTIP_HEADER_GAP
                + rowsPerColumn * CHOICE_TOOLTIP_ROW_HEIGHT;

        PanelBounds bounds = this.choiceTooltipBounds(mouseX, mouseY, panelWidth, panelHeight);
        int panelX = bounds.x();
        int panelY = bounds.y();
        int contentX = panelX + HOVER_TOOLTIP_PADDING;
        int headerY = panelY + HOVER_TOOLTIP_PADDING + 4;
        int rowsY = headerY + Math.max(HOVER_TOOLTIP_ICON_SIZE, HOVER_TOOLTIP_LINE_HEIGHT) + HOVER_TOOLTIP_HEADER_GAP;

        // Submit JEI's and item renderer's queued batches before painting the
        // tooltip. Otherwise those older vertices flush afterwards and appear
        // through the panel regardless of its Z translation.
        drawContext.method_51452();
        drawContext.method_51448().method_22903();
        // Match vanilla tooltip headroom and the full recipe-detail tooltip.
        // JEI's native display items can render above +200 and otherwise bleed
        // through this panel (for example the crafting-table catalyst icon).
        drawContext.method_51448().method_46416(0.0F, 0.0F, 400.0F);
        lmlp$drawTooltipBox(drawContext, panelX, panelY, panelWidth, panelHeight, 0xF0000000, 0xFF999999);

        drawContext.method_25294(contentX, headerY - 4, contentX + HOVER_TOOLTIP_ICON_SIZE,
                headerY - 4 + HOVER_TOOLTIP_ICON_SIZE, 0x20FFFFFF);
        RenderUtils.enableDiffuseLightingGui3D();
        drawContext.method_51427(headerStack, contentX, headerY - 4);
        RenderUtils.disableDiffuseLighting();
        this.drawString(contentX + HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP, headerY, 0xFFFFFFFF, headerText, drawContext);

        for (int index = 0; index < candidates.size(); index++) {
            MinimalSubMaterialListView.TooltipCandidate candidate = candidates.get(index);
            int column = index / rowsPerColumn;
            int row = index % rowsPerColumn;
            int rowX = contentX + column * (columnWidth + columnGap);
            int rowTop = rowsY + row * CHOICE_TOOLTIP_ROW_HEIGHT;
            int textX = rowX + HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP;
            int maxNameWidth = Math.max(20, columnWidth - HOVER_TOOLTIP_ICON_SIZE - HOVER_TOOLTIP_ICON_GAP);
            String itemName = this.truncateToWidth(candidate.name(), maxNameWidth);

            drawContext.method_25294(rowX, rowTop + 1, rowX + HOVER_TOOLTIP_ICON_SIZE,
                    rowTop + 1 + HOVER_TOOLTIP_ICON_SIZE, 0x20FFFFFF);
            RenderUtils.enableDiffuseLightingGui3D();
            drawContext.method_51427(candidate.icon(), rowX, rowTop + 1);
            RenderUtils.disableDiffuseLighting();
            this.drawString(textX, rowTop + 5, 0xFFFFFFFF, itemName, drawContext);
        }

        drawContext.method_51448().method_22909();
        return true;
    }

    @Override
    public boolean lmlp$renderPanelItemTooltip(class_332 drawContext, int mouseX, int mouseY) {
        if (this.entry == null) {
            return false;
        }

        class_1799 stack = this.panelHoveredStack(mouseX, mouseY);
        if (stack.method_7960()) {
            return false;
        }

        return ItemTooltipRenderer.render(drawContext, this.mc.field_1772, stack, mouseX, mouseY);
    }

    private ChoiceTooltipTarget minimalChoiceTooltipTarget(int mouseX, int mouseY) {
        if (!this.panelHoveredStack(mouseX, mouseY).method_7960()) {
            return null;
        }

        List<MinimalSubMaterialListView.TooltipCandidate> candidates = MinimalSubMaterialListView.tooltipCandidates(this.entry);
        if (!candidates.isEmpty() && this.isMinimalChoiceTextHovered(mouseX, mouseY)) {
            return new ChoiceTooltipTarget(
                    MinimalSubMaterialListView.displayStack(this.entry),
                    MinimalSubMaterialListView.displayName(this.entry),
                    candidates);
        }

        return this.requirementChoiceTooltipTarget(mouseX, mouseY);
    }

    private ChoiceTooltipTarget requirementChoiceTooltipTarget(int mouseX, int mouseY) {
        if (!MinimalSubMaterialListView.isSourcesVisible(this.entry)) {
            return null;
        }

        class_1799 stack = MinimalSubMaterialListView.displayStack(this.entry);
        int total = MinimalSubMaterialListView.total(this.entry, this.materialList);
        int missing = MinimalSubMaterialListView.netMissing(this.entry, this.materialList);
        List<MinimalSubMaterialListView.RequirementContribution> requirements = MinimalSubMaterialListView.sourceRequirements(this.entry, total, missing);
        if (requirements.isEmpty()) {
            return null;
        }

        List<MinimalSubMaterialListView.SourceContribution> sources = MinimalSubMaterialListView.sourceContributions(this.entry);
        boolean showAllSources = MinimalSubMaterialListView.isSourcesFull(this.entry);
        int panelX = this.x + 28;
        int panelY = this.y + 23;
        int panelWidth = this.minimalSourcePanelWidth();
        int visibleOuterHeight = MinimalSourceInlineRenderer.getOuterHeight(stack, requirements, sources, showAllSources, panelWidth, MinimalSubMaterialListView.sourceProgress(this.entry));

        // Delegate the hit-test to the renderer so the hoverable region tracks
        // render()'s exact layout (incl. the wrapped upstream second line) and
        // the tooltip's title icon is the one currently cycling on screen.
        MinimalSourceInlineRenderer.RequirementNameHit hit = MinimalSourceInlineRenderer.hoveredRequirementName(
                panelX, panelY, panelWidth, stack, requirements, sources, showAllSources, visibleOuterHeight, mouseX, mouseY);
        if (hit == null) {
            return null;
        }

        if (hit.upstream()) {
            MinimalSubMaterialListView.UpstreamRequirement upstream = hit.requirement().upstream();
            List<MinimalSubMaterialListView.TooltipCandidate> candidates = MinimalSubMaterialListView.upstreamTooltipCandidates(upstream);
            if (candidates.isEmpty()) {
                return null;
            }
            return new ChoiceTooltipTarget(hit.icon(), MinimalSubMaterialListView.upstreamDisplayName(upstream), candidates);
        }

        List<MinimalSubMaterialListView.TooltipCandidate> candidates = MinimalSubMaterialListView.requirementTooltipCandidates(hit.requirement());
        if (candidates.isEmpty()) {
            return null;
        }
        return new ChoiceTooltipTarget(hit.icon(), MinimalSubMaterialListView.requirementDisplayName(hit.requirement()), candidates);
    }

    private boolean isMinimalChoiceTextHovered(int mouseX, int mouseY) {
        String name = MinimalSubMaterialListView.displayName(this.entry);
        int nameWidth = this.getStringWidth(name);
        if (isTextHovered(this.getColumnPosX(0) + 20, this.y + 7, nameWidth, mouseX, mouseY)) {
            return true;
        }

        if (!MinimalSubMaterialListView.isSourcesVisible(this.entry)) {
            return false;
        }

        class_1799 stack = MinimalSubMaterialListView.displayStack(this.entry);
        int total = MinimalSubMaterialListView.total(this.entry, this.materialList);
        int missing = MinimalSubMaterialListView.netMissing(this.entry, this.materialList);
        List<MinimalSubMaterialListView.RequirementContribution> requirements = MinimalSubMaterialListView.sourceRequirements(this.entry, total, missing);
        List<MinimalSubMaterialListView.SourceContribution> sources = MinimalSubMaterialListView.sourceContributions(this.entry);
        boolean showAllSources = MinimalSubMaterialListView.isSourcesFull(this.entry);
        int panelWidth = this.minimalSourcePanelWidth();
        int panelY = this.y + 23;
        int visibleOuterHeight = MinimalSourceInlineRenderer.getOuterHeight(stack, requirements, sources, showAllSources, panelWidth, MinimalSubMaterialListView.sourceProgress(this.entry));
        return MinimalSourceInlineRenderer.isTargetNameHovered(this.x + 28, panelY, panelWidth, stack, nameWidth, requirements, sources, visibleOuterHeight, mouseX, mouseY);
    }

    private static boolean isTextHovered(int textX, int textY, int textWidth, int mouseX, int mouseY) {
        return mouseX >= textX
                && mouseX < textX + textWidth
                && mouseY >= textY
                && mouseY < textY + HOVER_TEXT_HEIGHT;
    }

    private class_1799 panelHoveredStack(int mouseX, int mouseY) {
        class_1799 rowStack = MinimalSubMaterialListView.displayStack(this.entry);
        if (this.isRowItemHovered(mouseX, mouseY)) {
            return rowStack;
        }

        if (MaterialListPlusState.isRecipeVisible(this.entry)) {
            List<RecipeSummary> summaries = MaterialListPlusState.getSummaries(this.entry, this.materialList);
            if (summaries.isEmpty()) {
                summaries = MaterialListPlusState.getCachedSummaries(this.entry);
            }
            int panelX = this.x + 28;
            int panelY = this.y + 23;
            int panelWidth = Math.max(180, this.width - 64);
            int visibleOuterHeight = RecipeInlineRenderer.getOuterHeight(summaries, panelWidth, MaterialListPlusState.recipeProgress(this.entry));
            return RecipeInlineRenderer.hoveredStackAt(summaries, panelX, panelY, panelWidth, visibleOuterHeight, mouseX, mouseY);
        }

        if (MinimalSubMaterialListView.isActive(this.materialList) && MinimalSubMaterialListView.isSourcesVisible(this.entry)) {
            int total = MinimalSubMaterialListView.total(this.entry, this.materialList);
            int missing = MinimalSubMaterialListView.netMissing(this.entry, this.materialList);
            List<MinimalSubMaterialListView.RequirementContribution> requirements = MinimalSubMaterialListView.sourceRequirements(this.entry, total, missing);
            List<MinimalSubMaterialListView.SourceContribution> sources = MinimalSubMaterialListView.sourceContributions(this.entry);
            boolean showAllSources = MinimalSubMaterialListView.isSourcesFull(this.entry);
            int panelX = this.x + 28;
            int panelY = this.y + 23;
            int panelWidth = this.minimalSourcePanelWidth();
            int visibleOuterHeight = MinimalSourceInlineRenderer.getOuterHeight(rowStack, requirements, sources, showAllSources, panelWidth, MinimalSubMaterialListView.sourceProgress(this.entry));
            return MinimalSourceInlineRenderer.hoveredStackAt(panelX, panelY, panelWidth, rowStack, requirements, sources, showAllSources, visibleOuterHeight, mouseX, mouseY);
        }

        return class_1799.field_8037;
    }

    private boolean isRowItemHovered(int mouseX, int mouseY) {
        int iconX = this.getColumnPosX(0);
        int iconY = this.y + 3;
        return isBoxHovered(iconX, iconY, 16, 16, mouseX, mouseY);
    }

    private static boolean isBoxHovered(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    private void renderVanillaMaterialHoverTooltip(class_332 drawContext, int mouseX, int mouseY) {
        class_1799 stack = MinimalSubMaterialListView.displayStack(this.entry);
        String itemLabel = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.item");
        String totalLabel = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.total");
        String missingLabel = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.missing");
        String itemText = MinimalSubMaterialListView.displayName(this.entry);
        int total = MinimalSubMaterialListView.total(this.entry, this.materialList);
        int missing = MinimalSubMaterialListView.missing(this.entry, this.materialList);
        String totalText = this.formatVanillaCount(total, stack.method_7914());
        String missingText = this.formatVanillaCount(missing, stack.method_7914());
        int labelWidth = Math.max(this.getStringWidth(itemLabel), Math.max(this.getStringWidth(totalLabel), this.getStringWidth(missingLabel)));
        int valueWidth = Math.max(this.getStringWidth(itemText) + 20, Math.max(this.getStringWidth(totalText), this.getStringWidth(missingText)));
        int panelWidth = labelWidth + valueWidth + VANILLA_TOOLTIP_LABEL_VALUE_GAP + VANILLA_TOOLTIP_PADDING_EXTRA;
        int panelX = mouseX + VANILLA_TOOLTIP_MARGIN;
        int panelY = mouseY - VANILLA_TOOLTIP_MARGIN;

        if (panelX + panelWidth - 20 >= this.width) {
            panelX -= panelWidth + 20;
        }

        int labelX = panelX + VANILLA_TOOLTIP_MARGIN;
        int valueX = labelX + labelWidth + VANILLA_TOOLTIP_LABEL_VALUE_GAP;
        int lineY = panelY + 6;
        int iconY = lineY;

        drawContext.method_51452();
        drawContext.method_51448().method_22903();
        drawContext.method_51448().method_46416(0.0F, 0.0F, 400.0F);
        lmlp$drawTooltipBox(drawContext, panelX, panelY, panelWidth, VANILLA_TOOLTIP_HEIGHT, 0xFF000000,
                0xFF999999);

        lineY += 4;
        this.drawString(labelX, lineY, 0xFFFFFFFF, itemLabel, drawContext);
        this.drawString(valueX + 20, lineY, 0xFFFFFFFF, itemText, drawContext);

        lineY += VANILLA_TOOLTIP_LINE_HEIGHT + VANILLA_TOOLTIP_HEADER_GAP;
        this.drawString(labelX, lineY, 0xFFFFFFFF, totalLabel, drawContext);
        this.drawString(valueX, lineY, 0xFFFFFFFF, totalText, drawContext);

        lineY += VANILLA_TOOLTIP_LINE_HEIGHT;
        this.drawString(labelX, lineY, 0xFFFFFFFF, missingLabel, drawContext);
        this.drawString(valueX, lineY, 0xFFFFFFFF, missingText, drawContext);

        RenderUtils.drawRect(valueX, iconY, 16, 16, 0x20FFFFFF);
        RenderUtils.enableDiffuseLightingGui3D();
        drawContext.method_51427(stack, valueX, iconY);
        RenderUtils.disableDiffuseLighting();
        drawContext.method_51448().method_22909();
    }

    private static void lmlp$drawTooltipBox(class_332 context, int x, int y, int width, int height,
            int background, int border) {
        context.method_25294(x, y, x + width, y + height, background);
        context.method_25294(x, y, x + width, y + 1, border);
        context.method_25294(x, y + height - 1, x + width, y + height, border);
        context.method_25294(x, y, x + 1, y + height, border);
        context.method_25294(x + width - 1, y, x + width, y + height, border);
    }

    private String formatVanillaCount(int count, int maxStackSize) {
        int stacks = count / maxStackSize;
        int remainder = count % maxStackSize;
        double shulkerBoxes = count / (27.0D * maxStackSize);
        String shulkerBoxAbbr = StringUtils.translate("litematica.gui.label.material_list.abbr.shulker_box");

        if (count > maxStackSize) {
            if (maxStackSize > 1) {
                if (remainder > 0) {
                    return String.format("%d = %d x %d + %d = %.2f %s", count, stacks, maxStackSize, remainder, shulkerBoxes, shulkerBoxAbbr);
                }

                return String.format("%d = %d x %d = %.2f %s", count, stacks, maxStackSize, shulkerBoxes, shulkerBoxAbbr);
            }

            return String.format("%d = %.2f %s", count, shulkerBoxes, shulkerBoxAbbr);
        }

        return String.format("%d", count);
    }

    private PanelBounds choiceTooltipBounds(int mouseX, int mouseY, int panelWidth, int panelHeight) {
        PanelBounds bounds = this.hoverTooltipBounds(mouseX, mouseY, panelWidth, panelHeight);
        if (Configs.Generic.DISABLE_LITEMATICA_HOVER_TOOLTIP.getBooleanValue()) {
            return bounds;
        }

        TooltipBounds vanilla = this.vanillaTooltipBounds(mouseX, mouseY);
        int screenWidth = this.mc.method_22683().method_4486();
        int screenHeight = this.mc.method_22683().method_4502();
        int minX = HOVER_TOOLTIP_MARGIN;
        int minY = HOVER_TOOLTIP_MARGIN;
        int maxX = Math.max(minX, screenWidth - panelWidth - HOVER_TOOLTIP_MARGIN);
        int maxY = Math.max(minY, screenHeight - panelHeight - HOVER_TOOLTIP_MARGIN);
        int alignedX = clamp(vanilla.x(), minX, maxX);

        int belowY = vanilla.y() + vanilla.height() + TOOLTIP_STACK_GAP;
        if (belowY <= maxY) {
            return new PanelBounds(alignedX, belowY);
        }

        int aboveY = vanilla.y() - panelHeight - TOOLTIP_STACK_GAP;
        if (aboveY >= minY) {
            return new PanelBounds(alignedX, aboveY);
        }

        int alignedY = clamp(vanilla.y(), minY, maxY);
        int rightX = vanilla.x() + vanilla.width() + TOOLTIP_STACK_GAP;
        if (rightX <= maxX) {
            return new PanelBounds(rightX, alignedY);
        }

        int leftX = vanilla.x() - panelWidth - TOOLTIP_STACK_GAP;
        if (leftX >= minX) {
            return new PanelBounds(leftX, alignedY);
        }

        return bounds;
    }

    private TooltipBounds vanillaTooltipBounds(int mouseX, int mouseY) {
        class_1799 stack = MinimalSubMaterialListView.displayStack(this.entry);
        String itemLabel = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.item");
        String totalLabel = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.total");
        String missingLabel = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.material_list.title.missing");
        String itemText = MinimalSubMaterialListView.displayName(this.entry);
        int total = MinimalSubMaterialListView.total(this.entry, this.materialList);
        int missing = MinimalSubMaterialListView.missing(this.entry, this.materialList);
        String totalText = this.formatVanillaCount(total, stack.method_7914());
        String missingText = this.formatVanillaCount(missing, stack.method_7914());
        int labelWidth = Math.max(this.getStringWidth(itemLabel), Math.max(this.getStringWidth(totalLabel), this.getStringWidth(missingLabel)));
        int valueWidth = Math.max(this.getStringWidth(itemText) + 20, Math.max(this.getStringWidth(totalText), this.getStringWidth(missingText)));
        int panelWidth = labelWidth + valueWidth + VANILLA_TOOLTIP_LABEL_VALUE_GAP + VANILLA_TOOLTIP_PADDING_EXTRA;
        int panelX = mouseX + VANILLA_TOOLTIP_MARGIN;
        int panelY = mouseY - VANILLA_TOOLTIP_MARGIN;

        if (panelX + panelWidth - 20 >= this.width) {
            panelX -= panelWidth + 20;
        }

        return new TooltipBounds(panelX, panelY, panelWidth, VANILLA_TOOLTIP_HEIGHT);
    }

    private PanelBounds hoverTooltipBounds(int mouseX, int mouseY, int panelWidth, int panelHeight) {
        int screenWidth = this.mc.method_22683().method_4486();
        int screenHeight = this.mc.method_22683().method_4502();
        int minX = HOVER_TOOLTIP_MARGIN;
        int minY = HOVER_TOOLTIP_MARGIN;
        int maxX = Math.max(minX, screenWidth - panelWidth - HOVER_TOOLTIP_MARGIN);
        int maxY = Math.max(minY, screenHeight - panelHeight - HOVER_TOOLTIP_MARGIN);
        int x = mouseX + HOVER_TOOLTIP_CURSOR_OFFSET;
        int y = mouseY + HOVER_TOOLTIP_CURSOR_OFFSET;

        if (x > maxX) {
            x = mouseX - HOVER_TOOLTIP_CURSOR_OFFSET - panelWidth;
        }
        if (y > maxY) {
            y = mouseY - HOVER_TOOLTIP_CURSOR_OFFSET - panelHeight;
        }

        x = clamp(x, minX, maxX);
        y = clamp(y, minY, maxY);
        return new PanelBounds(x, y);
    }

    private String truncateToWidth(String text, int maxWidth) {
        if (this.getStringWidth(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int suffixWidth = this.getStringWidth(suffix);
        int end = text.length();
        while (end > 0 && this.getStringWidth(text.substring(0, end)) + suffixWidth > maxWidth) {
            end--;
        }

        return end > 0 ? text.substring(0, end) + suffix : suffix;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean handleRecipePanelClick(int mouseX, int mouseY) {
        if (!MaterialListPlusState.isRecipeExpanded(this.entry)) {
            return false;
        }

        List<RecipeSummary> summaries = MaterialListPlusState.getSummaries(this.entry, this.materialList);
        int panelX = this.x + 28;
        int panelY = this.y + 23;
        int panelWidth = Math.max(180, this.width - 64);
        int visibleOuterHeight = RecipeInlineRenderer.getOuterHeight(summaries, panelWidth, MaterialListPlusState.recipeProgress(this.entry));
        RecipeInlineRenderer.ToggleTarget target = RecipeInlineRenderer.toggleAt(summaries, panelX, panelY, panelWidth, visibleOuterHeight, mouseX, mouseY);
        if (target.isNone()) {
            return false;
        }

        if (target.ingredient() != null) {
            MaterialListPlusState.toggleIngredientTree(target.ingredient());
        } else {
            MaterialListPlusState.toggleTreeNode(target.nodePath());
        }

        this.listWidget.refreshEntries();
        this.scrollExpandedEntryIntoView();
        return true;
    }

    private boolean openRecipePanelNavigation(int mouseX, int mouseY) {
        if (!MaterialListPlusState.isRecipeExpanded(this.entry)) {
            return false;
        }

        List<RecipeSummary> summaries = MaterialListPlusState.getSummaries(this.entry, this.materialList);
        int panelX = this.x + 28;
        int panelY = this.y + 23;
        int panelWidth = Math.max(180, this.width - 64);
        int visibleOuterHeight = RecipeInlineRenderer.getOuterHeight(summaries, panelWidth,
                MaterialListPlusState.recipeProgress(this.entry));
        RecipeInlineRenderer.NavigationTarget target = RecipeInlineRenderer.navigationTargetAt(
                summaries, panelX, panelY, panelWidth, visibleOuterHeight, mouseX, mouseY);
        if (target.isNone()) {
            return false;
        }

        int total = MaterialCounts.total(this.entry, this.materialList);
        int missing = MaterialCounts.netMissing(this.entry, this.materialList);
        class_437 parent = GuiUtils.getCurrentScreen();
        if (target.title()) {
            this.mc.method_1507(new RecipeDetailScreen(parent, this.entry.getStack(), total, missing, summaries));
        } else {
            this.mc.method_1507(new RecipeDetailScreen(parent, this.entry.getStack(), total, missing, summaries,
                    target.recipeId(), target.itemId()));
        }
        return true;
    }

    private boolean openMinimalSourceRecipe(int mouseX, int mouseY) {
        if (!MinimalSubMaterialListView.isSourcesVisible(this.entry)) {
            return false;
        }

        class_1799 stack = MinimalSubMaterialListView.displayStack(this.entry);
        int total = MinimalSubMaterialListView.total(this.entry, this.materialList);
        int missing = MinimalSubMaterialListView.netMissing(this.entry, this.materialList);
        List<MinimalSubMaterialListView.RequirementContribution> requirements = MinimalSubMaterialListView.sourceRequirements(this.entry, total, missing);
        List<MinimalSubMaterialListView.SourceContribution> sources = MinimalSubMaterialListView.sourceContributions(this.entry);
        boolean showAllSources = MinimalSubMaterialListView.isSourcesFull(this.entry);
        int panelX = this.x + 28;
        int panelY = this.y + 23;
        int panelWidth = this.minimalSourcePanelWidth();
        int visibleOuterHeight = MinimalSourceInlineRenderer.getOuterHeight(stack, requirements, sources, showAllSources, panelWidth, MinimalSubMaterialListView.sourceProgress(this.entry));
        MinimalSubMaterialListView.SourceContribution source = MinimalSourceInlineRenderer.sourceAt(panelX, panelY, panelWidth, stack, requirements, sources, showAllSources, visibleOuterHeight, mouseX, mouseY);
        if (source == null) {
            return false;
        }

        List<RecipeSummary> summaries = RecipeResolvers.findRecipes(source.icon(), source.sourceTotalCount(), source.sourceMissingCount());
        this.mc.method_1507(new RecipeDetailScreen(GuiUtils.getCurrentScreen(), source.icon(), source.sourceTotalCount(), source.sourceMissingCount(), summaries));
        return true;
    }

    private boolean toggleMinimalSourceSort(int mouseX, int mouseY) {
        if (!MinimalSubMaterialListView.isSourcesVisible(this.entry)) {
            return false;
        }

        class_1799 stack = MinimalSubMaterialListView.displayStack(this.entry);
        int total = MinimalSubMaterialListView.total(this.entry, this.materialList);
        int missing = MinimalSubMaterialListView.netMissing(this.entry, this.materialList);
        List<MinimalSubMaterialListView.RequirementContribution> requirements = MinimalSubMaterialListView.sourceRequirements(this.entry, total, missing);
        List<MinimalSubMaterialListView.SourceContribution> sources = MinimalSubMaterialListView.sourceContributions(this.entry);
        boolean showAllSources = MinimalSubMaterialListView.isSourcesFull(this.entry);
        int panelX = this.x + 28;
        int panelY = this.y + 23;
        int panelWidth = this.minimalSourcePanelWidth();
        int visibleOuterHeight = MinimalSourceInlineRenderer.getOuterHeight(stack, requirements, sources, showAllSources, panelWidth, MinimalSubMaterialListView.sourceProgress(this.entry));
        MinimalSourceInlineRenderer.SortButtonTarget target = MinimalSourceInlineRenderer.sortButtonTargetAt(
                panelX, panelY, panelWidth, stack, requirements, sources, visibleOuterHeight, mouseX, mouseY);
        if (target == MinimalSourceInlineRenderer.SortButtonTarget.NONE) {
            return false;
        }

        if (target == MinimalSourceInlineRenderer.SortButtonTarget.DIRECTION) {
            MinimalSubMaterialListView.toggleSourceSortDirection();
        } else {
            MinimalSubMaterialListView.cycleSourceSortMode();
        }
        return true;
    }

    private void scrollExpandedEntryIntoView() {
        if (!(this.listWidget instanceof WidgetListBoundsAccess access)) {
            return;
        }

        access.lmlp$scrollEntryIntoView(this.entry, EXPANDED_PANEL_BOTTOM_PADDING);
        this.listWidget.refreshEntries();
    }

    private int minimalSourcePanelWidth() {
        return Math.max(180, this.width - 36);
    }

    private static String missingColor(int missing, int available) {
        if (missing == 0) {
            return GuiBase.TXT_GREEN;
        }
        if (available >= missing) {
            return GuiBase.TXT_GOLD;
        }
        return GuiBase.TXT_RED;
    }

    private static String netMissingColor(int missing) {
        if (missing == 0) {
            return GuiBase.TXT_GREEN;
        }
        return GuiBase.TXT_RED;
    }

    private static String availableColor(int available, int missing) {
        if (available >= missing) {
            return GuiBase.TXT_GREEN;
        }
        return GuiBase.TXT_RED;
    }

    private static int missingColorInt(int missing, int available) {
        if (missing == 0) {
            return 0xFF55FF55;
        }
        if (available >= missing) {
            return 0xFFFFAA00;
        }
        return 0xFFFF5555;
    }

    private static int availableColorInt(int available, int missing) {
        if (available >= missing) {
            return 0xFF55FF55;
        }
        return 0xFFFF5555;
    }

    private record PanelBounds(int x, int y) {
    }

    private record TooltipBounds(int x, int y, int width, int height) {
    }

    private record ChoiceTooltipTarget(class_1799 icon, String name, List<MinimalSubMaterialListView.TooltipCandidate> candidates) {
    }
}
