package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ConfigButtonOptionList;
import fi.dy.masa.malilib.util.GuiUtils;
import io.github.huanmeng06.lmlp.gui.RestrictedJeiOptionListConfigs;
import io.github.huanmeng06.lmlp.gui.textlist.GuiItemIdStringListEdit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ConfigButtonOptionList.class, remap = false)
public abstract class ConfigButtonOptionListMixin {
    @Shadow @Final private IConfigOptionList config;

    @Inject(method = "onMouseClickedImpl", at = @At("HEAD"), cancellable = true)
    private void lmlp$openRestrictedJeiPicker(
            int mouseX,
            int mouseY,
            int mouseButton,
            CallbackInfoReturnable<Boolean> cir) {
        RestrictedJeiOptionListConfigs.Definition definition = RestrictedJeiOptionListConfigs.find(this.config);
        if (definition == null) {
            return;
        }

        ConfigButtonOptionList button = (ConfigButtonOptionList) (Object) this;
        GuiItemIdStringListEdit picker = GuiItemIdStringListEdit.createRestrictedPicker(
                definition.allowedItemIds(),
                itemId -> {
                    if (definition.select(itemId)) {
                        button.updateDisplayString();
                    }
                },
                GuiUtils.getCurrentScreen()
        );
        GuiBase.openGui(picker);
        cir.setReturnValue(true);
    }
}
