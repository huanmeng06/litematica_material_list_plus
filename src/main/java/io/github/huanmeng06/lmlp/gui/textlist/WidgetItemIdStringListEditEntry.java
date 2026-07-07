package io.github.huanmeng06.lmlp.gui.textlist;

import fi.dy.masa.malilib.config.IStringRepresentable;
import fi.dy.masa.malilib.config.gui.ConfigOptionChangeListenerTextField;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.MaLiLibIcons;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOptionBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.class_1799;
import net.minecraft.class_332;

import java.util.List;

public class WidgetItemIdStringListEditEntry extends WidgetConfigOptionBase<String> {
    private static final int INDEX_WIDTH = 30;
    private static final int ICON_AREA_WIDTH = 24;
    private static final int ICON_SLOT_SIZE = 18;
    private static final int ITEM_ICON_SIZE = 16;
    private static final int SELECT_WIDTH = 58;
    private static final int RESET_WIDTH = 58;
    private static final int ICON_BUTTON_WIDTH = 18;
    private static final int CONTROL_HEIGHT = 20;
    private static final int ACTION_GAP = 4;
    private static final int ROW_BACKGROUND_ODD = 0x20FFFFFF;
    private static final int ROW_BACKGROUND_EVEN = 0x30FFFFFF;
    private static final int ICON_SLOT_BACKGROUND = 0x55000000;
    private static final int ICON_SLOT_BORDER = 0xFF555555;

    private final WidgetListItemIdStringListEdit listWidget;
    private final GuiItemIdStringListEdit editor;
    private final String defaultValue;
    private final int listIndex;
    private final boolean isOdd;
    private final int iconX;
    private final int iconY;
    private int handleWidth;
    private ButtonGeneric resetButton;

    public WidgetItemIdStringListEditEntry(
            int x,
            int y,
            int width,
            int height,
            int listIndex,
            boolean isOdd,
            String value,
            String defaultValue,
            WidgetListItemIdStringListEdit listWidget,
            GuiItemIdStringListEdit editor
    ) {
        super(x, y, width, height, listWidget, value, listIndex);
        this.listWidget = listWidget;
        this.editor = editor;
        this.listIndex = listIndex;
        this.isOdd = isOdd;
        this.defaultValue = defaultValue;
        this.initialStringValue = value;
        this.lastAppliedValue = value;

        int centerY = y + height / 2;
        int buttonY = centerY - CONTROL_HEIGHT / 2;
        int textY = buttonY;
        // Reserve both icon-button slots (+/-) even for rows where either
        // could conceivably be hidden, so the buttons sit at fixed slot
        // offsets from actionStartX and every text field's right edge lines
        // up between rows.
        int reservedIconSlots = 2;
        int actionStartX = x + width - ICON_BUTTON_WIDTH * reservedIconSlots - 8;
        int resetX = actionStartX - RESET_WIDTH - 8;
        int selectX = resetX - SELECT_WIDTH - ACTION_GAP;
        this.iconX = x + INDEX_WIDTH + (ICON_AREA_WIDTH - ICON_SLOT_SIZE) / 2;
        this.iconY = centerY - ICON_SLOT_SIZE / 2;
        int textX = x + INDEX_WIDTH + ICON_AREA_WIDTH + ACTION_GAP;
        // Clamp to the real gap before the select button (never force 120, which
        // made the text field overlap the select/reset buttons on narrow rows).
        int textWidth = Math.max(0, selectX - textX - ACTION_GAP);
        // The index label + item icon area doubles as the drag handle (see
        // onMouseClicked): press-and-hold there, then release over another
        // row to reorder, replacing the old up/down move buttons.
        this.handleWidth = INDEX_WIDTH + ICON_AREA_WIDTH;

        if (!this.isDummy()) {
            this.addLabel(x + 2, centerY - 5, INDEX_WIDTH - 4, 12, 0xFFC0C0C0, String.format("%3d:", listIndex + 1));
            this.resetButton = this.createStringResetButton(resetX, buttonY);
            this.addEntryTextField(textX, textY, textWidth, CONTROL_HEIGHT, value, this.resetButton);
            this.addSelectButton(selectX, buttonY);
            this.addResetButton(this.resetButton);
            this.addActionButton(actionStartX, buttonY, MaLiLibIcons.PLUS, "lmlp.gui.button.hovertext.add_below", this::insertEntryAfter);
            this.addActionButton(actionStartX + ICON_BUTTON_WIDTH, buttonY, MaLiLibIcons.MINUS, "malilib.gui.button.hovertext.remove", this::removeEntry);
        } else {
            this.addActionButton(textX, buttonY, MaLiLibIcons.PLUS, "malilib.gui.button.hovertext.add", this::insertEntryAfter);
        }
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.isDummy() && mouseButton == 0 && this.isHandleHovered(mouseX, mouseY)) {
            this.listWidget.beginDrag(this.listIndex);
            return true;
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isHandleHovered(int mouseX, int mouseY) {
        return GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.handleWidth, this.height);
    }

    public void setSelectedItemId(String id) {
        if (this.isDummy() || this.textField == null) {
            return;
        }

        GuiTextFieldGeneric field = this.textField.getTextField();
        field.method_1852(id);
        this.applyNewValueToConfig();
        this.listWidget.markConfigsModified();
    }

    private boolean isDummy() {
        return this.listIndex < 0;
    }

    private void addEntryTextField(int x, int y, int width, int height, String value, ButtonBase resetButton) {
        GuiTextFieldGeneric field = this.createTextField(x, y + 1, width, height - 3);
        field.method_1880(this.maxTextfieldTextLength);
        field.method_1852(value);
        this.addTextField(field, new StableResetListener(new RowStringRepresentable(this.defaultValue), field, resetButton, this));
    }

