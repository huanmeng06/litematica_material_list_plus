package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import net.minecraft.class_2960;
import net.minecraft.class_332;

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
    private static final int FILE_TEXT_COLOR = 0xFFB8B8B8;

    private static final class_2960 OVERWORLD_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/overworld.png");
    private static final class_2960 NETHER_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/nether.png");
    private static final class_2960 END_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/end.png");
    private static final class_2960 DIM_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/dim.png");
    private static final Map<String, Boolean> COLLAPSED_GROUPS = new LinkedHashMap<>();
    private static final ExpandAnimationTracker GROUP_ANIMATIONS = new ExpandAnimationTracker();

    private KnownPlacementRows() {
    }

    public static List<KnownPlacementRow> rows(String pageId) {
        return rows(pageId, ChunkMissingMaterialListCache.knownPlacementContexts());
    }

    public static List<KnownPlacementRow> rows(String pageId, Collection<KnownPlacementContext> contexts) {
        List<KnownPlacementContext> sortedContexts = contexts.stream()
                .sorted(Comparator
                        .comparing((KnownPlacementContext context) -> dimensionSortKey(context.dimension()))
                        .thenComparing(context -> normalizedDimension(context.dimension()))
                        .thenComparing(KnownPlacementContext::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(KnownPlacementContext::key))
                .toList();

        Map<String, List<KnownPlacementContext>> groups = new LinkedHashMap<>();
        for (KnownPlacementContext context : sortedContexts) {
            groups.computeIfAbsent(normalizedDimension(context.dimension()), key -> new ArrayList<>()).add(context);
        }

        List<KnownPlacementRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<KnownPlacementContext>> entry : groups.entrySet()) {
            String dimension = entry.getKey();
            List<KnownPlacementContext> groupContexts = List.copyOf(entry.getValue());
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
        int statusWidth = status == null ? 0 : widget.getStringWidth(status.text());
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
                status);
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
        int preferredFileX = Math.max(rowLeft + FILE_COLUMN_MIN_X, rowLeft + widget.getWidth() * 42 / 100);
        int maxFileX = Math.max(nameX + MIN_NAME_WIDTH + COLUMN_GAP, right - MIN_FILE_WIDTH);
        int fileX = Math.min(Math.max(nameX + MIN_NAME_WIDTH + COLUMN_GAP, preferredFileX), maxFileX);

        int statusX = -1;
        int fileRight = right;
        if (statusWidth > 0) {
            statusX = Math.max(fileX + MIN_FILE_WIDTH + COLUMN_GAP, right - statusWidth);
            statusX = Math.min(statusX, Math.max(nameX + MIN_NAME_WIDTH + COLUMN_GAP, right - statusWidth));
            fileRight = statusX - COLUMN_GAP;
        }

        int nameWidth = Math.max(0, fileX - nameX - COLUMN_GAP);
        int fileWidth = Math.max(0, fileRight - fileX);
        return new PlacementLineLayout(nameX, nameWidth, fileX, fileWidth, statusX, statusWidth);
    }

    private static PlacementStatus placementStatus(KnownPlacementContext context) {
        if (context == null) {
            return null;
        }

        if (context.offlineCache()) {
            List<String> lines = new ArrayList<>();
            addTranslatedTooltipLines(lines, "lmlp.gui.known_placement.offline_cache_hint");
            StringBuilder statusText = new StringBuilder(StringUtils.translate("lmlp.gui.known_placement.offline_cache"));
            if (context.schematicMissing()) {
                statusText.append(" / ").append(StringUtils.translate("lmlp.gui.known_placement.status_schematic_missing"));
                lines.add(StringUtils.translate("lmlp.gui.known_placement.schematic_missing"));
            }
            if (!context.hasMaterialCache()) {
                statusText.append(" / ").append(StringUtils.translate("lmlp.gui.known_placement.status_cache_empty"));
                lines.add(StringUtils.translate("lmlp.gui.known_placement.offline_cache_empty"));
            }
            return new PlacementStatus(statusText.toString(), 0xFFFFAA66, lines);
        }

        if (context.schematicMissing()) {
            return new PlacementStatus(
                    StringUtils.translate("lmlp.gui.known_placement.status_schematic_missing"),
                    0xFFFF5555,
                    List.of(StringUtils.translate("lmlp.gui.known_placement.schematic_missing")));
        }

        if (!context.canEdit()) {
            return new PlacementStatus(
                    StringUtils.translate("lmlp.gui.known_placement.cache_only"),
                    0xFFAAAAAA,
                    List.of(StringUtils.translate("lmlp.gui.known_placement.cache_only_hint")));
        }

        return null;
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
            PlacementStatus status) {
        public boolean nameHovered(WidgetBase widget, int mouseX, int mouseY) {
            return isTextHovered(widget, this.layout.nameX(), this.nameTextWidth, mouseX, mouseY);
        }

        public boolean fileHovered(WidgetBase widget, int mouseX, int mouseY) {
            return this.fileTruncated && isTextHovered(widget, this.layout.fileX(), this.fileTextWidth, mouseX, mouseY);
        }

        public boolean statusHovered(WidgetBase widget, int mouseX, int mouseY) {
            return this.status != null && isTextHovered(widget, this.layout.statusX(), this.layout.statusWidth(), mouseX, mouseY);
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

        public boolean isPlacement() {
            return this.type == RowType.PLACEMENT && this.context != null;
        }
    }

    public enum RowType {
        HEADER,
        PLACEMENT
    }
}
