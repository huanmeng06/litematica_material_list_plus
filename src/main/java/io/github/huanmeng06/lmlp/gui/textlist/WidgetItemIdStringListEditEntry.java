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
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import net.minecraft.class_1799;
import fi.dy.masa.malilib.render.GuiContext;

import java.util.List;

public class WidgetItemIdStringListEditEntry extends WidgetConfigOptionBase<String> {
    private static final int INDEX_WIDTH = 30;
    private static final int ICON_AREA_WIDTH = 24;
    private static final int ICON_SLOT_SIZE = 18;
    private static final int ITEM_ICON_SIZE = 16;
    // Shared by the "选择"/enable-disable toggle buttons so they always match widths.
    // Matches malilib's own ConfigButtonBoolean width (WidgetConfigOption.addBooleanAndHotkeyWidgets), so the
    // toggle button lines up visually with the boolean config buttons on the Config Forms page.
    private static final int ACTION_BUTTON_WIDTH = 60;
    private static final int TEXT_COLOR_ENABLED = 0xFFE0E0E0;
    private static final int TEXT_COLOR_DISABLED = 0xFF808080;
    private static final int ICON_BUTTON_WIDTH = 18;
    private static final int CONTROL_HEIGHT = 20;
    private static final int ACTION_GAP = 4;
    private static final int ROW_BACKGROUND_ODD = 0x20FFFFFF;
    private static final int ROW_BACKGROUND_EVEN = 0x30FFFFFF;
    private static final int ICON_SLOT_BACKGROUND = 0x55000000;
    private static final int ICON_SLOT_BORDER = 0xFF555555;
    private static final IStringRepresentable NO_OP_STRING_REPRESENTABLE = new IStringRepresentable() {
        @Override
        public String getStringValue() {
            return "";
        }

        @Override
        public String getDefaultStringValue() {
            return "";
        }

        @Override
        public void setValueFromString(String value) {
        }

        @Override
        public boolean isModified(String value) {
            return false;
        }
    };

    private final WidgetListItemIdStringListEdit listWidget;
    private final GuiItemIdStringListEdit editor;
    private final int listIndex;
    private final boolean isOdd;
    private final int iconX;
    private final int iconY;
    private int handleWidth;
    private boolean enabled;
    private ButtonGeneric toggleButton;

    public WidgetItemIdStringListEditEntry(
            int x,
            int y,
            int width,
            int height,
            int listIndex,
            boolean isOdd,
            String rawValue,
            WidgetListItemIdStringListEdit listWidget,
            GuiItemIdStringListEdit editor
    ) {
        super(x, y, width, height, listWidget, Configs.stripEntryDisabledPrefix(rawValue), listIndex);
        this.listWidget = listWidget;
        this.editor = editor;
        this.listIndex = listIndex;
        this.isOdd = isOdd;
        this.enabled = !Configs.isEntryDisabled(rawValue);
        String cleanValue = Configs.stripEntryDisabledPrefix(rawValue);
        this.initialStringValue = cleanValue;
        this.lastAppliedValue = cleanValue;

        int centerY = y + height / 2;
        int buttonY = centerY - CONTROL_HEIGHT / 2;
        int textY = buttonY;
        // Reserve both icon-button slots (+/-) even for rows where either
        // could conceivably be hidden, so the buttons sit at fixed slot
        // offsets from actionStartX and every text field's right edge lines
        // up between rows.
        int reservedIconSlots = 2;
        int actionStartX = x + width - ICON_BUTTON_WIDTH * reservedIconSlots - 8;
        int toggleX = actionStartX - ACTION_BUTTON_WIDTH - 8;
        int selectX = toggleX - ACTION_BUTTON_WIDTH - ACTION_GAP;
        this.iconX = x + INDEX_WIDTH + (ICON_AREA_WIDTH - ICON_SLOT_SIZE) / 2;
        this.iconY = centerY - ICON_SLOT_SIZE / 2;
        int textX = x + INDEX_WIDTH + ICON_AREA_WIDTH + ACTION_GAP;
        // Clamp to the real gap before the select button (never force 120, which
        // made the text field overlap the select/toggle buttons on narrow rows).
        int textWidth = Math.max(0, selectX - textX - ACTION_GAP);
        // The index label + item icon area doubles as the drag handle (see
        // onMouseClicked): press-and-hold there, then release over another
        // row to reorder, replacing the old up/down move buttons.
        this.handleWidth = INDEX_WIDTH + ICON_AREA_WIDTH;

        if (!this.isDummy()) {
            if (!this.enabled) {
                // Build the grayscale icon textures now, outside the render pass;
                // doing it lazily mid-frame caused a visible flicker.
                GrayscaleItemIcon.prewarm(cleanValue);
            }
            this.addLabel(x + 2, centerY - 5, INDEX_WIDTH - 4, 12, 0xFFC0C0C0, String.format("%3d:", listIndex + 1));
            this.toggleButton = this.createToggleButton(toggleX, buttonY);
            this.addEntryTextField(textX, textY, textWidth, CONTROL_HEIGHT, cleanValue, this.toggleButton);
            this.textField.textField().method_1868(this.enabled ? TEXT_COLOR_ENABLED : TEXT_COLOR_DISABLED);
            this.addSelectButton(selectX, buttonY);
            this.addToggleButton(this.toggleButton);
            this.addActionButton(actionStartX, buttonY, MaLiLibIcons.PLUS, "lmlp.gui.button.hovertext.add_below", this::insertEntryAfter);
            this.addActionButton(actionStartX + ICON_BUTTON_WIDTH, buttonY, MaLiLibIcons.MINUS, "malilib.gui.button.hovertext.remove", this::removeEntry);
        } else {
            this.addActionButton(textX, buttonY, MaLiLibIcons.PLUS, "malilib.gui.button.hovertext.add", this::insertEntryAfter);
        }
    }

