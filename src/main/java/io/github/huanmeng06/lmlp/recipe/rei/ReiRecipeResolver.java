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
                List<RecipeSlotSummary> inputSlots = summarizeSlots(display, output);
                summaries.add(new RecipeSummary(
                        display.getCategoryIdentifier().getIdentifier().toString(),
                        display.getDisplayLocation().map(class_2960::toString).orElse("unknown"),
                        output.method_7972(),
                        outputCount,
                        craftsTotal,
                        craftsMissing,
                        summarizeInputs(inputSlots, craftsTotal, craftsMissing),
                        inputSlots,
                        3,
                        3,
                        display instanceof DefaultCraftingDisplay<?> craftingDisplay && craftingDisplay.isShapeless(),
                        display));
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

    private static List<IngredientSummary> summarizeInputs(List<RecipeSlotSummary> slots, int craftsTotal, int craftsMissing) {
        Map<String, IngredientAccumulator> ingredients = new LinkedHashMap<>();

        for (RecipeSlotSummary slot : slots) {
            if (slot.isEmpty()) {
                continue;
            }

            String key = keyFor(slot.alternatives(), slot.count());
            IngredientAccumulator accumulator = ingredients.computeIfAbsent(key, ignored -> new IngredientAccumulator(slot.icon(), slot.icons(), slot.alternatives()));
            accumulator.addSlot(slot.count());
        }

        List<IngredientSummary> summaries = new ArrayList<>();
        for (IngredientAccumulator accumulator : ingredients.values()) {
            summaries.add(accumulator.toSummary(craftsTotal, craftsMissing));
        }

        return summaries;
    }

    private static List<RecipeSlotSummary> summarizeSlots(Display display, class_1799 output) {
        List<EntryIngredient> inputs;
        if (display instanceof DefaultCraftingDisplay<?> craftingDisplay) {
            inputs = craftingDisplay.getOrganisedInputEntries(3, 3);
        } else {
            inputs = display.getInputEntries();
        }

        List<RecipeSlotSummary> slots = new ArrayList<>();
        for (EntryIngredient input : inputs) {
            slots.add(toSlot(input, output));
        }

        while (slots.size() < 9) {
            slots.add(RecipeSlotSummary.EMPTY);
        }

        if (slots.size() > 9) {
            return slots.subList(0, 9);
        }

        return slots;
    }

    private static RecipeSlotSummary toSlot(EntryIngredient ingredient, class_1799 output) {
        List<class_1799> alternatives = collectAlternatives(ingredient);
        if (alternatives.isEmpty()) {
            return RecipeSlotSummary.EMPTY;
        }

        alternatives = refineWoodVariantAlternatives(output, alternatives);

        class_1799 first = alternatives.get(0);
        List<String> names = new ArrayList<>();
        for (class_1799 alternative : alternatives) {
            String name = ItemStackTexts.name(alternative);
            if (!names.contains(name)) {
                names.add(name);
            }
        }

        return new RecipeSlotSummary(first.method_7972(), alternatives, names, Math.max(1, first.method_7947()));
    }

    private static List<class_1799> refineWoodVariantAlternatives(class_1799 output, List<class_1799> alternatives) {
        if (alternatives.size() < 2) {
            return alternatives;
        }

        String family = woodFamily(output);
        if (family.isEmpty()) {
            return alternatives;
        }

        List<class_1799> refined = new ArrayList<>();
        for (class_1799 alternative : alternatives) {
            if (woodFamily(alternative).equals(family)) {
                refined.add(alternative);
            }
        }

        return refined.isEmpty() ? alternatives : List.copyOf(refined);
    }

    private static String woodFamily(class_1799 stack) {
        String path = itemPath(stack);
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

    private static String itemPath(class_1799 stack) {
        String id = ItemStackTexts.id(stack);
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
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
        private final List<class_1799> icons;
        private final List<String> alternatives;
        private final int maxStackSize;
        private int countPerCraft;

        private IngredientAccumulator(class_1799 icon, List<class_1799> icons, List<String> alternatives) {
            this.icon = icon.method_7972();
            this.icons = icons.stream().map(class_1799::method_7972).toList();
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
                    this.icons,
                    this.alternatives,
                    this.countPerCraft,
                    this.countPerCraft * craftsTotal,
                    this.countPerCraft * craftsMissing,
                    this.maxStackSize);
        }
    }
}
