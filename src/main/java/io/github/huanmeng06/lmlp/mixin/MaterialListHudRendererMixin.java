package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.malilib.config.HudAlignment;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialHudRenderer;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MaterialListHudRenderer.class, remap = false)
public abstract class MaterialListHudRendererMixin {
    @Shadow
    @Final
    protected MaterialListBase materialList;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void lmlp$renderMinimalSubMaterialHud(int xOffset, int yOffset, HudAlignment alignment, class_332 drawContext, CallbackInfoReturnable<Integer> cir) {
        if (MinimalSubMaterialListView.isActive(this.materialList)) {
            cir.setReturnValue(MinimalSubMaterialHudRenderer.render(this.materialList, xOffset, yOffset, alignment, drawContext));
        }
    }

    /**
     * @author Huan_meeng (lmlp)
     * @reason Format HUD item counts with the mod's configured count display style.
     */
    @Overwrite
    protected String getFormattedCountString(int count, int maxStackSize) {
        return CountFormatter.format(count, maxStackSize);
    }
}
