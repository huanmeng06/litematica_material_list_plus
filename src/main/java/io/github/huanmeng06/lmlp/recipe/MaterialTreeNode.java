package io.github.huanmeng06.lmlp.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class MaterialTreeNode {
    private final String path;
    private final ItemStack icon;
    private final List<ItemStack> icons;
    private final List<String> alternatives;
    private final String name;
    private final int totalCount;
    private final int missingCount;
    private final int maxStackSize;
    private final int depth;
    private final List<MaterialTreeNode> children;

    public MaterialTreeNode(String path, ItemStack icon, String name, int totalCount, int missingCount, int maxStackSize, int depth, List<MaterialTreeNode> children) {
        this(path, icon, List.of(icon), List.of(), name, totalCount, missingCount, maxStackSize, depth, children);
    }

    public MaterialTreeNode(String path, ItemStack icon, List<ItemStack> icons, String name, int totalCount, int missingCount, int maxStackSize, int depth, List<MaterialTreeNode> children) {
        this(path, icon, icons, List.of(), name, totalCount, missingCount, maxStackSize, depth, children);
    }

    public MaterialTreeNode(String path, ItemStack icon, List<ItemStack> icons, List<String> alternatives, String name, int totalCount, int missingCount, int maxStackSize, int depth, List<MaterialTreeNode> children) {
        this.path = path;
        this.icon = icon;
        this.icons = copyIcons(icons);
        this.alternatives = List.copyOf(alternatives);
        this.name = name;
        this.totalCount = totalCount;
        this.missingCount = missingCount;
        this.maxStackSize = maxStackSize;
        this.depth = depth;
        this.children = List.copyOf(children);
    }

    public String path() {
        return this.path;
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

    public String name() {
        return this.name;
    }

    public int totalCount() {
        return this.totalCount;
    }

    public int missingCount() {
        return this.missingCount;
    }

    public int maxStackSize() {
        return this.maxStackSize;
    }

    public int depth() {
        return this.depth;
    }

    public List<MaterialTreeNode> children() {
        return this.children;
    }

    public boolean hasChildren() {
        return !this.children.isEmpty();
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
