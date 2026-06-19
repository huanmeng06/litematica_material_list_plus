package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.gui.SliderCallbackInteger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SliderCallbackInteger.class, remap = false)
public interface SliderCallbackIntegerAccess {
    @Accessor("config")
    IConfigInteger lmlp$getConfig();
}