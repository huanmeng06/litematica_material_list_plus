package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;

public final class MaterialListDefaultSort {
    private MaterialListDefaultSort() {
    }

    public static void apply(MaterialListBase materialList) {
        if (materialList.getSortCriteria() != MaterialListBase.SortCriteria.COUNT_MISSING
                || materialList.getSortInReverse()) {
            materialList.setSortCriteria(MaterialListBase.SortCriteria.COUNT_MISSING);
        }
    }
}
