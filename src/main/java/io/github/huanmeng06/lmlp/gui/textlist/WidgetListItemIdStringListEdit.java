package io.github.huanmeng06.lmlp.gui.textlist;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;

import java.util.Collection;
import java.util.List;

public class WidgetListItemIdStringListEdit extends WidgetListConfigOptionsBase<String, WidgetItemIdStringListEditEntry> {
    private final IConfigStringList config;
    private final GuiItemIdStringListEdit editor;

    public WidgetListItemIdStringListEdit(int x, int y, int width, int height, int configWidth, GuiItemIdStringListEdit editor) {
        super(x, y, width, height, configWidth);
        this.editor = editor;
        this.config = editor.getConfig();
        this.browserEntryHeight = 28;
    }

    public IConfigStringList getConfig() {
        return this.config;
    }

    List<String> getEntries() {
        return this.editor.getEditorEntries();
    }

    void setEntryValue(int index, String value) {
        List<String> entries = this.getEntries();
        if (index >= 0 && index < entries.size()) {
            entries.set(index, value);
        }
        if (index >= 0 && index < this.listContents.size()) {
            this.listContents.set(index, value);
        }
    }

    @Override
    protected Collection<String> getAllEntries() {
        return this.editor.getEditorEntries();
    }

    @Override
    protected void reCreateListEntryWidgets() {
        if (this.listContents.size() == 0) {
            this.listWidgets.clear();
            this.textFields.clear();
            this.maxVisibleBrowserEntries = 1;
            int x = this.posX + 2;
            int y = this.posY + 4 + this.browserEntriesOffsetY;
            this.listWidgets.add(this.createListEntryWidget(x, y, -1, false, ""));
            this.scrollBar.setMaxValue(0);
        } else {
            super.reCreateListEntryWidgets();
        }
    }

    @Override
    protected WidgetItemIdStringListEditEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, String value) {
        String defaultValue = "";
        if (listIndex >= 0 && listIndex < this.config.getDefaultStrings().size()) {
            defaultValue = this.config.getDefaultStrings().get(listIndex);
        }

        return new WidgetItemIdStringListEditEntry(
                x,
                y,
                this.browserEntryWidth,
                this.browserEntryHeight,
                listIndex,
                isOdd,
                value,
                defaultValue,
                this,
                this.editor
        );
    }
}