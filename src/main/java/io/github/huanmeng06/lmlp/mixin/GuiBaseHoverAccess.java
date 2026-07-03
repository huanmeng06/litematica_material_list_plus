package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GuiBase.class, remap = false)
public interface GuiBaseHoverAccess {
    @Accessor("hoveredWidget")
    void lmlp$setHoveredWidget(WidgetBase hoveredWidget);
}
