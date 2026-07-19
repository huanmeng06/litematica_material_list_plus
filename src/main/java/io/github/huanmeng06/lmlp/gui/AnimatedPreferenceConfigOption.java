package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.config.IConfigOptionList;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

/** Clips a standard MaLiLib config row while its owning preference group expands or collapses. */
final class AnimatedPreferenceConfigOption extends WidgetConfigOption {
    private static final int ICON_SIZE = 16;
    private static final int ICON_BACKGROUND = 0xFF555555;
    private static final int ICON_BORDER = 0xFFAAAAAA;

    private final int fullHeight;
    private final int configVisibleHeight;
    private final int valueX;
    private final int configWidth;
    private final IKeybindConfigGui host;

    AnimatedPreferenceConfigOption(
            int x,
            int y,
            int width,
            int fullHeight,
            int visibleHeight,
            int detailHeight,
            int maxLabelWidth,
            int configWidth,
            GuiConfigsBase.ConfigOptionWrapper wrapper,
            int listIndex,
            IKeybindConfigGui host,
            WidgetListConfigOptionsBase<?, ?> parentList) {
        super(x, y, width, fullHeight, maxLabelWidth, configWidth, wrapper, listIndex, host, parentList);
        this.fullHeight = fullHeight;
        this.configVisibleHeight = visibleHeight;
        this.valueX = x + maxLabelWidth + 10;
        this.configWidth = configWidth;
        this.host = host;
        this.setHeight(visibleHeight + detailHeight);
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return this.configVisibleHeight >= this.fullHeight
                && mouseY < this.getY() + this.fullHeight
                && super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void render(GuiContext context, int mouseX, int mouseY, boolean selected) {
        if (this.getHeight() <= 0 || this.configVisibleHeight <= 0) {
            return;
        }

        context.method_44379(
                this.getX(),
                this.getY(),
                this.getX() + this.getWidth(),
                this.getY() + this.configVisibleHeight);
        super.render(context, mouseX, mouseY, selected);
        this.renderSelectedMaterialIcon(context);
        context.method_44380();

        if (this.host instanceof GuiPreferredMaterialForm form) {
            form.renderInlinePreferenceContent(
                    this.wrapper.getConfig(),
                    context,
                    this.getX(),
                    this.getY(),
                    this.getWidth(),
                    this.configVisibleHeight,
                    mouseX,
                    mouseY);
        }
    }

    private void renderSelectedMaterialIcon(GuiContext context) {
        if (!(this.wrapper.getConfig() instanceof IConfigOptionList optionList)) {
            return;
        }

        RestrictedJeiOptionListConfigs.Definition definition = RestrictedJeiOptionListConfigs.find(optionList);
        if (definition == null || definition.selectedItemId().isEmpty()) {
            return;
        }
        class_2960 itemId = class_2960.method_60654(definition.selectedItemId());
        class_1792 item = class_7923.field_41178.method_17966(itemId).orElse(null);
        if (item == null) {
            return;
        }

        int resetWidth = this.textRenderer.method_1727(StringUtils.translate("malilib.gui.button.reset.caps")) + 10;
        int iconX = this.valueX + this.configWidth + 2 + resetWidth + 4;
        int iconY = this.getY() + 3;
        RenderUtils.drawOutlinedBox(
                context,
                iconX,
                iconY,
                ICON_SIZE,
                ICON_SIZE,
                ICON_BACKGROUND,
                ICON_BORDER
        );
        context.method_51427(new class_1799(item), iconX, iconY);
    }
}
