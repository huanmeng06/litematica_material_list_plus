package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialList;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.WoodFamily;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementMode;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementRow;
import net.minecraft.class_1799;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Material-list entry point for preference replacement.
 *
 * <p>The form intentionally uses the exact same native MaLiLib preference list as the main
 * config screen, but omits the four config-category tabs. The two bottom actions remain because
 * this entry point must either continue to the schematic save screen or return to the material
 * list.</p>
 */
public final class GuiPreferredMaterialForm extends GuiConfigsBase {
    private static final int CONFIG_WIDTH = 140;
    private static final int ACTION_WIDTH = 90;
    private static final int ACTION_GAP = 4;
    private static final int BOTTOM_ACTION_SPACE = 34;
    private static final int DETAIL_TOP = 126;
    private static final int DETAIL_BOTTOM_MARGIN = 42;
    private static final int DETAIL_MARGIN = 10;
    private static final int DETAIL_ROW_HEIGHT = 22;
    private static final int DETAIL_ROW_GAP = 0;
    private static final int DETAIL_ACTION_WIDTH = 92;
    private static final int DETAIL_WHEEL_PIXELS = 32;
    private static final int DETAIL_ROW_ODD = 0xA0101010;
    private static final int DETAIL_ROW_EVEN = 0xA0303030;
    private static final int DETAIL_ROW_HOVERED = 0xA0707070;
    private static final int DETAIL_ICON_BACKGROUND = 0x20FFFFFF;
    private static final int ARROW_SLOT_WIDTH = 20;
    private static final String DETAIL_ANIMATION_KEY = "material_preferred_wood_details";

    private final class_437 materialListParent;
    private final SchematicPlacement placement;
    private final LitematicaSchematic schematic;
    private final boolean initialWoodEnabled;
    private final WoodFamily initialWoodFamily;
    private final GuiScrollBar detailScrollBar = new GuiScrollBar();
    private final ExpandAnimationTracker detailAnimations = new ExpandAnimationTracker();
    private final List<RowState> rows = new ArrayList<>();
    private boolean detailsExpanded;
    private boolean draggingDetailScrollbar;
    private double detailScrollRemainder;
    private boolean rowsWoodEnabled;
    private WoodFamily rowsWoodFamily;
    private boolean closingConfirmed;

    private GuiPreferredMaterialForm(class_437 parent, SchematicPlacement placement) {
        super(
                10,
                50,
                LitematicaMaterialListPlus.MOD_ID,
                parent,
                "lmlp.gui.preferred_replacement.title"
        );
        this.materialListParent = parent;
        this.placement = placement;
        this.schematic = placement == null ? null : placement.getSchematic();
        this.initialWoodEnabled = Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue();
        this.initialWoodFamily = (WoodFamily) Configs.ConfigForms.PREFERRED_WOOD_FAMILY.getOptionListValue();
        this.rebuildRows();
        if (this.rowsWoodEnabled) {
            this.detailsExpanded = true;
            this.detailAnimations.start(DETAIL_ANIMATION_KEY, 0.0F, 1.0F);
        }
    }

    public static GuiPreferredMaterialForm forMaterialList(class_437 parent, MaterialListBase materialList) {
        return new GuiPreferredMaterialForm(parent, resolvePlacement(materialList));
    }

    public boolean hasSchematic() {
        return this.schematic != null;
    }

    @Override
    public void initGui() {
        this.clearOptions();
        super.initGui();

        int y = this.field_22790 - 30;
        int cancelX = this.field_22789 - 10 - ACTION_WIDTH;
        int confirmX = cancelX - ACTION_GAP - ACTION_WIDTH;

        ButtonGeneric confirm = new ButtonGeneric(
                confirmX,
                y,
                ACTION_WIDTH,
                20,
                StringUtils.translate("lmlp.gui.preferred_replacement.confirm")
        );
        confirm.setTextCentered(true);
        this.addButton(confirm, (button, mouseButton) -> this.confirm());

        ButtonGeneric cancel = new ButtonGeneric(
                cancelX,
                y,
                ACTION_WIDTH,
                20,
                StringUtils.translate("lmlp.gui.preferred_replacement.cancel")
        );
        cancel.setTextCentered(true);
        this.addButton(cancel, (button, mouseButton) -> this.cancel());
        this.updateKeybindButtons();
        this.rebuildRowsIfNeeded();
    }

