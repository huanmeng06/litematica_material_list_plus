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
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public final class MinimalSubMaterialListView {
    private static final int MAX_RECIPE_DEPTH = 16;
    private static final long DISPLAY_CYCLE_MS = 900L;
    private static final long BUILD_BUDGET_NS = 2_500_000L;
    private static final long INITIAL_BUILD_BUDGET_NS = 20_000_000L;
    private static final String COMMON_HIGHLIGHT = "\u00A7e\u00A7l\u00A7n";
    private static final String RESET = "\u00A7r";
    private static final Map<MaterialListBase, Boolean> ACTIVE_LISTS = new WeakHashMap<>();
    private static final Map<MaterialListBase, Cache> ENTRY_CACHES = new WeakHashMap<>();
    private static final Map<MaterialListBase, BuildState> BUILD_STATES = new WeakHashMap<>();
    private static final Map<MaterialListEntry, DisplayData> ENTRY_DISPLAYS = new IdentityHashMap<>();
    private static final Map<String, DisplayData> ENTRY_DISPLAY_KEYS = new HashMap<>();
    private static long layoutRevision;

    private MinimalSubMaterialListView() {
    }

    public static boolean isActive(MaterialListBase materialList) {
        return Boolean.TRUE.equals(ACTIVE_LISTS.get(materialList));
    }

    public static void setActive(MaterialListBase materialList, boolean active) {
        if (active) {
            ACTIVE_LISTS.put(materialList, true);
            prepare(materialList);
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

        BuildState state = buildState(materialList, signature, sourceEntries, false);

        state.process(materialList, BUILD_BUDGET_NS);
        if (state.isComplete()) {
            storeCache(materialList, signature, state.entries());
            BUILD_STATES.remove(materialList);
            return state.entries();
        }

        if (cache != null) {
            return cache.entries();
        }
        return state.entries();
    }

    public static void prepare(MaterialListBase materialList) {
        List<MaterialListEntry> sourceEntries = new ArrayList<>(materialList.getMaterialsFiltered(true));
        String signature = signature(materialList, sourceEntries);
        Cache cache = ENTRY_CACHES.get(materialList);
        if (cache != null && cache.signature().equals(signature)) {
            return;
        }

        BuildState state = buildState(materialList, signature, sourceEntries, true);
        if (state.isComplete()) {
            storeCache(materialList, signature, state.entries());
            BUILD_STATES.remove(materialList);
        }
    }

    public static void clearCaches() {
        ENTRY_CACHES.keySet().forEach(MinimalSubMaterialListView::removeDisplayData);
        ENTRY_CACHES.clear();
        BUILD_STATES.keySet().forEach(MinimalSubMaterialListView::removeBuildDisplayData);
        BUILD_STATES.clear();
        ENTRY_DISPLAY_KEYS.clear();
        layoutRevision++;
    }

    public static boolean tick(MaterialListBase materialList) {
        if (!isActive(materialList)) {
            return false;
        }

        BuildState state = BUILD_STATES.get(materialList);
        if (state == null) {
            return false;
        }

        state.process(materialList, BUILD_BUDGET_NS);
        boolean complete = state.isComplete();
        if (complete) {
            storeCache(materialList, state.signature(), state.entries());
            BUILD_STATES.remove(materialList);
        }
        return complete;
    }

    public static String displayName(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        return display == null ? ItemStackTexts.name(entry.getStack()) : display.displayName();
    }

    public static String widestDisplayName(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        return display == null ? ItemStackTexts.name(entry.getStack()) : display.widestName();
    }

    public static class_1799 displayStack(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        if (display == null || display.candidates().isEmpty()) {
            return entry.getStack();
        }

        return display.currentCandidate().icon().method_7972();
    }

    public static List<class_1799> displayStacks(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        if (display == null || display.candidates().isEmpty()) {
            return List.of(entry.getStack());
        }

        List<class_1799> stacks = new ArrayList<>(display.candidates().size());
        for (Candidate candidate : display.candidates()) {
            stacks.add(candidate.icon().method_7972());
        }
        return stacks;
    }

    public static long layoutRevision() {
        return layoutRevision;
    }

    private static DisplayData displayData(MaterialListEntry entry) {
        DisplayData display = ENTRY_DISPLAYS.get(entry);
        return display == null ? ENTRY_DISPLAY_KEYS.get(entryKey(entry)) : display;
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
        String key = groupKey(stack, icons, name);
        String displayName = groupDisplayName(icons.isEmpty() ? List.of(stack) : icons, name);
        materials.computeIfAbsent(key, ignored -> new Accumulator(stack, displayName))
                .add(totalCount, missingCount, icons, names);
    }

    private static String groupKey(class_1799 stack, List<class_1799> icons, String name) {
        String alternativeKey = knownAlternativeTranslationKey(icons.isEmpty() ? List.of(stack) : icons);
        if (!alternativeKey.isEmpty()) {
            return "group:" + alternativeKey;
        }

        String trimmed = name.trim();
        if (!trimmed.isEmpty()) {
            return "name:" + trimmed.toLowerCase(Locale.ROOT);
        }

        return "id:" + ItemStackTexts.id(stack);
    }

    private static String groupDisplayName(List<class_1799> icons, String fallback) {
        String alternativeKey = knownAlternativeTranslationKey(icons);
        if (!alternativeKey.isEmpty()) {
            return StringUtils.translate(alternativeKey);
        }

        return fallback;
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
        removeBuildDisplayData(materialList);
        ENTRY_CACHES.remove(materialList);
        BUILD_STATES.remove(materialList);
        layoutRevision++;
    }

    private static BuildState buildState(MaterialListBase materialList, String signature, List<MaterialListEntry> sourceEntries, boolean useInitialBudget) {
        BuildState state = BUILD_STATES.get(materialList);
        if (state == null || !state.signature().equals(signature)) {
            removeBuildDisplayData(materialList);
            state = new BuildState(signature, sourceEntries, materialList.getMultiplier() > 1);
            BUILD_STATES.put(materialList, state);
            if (useInitialBudget) {
                state.process(materialList, INITIAL_BUILD_BUDGET_NS);
            }
        }
        return state;
    }

    private static void storeCache(MaterialListBase materialList, String signature, List<MaterialListEntry> entries) {
        ENTRY_CACHES.put(materialList, new Cache(signature, entries));
        layoutRevision++;
    }

    private static void removeDisplayData(MaterialListBase materialList) {
        Cache cache = ENTRY_CACHES.get(materialList);
        if (cache == null) {
            return;
        }

        for (MaterialListEntry entry : cache.entries()) {
            ENTRY_DISPLAYS.remove(entry);
            ENTRY_DISPLAY_KEYS.remove(entryKey(entry));
        }
    }

    private static void removeBuildDisplayData(MaterialListBase materialList) {
        BuildState state = BUILD_STATES.get(materialList);
        if (state == null) {
            return;
        }

        for (MaterialListEntry entry : state.entries()) {
            ENTRY_DISPLAYS.remove(entry);
            ENTRY_DISPLAY_KEYS.remove(entryKey(entry));
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

    private static final class BuildState {
        private final String signature;
        private final List<MaterialListEntry> sourceEntries;
        private final boolean multiplied;
        private final Map<String, Accumulator> materials = new LinkedHashMap<>();
        private List<MaterialListEntry> entries = List.of();
        private int nextSourceIndex;
        private boolean complete;

        private BuildState(String signature, List<MaterialListEntry> sourceEntries, boolean multiplied) {
            this.signature = signature;
            this.sourceEntries = List.copyOf(sourceEntries);
            this.multiplied = multiplied;
        }

        private String signature() {
            return this.signature;
        }

        private List<MaterialListEntry> entries() {
            return this.entries;
        }

        private boolean isComplete() {
            return this.complete;
        }

        private boolean process(MaterialListBase materialList, long budgetNs) {
            if (this.complete) {
                return false;
            }

            long deadline = System.nanoTime() + budgetNs;
            boolean changed = false;
            do {
                if (this.nextSourceIndex >= this.sourceEntries.size()) {
                    this.complete = true;
                    break;
                }

                MaterialListEntry entry = this.sourceEntries.get(this.nextSourceIndex++);
                class_1799 stack = entry.getStack();
                int total = entry.getCountTotal();
                int missing = this.multiplied ? total : entry.getCountMissing();
                collectLeaves(stack, List.of(stack), List.of(ItemStackTexts.name(stack)), ItemStackTexts.name(stack), total, missing, 0, new HashSet<>(), this.materials);
                changed = true;
            } while (System.nanoTime() < deadline);

            if (this.complete) {
                this.publish(materialList);
            }
            return changed;
        }

        private void publish(MaterialListBase materialList) {
            removeDisplayData(materialList);
            removeBuildDisplayData(materialList);
            List<MaterialListEntry> entries = new ArrayList<>(this.materials.size());
            Map<MaterialListEntry, DisplayData> displays = new IdentityHashMap<>();
            for (Accumulator material : this.materials.values()) {
                int total = clampToInt(material.totalCount);
                int missing = clampToInt(material.missingCount);
                int available = this.multiplied ? 0 : Math.max(0, total - missing);
                MaterialListEntry entry = new MaterialListEntry(material.stack.method_7972(), total, missing, 0, available);
                entries.add(entry);
                DisplayData display = new DisplayData(material.name, material.candidates());
                displays.put(entry, display);
                ENTRY_DISPLAY_KEYS.put(entryKey(entry), display);
            }

            this.entries = entries;
            ENTRY_DISPLAYS.putAll(displays);
        }
    }

    private record Cache(String signature, List<MaterialListEntry> entries) {
    }

    private record DisplayData(String name, List<Candidate> candidates) {
        private String displayName() {
            return emphasizeAny(this.stableName());
        }

        private String widestName() {
            return this.displayName();
        }

        private Candidate currentCandidate() {
            int index = this.candidates.size() == 1 ? 0 : (int) ((System.currentTimeMillis() / DISPLAY_CYCLE_MS) % this.candidates.size());
            return this.candidates.get(index);
        }

        private String stableName() {
            if (this.candidates.size() > 1) {
                String knownName = knownAlternativeName();
                if (!knownName.isEmpty()) {
                    return knownName;
                }
            }

            String trimmed = this.name.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }

            return this.candidates.isEmpty() ? "" : this.candidates.get(0).name();
        }

        private String knownAlternativeName() {
            List<class_1799> icons = new ArrayList<>(this.candidates.size());
            for (Candidate candidate : this.candidates) {
                icons.add(candidate.icon());
            }

            return groupDisplayName(icons, "");
        }
    }

    private record Candidate(class_1799 icon, String name) {
    }

    private static String itemPath(class_1799 stack) {
        String id = ItemStackTexts.id(stack);
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private static String entryKey(MaterialListEntry entry) {
        return ItemStackTexts.id(entry.getStack())
                + ':'
                + entry.getCountTotal()
                + ':'
                + entry.getCountMissing()
                + ':'
                + entry.getCountAvailable();
    }

    private static String knownAlternativeTranslationKey(List<class_1799> icons) {
        if (icons.size() < 2) {
            return "";
        }
        if (allIconsMatch(icons, MinimalSubMaterialListView::isLogLike)) {
            return "lmlp.label.recipe.any.log";
        }
        if (allIconsMatch(icons, path -> path.equals("sand") || path.equals("red_sand"))) {
            return "lmlp.label.recipe.any.sand";
        }
        if (allIconsMatch(icons, path -> path.contains("quartz"))) {
            return "lmlp.label.recipe.any.quartz";
        }
        if (allIconsMatch(icons, MinimalSubMaterialListView::isCobblestoneLike)) {
            return "lmlp.label.recipe.any.cobblestone";
        }
        return "";
    }

    private static boolean allIconsMatch(List<class_1799> icons, Predicate<String> predicate) {
        if (icons.isEmpty()) {
            return false;
        }

        for (class_1799 icon : icons) {
            if (icon.method_7960() || !predicate.test(itemPath(icon))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isLogLike(String path) {
        return path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_stem")
                || path.endsWith("_hyphae");
    }

    private static boolean isCobblestoneLike(String path) {
        return path.contains("cobblestone")
                || path.equals("cobbled_deepslate")
                || path.equals("blackstone");
    }

    private static String emphasizeAny(String name) {
        if (name.startsWith("任意")) {
            return COMMON_HIGHLIGHT + "任意" + RESET + name.substring("任意".length());
        }
        if (name.startsWith("Any ")) {
            return COMMON_HIGHLIGHT + "Any" + RESET + name.substring("Any".length());
        }
        return name;
    }
}
