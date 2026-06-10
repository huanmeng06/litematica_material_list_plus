package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiSchematicLoadedList;
import fi.dy.masa.litematica.gui.widgets.WidgetListLoadedSchematics;
import io.github.huanmeng06.lmlp.gui.KnownLoadedSchematicsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiSchematicLoadedList.class, remap = false)
public abstract class GuiSchematicLoadedListMixin {
    @Shadow
    protected abstract int getBrowserWidth();

    @Shadow
    protected abstract int getBrowserHeight();

    @Inject(method = "createListWidget(II)Lfi/dy/masa/litematica/gui/widgets/WidgetListLoadedSchematics;", at = @At("HEAD"), cancellable = true)
    private void lmlp$createKnownLoadedSchematicsList(int listX, int listY, CallbackInfoReturnable<WidgetListLoadedSchematics> cir) {
        cir.setReturnValue(new KnownLoadedSchematicsList(
                listX,
                listY,
                this.getBrowserWidth(),
                this.getBrowserHeight(),
                (GuiSchematicLoadedList) (Object) this));
    }
}
