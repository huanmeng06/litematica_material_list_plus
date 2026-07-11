package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.gui.SliderCallbackInteger;
import fi.dy.masa.malilib.gui.interfaces.ISliderCallback;
import fi.dy.masa.malilib.gui.widgets.WidgetSlider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WidgetSlider.class, remap = false)
public abstract class WidgetSliderMixin {
    private static final int ORIGIN_MARKER_TEXT_SCALE_SLIDER_WIDTH = 8;

    @Shadow
    protected int sliderWidth;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lmlp$useCompactOriginMarkerTextScaleHandle(int x, int y, int width, int height,
                                                            ISliderCallback callback, CallbackInfo ci) {
        if (!(callback instanceof SliderCallbackInteger)) {
            return;
        }

        IConfigInteger config = ((SliderCallbackIntegerAccess) callback).lmlp$getConfig();
        if (config != null && "originMarkerTextScale".equals(config.getName())) {
            this.sliderWidth = ORIGIN_MARKER_TEXT_SCALE_SLIDER_WIDTH;
        }
    }
}