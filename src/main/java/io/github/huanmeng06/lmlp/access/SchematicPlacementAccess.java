package io.github.huanmeng06.lmlp.access;

import fi.dy.masa.litematica.materials.MaterialListBase;

public interface SchematicPlacementAccess {
    MaterialListBase lmlp$getMaterialList();

    void lmlp$setMaterialList(MaterialListBase materialList);
}
