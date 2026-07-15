package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.malilib.config.HudAlignment;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialHudRenderer;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import io.github.huanmeng06.lmlp.material.MaterialListHudState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MaterialListHudRenderer.class, remap = false)
public abstract class MaterialListHudRendererMixin {
    @Shadow
    @Final
    protected MaterialListBase materialList;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void lmlp$renderMinimalSubMaterialHud(GuiGraphicsExtractor drawContext, int xOffset, int yOffset, HudAlignment alignment, CallbackInfoReturnable<Integer> cir) {
        if (MinimalSubMaterialListView.isActive(this.materialList)) {
            cir.setReturnValue(MinimalSubMaterialHudRenderer.render(this.materialList, xOffset, yOffset, alignment, drawContext));
        }
    }

    @ModifyConstant(method = "render", constant = @Constant(longValue = 2000))
    private long lmlp$hudRefreshInterval(long interval) {
        return 1000L;
    }

    /**
     * @author Huan_meeng (lmlp)
     * @reason Format HUD item counts with the mod's configured count display style.
     */
    @Overwrite
    protected String getFormattedCountString(int count, int maxStackSize) {
        return CountFormatter.format(count, maxStackSize);
    }

    /**
     * @author Huan_meeng (lmlp)
     * @reason Share the HUD toggle per placement context so the state survives
     * the cache layer swapping in fresh MaterialListBase instances.
     */
    @Overwrite
    public boolean getShouldRenderCustom() {
        return MaterialListHudState.isEnabled(this.materialList);
    }

    /**
     * @author Huan_meeng (lmlp)
     * @reason Route the HUD toggle to the shared placement-scoped state.
     */
    @Overwrite
    public void toggleShouldRender() {
        MaterialListHudState.toggle(this.materialList);
    }
}
