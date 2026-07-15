package io.github.huanmeng06.lmlp.gui;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.FamilyIconCycle;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import fi.dy.masa.malilib.render.GuiContext;

public final class MinimalSourceInlineRenderer {
    private static final String UNDERLINE = "\u00A7n";
    private static final int ROW_HEIGHT = 22;
    private static final int PADDING = 8;
    private static final int INNER_BOTTOM_PADDING = 4;
    private static final int ENTRY_BOTTOM_GAP = 4;
    private static final int SOURCE_ICON_BOX_SIZE = 18;
    private static final int SOURCE_ICON_BOX_Y_OFFSET = -3;
    private static final int SECTION_GAP = 6;
    private static final int SORT_BUTTON_HEIGHT = 16;
    private static final int SORT_BUTTON_PADDING_X = 4;
    private static final int SORT_BUTTON_GAP = 2;
    private static final int SORT_DIRECTION_BUTTON_SIZE = 16;
    private static final int TEXT_HOVER_HEIGHT = 12;
    private static final int UPSTREAM_GAP = 18;
    private static final int UPSTREAM_ARROW_WIDTH = 18;
    // When the upstream requirement doesn't fit on the same line as the main
    // material, it wraps to a second line indented to align under the main
    // material's text (past the 18px icon box + its 8px gap = 26px).
    private static final int WRAP_INDENT = 26;
    private static final int COLUMN_GAP = 28;
    private static final int TWO_COLUMN_THRESHOLD = 4;
    private static final int THREE_COLUMN_THRESHOLD = 7;
    private static final int COLUMN_SEPARATOR_COLOR = 0xFF999999;
    // Per-family window + non-wood fallback step; see FamilyIconCycle. Kept in
    // sync with AlternativeItemDisplay so the recipe panel and the minimal
    // sub-material panel animate choice groups identically.
    private static final long FAMILY_CYCLE_MILLIS = FamilyIconCycle.FAMILY_WINDOW_MILLIS;
    private static final long ICON_CYCLE_MILLIS = FamilyIconCycle.FALLBACK_STEP_MILLIS;

    private MinimalSourceInlineRenderer() {
    }

    public static int getHeight(ItemStack targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll) {
        return getHeight(targetIcon, requirements, sources, showAll, 0);
    }

    public static int getHeight(ItemStack targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, int width) {
        if (sources.isEmpty()) {
            return 48;
        }
        if (isSelfSource(targetIcon, sources)) {
            return 56;
        }

        int panelWidth = panelWidthFor(width);
        int contentWidth = panelWidth - PADDING * 2;
        SourceColumnLayout layout = sourceColumnLayout(contentWidth, sources);
        return 50 + requirementHeight(requirements, contentWidth) + layout.rowCount() * ROW_HEIGHT + INNER_BOTTOM_PADDING;
    }

    public static int getOuterHeight(ItemStack targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll) {
        return getHeight(targetIcon, requirements, sources, showAll) + ENTRY_BOTTOM_GAP;
    }

    public static int getOuterHeight(ItemStack targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, int width) {
        return getHeight(targetIcon, requirements, sources, showAll, width) + ENTRY_BOTTOM_GAP;
    }

    public static int getOuterHeight(ItemStack targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, float progress) {
        return Math.round(getOuterHeight(targetIcon, requirements, sources, showAll) * progress);
    }

    public static int getOuterHeight(ItemStack targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, int width, float progress) {
        return Math.round(getOuterHeight(targetIcon, requirements, sources, showAll, width) * progress);
    }

    public static int getRequiredPanelWidth(List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources) {
        return requiredContentWidth(requirements, sources);
    }

    public static boolean isTargetNameHovered(int x, int y, int width, ItemStack targetIcon, int targetNameWidth, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, int visibleOuterHeight, int mouseX, int mouseY) {
        int visibleHeight = Math.max(0, visibleOuterHeight - ENTRY_BOTTOM_GAP);
        int textX = x + PADDING + 24;
        int textY = y + PADDING + 4;
        if (isVisibleTextHovered(textX, textY, targetNameWidth, y, visibleHeight, mouseX, mouseY)) {
            return true;
        }

        if (sources.isEmpty()) {
            return false;
        }

        int contentWidth = panelWidthFor(width) - PADDING * 2;
        int cursorY = y + PADDING + 24;
        textX = x + PADDING;
        if (isSelfSource(targetIcon, sources)) {
            return isVisibleTextHovered(textX, cursorY + 2, targetNameWidth, y, visibleHeight, mouseX, mouseY);
        }

        if (!requirements.isEmpty()) {
            if (isVisibleTextHovered(textX, cursorY, targetNameWidth, y, visibleHeight, mouseX, mouseY)) {
                return true;
            }
            cursorY += 18 + requirementsBlockRowsHeight(requirements, contentWidth) + SECTION_GAP;
        }

        return isVisibleTextHovered(textX, cursorY, targetNameWidth, y, visibleHeight, mouseX, mouseY);
    }

