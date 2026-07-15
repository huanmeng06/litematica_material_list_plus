package io.github.huanmeng06.lmlp.gui.textlist;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import fi.dy.masa.malilib.render.GuiContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class WidgetListItemIdStringListEdit extends WidgetListConfigOptionsBase<String, WidgetItemIdStringListEditEntry> {
    private static final int DRAG_LINE_HEIGHT = 2;
    private final IConfigStringList config;
    private final GuiItemIdStringListEdit editor;
    private int dragIndex = -1;

    public WidgetListItemIdStringListEdit(int x, int y, int width, int height, int configWidth, GuiItemIdStringListEdit editor) {
        super(x, y, width, height, configWidth);
        this.editor = editor;
        this.config = editor.getConfig();
        this.browserEntryHeight = 28;
    }

    public IConfigStringList getConfig() {
        return this.config;
    }

    // Drag-to-reorder replaces the old up/down move buttons: pressing on a
    // row's index/icon area (see WidgetItemIdStringListEditEntry#onMouseClicked)
    // starts a drag, tracked here so it survives per-row widget recreation on
    // scroll/refresh; the drop position is resolved from the currently visible
    // row widgets on release, since this malilib widget stack has no
    // mouse-dragged event to hook, only click/release.
    void beginDrag(int index) {
        if (index >= 0 && index < this.getEntries().size()) {
            this.applyPendingModifications();
            this.dragIndex = index;
        }
    }

    boolean isReordering() {
        return this.dragIndex >= 0;
    }

    int dragIndex() {
        return this.dragIndex;
    }

    @Override
    public boolean onMouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        int mouseY = (int) event.y();
        int mouseButton = event.buttonInfo().button();
        if (mouseButton == 0 && this.dragIndex >= 0) {
            int dragged = this.dragIndex;
            this.dragIndex = -1;
            this.applyPendingModifications();
            List<String> entries = this.getEntries();
            int size = entries.size();
            if (dragged < size) {
                int dropIndex = this.dropIndexAt(mouseY);
                int insertAt = dropIndex > dragged ? dropIndex - 1 : dropIndex;
                insertAt = Math.max(0, Math.min(insertAt, size - 1));
                if (insertAt != dragged) {
                    String value = entries.remove(dragged);
                    entries.add(insertAt, value);
                    this.markConfigsModified();
                }
            }
            this.refreshEntries();
            return true;
        }
        return super.onMouseReleased(event);
    }

    @Override
    public void drawContents(GuiContext context, int mouseX, int mouseY, float delta) {
        // MaLiLib intentionally creates one partially visible row at the edge
        // of a scrolling list. In 1.21.11 text fields are no longer clipped by
        // the surrounding dialog automatically, so that row can paint over
        // the panel below it. Clip this custom list to its actual browser box.
        context.getGuiGraphics().enableScissor(
                this.posX,
                this.posY,
                this.posX + this.browserWidth,
                this.posY + this.browserHeight);
        try {
            super.drawContents(context, mouseX, mouseY, delta);
            if (this.dragIndex >= 0) {
                this.renderDragFeedback(context, mouseX, mouseY);
            }
        } finally {
            context.getGuiGraphics().disableScissor();
        }
    }

    // Which list index the dragged row would land BEFORE if dropped here
    // (0..size, size meaning "after the last row"), resolved from the
    // currently visible row widgets' own Y positions rather than the list's
    // internal scroll math, which this subclass has no access to.
    private int dropIndexAt(int mouseY) {
        int size = this.getEntries().size();
        for (WidgetItemIdStringListEditEntry entry : this.listWidgets) {
            int index = entry.getListIndex();
            if (index < 0) {
                continue;
            }
            if (mouseY < entry.getY() + entry.getHeight() / 2) {
                return index;
            }
        }

        for (int i = this.listWidgets.size() - 1; i >= 0; i--) {
            int index = this.listWidgets.get(i).getListIndex();
            if (index >= 0) {
                return index + 1;
            }
        }

        return size;
    }

    private void renderDragFeedback(GuiContext context, int mouseX, int mouseY) {
        List<String> entries = this.getEntries();
        if (this.dragIndex >= entries.size()) {
            return;
        }

        int dropIndex = this.dropIndexAt(mouseY);
        Integer lineY = null;
        int lineX = this.posX + 2;
        int lineWidth = this.browserEntryWidth - 4;
        for (WidgetItemIdStringListEditEntry entry : this.listWidgets) {
            if (entry.getListIndex() == dropIndex) {
                lineY = entry.getY() - 2;
                lineX = entry.getX();
                lineWidth = entry.getWidth();
                break;
            }
        }
        if (lineY == null && !this.listWidgets.isEmpty()) {
            WidgetItemIdStringListEditEntry last = this.listWidgets.get(this.listWidgets.size() - 1);
            if (last.getListIndex() >= 0 && dropIndex == last.getListIndex() + 1) {
                lineY = last.getY() + last.getHeight() + 2;
                lineX = last.getX();
                lineWidth = last.getWidth();
            }
        }

        // The drop-indicator line is a plain fill; a +400 Z push (matching
        // vanilla's tooltip offset) keeps it above the row backgrounds. Use
        // the actual row widget's own x/width (same values its background
        // rect uses) so the line reaches exactly as far right as the row
        // itself, instead of the list's browserEntryWidth which can be a
        // couple pixels narrower.
        if (lineY != null) {
            context.pose().pushMatrix();
            RenderUtils.drawRect(context, lineX, lineY, lineWidth, DRAG_LINE_HEIGHT, 0xFFFFCC00);
            context.pose().popMatrix();
        }

        // The dragged-row ghost label is drawn as a REAL vanilla tooltip. Item
        // icons in the GUI render through the item render layer, which
        // composites above plain immediate-mode glyphs even under a Z push --
        // so a hand-drawn label (or just its shadow) kept losing to the row
        // icons. Vanilla tooltips are the one thing guaranteed to paint over
        // item icons (every inventory item tooltip does), so route the label
        // through setComponentTooltipForNextFrame and let MC own the layering/shadow/position.
        String label = Configs.stripEntryDisabledPrefix(entries.get(this.dragIndex));
        // Prefer the item's localized display name over the raw configured id
        // (e.g. "移动：铁锭" instead of "移动：minecraft:iron_ingot"); fall back
        // to the raw text when the id doesn't resolve to a real item.
        ItemIdListIconResolver.Display display = ItemIdListIconResolver.currentIcon(label);
        String name = display.stack().isEmpty() ? label : ItemStackTexts.name(display.stack());
        String shown = name.isEmpty() ? StringUtils.translate("lmlp.gui.label.text_list.empty_entry") : name;
        String prefixed = StringUtils.translate("lmlp.gui.label.text_list.dragging", shown);
        Component text = Component.literal(prefixed).withStyle(ChatFormatting.GOLD);
        context.setComponentTooltipForNextFrame(Minecraft.getInstance().font, List.of(text), mouseX, mouseY);
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
        return new WidgetItemIdStringListEditEntry(
                x,
                y,
                this.browserEntryWidth,
                this.browserEntryHeight,
                listIndex,
                isOdd,
                value,
                this,
                this.editor
        );
    }
}
