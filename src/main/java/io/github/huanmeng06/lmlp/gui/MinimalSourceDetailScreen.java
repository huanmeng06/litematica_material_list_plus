package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;

import java.util.List;

public class MinimalSourceDetailScreen extends class_437 {
    private static final int BACK_BUTTON_WIDTH = 112;
    private static final int BACK_BUTTON_HEIGHT = 20;
    private static final int SCREEN_BACKGROUND_COLOR = 0xB0000000;
    private static final int OUTLINE_CLIP_PADDING = 2;
    private static final int TITLE_Y = 10;
    private static final int TITLE_HEIGHT = 9;
    private static final int TITLE_CONTENT_GAP = 10;
    private static final int CONTENT_X = 20;
    private static final int PAGE_MARGIN_X = CONTENT_X;
    private static final int PAGE_TOP = TITLE_Y + TITLE_HEIGHT + TITLE_CONTENT_GAP;
    private static final int PAGE_BOTTOM_MARGIN = 20;
    private static final int CONTENT_RIGHT_INSET = 24;
    private static final int HEADER_MAX_WIDTH = 600;
    private static final int HEADER_HEIGHT = 50;
    private static final int HEADER_BUTTON_GAP = 8;
    private static final int SOURCE_BOX_HEIGHT = 46;
    private static final int SOURCE_BOX_GAP = 10;

    private final class_437 parent;
    private final class_1799 target;
    private final String targetName;
    private final int totalCount;
    private final int missingCount;
    private final List<MinimalSubMaterialListView.SourceContribution> sources;
    private final GuiScrollBar scrollBar = new GuiScrollBar();
    private final ButtonGeneric backButton = new ButtonGeneric(0, 0, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, "");
    private class_1799 hoveredStack = class_1799.field_8037;
    private boolean draggingScrollbar;

    public MinimalSourceDetailScreen(class_437 parent, class_1799 target, String targetName, int totalCount, int missingCount, List<MinimalSubMaterialListView.SourceContribution> sources) {
        super(class_2561.method_43471("lmlp.gui.minimal_sources.title"));
        this.parent = parent;
        this.target = target.method_7972();
        this.targetName = targetName;
        this.totalCount = totalCount;
        this.missingCount = missingCount;
        this.sources = List.copyOf(sources);
        this.backButton.setDisplayString(StringUtils.translate("lmlp.label.recipe.back"));
        this.backButton.setTextCentered(true);
        this.backButton.setActionListener((button, mouseButton) -> this.method_25419());
    }

    @Override
    public void method_25419() {
        this.field_22787.method_1507(this.parent);
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scrollBar.offsetValue(-(int) (verticalAmount * 24));
        return true;
    }

    @Override
    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (button == 0 && this.backButton.onMouseClicked((int) mouseX, (int) mouseY, button)) {
            return true;
        }

        if (button == 0 && this.scrollBar.wasMouseOver()) {
            this.scrollBar.setIsDragging(true);
            this.draggingScrollbar = true;
            return true;
        }

