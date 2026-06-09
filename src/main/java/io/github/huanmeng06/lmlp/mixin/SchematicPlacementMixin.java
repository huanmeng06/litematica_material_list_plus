package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import io.github.huanmeng06.lmlp.access.SchematicPlacementAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SchematicPlacement.class, remap = false)
public interface SchematicPlacementMixin extends SchematicPlacementAccess {
    @Override
    @Accessor("materialList")
    MaterialListBase lmlp$getMaterialList();

    @Override
    @Accessor("materialList")
    void lmlp$setMaterialList(MaterialListBase materialList);
}
