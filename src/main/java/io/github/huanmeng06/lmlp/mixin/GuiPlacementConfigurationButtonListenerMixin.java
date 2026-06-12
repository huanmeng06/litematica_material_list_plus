package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration.ButtonListener.Type;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "fi.dy.masa.litematica.gui.GuiPlacementConfiguration$ButtonListener", remap = false)
public abstract class GuiPlacementConfigurationButtonListenerMixin {
    @Shadow
    @Final
    public GuiPlacementConfiguration parent;

    @Shadow
    @Final
    public SchematicPlacement placement;

    @Shadow
    @Final
    public Type type;

    @Inject(method = "actionPerformedWithButton", at = @At("HEAD"), cancellable = true)
    private void lmlp$openMaterialListThroughResolver(ButtonBase button, int mouseButton, CallbackInfo ci) {
        if (this.type != Type.OPEN_MATERIAL_LIST_GUI) {
            return;
        }

        ChunkMissingMaterialListCache.rememberPlacementContext(this.placement, "placement_config.open_material_list");
        MaterialListBase materialList = ChunkMissingMaterialListCache.refreshForPlacementState(this.placement, null);
        GuiMaterialList gui = new GuiMaterialList(materialList);
        gui.setParent(this.parent);
        GuiBase.openGui(gui);
        this.parent.initGui();
        ci.cancel();
    }
}
