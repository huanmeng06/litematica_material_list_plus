package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntrySortable;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.cache.MaterialListDataSource;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
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
    private static final int ROW_RIGHT_PADDING = 8;
    private static final int COLUMN_GAP = 16;
    private static final int MIN_NAME_WIDTH = 48;
    private static final int MIN_FILE_WIDTH = 48;
    private static final int FILE_COLUMN_MIN_X = 190;
    private static final int STATUS_COLUMN_MIN_X = 150;
    private static final int FILE_TEXT_COLOR = 0xFFB8B8B8;
    private static final int ORIGINAL_HEADER_BACKGROUND = 0xA0101010;

    private static final class_2960 OVERWORLD_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/overworld.png");
    private static final class_2960 NETHER_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/nether.png");
    private static final class_2960 END_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/end.png");
    private static final class_2960 DIM_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/dim.png");
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
            boolean expanded = !isCollapsed(pageId, dimension);
            rows.add(KnownPlacementRow.header(pageId, dimension, displayName(dimension), expanded, groupContexts));
            if (expanded) {
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
        if (context == null || context.offlineCache() || context.placement() == null) {
            return ReadStatus.OFFLINE;
        }

        return ChunkMissingMaterialListCache.arePlacementChunksLoaded(context.placement())
                ? ReadStatus.LIVE
                : ReadStatus.CACHE;
    }

    public static ReadStatus readStatus(MaterialListDataSource dataSource) {
        if (dataSource == null) {
            return null;
        }

        return switch (dataSource) {
            case WORLD_SCAN -> ReadStatus.LIVE;
            case OFFLINE_CACHE -> ReadStatus.OFFLINE;
            case SCHEMATIC_CACHE, CROSS_DIMENSION_CACHE -> ReadStatus.CACHE;
            default -> null;
        };
    }

    public static void toggle(String pageId, String dimension) {
        String key = collapseKey(pageId, dimension);
        boolean expanded = !COLLAPSED_GROUPS.getOrDefault(key, false);
        float startProgress = GROUP_ANIMATIONS.progress(key, expanded);
        boolean nextExpanded = !expanded;
        COLLAPSED_GROUPS.put(key, !nextExpanded);
        GROUP_ANIMATIONS.start(key, startProgress, nextExpanded ? 1.0F : 0.0F);
    }

    public static List<String> filterStrings(KnownPlacementRow row) {
        List<String> strings = new ArrayList<>();
        strings.add(row.displayName().toLowerCase(Locale.ROOT));
        strings.add(row.dimension().toLowerCase(Locale.ROOT));
        if (row.isTableHeader()) {
            strings.add(StringUtils.translate("lmlp.gui.known_placement.header.project").toLowerCase(Locale.ROOT));
            strings.add(StringUtils.translate("lmlp.gui.known_placement.header.status").toLowerCase(Locale.ROOT));
            strings.add(StringUtils.translate("lmlp.gui.known_placement.header.schematic_name").toLowerCase(Locale.ROOT));
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

    public static void renderHeader(WidgetBase widget, KnownPlacementRow row, int mouseX, int mouseY, class_332 drawContext) {
        boolean hovered = widget.isMouseOver(mouseX, mouseY);
        int background = hovered ? 0xA0707070 : 0xA0303030;
        RenderUtils.drawRect(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), background);

        int centerY = widget.getY() + widget.getHeight() / 2;
        ToggleArrowRenderer.render(drawContext, widget.getX() + ARROW_SLOT_X, ARROW_SLOT_WIDTH, centerY, arrowProgress(row), hovered);
        drawIcon(row.dimension(), widget.getX() + ICON_X, widget.getY() + (widget.getHeight() - ICON_SIZE) / 2, drawContext);
        widget.drawString(widget.getX() + HEADER_TEXT_X, textY(widget), 0xFFE0E0E0, GuiBase.TXT_BOLD + row.displayName() + GuiBase.TXT_RST, drawContext);
        GROUP_ANIMATIONS.prune();
    }

    public static void renderTableHeader(WidgetBase widget, KnownPlacementRow row, int mouseX, int mouseY, class_332 drawContext) {
        RenderUtils.drawRect(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), ORIGINAL_HEADER_BACKGROUND);
        SortableHeaderRenderer renderer = SortableHeaderRenderer.create(widget, row);
        int textY = widget.getY() + 7;
        drawHeaderLabel(widget, SortColumn.PROJECT, renderer.getColumnPosX(0), textY, drawContext);
        drawHeaderLabel(widget, SortColumn.STATUS, renderer.getColumnPosX(1), textY, drawContext);
        drawHeaderLabel(widget, SortColumn.FILE, renderer.getColumnPosX(2), textY, drawContext);
        if (isEditPage(row.pageId())) {
            widget.drawString(
                    renderer.getColumnPosX(3),
                    textY,
                    -1,
                    GuiBase.TXT_BOLD + StringUtils.translate("lmlp.gui.known_placement.header.actions") + GuiBase.TXT_RST,
                    drawContext);
        }
        renderer.renderOriginalColumnHeader(mouseX, mouseY);
    }

    public static boolean clickTableHeader(WidgetBase widget, KnownPlacementRow row, int mouseX, int mouseY) {
        if (!row.isTableHeader() || !widget.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int hoveredColumn = SortableHeaderRenderer.create(widget, row).mouseOverColumn(mouseX, mouseY);
        if (hoveredColumn < 0 || hoveredColumn > SortColumn.FILE.ordinal()) {
            return false;
        }

        cycleSort(row.pageId(), SortColumn.values()[hoveredColumn]);
        return true;
    }

    public static void renderSelectedOutline(WidgetBase widget) {
        RenderUtils.drawOutline(widget.getX() + 1, widget.getY() + 1, widget.getWidth() - 2, widget.getHeight() - 2, 0xFFFFFFFF);
    }

    public static void renderPlacementIcon(WidgetBase widget, float zLevel, class_332 drawContext) {
        RenderUtils.color(1.0F, 1.0F, 1.0F, 1.0F);
        widget.bindTexture(Icons.TEXTURE);
        Icons.SCHEMATIC_TYPE_FILE.renderAt(
                widget.getX() + PLACEMENT_ICON_X,
                widget.getY() + (widget.getHeight() - Icons.SCHEMATIC_TYPE_FILE.getHeight()) / 2,
                zLevel,
                false,
                false);
    }

    public static PlacementLine placementLine(WidgetBase widget, KnownPlacementContext context, String placementName, int contentRight) {
        PlacementStatus status = placementStatus(context);
        int statusTextWidth = status == null ? 0 : widget.getStringWidth(status.text());
        int statusWidth = Math.max(statusTextWidth, statusColumnWidth(widget));
        PlacementLineLayout layout = placementLineLayout(widget, contentRight, statusWidth);
        String nameText = truncateToWidth(widget, placementName == null ? "" : placementName, layout.nameWidth());
        String fileName = schematicDisplayName(context);
        String fileText = truncateToWidth(widget, fileName, layout.fileWidth());
        boolean fileTruncated = !fileText.equals(fileName);

        return new PlacementLine(
                layout,
                nameText,
                widget.getStringWidth(nameText),
                fileText,
                widget.getStringWidth(fileText),
                fileTruncated,
                schematicHoverText(context),
                status,
                statusTextWidth);
    }

    public static void renderPlacementLine(WidgetBase widget, float zLevel, class_332 drawContext, PlacementLine line, String nameColor, boolean nameHovered) {
        renderPlacementIcon(widget, zLevel, drawContext);

        int textY = textY(widget);
        String formattedName = nameColor + (nameHovered ? UNDERLINE : "") + line.nameText() + GuiBase.TXT_RST;
        widget.drawString(line.layout().nameX(), textY, 0xFFFFFFFF, formattedName, drawContext);

        if (!line.fileText().isEmpty()) {
            widget.drawString(line.layout().fileX(), textY, FILE_TEXT_COLOR, line.fileText(), drawContext);
        }

        if (line.status() != null) {
            widget.drawString(line.layout().statusX(), textY, line.status().color(), line.status().text(), drawContext);
        }
    }

    public static int contentRight(WidgetBase widget) {
        return widget.getX() + widget.getWidth() - ROW_RIGHT_PADDING;
    }

    public static int contentRight(WidgetBase widget, int buttonsStartX) {
        int fallback = contentRight(widget);
        if (buttonsStartX <= widget.getX() || buttonsStartX > widget.getX() + widget.getWidth()) {
            return fallback;
        }

        return Math.max(widget.getX() + PLACEMENT_INDENT + MIN_NAME_WIDTH, Math.min(fallback, buttonsStartX - ROW_RIGHT_PADDING));
    }

    public static void addTranslatedTooltipLines(List<String> lines, String key) {
        StringUtils.translate(key).lines().forEach(lines::add);
    }

    public static int textY(WidgetBase widget) {
        return widget.getY() + (widget.getHeight() - TEXT_HEIGHT) / 2;
    }

    public static int buttonY(int rowY) {
        return rowY + Math.max(2, (ROW_HEIGHT - BUTTON_HEIGHT) / 2);
    }

    private static float arrowProgress(KnownPlacementRow row) {
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
        strings.add(context.sourceState().name().toLowerCase(Locale.ROOT));
        ReadStatus status = readStatus(context);
        strings.add(status.label().toLowerCase(Locale.ROOT));
        strings.add(status.name().toLowerCase(Locale.ROOT));
    }

    private static void drawIcon(String dimension, int x, int y, class_332 drawContext) {
        drawContext.method_25290(icon(dimension), x, y, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private static class_2960 icon(String dimension) {
        return switch (normalizedDimension(dimension)) {
            case "minecraft:overworld" -> OVERWORLD_ICON;
            case "minecraft:the_nether" -> NETHER_ICON;
            case "minecraft:the_end" -> END_ICON;
            default -> DIM_ICON;
        };
    }

    private static PlacementLineLayout placementLineLayout(WidgetBase widget, int contentRight, int statusWidth) {
        int rowLeft = widget.getX();
        int nameX = rowLeft + PLACEMENT_INDENT;
        int right = Math.max(nameX + MIN_NAME_WIDTH, contentRight);
        int statusX = -1;
        if (statusWidth > 0) {
            int preferredStatusX = Math.max(rowLeft + STATUS_COLUMN_MIN_X, rowLeft + widget.getWidth() * 30 / 100);
            int maxStatusX = Math.max(nameX + MIN_NAME_WIDTH + COLUMN_GAP, right - statusWidth - COLUMN_GAP - MIN_FILE_WIDTH);
            statusX = Math.min(Math.max(nameX + MIN_NAME_WIDTH + COLUMN_GAP, preferredStatusX), maxStatusX);
        }

        int fileMinX = statusWidth > 0
                ? statusX + statusWidth + COLUMN_GAP
                : nameX + MIN_NAME_WIDTH + COLUMN_GAP;
        int preferredFileX = Math.max(rowLeft + FILE_COLUMN_MIN_X, rowLeft + widget.getWidth() * 52 / 100);
        int maxFileX = Math.max(fileMinX, right - MIN_FILE_WIDTH);
        int fileX = Math.min(Math.max(fileMinX, preferredFileX), maxFileX);

        int nameRight = statusWidth > 0 ? statusX - COLUMN_GAP : fileX - COLUMN_GAP;
        int nameWidth = Math.max(0, nameRight - nameX);
        int fileRight = right;
        int fileWidth = Math.max(0, fileRight - fileX);
        return new PlacementLineLayout(nameX, nameWidth, fileX, fileWidth, statusX, statusWidth);
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
        return width;
    }

    private static int[] headerColumnPositions(WidgetBase widget, KnownPlacementRow row) {
        boolean editPage = isEditPage(row.pageId());
        int fullRight = contentRight(widget);
        int contentRight = editPage
                ? Math.max(widget.getX() + PLACEMENT_INDENT + MIN_NAME_WIDTH, widget.getX() + widget.getWidth() - 110)
                : fullRight;
        PlacementLineLayout layout = placementLineLayout(widget, contentRight, statusColumnWidth(widget));
        if (!editPage) {
            return new int[] { layout.nameX(), layout.statusX(), layout.fileX(), fullRight };
        }

        int actionX = Math.max(layout.fileX() + MIN_FILE_WIDTH + COLUMN_GAP, widget.getX() + widget.getWidth() - 90);
        return new int[] { layout.nameX(), layout.statusX(), layout.fileX(), actionX, fullRight };
    }

    private static void drawHeaderLabel(WidgetBase widget, SortColumn column, int x, int y, class_332 drawContext) {
        String label = switch (column) {
            case PROJECT -> StringUtils.translate("lmlp.gui.known_placement.header.project");
            case STATUS -> StringUtils.translate("lmlp.gui.known_placement.header.status");
            case FILE -> StringUtils.translate("lmlp.gui.known_placement.header.schematic_name");
        };
        widget.drawString(x, y, -1, GuiBase.TXT_BOLD + label + GuiBase.TXT_RST, drawContext);
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
        };
        return state.descending() ? comparator.reversed() : comparator;
    }

    private static SortState sortState(String pageId) {
        return SORT_STATES.getOrDefault(pageId, new SortState(SortColumn.PROJECT, false));
    }

    private static boolean isEditPage(String pageId) {
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

    private static String schematicHoverText(KnownPlacementContext context) {
        if (context == null) {
            return "";
        }

        if (!context.schematicPath().isEmpty()) {
            return context.schematicPath();
        }

        return schematicDisplayName(context);
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
            String fileText,
            int fileTextWidth,
            boolean fileTruncated,
            String fileHoverText,
            PlacementStatus status,
            int statusTextWidth) {
        public boolean nameHovered(WidgetBase widget, int mouseX, int mouseY) {
            return isTextHovered(widget, this.layout.nameX(), this.nameTextWidth, mouseX, mouseY);
        }

        public boolean fileHovered(WidgetBase widget, int mouseX, int mouseY) {
            return this.fileTruncated && isTextHovered(widget, this.layout.fileX(), this.fileTextWidth, mouseX, mouseY);
        }

        public boolean statusHovered(WidgetBase widget, int mouseX, int mouseY) {
            return this.status != null && isTextHovered(widget, this.layout.statusX(), this.statusTextWidth, mouseX, mouseY);
        }
    }

    public record PlacementLineLayout(
            int nameX,
            int nameWidth,
            int fileX,
            int fileWidth,
            int statusX,
            int statusWidth) {
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
        FILE
    }

    public enum ReadStatus {
        LIVE("lmlp.gui.known_placement.status.live", "lmlp.gui.known_placement.status.live_hint", 0xFF66CC66, 0),
        CACHE("lmlp.gui.known_placement.status.cache", "lmlp.gui.known_placement.status.cache_hint", 0xFFFFCC66, 1),
        OFFLINE("lmlp.gui.known_placement.status.offline_cache", "lmlp.gui.known_placement.status.offline_cache_hint", 0xFFFFAA66, 2);

        private final String translationKey;
        private final String tooltipKey;
        private final int color;
        private final int order;

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
            super(source.getX(), source.getY(), source.getWidth(), source.getHeight(), row, -1);
            this.sortState = sortState(row.pageId());
            this.columnPositions = columnPositions;
            this.columnCount = Math.max(1, columnPositions.length - 1);
        }

        private static SortableHeaderRenderer create(WidgetBase source, KnownPlacementRow row) {
            return new SortableHeaderRenderer(source, row, headerColumnPositions(source, row));
        }

        private void renderOriginalColumnHeader(int mouseX, int mouseY) {
            this.renderColumnHeader(mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP);
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
