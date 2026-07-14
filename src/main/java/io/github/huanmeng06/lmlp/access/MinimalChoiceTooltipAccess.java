package io.github.huanmeng06.lmlp.access;

import fi.dy.masa.malilib.render.GuiContext;

public interface MinimalChoiceTooltipAccess {
    boolean lmlp$renderMinimalChoiceTooltip(GuiContext drawContext, int mouseX, int mouseY);

    boolean lmlp$renderPanelItemTooltip(GuiContext drawContext, int mouseX, int mouseY);
}
