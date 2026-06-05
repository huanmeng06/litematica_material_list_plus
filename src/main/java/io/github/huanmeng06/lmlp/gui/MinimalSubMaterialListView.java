package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeResolvers;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import net.minecraft.class_1799;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class MinimalSubMaterialListView {
    private static final int MAX_RECIPE_DEPTH = 16;
    private static final long DISPLAY_CYCLE_MS = 900L;
    private static final String COMMON_HIGHLIGHT = "\u00A7e\u00A7l\u00A7n";
    private static final String RESET = "\u00A7r";
    private static final Map<MaterialListBase, Boolean> ACTIVE_LISTS = new WeakHashMap<>();
    private static final Map<MaterialListBase, Cache> ENTRY_CACHES = new WeakHashMap<>();
    private static final Map<MaterialListEntry, DisplayData> ENTRY_DISPLAYS = new IdentityHashMap<>();

    private MinimalSubMaterialListView() {
    }

    public static boolean isActive(MaterialListBase materialList) {
        return Boolean.TRUE.equals(ACTIVE_LISTS.get(materialList));
    }

    public static void setActive(MaterialListBase materialList, boolean active) {
        clearCache(materialList);
        if (active) {
            ACTIVE_LISTS.put(materialList, true);
        } else {
            ACTIVE_LISTS.remove(materialList);
        }
    }

    public static void cycle(MaterialListBase materialList, boolean forward) {
        BlockInfoListType type = materialList.getMaterialListType();
        boolean active = isActive(materialList);

        if (forward) {
            if (active) {
                setActive(materialList, false);
                materialList.setMaterialListType(BlockInfoListType.ALL);
            } else if (type == BlockInfoListType.ALL) {
                materialList.setMaterialListType(BlockInfoListType.RENDER_LAYERS);
            } else {
                materialList.setMaterialListType(BlockInfoListType.ALL);
                setActive(materialList, true);
            }
        } else {
            if (active) {
                setActive(materialList, false);
                materialList.setMaterialListType(BlockInfoListType.RENDER_LAYERS);
            } else if (type == BlockInfoListType.ALL) {
                setActive(materialList, true);
            } else {
                materialList.setMaterialListType(BlockInfoListType.ALL);
            }
        }
    }

    public static String displayName(MaterialListBase materialList, String original) {
        if (!isActive(materialList)) {
            return original;
        }

        return StringUtils.translate(
                "litematica.gui.button.material_list.list_type",
                StringUtils.translate("lmlp.gui.material_list.display_type.minimal_sub_materials"));
    }

    public static List<MaterialListEntry> entries(MaterialListBase materialList) {
        List<MaterialListEntry> sourceEntries = new ArrayList<>(materialList.getMaterialsFiltered(true));
        String signature = signature(materialList, sourceEntries);
        Cache cache = ENTRY_CACHES.get(materialList);
        if (cache != null && cache.signature().equals(signature)) {
            return cache.entries();
        }

        Map<String, Accumulator> materials = new LinkedHashMap<>();
        boolean multiplied = materialList.getMultiplier() > 1;

        for (MaterialListEntry entry : sourceEntries) {
            class_1799 stack = entry.getStack();
            int total = entry.getCountTotal();
            int missing = multiplied ? total : entry.getCountMissing();
            collectLeaves(stack, List.of(stack), List.of(ItemStackTexts.name(stack)), ItemStackTexts.name(stack), total, missing, 0, new HashSet<>(), materials);
        }

        List<MaterialListEntry> entries = new ArrayList<>(materials.size());
        Map<MaterialListEntry, DisplayData> displays = new IdentityHashMap<>();
        for (Accumulator material : materials.values()) {
            int total = clampToInt(material.totalCount);
            int missing = clampToInt(material.missingCount);
            int available = multiplied ? 0 : Math.max(0, total - missing);
            MaterialListEntry entry = new MaterialListEntry(material.stack.method_7972(), total, missing, 0, available);
            entries.add(entry);
            displays.put(entry, new DisplayData(material.name, material.candidates()));
        }

        clearCache(materialList);
        ENTRY_DISPLAYS.putAll(displays);
        ENTRY_CACHES.put(materialList, new Cache(signature, entries));
        return entries;
    }

    public static void clearCaches() {
        ENTRY_CACHES.keySet().forEach(MinimalSubMaterialListView::removeDisplayData);
        ENTRY_CACHES.clear();
    }

    public static String displayName(MaterialListEntry entry) {
        DisplayData display = ENTRY_DISPLAYS.get(entry);
        return display == null ? ItemStackTexts.name(entry.getStack()) : display.displayName();
    }

    public static String widestDisplayName(MaterialListEntry entry) {
        DisplayData display = ENTRY_DISPLAYS.get(entry);
        return display == null ? ItemStackTexts.name(entry.getStack()) : display.widestName();
    }

    public static class_1799 displayStack(MaterialListEntry entry) {
        DisplayData display = ENTRY_DISPLAYS.get(entry);
        if (display == null || display.candidates().isEmpty()) {
            return entry.getStack();
        }

        return display.currentCandidate().icon().method_7972();
    }

    private static void collectLeaves(class_1799 stack, List<class_1799> icons, List<String> names, String name, int totalCount, int missingCount, int depth, Set<String> seenItems, Map<String, Accumulator> materials) {
        class_1799 icon = stack.method_7972();
        String itemId = ItemStackTexts.id(icon);
        if (totalCount <= 0 && missingCount <= 0) {
            return;
        }

        if (depth >= MAX_RECIPE_DEPTH || seenItems.contains(itemId) || Configs.shouldStopRecipeDecomposition(itemId)) {
            addLeaf(icon, icons, names, name, totalCount, missingCount, materials);
            return;
        }

        Set<String> childSeenItems = new HashSet<>(seenItems);
        childSeenItems.add(itemId);
        List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, totalCount, missingCount);
        if (summaries.isEmpty() || summaries.get(0).ingredients().isEmpty()) {
            addLeaf(icon, icons, names, name, totalCount, missingCount, materials);
            return;
        }

        for (IngredientSummary ingredient : summaries.get(0).ingredients()) {
            List<class_1799> ingredientIcons = ingredient.icons().isEmpty() ? List.of(ingredient.icon()) : ingredient.icons();
            List<String> ingredientNames = candidateNames(ingredientIcons, ingredient.alternatives());
            collectLeaves(
                    ingredient.icon(),
                    ingredientIcons,
                    ingredientNames,
                    RecipeSummaryFormatter.ingredientName(ingredient),
                    ingredient.countTotal(),
                    ingredient.countMissing(),
                    depth + 1,
                    childSeenItems,
                    materials);
        }
    }

    private static void addLeaf(class_1799 stack, List<class_1799> icons, List<String> names, String name, int totalCount, int missingCount, Map<String, Accumulator> materials) {
        String key = groupKey(stack, name);
        materials.computeIfAbsent(key, ignored -> new Accumulator(stack, name))
                .add(totalCount, missingCount, icons, names);
    }

    private static String groupKey(class_1799 stack, String name) {
        String trimmed = name.trim();
        if (!trimmed.isEmpty()) {
            return "name:" + trimmed.toLowerCase(Locale.ROOT);
        }

        return "id:" + ItemStackTexts.id(stack);
    }

    private static List<String> candidateNames(List<class_1799> icons, List<String> alternatives) {
        List<String> names = new ArrayList<>();
        for (String alternative : alternatives) {
            if (alternative != null && !alternative.isBlank()) {
                names.add(alternative);
            }
        }

        if (names.size() == icons.size()) {
            return names;
        }

        names.clear();
        for (class_1799 icon : icons) {
            if (!icon.method_7960()) {
                names.add(ItemStackTexts.name(icon));
            }
        }
        return names.isEmpty() ? List.of() : names;
    }

    private static String signature(MaterialListBase materialList, List<MaterialListEntry> entries) {
        StringBuilder builder = new StringBuilder();
        builder.append(materialList.getMaterialListType().getStringValue())
                .append('|')
                .append(materialList.getHideAvailable())
                .append('|')
                .append(materialList.getMultiplier());
        for (MaterialListEntry entry : entries) {
            builder.append('|')
                    .append(ItemStackTexts.id(entry.getStack()))
                    .append(':')
                    .append(entry.getCountTotal())
                    .append(':')
                    .append(entry.getCountMissing())
                    .append(':')
                    .append(entry.getCountAvailable());
        }
        return builder.toString();
    }

    private static void clearCache(MaterialListBase materialList) {
        removeDisplayData(materialList);
        ENTRY_CACHES.remove(materialList);
    }

    private static void removeDisplayData(MaterialListBase materialList) {
        Cache cache = ENTRY_CACHES.get(materialList);
        if (cache == null) {
            return;
        }

        for (MaterialListEntry entry : cache.entries()) {
            ENTRY_DISPLAYS.remove(entry);
        }
    }

    private static int clampToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    private static final class Accumulator {
        private final class_1799 stack;
        private final String name;
        private final Map<String, Candidate> candidates = new LinkedHashMap<>();
        private long totalCount;
        private long missingCount;

        private Accumulator(class_1799 stack, String name) {
            this.stack = stack.method_7972();
            this.name = name;
            this.candidates.put(ItemStackTexts.id(stack), new Candidate(stack.method_7972(), ItemStackTexts.name(stack)));
        }

        private void add(int totalCount, int missingCount, List<class_1799> icons, List<String> names) {
            this.totalCount += totalCount;
            this.missingCount += missingCount;
            for (int index = 0; index < icons.size(); index++) {
                class_1799 icon = icons.get(index);
                if (!icon.method_7960()) {
                    String name = index < names.size() ? names.get(index) : ItemStackTexts.name(icon);
                    this.candidates.putIfAbsent(ItemStackTexts.id(icon), new Candidate(icon.method_7972(), name));
                }
            }
        }

        private List<Candidate> candidates() {
            return List.copyOf(this.candidates.values());
        }
    }

    private record Cache(String signature, List<MaterialListEntry> entries) {
    }

    private record DisplayData(String name, List<Candidate> candidates) {
        private String displayName() {
            if (this.candidates.size() <= 1) {
                return this.name;
            }

            return this.formatCandidate(this.currentCandidate().name());
        }

        private String widestName() {
            if (this.candidates.size() <= 1) {
                return this.name;
            }

            return this.formatCandidate(this.candidates.stream()
                    .map(Candidate::name)
                    .max(Comparator.comparingInt(String::length))
                    .orElse(this.currentCandidate().name()));
        }

        private Candidate currentCandidate() {
            int index = this.candidates.size() == 1 ? 0 : (int) ((System.currentTimeMillis() / DISPLAY_CYCLE_MS) % this.candidates.size());
            return this.candidates.get(index);
        }

        private String formatCandidate(String candidateName) {
            String highlight = highlightText(candidateName);
            if (highlight.isEmpty() || !candidateName.contains(highlight)) {
                return candidateName;
            }

            int index = "木".equals(highlight) ? candidateName.lastIndexOf(highlight) : candidateName.indexOf(highlight);
            if (index < 0) {
                return candidateName;
            }

            return candidateName.substring(0, index)
                    + COMMON_HIGHLIGHT
                    + highlight
                    + RESET
                    + candidateName.substring(index + highlight.length());
        }

        private String highlightText(String candidateName) {
            String common = commonText();
            if (candidateName.contains("原木") && this.candidates.stream().map(Candidate::name).anyMatch(name -> name.contains("原木"))) {
                return "原木";
            }

            if (candidateName.contains("木") && common.contains("木")) {
                return "木";
            }

            return common;
        }

        private String commonText() {
            String common = longestCommonSubstring(this.candidates.stream()
                    .map(Candidate::name)
                    .toList());
            return common.trim();
        }

    }

    private record Candidate(class_1799 icon, String name) {
    }

    private static String longestCommonSubstring(List<String> values) {
        if (values.size() <= 1) {
            return "";
        }

        String shortest = values.stream()
                .min(Comparator.comparingInt(String::length))
                .orElse("");
        String best = "";
        for (int start = 0; start < shortest.length(); start++) {
            for (int end = start + 1; end <= shortest.length(); end++) {
                String candidate = shortest.substring(start, end);
                if (candidate.length() <= best.length() || candidate.isBlank()) {
                    continue;
                }

                boolean present = true;
                for (String value : values) {
                    if (!value.contains(candidate)) {
                        present = false;
                        break;
                    }
                }
                if (present) {
                    best = candidate;
                }
            }
        }
        return best;
    }
}