    private void addSelectButton(int x, int y) {
        ButtonGeneric button = new ButtonGeneric(
                x,
                y,
                SELECT_WIDTH,
                CONTROL_HEIGHT,
                StringUtils.translate("lmlp.gui.button.text_list.select"),
                new String[0]
        );
        button.setTextCentered(true);
        this.addButton(button, (clickedButton, mouseButton) -> this.editor.openItemPicker(this));
    }

    private ButtonGeneric createStringResetButton(int x, int y) {
        String label = StringUtils.translate("malilib.gui.button.reset.caps");
        ButtonGeneric button = new ButtonGeneric(x, y, RESET_WIDTH, CONTROL_HEIGHT, label, new String[0]);
        button.setTextCentered(true);
        button.setEnabled(true);
        return button;
    }

    private void addResetButton(ButtonGeneric button) {
        this.addButton(button, (clickedButton, mouseButton) -> {
            if (this.textField != null) {
                this.textField.getTextField().method_1852(this.defaultValue);
                this.applyNewValueToConfig();
                this.listWidget.markConfigsModified();
            }
        });
    }

    private void addActionButton(int x, int y, MaLiLibIcons icon, String hoverKey, Runnable action) {
        ButtonGeneric button = new ButtonGeneric(x, y, icon, StringUtils.translate(hoverKey));
        this.addButton(button, (clickedButton, mouseButton) -> action.run());
    }

    @Override
    public boolean wasConfigModified() {
        return !this.isDummy() && !this.currentText().equals(this.initialStringValue);
    }

    @Override
    public void applyNewValueToConfig() {
        if (this.isDummy()) {
            return;
        }

        String value = this.currentText();
        this.listWidget.setEntryValue(this.listIndex, value);
        this.lastAppliedValue = value;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, class_332 context) {
        RenderUtils.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderUtils.drawRect(this.x, this.y, this.width, this.height, this.isOdd ? ROW_BACKGROUND_ODD : ROW_BACKGROUND_EVEN);
        if (!this.isDummy()) {
            this.renderItemIcon(context, mouseX, mouseY);
        }
        this.drawSubWidgets(mouseX, mouseY, context);
        this.drawTextFields(mouseX, mouseY, context);
    }

    private void renderItemIcon(class_332 context, int mouseX, int mouseY) {
        boolean overIconSlot = GuiBase.isMouseOver(mouseX, mouseY, this.iconX, this.iconY, ICON_SLOT_SIZE, ICON_SLOT_SIZE);
        boolean handleHovered = this.isHandleHovered(mouseX, mouseY) && !this.listWidget.isDragging();
        boolean isDraggedRow = this.listWidget.isDragging() && this.listWidget.dragIndex() == this.listIndex;
        if (handleHovered || isDraggedRow) {
            RenderUtils.drawRect(this.x, this.y, this.handleWidth, this.height, 0x40FFFFFF);
        }
        RenderUtils.drawOutlinedBox(this.iconX, this.iconY, ICON_SLOT_SIZE, ICON_SLOT_SIZE, ICON_SLOT_BACKGROUND, ICON_SLOT_BORDER);
        ItemIdListIconResolver.Display display = ItemIdListIconResolver.currentIcon(this.currentText());
        class_1799 stack = display.stack();
        if (!stack.method_7960() && overIconSlot) {
            this.editor.setHoveredText(display.id());
        }
        if (!stack.method_7960()) {
            context.method_51427(stack, this.iconX + (ICON_SLOT_SIZE - ITEM_ICON_SIZE) / 2, this.iconY + (ICON_SLOT_SIZE - ITEM_ICON_SIZE) / 2);
        }
        if (handleHovered && !overIconSlot) {
            this.editor.setHoveredText(StringUtils.translate("lmlp.gui.button.hovertext.drag_reorder"));
        }
    }

    private String currentText() {
        return this.textField != null ? this.textField.getTextField().method_1882() : "";
    }

    private void insertEntryAfter() {
        this.listWidget.applyPendingModifications();
        List<String> strings = this.listWidget.getEntries();
        int size = strings.size();
        int index = this.listIndex < 0 || this.listIndex >= size ? size : this.listIndex + 1;
        strings.add(index, "");
        this.listWidget.refreshEntries();
        this.listWidget.markConfigsModified();
    }

    private void removeEntry() {
        this.listWidget.applyPendingModifications();
        List<String> strings = this.listWidget.getEntries();
        if (this.listIndex >= 0 && this.listIndex < strings.size()) {
            strings.remove(this.listIndex);
            this.listWidget.refreshEntries();
            this.listWidget.markConfigsModified();
        }
    }

    private static final class StableResetListener extends ConfigOptionChangeListenerTextField {
        private final WidgetItemIdStringListEditEntry entry;

        private StableResetListener(IStringRepresentable config, GuiTextFieldGeneric textField, ButtonBase resetButton, WidgetItemIdStringListEditEntry entry) {
            super(config, textField, resetButton);
            this.entry = entry;
        }

        @Override
        public boolean onTextChange(GuiTextFieldGeneric textField) {
            this.entry.applyNewValueToConfig();
            this.entry.listWidget.markConfigsModified();
            return false;
        }
    }

    private static final class RowStringRepresentable implements IStringRepresentable {
        private final String defaultValue;

        private RowStringRepresentable(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public String getStringValue() {
            return this.defaultValue;
        }

        @Override
        public String getDefaultStringValue() {
            return this.defaultValue;
        }

        @Override
        public void setValueFromString(String value) {
        }

        @Override
        public boolean isModified(String value) {
            return !this.defaultValue.equals(value);
        }
    }
}