    public static MinimalSubMaterialListView.SourceContribution sourceAt(int x, int y, int width, ItemStack targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, int visibleOuterHeight, int mouseX, int mouseY) {
        if (sources.isEmpty() || isSelfSource(targetIcon, sources)) {
            return null;
        }

        int panelWidth = panelWidthFor(width);
        int contentWidth = Math.max(1, panelWidth - PADDING * 2);
        int visibleHeight = Math.max(0, Math.min(getHeight(targetIcon, requirements, sources, showAll, width), visibleOuterHeight));
        int cursorY = y + PADDING + 24;
        if (!requirements.isEmpty()) {
            cursorY += 18 + requirementsBlockRowsHeight(requirements, contentWidth) + SECTION_GAP;
        }
        cursorY += 18;

        int contentX = x + PADDING;
        SourceColumnLayout layout = sourceColumnLayout(contentWidth, sources);
        int visibleCount = visibleSourceCount(sources);

        for (int index = 0; index < visibleCount; index++) {
            MinimalSubMaterialListView.SourceContribution source = sources.get(index);
            int column = index / layout.rowCount();
            int row = index % layout.rowCount();
            int rowX = contentX + layout.columnX(column);
            int rowY = cursorY + row * ROW_HEIGHT;
            int lineX = rowX + 26;
            int lineY = rowY + 2;
            int lineWidth = StringUtils.getStringWidth(countLine(source.name(), source.totalCount(), source.missingCount(), source.maxStackSize()));
            if (isVisibleTextHovered(lineX, lineY, lineWidth, y, visibleHeight, mouseX, mouseY)) {
                return source;
            }
        }

        return null;
    }

