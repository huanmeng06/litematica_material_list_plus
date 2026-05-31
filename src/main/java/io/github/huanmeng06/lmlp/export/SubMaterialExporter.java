package io.github.huanmeng06.lmlp.export;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListSorter;
import fi.dy.masa.malilib.data.DataDump;
import fi.dy.masa.malilib.util.FileUtils;
import io.github.huanmeng06.lmlp.config.Configs;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SubMaterialExporter {
    private static final int MAX_RECIPE_DEPTH = 16;

    private SubMaterialExporter() {
    }

    public static File export(MaterialListBase materialList, boolean csv) {
        ExportRows rows = buildRows(materialList);
        List<String> lines = new ArrayList<>();
        lines.addAll(detailDump(rows.detailRows(), csv).getLines());
        lines.add("");
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
            SourceMaterial source = new SourceMaterial(ItemStackTexts.id(stack), ItemStackTexts.name(stack), total, missing);
            List<LeafMaterial> leaves = new ArrayList<>();
            collectLeaves(stack, source.name(), total, missing, 0, new HashSet<>(), leaves);

            for (LeafMaterial leaf : leaves) {
                detailRows.add(new DetailRow(source, leaf));
                summaryRows.computeIfAbsent(leaf.itemId(), ignored -> new SummaryAccumulator(leaf.itemId(), leaf.name()))
                        .add(leaf.totalCount(), leaf.missingCount());
            }
        }

        List<SummaryRow> summaries = summaryRows.values().stream()
                .map(SummaryAccumulator::toRow)
                .sorted(Comparator.comparing(SummaryRow::name).thenComparing(SummaryRow::itemId))
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
            leaves.add(new LeafMaterial(itemId, name, totalCount, missingCount));
            return;
        }

        Set<String> childSeenItems = new HashSet<>(seenItems);
        childSeenItems.add(itemId);
        List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, totalCount, missingCount);
        if (summaries.isEmpty() || summaries.get(0).ingredients().isEmpty()) {
            leaves.add(new LeafMaterial(itemId, name, totalCount, missingCount));
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
        DataDump dump = new DataDump(8, csv ? DataDump.Format.CSV : DataDump.Format.ASCII);
        dump.addTitle("Sheet1 - Material to minimal sub-materials");
        dump.addHeader("Source ID", "Source", "Source Total", "Source Missing", "Sub-material ID", "Sub-material", "Sub Total", "Sub Missing");
        for (DetailRow row : rows) {
            dump.addData(
                    row.source().itemId(),
                    row.source().name(),
                    Long.toString(row.source().totalCount()),
                    Long.toString(row.source().missingCount()),
                    row.leaf().itemId(),
                    row.leaf().name(),
                    Long.toString(row.leaf().totalCount()),
                    Long.toString(row.leaf().missingCount()));
        }

        dump.setColumnProperties(2, DataDump.Alignment.RIGHT, true);
        dump.setColumnProperties(3, DataDump.Alignment.RIGHT, true);
        dump.setColumnProperties(6, DataDump.Alignment.RIGHT, true);
        dump.setColumnProperties(7, DataDump.Alignment.RIGHT, true);
        dump.setSort(false);
        dump.setUseColumnSeparator(true);
        return dump;
    }

    private static DataDump summaryDump(List<SummaryRow> rows, boolean csv) {
        DataDump dump = new DataDump(4, csv ? DataDump.Format.CSV : DataDump.Format.ASCII);
        dump.addTitle("Sheet2 - Minimal sub-material totals");
        dump.addHeader("Sub-material ID", "Sub-material", "Total", "Missing");
        for (SummaryRow row : rows) {
            dump.addData(row.itemId(), row.name(), Long.toString(row.totalCount()), Long.toString(row.missingCount()));
        }

        dump.setColumnProperties(2, DataDump.Alignment.RIGHT, true);
        dump.setColumnProperties(3, DataDump.Alignment.RIGHT, true);
        dump.setSort(false);
        dump.setUseColumnSeparator(true);
        return dump;
    }

    private record ExportRows(List<DetailRow> detailRows, List<SummaryRow> summaryRows) {
    }

    private record SourceMaterial(String itemId, String name, long totalCount, long missingCount) {
    }

    private record LeafMaterial(String itemId, String name, long totalCount, long missingCount) {
    }

    private record DetailRow(SourceMaterial source, LeafMaterial leaf) {
    }

    private record SummaryRow(String itemId, String name, long totalCount, long missingCount) {
    }

    private static final class SummaryAccumulator {
        private final String itemId;
        private final String name;
        private long totalCount;
        private long missingCount;

        private SummaryAccumulator(String itemId, String name) {
            this.itemId = itemId;
            this.name = name;
        }

        private void add(long totalCount, long missingCount) {
            this.totalCount += totalCount;
            this.missingCount += missingCount;
        }

        private SummaryRow toRow() {
            return new SummaryRow(this.itemId, this.name, this.totalCount, this.missingCount);
        }
    }
}
