package io.github.huanmeng06.lmlp.recipe;

import java.util.List;

import net.minecraft.class_1799;

public final class RecipeSlotSummary {
    public static final RecipeSlotSummary EMPTY = new RecipeSlotSummary(class_1799.field_8037, List.of(), 0);

    private final class_1799 icon;
    private final List<class_1799> icons;
    private final List<String> alternatives;
    private final int count;

    public RecipeSlotSummary(class_1799 icon, List<String> alternatives, int count) {
        this(icon, List.of(icon), alternatives, count);
    }

    public RecipeSlotSummary(class_1799 icon, List<class_1799> icons, List<String> alternatives, int count) {
        this.icon = icon;
        this.icons = copyIcons(icons);
        this.alternatives = List.copyOf(alternatives);
        this.count = count;
    }

    public class_1799 icon() {
        return this.icon;
    }

    public List<class_1799> icons() {
        return this.icons;
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

    private static List<class_1799> copyIcons(List<class_1799> icons) {
        if (icons.isEmpty()) {
            return List.of();
        }

        return icons.stream()
                .filter(icon -> !icon.method_7960())
                .map(class_1799::method_7972)
                .toList();
    }
}
