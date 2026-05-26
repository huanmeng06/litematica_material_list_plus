package io.github.huanmeng06.lmlp.recipe.rei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.recipe.IngredientSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeResolver;
import io.github.huanmeng06.lmlp.recipe.RecipeSlotSummary;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import net.minecraft.class_1799;
import net.minecraft.class_2960;

public final class ReiRecipeResolver implements RecipeResolver {
    @Override
    public List<RecipeSummary> findRecipes(class_1799 target, int totalCount, int missingCount) {
        List<RecipeSummary> summaries = new ArrayList<>();
        DisplayRegistry registry = DisplayRegistry.getInstance();

        for (List<Display> displays : registry.getAll().values()) {
            for (Display display : displays) {
                if (!registry.isDisplayVisible(display) || isTagDisplay(display)) {
                    continue;
                }

                class_1799 output = findMatchingOutput(display, target);
                if (output.method_7960()) {
                    continue;
                }

                int outputCount = Math.max(1, output.method_7947());
                int craftsTotal = divideRoundUp(totalCount, outputCount);
                int craftsMissing = divideRoundUp(missingCount, outputCount);
                summaries.add(new RecipeSummary(
                        display.getCategoryIdentifier().getIdentifier().toString(),
                        display.getDisplayLocation().map(class_2960::toString).orElse("unknown"),
                        output.method_7972(),
                        outputCount,
                        craftsTotal,
                        craftsMissing,
                        summarizeInputs(display, craftsTotal, craftsMissing),
                        summarizeSlots(display),
                        3,
                        3,
                        display instanceof DefaultCraftingDisplay<?> craftingDisplay && craftingDisplay.isShapeless()));
            }
        }

        return summaries;
    }

    private static boolean isTagDisplay(Display display) {
        String category = display.getCategoryIdentifier().getIdentifier().toString();
        Optional<class_2960> location = display.getDisplayLocation();
        return category.contains("/tag") || category.endsWith(":tag") || location.map(class_2960::toString).orElse("").contains("/tag");
    }

    private static class_1799 findMatchingOutput(Display display, class_1799 target) {
        for (EntryIngredient ingredient : display.getOutputEntries()) {
            for (EntryStack<?> stack : ingredient) {
                class_1799 output = asItemStack(stack);
                if (!output.method_7960() && output.method_7909() == target.method_7909()) {
                    return output;
                }
            }
        }

        return class_1799.field_8037;
    }

    private static List<IngredientSummary> summarizeInputs(Display display, int craftsTotal, int craftsMissing) {
        Map<String, IngredientAccumulator> ingredients = new LinkedHashMap<>();

        for (RecipeSlotSummary slot : summarizeSlots(display)) {
            if (slot.isEmpty()) {
                continue;
            }

            String key = keyFor(slot.alternatives(), slot.count());
            IngredientAccumulator accumulator = ingredients.computeIfAbsent(key, ignored -> new IngredientAccumulator(slot.icon(), slot.alternatives()));
            accumulator.addSlot(slot.count());
        }

        List<IngredientSummary> summaries = new ArrayList<>();
        for (IngredientAccumulator accumulator : ingredients.values()) {
            summaries.add(accumulator.toSummary(craftsTotal, craftsMissing));
        }

        return summaries;
    }

    private static List<RecipeSlotSummary> summarizeSlots(Display display) {
        List<EntryIngredient> inputs;
        if (display instanceof DefaultCraftingDisplay<?> craftingDisplay) {
            inputs = craftingDisplay.getOrganisedInputEntries(3, 3);
        } else {
            inputs = display.getInputEntries();
        }

        List<RecipeSlotSummary> slots = new ArrayList<>();
        for (EntryIngredient input : inputs) {
            slots.add(toSlot(input));
        }

        while (slots.size() < 9) {
            slots.add(RecipeSlotSummary.EMPTY);
        }

        if (slots.size() > 9) {
            return slots.subList(0, 9);
        }

        return slots;
    }

    private static RecipeSlotSummary toSlot(EntryIngredient ingredient) {
        List<class_1799> alternatives = collectAlternatives(ingredient);
        if (alternatives.isEmpty()) {
            return RecipeSlotSummary.EMPTY;
        }

        class_1799 first = alternatives.get(0);
        List<String> names = new ArrayList<>();
        for (class_1799 alternative : alternatives) {
            String name = ItemStackTexts.name(alternative);
            if (!names.contains(name)) {
                names.add(name);
            }
        }

        return new RecipeSlotSummary(first.method_7972(), names, Math.max(1, first.method_7947()));
    }

    private static List<class_1799> collectAlternatives(EntryIngredient ingredient) {
        List<class_1799> alternatives = new ArrayList<>();
        for (EntryStack<?> stack : ingredient) {
            class_1799 itemStack = asItemStack(stack);
            if (!itemStack.method_7960()) {
                alternatives.add(itemStack);
            }
        }
        return alternatives;
    }

    private static class_1799 asItemStack(EntryStack<?> stack) {
        try {
            class_1799 itemStack = stack.cheatsAs().getValue();
            return itemStack == null ? class_1799.field_8037 : itemStack;
        } catch (Throwable throwable) {
            return class_1799.field_8037;
        }
    }

    private static String keyFor(List<String> alternatives, int countPerSlot) {
        StringBuilder builder = new StringBuilder();
        for (String alternative : alternatives) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(alternative);
        }
        builder.append('#').append(countPerSlot);
        return builder.toString();
    }

    private static int divideRoundUp(int value, int divisor) {
        if (value <= 0) {
            return 0;
        }
        return (value + divisor - 1) / divisor;
    }

    private static final class IngredientAccumulator {
        private final class_1799 icon;
        private final List<String> alternatives;
        private final int maxStackSize;
        private int countPerCraft;

        private IngredientAccumulator(class_1799 icon, List<String> alternatives) {
            this.icon = icon.method_7972();
            this.alternatives = List.copyOf(alternatives);
            this.maxStackSize = Math.max(1, icon.method_7914());
            this.countPerCraft = 0;
        }

        private void addSlot(int countPerSlot) {
            this.countPerCraft += countPerSlot;
        }

        private IngredientSummary toSummary(int craftsTotal, int craftsMissing) {
            return new IngredientSummary(
                    this.icon,
                    this.alternatives,
                    this.countPerCraft,
                    this.countPerCraft * craftsTotal,
                    this.countPerCraft * craftsMissing,
                    this.maxStackSize);
        }
    }
}
