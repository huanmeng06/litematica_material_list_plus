package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ConfigButtonStringList;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.util.GuiUtils;
import io.github.huanmeng06.lmlp.gui.textlist.GuiItemIdStringListEdit;
import io.github.huanmeng06.lmlp.gui.textlist.ItemIdStringListConfigs;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ConfigButtonStringList.class, remap = false)
public abstract class ConfigButtonStringListMixin {
    @Shadow @Final private IConfigStringList config;
    @Shadow @Final private IConfigGui configGui;
    @Shadow @Final private IDialogHandler dialogHandler;

    @Inject(method = "onMouseClickedImpl", at = @At("HEAD"), cancellable = true)
    private void lmlp$openItemIdStringListEditor(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!ItemIdStringListConfigs.isSupported(this.config)) {
            return;
        }

        GuiItemIdStringListEdit gui = new GuiItemIdStringListEdit(
                this.config,
                this.configGui,
                this.dialogHandler,
                this.dialogHandler == null ? GuiUtils.getCurrentScreen() : null
        );

        if (this.dialogHandler != null) {
            this.dialogHandler.openDialog(gui);
        } else {
            GuiBase.openGui(gui);
        }
        cir.setReturnValue(true);
    }
}
