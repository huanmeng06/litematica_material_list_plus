package io.github.huanmeng06.lmlp.export;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListSorter;
import fi.dy.masa.malilib.data.DataDump;
import fi.dy.masa.malilib.util.FileUtils;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.material.MaterialCounts;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeResolvers;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import net.minecraft.class_1799;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SubMaterialExporter {
    private static final int MAX_RECIPE_DEPTH = 16;
    private static final List<String> DETAIL_HEADERS = List.of(
            "Item",
            "Total",
            "Missing",
            "Available",
            "Sub-material",
            "Sub Total",
            "Sub Missing",
            "Sub Available"
    );
    private static final List<String> SUMMARY_HEADERS = List.of("Item", "Total", "Missing", "Available");

    private SubMaterialExporter() {
    }

    public static File export(MaterialListBase materialList, boolean csv) {
        ExportRows rows = buildRows(materialList);
        List<String> lines = new ArrayList<>();
        lines.addAll(detailDump(rows.detailRows(), csv).getLines());
        lines.add("");
        lines.add("Sheet2 - Minimal sub-material totals");
        lines.addAll(summaryDump(rows.summaryRows(), csv).getLines());

        File dir = new File(FileUtils.getConfigDirectory(), "litematica");
        return DataDump.dumpDataToFile(dir, "sub_material_list", csv ? ".csv" : ".txt", lines);
    }

    private static ExportRows buildRows(MaterialListBase materialList) {
        List<MaterialListEntry> entries = new ArrayList<>(materialList.getMaterialsFiltered(false));
        entries.sort(new MaterialListSorter(materialList));

        List<DetailRow> detailRows = new ArrayList<>();
        Map<String, SummaryAccumulator> summaryRows = new HashMap<>();
        for (MaterialListEntry entry : entries) {
            class_1799 stack = entry.getStack();
            int total = MaterialCounts.total(entry, materialList);
            int missing = MaterialCounts.missing(entry, materialList);
            SourceMaterial source = new SourceMaterial(ItemStackTexts.name(stack), stack.method_7914(), total, missing);
            List<LeafMaterial> leaves = new ArrayList<>();
            collectLeaves(stack, source.name(), total, missing, 0, new HashSet<>(), leaves);

            Map<String, LeafAccumulator> sourceLeaves = new LinkedHashMap<>();
            for (LeafMaterial leaf : leaves) {
                sourceLeaves.computeIfAbsent(leaf.groupKey(), ignored -> new LeafAccumulator(leaf.name(), leaf.maxStackSize()))
                        .add(leaf.totalCount(), leaf.missingCount());
                summaryRows.computeIfAbsent(leaf.groupKey(), ignored -> new SummaryAccumulator(leaf.name(), leaf.maxStackSize()))
                        .add(leaf.totalCount(), leaf.missingCount());
            }

            for (LeafAccumulator leaf : sourceLeaves.values()) {
                detailRows.add(new DetailRow(source, leaf.toLeaf()));
            }
        }

        List<SummaryRow> summaries = summaryRows.values().stream()
                .map(SummaryAccumulator::toRow)
                .sorted(Comparator.comparing(SummaryRow::name))
                .toList();
        return new ExportRows(detailRows, summaries);
    }

    private static void collectLeaves(class_1799 stack, String name, int totalCount, int missingCount, int depth, Set<String> seenItems, List<LeafMaterial> leaves) {
        class_1799 icon = stack.method_7972();
        String itemId = ItemStackTexts.id(icon);
        if (totalCount <= 0 && missingCount <= 0) {
            return;
        }

        if (depth >= MAX_RECIPE_DEPTH || seenItems.contains(itemId) || Configs.shouldStopRecipeDecomposition(itemId)) {
            leaves.add(new LeafMaterial(itemId, name, icon.method_7914(), totalCount, missingCount));
            return;
        }

        Set<String> childSeenItems = new HashSet<>(seenItems);
        childSeenItems.add(itemId);
        List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, totalCount, missingCount);
        if (summaries.isEmpty() || summaries.get(0).ingredients().isEmpty()) {
            leaves.add(new LeafMaterial(itemId, name, icon.method_7914(), totalCount, missingCount));
            return;
        }

        for (IngredientSummary ingredient : summaries.get(0).ingredients()) {
            collectLeaves(
                    ingredient.icon(),
                    RecipeSummaryFormatter.ingredientName(ingredient),
                    ingredient.countTotal(),
                    ingredient.countMissing(),
                    depth + 1,
                    childSeenItems,
                    leaves);
        }
    }

    private static DataDump detailDump(List<DetailRow> rows, boolean csv) {
        DataDump dump = new DataDump(DETAIL_HEADERS.size(), csv ? DataDump.Format.CSV : DataDump.Format.ASCII);
        dump.addHeader(DETAIL_HEADERS.toArray(new String[0]));
        for (DetailRow row : rows) {
            dump.addData(row.asCells().toArray(new String[0]));
        }

        for (int column = 1; column < DETAIL_HEADERS.size(); column++) {
            if (column != 4) {
                dump.setColumnProperties(column, DataDump.Alignment.RIGHT, true);
            }
        }
        dump.setSort(false);
        dump.setUseColumnSeparator(true);
        return dump;
    }

    private static DataDump summaryDump(List<SummaryRow> rows, boolean csv) {
        DataDump dump = new DataDump(SUMMARY_HEADERS.size(), csv ? DataDump.Format.CSV : DataDump.Format.ASCII);
        dump.addHeader(SUMMARY_HEADERS.toArray(new String[0]));
        for (SummaryRow row : rows) {
            dump.addData(row.asCells().toArray(new String[0]));
        }

        for (int column = 1; column < SUMMARY_HEADERS.size(); column++) {
            dump.setColumnProperties(column, DataDump.Alignment.RIGHT, true);
        }
        dump.setSort(false);
        dump.setUseColumnSeparator(true);
        return dump;
    }

    private record ExportRows(List<DetailRow> detailRows, List<SummaryRow> summaryRows) {
    }

    private record SourceMaterial(String name, int maxStackSize, long totalCount, long missingCount) {
        private long availableCount() {
            return this.totalCount - this.missingCount;
        }
    }

    private record LeafMaterial(String itemId, String name, int maxStackSize, long totalCount, long missingCount) {
        private String groupKey() {
            String trimmed = this.name.trim();
            return trimmed.isEmpty() ? this.itemId : trimmed.toLowerCase(Locale.ROOT);
        }

        private long availableCount() {
            return this.totalCount - this.missingCount;
        }
    }

    private record DetailRow(SourceMaterial source, LeafMaterial leaf) {
        private List<String> asCells() {
            return List.of(
                    this.source.name(),
                    CountFormatter.format(this.source.totalCount(), this.source.maxStackSize()),
                    CountFormatter.format(this.source.missingCount(), this.source.maxStackSize()),
                    CountFormatter.format(this.source.availableCount(), this.source.maxStackSize()),
                    this.leaf.name(),
                    CountFormatter.format(this.leaf.totalCount(), this.leaf.maxStackSize()),
                    CountFormatter.format(this.leaf.missingCount(), this.leaf.maxStackSize()),
                    CountFormatter.format(this.leaf.availableCount(), this.leaf.maxStackSize())
            );
        }
    }

    private record SummaryRow(String name, int maxStackSize, long totalCount, long missingCount) {
        private long availableCount() {
            return this.totalCount - this.missingCount;
        }

        private List<String> asCells() {
            return List.of(
                    this.name(),
                    CountFormatter.format(this.totalCount(), this.maxStackSize()),
                    CountFormatter.format(this.missingCount(), this.maxStackSize()),
                    CountFormatter.format(this.availableCount(), this.maxStackSize())
            );
        }
    }

    private static final class LeafAccumulator {
        private final String name;
        private final int maxStackSize;
        private long totalCount;
        private long missingCount;

        private LeafAccumulator(String name, int maxStackSize) {
            this.name = name;
            this.maxStackSize = maxStackSize;
        }

        private void add(long totalCount, long missingCount) {
            this.totalCount += totalCount;
            this.missingCount += missingCount;
        }

        private LeafMaterial toLeaf() {
            return new LeafMaterial("", this.name, this.maxStackSize, this.totalCount, this.missingCount);
        }
    }

    private static final class SummaryAccumulator {
        private final String name;
        private final int maxStackSize;
        private long totalCount;
        private long missingCount;

        private SummaryAccumulator(String name, int maxStackSize) {
            this.name = name;
            this.maxStackSize = maxStackSize;
        }

        private void add(long totalCount, long missingCount) {
            this.totalCount += totalCount;
            this.missingCount += missingCount;
        }

        private SummaryRow toRow() {
            return new SummaryRow(this.name, this.maxStackSize, this.totalCount, this.missingCount);
        }
    }
}
