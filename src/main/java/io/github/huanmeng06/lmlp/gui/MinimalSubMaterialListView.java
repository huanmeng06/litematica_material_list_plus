package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.InventoryCounts;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.material.MaterialCounts;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeResolvers;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import net.minecraft.class_1799;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
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
    private static final Logger LOGGER = LoggerFactory.getLogger("LMLP MinimalSubMaterialListView");
    private static final int MAX_RECIPE_DEPTH = 16;
    private static final long DISPLAY_CYCLE_MS = 900L;
    private static final long BUILD_BUDGET_NS = 2_500_000L;
    private static final long INITIAL_BUILD_BUDGET_NS = 20_000_000L;
    private static final String COMMON_HIGHLIGHT = "\u00A7e\u00A7l\u00A7n";
    private static final String RESET = "\u00A7r";
    private static final List<String> WOOD_FAMILIES = List.of(
            "dark_oak",
            "pale_oak",
            "oak",
            "spruce",
            "birch",
            "jungle",
            "acacia",
            "mangrove",
            "cherry",
            "bamboo",
            "crimson",
            "warped");
    private static final Map<MaterialListBase, Boolean> ACTIVE_LISTS = new WeakHashMap<>();
    private static final Map<MaterialListBase, Cache> ENTRY_CACHES = new WeakHashMap<>();
    private static final Map<MaterialListBase, BuildState> BUILD_STATES = new WeakHashMap<>();
    private static final Map<MaterialListEntry, DisplayData> ENTRY_DISPLAYS = new IdentityHashMap<>();
    private static final Map<String, DisplayData> ENTRY_DISPLAY_KEYS = new HashMap<>();
    private static final Map<String, List<RequirementContribution>> REQUIREMENT_CACHES = new HashMap<>();
    private static final Map<String, Set<String>> IGNORED_ENTRY_KEYS_BY_PLACEMENT = new LinkedHashMap<>();
    private static final ExpandAnimationTracker SOURCE_ANIMATIONS = new ExpandAnimationTracker();
    private static String expandedSourceKey = "";
    private static String visibleSourceKey = "";
    private static String fullSourceKey = "";
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
        InventoryCounts.Snapshot inventory = InventoryCounts.current();
        String signature = signature(materialList, sourceEntries, inventory);
        Cache cache = ENTRY_CACHES.get(materialList);
        if (cache != null && cache.signature().equals(signature)) {
            return cache.entries();
        }

        BuildState state = buildState(materialList, signature, sourceEntries, inventory, false);

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
        InventoryCounts.Snapshot inventory = InventoryCounts.current();
        String signature = signature(materialList, sourceEntries, inventory);
        Cache cache = ENTRY_CACHES.get(materialList);
        if (cache != null && cache.signature().equals(signature)) {
            return;
        }

        BuildState state = buildState(materialList, signature, sourceEntries, inventory, true);
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
        REQUIREMENT_CACHES.clear();
        layoutRevision++;
    }

    public static boolean ignoreEntry(MaterialListBase materialList, MaterialListEntry entry) {
        return ignoreEntry(materialList, entry, "unknown", false);
    }

    public static boolean ignoreEntry(MaterialListBase materialList, MaterialListEntry entry, String clickPath, boolean clickedHitbox) {
        String placementKey = placementKey(materialList);
        int beforeSize = ignoredSize(placementKey);
        String key = stableEntryKey(entry);
        if (!isMinimalEntry(entry)) {
            LOGGER.info("[minimal ignore] placementKey={} subMaterialKey={} beforeIgnoredSize={} afterIgnoredSize={}",
                    placementKey, key, beforeSize, beforeSize);
            return false;
        }

        if (key.isEmpty()) {
            LOGGER.info("[minimal ignore] placementKey={} subMaterialKey={} beforeIgnoredSize={} afterIgnoredSize={}",
                    placementKey, key, beforeSize, beforeSize);
            return false;
        }

        if (placementKey.isEmpty()) {
            LOGGER.info("[minimal ignore] placementKey={} subMaterialKey={} beforeIgnoredSize={} afterIgnoredSize={}",
                    placementKey, key, beforeSize, beforeSize);
            return false;
        }

        boolean changed = IGNORED_ENTRY_KEYS_BY_PLACEMENT.computeIfAbsent(placementKey, ignored -> new HashSet<>()).add(key);
        if (changed) {
            clearCaches();
        }
        LOGGER.info("[minimal ignore] placementKey={} subMaterialKey={} beforeIgnoredSize={} afterIgnoredSize={}",
                placementKey, key, beforeSize, ignoredSize(placementKey));
        return changed;
    }

    public static void clearIgnored(MaterialListBase materialList) {
        String placementKey = placementKey(materialList);
        int beforeSize = ignoredSize(placementKey);
        Set<String> ignored = placementKey.isEmpty() ? null : IGNORED_ENTRY_KEYS_BY_PLACEMENT.remove(placementKey);
        if (ignored != null && !ignored.isEmpty()) {
            clearCaches();
        }
        LOGGER.info("[minimal clear] placementKey={} beforeIgnoredSize={} afterIgnoredSize={}",
                placementKey, beforeSize, ignoredSize(placementKey));
    }

    public static String debugStableKey(MaterialListEntry entry) {
        return stableEntryKey(entry);
    }

    public static int ignoredSize(MaterialListBase materialList) {
        return ignoredSize(placementKey(materialList));
    }

    private static int ignoredSize(String placementKey) {
        Set<String> ignored = ignoredSet(placementKey);
        return ignored == null ? 0 : ignored.size();
    }

    private static Set<String> ignoredSet(String placementKey) {
        return placementKey == null || placementKey.isEmpty() ? null : IGNORED_ENTRY_KEYS_BY_PLACEMENT.get(placementKey);
    }

    private static String placementKey(MaterialListBase materialList) {
        return ChunkMissingMaterialListCache.materialListContextKey(materialList, "minimal_sub_material");
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

    public static List<TooltipCandidate> tooltipCandidates(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        if (display == null || display.candidates().size() < 2) {
            return List.of();
        }

        List<TooltipCandidate> candidates = new ArrayList<>(display.candidates().size());
        for (Candidate candidate : display.candidates()) {
            candidates.add(new TooltipCandidate(candidate.icon().method_7972(), candidate.name()));
        }
        return List.copyOf(candidates);
    }

    public static List<SourceContribution> sourceContributions(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        return display == null ? List.of() : display.sources();
    }

    public static List<RequirementContribution> sourceRequirements(MaterialListEntry entry, int totalCount, int missingCount) {
        DisplayData display = displayData(entry);
        if (display == null || !display.hasRequirementSection()) {
            return List.of();
        }

        String targetName = display.stableName();
        boolean targetPlanksGroup = allCandidatesMatch(display.candidates(), path -> isPlanksLike(path))
                || targetName.equals(StringUtils.translate("lmlp.label.recipe.any.planks"));
        String key = entryKey(entry) + '|' + targetName + '|' + targetPlanksGroup + '|' + totalCount + '|' + missingCount;
        return REQUIREMENT_CACHES.computeIfAbsent(key, ignored -> buildRequirements(targetPlanksGroup, display.candidates(), totalCount, missingCount));
    }

    public static String requirementDisplayName(RequirementContribution requirement) {
        return requirement.icons().size() > 1 && isAnyGroupName(requirement.name()) ? emphasizeVariable(requirement.name()) : requirement.name();
    }

    public static String upstreamDisplayName(UpstreamRequirement upstream) {
        return upstream.icons().size() > 1 && isAnyGroupName(upstream.name()) ? emphasizeVariable(upstream.name()) : upstream.name();
    }

    public static List<TooltipCandidate> requirementTooltipCandidates(RequirementContribution requirement) {
        if (requirement.icons().size() < 2) {
            return List.of();
        }

        List<TooltipCandidate> candidates = new ArrayList<>(requirement.icons().size());
        for (int index = 0; index < requirement.icons().size(); index++) {
            class_1799 icon = requirement.icons().get(index);
            String name = index < requirement.candidateNames().size() ? requirement.candidateNames().get(index) : ItemStackTexts.name(icon);
            candidates.add(new TooltipCandidate(icon.method_7972(), name));
        }
        return List.copyOf(candidates);
    }

    public static List<TooltipCandidate> upstreamTooltipCandidates(UpstreamRequirement upstream) {
        if (upstream == null || upstream.icons().size() < 2) {
            return List.of();
        }

        List<TooltipCandidate> candidates = new ArrayList<>(upstream.icons().size());
        for (int index = 0; index < upstream.icons().size(); index++) {
            class_1799 icon = upstream.icons().get(index);
            String name = index < upstream.candidateNames().size() ? upstream.candidateNames().get(index) : ItemStackTexts.name(icon);
            candidates.add(new TooltipCandidate(icon.method_7972(), name));
        }
        return List.copyOf(candidates);
    }

    public static boolean isSourcesExpanded(MaterialListEntry entry) {
        return !expandedSourceKey.isEmpty() && expandedSourceKey.equals(entryKey(entry));
    }

    public static boolean isSourcesVisible(MaterialListEntry entry) {
        return !visibleSourceKey.isEmpty() && visibleSourceKey.equals(entryKey(entry));
    }

    public static boolean isSourcesFull(MaterialListEntry entry) {
        return !fullSourceKey.isEmpty() && fullSourceKey.equals(entryKey(entry));
    }

    public static void toggleSources(MaterialListEntry entry, boolean showAll) {
        if (isSourcesExpanded(entry) && isSourcesFull(entry) == showAll) {
            clearSources(entry);
        } else {
            openSources(entry, showAll);
        }
    }

    public static float sourceProgress(MaterialListEntry entry) {
        if (!isSourcesVisible(entry)) {
            return 0.0F;
        }

        return SOURCE_ANIMATIONS.progress(visibleSourceKey, isSourcesExpanded(entry));
    }

    public static boolean hasActiveSourceAnimations() {
        return SOURCE_ANIMATIONS.isActive();
    }

    public static void pruneSourceAnimations() {
        boolean hadAnimation = SOURCE_ANIMATIONS.isActive();
        SOURCE_ANIMATIONS.prune();
        if (hadAnimation && !visibleSourceKey.isEmpty() && !visibleSourceKey.equals(expandedSourceKey)) {
            float progress = SOURCE_ANIMATIONS.progress(visibleSourceKey, false);
            if (progress <= 0.0F) {
                visibleSourceKey = "";
                SOURCE_ANIMATIONS.clear();
            }
        }
    }

    public static long layoutRevision() {
        return layoutRevision;
    }

    public static boolean isMinimalEntry(MaterialListEntry entry) {
        return ENTRY_DISPLAYS.containsKey(entry);
    }

    public static int total(MaterialListEntry entry, MaterialListBase materialList) {
        return isActive(materialList) && isMinimalEntry(entry) ? entry.getCountTotal() : MaterialCounts.total(entry, materialList);
    }

    public static int missing(MaterialListEntry entry, MaterialListBase materialList) {
        return isActive(materialList) && isMinimalEntry(entry) ? entry.getCountMissing() : MaterialCounts.missing(entry, materialList);
    }

    public static int netMissing(MaterialListEntry entry, MaterialListBase materialList) {
        if (isActive(materialList) && isMinimalEntry(entry)) {
            return Math.max(0, entry.getCountMissing() - entry.getCountAvailable());
        }

        return MaterialCounts.netMissing(entry, materialList);
    }

    public static int netMissing(MaterialListEntry entry, int multiplier) {
        return isMinimalEntry(entry) ? Math.max(0, entry.getCountMissing() - entry.getCountAvailable()) : MaterialCounts.netMissing(entry, multiplier);
    }

    private static DisplayData displayData(MaterialListEntry entry) {
        DisplayData display = ENTRY_DISPLAYS.get(entry);
        return display == null ? ENTRY_DISPLAY_KEYS.get(entryKey(entry)) : display;
    }

    private static void openSources(MaterialListEntry entry, boolean showAll) {
        String key = entryKey(entry);
        float startProgress = visibleSourceKey.equals(key) ? sourceProgress(entry) : 0.0F;
        expandedSourceKey = key;
        visibleSourceKey = key;
        fullSourceKey = showAll ? key : "";
        SOURCE_ANIMATIONS.start(key, startProgress, 1.0F);
    }

    private static void clearSources(MaterialListEntry entry) {
        String key = entryKey(entry);
        if (!expandedSourceKey.equals(key)) {
            return;
        }

        visibleSourceKey = expandedSourceKey;
        SOURCE_ANIMATIONS.start(visibleSourceKey, sourceProgress(entry), 0.0F);
        expandedSourceKey = "";
    }

    private static void collectLeaves(class_1799 stack, List<class_1799> icons, List<String> names, String name, SourceOrigin source, int count, int scale, boolean prepared, int depth, Set<String> seenItems, Map<String, Accumulator> materials) {
        class_1799 icon = stack.method_7972();
        String itemId = ItemStackTexts.id(icon);
        if (count <= 0 || scale <= 0) {
            return;
        }

        if (depth >= MAX_RECIPE_DEPTH || seenItems.contains(itemId) || Configs.shouldStopRecipeDecomposition(itemId)) {
            addLeaf(icon, icons, names, name, source, scaledCount(count, scale), prepared, materials);
            return;
        }

        Set<String> childSeenItems = new HashSet<>(seenItems);
        childSeenItems.add(itemId);
        List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, count, count);
        if (summaries.isEmpty() || summaries.get(0).ingredients().isEmpty()) {
            addLeaf(icon, icons, names, name, source, scaledCount(count, scale), prepared, materials);
            return;
        }

        for (IngredientSummary ingredient : summaries.get(0).ingredients()) {
            List<class_1799> ingredientIcons = ingredient.icons().isEmpty() ? List.of(ingredient.icon()) : ingredient.icons();
            List<String> ingredientNames = candidateNames(ingredientIcons, ingredient.alternatives());
            if (ingredient.isChoiceGroup()) {
                CandidateSet refinedChoice = refineWoodCandidatesForSource(source, ingredientIcons, ingredientNames);
                if (shouldDecomposeRefinedChoice(refinedChoice)) {
                    collectLeaves(
                            refinedChoice.icons().get(0),
                            refinedChoice.icons(),
                            refinedChoice.names(),
                            refinedChoice.names().isEmpty() ? RecipeSummaryFormatter.ingredientName(ingredient) : refinedChoice.names().get(0),
                            source,
                            ingredient.countTotal(),
                            scale,
                            prepared,
                            depth + 1,
                            childSeenItems,
                            materials);
                    continue;
                }

                addLeaf(
                        ingredient.icon(),
                        ingredientIcons,
                        ingredientNames,
                        RecipeSummaryFormatter.ingredientName(ingredient),
                        source,
                        scaledCount(ingredient.countTotal(), scale),
                        prepared,
                        materials);
                continue;
            }

            collectLeaves(
                    ingredient.icon(),
                    ingredientIcons,
                    ingredientNames,
                    RecipeSummaryFormatter.ingredientName(ingredient),
                    source,
                    ingredient.countTotal(),
                    scale,
                    prepared,
                    depth + 1,
                    childSeenItems,
                    materials);
        }
    }

    private static void collectPreparedLeaves(class_1799 stack, List<class_1799> icons, List<String> names, String name, SourceOrigin source, int baseCount, int preparedCount, Map<String, Accumulator> materials) {
        if (preparedCount <= 0) {
            return;
        }

        if (baseCount <= 0) {
            collectLeaves(stack, icons, names, name, source, preparedCount, 1, true, 0, new HashSet<>(), materials);
            return;
        }

        int fullCopies = preparedCount / baseCount;
        int remainder = preparedCount % baseCount;
        if (fullCopies > 0) {
            collectLeaves(stack, icons, names, name, source, baseCount, fullCopies, true, 0, new HashSet<>(), materials);
        }
        if (remainder > 0) {
            collectLeaves(stack, icons, names, name, source, remainder, 1, true, 0, new HashSet<>(), materials);
        }
    }

    private static void addLeaf(class_1799 stack, List<class_1799> icons, List<String> names, String name, SourceOrigin source, int count, boolean prepared, Map<String, Accumulator> materials) {
        CandidateSet candidates = refineWoodCandidatesForSource(source, icons.isEmpty() ? List.of(stack) : icons, names);
        String displayNameFallback = candidates.names().size() == 1 ? candidates.names().get(0) : name;
        class_1799 displayStack = candidates.icons().isEmpty() ? stack : candidates.icons().get(0);
        String key = groupKey(displayStack, candidates.icons(), displayNameFallback);
        String displayName = groupDisplayName(candidates.icons(), displayNameFallback);
        materials.computeIfAbsent(key, ignored -> new Accumulator(displayStack, displayName))
                .add(count, prepared, candidates.icons(), candidates.names(), source);
    }

    private static boolean shouldDecomposeRefinedChoice(CandidateSet candidates) {
        return candidates.icons().size() == 1 && isPlanksLike(itemPath(candidates.icons().get(0)));
    }

    private static CandidateSet refineWoodCandidatesForSource(SourceOrigin source, List<class_1799> icons, List<String> names) {
        if (icons.size() < 2) {
            return new CandidateSet(icons, names);
        }

        String sourceFamily = woodFamily(itemPath(source.id()));
        if (sourceFamily.isEmpty() || !allIconsMatch(icons, path -> isPlanksLike(path) || isLogLike(path))) {
            return new CandidateSet(icons, names);
        }

        List<class_1799> refinedIcons = new ArrayList<>();
        List<String> refinedNames = new ArrayList<>();
        for (int index = 0; index < icons.size(); index++) {
            class_1799 icon = icons.get(index);
            if (sourceFamily.equals(woodFamily(itemPath(icon)))) {
                refinedIcons.add(icon);
                refinedNames.add(index < names.size() ? names.get(index) : ItemStackTexts.name(icon));
            }
        }

        return refinedIcons.isEmpty() ? new CandidateSet(icons, names) : new CandidateSet(List.copyOf(refinedIcons), List.copyOf(refinedNames));
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

    private static String signature(MaterialListBase materialList, List<MaterialListEntry> entries, InventoryCounts.Snapshot inventory) {
        String placementKey = placementKey(materialList);
        StringBuilder builder = new StringBuilder();
        builder.append(materialList.getMaterialListType().getStringValue())
                .append('|')
                .append(StringUtils.translate("litematica.gui.label.material_list.title.item"))
                .append('|')
                .append(materialList.getHideAvailable())
                .append('|')
                .append(materialList.getMultiplier())
                .append('|')
                .append(inventory.signature())
                .append('|')
                .append("placement:")
                .append(placementKey);
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
        Set<String> ignored = ignoredSet(placementKey);
        if (ignored != null && !ignored.isEmpty()) {
            ignored.stream().sorted().forEach(key -> builder.append("|ignored:").append(key));
        }
        return builder.toString();
    }

    private static void clearCache(MaterialListBase materialList) {
        removeDisplayData(materialList);
        removeBuildDisplayData(materialList);
        ENTRY_CACHES.remove(materialList);
        BUILD_STATES.remove(materialList);
        REQUIREMENT_CACHES.clear();
        clearSourceState();
        layoutRevision++;
    }

    private static List<RequirementContribution> buildRequirements(boolean targetPlanksGroup, List<Candidate> candidates, int totalCount, int missingCount) {
        List<List<IngredientSummary>> recipeIngredients = new ArrayList<>();
        for (Candidate candidate : candidates) {
            List<RecipeSummary> summaries = RecipeResolvers.findRecipes(candidate.icon(), totalCount, missingCount);
            if (!summaries.isEmpty() && !summaries.get(0).ingredients().isEmpty()) {
                recipeIngredients.add(summaries.get(0).ingredients());
            }
        }

        if (recipeIngredients.isEmpty()) {
            return List.of();
        }

        List<IngredientSummary> baseIngredients = recipeIngredients.get(0);
        List<RequirementContribution> requirements = new ArrayList<>();
        for (int index = 0; index < baseIngredients.size(); index++) {
            IngredientSummary base = baseIngredients.get(index);
            Map<String, class_1799> iconsById = new LinkedHashMap<>();
            List<String> names = new ArrayList<>();

            for (List<IngredientSummary> ingredients : recipeIngredients) {
                if (index >= ingredients.size()) {
                    continue;
                }

                IngredientSummary ingredient = ingredients.get(index);
                List<class_1799> icons = ingredient.icons().isEmpty() ? List.of(ingredient.icon()) : ingredient.icons();
                for (class_1799 icon : icons) {
                    if (!icon.method_7960()) {
                        iconsById.putIfAbsent(ItemStackTexts.id(icon), icon.method_7972());
                    }
                }
                for (String alternative : ingredient.alternatives()) {
                    if (!alternative.isBlank() && !names.contains(alternative)) {
                        names.add(alternative);
                    }
                }
            }

            if (iconsById.isEmpty()) {
                continue;
            }

            List<class_1799> icons = List.copyOf(iconsById.values());
            List<String> candidateNames = candidateNames(icons, names);
            String fallbackName = candidateNames.isEmpty() ? RecipeSummaryFormatter.ingredientName(base) : candidateNames.get(0);
            String name = requirementGroupName(targetPlanksGroup, icons, fallbackName);
            List<class_1799> copiedIcons = icons.stream().map(class_1799::method_7972).toList();
            requirements.add(new RequirementContribution(
                    icons.get(0).method_7972(),
                    copiedIcons,
                    candidateNames,
                    name,
                    base.countTotal(),
                    base.countMissing(),
                    base.maxStackSize(),
                    upstreamRequirement(copiedIcons, base.countTotal(), base.countMissing())));
        }

        return List.copyOf(requirements);
    }

    private static UpstreamRequirement upstreamRequirement(List<class_1799> icons, int totalCount, int missingCount) {
        if (!allIconsMatch(icons, MinimalSubMaterialListView::isPlanksLike)) {
            return null;
        }

        Map<String, class_1799> iconsById = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        IngredientSummary firstBase = null;
        for (class_1799 icon : icons) {
            List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, totalCount, missingCount);
            if (summaries.isEmpty()) {
                continue;
            }

            IngredientSummary ingredient = firstLogIngredient(summaries.get(0).ingredients());
            if (ingredient == null) {
                continue;
            }

            if (firstBase == null) {
                firstBase = ingredient;
            }

            List<class_1799> ingredientIcons = ingredient.icons().isEmpty() ? List.of(ingredient.icon()) : ingredient.icons();
            for (class_1799 ingredientIcon : ingredientIcons) {
                if (!ingredientIcon.method_7960()) {
                    iconsById.putIfAbsent(ItemStackTexts.id(ingredientIcon), ingredientIcon.method_7972());
                }
            }
            for (String alternative : ingredient.alternatives()) {
                if (!alternative.isBlank() && !names.contains(alternative)) {
                    names.add(alternative);
                }
            }
        }

        if (firstBase == null || iconsById.isEmpty()) {
            return null;
        }

        List<class_1799> upstreamIcons = List.copyOf(iconsById.values());
        if (!allIconsMatch(upstreamIcons, MinimalSubMaterialListView::isLogLike)) {
            return null;
        }

        List<String> candidateNames = candidateNames(upstreamIcons, names);
        String name = StringUtils.translate("lmlp.label.recipe.any.log");
        return new UpstreamRequirement(
                upstreamIcons.get(0).method_7972(),
                upstreamIcons.stream().map(class_1799::method_7972).toList(),
                candidateNames,
                name,
                firstBase.countTotal(),
                firstBase.countMissing(),
                firstBase.maxStackSize());
    }

    private static IngredientSummary firstLogIngredient(List<IngredientSummary> ingredients) {
        for (IngredientSummary ingredient : ingredients) {
            List<class_1799> icons = ingredient.icons().isEmpty() ? List.of(ingredient.icon()) : ingredient.icons();
            if (allIconsMatch(icons, MinimalSubMaterialListView::isLogLike)) {
                return ingredient;
            }
        }

        return null;
    }

    private static String requirementGroupName(boolean targetPlanksGroup, List<class_1799> icons, String fallbackName) {
        if (targetPlanksGroup && allIconsMatch(icons, MinimalSubMaterialListView::isLogLike)) {
            return StringUtils.translate("lmlp.label.recipe.any.log");
        }
        if (allIconsMatch(icons, MinimalSubMaterialListView::isPlanksLike)) {
            return StringUtils.translate("lmlp.label.recipe.any.planks");
        }

        return groupDisplayName(icons, fallbackName);
    }

    private static BuildState buildState(MaterialListBase materialList, String signature, List<MaterialListEntry> sourceEntries, InventoryCounts.Snapshot inventory, boolean useInitialBudget) {
        BuildState state = BUILD_STATES.get(materialList);
        if (state == null || !state.signature().equals(signature)) {
            removeBuildDisplayData(materialList);
            state = new BuildState(signature, sourceEntries, materialList.getMultiplier(), inventory);
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

    private static void clearSourceState() {
        expandedSourceKey = "";
        visibleSourceKey = "";
        fullSourceKey = "";
        SOURCE_ANIMATIONS.clear();
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

    private static int scaledCount(int count, int scale) {
        return clampToInt((long) count * Math.max(1, scale));
    }

    private static int resolvedAvailable(long preparedCount, int directAvailable) {
        long available = Math.max(0L, preparedCount) + Math.max(0, directAvailable);
        return clampToInt(available);
    }

    private static final class Accumulator {
        private final class_1799 stack;
        private final String name;
        private final Map<String, Candidate> candidates = new LinkedHashMap<>();
        private final Map<String, SourceAccumulator> sources = new LinkedHashMap<>();
        private long totalCount;
        private long preparedCount;

        private Accumulator(class_1799 stack, String name) {
            this.stack = stack.method_7972();
            this.name = name;
            this.candidates.put(ItemStackTexts.id(stack), new Candidate(stack.method_7972(), ItemStackTexts.name(stack)));
        }

        private void add(int count, boolean prepared, List<class_1799> icons, List<String> names, SourceOrigin source) {
            if (prepared) {
                this.preparedCount += count;
            } else {
                this.totalCount += count;
            }
            for (int index = 0; index < icons.size(); index++) {
                class_1799 icon = icons.get(index);
                if (!icon.method_7960()) {
                    String name = index < names.size() ? names.get(index) : ItemStackTexts.name(icon);
                    this.candidates.putIfAbsent(ItemStackTexts.id(icon), new Candidate(icon.method_7972(), name));
                }
            }
            this.sources.computeIfAbsent(source.id(), ignored -> new SourceAccumulator(source))
                    .add(count, prepared);
        }

        private List<Candidate> candidates() {
            return List.copyOf(this.candidates.values());
        }

        private List<class_1799> candidateIcons() {
            List<class_1799> icons = new ArrayList<>(this.candidates.size());
            for (Candidate candidate : this.candidates.values()) {
                icons.add(candidate.icon().method_7972());
            }
            return List.copyOf(icons);
        }

        private List<SourceContribution> sources() {
            List<SourceContribution> contributions = new ArrayList<>(this.sources.size());
            int maxStackSize = Math.max(1, this.stack.method_7914());
            for (SourceAccumulator source : this.sources.values()) {
                contributions.add(source.toContribution(maxStackSize));
            }
            contributions.sort(Comparator
                    .comparingInt(SourceContribution::totalCount)
                    .reversed()
                    .thenComparing(SourceContribution::name));
            return List.copyOf(contributions);
        }
    }

    private static final class SourceAccumulator {
        private final SourceOrigin origin;
        private long totalCount;
        private long preparedCount;

        private SourceAccumulator(SourceOrigin origin) {
            this.origin = origin;
        }

        private void add(int count, boolean prepared) {
            if (prepared) {
                this.preparedCount += count;
            } else {
                this.totalCount += count;
            }
        }

        private SourceContribution toContribution(int maxStackSize) {
            long missingCount = Math.max(0L, this.totalCount - this.preparedCount);
            return new SourceContribution(
                    this.origin.icon().method_7972(),
                    this.origin.name(),
                    clampToInt(this.totalCount),
                    clampToInt(missingCount),
                    this.origin.totalCount(),
                    this.origin.missingCount(),
                    maxStackSize);
        }
    }

    private static final class BuildState {
        private final String signature;
        private final List<MaterialListEntry> sourceEntries;
        private final int multiplier;
        private final InventoryCounts.Snapshot inventory;
        private final Map<String, Accumulator> materials = new LinkedHashMap<>();
        private List<MaterialListEntry> entries = List.of();
        private int nextSourceIndex;
        private boolean complete;

        private BuildState(String signature, List<MaterialListEntry> sourceEntries, int multiplier, InventoryCounts.Snapshot inventory) {
            this.signature = signature;
            this.sourceEntries = List.copyOf(sourceEntries);
            this.multiplier = Math.max(1, multiplier);
            this.inventory = inventory;
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
                int baseTotal = entry.getCountTotal();
                int total = scaledCount(baseTotal, this.multiplier);
                int missing = MaterialCounts.netMissing(entry, this.multiplier);
                int prepared = Math.max(0, total - Math.min(total, missing));
                SourceOrigin source = new SourceOrigin(ItemStackTexts.id(stack), stack.method_7972(), ItemStackTexts.name(stack), total, missing);
                collectLeaves(stack, List.of(stack), List.of(ItemStackTexts.name(stack)), ItemStackTexts.name(stack), source, baseTotal, this.multiplier, false, 0, new HashSet<>(), this.materials);
                collectPreparedLeaves(stack, List.of(stack), List.of(ItemStackTexts.name(stack)), ItemStackTexts.name(stack), source, baseTotal, prepared, this.materials);
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
            String placementKey = placementKey(materialList);
            Set<String> ignored = ignoredSet(placementKey);
            int ignoredSize = ignored == null ? 0 : ignored.size();
            int filteredEntryCount = 0;
            List<MaterialListEntry> entries = new ArrayList<>(this.materials.size());
            Map<MaterialListEntry, DisplayData> displays = new IdentityHashMap<>();
            for (Accumulator material : this.materials.values()) {
                if (isIgnored(ignored, material)) {
                    filteredEntryCount++;
                    continue;
                }

                int total = clampToInt(material.totalCount);
                int available = resolvedAvailable(material.preparedCount, this.inventory.countAny(material.candidateIcons()));
                MaterialListEntry entry = new MaterialListEntry(material.stack.method_7972(), total, total, 0, available);
                entries.add(entry);
                DisplayData display = new DisplayData(material.name, material.candidates(), material.sources());
                displays.put(entry, display);
                ENTRY_DISPLAY_KEYS.put(entryKey(entry), display);
            }

            this.entries = entries;
            ENTRY_DISPLAYS.putAll(displays);
            LOGGER.info("[minimal rebuild] placementKey={} ignoredSize={} filteredEntryCount={}",
                    placementKey, ignoredSize, filteredEntryCount);
        }
    }

    private static boolean isIgnored(MaterialListBase materialList, Accumulator material) {
        return isIgnored(ignoredSet(placementKey(materialList)), material);
    }

    private static boolean isIgnored(Set<String> ignored, Accumulator material) {
        return ignored != null && ignored.contains(stableEntryKey(material.stack, material.candidates(), material.name));
    }

    private static List<String> ignoredKeys(MaterialListBase materialList) {
        Set<String> ignored = ignoredSet(placementKey(materialList));
        if (ignored == null || ignored.isEmpty()) {
            return List.of();
        }

        List<String> keys = new ArrayList<>(ignored);
        keys.sort(String::compareTo);
        return keys;
    }

    private record Cache(String signature, List<MaterialListEntry> entries) {
    }

    private record DisplayData(String name, List<Candidate> candidates, List<SourceContribution> sources) {
        private String displayName() {
            String stableName = this.stableName();
            return this.candidates.size() > 1 ? emphasizeVariable(stableName) : emphasizeAny(stableName);
        }

        private boolean hasRequirementSection() {
            return this.candidates.size() > 1 && isAnyGroupName(this.stableName());
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

    private record SourceOrigin(String id, class_1799 icon, String name, int totalCount, int missingCount) {
    }

    private record CandidateSet(List<class_1799> icons, List<String> names) {
    }

    public record TooltipCandidate(class_1799 icon, String name) {
    }

    public record RequirementContribution(class_1799 icon, List<class_1799> icons, List<String> candidateNames, String name, int totalCount, int missingCount, int maxStackSize, UpstreamRequirement upstream) {
    }

    public record UpstreamRequirement(class_1799 icon, List<class_1799> icons, List<String> candidateNames, String name, int totalCount, int missingCount, int maxStackSize) {
    }

    public record SourceContribution(class_1799 icon, String name, int totalCount, int missingCount, int sourceTotalCount, int sourceMissingCount, int maxStackSize) {
    }

    private static String itemPath(class_1799 stack) {
        String id = ItemStackTexts.id(stack);
        return itemPath(id);
    }

    private static String itemPath(String id) {
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

    private static String stableEntryKey(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        if (display == null) {
            return "id:" + ItemStackTexts.id(entry.getStack());
        }

        return stableEntryKey(display.currentCandidate().icon(), display.candidates(), display.name());
    }

    private static String stableEntryKey(class_1799 stack, List<Candidate> candidates, String name) {
        List<class_1799> icons = new ArrayList<>(candidates.size());
        for (Candidate candidate : candidates) {
            icons.add(candidate.icon());
        }

        return groupKey(stack, icons, name);
    }

    private static String knownAlternativeTranslationKey(List<class_1799> icons) {
        if (icons.size() < 2) {
            return "";
        }
        if (allIconsMatch(icons, MinimalSubMaterialListView::isLogLike) && hasMultipleWoodFamilies(icons)) {
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
        if (allIconsMatch(icons, MinimalSubMaterialListView::isPurpurLike)) {
            return "lmlp.label.recipe.any.purpur";
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

    private static boolean allCandidatesMatch(List<Candidate> candidates, Predicate<String> predicate) {
        if (candidates.isEmpty()) {
            return false;
        }

        for (Candidate candidate : candidates) {
            if (!predicate.test(itemPath(candidate.icon()))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isLogLike(String path) {
        return path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_stem")
                || path.endsWith("_hyphae")
                || path.equals("bamboo_block")
                || path.equals("stripped_bamboo_block");
    }

    private static boolean isPlanksLike(String path) {
        return path.endsWith("_planks");
    }

    private static boolean hasMultipleWoodFamilies(List<class_1799> icons) {
        String firstFamily = "";
        for (class_1799 icon : icons) {
            String family = woodFamily(itemPath(icon));
            if (family.isEmpty()) {
                return true;
            }
            if (firstFamily.isEmpty()) {
                firstFamily = family;
            } else if (!firstFamily.equals(family)) {
                return true;
            }
        }

        return false;
    }

    private static String woodFamily(String path) {
        if (path.startsWith("stripped_")) {
            path = path.substring("stripped_".length());
        }

        for (String family : WOOD_FAMILIES) {
            if (path.equals(family) || path.startsWith(family + "_")) {
                return family;
            }
        }

        return "";
    }

    private static boolean isCobblestoneLike(String path) {
        return path.contains("cobblestone")
                || path.equals("cobbled_deepslate")
                || path.equals("blackstone");
    }

    private static boolean isPurpurLike(String path) {
        return path.equals("purpur_block") || path.equals("purpur_pillar");
    }

    private static String emphasizeAny(String name) {
        if (name.startsWith("\u4efb\u610f") || name.startsWith("\u6d60\u7ed8\u5270") || name.startsWith("Any ")) {
            return COMMON_HIGHLIGHT + name + RESET;
        }
        if (name.startsWith("任意")) {
            return COMMON_HIGHLIGHT + "任意" + RESET + name.substring("任意".length());
        }
        if (name.startsWith("Any ")) {
            return COMMON_HIGHLIGHT + "Any" + RESET + name.substring("Any".length());
        }
        return name;
    }

    private static boolean isAnyGroupName(String name) {
        String trimmed = name.trim();
        return trimmed.startsWith("\u4efb\u610f")
                || trimmed.startsWith("\u6d60\u7ed8\u5270")
                || trimmed.startsWith("Any ");
    }

    private static String emphasizeVariable(String name) {
        return name.isEmpty() ? name : COMMON_HIGHLIGHT + name + RESET;
    }
}
