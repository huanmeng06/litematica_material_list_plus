package io.github.huanmeng06.lmlp.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class IngredientSummary {
    private final ItemStack icon;
    private final List<ItemStack> icons;
    private final List<String> alternatives;
    private final int countPerCraft;
    private final int countTotal;
    private final int countMissing;
    private final int maxStackSize;

    public IngredientSummary(ItemStack icon, List<String> alternatives, int countPerCraft, int countTotal, int countMissing, int maxStackSize) {
        this(icon, List.of(icon), alternatives, countPerCraft, countTotal, countMissing, maxStackSize);
    }

    public IngredientSummary(ItemStack icon, List<ItemStack> icons, List<String> alternatives, int countPerCraft, int countTotal, int countMissing, int maxStackSize) {
        this.icon = icon;
        this.icons = copyIcons(icons);
        this.alternatives = List.copyOf(alternatives);
        this.countPerCraft = countPerCraft;
        this.countTotal = countTotal;
        this.countMissing = countMissing;
        this.maxStackSize = maxStackSize;
    }

    public ItemStack icon() {
        return this.icon;
    }

    public List<ItemStack> icons() {
        return this.icons;
    }

    public List<String> alternatives() {
        return this.alternatives;
    }

    public boolean isChoiceGroup() {
        return this.icons.size() > 1 || this.alternatives.size() > 1;
    }

    public int countPerCraft() {
        return this.countPerCraft;
    }

    public int countTotal() {
        return this.countTotal;
    }

    public int countMissing() {
        return this.countMissing;
    }

    public int maxStackSize() {
        return this.maxStackSize;
    }

    private static List<ItemStack> copyIcons(List<ItemStack> icons) {
        if (icons.isEmpty()) {
            return List.of();
        }

        return icons.stream()
                .filter(icon -> !icon.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }
}
