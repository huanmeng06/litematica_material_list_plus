package io.github.huanmeng06.lmlp.recipe;

import java.util.List;
import java.util.Map;

import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import net.minecraft.class_1799;

// Shared logic for merging equivalent entries across a choice group's
// alternatives (oak_planks -> oak_log, spruce_planks -> spruce_log, ...)
// into one union candidate list ("任意原木"). Used by both the recipe-tree
// builder (inline recipe panel) and the full recipe-detail screen's nested
// recipe expansion so a choice-group ingredient's own recipe reads as a
// choice group too, instead of collapsing to one representative item.
public final class IngredientUnion {
    private IngredientUnion() {
    }

    public static void addIngredient(IngredientSummary child, Map<String, class_1799> icons, List<String> names) {
        addIcons(child.icons().isEmpty() ? List.of(child.icon()) : child.icons(), child.alternatives(), child.icon(), icons, names);
    }

    public static void addSlot(RecipeSlotSummary slot, Map<String, class_1799> icons, List<String> names) {
        addIcons(slot.icons().isEmpty() ? List.of(slot.icon()) : slot.icons(), slot.alternatives(), slot.icon(), icons, names);
    }

    private static void addIcons(List<class_1799> stacks, List<String> alternativeNames, class_1799 fallbackIcon, Map<String, class_1799> icons, List<String> names) {
        for (class_1799 stack : stacks) {
            if (stack.method_7960()) {
                continue;
            }
            icons.putIfAbsent(ItemStackTexts.id(stack), stack.method_7972());
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
