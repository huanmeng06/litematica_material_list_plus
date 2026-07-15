package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import fi.dy.masa.malilib.render.GuiContext;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class MinimalSourceDetailScreen extends Screen {
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
    private static final int WHEEL_SCROLL_PIXELS = 36;

    private final Screen parent;
    private final ItemStack target;
    private final String targetName;
    private final int totalCount;
    private final int missingCount;
    private final List<MinimalSubMaterialListView.SourceContribution> sources;
    private final GuiScrollBar scrollBar = new GuiScrollBar();
    private final ButtonGeneric backButton = new ButtonGeneric(0, 0, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, "");
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private boolean draggingScrollbar;
    private double scrollRemainder;

    public MinimalSourceDetailScreen(Screen parent, ItemStack target, String targetName, int totalCount, int missingCount, List<MinimalSubMaterialListView.SourceContribution> sources) {
        super(Component.translatable("lmlp.gui.minimal_sources.title"));
        this.parent = parent;
        this.target = target.copy();
        this.targetName = targetName;
        this.totalCount = totalCount;
        this.missingCount = missingCount;
        this.sources = List.copyOf(sources);
        this.backButton.setDisplayString(StringUtils.translate("lmlp.label.recipe.back"));
        this.backButton.setTextCentered(true);
        this.backButton.setActionListener((button, mouseButton) -> this.onClose());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.offsetScroll(verticalAmount);
        return true;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.buttonInfo().button();
        if (button == 0 && this.backButton.onMouseClicked(event, doubleClick)) {
            return true;
        }

        if (button == 0 && this.scrollBar.wasMouseOver()) {
            this.scrollBar.setIsDragging(true);
            this.draggingScrollbar = true;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double deltaX, double deltaY) {
        int button = event.buttonInfo().button();
        return button == 0 && this.draggingScrollbar || super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        int button = event.buttonInfo().button();
        if (button == 0) {
            this.scrollBar.setIsDragging(false);
            this.draggingScrollbar = false;
        }

        return super.mouseReleased(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        GuiContext context = GuiContext.fromGuiGraphics(drawContext);
        RenderUtils.drawRect(context, 0, 0, this.width, this.height, SCREEN_BACKGROUND_COLOR);

        Layout layout = this.layout();
        int contentTop = layout.headerTop() + HEADER_HEIGHT + 12;
        int contentBottom = this.height - PAGE_BOTTOM_MARGIN;
        int viewportHeight = Math.max(0, contentBottom - contentTop);

        this.hoveredStack = ItemStack.EMPTY;
        this.scrollBar.setMaxValue(Math.max(0, this.contentHeight() - viewportHeight));
        this.updateBackButtonPosition(layout);

        this.renderTitle(context, layout.left());
        this.renderBackButton(context, mouseX, mouseY);
        this.renderTargetHeader(context, layout.left(), layout.headerTop(), layout.headerWidth(), HEADER_HEIGHT, mouseX, mouseY);

        int y = contentTop - this.scrollBar.getValue();
        context.enableScissor(layout.left() - OUTLINE_CLIP_PADDING, contentTop - OUTLINE_CLIP_PADDING, layout.left() + layout.contentWidth() + OUTLINE_CLIP_PADDING, contentBottom);
        if (this.sources.isEmpty()) {
            RenderUtils.drawOutlinedBox(context, layout.left(), y, layout.contentWidth(), SOURCE_BOX_HEIGHT, 0xDD000000, 0xFF777777);
            context.drawString(this.font, StringUtils.translate("lmlp.label.minimal_sources.none"), layout.left() + 10, y + 17, 0xFFFFCC66, false);
        } else {
            for (MinimalSubMaterialListView.SourceContribution source : this.sources) {
                this.renderSourceBox(context, source, layout.left(), y, layout.contentWidth(), mouseX, mouseY);
                y += SOURCE_BOX_HEIGHT + SOURCE_BOX_GAP;
            }
        }
        context.disableScissor();

        this.renderScrollbar(context, mouseX, mouseY, delta, contentTop, contentBottom);
        if (!this.hoveredStack.isEmpty()) {
            ItemTooltipRenderer.render(context, this.font, this.hoveredStack, mouseX, mouseY);
        }
    }

    private void renderTitle(GuiContext context, int left) {
        context.drawString(
                this.font,
                StringUtils.translate("lmlp.gui.title.recipe_detail_header", LitematicaMaterialListPlus.MOD_VERSION),
                left,
                TITLE_Y,
                0xFFFFFFFF,
                false);
    }

    private void renderTargetHeader(GuiContext context, int left, int top, int width, int height, int mouseX, int mouseY) {
        RenderUtils.drawOutlinedBox(context, left, top, width, height, 0xDD000000, 0xFF888888);
        context.renderItem(this.target, left + 10, top + 9);
        if (isInside(mouseX, mouseY, left + 10, top + 9, 16, 16)) {
            this.hoveredStack = this.target;
        }

        int textX = left + 36;
        int textRight = Math.max(textX + 1, left + width - 6);
        context.enableScissor(textX, top + 4, textRight, top + height - 4);
        context.drawString(this.font, this.targetName, textX, top + 8, 0xFFFFFFFF, false);

        String total = StringUtils.translate("lmlp.label.recipe.total_short") + ": " + CountFormatter.format(this.target, this.totalCount);
        String missing = StringUtils.translate("lmlp.label.recipe.missing_short") + ": " + CountFormatter.format(this.target, this.missingCount);
        String counts = total + "    " + missing;
        if (this.font.width(counts) <= textRight - textX) {
            context.drawString(this.font, counts, textX, top + 28, 0xFFAAAAAA, false);
        } else {
            context.drawString(this.font, total, textX, top + 23, 0xFFAAAAAA, false);
            context.drawString(this.font, missing, textX, top + 35, 0xFFAAAAAA, false);
        }
        context.disableScissor();
    }

    private void renderBackButton(GuiContext context, int mouseX, int mouseY) {
        this.backButton.render(context, mouseX, mouseY, false);
    }

    private void renderSourceBox(GuiContext context, MinimalSubMaterialListView.SourceContribution source, int left, int y, int width, int mouseX, int mouseY) {
        RenderUtils.drawOutlinedBox(context, left, y, width, SOURCE_BOX_HEIGHT, 0xDD000000, 0xFF777777);
        context.renderItem(source.icon(), left + 10, y + 10);
        if (isInside(mouseX, mouseY, left + 10, y + 10, 16, 16)) {
            this.hoveredStack = source.icon();
        }

        int textX = left + 34;
        int textRight = left + width - 10;
        context.enableScissor(textX, y + 6, textRight, y + SOURCE_BOX_HEIGHT - 4);
        context.drawString(this.font, source.name(), textX, y + 12, 0xFFFFFFFF, false);

        String countText = CountFormatter.format(source.totalCount(), source.maxStackSize());
        context.drawString(this.font, countText, textX, y + 28, 0xFFFFAA00, false);
        context.disableScissor();
    }

    private void renderScrollbar(GuiContext context, int mouseX, int mouseY, float delta, int top, int bottom) {
        if (this.scrollBar.getMaxValue() <= 0) {
            return;
        }

        this.scrollBar.render(context, mouseX, mouseY, delta, this.scrollbarX(), top, 8, bottom - top, this.contentHeight());
    }

    private void offsetScroll(double verticalAmount) {
        double target = this.scrollRemainder - verticalAmount * WHEEL_SCROLL_PIXELS;
        int pixels = (int) target;
        this.scrollRemainder = target - pixels;
        if (pixels == 0 && verticalAmount != 0.0D) {
            pixels = verticalAmount > 0.0D ? -1 : 1;
            this.scrollRemainder = 0.0D;
        }
        this.scrollBar.offsetValue(pixels);
    }

    private int contentHeight() {
        if (this.sources.isEmpty()) {
            return SOURCE_BOX_HEIGHT;
        }

        return this.sources.size() * SOURCE_BOX_HEIGHT + Math.max(0, this.sources.size() - 1) * SOURCE_BOX_GAP;
    }

    private int scrollbarX() {
        return this.width - 18;
    }

    private Layout layout() {
        int left = PAGE_MARGIN_X;
        int availableWidth = Math.max(1, this.width - PAGE_MARGIN_X * 2);
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
