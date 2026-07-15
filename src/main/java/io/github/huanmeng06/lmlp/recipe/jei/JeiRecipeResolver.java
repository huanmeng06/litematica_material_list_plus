package io.github.huanmeng06.lmlp.recipe.jei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeResolver;
import io.github.huanmeng06.lmlp.recipe.RecipeNativeDisplayHandle;
import io.github.huanmeng06.lmlp.recipe.RecipeSlotSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

public final class JeiRecipeResolver implements RecipeResolver {
    @Override
    public List<RecipeSummary> findRecipes(ItemStack target, int totalCount, int missingCount) {
        IJeiRuntime runtime = JeiRuntimeBridge.runtime().orElse(null);
        if (runtime == null || target.isEmpty()) {
            return List.of();
        }

        IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
        IFocus<ItemStack> outputFocus = focusFactory.createFocus(
                RecipeIngredientRole.OUTPUT,
                VanillaTypes.ITEM_STACK,
                target.copy());
        List<IFocus<?>> focuses = List.of(outputFocus);
        IFocusGroup focusGroup = focusFactory.createFocusGroup(focuses);
        IRecipeManager recipeManager = runtime.getRecipeManager();
        List<RecipeSummary> summaries = new ArrayList<>();

        recipeManager.createRecipeCategoryLookup()
                .limitFocus(focuses)
                .get()
                .forEach(category -> collectCategory(recipeManager, category, focusGroup, focuses, target, totalCount, missingCount, summaries));
        return List.copyOf(summaries);
    }

    private static <R> void collectCategory(
            IRecipeManager recipeManager,
            IRecipeCategory<R> category,
            IFocusGroup focusGroup,
            List<IFocus<?>> focuses,
            ItemStack target,
            int totalCount,
            int missingCount,
            List<RecipeSummary> summaries) {
        recipeManager.createRecipeLookup(category.getRecipeType())
                .limitFocus(focuses)
                .get()
                .forEach(recipe -> recipeManager.createRecipeLayoutDrawable(category, recipe, focusGroup)
                        .ifPresent(layout -> addSummary(recipeManager, focusGroup, category, recipe, layout,
                                target, totalCount, missingCount, summaries)));
    }

    private static <R> void addSummary(
            IRecipeManager recipeManager,
            IFocusGroup focusGroup,
            IRecipeCategory<R> category,
            R recipe,
            IRecipeLayoutDrawable<R> layout,
            ItemStack target,
            int totalCount,
            int missingCount,
            List<RecipeSummary> summaries) {
        ItemStack output = layout.getRecipeSlotsView()
                .getSlotViews(RecipeIngredientRole.OUTPUT)
                .stream()
                .flatMap(IRecipeSlotView::getItemStacks)
                .filter(stack -> !stack.isEmpty() && stack.getItem() == target.getItem())
                .findFirst()
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
        if (output.isEmpty()) {
            return;
        }

        int outputCount = Math.max(1, output.getCount());
        int craftsTotal = divideRoundUp(totalCount, outputCount);
        int craftsMissing = divideRoundUp(missingCount, outputCount);
        List<RecipeSlotSummary> slots = summarizeSlots(layout);
        String recipeId = category.getRegistryName(recipe) == null ? "unknown" : category.getRegistryName(recipe).toString();
        Supplier<IRecipeLayoutDrawable<R>> layoutFactory = () -> recipeManager
                .createRecipeLayoutDrawable(category, recipe, focusGroup)
                .orElse(layout);
        summaries.add(new RecipeSummary(
                category.getRecipeType().getUid().toString(),
                recipeId,
                output,
                outputCount,
                craftsTotal,
                craftsMissing,
                summarizeInputs(slots, craftsTotal, craftsMissing),
                slots,
                3,
                3,
                false,
                new JeiNativeRecipe<>(category, recipe, layoutFactory, layout)));
    }

