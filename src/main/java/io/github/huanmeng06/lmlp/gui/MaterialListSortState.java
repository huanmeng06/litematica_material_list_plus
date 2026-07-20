package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class MaterialListSortState {
    private static final Map<MaterialListBase, Boolean> COMPATIBLE_SORTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private MaterialListSortState() {
    }

    public static boolean isCompatibleSort(MaterialListBase materialList) {
        return materialList != null && COMPATIBLE_SORTS.containsKey(materialList);
    }

    public static void setCompatibleSort(MaterialListBase materialList, boolean enabled) {
        if (enabled) {
            COMPATIBLE_SORTS.put(materialList, Boolean.TRUE);
        } else {
            COMPATIBLE_SORTS.remove(materialList);
        }
    }
}
