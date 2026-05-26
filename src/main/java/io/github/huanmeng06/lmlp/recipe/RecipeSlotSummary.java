package io.github.huanmeng06.lmlp.recipe;

import java.util.List;

import net.minecraft.class_1799;

public final class RecipeSlotSummary {
    public static final RecipeSlotSummary EMPTY = new RecipeSlotSummary(class_1799.field_8037, List.of(), 0);

    private final class_1799 icon;
    private final List<String> alternatives;
    private final int count;

    public RecipeSlotSummary(class_1799 icon, List<String> alternatives, int count) {
        this.icon = icon;
        this.alternatives = List.copyOf(alternatives);
        this.count = count;
    }

    public class_1799 icon() {
        return this.icon;
    }

    public List<String> alternatives() {
        return this.alternatives;
    }

    public int count() {
        return this.count;
    }

    public boolean isEmpty() {
        return this.icon.method_7960() || this.count <= 0;
    }
}
