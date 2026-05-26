package io.github.huanmeng06.lmlp.recipe;

import java.util.List;

import net.minecraft.class_1799;

public final class IngredientSummary {
    private final class_1799 icon;
    private final List<String> alternatives;
    private final int countPerCraft;
    private final int countTotal;
    private final int countMissing;
    private final int maxStackSize;

    public IngredientSummary(class_1799 icon, List<String> alternatives, int countPerCraft, int countTotal, int countMissing, int maxStackSize) {
        this.icon = icon;
        this.alternatives = List.copyOf(alternatives);
        this.countPerCraft = countPerCraft;
        this.countTotal = countTotal;
        this.countMissing = countMissing;
        this.maxStackSize = maxStackSize;
    }

    public class_1799 icon() {
        return this.icon;
    }

    public List<String> alternatives() {
        return this.alternatives;
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
}