    @Override
    public boolean onMouseClicked(net.minecraft.class_11909 event, boolean doubleClick) {
        int mouseX = (int) event.comp_4798();
        int mouseY = (int) event.comp_4799();
        int mouseButton = event.comp_4800().comp_4801();
        if (!this.isDummy() && mouseButton == 0 && this.isHandleHovered(mouseX, mouseY)) {
            this.listWidget.beginDrag(this.listIndex);
            return true;
        }

        return super.onMouseClicked(event, doubleClick);
    }

    private boolean isHandleHovered(int mouseX, int mouseY) {
        return GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.handleWidth, this.height);
    }

    public void setSelectedItemId(String id) {
        if (this.isDummy() || this.textField == null) {
            return;
        }

        GuiTextFieldGeneric field = this.textField.textField();
        field.method_1852(id);
        this.applyNewValueToConfig();
        this.listWidget.markConfigsModified();
    }

    private boolean isDummy() {
        return this.listIndex < 0;
    }

    private void addEntryTextField(int x, int y, int width, int height, String value, ButtonBase toggleButton) {
        GuiTextFieldGeneric field = this.createTextField(x, y + 1, width, height - 3);
        field.method_1880(this.maxTextfieldTextLength);
        field.method_1852(value);
        this.addTextField(field, new EntryChangeListener(NO_OP_STRING_REPRESENTABLE, field, toggleButton, this));
    }

    private void addSelectButton(int x, int y) {
        ButtonGeneric button = new ButtonGeneric(
                x,
                y,
                ACTION_BUTTON_WIDTH,
                CONTROL_HEIGHT,
                StringUtils.translate("lmlp.gui.button.text_list.select"),
                new String[0]
        );
        button.setTextCentered(true);
        this.addButton(button, (clickedButton, mouseButton) -> this.editor.openItemPicker(this));
    }

    private ButtonGeneric createToggleButton(int x, int y) {
        ButtonGeneric button = new ButtonGeneric(x, y, ACTION_BUTTON_WIDTH, CONTROL_HEIGHT, this.toggleButtonLabel(), new String[0]);
        button.setTextCentered(true);
        button.setEnabled(true);
        return button;
    }

    private String toggleButtonLabel() {
        // Mirrors malilib's ConfigButtonBoolean.updateDisplayString(): colored true/false,
        // matching the boolean config buttons elsewhere in the mod's config menu.
        String color = this.enabled ? GuiBase.TXT_DARK_GREEN : GuiBase.TXT_DARK_RED;
        return color + String.valueOf(this.enabled) + GuiBase.TXT_RST;
    }

    private void addToggleButton(ButtonGeneric button) {
        this.addButton(button, (clickedButton, mouseButton) -> {
            this.enabled = !this.enabled;
            this.applyNewValueToConfig();
            this.listWidget.markConfigsModified();
            // The button label depends on the new state; simplest way to
            // refresh it is to let the list recreate this row's widgets.
            this.listWidget.refreshEntries();
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

        String cleanValue = this.currentText();
        this.listWidget.setEntryValue(this.listIndex, Configs.withEntryDisabledState(cleanValue, !this.enabled));
        this.lastAppliedValue = cleanValue;
    }

    @Override
    public void render(GuiContext context, int mouseX, int mouseY, boolean selected) {
        RenderUtils.drawRect(context, this.x, this.y, this.width, this.height,
                this.isOdd ? ROW_BACKGROUND_ODD : ROW_BACKGROUND_EVEN);
        if (!this.isDummy()) {
            this.renderItemIcon(context, mouseX, mouseY);
        }
        this.drawSubWidgets(context, mouseX, mouseY);
        this.drawTextFields(context, mouseX, mouseY);
    }

    private void renderItemIcon(GuiContext context, int mouseX, int mouseY) {
        boolean overIconSlot = GuiBase.isMouseOver(mouseX, mouseY, this.iconX, this.iconY, ICON_SLOT_SIZE, ICON_SLOT_SIZE);
        boolean handleHovered = this.isHandleHovered(mouseX, mouseY) && !this.listWidget.isDragging();
        boolean isDraggedRow = this.listWidget.isDragging() && this.listWidget.dragIndex() == this.listIndex;
        if (handleHovered || isDraggedRow) {
            RenderUtils.drawRect(context, this.x, this.y, this.width, this.height, 0x40FFFFFF);
        }
        RenderUtils.drawOutlinedBox(context, this.iconX, this.iconY, ICON_SLOT_SIZE, ICON_SLOT_SIZE, ICON_SLOT_BACKGROUND, ICON_SLOT_BORDER);
        ItemIdListIconResolver.Display display = ItemIdListIconResolver.currentIcon(this.currentText());
        class_1799 stack = display.stack();
        // While a drag is in progress, only the dragged-row ghost label should
        // show -- suppress this row's own hover tooltips so the item id (or the
        // drag hint) doesn't render behind the ghost label.
        boolean dragging = this.listWidget.isDragging();
        if (!dragging && !stack.method_7960() && overIconSlot) {
            this.editor.setHoveredText(ItemStackTexts.name(stack));
        }
        if (!stack.method_7960()) {
            int drawX = this.iconX + (ICON_SLOT_SIZE - ITEM_ICON_SIZE) / 2;
            int drawY = this.iconY + (ICON_SLOT_SIZE - ITEM_ICON_SIZE) / 2;
            if (this.enabled || !GrayscaleItemIcon.render(context, stack, display.id(), drawX, drawY, ITEM_ICON_SIZE)) {
                context.method_51427(stack, drawX, drawY);
            }
        }
        if (!dragging && handleHovered && !overIconSlot) {
            this.editor.setHoveredText(StringUtils.translate("lmlp.gui.button.hovertext.drag_reorder"));
        }
    }

    private String currentText() {
        return this.textField != null ? this.textField.textField().method_1882() : "";
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

    private static final class EntryChangeListener extends ConfigOptionChangeListenerTextField {
        private final WidgetItemIdStringListEditEntry entry;

        private EntryChangeListener(IStringRepresentable config, GuiTextFieldGeneric textField, ButtonBase toggleButton, WidgetItemIdStringListEditEntry entry) {
            super(config, textField, toggleButton);
            this.entry = entry;
        }

        @Override
        public boolean onTextChange(GuiTextFieldGeneric textField) {
            if (!this.entry.enabled) {
                // Typing happens outside the render pass; prewarm here so the
                // grayscale icon for the new id never builds mid-frame.
                GrayscaleItemIcon.prewarm(this.entry.currentText());
            }
            this.entry.applyNewValueToConfig();
            this.entry.listWidget.markConfigsModified();
            return false;
        }
    }
}