        return super.method_25402(mouseX, mouseY, button);
    }

    @Override
    public boolean method_25403(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return button == 0 && this.draggingScrollbar || super.method_25403(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean method_25406(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.scrollBar.setIsDragging(false);
            this.draggingScrollbar = false;
        }

        return super.method_25406(mouseX, mouseY, button);
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        RenderUtils.drawRect(0, 0, this.field_22789, this.field_22790, SCREEN_BACKGROUND_COLOR);

        Layout layout = this.layout();
        int contentTop = layout.headerTop() + HEADER_HEIGHT + 12;
        int contentBottom = this.field_22790 - PAGE_BOTTOM_MARGIN;
        int viewportHeight = Math.max(0, contentBottom - contentTop);

        this.hoveredStack = class_1799.field_8037;
        this.scrollBar.setMaxValue(Math.max(0, this.contentHeight() - viewportHeight));
        this.updateBackButtonPosition(layout);

        this.renderTitle(context, layout.left());
        this.renderBackButton(context, mouseX, mouseY);
        this.renderTargetHeader(context, layout.left(), layout.headerTop(), layout.headerWidth(), HEADER_HEIGHT, mouseX, mouseY);

        int y = contentTop - this.scrollBar.getValue();
        context.method_44379(layout.left() - OUTLINE_CLIP_PADDING, contentTop - OUTLINE_CLIP_PADDING, layout.left() + layout.contentWidth() + OUTLINE_CLIP_PADDING, contentBottom);
        if (this.sources.isEmpty()) {
            RenderUtils.drawOutlinedBox(layout.left(), y, layout.contentWidth(), SOURCE_BOX_HEIGHT, 0xDD000000, 0xFF777777);
            context.method_51433(this.field_22793, StringUtils.translate("lmlp.label.minimal_sources.none"), layout.left() + 10, y + 17, 0xFFFFCC66, false);
        } else {
            for (MinimalSubMaterialListView.SourceContribution source : this.sources) {
                this.renderSourceBox(context, source, layout.left(), y, layout.contentWidth(), mouseX, mouseY);
                y += SOURCE_BOX_HEIGHT + SOURCE_BOX_GAP;
            }
        }
        context.method_44380();

        this.renderScrollbar(context, mouseX, mouseY, delta, contentTop, contentBottom);
        if (!this.hoveredStack.method_7960()) {
            context.method_51446(this.field_22793, this.hoveredStack, mouseX, mouseY);
        }
    }

    private void renderTitle(class_332 context, int left) {
        context.method_51433(
                this.field_22793,
                StringUtils.translate("lmlp.gui.title.recipe_detail_header", LitematicaMaterialListPlus.MOD_VERSION),
                left,
                TITLE_Y,
                0xFFFFFFFF,
                false);
    }

    private void renderTargetHeader(class_332 context, int left, int top, int width, int height, int mouseX, int mouseY) {
        RenderUtils.drawOutlinedBox(left, top, width, height, 0xDD000000, 0xFF888888);
        context.method_51427(this.target, left + 10, top + 9);
        if (isInside(mouseX, mouseY, left + 10, top + 9, 16, 16)) {
            this.hoveredStack = this.target;
        }

        int textX = left + 36;
        int textRight = Math.max(textX + 1, left + width - 6);
        context.method_44379(textX, top + 4, textRight, top + height - 4);
        context.method_51433(this.field_22793, this.targetName, textX, top + 8, 0xFFFFFFFF, false);

        String total = StringUtils.translate("lmlp.label.recipe.total_short") + ": " + CountFormatter.format(this.target, this.totalCount);
        String missing = StringUtils.translate("lmlp.label.recipe.missing_short") + ": " + CountFormatter.format(this.target, this.missingCount);
        String counts = total + "    " + missing;
        if (this.field_22793.method_1727(counts) <= textRight - textX) {
            context.method_51433(this.field_22793, counts, textX, top + 28, 0xFFAAAAAA, false);
        } else {
            context.method_51433(this.field_22793, total, textX, top + 23, 0xFFAAAAAA, false);
            context.method_51433(this.field_22793, missing, textX, top + 35, 0xFFAAAAAA, false);
        }
        context.method_44380();
    }

    private void renderBackButton(class_332 context, int mouseX, int mouseY) {
        this.backButton.render(mouseX, mouseY, false, context);
    }

    private void renderSourceBox(class_332 context, MinimalSubMaterialListView.SourceContribution source, int left, int y, int width, int mouseX, int mouseY) {
        RenderUtils.drawOutlinedBox(left, y, width, SOURCE_BOX_HEIGHT, 0xDD000000, 0xFF777777);
        context.method_51427(source.icon(), left + 10, y + 10);
        if (isInside(mouseX, mouseY, left + 10, y + 10, 16, 16)) {
            this.hoveredStack = source.icon();
        }

        int textX = left + 34;
        int textRight = left + width - 10;
        context.method_44379(textX, y + 6, textRight, y + SOURCE_BOX_HEIGHT - 4);
        context.method_51433(this.field_22793, source.name(), textX, y + 12, 0xFFFFFFFF, false);

        String countText = CountFormatter.format(source.totalCount(), source.maxStackSize());
        if (source.missingCount() != source.totalCount()) {
            countText += " / " + CountFormatter.format(source.missingCount(), source.maxStackSize());
        }
        context.method_51433(this.field_22793, countText, textX, y + 28, 0xFFFFAA00, false);
        context.method_44380();
    }

    private void renderScrollbar(class_332 context, int mouseX, int mouseY, float delta, int top, int bottom) {
        if (this.scrollBar.getMaxValue() <= 0) {
            return;
        }

        this.scrollBar.render(mouseX, mouseY, delta, this.scrollbarX(), top, 8, bottom - top, this.contentHeight());
    }

    private int contentHeight() {
        if (this.sources.isEmpty()) {
            return SOURCE_BOX_HEIGHT;
        }

        return this.sources.size() * SOURCE_BOX_HEIGHT + Math.max(0, this.sources.size() - 1) * SOURCE_BOX_GAP;
    }

    private int scrollbarX() {
        return this.field_22789 - 18;
    }

    private Layout layout() {
        int left = PAGE_MARGIN_X;
        int availableWidth = Math.max(1, this.field_22789 - PAGE_MARGIN_X * 2);
        int contentWidth = Math.max(1, availableWidth - CONTENT_RIGHT_INSET);
        int contentRight = left + contentWidth;
        int backButtonX = Math.max(left, contentRight - BACK_BUTTON_WIDTH);
        int headerWidth = Math.min(HEADER_MAX_WIDTH, Math.max(1, backButtonX - left - HEADER_BUTTON_GAP));
        return new Layout(left, PAGE_TOP, contentWidth, headerWidth, backButtonX);
    }

    private void updateBackButtonPosition(Layout layout) {
        this.backButton.setPosition(layout.backButtonX(), layout.headerTop());
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Layout(int left, int headerTop, int contentWidth, int headerWidth, int backButtonX) {
    }
}
