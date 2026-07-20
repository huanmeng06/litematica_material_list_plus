package io.github.huanmeng06.lmlp.gui.textlist;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.gui.Icons;
import io.github.huanmeng06.lmlp.gui.ItemTooltipRenderer;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import io.github.huanmeng06.lmlp.recipe.jei.JeiRuntimeBridge;
import net.minecraft.class_1799;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.class_332;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class GuiItemIdStringListEdit extends GuiListBase<String, WidgetItemIdStringListEditEntry, WidgetListItemIdStringListEdit> {
    private static final int PANEL_MARGIN_X = 42;
    private static final int PANEL_MARGIN_TOP = 48;
    private static final int PANEL_MARGIN_BOTTOM = 36;
    private static final int PANEL_TITLE_HEIGHT = 34;
    private static final int TITLE_INFO_ICON_GAP = 6;
    private static final int PICKER_SLOT = 24;
    private static final int PICKER_SLOT_GAP = 2;
    private static final int PICKER_WHEEL_PIXELS = 28;
    private static final int PICKER_SORT_WIDTH = 138;
    private static SortMode pickerSortMode = SortMode.CREATIVE;
    private static final Collator NAME_COLLATOR = Collator.getInstance(Locale.getDefault());

    static {
        NAME_COLLATOR.setStrength(Collator.PRIMARY);
    }

    private final IConfigStringList config;
    private final IConfigGui configGui;
    private final IDialogHandler dialogHandler;
    private final Set<String> candidateWhitelist;
    private final Consumer<String> directSelectionHandler;
    private final List<String> editorEntries = new ArrayList<>();
    private final GuiScrollBar pickerScrollBar = new GuiScrollBar();
    private final ButtonGeneric pickerCloseButton = new ButtonGeneric(0, 0, 92, 20, "");
    private final ButtonGeneric pickerSortButton = new ButtonGeneric(0, 0, PICKER_SORT_WIDTH, 20, "");
    private final ButtonGeneric backButton = new ButtonGeneric(0, 0, 110, 20, "");
    private final List<Candidate> allCandidates = new ArrayList<>();
    private final List<Candidate> filteredCandidates = new ArrayList<>();
    private WidgetItemIdStringListEditEntry pickerTarget;
    private GuiTextFieldGeneric pickerSearchField;
    private class_1799 hoveredStack = class_1799.field_8037;
    private String hoveredText = "";
    private List<String> titleInfoTooltip = List.of();
    private String pickerQuery = "";
    private int pickerSearchFieldWidth;
    private int pickerSearchDragAnchor;
    private int dialogWidth;
    private int dialogHeight;
    private int dialogLeft;
    private int dialogTop;
    private boolean pickerOpen;
    private boolean pickerSearchFocused;
    private boolean draggingPickerSearch;
    private boolean draggingPickerScrollbar;
    private double pickerScrollRemainder;

    public GuiItemIdStringListEdit(IConfigStringList config, IConfigGui configGui, IDialogHandler dialogHandler, class_437 parent) {
        this(config, configGui, dialogHandler, parent, null, null);
    }

    private GuiItemIdStringListEdit(
            IConfigStringList config,
            IConfigGui configGui,
            IDialogHandler dialogHandler,
            class_437 parent,
            Collection<String> candidateWhitelist,
            Consumer<String> directSelectionHandler) {
        super(0, 0);
        this.config = config;
        this.configGui = configGui;
        this.dialogHandler = dialogHandler;
        this.candidateWhitelist = candidateWhitelist == null ? null : Set.copyOf(candidateWhitelist);
        this.directSelectionHandler = directSelectionHandler;
        if (config != null) {
            this.editorEntries.addAll(config.getStrings());
            this.title = StringUtils.translate("malilib.gui.title.string_list_edit", config.getName());
        } else {
            this.title = StringUtils.translate("lmlp.gui.item_id_picker.title");
        }
        this.pickerCloseButton.setDisplayString(StringUtils.translate("lmlp.gui.button.item_id_picker.close"));
        this.pickerCloseButton.setTextCentered(true);
        this.pickerCloseButton.setActionListener((button, mouseButton) -> this.closeItemPicker());
        this.pickerSortButton.setTextCentered(true);
        this.pickerSortButton.setActionListener((button, mouseButton) -> this.cyclePickerSortMode());
        this.updatePickerSortButton();
        this.backButton.setDisplayString(StringUtils.translate("lmlp.gui.button.item_id_picker.close"));
        this.backButton.setTextCentered(true);
        this.backButton.setActionListener((button, mouseButton) -> this.closeEditor());
        if (directSelectionHandler != null) {
            this.pickerOpen = true;
            this.pickerSearchFocused = true;
        }
        if (dialogHandler == null) {
            this.setParent(parent);
        }
    }

    /** Opens the same JEI picker used by item-ID list fields, restricted to the supplied IDs. */
    public static GuiItemIdStringListEdit createRestrictedPicker(
            Collection<String> allowedItemIds,
            Consumer<String> selectionHandler,
            class_437 parent) {
        return new GuiItemIdStringListEdit(
                null,
                null,
                null,
                parent,
                allowedItemIds,
                selectionHandler
        );
    }

    public IConfigStringList getConfig() {
        return this.config;
    }

    List<String> getEditorEntries() {
        return this.editorEntries;
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
        if (this.pickerSearchField != null) {
            this.pickerSearchField.method_25365(true);
        }
        this.updatePickerSortButton();
        this.updateFilteredCandidates();
    }

    @Override
    public void initGui() {
        this.setWidthAndHeight();
        this.reCreateListWidget();
        super.initGui();
        if (this.pickerOpen && this.allCandidates.isEmpty()) {
            this.reloadCandidates();
            this.updateFilteredCandidates();
        }
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
        if (this.directSelectionHandler == null) {
            this.saveEditorEntries();
        }
        super.method_25432();
    }

    @Override
    public void method_25394(class_332 drawContext, int mouseX, int mouseY, float delta) {
        GuiContext context = GuiContext.fromGuiGraphics(drawContext);
        this.hoveredStack = class_1799.field_8037;
        this.hoveredText = "";
        this.titleInfoTooltip = List.of();

        if (this.pickerOpen) {
            this.renderPicker(context, mouseX, mouseY, delta);
            this.renderActiveTooltip(context, mouseX, mouseY);
            this.drawGuiMessages(context);
            return;
        }

        this.drawScreenBackground(context, mouseX, mouseY);
        this.drawTitle(context, mouseX, mouseY, delta);
        this.drawContents(context, mouseX, mouseY, delta);
        this.drawHoveredWidget(context, mouseX, mouseY);
        this.updateBackButtonPosition();
        this.backButton.render(context, mouseX, mouseY, this.backButton.isMouseOver());
        this.renderActiveTooltip(context, mouseX, mouseY);
        this.drawGuiMessages(context);
    }

    private void renderActiveTooltip(GuiContext context, int mouseX, int mouseY) {
        if (!this.hoveredStack.method_7960()) {
            ItemTooltipRenderer.render(context, this.font, this.hoveredStack, mouseX, mouseY);
        } else if (!this.hoveredText.isEmpty()) {
            RenderUtils.drawHoverText(context, mouseX, mouseY, List.of(this.hoveredText));
        } else if (!this.titleInfoTooltip.isEmpty()) {
            RenderUtils.drawHoverText(context, mouseX, mouseY, this.titleInfoTooltip);
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
    public boolean method_25402(net.minecraft.class_11909 event, boolean doubleClick) {
        double mouseX = event.comp_4798();
        double mouseY = event.comp_4799();
        int button = event.comp_4800().comp_4801();
        if (this.pickerOpen) {
            return button == 0 ? this.onPickerClicked(event, doubleClick) : true;
        }
        if (this.backButton.onMouseClicked(event, doubleClick)) {
            return true;
        }
        return super.method_25402(event, doubleClick);
    }

    @Override
    public boolean method_25406(net.minecraft.class_11909 event) {
        if (this.pickerOpen) {
            this.draggingPickerSearch = false;
            this.draggingPickerScrollbar = false;
            this.pickerScrollBar.setIsDragging(false);
            return true;
        }
        return super.method_25406(event);
    }

    @Override
    public boolean method_25403(net.minecraft.class_11909 event, double deltaX, double deltaY) {
        double mouseX = event.comp_4798();
        double mouseY = event.comp_4799();
        int button = event.comp_4800().comp_4801();
        if (this.pickerOpen) {
            if (button == 0 && this.draggingPickerSearch) {
                GuiTextFieldGeneric field = this.ensurePickerSearchField(this.pickerLayout());
                field.method_25348(event, true);
                field.method_1884(this.pickerSearchDragAnchor);
            }
            return true;
        }
        return super.method_25403(event, deltaX, deltaY);
    }

    @Override
    public boolean method_25404(net.minecraft.class_11908 event) {
        int keyCode = event.comp_4795();
        int scanCode = event.comp_4796();
        int modifiers = event.comp_4797();
        if (this.pickerOpen) {
            return this.onPickerKeyPressed(event);
        }
        return super.method_25404(event);
    }

    @Override
    public boolean method_25400(net.minecraft.class_11905 event) {
        if (this.pickerOpen) {
            this.ensurePickerSearchField(this.pickerLayout()).method_25400(event);
            return true;
        }
        return super.method_25400(event);
    }

    @Override
    public boolean onKeyTyped(net.minecraft.class_11908 event) {
        int keyCode = event.comp_4795();
        int scanCode = event.comp_4796();
        int modifiers = event.comp_4797();
        if (this.pickerOpen) {
            return this.onPickerKeyPressed(event);
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.dialogHandler != null) {
            this.closeEditor();
            return true;
        }
        return super.onKeyTyped(event);
    }

    @Override
    protected void drawScreenBackground(GuiContext context, int mouseX, int mouseY) {
        RenderUtils.drawRect(context, 0, 0, GuiUtils.getScaledWindowWidth(), GuiUtils.getScaledWindowHeight(),
                0x88000000);
        RenderUtils.drawOutlinedBox(context, this.dialogLeft, this.dialogTop, this.dialogWidth, this.dialogHeight, 0xD6000000, 0xFF999999);
    }

    @Override
    protected void drawTitle(GuiContext context, int mouseX, int mouseY, float partialTicks) {
        int textY = this.dialogTop + (PANEL_TITLE_HEIGHT - this.font.field_2000) / 2;
        this.drawStringWithShadow(context, this.title, this.dialogLeft + 10, textY, 0xFFFFFFFF);

        // "More info" icon next to the title: hovering it explains the
        // {color}/{wood} wildcards supported by this item-id list editor.
        int iconX = this.dialogLeft + 10 + this.font.method_1727(this.title) + TITLE_INFO_ICON_GAP;
        int iconY = this.dialogTop + (PANEL_TITLE_HEIGHT - Icons.INFO_11.getHeight()) / 2;
        boolean hovered = GuiBase.isMouseOver(mouseX, mouseY, iconX, iconY, Icons.INFO_11.getWidth(), Icons.INFO_11.getHeight());
        Icons.INFO_11.renderAt(context, iconX, iconY, 0.0F, hovered, false);
        if (hovered) {
            this.titleInfoTooltip = List.of(
                    StringUtils.translate("lmlp.gui.tooltip.wildcards.title"),
                    StringUtils.translate("lmlp.gui.tooltip.wildcards.color"),
                    StringUtils.translate("lmlp.gui.tooltip.wildcards.wood")
            );
        }
    }

    private void setWidthAndHeight() {
        int screenWidth = GuiUtils.getScaledWindowWidth();
        int screenHeight = GuiUtils.getScaledWindowHeight();
        // Never let the dialog exceed the window: the old `max(520, ...)` floor
        // forced 520px even on small windows / high GUI scale, clipping the
        // right edge (and the back button) off screen.
        this.dialogWidth = Math.min(screenWidth - 16, Math.max(360, screenWidth - PANEL_MARGIN_X * 2));

        // The dialog is pinned at PANEL_MARGIN_TOP, so its height must be capped
        // to the room BELOW that top, not just to a raw `screenHeight - 16`.
        // The old cap ignored the top offset, so at small windows / high GUI
        // scale `dialogTop + dialogHeight` ran past the screen bottom and the
        // last rows were clipped off. Cap to the available space; if even the
        // 220px minimum won't fit, pull the top up to reclaim room.
        int bottomMargin = 8;
        int top = PANEL_MARGIN_TOP;
        int desired = Math.max(220, screenHeight - PANEL_MARGIN_TOP - PANEL_MARGIN_BOTTOM);
        int height = Math.min(desired, screenHeight - top - bottomMargin);
        if (height < 220) {
            top = Math.max(8, screenHeight - bottomMargin - 220);
            height = Math.min(220, screenHeight - top - bottomMargin);
        }
        this.dialogHeight = height;
        this.dialogLeft = Math.max(8, (screenWidth - this.dialogWidth) / 2);
        this.dialogTop = top;
        this.updateBackButtonPosition();
    }

    private void updateBackButtonPosition() {
        this.backButton.setPosition(this.dialogLeft + this.dialogWidth - 120, this.dialogTop + 7);
    }

    private void closeEditor() {
        if (this.directSelectionHandler != null) {
            this.closeGui(true);
            return;
        }

        this.saveEditorEntries();
        if (this.dialogHandler != null) {
            this.dialogHandler.closeDialog();
        } else {
            this.closeGui(true);
        }
    }

    private void saveEditorEntries() {
        if (this.config == null || this.configGui == null) {
            return;
        }

        WidgetListItemIdStringListEdit list = this.getListWidget();
        if (list != null) {
            list.applyPendingModifications();
        }

        this.config.setStrings(new ArrayList<>(this.editorEntries));
        ConfigManager.getInstance().onConfigsChanged(this.configGui.getModId());
    }

    private void closeItemPicker() {
        if (this.directSelectionHandler != null) {
            this.closeGui(true);
            return;
        }

        this.pickerOpen = false;
        this.pickerTarget = null;
        this.pickerSearchFocused = false;
        this.draggingPickerSearch = false;
        this.draggingPickerScrollbar = false;
        if (this.pickerSearchField != null) {
            this.pickerSearchField.method_25365(false);
        }
        this.pickerScrollBar.setIsDragging(false);
    }

    private void reloadCandidates() {
        Map<String, Integer> creativeOrder = CreativeItemOrderResolver.createOrder();
        Map<String, Candidate> byId = new LinkedHashMap<>();
        int originalIndex = 0;
        List<class_1799> jeiItems = JeiRuntimeBridge.runtime()
                .map(runtime -> List.copyOf(runtime.getIngredientManager().getAllItemStacks()))
                .orElseGet(List::of);
        for (class_1799 stack : jeiItems) {
            if (stack.method_7960()) {
                continue;
            }
            String id = ItemStackTexts.id(stack);
            if (this.candidateWhitelist != null && !this.candidateWhitelist.contains(id)) {
                continue;
            }
            String name = ItemStackTexts.name(stack);
            byId.putIfAbsent(id, new Candidate(
                    stack.method_7972(),
                    id,
                    name,
                    normalizeDisplayName(name),
                    creativeOrder.getOrDefault(id, Integer.MAX_VALUE),
                    originalIndex
            ));
            originalIndex++;
        }

        this.allCandidates.clear();
        this.allCandidates.addAll(byId.values());
        this.sortCandidates(this.allCandidates);
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
        this.sortCandidates(this.filteredCandidates);
        this.pickerScrollBar.setValue(0);
    }

    private void renderPicker(GuiContext context, int mouseX, int mouseY, float delta) {
        PickerLayout layout = this.pickerLayout();
        this.hoveredStack = class_1799.field_8037;
        this.hoveredText = "";
        this.pickerCloseButton.setPosition(layout.right() - 96, layout.top() + 8);
        this.pickerSortButton.setPosition(layout.gridLeft() + 60, this.pickerSortY(layout));
        this.updatePickerSortButton();
        this.pickerScrollBar.setMaxValue(Math.max(0, this.pickerContentHeight(layout) - layout.gridHeight()));

        RenderUtils.drawRect(context, 0, 0, GuiUtils.getScaledWindowWidth(), GuiUtils.getScaledWindowHeight(),
                0x66000000);
        RenderUtils.drawOutlinedBox(context, layout.left(), layout.top(), layout.width(), layout.height(), 0xEE000000, 0xFFAAAAAA);
        context.method_51433(this.font, StringUtils.translate("lmlp.gui.item_id_picker.title"), layout.left() + 10, layout.top() + 12, 0xFFFFFFFF, false);
        this.pickerCloseButton.render(context, mouseX, mouseY, false);
        this.renderPickerSearch(context, layout, mouseX, mouseY, delta);
        this.renderPickerSort(context, layout, mouseX, mouseY);
        this.renderPickerGrid(context, layout, mouseX, mouseY);
        if (this.pickerScrollBar.getMaxValue() > 0) {
            this.pickerScrollBar.render(context, mouseX, mouseY, delta, layout.scrollbarX(), layout.gridTop(), 8, layout.gridHeight(), this.pickerContentHeight(layout));
        }
    }

    private void renderPickerSearch(GuiContext context, PickerLayout layout, int mouseX, int mouseY, float delta) {
        context.method_51433(this.font, StringUtils.translate("lmlp.gui.item_id_picker.search"), layout.gridLeft(), layout.top() + 36, 0xFFE0E0E0, false);
        this.ensurePickerSearchField(layout).method_48579(context, mouseX, mouseY, delta);
    }

    private void renderPickerSort(GuiContext context, PickerLayout layout, int mouseX, int mouseY) {
        int y = this.pickerSortY(layout);
        context.method_51433(this.font, StringUtils.translate("lmlp.gui.item_id_picker.sort"), layout.gridLeft(), y + 6, 0xFFE0E0E0, false);
        this.pickerSortButton.render(context, mouseX, mouseY, this.pickerSortButton.isMouseOver());
    }

    private void renderPickerGrid(GuiContext context, PickerLayout layout, int mouseX, int mouseY) {
        RenderUtils.drawOutlinedBox(context, layout.gridLeft(), layout.gridTop(), layout.gridWidth(), layout.gridHeight(), 0xDD000000, 0xFF888888);
        context.method_44379(layout.gridLeft() + 1, layout.gridTop() + 1, layout.gridRight() - 1, layout.gridBottom() - 1);
        if (this.filteredCandidates.isEmpty()) {
            context.method_51433(this.font, StringUtils.translate("lmlp.gui.item_id_picker.empty"), layout.gridLeft() + 8, layout.gridTop() + 10, 0xFFFFCC66, false);
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

    private void renderPickerSlot(GuiContext context, Candidate candidate, int x, int y, int mouseX, int mouseY) {
        boolean hovered = GuiBase.isMouseOver(mouseX, mouseY, x, y, PICKER_SLOT, PICKER_SLOT);
        RenderUtils.drawOutlinedBox(context, x, y, PICKER_SLOT, PICKER_SLOT, hovered ? 0xA0707070 : 0xA0404040, hovered ? 0xFFFFFF88 : 0xFF888888);
        context.method_51427(candidate.stack(), x + 4, y + 4);
        if (hovered) {
            this.hoveredStack = candidate.stack();
            this.hoveredText = candidate.id();
        }
    }

    private boolean onPickerClicked(net.minecraft.class_11909 event, boolean doubleClick) {
        double mouseX = event.comp_4798();
        double mouseY = event.comp_4799();
        int button = event.comp_4800().comp_4801();
        PickerLayout layout = this.pickerLayout();
        GuiTextFieldGeneric searchField = this.ensurePickerSearchField(layout);
        if (this.pickerCloseButton.onMouseClicked(event, doubleClick)) {
            return true;
        }
        if (this.pickerSortButton.onMouseClicked(event, doubleClick)) {
            return true;
        }
        if (searchField.method_25402(event, doubleClick)) {
            this.pickerSearchFocused = true;
            this.draggingPickerSearch = button == 0 && searchField.isMouseOver((int) mouseX, (int) mouseY);
            this.pickerSearchDragAnchor = searchField.method_1881();
            return true;
        }
        this.pickerSearchFocused = false;
        this.draggingPickerSearch = false;
        searchField.method_25365(false);

        int index = this.pickerIndexAt(layout, mouseX, mouseY);
        if (index >= 0) {
            String selectedId = this.filteredCandidates.get(index).id();
            if (this.directSelectionHandler != null) {
                this.directSelectionHandler.accept(selectedId);
                this.closeGui(true);
                return true;
            }
            if (this.pickerTarget != null) {
                this.pickerTarget.setSelectedItemId(selectedId);
                this.closeItemPicker();
                return true;
            }
        }

        if (this.pickerScrollBar.wasMouseOver()) {
            this.pickerScrollBar.setIsDragging(true);
            this.draggingPickerScrollbar = true;
        }
        return true;
    }

    private boolean onPickerKeyPressed(net.minecraft.class_11908 event) {
        int keyCode = event.comp_4795();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.closeItemPicker();
            return true;
        }
        this.ensurePickerSearchField(this.pickerLayout()).method_25404(event);
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
        int width = Math.min(460, Math.max(340, GuiUtils.getScaledWindowWidth() - 160));
        int height = Math.min(370, Math.max(280, GuiUtils.getScaledWindowHeight() - 120));
        int left = Math.max(10, (GuiUtils.getScaledWindowWidth() - width) / 2);
        int top = Math.max(10, (GuiUtils.getScaledWindowHeight() - height) / 2);
        int margin = 10;
        int scrollbarWidth = 8;
        int scrollbarGap = 6;
        int gridLeft = left + margin;
        int gridTop = top + 104;
        int contentWidth = Math.max(PICKER_SLOT + 8, width - margin * 2 - scrollbarWidth - scrollbarGap);
        int columns = Math.max(1, (contentWidth - 8 + PICKER_SLOT_GAP) / (PICKER_SLOT + PICKER_SLOT_GAP));
        int gridWidth = Math.min(contentWidth, 8 + columns * PICKER_SLOT + (columns - 1) * PICKER_SLOT_GAP);
        int gridHeight = Math.max(80, height - 118);
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

        int index = row * layout.columns();
        index += column;
        return index >= 0 && index < this.filteredCandidates.size() ? index : -1;
    }

    private GuiTextFieldGeneric ensurePickerSearchField(PickerLayout layout) {
        int x = layout.gridLeft();
        int y = this.pickerSearchY(layout);
        int width = layout.gridWidth();
        if (this.pickerSearchField == null || this.pickerSearchFieldWidth != width) {
            GuiTextFieldGeneric field = new GuiTextFieldGeneric(x, y, width, 20, this.font);
            field.method_1880(128);
            field.method_1852(this.pickerQuery);
            field.method_1863(value -> {
                if (!this.pickerQuery.equals(value)) {
                    this.pickerQuery = value;
                    this.updateFilteredCandidates();
                }
            });
            field.method_25365(this.pickerSearchFocused);
            this.pickerSearchField = field;
            this.pickerSearchFieldWidth = width;
        } else {
            this.pickerSearchField.method_46421(x);
            this.pickerSearchField.method_46419(y);
            this.pickerSearchField.method_25365(this.pickerSearchFocused);
        }

        return this.pickerSearchField;
    }

    private int pickerSearchY(PickerLayout layout) {
        return layout.top() + 52;
    }

    private int pickerSortY(PickerLayout layout) {
        return layout.top() + 78;
    }

    private int pickerContentHeight(PickerLayout layout) {
        int rows = Math.max(1, (this.filteredCandidates.size() + layout.columns() - 1) / layout.columns());
        return rows * layout.cellStride() + 8;
    }

    private void sortCandidates(List<Candidate> candidates) {
        candidates.sort(this.candidateComparator());
    }

    private Comparator<Candidate> candidateComparator() {
        return switch (pickerSortMode) {
            case CREATIVE -> Comparator
                    .comparingInt(Candidate::creativeOrder)
                    .thenComparingInt(GuiItemIdStringListEdit::namespacePriority)
                    .thenComparing(Candidate::namespace)
                    .thenComparing(Candidate::path)
                    .thenComparing(Candidate::id)
                    .thenComparingInt(Candidate::originalIndex);
            case NAME -> (a, b) -> {
                int compare = NAME_COLLATOR.compare(a.normalizedName(), b.normalizedName());
                if (compare != 0) {
                    return compare;
                }

                compare = a.id().compareTo(b.id());
                return compare != 0 ? compare : Integer.compare(a.originalIndex(), b.originalIndex());
            };
            case NAMESPACE -> Comparator
                    .comparingInt(GuiItemIdStringListEdit::namespacePriority)
                    .thenComparing(Candidate::namespace)
                    .thenComparing(Candidate::path)
                    .thenComparing(Candidate::id)
                    .thenComparingInt(Candidate::originalIndex);
            case REGISTRY_ID -> Comparator
                    .comparing(Candidate::id)
                    .thenComparingInt(Candidate::originalIndex);
        };
    }

    private static String normalizeDisplayName(String name) {
        String raw = name == null ? "" : name;
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char character = raw.charAt(i);
            if (character == 167 && i + 1 < raw.length()) {
                i++;
                continue;
            }
            builder.append(character);
        }

        return builder.toString().trim().toLowerCase(Locale.ROOT);
    }
    private static int namespacePriority(Candidate candidate) {
        return "minecraft".equals(candidate.namespace()) ? 0 : 1;
    }

    private void updatePickerSortButton() {
        this.pickerSortButton.setDisplayString(StringUtils.translate("lmlp.gui.item_id_picker.sort_mode." + pickerSortMode.key));
    }

    private void cyclePickerSortMode() {
        pickerSortMode = pickerSortMode.next();
        this.updatePickerSortButton();
        this.updateFilteredCandidates();
    }

    private enum SortMode {
        CREATIVE("creative"),
        NAME("name"),
        NAMESPACE("namespace"),
        REGISTRY_ID("registry_id");

        private final String key;

        SortMode(String key) {
            this.key = key;
        }

        private SortMode next() {
            SortMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private record Candidate(class_1799 stack, String id, String name, String normalizedName, int creativeOrder, int originalIndex) {
        private String namespace() {
            int separator = this.id.indexOf(':');
            return separator > 0 ? this.id.substring(0, separator) : "minecraft";
        }

        private String path() {
            int separator = this.id.indexOf(':');
            return separator >= 0 && separator < this.id.length() - 1 ? this.id.substring(separator + 1) : this.id;
        }
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
