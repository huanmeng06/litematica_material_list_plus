package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.config.IConfigBoolean;
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
import io.github.huanmeng06.lmlp.config.CarpetMaterial;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.GlassMaterial;
import io.github.huanmeng06.lmlp.config.TerracottaMaterial;
import io.github.huanmeng06.lmlp.config.WoodFamily;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.PreferredMaterialCategory;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementMode;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementRow;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.Targets;
import net.minecraft.class_1799;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
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
    private static final String DETAIL_ANIMATION_KEY_PREFIX = "material_preferred_details_";

    private final class_437 materialListParent;
    private final SchematicPlacement placement;
    private final LitematicaSchematic schematic;
    private final PreferenceSnapshot initialPreferences;
    private final GuiScrollBar detailScrollBar = new GuiScrollBar();
    private final ExpandAnimationTracker detailAnimations = new ExpandAnimationTracker();
    private final List<RowState> rows = new ArrayList<>();
    private final EnumMap<PreferredMaterialCategory, Boolean> detailsExpanded =
            new EnumMap<>(PreferredMaterialCategory.class);
    private boolean draggingDetailScrollbar;
    private double detailScrollRemainder;
    private PreferenceSnapshot rowsSnapshot;
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
        this.initialPreferences = PreferenceSnapshot.current();
        this.rebuildRows();
        for (PreferredMaterialCategory category : PreferredMaterialCategory.values()) {
            boolean enabled = this.initialPreferences.enabled(category);
            this.detailsExpanded.put(category, enabled);
            if (enabled) {
                this.detailAnimations.start(this.detailAnimationKey(category), 0.0F, 1.0F);
            }
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
        PreferenceSnapshot before = PreferenceSnapshot.current();

        int mouseX = (int) mouseClick.comp_4798();
        int mouseY = (int) mouseClick.comp_4799();
        int mouseButton = mouseClick.comp_4800().comp_4801();
        if (mouseButton == 0 || mouseButton == 1) {
            for (PreferredMaterialCategory category : PreferredMaterialCategory.values()) {
                if (this.isDetailArrowVisible(category)
                        && this.isDetailArrowHovered(category, mouseX, mouseY)) {
                    this.toggleDetailsExpanded(category);
                    return true;
                }
            }
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
        PreferenceSnapshot after = PreferenceSnapshot.current();

        for (PreferredMaterialCategory category : PreferredMaterialCategory.values()) {
            boolean wasEnabled = before.enabled(category);
            boolean isEnabled = after.enabled(category);
            if (wasEnabled != isEnabled) {
                if (this.getListWidget() instanceof PreferenceWidgetListConfigOptions preferenceList) {
                    preferenceList.setGroupExpanded(this.preferenceToggle(category), isEnabled);
                }
                this.detailsExpanded.put(category, isEnabled);
                this.detailAnimations.start(
                        this.detailAnimationKey(category),
                        wasEnabled ? 1.0F : 0.0F,
                        isEnabled ? 1.0F : 0.0F);
            }
        }
        if (!before.equals(after)) {
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
        this.renderDetailArrows(context, mouseX, mouseY);
        this.renderReplacementDetails(context, mouseX, mouseY, partialTicks);
    }

    private void renderDetailArrows(GuiContext context, int mouseX, int mouseY) {
        int arrowX = this.detailArrowX();
        for (PreferredMaterialCategory category : PreferredMaterialCategory.values()) {
            if (this.isDetailArrowVisible(category)) {
                ToggleArrowRenderer.render(
                        context,
                        arrowX,
                        ARROW_SLOT_WIDTH,
                        this.preferenceToggleRowTop(category) + 11,
                        this.detailProgress(category),
                        this.isDetailArrowHovered(category, mouseX, mouseY)
                );
            }
        }
        this.detailAnimations.prune();
    }

    private void renderReplacementDetails(GuiContext context, int mouseX, int mouseY, float partialTicks) {
        float progress = this.detailProgress();
        if (progress <= 0.001F) {
            return;
        }

        this.rebuildRowsIfNeeded();
        List<RowState> visibleRows = this.visibleRows();
        int detailTop = this.detailTop();
        int fullBottom = this.detailFullBottom();
        int fullHeight = Math.max(0, fullBottom - detailTop);
        int visibleHeight = Math.round(fullHeight * progress);
        if (visibleHeight <= 0) {
            return;
        }

        int visibleBottom = detailTop + visibleHeight;
        this.updateDetailScrollRange(fullHeight, visibleRows);
        context.method_44379(DETAIL_MARGIN, detailTop, this.field_22789 - DETAIL_MARGIN, visibleBottom);

        if (visibleRows.isEmpty()) {
            this.renderCenteredDetailMessage(
                    context,
                    StringUtils.translate("lmlp.gui.preferred_replacement.none"),
                    detailTop + 24
            );
        } else {
            int y = detailTop - this.detailScrollBar.getValue();
            for (int index = 0; index < visibleRows.size(); index++) {
                RowState row = visibleRows.get(index);
                if (y + DETAIL_ROW_HEIGHT >= detailTop && y < visibleBottom) {
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
                    detailTop,
                    8,
                    visibleHeight,
                    this.detailContentHeight(visibleRows)
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
        int iconY = y + 3;
        if (row.row.sourceBlock() != null) {
            RenderUtils.drawRect(context, sourceIconX, iconY, 16, 16, DETAIL_ICON_BACKGROUND);
            context.method_51427(new class_1799(row.row.sourceBlock()), sourceIconX, iconY);
        }

        int buttonX = rowRight - DETAIL_ACTION_WIDTH - 4;
        int countX = rowLeft + Math.round((buttonX - rowLeft) * 0.57F);
        int statusX = rowLeft + Math.round((buttonX - rowLeft) * 0.78F);
        int sourceNameX = sourceIconX + 20;
        int mappingRight = countX - 8;
        int mappingWidth = Math.max(0, mappingRight - sourceNameX);
        int sourceNameColumnWidth = this.detailSourceNameColumnWidth(Math.round(mappingWidth * 0.46F));
        int arrowX = sourceNameX + sourceNameColumnWidth + 8;
        String arrow = "→";
        int targetIconX = arrowX + this.field_22793.method_1727(arrow) + 8;
        int targetNameX = targetIconX + 20;

        String sourceName = this.truncateDetailText(
                row.row.sourceName(),
                Math.max(0, arrowX - sourceNameX - 8)
        );
        String targetName = row.row.targetBlock() == null
                ? StringUtils.translate("lmlp.gui.preferred_replacement.no_target")
                : row.row.targetName();
        targetName = this.truncateDetailText(targetName, Math.max(0, mappingRight - targetNameX));

        context.method_51433(
                this.field_22793,
                sourceName,
                sourceNameX,
                y + 7,
                0xFFFFFFFF,
                false
        );
        context.method_51433(this.field_22793, arrow, arrowX, y + 7, 0xFFFFFFFF, false);
        if (row.row.targetBlock() != null) {
            RenderUtils.drawRect(context, targetIconX, iconY, 16, 16, DETAIL_ICON_BACKGROUND);
            context.method_51427(new class_1799(row.row.targetBlock()), targetIconX, iconY);
        }
        context.method_51433(this.field_22793, targetName, targetNameX, y + 7, 0xFFFFFFFF, false);

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
        if (maxWidth < ellipsisWidth) {
            return "";
        }
        int end = text.length();
        while (end > 0 && this.field_22793.method_1727(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private int detailSourceNameColumnWidth(int maxWidth) {
        int contentWidth = this.visibleRows().stream()
                .mapToInt(row -> this.field_22793.method_1727(row.row.sourceName()))
                .max()
                .orElse(0);
        return Math.max(0, Math.min(maxWidth, contentWidth));
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
        int detailTop = this.detailTop();
        if (mouseY < detailTop || mouseY >= visibleBottom) {
            return false;
        }
        int y = detailTop - this.detailScrollBar.getValue();
        for (RowState row : this.visibleRows()) {
            if (y + DETAIL_ROW_HEIGHT >= detailTop && y < visibleBottom
                    && row.button.onMouseClicked(event, doubleClick)) {
                return true;
            }
            y += DETAIL_ROW_HEIGHT + DETAIL_ROW_GAP;
        }
        return false;
    }

    private void toggleDetailsExpanded(PreferredMaterialCategory category) {
        float startProgress = this.detailProgress(category);
        boolean expanded = !this.detailsExpanded.getOrDefault(category, false);
        this.detailsExpanded.put(category, expanded);
        this.detailAnimations.start(
                this.detailAnimationKey(category),
                startProgress,
                expanded ? 1.0F : 0.0F
        );
        this.detailScrollBar.setValue(0);
    }

    private float detailProgress() {
        float progress = 0.0F;
        for (PreferredMaterialCategory category : PreferredMaterialCategory.values()) {
            if (this.isDetailArrowVisible(category)) {
                progress = Math.max(progress, this.detailProgress(category));
            }
        }
        return progress;
    }

    private float detailProgress(PreferredMaterialCategory category) {
        return this.detailAnimations.progress(
                this.detailAnimationKey(category),
                this.detailsExpanded.getOrDefault(category, false));
    }

    private boolean isDetailsVisible() {
        return this.detailProgress() > 0.001F;
    }

    private boolean isMouseOverDetails(int mouseX, int mouseY) {
        return mouseX >= DETAIL_MARGIN
                && mouseX < this.field_22789 - DETAIL_MARGIN
                && mouseY >= this.detailTop()
                && mouseY < this.detailVisibleBottom();
    }

    private boolean isDetailArrowVisible(PreferredMaterialCategory category) {
        if (!PreferenceSnapshot.current().enabled(category) || this.getListWidget() == null) {
            return false;
        }
        return this.getListWidget().getCurrentEntries().stream()
                .anyMatch(wrapper -> wrapper.getConfig() == this.preferenceToggle(category));
    }

    private boolean isDetailArrowHovered(PreferredMaterialCategory category, int mouseX, int mouseY) {
        int arrowX = this.detailArrowX();
        int rowTop = this.preferenceToggleRowTop(category);
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

    private int preferenceToggleRowTop(PreferredMaterialCategory category) {
        if (this.getListWidget() == null) {
            return 77;
        }
        List<ConfigOptionWrapper> entries = this.getListWidget().getCurrentEntries();
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).getConfig() == this.preferenceToggle(category)) {
                return 77 + index * 22;
            }
        }
        return 77;
    }

    private int detailTop() {
        int entryCount = this.getListWidget() == null ? 0 : this.getListWidget().getCurrentEntries().size();
        return Math.max(DETAIL_TOP, 77 + entryCount * 22 + 5);
    }

    private int detailFullBottom() {
        return Math.max(this.detailTop(), this.field_22790 - DETAIL_BOTTOM_MARGIN);
    }

    private int detailVisibleBottom() {
        int detailTop = this.detailTop();
        int fullHeight = this.detailFullBottom() - detailTop;
        return detailTop + Math.round(fullHeight * this.detailProgress());
    }

    private void updateDetailScrollRange(int viewportHeight, List<RowState> visibleRows) {
        this.detailScrollBar.setMaxValue(Math.max(0, this.detailContentHeight(visibleRows) - Math.max(1, viewportHeight)));
    }

    private int detailContentHeight(List<RowState> visibleRows) {
        return visibleRows.isEmpty()
                ? DETAIL_ROW_HEIGHT
                : visibleRows.size() * (DETAIL_ROW_HEIGHT + DETAIL_ROW_GAP) - DETAIL_ROW_GAP;
    }

    private void rebuildRowsIfNeeded() {
        if (!PreferenceSnapshot.current().equals(this.rowsSnapshot)) {
            this.rebuildRows();
        }
    }

    private void rebuildRows() {
        this.rows.clear();
        this.rowsSnapshot = PreferenceSnapshot.current();
        if (this.rowsSnapshot.anyEnabled() && this.schematic != null) {
            for (ReplacementRow row : PreferredSchematicReplacement.scan(this.schematic, this.rowsSnapshot.targets())) {
                this.rows.add(new RowState(row));
            }
        }
        this.detailScrollBar.setValue(0);
    }

    private List<RowState> visibleRows() {
        return this.rows.stream()
                .filter(row -> this.detailProgress(row.row.category()) > 0.001F)
                .toList();
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

        if (this.schematic == null || !PreferenceSnapshot.current().anyEnabled()) {
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
                this.defaultSaveName(source),
                this.placement,
                this.materialListParent
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
        this.initialPreferences.restore();
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

    private IConfigBoolean preferenceToggle(PreferredMaterialCategory category) {
        return switch (category) {
            case WOOD -> Configs.ConfigForms.PREFERRED_WOOD_ENABLED;
            case GLASS -> Configs.ConfigForms.PREFERRED_GLASS_ENABLED;
            case CARPET -> Configs.ConfigForms.PREFERRED_CARPET_ENABLED;
            case TERRACOTTA -> Configs.ConfigForms.PREFERRED_TERRACOTTA_ENABLED;
        };
    }

    private String detailAnimationKey(PreferredMaterialCategory category) {
        return DETAIL_ANIMATION_KEY_PREFIX + category.name().toLowerCase(java.util.Locale.ROOT);
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

    private record PreferenceSnapshot(
            boolean woodEnabled,
            WoodFamily wood,
            boolean glassEnabled,
            GlassMaterial glass,
            boolean carpetEnabled,
            CarpetMaterial carpet,
            boolean terracottaEnabled,
            TerracottaMaterial terracotta) {

        private static PreferenceSnapshot current() {
            return new PreferenceSnapshot(
                    Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue(),
                    (WoodFamily) Configs.ConfigForms.PREFERRED_WOOD_FAMILY.getOptionListValue(),
                    Configs.ConfigForms.PREFERRED_GLASS_ENABLED.getBooleanValue(),
                    (GlassMaterial) Configs.ConfigForms.PREFERRED_GLASS_MATERIAL.getOptionListValue(),
                    Configs.ConfigForms.PREFERRED_CARPET_ENABLED.getBooleanValue(),
                    (CarpetMaterial) Configs.ConfigForms.PREFERRED_CARPET_MATERIAL.getOptionListValue(),
                    Configs.ConfigForms.PREFERRED_TERRACOTTA_ENABLED.getBooleanValue(),
                    (TerracottaMaterial) Configs.ConfigForms.PREFERRED_TERRACOTTA_MATERIAL.getOptionListValue());
        }

        private boolean enabled(PreferredMaterialCategory category) {
            return switch (category) {
                case WOOD -> this.woodEnabled;
                case GLASS -> this.glassEnabled;
                case CARPET -> this.carpetEnabled;
                case TERRACOTTA -> this.terracottaEnabled;
            };
        }

        private boolean anyEnabled() {
            return this.woodEnabled || this.glassEnabled || this.carpetEnabled || this.terracottaEnabled;
        }

        private Targets targets() {
            return new Targets(
                    this.woodEnabled ? this.wood : null,
                    this.glassEnabled ? this.glass : null,
                    this.carpetEnabled ? this.carpet : null,
                    this.terracottaEnabled ? this.terracotta : null);
        }

        private void restore() {
            Configs.ConfigForms.PREFERRED_WOOD_ENABLED.setBooleanValue(this.woodEnabled);
            Configs.ConfigForms.PREFERRED_WOOD_FAMILY.setOptionListValue(this.wood);
            Configs.ConfigForms.PREFERRED_GLASS_ENABLED.setBooleanValue(this.glassEnabled);
            Configs.ConfigForms.PREFERRED_GLASS_MATERIAL.setOptionListValue(this.glass);
            Configs.ConfigForms.PREFERRED_CARPET_ENABLED.setBooleanValue(this.carpetEnabled);
            Configs.ConfigForms.PREFERRED_CARPET_MATERIAL.setOptionListValue(this.carpet);
            Configs.ConfigForms.PREFERRED_TERRACOTTA_ENABLED.setBooleanValue(this.terracottaEnabled);
            Configs.ConfigForms.PREFERRED_TERRACOTTA_MATERIAL.setOptionListValue(this.terracotta);
        }
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