    private static List<RecipeSlotSummary> summarizeSlots(IRecipeLayoutDrawable<?> layout) {
        List<RecipeSlotSummary> slots = new ArrayList<>();
        for (IRecipeSlotView slot : layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT)) {
            List<ItemStack> alternatives = slot.getItemStacks()
                    .filter(stack -> !stack.isEmpty())
                    .map(ItemStack::copy)
                    .toList();
            if (alternatives.isEmpty()) {
                slots.add(RecipeSlotSummary.EMPTY);
                continue;
            }

            List<String> names = alternatives.stream().map(ItemStackTexts::name).distinct().toList();
            ItemStack first = alternatives.get(0);
            slots.add(new RecipeSlotSummary(first.copy(), alternatives, names, Math.max(1, first.getCount())));
        }
        while (slots.size() < 9) {
            slots.add(RecipeSlotSummary.EMPTY);
        }
        return List.copyOf(slots.size() > 9 ? slots.subList(0, 9) : slots);
    }

    private static List<IngredientSummary> summarizeInputs(List<RecipeSlotSummary> slots, int craftsTotal, int craftsMissing) {
        Map<String, IngredientAccumulator> ingredients = new LinkedHashMap<>();
        for (RecipeSlotSummary slot : slots) {
            if (!slot.isEmpty()) {
                String key = String.join("|", slot.alternatives()) + '#' + slot.count();
                ingredients.computeIfAbsent(key, ignored -> new IngredientAccumulator(slot)).add(slot.count());
            }
        }
        return ingredients.values().stream()
                .map(accumulator -> accumulator.toSummary(craftsTotal, craftsMissing))
                .toList();
    }

    private static int divideRoundUp(int value, int divisor) {
        return value <= 0 ? 0 : (value + divisor - 1) / divisor;
    }

    public static final class JeiNativeRecipe<R> implements RecipeNativeDisplayHandle {
        private final IRecipeCategory<R> category;
        private final R recipe;
        private final Supplier<IRecipeLayoutDrawable<R>> layoutFactory;
        private final IRecipeLayoutDrawable<R> fallbackLayout;
        private IRecipeLayoutDrawable<R> layout;

        private JeiNativeRecipe(IRecipeCategory<R> category, R recipe,
                Supplier<IRecipeLayoutDrawable<R>> layoutFactory, IRecipeLayoutDrawable<R> layout) {
            this.category = category;
            this.recipe = recipe;
            this.layoutFactory = layoutFactory;
            this.fallbackLayout = layout;
            this.layout = layout;
        }

        public IRecipeCategory<R> category() {
            return this.category;
        }

        public R recipe() {
            return this.recipe;
        }

        public IRecipeLayoutDrawable<R> layout() {
            if (this.layout == null) {
                synchronized (this) {
                    if (this.layout == null) {
                        try {
                            this.layout = this.layoutFactory.get();
                        } catch (Throwable throwable) {
                            this.layout = this.fallbackLayout;
                        }
                    }
                }
            }
            return this.layout;
        }

        @Override
        public Object fork() {
            return new JeiNativeRecipe<>(this.category, this.recipe, this.layoutFactory, this.fallbackLayout, null);
        }

        private JeiNativeRecipe(IRecipeCategory<R> category, R recipe,
                Supplier<IRecipeLayoutDrawable<R>> layoutFactory,
                IRecipeLayoutDrawable<R> fallbackLayout,
                IRecipeLayoutDrawable<R> layout) {
            this.category = category;
            this.recipe = recipe;
            this.layoutFactory = layoutFactory;
            this.fallbackLayout = fallbackLayout;
            this.layout = layout;
        }
    }

    private static final class IngredientAccumulator {
        private final RecipeSlotSummary slot;
        private int countPerCraft;

        private IngredientAccumulator(RecipeSlotSummary slot) {
            this.slot = slot;
        }

        private void add(int count) {
            this.countPerCraft += count;
        }

        private IngredientSummary toSummary(int craftsTotal, int craftsMissing) {
            return new IngredientSummary(
                    this.slot.icon().copy(),
                    this.slot.icons(),
                    this.slot.alternatives(),
                    this.countPerCraft,
                    this.countPerCraft * craftsTotal,
                    this.countPerCraft * craftsMissing,
                    Math.max(1, this.slot.icon().getMaxStackSize()));
        }
    }
}