    public static ItemStack hoveredStackAt(int x, int y, int width, ItemStack targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, int visibleOuterHeight, int mouseX, int mouseY) {
        if (sources.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int panelWidth = panelWidthFor(width);
        int contentWidth = Math.max(1, panelWidth - PADDING * 2);
        int visibleHeight = Math.max(0, Math.min(getHeight(targetIcon, requirements, sources, showAll, width), visibleOuterHeight));
        int textX = x + PADDING;
        int cursorY = y + PADDING;
        if (isVisibleBoxHovered(textX, cursorY, SOURCE_ICON_BOX_SIZE, SOURCE_ICON_BOX_SIZE, y, visibleHeight, mouseX, mouseY)) {
            return targetIcon;
        }

        cursorY += 24;
        if (isSelfSource(targetIcon, sources)) {
            return ItemStack.EMPTY;
        }

        if (!requirements.isEmpty()) {
            cursorY += 18;
            for (MinimalSubMaterialListView.RequirementContribution requirement : requirements) {
                ItemStack stack = hoveredRequirementStackAt(requirement, textX, cursorY, y, visibleHeight, mouseX, mouseY, contentWidth);
                if (!stack.isEmpty()) {
                    return stack;
                }
                cursorY += requirementLineCount(requirement, contentWidth) * ROW_HEIGHT;
            }
            cursorY += SECTION_GAP;
        }

        cursorY += 18;
        SourceColumnLayout layout = sourceColumnLayout(contentWidth, sources);
        int visibleCount = visibleSourceCount(sources);

        for (int index = 0; index < visibleCount; index++) {
            MinimalSubMaterialListView.SourceContribution source = sources.get(index);
            int column = index / layout.rowCount();
            int row = index % layout.rowCount();
            int rowX = textX + layout.columnX(column);
            int rowY = cursorY + row * ROW_HEIGHT;
            if (isCountRowHovered(rowX, rowY, countLine(source.name(), source.totalCount(), source.missingCount(), source.maxStackSize()), y, visibleHeight, mouseX, mouseY)) {
                return source.icon();
            }
        }

        return ItemStack.EMPTY;
    }

    public static void render(WidgetBase widget, GuiContext context, int x, int y, int width, ItemStack targetIcon, String targetName, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, int visibleOuterHeight, int mouseX, int mouseY) {
        int height = getHeight(targetIcon, requirements, sources, showAll, width);
        int panelWidth = panelWidthFor(width);
        int contentWidth = panelWidth - PADDING * 2;
        int visibleHeight = Math.max(0, Math.min(height, visibleOuterHeight));
        if (visibleHeight <= 0) {
            return;
        }

        RenderUtils.drawRect(context, x, y, panelWidth, visibleHeight, 0xDD000000);
        int textX = x + PADDING;
        if (visibleHeight <= 2) {
            drawOutline(context, x, y, panelWidth, visibleHeight, 0xFF777777);
            return;
        }

        context.enableScissor(x + 1, y + 1, x + panelWidth - 1, y + visibleHeight - 1);

        if (sources.isEmpty()) {
            widget.drawString(context, textX, y + 16, 0xFFFFCC66, StringUtils.translate("lmlp.label.minimal_sources.none"));
            context.disableScissor();
            drawOutline(context, x, y, panelWidth, visibleHeight, 0xFF777777);
            return;
        }

        int cursorY = y + PADDING;
        context.renderItem(targetIcon, textX, cursorY);
        widget.drawString(context, textX + 24, cursorY + 4, 0xFFFFFFFF, GuiBase.TXT_BOLD + targetName);
        cursorY += 24;

        String boldTargetName = GuiBase.TXT_BOLD + targetName + GuiBase.TXT_RST;
        if (isSelfSource(targetIcon, sources)) {
            widget.drawString(context, textX, cursorY + 2, 0xFFFFFFFF, StringUtils.translate("lmlp.label.minimal_sources.self_material", boldTargetName));
            context.disableScissor();
            drawOutline(context, x, y, panelWidth, visibleHeight, 0xFF777777);
            return;
        }

        if (!requirements.isEmpty()) {
            widget.drawString(context, textX, cursorY, 0xFFFFFFFF, StringUtils.translate("lmlp.label.minimal_sources.requires_named", boldTargetName));
            cursorY += 18;

            int requirementBoxY = cursorY;
            int requirementBoxHeight = requirementsBlockRowsHeight(requirements, contentWidth);
            RenderUtils.drawRect(context, textX - 2, requirementBoxY - 2, panelWidth - PADDING * 2 + 4,
                    requirementBoxHeight, 0x66000000);
            int rowTop = cursorY;
            for (int index = 0; index < requirements.size(); index++) {
                MinimalSubMaterialListView.RequirementContribution requirement = requirements.get(index);
                renderRequirementRow(widget, context, textX, rowTop, requirement, contentWidth);
                rowTop += requirementLineCount(requirement, contentWidth) * ROW_HEIGHT;
            }
            cursorY += requirementBoxHeight + SECTION_GAP;
        }

        widget.drawString(context, textX, cursorY, 0xFFFFFFFF, StringUtils.translate("lmlp.label.minimal_sources.header_named", boldTargetName));
        String sortLabel = sortButtonLabel();
        SortButtonBounds sortButton = sortButtonBounds(x, panelWidth, cursorY, sortLabel);
        SortButtonTarget sortTarget = sortButtonTarget(sortButton, y, visibleHeight, mouseX, mouseY);
        RenderUtils.drawRect(context, sortButton.modeX(), sortButton.y(), sortButton.modeWidth(), sortButton.height(),
                0x30FFFFFF);
        RenderUtils.drawRect(context, sortButton.directionX(), sortButton.y(), sortButton.directionSize(),
                sortButton.directionSize(), 0x30FFFFFF);
        widget.drawString(context, sortButton.modeX() + SORT_BUTTON_PADDING_X, cursorY, 0xFFFFFFFF, sortLabel);
        String directionLabel = MinimalSubMaterialListView.sourceSortDescending() ? "↓" : "↑";
        int directionLabelWidth = StringUtils.getStringWidth(directionLabel);
        widget.drawString(
                context,
                sortButton.directionX() + (sortButton.directionSize() - directionLabelWidth) / 2,
                cursorY,
                0xFFFFFFFF,
                directionLabel);
        if (sortTarget == SortButtonTarget.MODE) {
            drawOutline(context, sortButton.modeX(), sortButton.y(), sortButton.modeWidth(), sortButton.height(),
                    0xFFFFFFFF);
            ClickableCursor.requestHand();
        } else if (sortTarget == SortButtonTarget.DIRECTION) {
            drawOutline(context, sortButton.directionX(), sortButton.y(), sortButton.directionSize(),
                    sortButton.directionSize(), 0xFFFFFFFF);
            ClickableCursor.requestHand();
        }
        cursorY += 18;

        int boxY = cursorY;
        SourceColumnLayout layout = sourceColumnLayout(Math.max(1, contentWidth), sources);
        int visibleCount = visibleSourceCount(sources);
        int boxHeight = layout.rowCount() * ROW_HEIGHT;
        RenderUtils.drawRect(context, textX - 2, boxY - 2, panelWidth - PADDING * 2 + 4, boxHeight, 0x66000000);

        MinimalSubMaterialListView.SourceContribution hoveredSource = sourceAt(x, y, width, targetIcon, requirements, sources, showAll, visibleOuterHeight, mouseX, mouseY);
        if (hoveredSource != null) {
            ClickableCursor.requestHand();
        }
        if (layout.columns() > 1) {
            drawColumnSeparators(context, textX, boxY, layout);
        }

        for (int index = 0; index < visibleCount; index++) {
            MinimalSubMaterialListView.SourceContribution source = sources.get(index);
            int column = index / layout.rowCount();
            int row = index % layout.rowCount();
            int rowX = textX + layout.columnX(column);
            int rowY = cursorY + row * ROW_HEIGHT;
            renderSourceRow(widget, context, rowX, rowY, source, source == hoveredSource);
        }

        context.disableScissor();
        drawOutline(context, x, y, panelWidth, visibleHeight, 0xFF777777);
    }

    private static int headerRowY(int y, List<MinimalSubMaterialListView.RequirementContribution> requirements, int contentWidth) {
        int cursorY = y + PADDING + 24;
        if (!requirements.isEmpty()) {
            cursorY += 18 + requirementsBlockRowsHeight(requirements, contentWidth) + SECTION_GAP;
        }
        return cursorY;
    }

    private static String sortButtonLabel() {
        String key = MinimalSubMaterialListView.sourceSortMode() == MinimalSubMaterialListView.SourceSortMode.TOTAL_COUNT
                ? "lmlp.label.minimal_sources.sort_total"
                : "lmlp.label.minimal_sources.sort_missing";
        return StringUtils.translate(key);
    }

    public static SortButtonTarget sortButtonTargetAt(int x, int y, int width, ItemStack targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, int visibleOuterHeight, int mouseX, int mouseY) {
        if (sources.isEmpty() || isSelfSource(targetIcon, sources)) {
            return SortButtonTarget.NONE;
        }

        int panelWidth = panelWidthFor(width);
        int contentWidth = Math.max(1, panelWidth - PADDING * 2);
        int visibleHeight = Math.max(0, visibleOuterHeight - ENTRY_BOTTOM_GAP);
        int cursorY = headerRowY(y, requirements, contentWidth);
        String label = sortButtonLabel();
        return sortButtonTarget(sortButtonBounds(x, panelWidth, cursorY, label), y, visibleHeight, mouseX, mouseY);
    }

    private static SortButtonBounds sortButtonBounds(int panelX, int panelWidth, int cursorY, String label) {
        int labelWidth = StringUtils.getStringWidth(label);
        int modeWidth = labelWidth + SORT_BUTTON_PADDING_X * 2;
        int directionX = panelX + panelWidth - PADDING - SORT_DIRECTION_BUTTON_SIZE;
        int modeX = directionX - SORT_BUTTON_GAP - modeWidth;
        int y = cursorY - (SORT_BUTTON_HEIGHT - 10) / 2;
        return new SortButtonBounds(modeX, y, modeWidth, SORT_BUTTON_HEIGHT, directionX, SORT_DIRECTION_BUTTON_SIZE);
    }

    private static SortButtonTarget sortButtonTarget(SortButtonBounds bounds, int panelY, int visibleHeight, int mouseX, int mouseY) {
        if (isVisibleBoxHovered(bounds.directionX(), bounds.y(), bounds.directionSize(), bounds.directionSize(), panelY, visibleHeight, mouseX, mouseY)) {
            return SortButtonTarget.DIRECTION;
        }

        return isVisibleBoxHovered(bounds.modeX(), bounds.y(), bounds.modeWidth(), bounds.height(), panelY, visibleHeight, mouseX, mouseY)
                ? SortButtonTarget.MODE
                : SortButtonTarget.NONE;
    }

    private static int visibleSourceCount(List<MinimalSubMaterialListView.SourceContribution> sources) {
        return sources.size();
    }

    private static int requirementHeight(List<MinimalSubMaterialListView.RequirementContribution> requirements, int contentWidth) {
        return requirements.isEmpty() ? 0 : 18 + requirementsBlockRowsHeight(requirements, contentWidth) + SECTION_GAP;
    }

    // Total pixel height of the requirement rows (excluding the "所需" header
    // and the section gap): each requirement is 1 row tall, or 2 when its
    // upstream requirement wraps to a second line at this content width.
    private static int requirementsBlockRowsHeight(List<MinimalSubMaterialListView.RequirementContribution> requirements, int contentWidth) {
        int height = 0;
        for (MinimalSubMaterialListView.RequirementContribution requirement : requirements) {
            height += requirementLineCount(requirement, contentWidth) * ROW_HEIGHT;
        }
        return height;
    }

    // 1 line normally; 2 when the requirement has an upstream requirement and
    // the full horizontal "main ▶ upstream" row is wider than the content
    // width (so the upstream wraps to a second, indented line).
    private static int requirementLineCount(MinimalSubMaterialListView.RequirementContribution requirement, int contentWidth) {
        return requirement.upstream() != null && requirementFullWidth(requirement) > contentWidth ? 2 : 1;
    }

    // Width of the main material row: icon box (26 incl. gap) + "name: counts".
    private static int requirementMainWidth(MinimalSubMaterialListView.RequirementContribution requirement) {
        return 26 + StringUtils.getStringWidth(countLine(
                MinimalSubMaterialListView.requirementDisplayName(requirement),
                requirement.totalCount(),
                requirement.missingCount(),
                requirement.maxStackSize()));
    }

    // Width of the wrapped upstream line, measured from the row's left edge:
    // indent + arrow + gap + upstream icon box (26) + "upstream: counts".
    private static int upstreamLineWidth(MinimalSubMaterialListView.UpstreamRequirement upstream) {
        return WRAP_INDENT + UPSTREAM_ARROW_WIDTH + 8 + 26 + StringUtils.getStringWidth(countLine(
                MinimalSubMaterialListView.upstreamDisplayName(upstream),
                upstream.totalCount(),
                upstream.missingCount(),
                upstream.maxStackSize()));
    }

    // Full width of the requirement rendered horizontally on one line:
    // main + gap + arrow + gap + upstream icon box (26) + "upstream: counts".
    private static int requirementFullWidth(MinimalSubMaterialListView.RequirementContribution requirement) {
        int width = requirementMainWidth(requirement);
        MinimalSubMaterialListView.UpstreamRequirement upstream = requirement.upstream();
        if (upstream != null) {
            width += UPSTREAM_GAP + UPSTREAM_ARROW_WIDTH + 8 + 26 + StringUtils.getStringWidth(countLine(
                    MinimalSubMaterialListView.upstreamDisplayName(upstream),
                    upstream.totalCount(),
                    upstream.missingCount(),
                    upstream.maxStackSize()));
        }
        return width;
    }

    // Minimum content width this requirement needs so that neither its main
    // line nor its wrapped upstream line ever clips (independent of whether it
    // ends up on one line or two). The full one-line width is intentionally
    // NOT required — that's exactly what triggers wrapping.
    private static int requirementContentWidth(MinimalSubMaterialListView.RequirementContribution requirement) {
        MinimalSubMaterialListView.UpstreamRequirement upstream = requirement.upstream();
        if (upstream == null) {
            return requirementMainWidth(requirement);
        }
        return Math.max(requirementMainWidth(requirement), upstreamLineWidth(upstream));
    }

    private static int widestRequirementContentWidth(List<MinimalSubMaterialListView.RequirementContribution> requirements) {
        int width = 0;
        for (MinimalSubMaterialListView.RequirementContribution requirement : requirements) {
            width = Math.max(width, requirementContentWidth(requirement));
        }
        return width;
    }

    private static boolean isVisibleTextHovered(int textX, int textY, int textWidth, int panelY, int visibleHeight, int mouseX, int mouseY) {
        return textWidth > 0
                && mouseX >= textX
                && mouseX < textX + textWidth
                && mouseY >= textY
                && mouseY < textY + TEXT_HOVER_HEIGHT
                && mouseY >= panelY
                && mouseY < panelY + visibleHeight;
    }

    private static int sourceColumnCount(int visibleCount) {
        if (visibleCount >= THREE_COLUMN_THRESHOLD) {
            return 3;
        }
        if (visibleCount >= TWO_COLUMN_THRESHOLD) {
            return 2;
        }
        return 1;
    }

    private static int sourceRowCount(List<MinimalSubMaterialListView.SourceContribution> sources) {
        int visibleCount = visibleSourceCount(sources);
        return sourceRowCount(visibleCount, sourceColumnCount(visibleCount));
    }

    private static int sourceRowCount(int visibleCount, int columns) {
        return (visibleCount + columns - 1) / columns;
    }

    private static boolean isSelfSource(ItemStack targetIcon, List<MinimalSubMaterialListView.SourceContribution> sources) {
        return sources.size() == 1 && ItemStackTexts.id(targetIcon).equals(ItemStackTexts.id(sources.get(0).icon()));
    }

    private static void renderSourceRow(WidgetBase widget, GuiContext context, int textX, int y, MinimalSubMaterialListView.SourceContribution source, boolean underlined) {
        renderCountRow(widget, context, textX, y, source.icon(), source.name(), source.totalCount(), source.missingCount(), source.maxStackSize(), underlined);
    }

    private static void renderRequirementRow(WidgetBase widget, GuiContext context, int textX, int y, MinimalSubMaterialListView.RequirementContribution requirement, int contentWidth) {
        String line = renderCountRow(widget, context, textX, y, cyclingIcon(requirement.icons(), requirement.icon()), MinimalSubMaterialListView.requirementDisplayName(requirement), requirement.totalCount(), requirement.missingCount(), requirement.maxStackSize());
        MinimalSubMaterialListView.UpstreamRequirement upstream = requirement.upstream();
        if (upstream == null) {
            return;
        }

        int arrowX;
        int rowY;
        if (requirementLineCount(requirement, contentWidth) == 1) {
            // Fits on one line: arrow + upstream right after the main material.
            arrowX = textX + 26 + StringUtils.getStringWidth(line) + UPSTREAM_GAP;
            rowY = y;
        } else {
            // Doesn't fit: wrap the arrow + upstream onto a second line, indented
            // to align under the main material's text.
            arrowX = textX + WRAP_INDENT;
            rowY = y + ROW_HEIGHT;
        }

        int centerY = rowY + SOURCE_ICON_BOX_Y_OFFSET + SOURCE_ICON_BOX_SIZE / 2;
        ToggleArrowRenderer.render(context, arrowX, UPSTREAM_ARROW_WIDTH, centerY, 0.0F, false);

        int upstreamIconX = arrowX + UPSTREAM_ARROW_WIDTH + 8;
        renderCountRow(
                widget,
                context,
                upstreamIconX,
                rowY,
                cyclingIcon(upstream.icons(), upstream.icon()),
                MinimalSubMaterialListView.upstreamDisplayName(upstream),
                upstream.totalCount(),
                upstream.missingCount(),
                upstream.maxStackSize());
    }

    private static ItemStack hoveredRequirementStackAt(MinimalSubMaterialListView.RequirementContribution requirement, int textX, int y, int panelY, int visibleHeight, int mouseX, int mouseY, int contentWidth) {
        String line = countLine(MinimalSubMaterialListView.requirementDisplayName(requirement), requirement.totalCount(), requirement.missingCount(), requirement.maxStackSize());
        if (isCountRowHovered(textX, y, line, panelY, visibleHeight, mouseX, mouseY)) {
            return cyclingIcon(requirement.icons(), requirement.icon());
        }

        MinimalSubMaterialListView.UpstreamRequirement upstream = requirement.upstream();
        if (upstream == null) {
            return ItemStack.EMPTY;
        }

        int arrowX;
        int rowY;
        if (requirementLineCount(requirement, contentWidth) == 1) {
            arrowX = textX + 26 + StringUtils.getStringWidth(line) + UPSTREAM_GAP;
            rowY = y;
        } else {
            arrowX = textX + WRAP_INDENT;
            rowY = y + ROW_HEIGHT;
        }

        int upstreamIconX = arrowX + UPSTREAM_ARROW_WIDTH + 8;
        String upstreamLine = countLine(MinimalSubMaterialListView.upstreamDisplayName(upstream), upstream.totalCount(), upstream.missingCount(), upstream.maxStackSize());
        if (isCountRowHovered(upstreamIconX, rowY, upstreamLine, panelY, visibleHeight, mouseX, mouseY)) {
            return cyclingIcon(upstream.icons(), upstream.icon());
        }

        return ItemStack.EMPTY;
    }

    /** A hovered "任意X" name in the requirement block: which requirement/upstream
     * plus the icon currently on screen (cycling with the family clock). */
    public record RequirementNameHit(ItemStack icon, MinimalSubMaterialListView.RequirementContribution requirement, boolean upstream) {
    }

    // Hit-test the requirement block's yellow "任意X" name text (the trigger for the
    // rich choice-group grid tooltip), reusing render()'s exact layout so the
    // hit-box follows the same one-line/wrapped placement and the returned icon
    // matches the inline cycling icon. Returns null when no requirement name is
    // hovered.
    public static RequirementNameHit hoveredRequirementName(int x, int y, int width, ItemStack targetIcon, List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources, boolean showAll, int visibleOuterHeight, int mouseX, int mouseY) {
        if (requirements.isEmpty() || sources.isEmpty() || isSelfSource(targetIcon, sources)) {
            return null;
        }

        int panelWidth = panelWidthFor(width);
        int contentWidth = Math.max(1, panelWidth - PADDING * 2);
        int visibleHeight = Math.max(0, Math.min(getHeight(targetIcon, requirements, sources, showAll, width), visibleOuterHeight));
        int textX = x + PADDING;
        // target header (24) + "所需" label (18): matches render()'s cursorY steps.
        int cursorY = y + PADDING + 24 + 18;

        for (MinimalSubMaterialListView.RequirementContribution requirement : requirements) {
            String name = MinimalSubMaterialListView.requirementDisplayName(requirement);
            if (isVisibleTextHovered(textX + 26, cursorY + 2, StringUtils.getStringWidth(name), y, visibleHeight, mouseX, mouseY)) {
                return new RequirementNameHit(cyclingIcon(requirement.icons(), requirement.icon()), requirement, false);
            }

            MinimalSubMaterialListView.UpstreamRequirement upstream = requirement.upstream();
            if (upstream != null) {
                String line = countLine(name, requirement.totalCount(), requirement.missingCount(), requirement.maxStackSize());
                int arrowX;
                int rowY;
                if (requirementLineCount(requirement, contentWidth) == 1) {
                    arrowX = textX + 26 + StringUtils.getStringWidth(line) + UPSTREAM_GAP;
                    rowY = cursorY;
                } else {
                    arrowX = textX + WRAP_INDENT;
                    rowY = cursorY + ROW_HEIGHT;
                }
                int upstreamNameX = arrowX + UPSTREAM_ARROW_WIDTH + 8 + 26;
                String upstreamName = MinimalSubMaterialListView.upstreamDisplayName(upstream);
                if (isVisibleTextHovered(upstreamNameX, rowY + 2, StringUtils.getStringWidth(upstreamName), y, visibleHeight, mouseX, mouseY)) {
                    return new RequirementNameHit(cyclingIcon(upstream.icons(), upstream.icon()), requirement, true);
                }
            }

            cursorY += requirementLineCount(requirement, contentWidth) * ROW_HEIGHT;
        }

        return null;
    }

    private static void renderNameRow(WidgetBase widget, GuiContext context, int textX, int y, ItemStack icon, String name) {
        RenderUtils.drawRect(context, textX, y + SOURCE_ICON_BOX_Y_OFFSET, SOURCE_ICON_BOX_SIZE,
                SOURCE_ICON_BOX_SIZE, 0x30FFFFFF);
        context.renderItem(icon, textX + 1, y + SOURCE_ICON_BOX_Y_OFFSET + 1);

        widget.drawString(context, textX + 26, y + 2, 0xFFFFFFFF, name);
    }

    private static String renderCountRow(WidgetBase widget, GuiContext context, int textX, int y, ItemStack icon, String name, int totalCount, int missingCount, int maxStackSize) {
        return renderCountRow(widget, context, textX, y, icon, name, totalCount, missingCount, maxStackSize, false);
    }

    private static String renderCountRow(WidgetBase widget, GuiContext context, int textX, int y, ItemStack icon, String name, int totalCount, int missingCount, int maxStackSize, boolean underlined) {
        RenderUtils.drawRect(context, textX, y + SOURCE_ICON_BOX_Y_OFFSET, SOURCE_ICON_BOX_SIZE,
                SOURCE_ICON_BOX_SIZE, 0x30FFFFFF);
        context.renderItem(icon, textX + 1, y + SOURCE_ICON_BOX_Y_OFFSET + 1);

        String total = CountFormatter.format(totalCount, maxStackSize);
        String missing = CountFormatter.format(missingCount, maxStackSize);
        String missingColor = missingCount == 0 ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        String line = countLine(name, totalCount, missingCount, maxStackSize);
        String renderedLine = underlined
                ? UNDERLINE + name + ": "
                        + GuiBase.TXT_GOLD + UNDERLINE + total
                        + GuiBase.TXT_RST + UNDERLINE + " / "
                        + missingColor + UNDERLINE + missing
                        + GuiBase.TXT_RST
                : name + ": "
                        + GuiBase.TXT_GOLD + total
                        + GuiBase.TXT_RST + " / "
                        + missingColor + missing
                        + GuiBase.TXT_RST;
        widget.drawString(context, textX + 26, y + 2, 0xFFFFFFFF, renderedLine);
        return line;
    }

    private static String countLine(String name, int totalCount, int missingCount, int maxStackSize) {
        return name + ": " + CountFormatter.format(totalCount, maxStackSize) + " / " + CountFormatter.format(missingCount, maxStackSize);
    }

    private static boolean isCountRowHovered(int textX, int y, String line, int panelY, int visibleHeight, int mouseX, int mouseY) {
        int lineWidth = SOURCE_ICON_BOX_SIZE;
        return isVisibleBoxHovered(textX, y + SOURCE_ICON_BOX_Y_OFFSET, lineWidth, SOURCE_ICON_BOX_SIZE, panelY, visibleHeight, mouseX, mouseY);
    }

    private static boolean isVisibleBoxHovered(int x, int y, int width, int height, int panelY, int visibleHeight, int mouseX, int mouseY) {
        return width > 0
                && height > 0
                && mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height
                && mouseY >= panelY
                && mouseY < panelY + visibleHeight;
    }

    private static ItemStack cyclingIcon(List<ItemStack> icons, ItemStack fallback) {
        if (icons.isEmpty()) {
            return fallback;
        }

        ItemStack icon = FamilyIconCycle.pick(icons, System.currentTimeMillis(), FAMILY_CYCLE_MILLIS, ICON_CYCLE_MILLIS);
        return icon.isEmpty() ? fallback : icon;
    }

    private static void drawColumnSeparators(GuiContext context, int contentX, int boxY, SourceColumnLayout layout) {
        int separatorY = boxY + SOURCE_ICON_BOX_Y_OFFSET;
        int separatorHeight = Math.max(0, (layout.rowCount() - 1) * ROW_HEIGHT + SOURCE_ICON_BOX_SIZE);
        for (int column = 0; column < layout.columns() - 1; column++) {
            int x = contentX + layout.columnX(column) + layout.columnWidth(column) + COLUMN_GAP / 2;
            RenderUtils.drawRect(context, x, separatorY, 1, separatorHeight, COLUMN_SEPARATOR_COLOR);
        }
    }

    // The rendered panel exactly fills the width it is given (the entry's usable
    // width, already capped to the viewport by the browser-width logic). It does
    // NOT grow to fit content — growing past the entry made the panel overlap
    // the vertical scrollbar. Content adapts instead: source rows drop columns,
    // the upstream requirement wraps, and anything still too wide clips at the
    // panel's own scissor.
    private static int panelWidthFor(int width) {
        return Math.max(160, width);
    }

    // Content-based width the panel would like, used only to negotiate the
    // browser width (which is separately capped at the viewport). Covers the
    // source rows and each requirement's main / wrapped-upstream line; the full
    // one-line requirement width is excluded so a too-wide upstream wraps.
    private static int requiredContentWidth(List<MinimalSubMaterialListView.RequirementContribution> requirements, List<MinimalSubMaterialListView.SourceContribution> sources) {
        int content = Math.max(widestSourceRowWidth(sources), widestRequirementContentWidth(requirements));
        return Math.max(160, content + PADDING * 2);
    }

    private static SourceColumnLayout sourceColumnLayout(int contentWidth, List<MinimalSubMaterialListView.SourceContribution> sources) {
        int visibleCount = visibleSourceCount(sources);
        int preferredColumns = sourceColumnCount(visibleCount);
        for (int columns = preferredColumns; columns > 1; columns--) {
            SourceColumnLayout layout = sourceColumnLayout(sources, visibleCount, columns);
            if (layout.totalWidth() <= contentWidth) {
                return layout;
            }
        }

        return sourceColumnLayout(sources, visibleCount, 1);
    }

    private static SourceColumnLayout sourceColumnLayout(List<MinimalSubMaterialListView.SourceContribution> sources, int visibleCount, int columns) {
        int rowCount = sourceRowCount(visibleCount, columns);
        int[] columnX = new int[columns];
        int[] columnWidths = new int[columns];

        for (int index = 0; index < visibleCount; index++) {
            int column = index / rowCount;
            columnWidths[column] = Math.max(columnWidths[column], sourceRowWidth(sources.get(index)));
        }

        int x = 0;
        for (int column = 0; column < columns; column++) {
            columnX[column] = x;
            x += columnWidths[column];
            if (column < columns - 1) {
                x += COLUMN_GAP;
            }
        }

        return new SourceColumnLayout(columnX, columnWidths, rowCount, columns, x);
    }

    private static int widestSourceRowWidth(List<MinimalSubMaterialListView.SourceContribution> sources) {
        int width = 1;
        for (MinimalSubMaterialListView.SourceContribution source : sources) {
            width = Math.max(width, sourceRowWidth(source));
        }
        return width;
    }

    private static int sourceRowWidth(MinimalSubMaterialListView.SourceContribution source) {
        return 26 + StringUtils.getStringWidth(countLine(source.name(), source.totalCount(), source.missingCount(), source.maxStackSize()));
    }

    private static void drawOutline(GuiContext context, int x, int y, int width, int height, int color) {
        if (height <= 0) {
            return;
        }

        RenderUtils.drawRect(context, x, y, width, 1, color);
        RenderUtils.drawRect(context, x, y + height - 1, width, 1, color);
        RenderUtils.drawRect(context, x, y, 1, height, color);
        RenderUtils.drawRect(context, x + width - 1, y, 1, height, color);
    }

    public enum SortButtonTarget {
        NONE, MODE, DIRECTION
    }

    private record SortButtonBounds(int modeX, int y, int modeWidth, int height, int directionX, int directionSize) {
    }

    private record SourceColumnLayout(int[] columnX, int[] columnWidths, int rowCount, int columns, int totalWidth) {
        private int columnX(int column) {
            return this.columnX[column];
        }

        private int columnWidth(int column) {
            return this.columnWidths[column];
        }
    }
}