    @Override
    public boolean onMouseClicked(class_11909 mouseClick, boolean doubleClick) {
        boolean preferredWoodWasEnabled = Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue();
        WoodFamily preferredWoodWas = this.currentWoodFamily();

        int mouseX = (int) mouseClick.comp_4798();
        int mouseY = (int) mouseClick.comp_4799();
        int mouseButton = mouseClick.comp_4800().comp_4801();
        if ((mouseButton == 0 || mouseButton == 1)
                && this.isDetailArrowVisible()
                && this.isDetailArrowHovered(mouseX, mouseY)) {
            this.toggleDetailsExpanded();
            return true;
        }
        if ((mouseButton == 0 || mouseButton == 1) && this.clickDetailRowButton(mouseClick, doubleClick)) {
            return true;
        }
        if (mouseButton == 0 && this.isDetailsVisible() && this.detailScrollBar.wasMouseOver()) {
            this.detailScrollBar.setIsDragging(true);
            this.draggingDetailScrollbar = true;
            return true;
        }

        boolean handled = super.onMouseClicked(mouseClick, doubleClick);
        boolean preferredWoodIsEnabled = Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue();
        WoodFamily preferredWoodIs = this.currentWoodFamily();

        if (preferredWoodWasEnabled != preferredWoodIsEnabled
                && this.getListWidget() instanceof PreferenceWidgetListConfigOptions preferenceList) {
            preferenceList.setGroupExpanded(Configs.ConfigForms.PREFERRED_WOOD_ENABLED, preferredWoodIsEnabled);
        }
        if (preferredWoodWasEnabled != preferredWoodIsEnabled || preferredWoodWas != preferredWoodIs) {
            if (!preferredWoodIsEnabled) {
                this.detailsExpanded = false;
                this.detailAnimations.clear();
                this.detailScrollBar.setValue(0);
            } else if (!preferredWoodWasEnabled) {
                this.detailsExpanded = true;
                this.detailAnimations.start(DETAIL_ANIMATION_KEY, 0.0F, 1.0F);
            }
            this.rebuildRows();
        }

        this.updateKeybindButtons();
        return handled;
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.isDetailsVisible() && this.isMouseOverDetails((int) mouseX, (int) mouseY)) {
            double target = this.detailScrollRemainder - verticalAmount * DETAIL_WHEEL_PIXELS;
            int pixels = (int) target;
            this.detailScrollRemainder = target - pixels;
            this.detailScrollBar.offsetValue(pixels);
            return true;
        }
        return super.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean method_25403(class_11909 event, double deltaX, double deltaY) {
        return this.draggingDetailScrollbar || super.method_25403(event, deltaX, deltaY);
    }

    @Override
    public boolean method_25406(class_11909 event) {
        this.draggingDetailScrollbar = false;
        this.detailScrollBar.setIsDragging(false);
        return super.method_25406(event);
    }

    @Override
    public boolean onKeyTyped(class_11908 keyInput) {
        if (keyInput.comp_4795() == GLFW.GLFW_KEY_ESCAPE) {
            this.cancel();
            return true;
        }
        return super.onKeyTyped(keyInput);
    }

    @Override
    protected WidgetListConfigOptions createListWidget(int x, int y) {
        return new PreferenceWidgetListConfigOptions(
                x,
                y,
                this.getBrowserWidth(),
                this.getBrowserHeight(),
                this.getConfigWidth(),
                0.0F,
                this.useKeybindSearch(),
                this
        );
    }

