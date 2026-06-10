package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import net.minecraft.class_2960;
import net.minecraft.class_332;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KnownPlacementRows {
    public static final int ROW_HEIGHT = 26;
    public static final int ICON_SIZE = 16;
    public static final int PLACEMENT_INDENT = 24;

    private static final class_2960 OVERWORLD_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/overworld.png");
    private static final class_2960 NETHER_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/nether.png");
    private static final class_2960 END_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/end.png");
    private static final class_2960 DIM_ICON = new class_2960(LitematicaMaterialListPlus.MOD_ID, "textures/gui/dimensions/dim.png");
    private static final Map<String, Boolean> COLLAPSED_GROUPS = new LinkedHashMap<>();

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
        COLLAPSED_GROUPS.put(key, !COLLAPSED_GROUPS.getOrDefault(key, false));
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
        int background = hovered ? 0xA0707070 : 0xA0202020;
        RenderUtils.drawRect(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), background);

        String arrow = row.expanded() ? "\u25BE" : "\u25B8";
        int centerY = widget.getY() + (widget.getHeight() - 8) / 2;
        widget.drawString(widget.getX() + 6, centerY, 0xFFFFFFFF, arrow, drawContext);
        drawIcon(row.dimension(), widget.getX() + 18, widget.getY() + 5, drawContext);
        widget.drawString(widget.getX() + 39, centerY, 0xFFE0E0E0, row.displayName(), drawContext);
    }

    public static void renderSelectedOutline(WidgetBase widget) {
        RenderUtils.drawOutline(widget.getX() + 1, widget.getY() + 1, widget.getWidth() - 2, widget.getHeight() - 2, 0xFFFFFFFF);
    }

    public static String displayName(String dimension) {
        String normalized = normalizedDimension(dimension);
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
        return dimension == null || dimension.isEmpty() ? "unknown" : dimension;
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
