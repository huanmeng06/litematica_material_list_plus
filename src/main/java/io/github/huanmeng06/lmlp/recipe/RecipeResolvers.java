package io.github.huanmeng06.lmlp.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;

public final class RecipeResolvers {
    private static final int MAX_QUERY_CACHE_SIZE = 8192;
    private static final int MAX_CYCLE_CACHE_SIZE = 8192;
    private static final Map<QueryKey, List<RecipeSummary>> QUERY_CACHE = new LinkedHashMap<>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<QueryKey, List<RecipeSummary>> eldest) {
            return size() > MAX_QUERY_CACHE_SIZE;
        }
    };
    private static final Map<CycleKey, Boolean> CYCLE_CACHE = new LinkedHashMap<>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CycleKey, Boolean> eldest) {
            return size() > MAX_CYCLE_CACHE_SIZE;
        }
    };
    private static final ThreadLocal<Long> COMPUTATION_DEADLINE = new ThreadLocal<>();
    private static RecipeResolver resolver;

    private RecipeResolvers() {
    }

    public static List<RecipeSummary> findRecipes(ItemStack target, int totalCount, int missingCount) {
        if (Configs.shouldStopRecipeDecomposition(ItemStackTexts.id(target))) {
            return Collections.emptyList();
        }

        QueryKey key = new QueryKey(stackFingerprint(target));
        List<RecipeSummary> baseSummaries;
        synchronized (QUERY_CACHE) {
            if (QUERY_CACHE.containsKey(key)) {
                baseSummaries = QUERY_CACHE.get(key);
                return applyPreferredOrder(scaleSummaries(baseSummaries, totalCount, missingCount));
            }
        }

        checkpoint();
        try {
            // Query JEI once per distinct item stack. Recipe discovery, category
            // lookup and native layout construction do not depend on the requested
            // material count; only the derived craft/ingredient totals do.
            baseSummaries = List.copyOf(getResolver().findRecipes(target, 1, 1));
        } catch (BudgetExceededException exception) {
            throw exception;
        } catch (Throwable throwable) {
            baseSummaries = Collections.emptyList();
        }

        synchronized (QUERY_CACHE) {
            QUERY_CACHE.put(key, baseSummaries);
        }
        return applyPreferredOrder(scaleSummaries(baseSummaries, totalCount, missingCount));
    }

    public static void clearCache() {
        synchronized (QUERY_CACHE) {
            QUERY_CACHE.clear();
        }
        synchronized (CYCLE_CACHE) {
            CYCLE_CACHE.clear();
        }
    }

    public static ComputationScope withComputationDeadline(long deadlineNs) {
        Long previousDeadline = COMPUTATION_DEADLINE.get();
        COMPUTATION_DEADLINE.set(deadlineNs);
        return new ComputationScope(previousDeadline);
    }

    public static void checkpoint() {
        Long deadline = COMPUTATION_DEADLINE.get();
        if (deadline != null && System.nanoTime() >= deadline) {
            throw BudgetExceededException.INSTANCE;
        }
    }

    public static List<RecipeSummary> applyPreferredOrder(List<RecipeSummary> summaries) {
        if (summaries.size() <= 1) {
            return summaries;
        }

        int preferredIndex = preferredIndex(summaries);
        if (preferredIndex <= 0) {
            return summaries;
        }

        List<RecipeSummary> sorted = new ArrayList<>(summaries.size());
        sorted.add(summaries.get(preferredIndex));
        for (int index = 0; index < summaries.size(); index++) {
            if (index != preferredIndex) {
                sorted.add(summaries.get(index));
            }
        }
        return List.copyOf(sorted);
    }

    /** Returns true when following the selected (first/preferred) recipe can
     * lead back to the item currently being decomposed. This must be checked
     * before applying craft-output rounding; detecting the repeated item only
     * after recursion can inflate requirements (29 bone meal -> 4 bone blocks
     * -> 36 bone meal). */
    public static boolean leadsBackTo(String targetItemId, RecipeSummary summary, int maxDepth) {
        if (targetItemId == null || targetItemId.isEmpty() || summary == null || maxDepth < 0) {
            return false;
        }
        CycleKey key = new CycleKey(targetItemId, summary.category(), summary.recipeId(), maxDepth);
        synchronized (CYCLE_CACHE) {
            if (CYCLE_CACHE.containsKey(key)) {
                return CYCLE_CACHE.get(key);
            }
        }

        boolean cycles = leadsBackTo(targetItemId, summary, maxDepth, new HashMap<>());
        synchronized (CYCLE_CACHE) {
            CYCLE_CACHE.put(key, cycles);
        }
        return cycles;
    }

    private static boolean leadsBackTo(String targetItemId, RecipeSummary summary, int remainingDepth,
            Map<String, Integer> visitedDepths) {
        for (IngredientSummary ingredient : summary.ingredients()) {
            List<ItemStack> icons = ingredient.icons().isEmpty()
                    ? List.of(ingredient.icon())
                    : ingredient.icons();
            for (ItemStack icon : icons) {
                if (icon.isEmpty()) {
                    continue;
                }
                String itemId = ItemStackTexts.id(icon);
                if (targetItemId.equals(itemId)) {
                    return true;
                }
                if (remainingDepth <= 0) {
                    continue;
                }
                Integer previousDepth = visitedDepths.get(itemId);
                if (previousDepth != null && previousDepth >= remainingDepth) {
                    continue;
                }
                visitedDepths.put(itemId, remainingDepth);

                List<RecipeSummary> nested = findRecipes(icon, 1, 1);
                boolean cycles = !nested.isEmpty()
                        && isUnaryConversion(nested.get(0))
                        && leadsBackTo(targetItemId, nested.get(0), remainingDepth - 1, visitedDepths);
                if (cycles) {
                    return true;
                }
            }
        }
        return false;
    }

    // Which recipe to decompose first. A user-pinned preference always wins; when
    // none is pinned, fall back to a built-in default so ambiguous items resolve
    // deterministically. Returns 0 when the current head is already preferred and
    // -1 when there's nothing to prefer.
    private static int preferredIndex(List<RecipeSummary> summaries) {
        String itemId = ItemStackTexts.id(summaries.get(0).outputIcon());
        String userPin = Configs.preferredRecipeId(itemId);
        if (!userPin.isEmpty()) {
            for (int index = 0; index < summaries.size(); index++) {
                if (userPin.equals(summaries.get(index).recipeId())) {
                    return index;
                }
            }
            return -1;
        }

        // Colored beds also have a dye conversion recipe whose input is the
        // "any bed" tag. That candidate set includes the output bed itself,
        // so choosing it first makes cycle protection stop decomposition at
        // the bed. Prefer the normal same-id recipe (wool + planks); explicit
        // user recipe pins above still take precedence.
        if (itemPath(itemId).endsWith("_bed")) {
            for (int index = 0; index < summaries.size(); index++) {
                if (itemId.equals(summaries.get(index).recipeId())) {
                    return index;
                }
            }
        }

        // Default: prefer a recipe whose every ingredient is planks. This keeps
        // wooden intermediates like sticks (craftable from planks OR bamboo)
        // decomposing along the planks -> logs chain, matching how slabs behave,
        // instead of depending on a recipe viewer's non-deterministic enumeration order.
        for (int index = 0; index < summaries.size(); index++) {
            if (isAllPlanksRecipe(summaries.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isAllPlanksRecipe(RecipeSummary summary) {
        if (summary.ingredients().isEmpty()) {
            return false;
        }
        for (IngredientSummary ingredient : summary.ingredients()) {
            if (ingredient.icons().isEmpty()) {
                return false;
            }
            for (ItemStack icon : ingredient.icons()) {
                if (!itemPath(ItemStackTexts.id(icon)).endsWith("_planks")) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String itemPath(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private static boolean isUnaryConversion(RecipeSummary summary) {
        // Deep cycle lookahead exists to catch reversible one-material
        // conversions such as bone_meal <-> bone_block before craft rounding.
        // Do not walk arbitrary multi-input recipes: JEI processing categories
        // often expose reusable tools or containers as ordinary INPUT slots,
        // which can otherwise create false paths through axes, picks and sticks.
        return summary.ingredients().size() == 1;
    }

    private static String stackFingerprint(ItemStack stack) {
        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        return ItemStackTexts.id(normalized) + '|' + normalized;
    }

    private static List<RecipeSummary> scaleSummaries(List<RecipeSummary> summaries, int totalCount, int missingCount) {
        if (summaries.isEmpty()) {
            return summaries;
        }

        List<RecipeSummary> scaled = new ArrayList<>(summaries.size());
        for (RecipeSummary summary : summaries) {
            int craftsTotal = divideRoundUp(totalCount, summary.outputCount());
            int craftsMissing = divideRoundUp(missingCount, summary.outputCount());
            List<IngredientSummary> ingredients = new ArrayList<>(summary.ingredients().size());
            for (IngredientSummary ingredient : summary.ingredients()) {
                ingredients.add(new IngredientSummary(
                        ingredient.icon(),
                        ingredient.icons(),
                        ingredient.alternatives(),
                        ingredient.countPerCraft(),
                        scaledIngredientCount(ingredient.countPerCraft(), craftsTotal),
                        scaledIngredientCount(ingredient.countPerCraft(), craftsMissing),
                        ingredient.maxStackSize()));
            }
            Object nativeDisplay = summary.nativeDisplay();
            if (nativeDisplay instanceof RecipeNativeDisplayHandle handle) {
                nativeDisplay = handle.fork();
            }
            scaled.add(new RecipeSummary(
                    summary.category(),
                    summary.recipeId(),
                    summary.outputIcon(),
                    summary.outputCount(),
                    craftsTotal,
                    craftsMissing,
                    ingredients,
                    summary.inputSlots(),
                    summary.gridWidth(),
                    summary.gridHeight(),
                    summary.shapeless(),
                    nativeDisplay));
        }
        return List.copyOf(scaled);
    }

    private static int divideRoundUp(int value, int divisor) {
        return value <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE,
                ((long) value + divisor - 1L) / divisor);
    }

    private static int scaledIngredientCount(int countPerCraft, int crafts) {
        return (int) Math.min(Integer.MAX_VALUE, (long) countPerCraft * crafts);
    }

    private static RecipeResolver getResolver() throws ReflectiveOperationException {
        if (resolver != null) {
            return resolver;
        }

        if (FabricLoader.getInstance().isModLoaded("jei")) {
            Class<?> resolverClass = Class.forName("io.github.huanmeng06.lmlp.recipe.jei.JeiRecipeResolver");
            resolver = (RecipeResolver) resolverClass.getDeclaredConstructor().newInstance();
        } else {
            resolver = (target, totalCount, missingCount) -> Collections.emptyList();
        }

        return resolver;
    }

    public static final class ComputationScope implements AutoCloseable {
        private final Long previousDeadline;
        private boolean closed;

        private ComputationScope(Long previousDeadline) {
            this.previousDeadline = previousDeadline;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.previousDeadline == null) {
                COMPUTATION_DEADLINE.remove();
            } else {
                COMPUTATION_DEADLINE.set(this.previousDeadline);
            }
        }
    }

    public static final class BudgetExceededException extends RuntimeException {
        private static final BudgetExceededException INSTANCE = new BudgetExceededException();

        private BudgetExceededException() {
            super(null, null, false, false);
        }
    }

    private record QueryKey(String stackFingerprint) {
    }

    private record CycleKey(String targetItemId, String category, String recipeId, int maxDepth) {
    }
}
