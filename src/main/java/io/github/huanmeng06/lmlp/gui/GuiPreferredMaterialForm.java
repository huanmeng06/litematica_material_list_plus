package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
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
import io.github.huanmeng06.lmlp.config.GlazedTerracottaMaterial;
import io.github.huanmeng06.lmlp.config.StoneMaterialFamily;
import io.github.huanmeng06.lmlp.config.TerracottaMaterial;
import io.github.huanmeng06.lmlp.config.WoodFamily;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.PreferredMaterialCategory;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementMode;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementRow;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.Targets;
import io.github.huanmeng06.lmlp.gui.textlist.GuiItemIdStringListEdit;
import net.minecraft.class_1799;
import net.minecraft.class_2248;
import net.minecraft.class_2960;
import net.minecraft.class_7923;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final int DETAIL_MARGIN = 10;
    private static final int DETAIL_ROW_HEIGHT = 22;
    private static final int DETAIL_ROW_GAP = 0;
    private static final int DETAIL_ACTION_WIDTH = 92;
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
    private final ExpandAnimationTracker detailAnimations = new ExpandAnimationTracker();
    private final List<RowState> rows = new ArrayList<>();
    private final EnumMap<PreferredMaterialCategory, Boolean> detailsExpanded =
            new EnumMap<>(PreferredMaterialCategory.class);
    private final Map<PreferredMaterialCategory, ArrowBounds> arrowBounds =
            new EnumMap<>(PreferredMaterialCategory.class);
    private final Set<RowState> renderedRows = Collections.newSetFromMap(new IdentityHashMap<>());
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
        if (mouseButton == 0 && this.clickDetailTargetPicker(mouseX, mouseY)) {
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
        if (this.detailAnimations.isActive() && this.getListWidget() != null) {
            this.getListWidget().refreshEntries();
        }
        this.arrowBounds.clear();
        this.renderedRows.clear();
        super.drawContents(context, mouseX, mouseY, partialTicks);
        this.detailAnimations.prune();
    }

    int inlineDetailHeight(fi.dy.masa.malilib.config.IConfigBase config) {
        PreferredMaterialCategory category = this.categoryForTarget(config);
        if (category == null || !PreferenceSnapshot.current().enabled(category)) {
            return 0;
        }
        this.rebuildRowsIfNeeded();
        List<RowState> categoryRows = this.rowsFor(category);
        int fullHeight = categoryRows.isEmpty()
                ? DETAIL_ROW_HEIGHT
                : categoryRows.size() * (DETAIL_ROW_HEIGHT + DETAIL_ROW_GAP) - DETAIL_ROW_GAP;
        return Math.round(fullHeight * this.detailProgress(category));
    }

    void renderInlinePreferenceContent(
            fi.dy.masa.malilib.config.IConfigBase config,
            GuiContext context,
            int x,
            int y,
            int width,
            int configVisibleHeight,
            int mouseX,
            int mouseY) {
        PreferredMaterialCategory toggleCategory = this.categoryForToggle(config);
        if (toggleCategory != null && PreferenceSnapshot.current().enabled(toggleCategory)) {
            int arrowX = this.detailArrowX();
            ArrowBounds bounds = new ArrowBounds(arrowX, y, ARROW_SLOT_WIDTH, DETAIL_ROW_HEIGHT);
            this.arrowBounds.put(toggleCategory, bounds);
            ToggleArrowRenderer.render(
                    context,
                    arrowX,
                    ARROW_SLOT_WIDTH,
                    y + 11,
                    this.detailProgress(toggleCategory),
                    bounds.contains(mouseX, mouseY));
            return;
        }

        PreferredMaterialCategory category = this.categoryForTarget(config);
        int visibleHeight = this.inlineDetailHeight(config);
        if (category == null || visibleHeight <= 0) {
            return;
        }

        int detailTop = y + configVisibleHeight;
        int visibleBottom = detailTop + visibleHeight;
        context.method_44379(DETAIL_MARGIN, detailTop, this.field_22789 - DETAIL_MARGIN, visibleBottom);
        List<RowState> categoryRows = this.rowsFor(category);
        if (categoryRows.isEmpty()) {
            this.renderCenteredDetailMessage(
                    context,
                    StringUtils.translate("lmlp.gui.preferred_replacement.none"),
                    detailTop + 7);
        } else {
            int rowY = detailTop;
            for (int index = 0; index < categoryRows.size(); index++) {
                this.renderDetailRow(context, categoryRows.get(index), index, rowY, mouseX, mouseY);
                rowY += DETAIL_ROW_HEIGHT + DETAIL_ROW_GAP;
            }
        }
        context.method_44380();
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
        String targetName = row.targetBlock() == null
                ? StringUtils.translate("lmlp.gui.preferred_replacement.choose_alternative")
                : row.targetName();
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
        boolean targetHovered = !row.row.allowedTargetIds().isEmpty()
                && mouseX >= targetIconX - 2
                && mouseX < mappingRight
                && mouseY >= y
                && mouseY < y + DETAIL_ROW_HEIGHT;
        row.targetBounds = new ArrowBounds(
                targetIconX - 2,
                y,
                Math.max(18, mappingRight - targetIconX + 2),
                DETAIL_ROW_HEIGHT);
        if (row.targetBlock() != null) {
            RenderUtils.drawRect(
                    context,
                    targetIconX,
                    iconY,
                    16,
                    16,
                    targetHovered ? 0x60FFFF88 : DETAIL_ICON_BACKGROUND);
            context.method_51427(new class_1799(row.targetBlock()), targetIconX, iconY);
        }
        context.method_51433(
                this.field_22793,
                targetName,
                targetNameX,
                y + 7,
                targetHovered ? 0xFFFFFF88 : 0xFFFFFFFF,
                false);

        String count = StringUtils.translate("lmlp.gui.preferred_replacement.count", row.row.count());
        context.method_51433(this.field_22793, count, countX, y + 7, 0xFFFFFFFF, false);

        String status = StringUtils.translate(row.statusKey());
        context.method_51433(
                this.field_22793,
                status,
                statusX,
                y + 7,
                row.statusColor(),
                false
        );

        row.button.setPosition(buttonX, y + 1);
        row.button.render(context, mouseX, mouseY, row.button.isMouseOver());
        this.renderedRows.add(row);
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
        int contentWidth = this.rows.stream()
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
        for (RowState row : this.renderedRows) {
            if (this.detailsExpanded.getOrDefault(row.row.category(), false)
                    && row.button.onMouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return false;
    }

    private boolean clickDetailTargetPicker(int mouseX, int mouseY) {
        for (RowState row : this.renderedRows) {
            if (row.row.category() == PreferredMaterialCategory.STONE
                    && row.targetBounds != null
                    && row.targetBounds.contains(mouseX, mouseY)
                    && !row.row.allowedTargetIds().isEmpty()) {
                GuiItemIdStringListEdit picker = GuiItemIdStringListEdit.createRestrictedPicker(
                        row.row.allowedTargetIds(),
                        row::selectTarget,
                        this);
                GuiBase.openGui(picker);
                return true;
            }
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
    }

    private float detailProgress(PreferredMaterialCategory category) {
        return this.detailAnimations.progress(
                this.detailAnimationKey(category),
                this.detailsExpanded.getOrDefault(category, false));
    }

    private boolean isDetailArrowVisible(PreferredMaterialCategory category) {
        return PreferenceSnapshot.current().enabled(category) && this.arrowBounds.containsKey(category);
    }

    private boolean isDetailArrowHovered(PreferredMaterialCategory category, int mouseX, int mouseY) {
        ArrowBounds bounds = this.arrowBounds.get(category);
        return bounds != null && bounds.contains(mouseX, mouseY);
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
    }

    private List<RowState> rowsFor(PreferredMaterialCategory category) {
        return this.rows.stream()
                .filter(row -> row.row.category() == category)
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
                .map(RowState::choice)
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
            case STONE -> Configs.ConfigForms.PREFERRED_STONE_ENABLED;
            case GLASS -> Configs.ConfigForms.PREFERRED_GLASS_ENABLED;
            case CARPET -> Configs.ConfigForms.PREFERRED_CARPET_ENABLED;
            case TERRACOTTA -> Configs.ConfigForms.PREFERRED_TERRACOTTA_ENABLED;
            case GLAZED_TERRACOTTA -> Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_ENABLED;
        };
    }

    private PreferredMaterialCategory categoryForToggle(fi.dy.masa.malilib.config.IConfigBase config) {
        for (PreferredMaterialCategory category : PreferredMaterialCategory.values()) {
            if (config == this.preferenceToggle(category)) {
                return category;
            }
        }
        return null;
    }

    private PreferredMaterialCategory categoryForTarget(fi.dy.masa.malilib.config.IConfigBase config) {
        if (config == Configs.ConfigForms.PREFERRED_WOOD_FAMILY) {
            return PreferredMaterialCategory.WOOD;
        }
        if (config == Configs.ConfigForms.PREFERRED_STONE_FAMILY) {
            return PreferredMaterialCategory.STONE;
        }
        if (config == Configs.ConfigForms.PREFERRED_GLASS_MATERIAL) {
            return PreferredMaterialCategory.GLASS;
        }
        if (config == Configs.ConfigForms.PREFERRED_CARPET_MATERIAL) {
            return PreferredMaterialCategory.CARPET;
        }
        if (config == Configs.ConfigForms.PREFERRED_TERRACOTTA_MATERIAL) {
            return PreferredMaterialCategory.TERRACOTTA;
        }
        if (config == Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_MATERIAL) {
            return PreferredMaterialCategory.GLAZED_TERRACOTTA;
        }
        return null;
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
            boolean stoneEnabled,
            StoneMaterialFamily stone,
            boolean glassEnabled,
            GlassMaterial glass,
            boolean carpetEnabled,
            CarpetMaterial carpet,
            boolean terracottaEnabled,
            TerracottaMaterial terracotta,
            boolean glazedTerracottaEnabled,
            GlazedTerracottaMaterial glazedTerracotta) {

        private static PreferenceSnapshot current() {
            return new PreferenceSnapshot(
                    Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue(),
                    (WoodFamily) Configs.ConfigForms.PREFERRED_WOOD_FAMILY.getOptionListValue(),
                    Configs.ConfigForms.PREFERRED_STONE_ENABLED.getBooleanValue(),
                    (StoneMaterialFamily) Configs.ConfigForms.PREFERRED_STONE_FAMILY.getOptionListValue(),
                    Configs.ConfigForms.PREFERRED_GLASS_ENABLED.getBooleanValue(),
                    (GlassMaterial) Configs.ConfigForms.PREFERRED_GLASS_MATERIAL.getOptionListValue(),
                    Configs.ConfigForms.PREFERRED_CARPET_ENABLED.getBooleanValue(),
                    (CarpetMaterial) Configs.ConfigForms.PREFERRED_CARPET_MATERIAL.getOptionListValue(),
                    Configs.ConfigForms.PREFERRED_TERRACOTTA_ENABLED.getBooleanValue(),
                    (TerracottaMaterial) Configs.ConfigForms.PREFERRED_TERRACOTTA_MATERIAL.getOptionListValue(),
                    Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_ENABLED.getBooleanValue(),
                    (GlazedTerracottaMaterial) Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_MATERIAL.getOptionListValue());
        }

        private boolean enabled(PreferredMaterialCategory category) {
            return switch (category) {
                case WOOD -> this.woodEnabled;
                case STONE -> this.stoneEnabled;
                case GLASS -> this.glassEnabled;
                case CARPET -> this.carpetEnabled;
                case TERRACOTTA -> this.terracottaEnabled;
                case GLAZED_TERRACOTTA -> this.glazedTerracottaEnabled;
            };
        }

        private boolean anyEnabled() {
            return this.woodEnabled
                    || this.stoneEnabled
                    || this.glassEnabled
                    || this.carpetEnabled
                    || this.terracottaEnabled
                    || this.glazedTerracottaEnabled;
        }

        private Targets targets() {
            return new Targets(
                    this.woodEnabled ? this.wood : null,
                    this.stoneEnabled ? this.stone : null,
                    this.glassEnabled ? this.glass : null,
                    this.carpetEnabled ? this.carpet : null,
                    this.terracottaEnabled ? this.terracotta : null,
                    this.glazedTerracottaEnabled ? this.glazedTerracotta : null);
        }

        private void restore() {
            Configs.ConfigForms.PREFERRED_WOOD_ENABLED.setBooleanValue(this.woodEnabled);
            Configs.ConfigForms.PREFERRED_WOOD_FAMILY.setOptionListValue(this.wood);
            Configs.ConfigForms.PREFERRED_STONE_ENABLED.setBooleanValue(this.stoneEnabled);
            Configs.ConfigForms.PREFERRED_STONE_FAMILY.setOptionListValue(this.stone);
            Configs.ConfigForms.PREFERRED_GLASS_ENABLED.setBooleanValue(this.glassEnabled);
            Configs.ConfigForms.PREFERRED_GLASS_MATERIAL.setOptionListValue(this.glass);
            Configs.ConfigForms.PREFERRED_CARPET_ENABLED.setBooleanValue(this.carpetEnabled);
            Configs.ConfigForms.PREFERRED_CARPET_MATERIAL.setOptionListValue(this.carpet);
            Configs.ConfigForms.PREFERRED_TERRACOTTA_ENABLED.setBooleanValue(this.terracottaEnabled);
            Configs.ConfigForms.PREFERRED_TERRACOTTA_MATERIAL.setOptionListValue(this.terracotta);
            Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_ENABLED.setBooleanValue(this.glazedTerracottaEnabled);
            Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_MATERIAL.setOptionListValue(this.glazedTerracotta);
        }
    }

    private record ArrowBounds(int x, int y, int width, int height) {
        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.x
                    && mouseX < this.x + this.width
                    && mouseY >= this.y
                    && mouseY < this.y + this.height;
        }
    }

    private final class RowState {
        private final ReplacementRow row;
        private final ButtonGeneric button = new ButtonGeneric(0, 0, DETAIL_ACTION_WIDTH, 20, "");
        private String targetId;
        private class_2248 targetBlock;
        private String targetName;
        private boolean targetExact;
        private boolean customTarget;
        private ArrowBounds targetBounds;
        private ReplacementMode mode;

        private RowState(ReplacementRow row) {
            this.row = row;
            this.targetId = row.targetId();
            this.targetBlock = row.targetBlock();
            this.targetName = row.targetName();
            this.targetExact = row.exact();
            this.mode = this.targetBlock != null && this.targetExact
                    ? ReplacementMode.REPLACE
                    : ReplacementMode.SKIP;
            this.button.setTextCentered(true);
            this.button.setActionListener((button, mouseButton) -> this.cycle());
            this.updateButton();
        }

        private class_2248 targetBlock() {
            return this.targetBlock;
        }

        private String targetName() {
            return this.targetName;
        }

        private void selectTarget(String itemId) {
            class_2248 selected = class_7923.field_41175
                    .method_17966(class_2960.method_60654(itemId))
                    .orElse(null);
            if (selected == null) {
                return;
            }
            this.targetId = itemId;
            this.targetBlock = selected;
            this.targetName = selected.method_9518().getString();
            this.targetExact = PreferredSchematicReplacement.isExactReplacement(this.row.sourceBlock(), selected);
            this.customTarget = true;
            this.mode = this.targetExact ? ReplacementMode.REPLACE : ReplacementMode.SKIP;
            this.updateButton();
        }

        private PreferredSchematicReplacement.ReplacementChoice choice() {
            return new PreferredSchematicReplacement.ReplacementChoice(
                    this.row.sourceId(),
                    this.targetId,
                    this.targetBlock,
                    this.mode);
        }

        private String statusKey() {
            if (this.targetBlock == null) {
                return "lmlp.gui.preferred_replacement.missing_shape";
            }
            if (!this.targetExact) {
                return "lmlp.gui.preferred_replacement.incompatible";
            }
            if (this.customTarget) {
                return "lmlp.gui.preferred_replacement.custom_alternative";
            }
            if (!this.row.roleExact()) {
                return "lmlp.gui.preferred_replacement.same_shape";
            }
            return "lmlp.gui.preferred_replacement.compatible";
        }

        private int statusColor() {
            return this.targetBlock != null && this.targetExact && this.row.roleExact() && !this.customTarget
                    ? 0xFF88DD88
                    : 0xFFFFAA55;
        }

        private void cycle() {
            if (this.targetBlock == null) {
                this.mode = ReplacementMode.SKIP;
            } else if (this.targetExact) {
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
