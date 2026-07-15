package io.github.huanmeng06.lmlp.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class RecipeSlotSummary {
    public static final RecipeSlotSummary EMPTY = new RecipeSlotSummary(ItemStack.EMPTY, List.of(), 0);

    private final ItemStack icon;
    private final List<ItemStack> icons;
    private final List<String> alternatives;
    private final int count;

    public RecipeSlotSummary(ItemStack icon, List<String> alternatives, int count) {
        this(icon, List.of(icon), alternatives, count);
    }

    public RecipeSlotSummary(ItemStack icon, List<ItemStack> icons, List<String> alternatives, int count) {
        this.icon = icon;
        this.icons = copyIcons(icons);
        this.alternatives = List.copyOf(alternatives);
        this.count = count;
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

    public int count() {
        return this.count;
    }

    public boolean isEmpty() {
        return this.icon.isEmpty() || this.count <= 0;
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
