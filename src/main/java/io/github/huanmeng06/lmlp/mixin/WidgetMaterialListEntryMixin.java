package io.github.huanmeng06.lmlp.mixin;

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
import io.github.huanmeng06.lmlp.config.Hotkeys;
import io.github.huanmeng06.lmlp.gui.MaterialListColumnLayout;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import io.github.huanmeng06.lmlp.gui.ItemTooltipRenderer;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import io.github.huanmeng06.lmlp.gui.MinimalSourceInlineRenderer;
import io.github.huanmeng06.lmlp.gui.RecipeDetailScreen;
import io.github.huanmeng06.lmlp.gui.RecipeInlineRenderer;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.MaterialCounts;
import io.github.huanmeng06.lmlp.recipe.RecipeResolvers;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import net.minecraft.class_1799;
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
    private static final int CHOICE_TOOLTIP_TWO_COLUMN_THRESHOLD = 7;
    private static final int REQUIREMENT_UPSTREAM_GAP = 18;
    private static final int REQUIREMENT_UPSTREAM_ARROW_WIDTH = 18;
    private static final int REQUIREMENT_UPSTREAM_AFTER_ARROW_GAP = 8;
    private static final int REQUIREMENT_ICON_TEXT_GAP = 26;
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
        int xItem = this.x + 4;
        int xTotal = xItem + MaterialListColumnLayout.nameWidth() + MaterialListColumnLayout.nameToTotalGap();
        int xMissing = xTotal + MaterialListColumnLayout.totalWidth() + MaterialListColumnLayout.countColumnGap();
        int xAvailable = xMissing + MaterialListColumnLayout.missingWidth() + MaterialListColumnLayout.countColumnGap();

        return switch (column) {
            case 0 -> xItem;
            case 1 -> xTotal;
            case 2 -> xMissing;
            case 3 -> xAvailable;
            case 4 -> xAvailable + MaterialListColumnLayout.availableWidth() + MaterialListColumnLayout.countColumnGap();
            default -> xItem;
        };
    }

    /**
     * @author Huan_meeng
     * @reason Use row clicks for inline recipe expansion and shift-click for the detail screen.
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
            if (!GuiBase.isShiftDown()) {
                if (this.handleRecipePanelClick(mouseX, mouseY)) {
                    return true;
                }
            }

            if (GuiBase.isShiftDown()) {
                List<RecipeSummary> summaries = MaterialListPlusState.resolveFor(this.entry, this.materialList);
                this.mc.method_1507(new RecipeDetailScreen(GuiUtils.getCurrentScreen(), this.entry.getStack(), MaterialCounts.total(this.entry, this.materialList), MaterialCounts.netMissing(this.entry, this.materialList), summaries));
            } else {
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
                this.drawString(xTotal, yText, -1, this.header2, drawContext);
                this.drawString(xMissing, yText, -1, this.header3, drawContext);
                this.drawString(xAvailable, yText, -1, this.header4, drawContext);
                this.renderColumnHeader(mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP, drawContext);
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
        this.drawString(xItem + 20, yText, -1, name, drawContext);
        this.drawString(xTotal, yText, -1, CountFormatter.formatAligned(stack, total, lmlpMaxTotalDigits), drawContext);
        this.drawString(xMissing, yText, -1, netMissingColor(missing) + CountFormatter.formatAligned(stack, missing, lmlpMaxMissingDigits), drawContext);
        this.drawString(xAvailable, yText, -1, availableColor(available, rawMissing) + CountFormatter.formatAligned(stack, available, lmlpMaxAvailableDigits), drawContext);

        drawContext.method_51448().method_22903();
        RenderUtils.enableDiffuseLightingGui3D();
        int iconY = this.y + 3;
        RenderUtils.drawRect(iconX, iconY, 16, 16, 0x20FFFFFF);
        drawContext.method_51427(stack, iconX, iconY);
        RenderUtils.disableDiffuseLighting();
        drawContext.method_51448().method_22909();

        if (MaterialListPlusState.isRecipeVisible(this.entry)) {
            List<RecipeSummary> summaries = MaterialListPlusState.getSummaries(this.entry, this.materialList);
            if (summaries.isEmpty()) {
                summaries = MaterialListPlusState.getCachedSummaries(this.entry);
            }
            int panelY = this.y + 23;
            int visibleOuterHeight = RecipeInlineRenderer.getOuterHeight(summaries, MaterialListPlusState.recipeProgress(this.entry));
            RecipeInlineRenderer.render(this, drawContext, this.x + 28, panelY, Math.max(180, this.width - 64), summaries, visibleOuterHeight, mouseX, mouseY);
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

        if (!minimalSubMaterialView) {
            super.render(mouseX, mouseY, selected, drawContext);
        }
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

        if (this.lmlp$renderPanelItemTooltip(drawContext, mouseX, mouseY)) {
            return;
        }

        if (Configs.Generic.DISABLE_LITEMATICA_HOVER_TOOLTIP.getBooleanValue()) {
            return;
        }

        this.renderVanillaMaterialHoverTooltip(drawContext, mouseX, mouseY);
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

        int maxPanelWidth = Math.max(120, this.mc.method_22683().method_4486() - HOVER_TOOLTIP_MARGIN * 2);
        int maxContentWidth = Math.max(80, maxPanelWidth - HOVER_TOOLTIP_PADDING * 2);
        int maxCandidateNameWidth = 0;
        for (MinimalSubMaterialListView.TooltipCandidate candidate : candidates) {
            maxCandidateNameWidth = Math.max(maxCandidateNameWidth, this.getStringWidth(candidate.name()));
        }

        int naturalColumnWidth = HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP + maxCandidateNameWidth;
        int minTwoColumnWidth = HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP + 60;
        boolean twoColumns = candidates.size() >= CHOICE_TOOLTIP_TWO_COLUMN_THRESHOLD
                && maxContentWidth >= minTwoColumnWidth * 2 + CHOICE_TOOLTIP_COLUMN_GAP;
        int columns = twoColumns ? 2 : 1;
        int columnGap = twoColumns ? CHOICE_TOOLTIP_COLUMN_GAP : 0;
        int columnWidth = twoColumns
                ? Math.min(naturalColumnWidth, Math.max(minTwoColumnWidth, (maxContentWidth - columnGap) / 2))
                : Math.min(naturalColumnWidth, maxContentWidth);
        int rowsPerColumn = (candidates.size() + columns - 1) / columns;
        int candidateContentWidth = columns * columnWidth + columnGap;

        class_1799 headerStack = target.icon();
        String headerText = this.truncateToWidth(
                target.name(),
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

        drawContext.method_51448().method_22903();
        drawContext.method_51448().method_46416(0.0F, 0.0F, 200.0F);
        RenderUtils.drawOutlinedBox(panelX, panelY, panelWidth, panelHeight, 0xF0000000, 0xFF999999);

        RenderUtils.drawRect(contentX, headerY - 4, HOVER_TOOLTIP_ICON_SIZE, HOVER_TOOLTIP_ICON_SIZE, 0x20FFFFFF);
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

            RenderUtils.drawRect(rowX, rowTop + 1, HOVER_TOOLTIP_ICON_SIZE, HOVER_TOOLTIP_ICON_SIZE, 0x20FFFFFF);
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
        int visibleHeight = Math.max(0, visibleOuterHeight - 4);
        int textX = panelX + 8 + 26;
        int rowY = panelY + 8 + 24 + 18;

        for (MinimalSubMaterialListView.RequirementContribution requirement : requirements) {
            List<MinimalSubMaterialListView.TooltipCandidate> candidates = MinimalSubMaterialListView.requirementTooltipCandidates(requirement);
            String name = MinimalSubMaterialListView.requirementDisplayName(requirement);
            int textY = rowY + 2;
            if (!candidates.isEmpty()
                    && isTextHovered(textX, textY, this.getStringWidth(name), mouseX, mouseY)
                    && mouseY >= panelY
                    && mouseY < panelY + visibleHeight) {
                return new ChoiceTooltipTarget(requirement.icon(), name, candidates);
            }

            MinimalSubMaterialListView.UpstreamRequirement upstream = requirement.upstream();
            List<MinimalSubMaterialListView.TooltipCandidate> upstreamCandidates = MinimalSubMaterialListView.upstreamTooltipCandidates(upstream);
            if (upstream != null && !upstreamCandidates.isEmpty()) {
                String primaryLine = name + ": " + GuiBase.TXT_GOLD + CountFormatter.format(requirement.totalCount(), requirement.maxStackSize());
                String upstreamName = MinimalSubMaterialListView.upstreamDisplayName(upstream);
                int upstreamTextX = textX
                        + StringUtils.getStringWidth(primaryLine)
                        + REQUIREMENT_UPSTREAM_GAP
                        + REQUIREMENT_UPSTREAM_ARROW_WIDTH
                        + REQUIREMENT_UPSTREAM_AFTER_ARROW_GAP
                        + REQUIREMENT_ICON_TEXT_GAP;
                if (isTextHovered(upstreamTextX, textY, this.getStringWidth(upstreamName), mouseX, mouseY)
                        && mouseY >= panelY
                        && mouseY < panelY + visibleHeight) {
                    return new ChoiceTooltipTarget(upstream.icon(), upstreamName, upstreamCandidates);
                }
            }
            rowY += 22;
        }

        return null;
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
        return MinimalSourceInlineRenderer.isTargetNameHovered(this.x + 28, panelY, stack, nameWidth, requirements, sources, visibleOuterHeight, mouseX, mouseY);
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
            int visibleOuterHeight = RecipeInlineRenderer.getOuterHeight(summaries, MaterialListPlusState.recipeProgress(this.entry));
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

    private void renderMaterialHoverTooltip(class_332 drawContext, int mouseX, int mouseY, boolean detailed) {
        class_1799 stack = MinimalSubMaterialListView.displayStack(this.entry);
        String totalLabel = StringUtils.translate(detailed ? "litematica.gui.label.material_list.title.total" : "lmlp.label.recipe.total_short");
        String missingLabel = StringUtils.translate(detailed ? "litematica.gui.label.material_list.title.missing" : "lmlp.label.recipe.missing_short");
        String availableLabel = StringUtils.translate("litematica.gui.label.material_list.title.available");
        String itemText = MinimalSubMaterialListView.displayName(this.entry);
        int total = MinimalSubMaterialListView.total(this.entry, this.materialList);
        int missing = MinimalSubMaterialListView.missing(this.entry, this.materialList);
        int available = this.entry.getCountAvailable();
        int maxPanelWidth = Math.max(80, this.mc.method_22683().method_4486() - HOVER_TOOLTIP_MARGIN * 2);
        int maxTextWidth = Math.max(40, maxPanelWidth - HOVER_TOOLTIP_PADDING * 2);
        int maxHeaderTextWidth = Math.max(20, maxTextWidth - HOVER_TOOLTIP_ICON_SIZE - HOVER_TOOLTIP_ICON_GAP);
        String headerText = this.truncateToWidth(itemText, maxHeaderTextWidth);
        String totalText = detailed ? CountFormatter.format(stack, total) : Integer.toString(total);
        String missingText = detailed ? CountFormatter.format(stack, missing) : Integer.toString(missing);
        String availableText = detailed ? CountFormatter.format(stack, available) : Integer.toString(available);
        String countLine = missingLabel + ": " + missingText + " / " + availableLabel + ": " + availableText;
        String availableLine = availableLabel + ": " + availableText;
        String hintLine = detailed ? "" : GuiBase.TXT_YELLOW + GuiBase.TXT_ITALIC + StringUtils.translate("lmlp.label.hover.detail_hint", this.detailHoverKeyDisplayName()) + GuiBase.TXT_RST;

        if (detailed) {
            countLine = totalLabel + ": " + totalText;
        }

        countLine = this.truncateToWidth(countLine, maxTextWidth);
        availableLine = this.truncateToWidth(availableLine, maxTextWidth);
        hintLine = this.truncateToWidth(hintLine, maxTextWidth);
        String missingLine = this.truncateToWidth(missingLabel + ": " + missingText, maxTextWidth);
        int headerWidth = HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP + this.getStringWidth(headerText);
        int countWidth = this.getStringWidth(countLine);
        int missingWidth = detailed ? this.getStringWidth(missingLine) : 0;
        int availableWidth = detailed ? this.getStringWidth(availableLine) : 0;
        int hintWidth = detailed ? 0 : this.getStringWidth(hintLine);
        int panelWidth = Math.min(maxPanelWidth, Math.max(headerWidth, Math.max(countWidth, Math.max(missingWidth, Math.max(availableWidth, hintWidth)))) + HOVER_TOOLTIP_PADDING * 2);
        int lineCount = detailed ? 4 : 3;
        int panelHeight = HOVER_TOOLTIP_PADDING * 2 + Math.max(HOVER_TOOLTIP_ICON_SIZE, HOVER_TOOLTIP_LINE_HEIGHT) + HOVER_TOOLTIP_HEADER_GAP + (lineCount - 1) * HOVER_TOOLTIP_LINE_HEIGHT;

        PanelBounds bounds = this.hoverTooltipBounds(mouseX, mouseY, panelWidth, panelHeight);
        int panelX = bounds.x();
        int panelY = bounds.y();
        int lineX = panelX + HOVER_TOOLTIP_PADDING;
        int lineY = panelY + HOVER_TOOLTIP_PADDING + 4;

        drawContext.method_51448().method_22903();
        drawContext.method_51448().method_46416(0.0F, 0.0F, 200.0F);
        RenderUtils.drawOutlinedBox(panelX, panelY, panelWidth, panelHeight, 0xF0000000, 0xFF999999);

        RenderUtils.drawRect(lineX, lineY - 4, HOVER_TOOLTIP_ICON_SIZE, HOVER_TOOLTIP_ICON_SIZE, 0x20FFFFFF);
        RenderUtils.enableDiffuseLightingGui3D();
        drawContext.method_51427(stack, lineX, lineY - 4);
        RenderUtils.disableDiffuseLighting();
        this.drawString(lineX + HOVER_TOOLTIP_ICON_SIZE + HOVER_TOOLTIP_ICON_GAP, lineY, 0xFFFFFFFF, headerText, drawContext);

        lineY += HOVER_TOOLTIP_LINE_HEIGHT + HOVER_TOOLTIP_HEADER_GAP;
        this.drawString(lineX, lineY, detailed ? 0xFFFFFFFF : missingColorInt(missing, available), countLine, drawContext);

        if (detailed) {
            lineY += HOVER_TOOLTIP_LINE_HEIGHT;
            this.drawString(lineX, lineY, missingColorInt(missing, available), missingLine, drawContext);

            lineY += HOVER_TOOLTIP_LINE_HEIGHT;
            this.drawString(lineX, lineY, availableColorInt(available, missing), availableLine, drawContext);
        } else {
            lineY += HOVER_TOOLTIP_LINE_HEIGHT;
            this.drawString(lineX, lineY, 0xFFFFFFFF, hintLine, drawContext);
        }

        drawContext.method_51448().method_22909();
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

        drawContext.method_51448().method_22903();
        drawContext.method_51448().method_46416(0.0F, 0.0F, 200.0F);
        RenderUtils.drawOutlinedBox(panelX, panelY, panelWidth, VANILLA_TOOLTIP_HEIGHT, 0xFF000000, 0xFF999999);

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

    private boolean isDetailHoverKeyDown() {
        return Hotkeys.SHOW_HOVER_DETAILS.getKeybind().isKeybindHeld();
    }

    private String detailHoverKeyDisplayName() {
        String keyName = Hotkeys.SHOW_HOVER_DETAILS.getKeybind().getKeysDisplayString();
        return keyName.isEmpty() ? StringUtils.translate("lmlp.label.hover.detail_key_unbound") : keyName;
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
        int visibleOuterHeight = RecipeInlineRenderer.getOuterHeight(summaries, MaterialListPlusState.recipeProgress(this.entry));
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
