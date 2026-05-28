package io.github.huanmeng06.lmlp.recipe;

import java.util.List;

import net.minecraft.class_1799;

public final class MaterialTreeNode {
    private final String path;
    private final class_1799 icon;
    private final String name;
    private final int totalCount;
    private final int missingCount;
    private final int maxStackSize;
    private final int depth;
    private final List<MaterialTreeNode> children;

    public MaterialTreeNode(String path, class_1799 icon, String name, int totalCount, int missingCount, int maxStackSize, int depth, List<MaterialTreeNode> children) {
        this.path = path;
        this.icon = icon;
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

    public class_1799 icon() {
        return this.icon;
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
}
