package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.WoodFamily;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

/** Clips a standard MaLiLib config row while its owning preference group expands or collapses. */
final class AnimatedPreferenceConfigOption extends WidgetConfigOption {
    private static final int ICON_SIZE = 18;
    private static final int ICON_BACKGROUND = 0xFF555555;
    private static final int ICON_BORDER = 0xFFAAAAAA;

    private final int fullHeight;
    private final int valueX;
    private final int configWidth;

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
        this.valueX = x + maxLabelWidth + 10;
        this.configWidth = configWidth;
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
        this.renderSelectedWoodIcon(context);
        context.method_44380();
    }

    private void renderSelectedWoodIcon(GuiContext context) {
        if (this.wrapper.getConfig() != Configs.ConfigForms.PREFERRED_WOOD_FAMILY) {
            return;
        }

        WoodFamily family = (WoodFamily) Configs.ConfigForms.PREFERRED_WOOD_FAMILY.getOptionListValue();
        class_2960 itemId = class_2960.method_60654(RestrictedJeiOptionListConfigs.representativeItemId(family));
        class_1792 item = class_7923.field_41178.method_17966(itemId).orElse(null);
        if (item == null) {
            return;
        }

        int resetWidth = this.textRenderer.method_1727(StringUtils.translate("malilib.gui.button.reset.caps")) + 10;
        int iconX = this.valueX + this.configWidth + 2 + resetWidth + 4;
        int iconY = this.getY() + 2;
        RenderUtils.drawOutlinedBox(
                context,
                iconX,
                iconY,
                ICON_SIZE,
                ICON_SIZE,
                ICON_BACKGROUND,
                ICON_BORDER
        );
        context.method_51427(new class_1799(item), iconX + 1, iconY + 1);
    }
}
