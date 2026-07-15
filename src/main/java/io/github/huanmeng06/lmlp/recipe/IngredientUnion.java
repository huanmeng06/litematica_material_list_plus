package io.github.huanmeng06.lmlp.recipe;

import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;

// Shared logic for merging equivalent entries across a choice group's
// alternatives (oak_planks -> oak_log, spruce_planks -> spruce_log, ...)
// into one union candidate list ("任意原木"). Used by both the recipe-tree
// builder (inline recipe panel) and the full recipe-detail screen's nested
// recipe expansion so a choice-group ingredient's own recipe reads as a
// choice group too, instead of collapsing to one representative item.
public final class IngredientUnion {
    private IngredientUnion() {
    }

    public static void addIngredient(IngredientSummary child, Map<String, ItemStack> icons, List<String> names) {
        addIcons(child.icons().isEmpty() ? List.of(child.icon()) : child.icons(), child.alternatives(), child.icon(), icons, names);
    }

    public static void addSlot(RecipeSlotSummary slot, Map<String, ItemStack> icons, List<String> names) {
        addIcons(slot.icons().isEmpty() ? List.of(slot.icon()) : slot.icons(), slot.alternatives(), slot.icon(), icons, names);
    }

    private static void addIcons(List<ItemStack> stacks, List<String> alternativeNames, ItemStack fallbackIcon, Map<String, ItemStack> icons, List<String> names) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            icons.putIfAbsent(ItemStackTexts.id(stack), stack.copy());
        }

        if (alternativeNames.isEmpty()) {
            String name = ItemStackTexts.name(fallbackIcon);
            if (!name.isEmpty() && !names.contains(name)) {
                names.add(name);
            }
            return;
        }

        for (String name : alternativeNames) {
            if (!names.contains(name)) {
                names.add(name);
            }
        }
    }
}
