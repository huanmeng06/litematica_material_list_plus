package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntrySortable;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.ReadMode;
import io.github.huanmeng06.lmlp.cache.MaterialListDataSource;
import io.github.huanmeng06.lmlp.config.Configs;
import net.minecraft.class_1297;
import net.minecraft.class_2338;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.class_638;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KnownPlacementRows {
    public static final int ROW_HEIGHT = 24;
    public static final int ICON_SIZE = 16;
    public static final int PLACEMENT_INDENT = 50;
    public static final int PLACEMENT_ICON_X = PLACEMENT_INDENT - 16;
    public static final String UNDERLINE = "\u00A7n";
    public static final String PAGE_LOADED_SCHEMATICS = "loaded_schematics";
    public static final String PAGE_SCHEMATIC_PLACEMENTS = "schematic_placements";

    private static final int BUTTON_HEIGHT = 20;
    private static final int ARROW_SLOT_X = 6;
    private static final int ARROW_SLOT_WIDTH = 14;
    private static final int ICON_X = 28;
    private static final int HEADER_TEXT_X = 50;
    private static final int TEXT_HEIGHT = 8;
    private static final int ROW_CONTENT_INSET = 1;
    private static final int ROW_RIGHT_PADDING = 2;
    private static final int COLUMN_GAP = 8;
    private static final int MIN_NAME_WIDTH = 48;
    private static final int MIN_FILE_WIDTH = 160;
    private static final int MIN_PLACEMENT_WIDTH = 160;
    private static final int STATUS_COLUMN_PADDING = 24;
    private static final int ORIGIN_COLUMN_PADDING = 24;
    private static final int BUTTON_GAP = 2;
    private static final int HEADER_RENDER_LEFT_OVERHANG = 3;
    private static final int HEADER_RENDER_RIGHT_TRIM = 6;
    private static final int FILE_TEXT_COLOR = 0xFFB8B8B8;
    private static final int ORIGIN_TEXT_COLOR = 0xFFB8B8B8;
    private static final int ORIGINAL_HEADER_BACKGROUND = 0xA0101010;

    private static final class_2960 OVERWORLD_ICON = class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/overworld.png");
    private static final class_2960 NETHER_ICON = class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/nether.png");
    private static final class_2960 END_ICON = class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/end.png");
    private static final class_2960 DIM_ICON = class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/dim.png");
    private static final Map<String, Boolean> COLLAPSED_GROUPS = new LinkedHashMap<>();
    private static final Map<String, SortState> SORT_STATES = new LinkedHashMap<>();
    private static final ExpandAnimationTracker GROUP_ANIMATIONS = new ExpandAnimationTracker();

    private KnownPlacementRows() {
    }

    public static List<KnownPlacementRow> rows(String pageId) {
        return rows(pageId, ChunkMissingMaterialListCache.knownPlacementContexts());
    }

    public static List<KnownPlacementRow> rows(String pageId, Collection<KnownPlacementContext> contexts) {
        String currentDimension = currentDimensionId();
        List<KnownPlacementContext> sortedContexts = contexts.stream()
                .sorted(Comparator
                        .comparing((KnownPlacementContext context) -> dimensionDisplaySortKey(context.dimension(), currentDimension))
                        .thenComparing(context -> normalizedDimension(context.dimension()))
                        .thenComparing(KnownPlacementContext::key))
                .toList();

        Map<String, List<KnownPlacementContext>> groups = new LinkedHashMap<>();
        for (KnownPlacementContext context : sortedContexts) {
            groups.computeIfAbsent(normalizedDimension(context.dimension()), key -> new ArrayList<>()).add(context);
        }

        List<KnownPlacementRow> rows = new ArrayList<>();
        rows.add(KnownPlacementRow.tableHeader(pageId, sortedContexts));
        for (Map.Entry<String, List<KnownPlacementContext>> entry : groups.entrySet()) {
            String dimension = entry.getKey();
            List<KnownPlacementContext> groupContexts = entry.getValue().stream()
                    .sorted(sortComparator(pageId))
                    .toList();
            String animationKey = collapseKey(pageId, dimension);
            boolean expanded = !isCollapsed(pageId, dimension);
            float progress = GROUP_ANIMATIONS.progress(animationKey, expanded);
            rows.add(KnownPlacementRow.header(pageId, dimension, displayName(dimension), expanded, groupContexts));
            if (expanded || progress > 0.001F) {
                for (KnownPlacementContext context : groupContexts) {
                    rows.add(KnownPlacementRow.placement(pageId, dimension, displayName(dimension), context));
                }
            }
        }

        return rows;
    }

    public static void cycleSort(String pageId, SortColumn column) {
        SortState current = sortState(pageId);
        boolean descending = current.column() == column && !current.descending();
        SORT_STATES.put(pageId, new SortState(column, descending));
    }

    public static ReadStatus readStatus(KnownPlacementContext context) {
        if (context == null) {
            return null;
        }

        if (context.placement() != null && !context.placement().isEnabled()) {
            return ReadStatus.DISABLED;
        }

        return readStatus(context.key());
    }

    public static ReadStatus readStatus(String contextKey) {
        return ReadStatus.from(ChunkMissingMaterialListCache.resolveReadMode(contextKey));
    }

    public static ReadStatus readStatus(MaterialListBase materialList) {
        return ReadStatus.from(ChunkMissingMaterialListCache.resolveReadMode(materialList));
    }

    public static ReadStatus readStatus(MaterialListDataSource dataSource) {
        return ReadStatus.from(ChunkMissingMaterialListCache.resolveReadMode(dataSource));
    }

    public static void toggle(String pageId, String dimension) {
        String key = collapseKey(pageId, dimension);
        boolean expanded = !COLLAPSED_GROUPS.getOrDefault(key, false);
        float startProgress = GROUP_ANIMATIONS.progress(key, expanded);
        boolean nextExpanded = !expanded;
        COLLAPSED_GROUPS.put(key, !nextExpanded);
        GROUP_ANIMATIONS.start(key, startProgress, nextExpanded ? 1.0F : 0.0F);
    }

    public static boolean hasActiveGroupAnimations() {
        return GROUP_ANIMATIONS.isActive();
    }

    public static void pruneGroupAnimations() {
        GROUP_ANIMATIONS.prune();
    }

    public static List<String> filterStrings(KnownPlacementRow row) {
        List<String> strings = new ArrayList<>();
        strings.add(row.displayName().toLowerCase(Locale.ROOT));
        strings.add(row.dimension().toLowerCase(Locale.ROOT));
        if (row.isTableHeader()) {
            strings.add(StringUtils.translate("lmlp.gui.known_placement.header.project").toLowerCase(Locale.ROOT));
            strings.add(StringUtils.translate("lmlp.gui.known_placement.header.status").toLowerCase(Locale.ROOT));
            strings.add(StringUtils.translate("lmlp.gui.known_placement.header.schematic_name").toLowerCase(Locale.ROOT));
            strings.add(StringUtils.translate("lmlp.gui.known_placement.header.origin").toLowerCase(Locale.ROOT));
            strings.add(StringUtils.translate("lmlp.gui.known_placement.header.actions").toLowerCase(Locale.ROOT));
            for (KnownPlacementContext context : row.groupContexts()) {
                addContextFilterStrings(strings, context);
            }
            return strings;
        }

        if (row.isHeader()) {
            for (KnownPlacementContext context : row.groupContexts()) {
                addContextFilterStrings(strings, context);
            }
        } else if (row.context() != null) {
            addContextFilterStrings(strings, row.context());
        }
        return strings;
    }

    public static List<String> filterStrings(KnownPlacementContext context) {
        List<String> strings = new ArrayList<>();
        strings.add(displayName(context.dimension()).toLowerCase(Locale.ROOT));
        strings.add(normalizedDimension(context.dimension()).toLowerCase(Locale.ROOT));
        addContextFilterStrings(strings, context);
        return strings;
    }

    public static void renderHeader(WidgetBase widget, KnownPlacementRow row, int mouseX, int mouseY, GuiContext drawContext) {
        boolean hovered = widget.isMouseOver(mouseX, mouseY);
        int background = hovered ? 0xA0707070 : 0xA0303030;
        RenderUtils.drawRect(drawContext, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(),
                background);

        int centerY = widget.getY() + widget.getHeight() / 2;
        ToggleArrowRenderer.render(drawContext, widget.getX() + ARROW_SLOT_X, ARROW_SLOT_WIDTH, centerY, arrowProgress(row), hovered);
        drawIcon(row.dimension(), widget.getX() + ICON_X, widget.getY() + (widget.getHeight() - ICON_SIZE) / 2, drawContext);
        widget.drawString(drawContext, widget.getX() + HEADER_TEXT_X, textY(widget), 0xFFE0E0E0, GuiBase.TXT_BOLD + row.displayName() + GuiBase.TXT_RST);
        GROUP_ANIMATIONS.prune();
    }

    public static int rowHeight(KnownPlacementRow row) {
        if (row == null || !row.isPlacement()) {
            return ROW_HEIGHT;
        }

        float progress = groupProgress(row);
        if (progress <= 0.001F) {
            return 0;
        }

        return Math.max(1, Math.round(ROW_HEIGHT * progress));
    }

    public static void renderTableHeader(WidgetBase widget, KnownPlacementRow row, int mouseX, int mouseY, GuiContext drawContext) {
        RenderUtils.drawRect(drawContext, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(),
                ORIGINAL_HEADER_BACKGROUND);
        SortableHeaderRenderer renderer = SortableHeaderRenderer.create(widget, row);
        ColumnLayout columns = computeColumns(widget, row.pageId());
        int textY = textY(widget);
        drawHeaderLabel(widget, SortColumn.PROJECT, headerTextX(columns.placementX()), textY, columns.placementWidth(), drawContext);
        drawHeaderLabel(widget, SortColumn.STATUS, headerTextX(columns.statusX()), textY, columns.statusWidth(), drawContext);
        drawHeaderLabel(widget, SortColumn.FILE, headerTextX(columns.fileX()), textY, columns.fileWidth(), drawContext);
        drawHeaderLabel(widget, SortColumn.ORIGIN, headerTextX(columns.originX()), textY, columns.originWidth(), drawContext);
        if (hasActionColumn(row.pageId())) {
            widget.drawString(
                    drawContext,
                    headerTextX(columns.actionX()),
                    textY,
                    -1,
                    GuiBase.TXT_BOLD + StringUtils.translate("lmlp.gui.known_placement.header.actions") + GuiBase.TXT_RST);
        }
        renderer.renderOriginalColumnHeader(mouseX, mouseY, drawContext);
    }

    public static boolean clickTableHeader(WidgetBase widget, KnownPlacementRow row, int mouseX, int mouseY) {
        if (!row.isTableHeader() || !widget.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int hoveredColumn = SortableHeaderRenderer.create(widget, row).mouseOverColumn(mouseX, mouseY);
        if (hoveredColumn < 0 || hoveredColumn > SortColumn.ORIGIN.ordinal()) {
            return false;
        }

        cycleSort(row.pageId(), SortColumn.values()[hoveredColumn]);
        return true;
    }

    public static void renderSelectedOutline(WidgetBase widget, GuiContext drawContext) {
        RenderUtils.drawOutline(drawContext, contentLeft(widget), widget.getY() + 1, widget.getWidth() - 2, Math.max(0, Math.min(ROW_HEIGHT, widget.getHeight()) - 2), 0xFFFFFFFF);
    }

    public static void renderPlacementIcon(WidgetBase widget, float zLevel, GuiContext drawContext) {
        Icons.SCHEMATIC_TYPE_FILE.renderAt(
                drawContext,
                widget.getX() + PLACEMENT_ICON_X,
                widget.getY() + (ROW_HEIGHT - Icons.SCHEMATIC_TYPE_FILE.getHeight()) / 2,
                zLevel,
                false,
                false);
    }

    public static PlacementLine placementLine(WidgetBase widget, KnownPlacementContext context, String placementName, String pageId) {
        PlacementStatus status = placementStatus(context);
        int statusTextWidth = status == null ? 0 : widget.getStringWidth(status.text());
        ColumnLayout columns = computeColumns(widget, pageId);
        PlacementLineLayout layout = columns.placementLineLayout();
        String fullName = placementName == null ? "" : placementName;
        String nameText = truncateToWidth(widget, fullName, layout.nameWidth());
        boolean nameTruncated = !nameText.equals(fullName);
        String fileName = schematicDisplayName(context);
        String fileText = truncateToWidth(widget, fileName, layout.fileWidth());
        boolean fileTruncated = !fileText.equals(fileName);
        String origin = originPosition(context);
        String originText = truncateToWidth(widget, origin, layout.originWidth());
        boolean originTruncated = !originText.equals(origin);

        return new PlacementLine(
                layout,
                nameText,
                widget.getStringWidth(nameText),
                nameTruncated,
                fullName,
                fileText,
                widget.getStringWidth(fileText),
                fileTruncated,
                schematicHoverText(widget, context),
                originText,
                widget.getStringWidth(originText),
                originTruncated,
                status,
                statusTextWidth);
    }

    public static void renderPlacementLine(WidgetBase widget, float zLevel, GuiContext drawContext, PlacementLine line, String nameColor, boolean nameHovered) {
        renderPlacementLine(widget, zLevel, drawContext, line, nameColor, nameHovered, null, false);
    }

    public static void renderPlacementLine(WidgetBase widget, float zLevel, GuiContext drawContext, PlacementLine line, String nameColor, boolean nameHovered, KnownPlacementContext context, boolean originHovered) {
        renderPlacementIcon(widget, zLevel, drawContext);

        int textY = textY(widget);
        String formattedName = nameColor + (nameHovered ? UNDERLINE : "") + line.nameText() + GuiBase.TXT_RST;
        widget.drawString(drawContext, line.layout().nameX(), textY, 0xFFFFFFFF, formattedName);

        if (!line.fileText().isEmpty()) {
            widget.drawString(drawContext, line.layout().fileX(), textY, FILE_TEXT_COLOR, line.fileText());
        }

        if (line.status() != null && line.layout().statusWidth() > 0) {
            widget.drawString(drawContext, line.layout().statusX(), textY, line.status().color(), line.status().text());
        }

        if (!line.originText().isEmpty()) {
            boolean highlight = PlacementOriginMarker.hasHighlight(context);
            String formattedOrigin = (highlight ? GuiBase.TXT_BOLD : "")
                    + (originHovered ? UNDERLINE : "")
                    + line.originText()
                    + GuiBase.TXT_RST;
            int color = PlacementOriginMarker.canHighlightOrigin(context) ? PlacementOriginMarker.ORIGIN_TEXT_COLOR : ORIGIN_TEXT_COLOR;
            widget.drawString(drawContext, line.layout().originX(), textY, color, formattedOrigin);
        }
    }

    public static int contentRight(WidgetBase widget) {
        return widget.getX() + widget.getWidth() - ROW_RIGHT_PADDING;
    }

    public static int contentLeft(WidgetBase widget) {
        return widget.getX() + ROW_CONTENT_INSET;
    }

    public static ColumnLayout computeColumns(WidgetBase widget, String pageId) {
        int rowLeft = widget.getX();
        int placementX = contentLeft(widget);
        int nameX = rowLeft + PLACEMENT_INDENT;
        int right = contentRight(widget);
        boolean hasActionColumn = hasActionColumn(pageId);
        int operationWidth = actionColumnWidth(widget);
        int actionX = hasActionColumn ? Math.max(nameX + MIN_NAME_WIDTH, right - operationWidth) : right;
        int statusWidth = statusColumnWidth(widget);
        int originMin = originCompactWidth(widget);

        // Space the placement/status/file/origin columns share (before the
        // right-anchored action column, if any). Rows cannot scroll
        // horizontally, so when this is too small we drop lower-priority
        // columns (file, then origin, then status). Visibility is decided
        // against each column's MINIMUM width (origin uses a compact minimum
        // so the coordinate shows before the name column hogs all the space)
        // rather than its preferred width.
        int middleEnd = hasActionColumn ? actionX - COLUMN_GAP : right;
        int middle = Math.max(0, middleEnd - placementX);

        boolean statusVisible = false;
        boolean fileVisible = false;
        boolean originVisible = false;
        int need = MIN_PLACEMENT_WIDTH;
        if (need + COLUMN_GAP + statusWidth <= middle) {
            statusVisible = true;
            need += COLUMN_GAP + statusWidth;
            if (need + COLUMN_GAP + originMin <= middle) {
                originVisible = true;
                need += COLUMN_GAP + originMin;
                if (need + COLUMN_GAP + MIN_FILE_WIDTH <= middle) {
                    fileVisible = true;
                }
            }
        }

        int visibleCount = 1 + (statusVisible ? 1 : 0) + (fileVisible ? 1 : 0) + (originVisible ? 1 : 0);
        int gaps = (visibleCount - 1) * COLUMN_GAP;
        int afterStatus = middle - gaps - (statusVisible ? statusWidth : 0);

        // Origin always renders at its compact width instead of growing to
        // fill leftover space; any extra room goes to the name/file columns.
        int originWidth = originVisible ? originMin : 0;

        int flexSpace = Math.max(0, afterStatus - originWidth);
        int placementWidth;
        int fileWidth;
        if (fileVisible) {
            int surplus = Math.max(0, flexSpace - MIN_PLACEMENT_WIDTH - MIN_FILE_WIDTH);
            placementWidth = MIN_PLACEMENT_WIDTH + surplus / 2;
            fileWidth = flexSpace - placementWidth;
        } else {
            placementWidth = flexSpace;
            fileWidth = 0;
        }

        int cursor = placementX + placementWidth;
        int statusXPos = 0;
        int fileXPos = 0;
        int originXPos = 0;
        if (statusVisible) {
            cursor += COLUMN_GAP;
            statusXPos = cursor;
            cursor += statusWidth;
        }
        if (fileVisible) {
            cursor += COLUMN_GAP;
            fileXPos = cursor;
            cursor += fileWidth;
        }
        if (originVisible) {
            cursor += COLUMN_GAP;
            originXPos = cursor;
            cursor += originWidth;
        }

        // Hidden columns collapse onto the next visible column's start so the
        // header sort-position array stays monotonic and their click/hover
        // regions shrink to zero width.
        int anchorAfterOrigin = hasActionColumn ? actionX : right;
        int originXFinal = originVisible ? originXPos : anchorAfterOrigin;
        int fileXFinal = fileVisible ? fileXPos : originXFinal;
        int statusXFinal = statusVisible ? statusXPos : fileXFinal;

        return new ColumnLayout(
                placementX,
                placementWidth,
                nameX,
                Math.max(0, statusXFinal - COLUMN_GAP - nameX),
                statusXFinal,
                statusVisible ? statusWidth : 0,
                fileXFinal,
                fileVisible ? fileWidth : 0,
                originXFinal,
                originVisible ? originWidth : 0,
                actionX,
                Math.max(0, right - actionX),
                right);
    }

    public static int actionColumnX(WidgetBase widget) {
        return computeColumns(widget, PAGE_SCHEMATIC_PLACEMENTS).actionX();
    }

    public static int buttonGap() {
        return BUTTON_GAP;
    }

    public static void addTranslatedTooltipLines(List<String> lines, String key) {
        StringUtils.translate(key).lines().forEach(lines::add);
    }

    public static int textY(WidgetBase widget) {
        return widget.getY() + (Math.max(widget.getHeight(), ROW_HEIGHT) - TEXT_HEIGHT) / 2;
    }

    public static int buttonY(int rowY) {
        return rowY + Math.max(2, (ROW_HEIGHT - BUTTON_HEIGHT) / 2);
    }

    private static float arrowProgress(KnownPlacementRow row) {
        return GROUP_ANIMATIONS.progress(collapseKey(row.pageId(), row.dimension()), row.expanded());
    }

    private static float groupProgress(KnownPlacementRow row) {
        return GROUP_ANIMATIONS.progress(collapseKey(row.pageId(), row.dimension()), row.expanded());
    }

    public static String displayName(String dimension) {
        String normalized = normalizedDimension(dimension);
        if ("unknown".equals(normalized)) {
            return StringUtils.translate("lmlp.dimension.unknown");
        }

        String key = "lmlp.dimension." + normalized.replace(':', '.');
        String translated = StringUtils.translate(key);
        if (!translated.equals(key)) {
            return translated;
        }

        String vanillaKey = "dimension." + normalized.replace(':', '.');
        translated = StringUtils.translate(vanillaKey);
        return translated.equals(vanillaKey) ? normalized : translated;
    }

    public static String normalizedDimension(String dimension) {
        if (dimension == null) {
            return "unknown";
        }

        String normalized = dimension.trim();
        if (normalized.isEmpty()) {
            return "unknown";
        }

        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1).trim();
        }

        while (normalized.endsWith("]")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }

        if (normalized.startsWith("ResourceKey[")) {
            normalized = normalized.substring("ResourceKey[".length()).trim();
        }

        if (normalized.startsWith("minecraft:dimension ")) {
            normalized = normalized.substring("minecraft:dimension ".length()).trim();
        }

        int space = normalized.lastIndexOf(' ');
        if (space >= 0 && normalized.substring(space + 1).contains(":")) {
            normalized = normalized.substring(space + 1).trim();
        }

        return normalized.isEmpty() ? "unknown" : normalized;
    }

    public static int dimensionSortKey(String dimension) {
        return switch (normalizedDimension(dimension)) {
            case "minecraft:overworld" -> 0;
            case "minecraft:the_nether" -> 1;
            case "minecraft:the_end" -> 2;
            default -> 3;
        };
    }

    private static int dimensionDisplaySortKey(String dimension, String currentDimension) {
        String normalized = normalizedDimension(dimension);
        String current = normalizedDimension(currentDimension);
        if (!"unknown".equals(current) && normalized.equals(current)) {
            return -1;
        }

        return dimensionSortKey(normalized);
    }

    private static boolean isCollapsed(String pageId, String dimension) {
        return COLLAPSED_GROUPS.getOrDefault(collapseKey(pageId, dimension), false);
    }

    private static String collapseKey(String pageId, String dimension) {
        return pageId + "|" + normalizedDimension(dimension);
    }

    private static void addContextFilterStrings(List<String> strings, KnownPlacementContext context) {
        strings.add(context.name().toLowerCase(Locale.ROOT));
        strings.add(context.key().toLowerCase(Locale.ROOT));
        strings.add(context.schematicName().toLowerCase(Locale.ROOT));
        strings.add(context.schematicPath().toLowerCase(Locale.ROOT));
        strings.add(originPosition(context).toLowerCase(Locale.ROOT));
        strings.add(context.sourceState().name().toLowerCase(Locale.ROOT));
        ReadStatus status = readStatus(context);
        strings.add(status.label().toLowerCase(Locale.ROOT));
        strings.add(status.name().toLowerCase(Locale.ROOT));
    }

    private static void drawIcon(String dimension, int x, int y, GuiContext drawContext) {
        drawContext.method_25290(net.minecraft.class_10799.field_56883, icon(dimension), x, y, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private static class_2960 icon(String dimension) {
        return switch (normalizedDimension(dimension)) {
            case "minecraft:overworld" -> OVERWORLD_ICON;
            case "minecraft:the_nether" -> NETHER_ICON;
            case "minecraft:the_end" -> END_ICON;
            default -> DIM_ICON;
        };
    }

    private static PlacementStatus placementStatus(KnownPlacementContext context) {
        if (context == null) {
            return null;
        }

        ReadStatus readStatus = readStatus(context);
        List<String> lines = new ArrayList<>();
        addTranslatedTooltipLines(lines, readStatus.tooltipKey());
        if (readStatus == ReadStatus.OFFLINE) {
            if (context.schematicMissing()) {
                lines.add(StringUtils.translate("lmlp.gui.known_placement.schematic_missing"));
            }
            if (!context.hasMaterialCache()) {
                lines.add(StringUtils.translate("lmlp.gui.known_placement.offline_cache_empty"));
            }
        }

        return new PlacementStatus(readStatus.label(), readStatus.color(), lines);
    }

    private static int statusColumnWidth(WidgetBase widget) {
        int width = widget.getStringWidth(StringUtils.translate("lmlp.gui.known_placement.header.status"));
        for (ReadStatus status : ReadStatus.values()) {
            width = Math.max(width, widget.getStringWidth(status.label()));
        }
        return width + STATUS_COLUMN_PADDING;
    }

    // The origin column always renders at this compact width (enough for the
    // header and a typical coordinate) rather than growing to fill leftover
    // space; unusually long coordinates fall back to the "..." truncation
    // already used elsewhere.
    private static int originCompactWidth(WidgetBase widget) {
        int headerWidth = widget.getStringWidth(StringUtils.translate("lmlp.gui.known_placement.header.origin"));
        int coordinateWidth = widget.getStringWidth("[-1234, 64, -1234]");
        return Math.max(headerWidth, coordinateWidth) + ORIGIN_COLUMN_PADDING;
    }

    private static int actionColumnWidth(WidgetBase widget) {
        return configureButtonWidth(widget)
                + toggleButtonWidth(widget)
                + removeButtonWidth(widget)
                + (BUTTON_GAP * 2);
    }

    public static int configureButtonWidth(WidgetBase widget) {
        String label = StringUtils.translate("litematica.gui.button.schematic_placements.configure");
        return new ButtonGeneric(0, 0, -1, true, label).getWidth();
    }

    public static int toggleButtonWidth(WidgetBase widget) {
        int enabledWidth = new ButtonOnOff(
                0,
                0,
                -1,
                true,
                "litematica.gui.button.schematic_placements.placement_enabled",
                true).getWidth();
        int disabledWidth = new ButtonOnOff(
                0,
                0,
                -1,
                true,
                "litematica.gui.button.schematic_placements.placement_enabled",
                false).getWidth();
        return Math.max(enabledWidth, disabledWidth);
    }

    public static int removeButtonWidth(WidgetBase widget) {
        String label = StringUtils.translate("litematica.gui.button.schematic_placements.remove");
        return new ButtonGeneric(0, 0, -1, true, label).getWidth();
    }

    public static boolean shouldShowOfflineMissingButton(KnownPlacementContext context) {
        String currentDimension = currentDimensionId();
        if (context == null
                || !context.offlineCache()
                || currentDimension == null
                || !normalizedDimension(context.dimension()).equals(normalizedDimension(currentDimension))) {
            return false;
        }

        return isNearOrigin(context);
    }

    private static boolean isNearOrigin(KnownPlacementContext context) {
        class_2338 origin = PlacementOriginMarker.parseOrigin(context.originPosition());
        class_1297 player = class_310.method_1551().field_1724;
        if (origin == null || player == null) {
            return false;
        }

        // Range is configurable (see Configs.Generic.MISSING_PLACEMENT_BUTTON_RANGE):
        // large enough that the player doesn't need to stand exactly on the
        // recorded coordinate, small enough to filter out "same dimension, but
        // nowhere near the site" false triggers.
        double range = Configs.Generic.MISSING_PLACEMENT_BUTTON_RANGE.getIntegerValue();
        return player.method_5707(origin.method_46558()) <= range * range;
    }

    private static int[] headerColumnPositions(WidgetBase widget, KnownPlacementRow row) {
        ColumnLayout layout = computeColumns(widget, row.pageId());
        if (!hasActionColumn(row.pageId())) {
            return new int[] {
                    headerRendererX(layout.placementX()),
                    headerRendererX(layout.statusX()),
                    headerRendererX(layout.fileX()),
                    headerRendererX(layout.originX()),
                    headerRendererRight(layout.contentRight()) };
        }

        return new int[] {
                headerRendererX(layout.placementX()),
                headerRendererX(layout.statusX()),
                headerRendererX(layout.fileX()),
                headerRendererX(layout.originX()),
                headerRendererX(layout.actionX()),
                headerRendererRight(layout.contentRight()) };
    }

    private static int headerRendererX(int visualColumnX) {
        return visualColumnX + HEADER_RENDER_LEFT_OVERHANG;
    }

    private static int headerRendererRight(int visualRight) {
        return visualRight + HEADER_RENDER_RIGHT_TRIM;
    }

    private static int headerTextX(int visualColumnX) {
        return headerRendererX(visualColumnX);
    }

    private static void drawHeaderLabel(WidgetBase widget, SortColumn column, int x, int y, int maxWidth, GuiContext drawContext) {
        String label = switch (column) {
            case PROJECT -> StringUtils.translate("lmlp.gui.known_placement.header.project");
            case STATUS -> StringUtils.translate("lmlp.gui.known_placement.header.status");
            case FILE -> StringUtils.translate("lmlp.gui.known_placement.header.schematic_name");
            case ORIGIN -> StringUtils.translate("lmlp.gui.known_placement.header.origin");
        };
        String truncated = truncateToWidth(widget, label, maxWidth);
        widget.drawString(drawContext, x, y, -1, GuiBase.TXT_BOLD + truncated + GuiBase.TXT_RST);
    }

    private static Comparator<KnownPlacementContext> sortComparator(String pageId) {
        SortState state = sortState(pageId);
        Comparator<KnownPlacementContext> comparator = switch (state.column()) {
            case PROJECT -> Comparator
                    .comparing(KnownPlacementContext::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(KnownPlacementContext::key);
            case STATUS -> Comparator
                    .comparingInt((KnownPlacementContext context) -> readStatus(context).order())
                    .thenComparing(KnownPlacementContext::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(KnownPlacementContext::key);
            case FILE -> Comparator
                    .comparing(KnownPlacementRows::schematicDisplayName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(KnownPlacementContext::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(KnownPlacementContext::key);
            case ORIGIN -> Comparator
                    .comparingInt((KnownPlacementContext context) -> originCoordinate(context, 0))
                    .thenComparingInt(context -> originCoordinate(context, 1))
                    .thenComparingInt(context -> originCoordinate(context, 2))
                    .thenComparing(KnownPlacementContext::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(KnownPlacementContext::key);
        };
        return state.descending() ? comparator.reversed() : comparator;
    }

    private static SortState sortState(String pageId) {
        return SORT_STATES.getOrDefault(pageId, new SortState(SortColumn.PROJECT, false));
    }

    private static boolean isEditPage(String pageId) {
        return PAGE_SCHEMATIC_PLACEMENTS.equals(pageId);
    }

    public static boolean isLoadedSchematicsPage(String pageId) {
        return PAGE_LOADED_SCHEMATICS.equals(pageId);
    }

    private static boolean hasActionColumn(String pageId) {
        return PAGE_SCHEMATIC_PLACEMENTS.equals(pageId);
    }

    private static String currentDimensionId() {
        class_638 world = class_310.method_1551().field_1687;
        return world == null ? null : world.method_27983().method_29177().toString();
    }

    private static String schematicDisplayName(KnownPlacementContext context) {
        if (context == null) {
            return "";
        }

        if (!context.schematicPath().isEmpty()) {
            return new File(context.schematicPath()).getName();
        }

        if (!context.schematicName().isEmpty()) {
            return context.schematicName();
        }

        return StringUtils.translate("litematica.gui.label.schematic_placement.in_memory");
    }

    private static String schematicHoverText(WidgetBase widget, KnownPlacementContext context) {
        if (context == null) {
            return "";
        }

        if (!context.schematicPath().isEmpty()) {
            return shortenPath(widget, context.schematicPath());
        }

        return schematicDisplayName(context);
    }

    private static String shortenPath(WidgetBase widget, String absolutePath) {
        String normalized = absolutePath.replace('\\', '/');
        String[] parts = normalized.split("/");
        List<String> segments = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                segments.add(part);
            }
        }

        int schematicsIndex = -1;
        for (int i = segments.size() - 2; i >= 0; i--) {
            if (segments.get(i).equalsIgnoreCase("schematics")) {
                schematicsIndex = i;
                break;
            }
        }

        String firstLine;
        List<String> remainderSegments;

        if (schematicsIndex > 0) {
            firstLine = "~/" + segments.get(schematicsIndex - 1) + "/" + segments.get(schematicsIndex);
            remainderSegments = segments.subList(schematicsIndex + 1, segments.size());
        } else if (segments.size() > 2) {
            firstLine = "~/.../" + segments.get(segments.size() - 2);
            remainderSegments = segments.subList(segments.size() - 1, segments.size());
        } else {
            return normalized;
        }

        StringBuilder result = new StringBuilder(firstLine);
        int maxWidth = Math.max(widget.getStringWidth(firstLine), 120);
        String pad = alignmentPad(widget);
        for (String wrappedLine : wrapSegments(widget, remainderSegments, pad, maxWidth)) {
            result.append('\n').append(wrappedLine);
        }

        return result.toString();
    }

    /**
     * Spaces whose width best matches "~", so continuation lines' leading
     * slash lines up under the slash in the first line's "~/".
     */
    private static String alignmentPad(WidgetBase widget) {
        int target = widget.getStringWidth("~");
        StringBuilder pad = new StringBuilder();
        while (widget.getStringWidth(pad.toString()) < target) {
            pad.append(' ');
        }

        return pad.toString();
    }

    /**
     * Greedily packs "/segment" tokens into lines no wider than maxWidth,
     * never breaking inside a folder or file name. A single oversized token
     * gets its own line.
     */
    private static List<String> wrapSegments(WidgetBase widget, List<String> segments, String pad, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder(pad);
        boolean lineEmpty = true;
        for (String segment : segments) {
            String token = "/" + segment;
            if (!lineEmpty && widget.getStringWidth(current + token) > maxWidth) {
                lines.add(current.toString());
                current = new StringBuilder(pad);
                lineEmpty = true;
            }

            current.append(token);
            lineEmpty = false;
        }

        if (!lineEmpty) {
            lines.add(current.toString());
        }

        return lines;
    }

    private static String originPosition(KnownPlacementContext context) {
        if (context == null || context.originPosition() == null || context.originPosition().isEmpty()) {
            return StringUtils.translate("lmlp.gui.known_placement.origin_unknown");
        }

        return context.originPosition();
    }

    private static int originCoordinate(KnownPlacementContext context, int index) {
        String origin = context == null ? "" : context.originPosition();
        if (origin == null || origin.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        String cleaned = origin.replace("[", "").replace("]", "");
        String[] parts = cleaned.split(",");
        if (index < 0 || index >= parts.length) {
            return Integer.MAX_VALUE;
        }

        try {
            return Integer.parseInt(parts[index].trim());
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private static String truncateToWidth(WidgetBase widget, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }

        if (widget.getStringWidth(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int suffixWidth = widget.getStringWidth(suffix);
        if (suffixWidth > maxWidth) {
            return "";
        }

        int end = text.length();
        while (end > 0 && widget.getStringWidth(text.substring(0, end)) + suffixWidth > maxWidth) {
            end--;
        }

        return end > 0 ? text.substring(0, end) + suffix : suffix;
    }

    private static boolean isTextHovered(WidgetBase widget, int x, int width, int mouseX, int mouseY) {
        int textY = textY(widget);
        return width > 0
                && mouseX >= x
                && mouseX < x + width
                && mouseY >= textY
                && mouseY < textY + TEXT_HEIGHT;
    }

    public record PlacementLine(
            PlacementLineLayout layout,
            String nameText,
            int nameTextWidth,
            boolean nameTruncated,
            String nameHoverText,
            String fileText,
            int fileTextWidth,
            boolean fileTruncated,
            String fileHoverText,
            String originText,
            int originTextWidth,
            boolean originTruncated,
            PlacementStatus status,
            int statusTextWidth) {
        public boolean nameHovered(WidgetBase widget, int mouseX, int mouseY) {
            return isTextHovered(widget, this.layout.nameX(), this.nameTextWidth, mouseX, mouseY);
        }

        public boolean nameTooltipHovered(WidgetBase widget, int mouseX, int mouseY) {
            return this.nameTruncated && isTextHovered(widget, this.layout.nameX(), this.nameTextWidth, mouseX, mouseY);
        }

        public boolean fileHovered(WidgetBase widget, int mouseX, int mouseY) {
            return isTextHovered(widget, this.layout.fileX(), this.fileTextWidth, mouseX, mouseY);
        }

        public boolean statusHovered(WidgetBase widget, int mouseX, int mouseY) {
            return this.status != null && isTextHovered(widget, this.layout.statusX(), this.statusTextWidth, mouseX, mouseY);
        }

        public boolean originHovered(WidgetBase widget, int mouseX, int mouseY) {
            return isTextHovered(widget, this.layout.originX(), this.originTextWidth, mouseX, mouseY);
        }
    }

    public record PlacementLineLayout(
            int nameX,
            int nameWidth,
            int fileX,
            int fileWidth,
            int statusX,
            int statusWidth,
            int originX,
            int originWidth) {
    }

    public record ColumnLayout(
            int placementX,
            int placementWidth,
            int nameX,
            int nameWidth,
            int statusX,
            int statusWidth,
            int fileX,
            int fileWidth,
            int originX,
            int originWidth,
            int actionX,
            int actionWidth,
            int contentRight) {
        private PlacementLineLayout placementLineLayout() {
            return new PlacementLineLayout(
                    this.nameX,
                    this.nameWidth,
                    this.fileX,
                    this.fileWidth,
                    this.statusX,
                    this.statusWidth,
                    this.originX,
                    this.originWidth);
        }
    }

    public record PlacementStatus(String text, int color, List<String> tooltipLines) {
    }

    public record KnownPlacementRow(
            RowType type,
            String pageId,
            String dimension,
            String displayName,
            boolean expanded,
            KnownPlacementContext context,
            List<KnownPlacementContext> groupContexts) {
        public static KnownPlacementRow tableHeader(String pageId, List<KnownPlacementContext> groupContexts) {
            return new KnownPlacementRow(RowType.TABLE_HEADER, pageId, "", "", true, null, List.copyOf(groupContexts));
        }

        public static KnownPlacementRow header(
                String pageId,
                String dimension,
                String displayName,
                boolean expanded,
                List<KnownPlacementContext> groupContexts) {
            return new KnownPlacementRow(RowType.HEADER, pageId, dimension, displayName, expanded, null, groupContexts);
        }

        public static KnownPlacementRow placement(String pageId, String dimension, String displayName, KnownPlacementContext context) {
            return new KnownPlacementRow(RowType.PLACEMENT, pageId, dimension, displayName, true, context, List.of());
        }

        public boolean isHeader() {
            return this.type == RowType.HEADER;
        }

        public boolean isTableHeader() {
            return this.type == RowType.TABLE_HEADER;
        }

        public boolean isPlacement() {
            return this.type == RowType.PLACEMENT && this.context != null;
        }
    }

    public enum RowType {
        TABLE_HEADER,
        HEADER,
        PLACEMENT
    }

    public enum SortColumn {
        PROJECT,
        STATUS,
        FILE,
        ORIGIN
    }

    public enum ReadStatus {
        LIVE("lmlp.gui.known_placement.status.live", "lmlp.gui.known_placement.status.live_hint", 0xFF33FF33, 0),
        DISABLED("lmlp.gui.known_placement.status.disabled", "lmlp.gui.known_placement.status.disabled_hint", 0xFFFF5555, 1),
        CHUNK_CACHE("lmlp.gui.known_placement.status.chunk_cache", "lmlp.gui.known_placement.status.chunk_cache_hint", 0xFFFFCC66, 2),
        DIMENSION_CACHE("lmlp.gui.known_placement.status.dimension_cache", "lmlp.gui.known_placement.status.dimension_cache_hint", 0xFF66CCFF, 3),
        OFFLINE("lmlp.gui.known_placement.status.offline_cache", "lmlp.gui.known_placement.status.offline_cache_hint", 0xFFFF9900, 4);

        private final String translationKey;
        private final String tooltipKey;
        private final int color;
        private final int order;

        private static ReadStatus from(ReadMode readMode) {
            if (readMode == null) {
                return null;
            }

            return switch (readMode) {
                case LIVE -> LIVE;
                case CHUNK_CACHE -> CHUNK_CACHE;
                case DIMENSION_CACHE -> DIMENSION_CACHE;
                case OFFLINE_CACHE -> OFFLINE;
            };
        }

        ReadStatus(String translationKey, String tooltipKey, int color, int order) {
            this.translationKey = translationKey;
            this.tooltipKey = tooltipKey;
            this.color = color;
            this.order = order;
        }

        public String label() {
            return StringUtils.translate(this.translationKey);
        }

        public String tooltipKey() {
            return this.tooltipKey;
        }

        public int color() {
            return this.color;
        }

        public int order() {
            return this.order;
        }
    }

    private record SortState(SortColumn column, boolean descending) {
    }

    private static final class SortableHeaderRenderer extends WidgetListEntrySortable<KnownPlacementRow> {
        private final SortState sortState;
        private final int[] columnPositions;

        private SortableHeaderRenderer(WidgetBase source, KnownPlacementRow row, int[] columnPositions) {
            super(contentLeft(source), centeredHeaderY(source), sortableHeaderRight(source, row) - contentLeft(source), centeredHeaderHeight(source), row, -1);
            this.sortState = sortState(row.pageId());
            this.columnPositions = columnPositions;
            this.columnCount = Math.max(1, columnPositions.length - 1);
        }

        private static int sortableHeaderRight(WidgetBase source, KnownPlacementRow row) {
            ColumnLayout layout = computeColumns(source, row.pageId());
            return layout.contentRight();
        }

        private static int centeredHeaderHeight(WidgetBase source) {
            return Math.min(20, source.getHeight());
        }

        private static int centeredHeaderY(WidgetBase source) {
            int height = centeredHeaderHeight(source);
            return source.getY() + (source.getHeight() - height) / 2;
        }

        private static SortableHeaderRenderer create(WidgetBase source, KnownPlacementRow row) {
            return new SortableHeaderRenderer(source, row, headerColumnPositions(source, row));
        }

        private void renderOriginalColumnHeader(int mouseX, int mouseY, GuiContext drawContext) {
            this.renderColumnHeader(drawContext, mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP);
        }

        private int mouseOverColumn(int mouseX, int mouseY) {
            return this.getMouseOverColumn(mouseX, mouseY);
        }

        @Override
        protected int getColumnPosX(int column) {
            int index = Math.max(0, Math.min(column, this.columnPositions.length - 1));
            return this.columnPositions[index];
        }

        @Override
        protected int getCurrentSortColumn() {
            return this.sortState.column().ordinal();
        }

        @Override
        protected boolean getSortInReverse() {
            return this.sortState.descending();
        }
    }

}