    @Override
    protected int getBrowserHeight() {
        return Math.max(40, super.getBrowserHeight() - BOTTOM_ACTION_SPACE);
    }

    @Override
    protected boolean useKeybindSearch() {
        return true;
    }

    @Override
    protected int getConfigWidth() {
        return CONFIG_WIDTH;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        Collection<? extends fi.dy.masa.malilib.config.IConfigBase> configs = Configs.ConfigForms.PREFERENCE_OPTIONS;
        return ConfigOptionWrapper.createFor(configs);
    }

    @Override
    public void drawContents(GuiContext context, int mouseX, int mouseY, float partialTicks) {
        super.drawContents(context, mouseX, mouseY, partialTicks);
        this.renderDetailArrow(context, mouseX, mouseY);
        this.renderReplacementDetails(context, mouseX, mouseY, partialTicks);
    }

    private void renderDetailArrow(GuiContext context, int mouseX, int mouseY) {
        if (!this.isDetailArrowVisible()) {
            this.detailAnimations.prune();
            return;
        }

        int arrowX = this.detailArrowX();
        ToggleArrowRenderer.render(
                context,
                arrowX,
                ARROW_SLOT_WIDTH,
                this.preferenceToggleRowTop() + 11,
                this.detailProgress(),
                this.isDetailArrowHovered(mouseX, mouseY)
        );
        this.detailAnimations.prune();
    }

    private void renderReplacementDetails(GuiContext context, int mouseX, int mouseY, float partialTicks) {
        float progress = this.detailProgress();
        if (!this.isDetailArrowVisible() || progress <= 0.001F) {
            return;
        }

        this.rebuildRowsIfNeeded();
        int fullBottom = this.detailFullBottom();
        int fullHeight = Math.max(0, fullBottom - DETAIL_TOP);
        int visibleHeight = Math.round(fullHeight * progress);
        if (visibleHeight <= 0) {
            return;
        }

        int visibleBottom = DETAIL_TOP + visibleHeight;
        this.updateDetailScrollRange(fullHeight);
        context.method_44379(DETAIL_MARGIN, DETAIL_TOP, this.field_22789 - DETAIL_MARGIN, visibleBottom);

        if (this.rows.isEmpty()) {
            this.renderCenteredDetailMessage(
                    context,
                    StringUtils.translate("lmlp.gui.preferred_replacement.none"),
                    DETAIL_TOP + 24
            );
        } else {
            int y = DETAIL_TOP - this.detailScrollBar.getValue();
            for (int index = 0; index < this.rows.size(); index++) {
                RowState row = this.rows.get(index);
                if (y + DETAIL_ROW_HEIGHT >= DETAIL_TOP && y < visibleBottom) {
                    this.renderDetailRow(context, row, index, y, mouseX, mouseY);
                }
                y += DETAIL_ROW_HEIGHT + DETAIL_ROW_GAP;
            }
        }
        context.method_44380();

        if (this.detailScrollBar.getMaxValue() > 0 && visibleHeight > 24) {
            this.detailScrollBar.render(
                    context,
                    mouseX,
                    mouseY,
                    partialTicks,
                    this.field_22789 - 13,
                    DETAIL_TOP,
                    8,
                    visibleHeight,
                    this.detailContentHeight()
            );
        }
    }

