package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = fi.dy.masa.litematica.materials.MaterialListPlacement.class, remap = false)
public abstract class MaterialListPlacementMixin implements MaterialListPlacementAccess {
    @Shadow
    @Final
    private SchematicPlacement placement;

    @Override
    public SchematicPlacement lmlp$getPlacement() {
        return this.placement;
    }
}
