package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.render.GuiContext;

/** Clips a standard MaLiLib config row while its owning preference group expands or collapses. */
final class AnimatedPreferenceConfigOption extends WidgetConfigOption {
    private final int fullHeight;

    AnimatedPreferenceConfigOption(
            int x,
            int y,
            int width,
            int fullHeight,
            int visibleHeight,
            int maxLabelWidth,
            int configWidth,
            GuiConfigsBase.ConfigOptionWrapper wrapper,
            int listIndex,
            IKeybindConfigGui host,
            WidgetListConfigOptionsBase<?, ?> parentList) {
        super(x, y, width, fullHeight, maxLabelWidth, configWidth, wrapper, listIndex, host, parentList);
        this.fullHeight = fullHeight;
        this.setHeight(visibleHeight);
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return this.getHeight() >= this.fullHeight && super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void render(GuiContext context, int mouseX, int mouseY, boolean selected) {
        if (this.getHeight() <= 0) {
            return;
        }

        context.method_44379(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
        super.render(context, mouseX, mouseY, selected);
        context.method_44380();
    }
}
