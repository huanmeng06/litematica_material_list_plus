package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.FamilyIconCycle;
import io.github.huanmeng06.lmlp.material.InventoryCounts;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.material.MaterialCounts;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeResolvers;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummaryFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class MinimalSubMaterialListView {
    private static final Logger LOGGER = LoggerFactory.getLogger("LMLP MinimalSubMaterialListView");
    private static final int MAX_RECIPE_DEPTH = 16;
    // Non-wood fallback step; wood groups advance per family window instead.
    // Kept equal to FamilyIconCycle's use in the renderers so the target row's
    // own cycling icon stays in phase with its inline "所需" upstream icons.
    private static final long DISPLAY_CYCLE_MS = FamilyIconCycle.FALLBACK_STEP_MILLIS;
    private static final long FAMILY_CYCLE_MS = FamilyIconCycle.FAMILY_WINDOW_MILLIS;
    private static final long BUILD_BUDGET_NS = 2_500_000L;
    private static final long BACKGROUND_BUILD_BUDGET_NS = 1_000_000L;
    private static final String COMMON_HIGHLIGHT = "\u00A7e";
    private static final String RESET = "\u00A7r";
    private static final List<String> FALLBACK_WOOD_FAMILIES = List.of(
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
    private static List<String> discoveredWoodFamilies;
    private static final Map<MaterialListBase, Boolean> ACTIVE_LISTS = new WeakHashMap<>();
    private static final Map<MaterialListBase, Cache> ENTRY_CACHES = new WeakHashMap<>();
    private static final Map<MaterialListBase, BuildState> BUILD_STATES = new WeakHashMap<>();
    private static final Map<MaterialListEntry, DisplayData> ENTRY_DISPLAYS = new IdentityHashMap<>();
    private static final Map<String, DisplayData> ENTRY_DISPLAY_KEYS = new HashMap<>();
    private static final Map<String, List<RequirementContribution>> REQUIREMENT_CACHES = new HashMap<>();
    private static final ExpandAnimationTracker SOURCE_ANIMATIONS = new ExpandAnimationTracker();
    private static String expandedSourceKey = "";
    private static String visibleSourceKey = "";
    private static String fullSourceKey = "";
    private static SourceSortMode sourceSortMode = SourceSortMode.MISSING_COUNT;
    private static boolean sourceSortDescending = true;
    private static long layoutRevision;

    private MinimalSubMaterialListView() {
    }

    public static boolean isActive(MaterialListBase materialList) {
        return Boolean.TRUE.equals(ACTIVE_LISTS.get(materialList));
    }

    public static void setActive(MaterialListBase materialList, boolean active) {
        if (active) {
            MaterialListPlusState.suspendForMinimalView(materialList);
            ACTIVE_LISTS.put(materialList, true);
            prepare(materialList);
        } else {
            ACTIVE_LISTS.remove(materialList);
            MaterialListPlusState.restoreAfterMinimalView(materialList);
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
            return filterIgnored(materialList, cache.entries());
        }

        BuildState state = buildState(materialList, signature, sourceEntries, inventory);

        state.process(materialList, BUILD_BUDGET_NS, true);
        if (state.isComplete()) {
            storeCache(materialList, signature, state.entries());
            BUILD_STATES.remove(materialList);
            return filterIgnored(materialList, state.entries());
        }

        if (cache != null) {
            return filterIgnored(materialList, cache.entries());
        }
        return filterIgnored(materialList, state.entries());
    }

    public static void prepare(MaterialListBase materialList) {
        List<MaterialListEntry> sourceEntries = new ArrayList<>(materialList.getMaterialsFiltered(true));
        InventoryCounts.Snapshot inventory = InventoryCounts.current();
        String signature = signature(materialList, sourceEntries, inventory);
        Cache cache = ENTRY_CACHES.get(materialList);
        if (cache != null && cache.signature().equals(signature)) {
            return;
        }

        BuildState state = buildState(materialList, signature, sourceEntries, inventory);
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
        String placementKey = IgnoredMaterialRegistry.placementKey(materialList);
        int beforeSize = ignoredSize(materialList);
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

        boolean changed = IgnoredMaterialRegistry.ignoreMinimal(materialList, key);
        if (changed) {
            onIgnoredFilterChanged();
        }
        return changed;
    }

    public static void clearIgnored(MaterialListBase materialList) {
        int beforeSize = ignoredSize(materialList);
        IgnoredMaterialRegistry.clearMinimalIgnored(materialList);
        if (beforeSize > 0) {
            onIgnoredFilterChanged();
        }
    }

    public static String debugStableKey(MaterialListEntry entry) {
        return stableEntryKey(entry);
    }

    public static int ignoredSize(MaterialListBase materialList) {
        return IgnoredMaterialRegistry.minimalIgnoredSize(materialList);
    }

    public static boolean tick(MaterialListBase materialList) {
        if (!isActive(materialList)) {
            prewarm(materialList);
            return false;
        }

        BuildState state = BUILD_STATES.get(materialList);
        if (state == null) {
            return false;
        }

        boolean changed = state.process(materialList, BUILD_BUDGET_NS, true);
        boolean complete = state.isComplete();
        if (complete) {
            storeCache(materialList, state.signature(), state.entries());
            BUILD_STATES.remove(materialList);
        }
        return changed || complete;
    }

    private static boolean prewarm(MaterialListBase materialList) {
        BuildState state = BUILD_STATES.get(materialList);
        if (state == null) {
            // A completed cache remains valid while cycling through the normal
            // list modes. setActive() performs the definitive signature check.
            if (ENTRY_CACHES.containsKey(materialList)) {
                return true;
            }
            if (materialList.getMaterialListType() != BlockInfoListType.ALL) {
                return false;
            }

            List<MaterialListEntry> sourceEntries = new ArrayList<>(materialList.getMaterialsFiltered(true));
            InventoryCounts.Snapshot inventory = InventoryCounts.current();
            state = buildState(materialList, signature(materialList, sourceEntries, inventory), sourceEntries, inventory);
        }

        state.process(materialList, BACKGROUND_BUILD_BUDGET_NS, false);
        if (!state.isComplete()) {
            return false;
        }

        storeCache(materialList, state.signature(), state.entries());
        BUILD_STATES.remove(materialList);
        return true;
    }

    public static String displayName(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        return display == null ? ItemStackTexts.name(entry.getStack()) : display.displayName();
    }

    public static String widestDisplayName(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        return display == null ? ItemStackTexts.name(entry.getStack()) : display.widestName();
    }

    public static ItemStack displayStack(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        if (display == null || display.candidates().isEmpty()) {
            return entry.getStack();
        }

        return display.currentCandidate().icon().copy();
    }

    public static List<ItemStack> displayStacks(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        if (display == null || display.candidates().isEmpty()) {
            return List.of(entry.getStack());
        }

        List<ItemStack> stacks = new ArrayList<>(display.candidates().size());
        for (Candidate candidate : display.candidates()) {
            stacks.add(candidate.icon().copy());
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
            candidates.add(new TooltipCandidate(candidate.icon().copy(), candidate.name()));
        }
        return List.copyOf(candidates);
    }

    public static List<SourceContribution> sourceContributions(MaterialListEntry entry) {
        DisplayData display = displayData(entry);
        return display == null ? List.of() : sortedSources(display.sources());
    }

    public static SourceSortMode sourceSortMode() {
        return sourceSortMode;
    }

    public static void cycleSourceSortMode() {
        sourceSortMode = sourceSortMode.next();
    }

    public static boolean sourceSortDescending() {
        return sourceSortDescending;
    }

    public static void toggleSourceSortDirection() {
        sourceSortDescending = !sourceSortDescending;
    }

    private static List<SourceContribution> sortedSources(List<SourceContribution> sources) {
        List<SourceContribution> sorted = new ArrayList<>(sources);
        Comparator<SourceContribution> counts = sourceSortMode == SourceSortMode.TOTAL_COUNT
                ? Comparator.comparingInt(SourceContribution::totalCount)
                : Comparator.comparingInt(SourceContribution::missingCount)
                        .thenComparingInt(SourceContribution::totalCount);
        if (sourceSortDescending) {
            counts = counts.reversed();
        }
        sorted.sort(counts.thenComparing(SourceContribution::name));
        return List.copyOf(sorted);
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

    // Style a choice-group ("任意X" / tag) title so the recipe-panel hover popup
    // matches the minimal sub-material page: yellow, regular, without underline. The name
    // may already carry format codes (defensive: only add ours when it doesn't).
    public static String emphasizeChoiceGroupName(String name) {
        if (name == null || name.isEmpty() || name.indexOf('§') >= 0) {
            return name;
        }
        return emphasizeVariable(name);
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
            ItemStack icon = requirement.icons().get(index);
            String name = index < requirement.candidateNames().size() ? requirement.candidateNames().get(index) : ItemStackTexts.name(icon);
            candidates.add(new TooltipCandidate(icon.copy(), name));
        }
        return List.copyOf(candidates);
    }

    public static List<TooltipCandidate> upstreamTooltipCandidates(UpstreamRequirement upstream) {
        if (upstream == null || upstream.icons().size() < 2) {
            return List.of();
        }

        List<TooltipCandidate> candidates = new ArrayList<>(upstream.icons().size());
        for (int index = 0; index < upstream.icons().size(); index++) {
            ItemStack icon = upstream.icons().get(index);
            String name = index < upstream.candidateNames().size() ? upstream.candidateNames().get(index) : ItemStackTexts.name(icon);
            candidates.add(new TooltipCandidate(icon.copy(), name));
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

    private static void collectLeaves(ItemStack stack, List<ItemStack> icons, List<String> names, String name, SourceOrigin source, int count, int scale, boolean prepared, int depth, Set<String> seenItems, Map<String, Accumulator> materials) {
        ItemStack icon = stack.copy();
        String itemId = ItemStackTexts.id(icon);
        if (count <= 0 || scale <= 0) {
            return;
        }

        // Raw wood is the terminal result of the wood decomposition chain.
        // JEI integrations may expose stripping/cutting recipes whose axe slot
        // would otherwise be treated as another consumed material.
        if (isLogLike(itemPath(icon))) {
            addLeaf(icon, icons, names, name, source, scaledCount(count, scale), prepared, materials);
            return;
        }

        if (depth >= MAX_RECIPE_DEPTH || seenItems.contains(itemId) || Configs.shouldStopRecipeDecomposition(itemId) || keepAsLeaf(itemId)) {
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

        // Stop before a conversion chain loops back to this same material.
        // Waiting until the recursive seenItems guard would preserve the
        // rounded overproduction instead of the original demand.
        if (RecipeResolvers.leadsBackTo(itemId, summaries.get(0), MAX_RECIPE_DEPTH - depth)) {
            addLeaf(icon, icons, names, name, source, scaledCount(count, scale), prepared, materials);
            return;
        }

        for (IngredientSummary ingredient : summaries.get(0).ingredients()) {
            List<ItemStack> ingredientIcons = ingredient.icons().isEmpty() ? List.of(ingredient.icon()) : ingredient.icons();
            List<String> ingredientNames = candidateNames(ingredientIcons, ingredient.alternatives());
            if (ingredient.isChoiceGroup()) {
                CandidateSet refinedChoice = expandRegisteredLogCandidates(
                        refineWoodCandidatesForSource(source, ingredientIcons, ingredientNames));
                String groupName = refinedChoice.names().isEmpty()
                        ? RecipeSummaryFormatter.ingredientName(ingredient)
                        : refinedChoice.names().get(0);

                if (allIconsMatch(refinedChoice.icons(), MinimalSubMaterialListView::isLogLike)) {
                    addLeaf(
                            refinedChoice.icons().get(0),
                            refinedChoice.icons(),
                            refinedChoice.names(),
                            groupName,
                            source,
                            scaledCount(ingredient.countTotal(), scale),
                            prepared,
                            materials);
                    continue;
                }

                // (1) Kept by config (任意台阶 by default): show as its own row
                // plus the "所需" decomposition hint.
                if (keepGroupAsLeaf(refinedChoice.icons())) {
                    addLeaf(
                            refinedChoice.icons().get(0),
                            refinedChoice.icons(),
                            refinedChoice.names(),
                            RecipeSummaryFormatter.ingredientName(ingredient),
                            source,
                            scaledCount(ingredient.countTotal(), scale),
                            prepared,
                            materials);
                    continue;
                }

                // (2) Refined to a single planks item: existing single-item recursion.
                if (shouldDecomposeRefinedChoice(refinedChoice)) {
                    collectLeaves(
                            refinedChoice.icons().get(0),
                            refinedChoice.icons(),
                            refinedChoice.names(),
                            groupName,
                            source,
                            ingredient.countTotal(),
                            scale,
                            prepared,
                            depth + 1,
                            childSeenItems,
                            materials);
                    continue;
                }

                // (3) Multi-variant and every candidate is craftable: union-decompose
                // (任意台阶 -> 任意木板 -> 任意原木) rather than collapsing to the
                // representative wood family.
                if (refinedChoice.icons().size() > 1
                        && isWoodChoiceGroup(refinedChoice.icons())
                        && isChoiceGroupDecomposable(refinedChoice.icons())) {
                    collectChoiceGroupLeaves(
                            refinedChoice.icons(),
                            refinedChoice.names(),
                            groupName,
                            ingredient.countTotal(),
                            source,
                            scale,
                            prepared,
                            depth + 1,
                            childSeenItems,
                            materials);
                    continue;
                }

                // (4) Not union-decomposable (coal/charcoal, sand/red_sand, ...): keep.
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

    // Decompose a multi-variant choice group into leaves by unioning each
    // candidate's recipe slot-by-slot (任意台阶 -> 任意木板 -> 任意原木), mirroring
    // MaterialTreeBuilder.buildChoiceGroupChildren but aggregating into the leaf
    // accumulator instead of building nodes. All candidates in a wood group share
    // the same recipe yield ratio, so the representative's per-slot counts are
    // authoritative (same assumption as the tree engine).
    private static void collectChoiceGroupLeaves(List<ItemStack> icons, List<String> names, String name, int count, SourceOrigin source, int scale, boolean prepared, int depth, Set<String> seenItems, Map<String, Accumulator> materials) {
        if (count <= 0 || scale <= 0 || icons.isEmpty()) {
            return;
        }

        ItemStack representative = icons.get(0);
        if (allIconsMatch(icons, MinimalSubMaterialListView::isLogLike)) {
            addLeaf(representative, icons, names, name, source, scaledCount(count, scale), prepared, materials);
            return;
        }

        String representativeId = ItemStackTexts.id(representative);
        if (depth >= MAX_RECIPE_DEPTH || seenItems.contains(representativeId)
                || Configs.shouldStopRecipeDecomposition(representativeId) || keepGroupAsLeaf(icons)) {
            addLeaf(representative, icons, names, name, source, scaledCount(count, scale), prepared, materials);
            return;
        }

        List<RecipeSummary> representativeSummaries = RecipeResolvers.findRecipes(representative, count, count);
        if (representativeSummaries.isEmpty() || representativeSummaries.get(0).ingredients().isEmpty()
                || !isChoiceGroupDecomposable(icons)) {
            addLeaf(representative, icons, names, name, source, scaledCount(count, scale), prepared, materials);
            return;
        }

        Set<String> childSeenItems = new HashSet<>(seenItems);
        childSeenItems.add(representativeId);

        List<List<IngredientSummary>> perCandidate = new ArrayList<>();
        for (ItemStack candidate : icons) {
            List<RecipeSummary> summaries = RecipeResolvers.findRecipes(candidate, count, count);
            perCandidate.add(summaries.isEmpty() ? List.of() : summaries.get(0).ingredients());
        }

        List<IngredientSummary> representativeIngredients = representativeSummaries.get(0).ingredients();
        for (int index = 0; index < representativeIngredients.size(); index++) {
            IngredientSummary representativeChild = representativeIngredients.get(index);
            if (representativeChild.countTotal() <= 0 && representativeChild.countMissing() <= 0) {
                continue;
            }

            Map<String, ItemStack> unionIcons = new LinkedHashMap<>();
            List<String> unionNames = new ArrayList<>();
            addSlotToUnion(representativeChild, unionIcons, unionNames);
            for (List<IngredientSummary> candidateIngredients : perCandidate) {
                if (index < candidateIngredients.size()) {
                    addSlotToUnion(candidateIngredients.get(index), unionIcons, unionNames);
                }
            }

            List<ItemStack> childIcons = new ArrayList<>(unionIcons.values());
            if (childIcons.isEmpty()) {
                childIcons = List.of(representativeChild.icon().copy());
            }
            List<String> childNames = candidateNames(childIcons, unionNames);
            String childFallback = childNames.isEmpty()
                    ? RecipeSummaryFormatter.ingredientName(representativeChild)
                    : childNames.get(0);
            boolean childIsGroup = childIcons.size() > 1 || unionNames.size() > 1;
            String childName = childIsGroup ? groupDisplayName(childIcons, childFallback) : childFallback;

            if (childIsGroup) {
                collectChoiceGroupLeaves(childIcons, childNames, childName, representativeChild.countTotal(), source, scale, prepared, depth + 1, childSeenItems, materials);
            } else {
                collectLeaves(childIcons.get(0), childIcons, childNames, childName, source, representativeChild.countTotal(), scale, prepared, depth + 1, childSeenItems, materials);
            }
        }
    }

    private static void addSlotToUnion(IngredientSummary child, Map<String, ItemStack> icons, List<String> names) {
        List<ItemStack> childIcons = child.icons().isEmpty() ? List.of(child.icon()) : child.icons();
        for (ItemStack stack : childIcons) {
            if (!stack.isEmpty()) {
                icons.putIfAbsent(ItemStackTexts.id(stack), stack.copy());
            }
        }

        List<String> childNames = child.alternatives();
        if (childNames.isEmpty()) {
            String childName = ItemStackTexts.name(child.icon());
            if (!childName.isEmpty() && !names.contains(childName)) {
                names.add(childName);
            }
            return;
        }

        for (String childName : childNames) {
            if (!names.contains(childName)) {
                names.add(childName);
            }
        }
    }

    private static boolean keepGroupAsLeaf(List<ItemStack> icons) {
        if (icons.isEmpty()) {
            return false;
        }
        for (ItemStack icon : icons) {
            if (icon.isEmpty() || !Configs.shouldKeepAsLeaf(ItemStackTexts.id(icon))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isChoiceGroupDecomposable(List<ItemStack> icons) {
        for (ItemStack icon : icons) {
            List<RecipeSummary> summaries = RecipeResolvers.findRecipes(icon, 1, 1);
            if (summaries.isEmpty() || summaries.get(0).ingredients().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWoodChoiceGroup(List<ItemStack> icons) {
        return allIconsMatch(icons, path -> !woodFamily(path).isEmpty());
    }

    private static void collectPreparedLeaves(ItemStack stack, List<ItemStack> icons, List<String> names, String name, SourceOrigin source, int baseCount, int preparedCount, Map<String, Accumulator> materials) {
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

    private static void addLeaf(ItemStack stack, List<ItemStack> icons, List<String> names, String name, SourceOrigin source, int count, boolean prepared, Map<String, Accumulator> materials) {
        CandidateSet candidates = expandRegisteredLogCandidates(
                refineWoodCandidatesForSource(source, icons.isEmpty() ? List.of(stack) : icons, names));
        String displayNameFallback = candidates.names().size() == 1 ? candidates.names().get(0) : name;
        ItemStack displayStack = candidates.icons().isEmpty() ? stack : candidates.icons().get(0);
        String key = groupKey(displayStack, candidates.icons(), displayNameFallback);
        String displayName = groupDisplayName(candidates.icons(), displayNameFallback);
        materials.computeIfAbsent(key, ignored -> new Accumulator(displayStack, displayName))
                .add(count, prepared, candidates.icons(), candidates.names(), source);
    }

    private static boolean shouldDecomposeRefinedChoice(CandidateSet candidates) {
        return candidates.icons().size() == 1 && isPlanksLike(itemPath(candidates.icons().get(0)));
    }

    private static CandidateSet refineWoodCandidatesForSource(SourceOrigin source, List<ItemStack> icons, List<String> names) {
        if (icons.size() < 2) {
            return new CandidateSet(icons, names);
        }

        String sourceFamily = woodFamily(itemPath(source.id()));
        if (sourceFamily.isEmpty() || !allIconsMatch(icons, path -> isPlanksLike(path) || isLogLike(path))) {
            return new CandidateSet(icons, names);
        }

        List<ItemStack> refinedIcons = new ArrayList<>();
        List<String> refinedNames = new ArrayList<>();
        for (int index = 0; index < icons.size(); index++) {
            ItemStack icon = icons.get(index);
            if (sourceFamily.equals(woodFamily(itemPath(icon)))) {
                refinedIcons.add(icon);
                refinedNames.add(index < names.size() ? names.get(index) : ItemStackTexts.name(icon));
            }
        }

        return refinedIcons.isEmpty() ? new CandidateSet(icons, names) : new CandidateSet(List.copyOf(refinedIcons), List.copyOf(refinedNames));
    }

    private static CandidateSet expandRegisteredLogCandidates(CandidateSet candidates) {
        if (candidates.icons().size() < 2
                || !allIconsMatch(candidates.icons(), MinimalSubMaterialListView::isLogLike)
                || !hasMultipleWoodFamilies(candidates.icons())) {
            return candidates;
        }

        Map<String, ItemStack> iconsById = new LinkedHashMap<>();
        for (ItemStack icon : candidates.icons()) {
            iconsById.putIfAbsent(ItemStackTexts.id(icon), icon.copy());
        }

        // JEI may keep a recipe's alternative list from an older tag snapshot.
        // Merge every currently registered log-like item so new vanilla and
        // modded wood families still appear in the "Any log" hover panel.
        try {
            for (var identifier : BuiltInRegistries.ITEM.keySet()) {
                if (!isLogLike(identifier.getPath())) {
                    continue;
                }
                var item = BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
                if (item != null) {
                    ItemStack icon = new ItemStack(item, 1);
                    iconsById.putIfAbsent(ItemStackTexts.id(icon), icon);
                }
            }
        } catch (Throwable ignored) {
            return candidates;
        }

        List<ItemStack> icons = List.copyOf(iconsById.values());
        return new CandidateSet(icons, candidateNames(icons, List.of()));
    }

    private static String groupKey(ItemStack stack, List<ItemStack> icons, String name) {
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

    private static String groupDisplayName(List<ItemStack> icons, String fallback) {
        String alternativeKey = knownAlternativeTranslationKey(icons);
        if (!alternativeKey.isEmpty()) {
            return StringUtils.translate(alternativeKey);
        }

        return fallback;
    }

    private static List<String> candidateNames(List<ItemStack> icons, List<String> alternatives) {
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
        for (ItemStack icon : icons) {
            if (!icon.isEmpty()) {
                names.add(ItemStackTexts.name(icon));
            }
        }
        return names.isEmpty() ? List.of() : names;
    }

    private static String signature(MaterialListBase materialList, List<MaterialListEntry> entries, InventoryCounts.Snapshot inventory) {
        String placementKey = IgnoredMaterialRegistry.placementKey(materialList);
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
        return builder.toString();
    }

    private static List<MaterialListEntry> filterIgnored(MaterialListBase materialList, List<MaterialListEntry> entries) {
        Set<String> ignored = IgnoredMaterialRegistry.minimalIgnoredKeys(materialList);
        if (ignored == null || ignored.isEmpty() || entries.isEmpty()) {
            return entries;
        }

        List<MaterialListEntry> filtered = new ArrayList<>(entries.size());
        for (MaterialListEntry entry : entries) {
            if (!ignored.contains(stableEntryKey(entry))) {
                filtered.add(entry);
            }
        }
        return List.copyOf(filtered);
    }

    private static void onIgnoredFilterChanged() {
        clearSourceState();
        layoutRevision++;
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
            Map<String, ItemStack> iconsById = new LinkedHashMap<>();
            List<String> names = new ArrayList<>();

            for (List<IngredientSummary> ingredients : recipeIngredients) {
                if (index >= ingredients.size()) {
                    continue;
                }

                IngredientSummary ingredient = ingredients.get(index);
                List<ItemStack> icons = ingredient.icons().isEmpty() ? List.of(ingredient.icon()) : ingredient.icons();
                for (ItemStack icon : icons) {
                    if (!icon.isEmpty()) {
                        iconsById.putIfAbsent(ItemStackTexts.id(icon), icon.copy());
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

            List<ItemStack> icons = List.copyOf(iconsById.values());
            List<String> candidateNames = candidateNames(icons, names);
            String fallbackName = candidateNames.isEmpty() ? RecipeSummaryFormatter.ingredientName(base) : candidateNames.get(0);
            String name = requirementGroupName(targetPlanksGroup, icons, fallbackName);
            List<ItemStack> copiedIcons = icons.stream().map(ItemStack::copy).toList();
            requirements.add(new RequirementContribution(
                    icons.get(0).copy(),
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

    private static UpstreamRequirement upstreamRequirement(List<ItemStack> icons, int totalCount, int missingCount) {
        if (!allIconsMatch(icons, MinimalSubMaterialListView::isPlanksLike)) {
            return null;
        }

        Map<String, ItemStack> iconsById = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        IngredientSummary firstBase = null;
        for (ItemStack icon : icons) {
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

            List<ItemStack> ingredientIcons = ingredient.icons().isEmpty() ? List.of(ingredient.icon()) : ingredient.icons();
            for (ItemStack ingredientIcon : ingredientIcons) {
                if (!ingredientIcon.isEmpty()) {
                    iconsById.putIfAbsent(ItemStackTexts.id(ingredientIcon), ingredientIcon.copy());
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

        List<ItemStack> upstreamIcons = List.copyOf(iconsById.values());
        if (!allIconsMatch(upstreamIcons, MinimalSubMaterialListView::isLogLike)) {
            return null;
        }

        CandidateSet expanded = expandRegisteredLogCandidates(
                new CandidateSet(upstreamIcons, candidateNames(upstreamIcons, names)));
        upstreamIcons = expanded.icons();
        List<String> candidateNames = expanded.names();
        String name = StringUtils.translate("lmlp.label.recipe.any.log");
        return new UpstreamRequirement(
                upstreamIcons.get(0).copy(),
                upstreamIcons.stream().map(ItemStack::copy).toList(),
                candidateNames,
                name,
                firstBase.countTotal(),
                firstBase.countMissing(),
                firstBase.maxStackSize());
    }

    private static IngredientSummary firstLogIngredient(List<IngredientSummary> ingredients) {
        for (IngredientSummary ingredient : ingredients) {
            List<ItemStack> icons = ingredient.icons().isEmpty() ? List.of(ingredient.icon()) : ingredient.icons();
            if (allIconsMatch(icons, MinimalSubMaterialListView::isLogLike)) {
                return ingredient;
            }
        }

        return null;
    }

    private static String requirementGroupName(boolean targetPlanksGroup, List<ItemStack> icons, String fallbackName) {
        if (targetPlanksGroup && allIconsMatch(icons, MinimalSubMaterialListView::isLogLike)) {
            return StringUtils.translate("lmlp.label.recipe.any.log");
        }
        if (allIconsMatch(icons, MinimalSubMaterialListView::isPlanksLike)) {
            return StringUtils.translate("lmlp.label.recipe.any.planks");
        }

        return groupDisplayName(icons, fallbackName);
    }

    private static BuildState buildState(MaterialListBase materialList, String signature, List<MaterialListEntry> sourceEntries, InventoryCounts.Snapshot inventory) {
        BuildState state = BUILD_STATES.get(materialList);
        if (state == null || !state.signature().equals(signature)) {
            removeBuildDisplayData(materialList);
            state = new BuildState(signature, sourceEntries, materialList.getMultiplier(), inventory);
            BUILD_STATES.put(materialList, state);
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
        private final ItemStack stack;
        private final String name;
        private final Map<String, Candidate> candidates = new LinkedHashMap<>();
        private final Map<String, SourceAccumulator> sources = new LinkedHashMap<>();
        private long totalCount;
        private long preparedCount;

        private Accumulator(ItemStack stack, String name) {
            this.stack = stack.copy();
            this.name = name;
            this.candidates.put(ItemStackTexts.id(stack), new Candidate(stack.copy(), ItemStackTexts.name(stack)));
        }

        private void add(int count, boolean prepared, List<ItemStack> icons, List<String> names, SourceOrigin source) {
            if (prepared) {
                this.preparedCount += count;
            } else {
                this.totalCount += count;
            }
            for (int index = 0; index < icons.size(); index++) {
                ItemStack icon = icons.get(index);
                if (!icon.isEmpty()) {
                    String name = index < names.size() ? names.get(index) : ItemStackTexts.name(icon);
                    this.candidates.putIfAbsent(ItemStackTexts.id(icon), new Candidate(icon.copy(), name));
                }
            }
            this.sources.computeIfAbsent(source.id(), ignored -> new SourceAccumulator(source))
                    .add(count, prepared);
        }

        private void mergeFrom(Accumulator other) {
            this.totalCount += other.totalCount;
            this.preparedCount += other.preparedCount;
            other.candidates.forEach(this.candidates::putIfAbsent);
            other.sources.forEach((key, source) -> this.sources
                    .computeIfAbsent(key, ignored -> new SourceAccumulator(source.origin))
                    .mergeFrom(source));
        }

        private List<Candidate> candidates() {
            return List.copyOf(this.candidates.values());
        }

        private List<ItemStack> candidateIcons() {
            List<ItemStack> icons = new ArrayList<>(this.candidates.size());
            for (Candidate candidate : this.candidates.values()) {
                icons.add(candidate.icon().copy());
            }
            return List.copyOf(icons);
        }

        private List<SourceContribution> sources() {
            List<SourceContribution> contributions = new ArrayList<>(this.sources.size());
            int maxStackSize = Math.max(1, this.stack.getMaxStackSize());
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

        private void mergeFrom(SourceAccumulator other) {
            this.totalCount += other.totalCount;
            this.preparedCount += other.preparedCount;
        }

        private SourceContribution toContribution(int maxStackSize) {
            long missingCount = Math.max(0L, this.totalCount - this.preparedCount);
            return new SourceContribution(
                    this.origin.icon().copy(),
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

        private boolean process(MaterialListBase materialList, long budgetNs, boolean publishPartial) {
            if (this.complete) {
                return false;
            }

            long deadline = System.nanoTime() + budgetNs;
            boolean changed = false;
            try (RecipeResolvers.ComputationScope ignored = RecipeResolvers.withComputationDeadline(deadline)) {
                while (this.nextSourceIndex < this.sourceEntries.size()) {
                    Map<String, Accumulator> delta = new LinkedHashMap<>();
                    try {
                        RecipeResolvers.checkpoint();
                        MaterialListEntry entry = this.sourceEntries.get(this.nextSourceIndex);
                        ItemStack stack = entry.getStack();
                        int baseTotal = entry.getCountTotal();
                        int total = scaledCount(baseTotal, this.multiplier);
                        int missing = MaterialCounts.netMissing(entry, this.multiplier);
                        int prepared = Math.max(0, total - Math.min(total, missing));
                        SourceOrigin source = new SourceOrigin(ItemStackTexts.id(stack), stack.copy(), ItemStackTexts.name(stack), total, missing);
                        collectLeaves(stack, List.of(stack), List.of(ItemStackTexts.name(stack)), ItemStackTexts.name(stack), source, baseTotal, this.multiplier, false, 0, new HashSet<>(), delta);
                        collectPreparedLeaves(stack, List.of(stack), List.of(ItemStackTexts.name(stack)), ItemStackTexts.name(stack), source, baseTotal, prepared, delta);
                    } catch (RecipeResolvers.BudgetExceededException exception) {
                        break;
                    }

                    mergeMaterials(this.materials, delta);
                    this.nextSourceIndex++;
                    changed = true;
                }
                this.complete = this.nextSourceIndex >= this.sourceEntries.size();
            }

            if (this.complete || (publishPartial && changed && ENTRY_CACHES.get(materialList) == null)) {
                this.publish(materialList, this.complete);
            }
            return changed;
        }

        private static void mergeMaterials(Map<String, Accumulator> target, Map<String, Accumulator> delta) {
            delta.forEach((key, material) -> target
                    .computeIfAbsent(key, ignored -> new Accumulator(material.stack, material.name))
                    .mergeFrom(material));
        }

        private void publish(MaterialListBase materialList, boolean logCompletion) {
            removeDisplayData(materialList);
            removeBuildDisplayData(materialList);
            String placementKey = IgnoredMaterialRegistry.placementKey(materialList);
            List<MaterialListEntry> entries = new ArrayList<>(this.materials.size());
            Map<MaterialListEntry, DisplayData> displays = new IdentityHashMap<>();
            for (Accumulator material : this.materials.values()) {
                int total = clampToInt(material.totalCount);
                int available = resolvedAvailable(material.preparedCount, this.inventory.countAny(material.candidateIcons()));
                MaterialListEntry entry = new MaterialListEntry(material.stack.copy(), total, total, 0, available);
                entries.add(entry);
                DisplayData display = new DisplayData(material.name, material.candidates(), material.sources());
                displays.put(entry, display);
                ENTRY_DISPLAY_KEYS.put(entryKey(entry), display);
            }

            this.entries = entries;
            ENTRY_DISPLAYS.putAll(displays);
            if (logCompletion) {
                LOGGER.info("[minimal rebuild] placementKey={} entryCount={}",
                        placementKey, entries.size());
            }
        }
    }

    private record Cache(String signature, List<MaterialListEntry> entries) {
    }

    private record DisplayData(String name, List<Candidate> candidates, List<SourceContribution> sources) {
        private String displayName() {
            String stableName = this.stableName();
            return this.candidates.size() > 1 ? emphasizeVariable(stableName) : emphasizeAny(stableName);
        }

        private boolean hasRequirementSection() {
            if (this.candidates.size() > 1) {
                // Logs (任意原木) are the terminal raw-gatherable resource — they
                // never decompose further. Without this guard, a stripping-axe
                // "recipe" (log -> stripped_log) some candidates carry gets
                // mistaken for a real decomposition step, producing a bogus
                // self-referential "所需 任意原木" row under 任意原木 itself.
                if (allCandidatesMatch(this.candidates, MinimalSubMaterialListView::isLogLike)) {
                    return false;
                }
                return isAnyGroupName(this.stableName());
            }
            // Single-item wood intermediates kept as a leaf (e.g. 木棍) also carry
            // a "所需 任意木板 ▶ 任意原木" hint, resolved from their own recipe.
            return this.candidates.size() == 1
                    && keepAsLeaf(ItemStackTexts.id(this.candidates.get(0).icon()));
        }

        private String widestName() {
            return this.displayName();
        }

        private Candidate currentCandidate() {
            if (this.candidates.size() == 1) {
                return this.candidates.get(0);
            }

            // Cycle the target row/header icon on the SAME family-grouped clock
            // as the inline "所需" upstream icons (FamilyIconCycle), so 任意台阶
            // / 任意木板 shown here stays on the same wood family that its
            // upstream 任意原木 is currently showing. Pick the icon via the
            // shared helper, then map it back to its candidate by id.
            List<ItemStack> icons = new ArrayList<>(this.candidates.size());
            for (Candidate candidate : this.candidates) {
                icons.add(candidate.icon());
            }
            ItemStack picked = FamilyIconCycle.pick(icons, System.currentTimeMillis(), FAMILY_CYCLE_MS, DISPLAY_CYCLE_MS);
            String pickedId = ItemStackTexts.id(picked);
            for (Candidate candidate : this.candidates) {
                if (ItemStackTexts.id(candidate.icon()).equals(pickedId)) {
                    return candidate;
                }
            }
            return this.candidates.get(0);
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
            List<ItemStack> icons = new ArrayList<>(this.candidates.size());
            for (Candidate candidate : this.candidates) {
                icons.add(candidate.icon());
            }

            return groupDisplayName(icons, "");
        }
    }

    private record Candidate(ItemStack icon, String name) {
    }

    private record SourceOrigin(String id, ItemStack icon, String name, int totalCount, int missingCount) {
    }

    private record CandidateSet(List<ItemStack> icons, List<String> names) {
    }

    public record TooltipCandidate(ItemStack icon, String name) {
    }

    public record RequirementContribution(ItemStack icon, List<ItemStack> icons, List<String> candidateNames, String name, int totalCount, int missingCount, int maxStackSize, UpstreamRequirement upstream) {
    }

    public record UpstreamRequirement(ItemStack icon, List<ItemStack> icons, List<String> candidateNames, String name, int totalCount, int missingCount, int maxStackSize) {
    }

    public record SourceContribution(ItemStack icon, String name, int totalCount, int missingCount, int sourceTotalCount, int sourceMissingCount, int maxStackSize) {
    }

    public enum SourceSortMode {
        TOTAL_COUNT, MISSING_COUNT;

        public SourceSortMode next() {
            SourceSortMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private static String itemPath(ItemStack stack) {
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

    private static String stableEntryKey(ItemStack stack, List<Candidate> candidates, String name) {
        List<ItemStack> icons = new ArrayList<>(candidates.size());
        for (Candidate candidate : candidates) {
            icons.add(candidate.icon());
        }

        return groupKey(stack, icons, name);
    }

    private static String knownAlternativeTranslationKey(List<ItemStack> icons) {
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

    private static boolean allIconsMatch(List<ItemStack> icons, Predicate<String> predicate) {
        if (icons.isEmpty()) {
            return false;
        }

        for (ItemStack icon : icons) {
            if (icon.isEmpty() || !predicate.test(itemPath(icon))) {
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
        return !woodFamily(path).isEmpty()
                && (path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_stem")
                || path.endsWith("_hyphae")
                || path.equals("bamboo_block")
                || path.equals("stripped_bamboo_block"));
    }

    private static boolean isPlanksLike(String path) {
        return path.endsWith("_planks") && !woodFamily(path).isEmpty();
    }

    // Single items kept as their own counted leaf row (with the "所需 …"
    // decomposition hint) instead of being decomposed away. Driven by the
    // user-editable keepAsLeafItems list; the item stays recipe-resolvable (it's
    // NOT in recipeStopItems), so the hint still shows.
    private static boolean keepAsLeaf(String itemId) {
        return Configs.shouldKeepAsLeaf(itemId);
    }

    private static boolean hasMultipleWoodFamilies(List<ItemStack> icons) {
        String firstFamily = "";
        for (ItemStack icon : icons) {
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

        for (String family : woodFamilies()) {
            if (path.equals(family) || path.startsWith(family + "_")) {
                return family;
            }
        }

        return "";
    }

    private static List<String> woodFamilies() {
        if (discoveredWoodFamilies != null) {
            return discoveredWoodFamilies;
        }

        Set<String> families = new LinkedHashSet<>(FALLBACK_WOOD_FAMILIES);
        try {
            for (var identifier : BuiltInRegistries.ITEM.keySet()) {
                String path = identifier.getPath();
                if (path.endsWith("_planks") && path.length() > "_planks".length()) {
                    families.add(path.substring(0, path.length() - "_planks".length()));
                }
            }
        } catch (Throwable ignored) {
            // The fallback list keeps vanilla behavior if a registry is not yet ready.
        }

        List<String> sorted = new ArrayList<>(families);
        sorted.sort(Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo));
        discoveredWoodFamilies = List.copyOf(sorted);
        return discoveredWoodFamilies;
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
