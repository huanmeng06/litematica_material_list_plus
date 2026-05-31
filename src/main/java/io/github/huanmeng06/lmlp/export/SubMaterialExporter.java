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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SubMaterialExporter {
    private static final int MAX_RECIPE_DEPTH = 16;
    private static final List<String> TREE_HEADERS = List.of("Material", "Type", "Total", "Missing", "Available");

    private SubMaterialExporter() {
    }

    public static File export(MaterialListBase materialList) {
        File dir = new File(FileUtils.getConfigDirectory(), "litematica");
        if ((!dir.exists() && !dir.mkdirs()) || !dir.isDirectory()) {
            return null;
        }

        List<TreeRow> rows = buildRows(materialList);
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

    private static List<TreeRow> buildRows(MaterialListBase materialList) {
        List<MaterialListEntry> entries = new ArrayList<>(materialList.getMaterialsFiltered(false));
        entries.sort(new MaterialListSorter(materialList));

        List<TreeRow> rows = new ArrayList<>();
        for (MaterialListEntry entry : entries) {
            class_1799 stack = entry.getStack();
            int total = MaterialCounts.total(entry, materialList);
            int missing = MaterialCounts.missing(entry, materialList);
            rows.add(TreeRow.root(ItemStackTexts.name(stack), stack.method_7914(), total, missing));
            appendChildren(stack, total, missing, 1, List.of(), new HashSet<>(), rows);
        }
        return rows;
    }

    private static void appendChildren(class_1799 stack, int totalCount, int missingCount, int depth, List<Boolean> ancestorLast, Set<String> seenItems, List<TreeRow> rows) {
        class_1799 icon = stack.method_7972();
        String itemId = ItemStackTexts.id(icon);
        if (depth > MAX_RECIPE_DEPTH || seenItems.contains(itemId) || Configs.shouldStopRecipeDecomposition(itemId)) {
            return;
        }

        Set<String> childSeenItems = new HashSet<>(seenItems);
        childSeenItems.add(itemId);
        List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, totalCount, missingCount);
        if (summaries.isEmpty() || summaries.get(0).ingredients().isEmpty()) {
            return;
        }

        List<IngredientSummary> ingredients = summaries.get(0).ingredients();
        for (int index = 0; index < ingredients.size(); index++) {
            IngredientSummary ingredient = ingredients.get(index);
            boolean last = index == ingredients.size() - 1;
            String name = RecipeSummaryFormatter.ingredientName(ingredient);
            rows.add(TreeRow.child(
                    name,
                    ingredient.icon().method_7914(),
                    ingredient.countTotal(),
                    ingredient.countMissing(),
                    depth,
                    treePrefix(ancestorLast, last)));

            List<Boolean> childAncestorLast = new ArrayList<>(ancestorLast);
            childAncestorLast.add(last);
            appendChildren(
                    ingredient.icon(),
                    ingredient.countTotal(),
                    ingredient.countMissing(),
                    depth + 1,
                    childAncestorLast,
                    childSeenItems,
                    rows);
        }
    }

    private static String treePrefix(List<Boolean> ancestorLast, boolean last) {
        StringBuilder builder = new StringBuilder();
        for (boolean ancestor : ancestorLast) {
            builder.append(ancestor ? "   " : "│  ");
        }
        builder.append(last ? "└─ " : "├─ ");
        return builder.toString();
    }

    private static void writeWorkbook(File file, List<TreeRow> rows) throws IOException {
        List<List<String>> sheetRows = sheetRows(rows);
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            addZipEntry(zip, "[Content_Types].xml", contentTypesXml());
            addZipEntry(zip, "_rels/.rels", rootRelationshipsXml());
            addZipEntry(zip, "docProps/app.xml", appPropertiesXml());
            addZipEntry(zip, "docProps/core.xml", corePropertiesXml());
            addZipEntry(zip, "xl/workbook.xml", workbookXml());
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelationshipsXml());
            addZipEntry(zip, "xl/styles.xml", stylesXml());
            addZipEntry(zip, "xl/worksheets/sheet1.xml", worksheetXml(sheetRows, rows));
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

    private static List<List<String>> sheetRows(List<TreeRow> rows) {
        List<List<String>> sheetRows = new ArrayList<>();
        sheetRows.add(TREE_HEADERS);
        for (TreeRow row : rows) {
            sheetRows.add(row.asCells());
        }
        return sheetRows;
    }

    private static String worksheetXml(List<List<String>> rows, List<TreeRow> treeRows) {
        int rowCount = Math.max(1, rows.size());
        int columnCount = TREE_HEADERS.size();
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
                int style = cellStyle(rowIndex, columnIndex, rowIndex > 0 && treeRows.get(rowIndex - 1).depth() == 0);
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

    private static int cellStyle(int rowIndex, int columnIndex, boolean mainRow) {
        if (rowIndex == 0) {
            return 3;
        }

        if (mainRow) {
            return columnIndex == 0 ? 4 : 5;
        }

        return columnIndex == 0 ? 1 : 2;
    }

    private static void appendColumns(StringBuilder builder, List<List<String>> rows, int columnCount) {
        builder.append("<cols>");
        for (int column = 0; column < columnCount; column++) {
            int width = column == 0 ? 12 : 8;
            for (List<String> row : rows) {
                if (column < row.size()) {
                    width = Math.max(width, visualWidth(row.get(column)) + 2);
                }
            }
            builder.append("<col min=\"").append(column + 1)
                    .append("\" max=\"").append(column + 1)
                    .append("\" width=\"").append(Math.min(width, column == 0 ? 56 : 28))
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
                + "<sheets><sheet name=\"Material Tree\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "</workbook>";
    }

    private static String workbookRelationshipsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                + "</Relationships>";
    }

    private static String stylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<fonts count=\"3\">"
                + "<font><sz val=\"11\"/><color theme=\"1\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
                + "<font><b/><sz val=\"11\"/><color theme=\"1\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
                + "</fonts>"
                + "<fills count=\"4\">"
                + "<fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1F4E78\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFDDEBF7\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "</fills>"
                + "<borders count=\"2\">"
                + "<border><left/><right/><top/><bottom/><diagonal/></border>"
                + "<border><left style=\"thin\"><color rgb=\"FFD9E2F3\"/></left><right style=\"thin\"><color rgb=\"FFD9E2F3\"/></right><top style=\"thin\"><color rgb=\"FFD9E2F3\"/></top><bottom style=\"thin\"><color rgb=\"FFD9E2F3\"/></bottom><diagonal/></border>"
                + "</borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"6\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\"><alignment horizontal=\"left\" vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"left\" vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
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
                + "<HeadingPairs><vt:vector size=\"2\" baseType=\"variant\"><vt:variant><vt:lpstr>Worksheets</vt:lpstr></vt:variant><vt:variant><vt:i4>1</vt:i4></vt:variant></vt:vector></HeadingPairs>"
                + "<TitlesOfParts><vt:vector size=\"1\" baseType=\"lpstr\"><vt:lpstr>Material Tree</vt:lpstr></vt:vector></TitlesOfParts>"
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

    private record TreeRow(String material, String type, int depth, int maxStackSize, long totalCount, long missingCount) {
        private static TreeRow root(String name, int maxStackSize, long totalCount, long missingCount) {
            return new TreeRow(name, "Main", 0, maxStackSize, totalCount, missingCount);
        }

        private static TreeRow child(String name, int maxStackSize, long totalCount, long missingCount, int depth, String prefix) {
            return new TreeRow(prefix + name, typeName(depth), depth, maxStackSize, totalCount, missingCount);
        }

        private static String typeName(int depth) {
            if (depth <= 0) {
                return "Main";
            }
            if (depth == 1) {
                return "Sub";
            }
            if (depth == 2) {
                return "Sub-sub";
            }
            return "Sub-depth";
        }

        private long availableCount() {
            return this.totalCount - this.missingCount;
        }

        private List<String> asCells() {
            return List.of(
                    this.material(),
                    this.type(),
                    CountFormatter.format(this.totalCount(), this.maxStackSize()),
                    CountFormatter.format(this.missingCount(), this.maxStackSize()),
                    CountFormatter.format(this.availableCount(), this.maxStackSize())
            );
        }
    }
}
