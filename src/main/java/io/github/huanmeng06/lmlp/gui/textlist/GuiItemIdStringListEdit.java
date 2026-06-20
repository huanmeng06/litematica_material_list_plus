package io.github.huanmeng06.lmlp.gui.textlist;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.gui.ItemTooltipRenderer;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GuiItemIdStringListEdit extends GuiListBase<String, WidgetItemIdStringListEditEntry, WidgetListItemIdStringListEdit> {
    private static final int PANEL_MARGIN_X = 42;
    private static final int PANEL_MARGIN_TOP = 48;
    private static final int PANEL_MARGIN_BOTTOM = 36;
    private static final int PANEL_TITLE_HEIGHT = 34;
    private static final int PICKER_SLOT = 24;
    private static final int PICKER_SLOT_GAP = 2;
    private static final int PICKER_WHEEL_PIXELS = 28;

    private final IConfigStringList config;
    private final IConfigGui configGui;
    private final IDialogHandler dialogHandler;
    private final GuiScrollBar pickerScrollBar = new GuiScrollBar();
    private final ButtonGeneric pickerCloseButton = new ButtonGeneric(0, 0, 92, 20, "");
    private final ButtonGeneric backButton = new ButtonGeneric(0, 0, 110, 20, "");
    private final List<Candidate> allCandidates = new ArrayList<>();
    private final List<Candidate> filteredCandidates = new ArrayList<>();
    private WidgetItemIdStringListEditEntry pickerTarget;
    private class_1799 hoveredStack = class_1799.field_8037;
    private String hoveredText = "";
    private String pickerQuery = "";
    private int dialogWidth;
    private int dialogHeight;
    private int dialogLeft;
    private int dialogTop;
    private boolean pickerOpen;
    private boolean pickerSearchFocused;
    private boolean draggingPickerScrollbar;
    private double pickerScrollRemainder;

    public GuiItemIdStringListEdit(IConfigStringList config, IConfigGui configGui, IDialogHandler dialogHandler, class_437 parent) {
        super(0, 0);
        this.config = config;
        this.configGui = configGui;
        this.dialogHandler = dialogHandler;
        this.title = StringUtils.translate("malilib.gui.title.string_list_edit", config.getName());
        this.pickerCloseButton.setDisplayString(StringUtils.translate("lmlp.gui.button.item_id_picker.close"));
        this.pickerCloseButton.setTextCentered(true);
        this.pickerCloseButton.setActionListener((button, mouseButton) -> this.closeItemPicker());
        this.backButton.setDisplayString(StringUtils.translate("lmlp.gui.button.item_id_picker.close"));
        this.backButton.setTextCentered(true);
        this.backButton.setActionListener((button, mouseButton) -> this.closeEditor());
        if (dialogHandler == null) {
            this.setParent(parent);
        }
    }

    public IConfigStringList getConfig() {
        return this.config;
    }

    void setHoveredText(String text) {
        this.hoveredStack = class_1799.field_8037;
        this.hoveredText = text == null ? "" : text;
    }

    public void openItemPicker(WidgetItemIdStringListEditEntry target) {
        if (this.allCandidates.isEmpty()) {
            this.reloadCandidates();
        }
        this.pickerTarget = target;
        this.pickerOpen = true;
        this.pickerSearchFocused = true;
        this.updateFilteredCandidates();
    }

    @Override
    public void initGui() {
        this.setWidthAndHeight();
        this.reCreateListWidget();
        super.initGui();
    }

    @Override
    protected int getBrowserWidth() {
        return this.dialogWidth - 28;
    }

    @Override
    protected int getBrowserHeight() {
        return this.dialogHeight - PANEL_TITLE_HEIGHT - 14;
    }

    @Override
    protected WidgetListItemIdStringListEdit createListWidget(int listX, int listY) {
        return new WidgetListItemIdStringListEdit(
                this.dialogLeft + 10,
                this.dialogTop + PANEL_TITLE_HEIGHT,
                this.getBrowserWidth(),
                this.getBrowserHeight(),
                this.dialogWidth - 160,
                this
        );
    }

    @Override
    public void method_25432() {
        WidgetListItemIdStringListEdit list = this.getListWidget();
        if (list.wereConfigsModified()) {
            list.applyPendingModifications();
            ConfigManager.getInstance().onConfigsChanged(this.configGui.getModId());
        }
        super.method_25432();
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        this.hoveredStack = class_1799.field_8037;
        this.hoveredText = "";

        if (this.pickerOpen) {
            this.renderPicker(context, mouseX, mouseY, delta);
            this.renderActiveTooltip(context, mouseX, mouseY);
            this.drawGuiMessages(context);
            return;
        }

        this.drawScreenBackground(mouseX, mouseY);
        this.drawTitle(context, mouseX, mouseY, delta);
        this.drawContents(context, mouseX, mouseY, delta);
        this.drawHoveredWidget(mouseX, mouseY, context);
        this.updateBackButtonPosition();
        this.backButton.render(mouseX, mouseY, this.backButton.isMouseOver(), context);
        this.renderActiveTooltip(context, mouseX, mouseY);
        this.drawGuiMessages(context);
    }

    private void renderActiveTooltip(class_332 context, int mouseX, int mouseY) {
        if (!this.hoveredStack.method_7960()) {
            ItemTooltipRenderer.render(context, this.textRenderer, this.hoveredStack, mouseX, mouseY);
        } else if (!this.hoveredText.isEmpty()) {
            RenderUtils.drawHoverText(mouseX, mouseY, List.of(this.hoveredText), context);
        }
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.pickerOpen) {
            this.offsetPickerScroll(verticalAmount);
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (this.pickerOpen) {
            return button == 0 ? this.onPickerClicked(mouseX, mouseY, button) : true;
        }
        if (this.backButton.onMouseClicked((int) mouseX, (int) mouseY, button)) {
            return true;
        }
        return super.method_25402(mouseX, mouseY, button);
    }

    @Override
    public boolean method_25406(double mouseX, double mouseY, int button) {
        if (this.pickerOpen) {
            this.draggingPickerScrollbar = false;
            this.pickerScrollBar.setIsDragging(false);
            return true;
        }
        return super.method_25406(mouseX, mouseY, button);
    }

    @Override
    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (this.pickerOpen) {
            return this.onPickerKeyPressed(keyCode, scanCode, modifiers);
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean method_25400(char chr, int modifiers) {
        if (this.pickerOpen) {
            if (this.pickerSearchFocused && chr >= 32) {
                this.pickerQuery += chr;
                this.updateFilteredCandidates();
                return true;
            }
            return true;
        }
        return super.method_25400(chr, modifiers);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers) {
        if (this.pickerOpen) {
            return this.onPickerKeyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.dialogHandler != null) {
            this.dialogHandler.closeDialog();
            return true;
        }
        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    @Override
    protected void drawScreenBackground(int mouseX, int mouseY) {
        RenderUtils.drawRect(0, 0, GuiUtils.getScaledWindowWidth(), GuiUtils.getScaledWindowHeight(), 0x88000000);
        RenderUtils.drawOutlinedBox(this.dialogLeft, this.dialogTop, this.dialogWidth, this.dialogHeight, 0xD6000000, 0xFF999999);
    }

    @Override
    protected void drawTitle(class_332 context, int mouseX, int mouseY, float partialTicks) {
        this.drawStringWithShadow(context, this.title, this.dialogLeft + 10, this.dialogTop + 6, 0xFFFFFFFF);
    }

    private void setWidthAndHeight() {
        int screenWidth = GuiUtils.getScaledWindowWidth();
        int screenHeight = GuiUtils.getScaledWindowHeight();
        this.dialogWidth = Math.max(520, screenWidth - PANEL_MARGIN_X * 2);
        this.dialogHeight = Math.max(260, screenHeight - PANEL_MARGIN_TOP - PANEL_MARGIN_BOTTOM);
        this.dialogLeft = Math.max(8, (screenWidth - this.dialogWidth) / 2);
        this.dialogTop = PANEL_MARGIN_TOP;
        this.updateBackButtonPosition();
    }

    private void updateBackButtonPosition() {
        this.backButton.setPosition(this.dialogLeft + this.dialogWidth - 120, this.dialogTop + 7);
    }

    private void closeEditor() {
        if (this.dialogHandler != null) {
            this.dialogHandler.closeDialog();
        } else {
            this.closeGui(true);
        }
    }

    private void closeItemPicker() {
        this.pickerOpen = false;
        this.pickerTarget = null;
        this.draggingPickerScrollbar = false;
        this.pickerScrollBar.setIsDragging(false);
    }
    private void reloadCandidates() {
        Map<String, Candidate> byId = new LinkedHashMap<>();
        for (EntryStack<?> entry : EntryRegistry.getInstance().getPreFilteredList()) {
            class_1799 stack = asItemStack(entry);
            if (stack.method_7960()) {
                continue;
            }
            String id = ItemStackTexts.id(stack);
            byId.putIfAbsent(id, new Candidate(stack.method_7972(), id, ItemStackTexts.name(stack)));
        }

        this.allCandidates.clear();
        this.allCandidates.addAll(byId.values());
        this.allCandidates.sort(Comparator.comparing(Candidate::id));
    }

    private void updateFilteredCandidates() {
        String query = this.pickerQuery.trim().toLowerCase(Locale.ROOT);
        this.filteredCandidates.clear();
        for (Candidate candidate : this.allCandidates) {
            if (query.isEmpty()
                    || candidate.id().toLowerCase(Locale.ROOT).contains(query)
                    || candidate.name().toLowerCase(Locale.ROOT).contains(query)) {
                this.filteredCandidates.add(candidate);
            }
        }
        this.pickerScrollBar.setValue(0);
    }

    private void renderPicker(class_332 context, int mouseX, int mouseY, float delta) {
        PickerLayout layout = this.pickerLayout();
        this.hoveredStack = class_1799.field_8037;
        this.hoveredText = "";
        this.pickerCloseButton.setPosition(layout.right() - 96, layout.top() + 8);
        this.pickerScrollBar.setMaxValue(Math.max(0, this.pickerContentHeight(layout) - layout.gridHeight()));

        RenderUtils.drawRect(0, 0, GuiUtils.getScaledWindowWidth(), GuiUtils.getScaledWindowHeight(), 0x66000000);
        RenderUtils.drawOutlinedBox(layout.left(), layout.top(), layout.width(), layout.height(), 0xEE000000, 0xFFAAAAAA);
        context.method_51433(this.textRenderer, StringUtils.translate("lmlp.gui.item_id_picker.title"), layout.left() + 10, layout.top() + 12, 0xFFFFFFFF, false);
        this.pickerCloseButton.render(mouseX, mouseY, false, context);
        this.renderPickerSearch(context, layout);
        this.renderPickerGrid(context, layout, mouseX, mouseY);
        if (this.pickerScrollBar.getMaxValue() > 0) {
            this.pickerScrollBar.render(mouseX, mouseY, delta, layout.scrollbarX(), layout.gridTop(), 8, layout.gridHeight(), this.pickerContentHeight(layout));
        }
    }

    private void renderPickerSearch(class_332 context, PickerLayout layout) {
        int y = layout.top() + 38;
        context.method_51433(this.textRenderer, StringUtils.translate("lmlp.gui.item_id_picker.search"), layout.left() + 10, y + 6, 0xFFE0E0E0, false);
        int fieldX = layout.left() + 70;
        int fieldWidth = layout.right() - fieldX - 12;
        RenderUtils.drawOutlinedBox(fieldX, y, fieldWidth, 20, 0xDD000000, this.pickerSearchFocused ? 0xFFFFFF88 : 0xFF888888);
        String shown = this.pickerQuery.isEmpty() ? StringUtils.translate("lmlp.gui.item_id_picker.search_placeholder") : this.pickerQuery;
        int color = this.pickerQuery.isEmpty() ? 0xFFAAAAAA : 0xFFFFFFFF;
        context.method_44379(fieldX + 4, y + 2, fieldX + fieldWidth - 4, y + 18);
        context.method_51433(this.textRenderer, shown, fieldX + 4, y + 6, color, false);
        context.method_44380();
    }

    private void renderPickerGrid(class_332 context, PickerLayout layout, int mouseX, int mouseY) {
        RenderUtils.drawOutlinedBox(layout.gridLeft(), layout.gridTop(), layout.gridWidth(), layout.gridHeight(), 0xDD000000, 0xFF888888);
        context.method_44379(layout.gridLeft() + 1, layout.gridTop() + 1, layout.gridRight() - 1, layout.gridBottom() - 1);
        if (this.filteredCandidates.isEmpty()) {
            context.method_51433(this.textRenderer, StringUtils.translate("lmlp.gui.item_id_picker.empty"), layout.gridLeft() + 8, layout.gridTop() + 10, 0xFFFFCC66, false);
        } else {
            int columns = layout.columns();
            int startRow = this.pickerScrollBar.getValue() / layout.cellStride();
            int yOffset = -(this.pickerScrollBar.getValue() % layout.cellStride());
            int visibleRows = layout.gridHeight() / layout.cellStride() + 2;
            for (int row = 0; row < visibleRows; row++) {
                int candidateRow = startRow + row;
                for (int column = 0; column < columns; column++) {
                    int index = candidateRow * columns + column;
                    if (index >= this.filteredCandidates.size()) {
                        break;
                    }
                    int x = layout.gridLeft() + 4 + column * layout.cellStride();
                    int y = layout.gridTop() + 4 + yOffset + row * layout.cellStride();
                    this.renderPickerSlot(context, this.filteredCandidates.get(index), x, y, mouseX, mouseY);
                }
            }
        }
        context.method_44380();
    }

    private void renderPickerSlot(class_332 context, Candidate candidate, int x, int y, int mouseX, int mouseY) {
        boolean hovered = GuiBase.isMouseOver(mouseX, mouseY, x, y, PICKER_SLOT, PICKER_SLOT);
        RenderUtils.drawOutlinedBox(x, y, PICKER_SLOT, PICKER_SLOT, hovered ? 0xA0707070 : 0xA0404040, hovered ? 0xFFFFFF88 : 0xFF888888);
        context.method_51427(candidate.stack(), x + 4, y + 4);
        if (hovered) {
            this.hoveredStack = candidate.stack();
            this.hoveredText = candidate.id();
        }
    }

    private boolean onPickerClicked(double mouseX, double mouseY, int button) {
        PickerLayout layout = this.pickerLayout();
        this.pickerSearchFocused = this.isPickerSearchHovered(layout, mouseX, mouseY);
        if (this.pickerCloseButton.onMouseClicked((int) mouseX, (int) mouseY, button)) {
            return true;
        }

        int index = this.pickerIndexAt(layout, mouseX, mouseY);
        if (index >= 0 && this.pickerTarget != null) {
            this.pickerTarget.setSelectedItemId(this.filteredCandidates.get(index).id());
            this.closeItemPicker();
            return true;
        }

        if (this.pickerScrollBar.wasMouseOver()) {
            this.pickerScrollBar.setIsDragging(true);
            this.draggingPickerScrollbar = true;
        }
        return true;
    }

    private boolean onPickerKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.closeItemPicker();
            return true;
        }
        if (this.pickerSearchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !this.pickerQuery.isEmpty()) {
                this.pickerQuery = this.pickerQuery.substring(0, this.pickerQuery.length() - 1);
                this.updateFilteredCandidates();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                this.pickerQuery = "";
                this.updateFilteredCandidates();
                return true;
            }
        }
        return true;
    }

    private void offsetPickerScroll(double verticalAmount) {
        double target = this.pickerScrollRemainder - verticalAmount * PICKER_WHEEL_PIXELS;
        int pixels = (int) target;
        this.pickerScrollRemainder = target - pixels;
        if (pixels == 0 && verticalAmount != 0.0D) {
            pixels = verticalAmount > 0.0D ? -1 : 1;
            this.pickerScrollRemainder = 0.0D;
        }
        this.pickerScrollBar.offsetValue(pixels);
    }

    private PickerLayout pickerLayout() {
        int width = Math.min(430, Math.max(320, GuiUtils.getScaledWindowWidth() - 160));
        int height = Math.min(330, Math.max(250, GuiUtils.getScaledWindowHeight() - 120));
        int left = Math.max(10, (GuiUtils.getScaledWindowWidth() - width) / 2);
        int top = Math.max(10, (GuiUtils.getScaledWindowHeight() - height) / 2);
        int gridLeft = left + 10;
        int gridTop = top + 70;
        int gridWidth = width - 30;
        int gridHeight = height - 84;
        int columns = Math.max(1, (gridWidth - 8) / (PICKER_SLOT + PICKER_SLOT_GAP));
        return new PickerLayout(left, top, width, height, gridLeft, gridTop, gridWidth, gridHeight, columns);
    }

    private int pickerIndexAt(PickerLayout layout, double mouseX, double mouseY) {
        if (!GuiBase.isMouseOver((int) mouseX, (int) mouseY, layout.gridLeft(), layout.gridTop(), layout.gridWidth(), layout.gridHeight())) {
            return -1;
        }

        int localX = (int) mouseX - layout.gridLeft() - 4;
        int localY = (int) mouseY - layout.gridTop() - 4 + this.pickerScrollBar.getValue();
        if (localX < 0 || localY < 0) {
            return -1;
        }

        int column = localX / layout.cellStride();
        int row = localY / layout.cellStride();
        if (column < 0 || column >= layout.columns() || localX % layout.cellStride() >= PICKER_SLOT || localY % layout.cellStride() >= PICKER_SLOT) {
            return -1;
        }

        int index = row * layout.columns() + column;
        return index >= 0 && index < this.filteredCandidates.size() ? index : -1;
    }

    private boolean isPickerSearchHovered(PickerLayout layout, double mouseX, double mouseY) {
        int fieldX = layout.left() + 70;
        int fieldY = layout.top() + 38;
        return GuiBase.isMouseOver((int) mouseX, (int) mouseY, fieldX, fieldY, layout.right() - fieldX - 12, 20);
    }

    private int pickerContentHeight(PickerLayout layout) {
        int rows = Math.max(1, (this.filteredCandidates.size() + layout.columns() - 1) / layout.columns());
        return rows * layout.cellStride() + 8;
    }

    private static class_1799 asItemStack(EntryStack<?> stack) {
        try {
            class_1799 itemStack = stack.cheatsAs().getValue();
            return itemStack == null ? class_1799.field_8037 : itemStack;
        } catch (Throwable throwable) {
            return class_1799.field_8037;
        }
    }

    private record Candidate(class_1799 stack, String id, String name) {
    }

    private record PickerLayout(int left, int top, int width, int height, int gridLeft, int gridTop, int gridWidth, int gridHeight, int columns) {
        int right() {
            return this.left + this.width;
        }

        int gridRight() {
            return this.gridLeft + this.gridWidth;
        }

        int gridBottom() {
            return this.gridTop + this.gridHeight;
        }

        int scrollbarX() {
            return this.gridRight() + 6;
        }

        int cellStride() {
            return PICKER_SLOT + PICKER_SLOT_GAP;
        }
    }
}