    private void renderDetailRow(
            GuiContext context,
            RowState row,
            int index,
            int y,
            int mouseX,
            int mouseY) {
        int rowLeft = DETAIL_MARGIN;
        int rowRight = this.field_22789 - DETAIL_MARGIN - 12;
        boolean hovered = mouseX >= rowLeft
                && mouseX < rowRight
                && mouseY >= y
                && mouseY < y + DETAIL_ROW_HEIGHT;
        int background = hovered
                ? DETAIL_ROW_HOVERED
                : (index & 1) == 0 ? DETAIL_ROW_EVEN : DETAIL_ROW_ODD;
        RenderUtils.drawRect(context, rowLeft, y, rowRight - rowLeft, DETAIL_ROW_HEIGHT, background);

        int sourceIconX = rowLeft + 4;
        int targetIconX = rowLeft + 24;
        int iconY = y + 3;
        if (row.row.sourceBlock() != null) {
            RenderUtils.drawRect(context, sourceIconX, iconY, 16, 16, DETAIL_ICON_BACKGROUND);
            context.method_51427(new class_1799(row.row.sourceBlock()), sourceIconX, iconY);
        }
        if (row.row.targetBlock() != null) {
            RenderUtils.drawRect(context, targetIconX, iconY, 16, 16, DETAIL_ICON_BACKGROUND);
            context.method_51427(new class_1799(row.row.targetBlock()), targetIconX, iconY);
        }

        int textX = rowLeft + 46;
        int buttonX = rowRight - DETAIL_ACTION_WIDTH - 4;
        int availableTextWidth = Math.max(0, buttonX - 8 - textX);
        int countX = textX + Math.round(availableTextWidth * 0.55F);
        int statusX = textX + Math.round(availableTextWidth * 0.76F);
        String targetName = row.row.targetBlock() == null
                ? StringUtils.translate("lmlp.gui.preferred_replacement.no_target")
                : row.row.targetName();
        String replacement = row.row.sourceName() + "  →  " + targetName;
        replacement = this.truncateDetailText(replacement, Math.max(0, countX - textX - 8));
        context.method_51433(
                this.field_22793,
                replacement,
                textX,
                y + 7,
                0xFFFFFFFF,
                false
        );

        String count = StringUtils.translate("lmlp.gui.preferred_replacement.count", row.row.count());
        context.method_51433(this.field_22793, count, countX, y + 7, 0xFFFFFFFF, false);

        String status = StringUtils.translate(row.row.exact()
                ? "lmlp.gui.preferred_replacement.compatible"
                : "lmlp.gui.preferred_replacement.incompatible");
        context.method_51433(
                this.field_22793,
                status,
                statusX,
                y + 7,
                row.row.exact() ? 0xFF88DD88 : 0xFFFFAA55,
                false
        );

        row.button.setPosition(buttonX, y + 1);
        row.button.render(context, mouseX, mouseY, row.button.isMouseOver());
    }

