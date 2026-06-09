package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.malilib.gui.button.ButtonBase;
import io.github.huanmeng06.lmlp.access.ButtonBaseAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ButtonBase.class, remap = false)
public interface ButtonBaseMixin extends ButtonBaseAccess {
    @Override
    @Accessor("displayString")
    String lmlp$getDisplayString();
}
