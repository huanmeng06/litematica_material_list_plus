package io.github.huanmeng06.lmlp.export;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListSorter;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SubMaterialExporter {
    private static final int MAX_RECIPE_DEPTH = 16;
    private static final List<String> DETAIL_HEADERS = List.of(
            "Item",
            "Total",
            "Missing",
            "Available",
            "Sub-material",
            "Total",
            "Missing",
            "Available"
    );
    private static final List<String> SUMMARY_HEADERS = List.of("Item", "Total", "Missing", "Available");

    private SubMaterialExporter() {
    }

    public static File export(MaterialListBase materialList) {
        File dir = new File(FileUtils.getConfigDirectory(), "litematica");
        if ((!dir.exists() && !dir.mkdirs()) || !dir.isDirectory()) {
            return null;
        }

        ExportRows rows = buildRows(materialList);
        File file = timestampedFile(dir, "sub_material_list", ".xlsx");
        try {
            writeWorkbook(file, rows);
            return file;
        } catch (IOException e) {
            if (file.exists()) {
                file.delete();
            }
            return null;
        }
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

    private static void writeWorkbook(File file, ExportRows rows) throws IOException {
        List<List<String>> detailRows = detailSheetRows(rows.detailRows());
        List<List<String>> summaryRows = summarySheetRows(rows.summaryRows());
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            addZipEntry(zip, "[Content_Types].xml", contentTypesXml());
            addZipEntry(zip, "_rels/.rels", rootRelationshipsXml());
            addZipEntry(zip, "docProps/app.xml", appPropertiesXml());
            addZipEntry(zip, "docProps/core.xml", corePropertiesXml());
            addZipEntry(zip, "xl/workbook.xml", workbookXml());
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelationshipsXml());
            addZipEntry(zip, "xl/styles.xml", stylesXml());
            addZipEntry(zip, "xl/worksheets/sheet1.xml", worksheetXml(detailRows, countColumns(1, 2, 3, 5, 6, 7)));
            addZipEntry(zip, "xl/worksheets/sheet2.xml", worksheetXml(summaryRows, countColumns(1, 2, 3)));
        }
    }

    private static File timestampedFile(File dir, String baseName, String extension) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.ROOT).format(new Date());
        File file = new File(dir, baseName + "_" + timestamp + extension);
        int suffix = 2;
        while (file.exists()) {
            file = new File(dir, baseName + "_" + timestamp + "_" + suffix + extension);
            suffix++;
        }
        return file;
    }

    private static List<List<String>> detailSheetRows(List<DetailRow> rows) {
        List<List<String>> sheetRows = new ArrayList<>();
        sheetRows.add(DETAIL_HEADERS);
        for (DetailRow row : rows) {
            sheetRows.add(row.asCells());
        }
        return sheetRows;
    }

    private static List<List<String>> summarySheetRows(List<SummaryRow> rows) {
        List<List<String>> sheetRows = new ArrayList<>();
        sheetRows.add(SUMMARY_HEADERS);
        for (SummaryRow row : rows) {
            sheetRows.add(row.asCells());
        }
        return sheetRows;
    }

    private static Set<Integer> countColumns(Integer... columns) {
        return Set.of(columns);
    }

    private static String worksheetXml(List<List<String>> rows, Set<Integer> countColumns) {
        int rowCount = Math.max(1, rows.size());
        int columnCount = rows.stream().mapToInt(List::size).max().orElse(1);
        String range = "A1:" + cellReference(columnCount - 1, rowCount);
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        builder.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        builder.append("<dimension ref=\"").append(range).append("\"/>");
        builder.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/><selection pane=\"bottomLeft\"/></sheetView></sheetViews>");
        builder.append("<sheetFormatPr defaultRowHeight=\"18\"/>");
        appendColumns(builder, rows, columnCount);
        builder.append("<sheetData>");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            int rowNumber = rowIndex + 1;
            builder.append("<row r=\"").append(rowNumber).append("\"");
            if (rowIndex == 0) {
                builder.append(" ht=\"20\" customHeight=\"1\"");
            }
            builder.append(">");
            List<String> row = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                int style = cellStyle(rowIndex, columnIndex, countColumns.contains(columnIndex));
                builder.append("<c r=\"").append(cellReference(columnIndex, rowNumber)).append("\" s=\"").append(style).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                        .append(escapeXml(row.get(columnIndex)))
                        .append("</t></is></c>");
            }
            builder.append("</row>");
        }
        builder.append("</sheetData>");
        builder.append("<autoFilter ref=\"").append(range).append("\"/>");
        builder.append("<pageMargins left=\"0.7\" right=\"0.7\" top=\"0.75\" bottom=\"0.75\" header=\"0.3\" footer=\"0.3\"/>");
        builder.append("</worksheet>");
        return builder.toString();
    }

    private static int cellStyle(int rowIndex, int columnIndex, boolean countColumn) {
        if (rowIndex == 0) {
            return 3;
        }

        boolean stripe = rowIndex % 2 == 0;
        if (countColumn) {
            return stripe ? 5 : 2;
        }

        return stripe ? 4 : 1;
    }

    private static void appendColumns(StringBuilder builder, List<List<String>> rows, int columnCount) {
        builder.append("<cols>");
        for (int column = 0; column < columnCount; column++) {
            int width = 8;
            for (List<String> row : rows) {
                if (column < row.size()) {
                    width = Math.max(width, visualWidth(row.get(column)) + 2);
                }
            }
            builder.append("<col min=\"").append(column + 1)
                    .append("\" max=\"").append(column + 1)
                    .append("\" width=\"").append(Math.min(width, 42))
                    .append("\" customWidth=\"1\"/>");
        }
        builder.append("</cols>");
    }

    private static int visualWidth(String value) {
        int width = 0;
        for (int i = 0; i < value.length(); i++) {
            width += value.charAt(i) > 127 ? 2 : 1;
        }
        return width;
    }

    private static String cellReference(int columnIndex, int rowNumber) {
        StringBuilder column = new StringBuilder();
        int value = columnIndex + 1;
        while (value > 0) {
            int remainder = (value - 1) % 26;
            column.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return column + Integer.toString(rowNumber);
    }

    private static void addZipEntry(ZipOutputStream zip, String path, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>"
                + "<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>";
    }

    private static String rootRelationshipsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>"
                + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/>"
                + "</Relationships>";
    }

    private static String workbookXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<bookViews><workbookView xWindow=\"240\" yWindow=\"120\" windowWidth=\"18000\" windowHeight=\"12000\"/></bookViews>"
                + "<sheets>"
                + "<sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/>"
                + "<sheet name=\"Sheet2\" sheetId=\"2\" r:id=\"rId2\"/>"
                + "</sheets>"
                + "</workbook>";
    }

    private static String workbookRelationshipsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
                + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                + "</Relationships>";
    }

    private static String stylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<fonts count=\"2\">"
                + "<font><sz val=\"11\"/><color theme=\"1\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
                + "</fonts>"
                + "<fills count=\"4\">"
                + "<fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1F4E78\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFEAF2F8\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "</fills>"
                + "<borders count=\"2\">"
                + "<border><left/><right/><top/><bottom/><diagonal/></border>"
                + "<border><left style=\"thin\"><color rgb=\"FFD9E2F3\"/></left><right style=\"thin\"><color rgb=\"FFD9E2F3\"/></right><top style=\"thin\"><color rgb=\"FFD9E2F3\"/></top><bottom style=\"thin\"><color rgb=\"FFD9E2F3\"/></bottom><diagonal/></border>"
                + "</borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"6\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\"><alignment vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\" vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFill=\"1\" applyBorder=\"1\"><alignment vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\" vertical=\"center\"/></xf>"
                + "</cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                + "<dxfs count=\"0\"/><tableStyles count=\"0\" defaultTableStyle=\"TableStyleMedium2\" defaultPivotStyle=\"PivotStyleLight16\"/>"
                + "</styleSheet>";
    }

    private static String appPropertiesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">"
                + "<Application>Litematica Material List Plus</Application>"
                + "<DocSecurity>0</DocSecurity><ScaleCrop>false</ScaleCrop>"
                + "<HeadingPairs><vt:vector size=\"2\" baseType=\"variant\"><vt:variant><vt:lpstr>Worksheets</vt:lpstr></vt:variant><vt:variant><vt:i4>2</vt:i4></vt:variant></vt:vector></HeadingPairs>"
                + "<TitlesOfParts><vt:vector size=\"2\" baseType=\"lpstr\"><vt:lpstr>Sheet1</vt:lpstr><vt:lpstr>Sheet2</vt:lpstr></vt:vector></TitlesOfParts>"
                + "<Company></Company><LinksUpToDate>false</LinksUpToDate><SharedDoc>false</SharedDoc><HyperlinksChanged>false</HyperlinksChanged><AppVersion>16.0300</AppVersion>"
                + "</Properties>";
    }

    private static String corePropertiesXml() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).format(new Date());
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:dcmitype=\"http://purl.org/dc/dcmitype/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<dc:creator>Litematica Material List Plus</dc:creator>"
                + "<cp:lastModifiedBy>Litematica Material List Plus</cp:lastModifiedBy>"
                + "<dcterms:created xsi:type=\"dcterms:W3CDTF\">" + timestamp + "</dcterms:created>"
                + "<dcterms:modified xsi:type=\"dcterms:W3CDTF\">" + timestamp + "</dcterms:modified>"
                + "</cp:coreProperties>";
    }

    private static String escapeXml(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '&' -> builder.append("&amp;");
                case '<' -> builder.append("&lt;");
                case '>' -> builder.append("&gt;");
                case '"' -> builder.append("&quot;");
                case '\'' -> builder.append("&apos;");
                default -> {
                    if (isValidXmlCharacter(character)) {
                        builder.append(character);
                    }
                }
            }
        }
        return builder.toString();
    }

    private static boolean isValidXmlCharacter(char character) {
        return character == 0x9 || character == 0xA || character == 0xD || character >= 0x20;
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