    private String truncateDetailText(String text, int maxWidth) {
        if (maxWidth <= 0 || this.field_22793.method_1727(text) <= maxWidth) {
            return maxWidth <= 0 ? "" : text;
        }

        String ellipsis = "…";
        int ellipsisWidth = this.field_22793.method_1727(ellipsis);
        int end = text.length();
        while (end > 0 && this.field_22793.method_1727(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private void renderCenteredDetailMessage(GuiContext context, String message, int y) {
        int x = Math.max(DETAIL_MARGIN, (this.field_22789 - this.field_22793.method_1727(message)) / 2);
        context.method_51433(this.field_22793, message, x, y, 0xFFFFCC66, false);
    }

    private boolean clickDetailRowButton(class_11909 event, boolean doubleClick) {
        if (!this.isDetailsVisible()) {
            return false;
        }

        int mouseY = (int) event.comp_4799();
        int visibleBottom = this.detailVisibleBottom();
        if (mouseY < DETAIL_TOP || mouseY >= visibleBottom) {
            return false;
        }
        int y = DETAIL_TOP - this.detailScrollBar.getValue();
        for (RowState row : this.rows) {
            if (y + DETAIL_ROW_HEIGHT >= DETAIL_TOP && y < visibleBottom
                    && row.button.onMouseClicked(event, doubleClick)) {
                return true;
            }
            y += DETAIL_ROW_HEIGHT + DETAIL_ROW_GAP;
        }
        return false;
    }

    private void toggleDetailsExpanded() {
        float startProgress = this.detailProgress();
        this.detailsExpanded = !this.detailsExpanded;
        this.detailAnimations.start(
                DETAIL_ANIMATION_KEY,
                startProgress,
                this.detailsExpanded ? 1.0F : 0.0F
        );
        this.detailScrollBar.setValue(0);
    }

    private float detailProgress() {
        return this.detailAnimations.progress(DETAIL_ANIMATION_KEY, this.detailsExpanded);
    }

    private boolean isDetailsVisible() {
        return this.isDetailArrowVisible() && this.detailProgress() > 0.001F;
    }

    private boolean isMouseOverDetails(int mouseX, int mouseY) {
        return mouseX >= DETAIL_MARGIN
                && mouseX < this.field_22789 - DETAIL_MARGIN
                && mouseY >= DETAIL_TOP
                && mouseY < this.detailVisibleBottom();
    }

    private boolean isDetailArrowVisible() {
        if (!Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue() || this.getListWidget() == null) {
            return false;
        }
        return this.getListWidget().getCurrentEntries().stream()
                .anyMatch(wrapper -> wrapper.getConfig() == Configs.ConfigForms.PREFERRED_WOOD_ENABLED);
    }

    private boolean isDetailArrowHovered(int mouseX, int mouseY) {
        int arrowX = this.detailArrowX();
        int rowTop = this.preferenceToggleRowTop();
        return mouseX >= arrowX
                && mouseX < arrowX + ARROW_SLOT_WIDTH
                && mouseY >= rowTop
                && mouseY < rowTop + 22;
    }

    private int detailArrowX() {
        int maxLabelWidth = this.getListWidget().getCurrentEntries().stream()
                .filter(wrapper -> wrapper.getConfig() != null)
                .mapToInt(wrapper -> this.field_22793.method_1727(wrapper.getConfig().getConfigGuiDisplayName()))
                .max()
                .orElse(0);
        String reset = StringUtils.translate("malilib.gui.button.reset.caps");
        int resetWidth = this.field_22793.method_1727(reset) + 10;
        int rowX = 12;
        int valueX = rowX + maxLabelWidth + 10;
        return valueX + CONFIG_WIDTH + 2 + resetWidth + 2;
    }

    private int preferenceToggleRowTop() {
        return 77;
    }

    private int detailFullBottom() {
        return Math.max(DETAIL_TOP, this.field_22790 - DETAIL_BOTTOM_MARGIN);
    }

    private int detailVisibleBottom() {
        int fullHeight = this.detailFullBottom() - DETAIL_TOP;
        return DETAIL_TOP + Math.round(fullHeight * this.detailProgress());
    }

    private void updateDetailScrollRange(int viewportHeight) {
        this.detailScrollBar.setMaxValue(Math.max(0, this.detailContentHeight() - Math.max(1, viewportHeight)));
    }

    private int detailContentHeight() {
        return this.rows.isEmpty()
                ? DETAIL_ROW_HEIGHT
                : this.rows.size() * (DETAIL_ROW_HEIGHT + DETAIL_ROW_GAP) - DETAIL_ROW_GAP;
    }

    private void rebuildRowsIfNeeded() {
        boolean enabled = Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue();
        WoodFamily family = this.currentWoodFamily();
        if (this.rowsWoodEnabled != enabled || this.rowsWoodFamily != family) {
            this.rebuildRows();
        }
    }

    private void rebuildRows() {
        this.rows.clear();
        this.rowsWoodEnabled = Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue();
        this.rowsWoodFamily = this.currentWoodFamily();
        if (this.rowsWoodEnabled && this.schematic != null) {
            for (ReplacementRow row : PreferredSchematicReplacement.scan(this.schematic, this.rowsWoodFamily)) {
                this.rows.add(new RowState(row));
            }
        }
        this.detailScrollBar.setValue(0);
    }

    private WoodFamily currentWoodFamily() {
        return (WoodFamily) Configs.ConfigForms.PREFERRED_WOOD_FAMILY.getOptionListValue();
    }

    @Override
    public void method_25419() {
        if (!this.closingConfirmed) {
            this.restoreInitialPreferences();
        }
        this.field_22787.method_1507(this.materialListParent);
    }

    private void confirm() {
        this.getListWidget().applyPendingModifications();
        Configs.saveToFile();

        if (this.schematic == null || !Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue()) {
            this.closingConfirmed = true;
            this.field_22787.method_1507(this.materialListParent);
            return;
        }

        this.rebuildRowsIfNeeded();
        List<PreferredSchematicReplacement.ReplacementChoice> choices = this.rows.stream()
                .map(row -> row.row.choice(row.mode))
                .toList();

        Path source = this.placement.getSchematicFile();
        GuiPreferredSchematicSave saveGui = new GuiPreferredSchematicSave(
                this.schematic,
                choices,
                source,
                this.defaultSaveName(source)
        );
        this.closingConfirmed = true;
        GuiBase.openGui(saveGui.setParent(this));
    }

    private void cancel() {
        if (!this.closingConfirmed) {
            this.restoreInitialPreferences();
        }
        this.closingConfirmed = true;
        this.field_22787.method_1507(this.materialListParent);
    }

    private void restoreInitialPreferences() {
        Configs.ConfigForms.PREFERRED_WOOD_ENABLED.setBooleanValue(this.initialWoodEnabled);
        Configs.ConfigForms.PREFERRED_WOOD_FAMILY.setOptionListValue(this.initialWoodFamily);
        Configs.saveToFile();
    }

    private String defaultSaveName(Path source) {
        String name = source != null && source.getFileName() != null
                ? source.getFileName().toString()
                : this.schematic.getMetadata().getName();
        if (name.endsWith(LitematicaSchematic.FILE_EXTENSION)) {
            name = name.substring(0, name.length() - LitematicaSchematic.FILE_EXTENSION.length());
        }
        return name + "_preferred";
    }

    private static SchematicPlacement resolvePlacement(MaterialListBase materialList) {
        if (materialList instanceof ChunkMissingMaterialList list) {
            return list.placement();
        }
        if (materialList instanceof MaterialListPlacement && materialList instanceof MaterialListPlacementAccess access) {
            return access.lmlp$getPlacement();
        }
        return null;
    }

    private final class RowState {
        private final ReplacementRow row;
        private final ButtonGeneric button = new ButtonGeneric(0, 0, DETAIL_ACTION_WIDTH, 20, "");
        private ReplacementMode mode;

        private RowState(ReplacementRow row) {
            this.row = row;
            this.mode = row.exact() ? ReplacementMode.REPLACE : ReplacementMode.SKIP;
            this.button.setTextCentered(true);
            this.button.setActionListener((button, mouseButton) -> this.cycle());
            this.updateButton();
        }

        private void cycle() {
            if (this.row.targetBlock() == null) {
                this.mode = ReplacementMode.SKIP;
            } else if (this.row.exact()) {
                this.mode = this.mode == ReplacementMode.REPLACE
                        ? ReplacementMode.SKIP
                        : ReplacementMode.REPLACE;
            } else {
                this.mode = this.mode == ReplacementMode.FORCE
                        ? ReplacementMode.SKIP
                        : ReplacementMode.FORCE;
            }
            this.updateButton();
        }

        private void updateButton() {
            String key = switch (this.mode) {
                case REPLACE -> "lmlp.gui.preferred_replacement.mode.replace";
                case SKIP -> "lmlp.gui.preferred_replacement.mode.skip";
                case FORCE -> "lmlp.gui.preferred_replacement.mode.force";
            };
            this.button.setDisplayString(StringUtils.translate(key));
        }
    }
}
