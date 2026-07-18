package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonBoolean;
import fi.dy.masa.malilib.gui.button.ConfigButtonOptionList;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialList;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.WoodFamily;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementMode;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementRow;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GuiPreferredMaterialForm extends class_437 {
    private static final int BACKGROUND = 0xB0000000;
    private static final int PANEL = 0xE0202020;
    private static final int BORDER = 0xFF777777;
    private static final int ROW_HEIGHT = 36;
    private static final int ROW_GAP = 4;
    private static final int MARGIN = 10;
    private static final int GROUP_ROW_Y = 31;
    private static final int CONFIG_VALUE_WIDTH = 140;
    private static final int CONFIG_COLUMN_GAP = 10;
    private static final int CONFIG_CONTROL_GAP = 2;
    private static final int GROUP_ARROW_WIDTH = 20;
    private static final int TARGET_ROW_Y = 55;
    private static final int TOP = 81;
    private static final int BOTTOM = 42;
    private static final int ACTION_WIDTH = 92;
    private static final int WHEEL_PIXELS = 32;

    private final class_437 parent;
    private final SchematicPlacement placement;
    private final LitematicaSchematic schematic;
    private final GuiScrollBar scrollBar = new GuiScrollBar();
    private final ExpandAnimationTracker groupAnimations = new ExpandAnimationTracker();
    private final ConfigBoolean woodEnabledDraft = new ConfigBoolean("preferredWoodEnabledDraft", false);
    private final ConfigOptionList targetFamilyDraft = new ConfigOptionList("preferredWoodFamilyDraft", WoodFamily.OAK);
    private final ConfigButtonBoolean woodToggleButton;
    private final ConfigButtonOptionList targetButton;
    private final ButtonGeneric woodResetButton;
    private final ButtonGeneric targetResetButton;
    private final ButtonGeneric confirmButton = new ButtonGeneric(0, 0, 90, 20, "");
    private final ButtonGeneric cancelButton = new ButtonGeneric(0, 0, 90, 20, "");
    private final List<RowState> rows = new ArrayList<>();
    private WoodFamily targetFamily;
    private boolean woodEnabled;
    private boolean woodExpanded;
    private boolean draggingScrollbar;
    private double scrollRemainder;

    private GuiPreferredMaterialForm(class_437 parent, SchematicPlacement placement) {
        super(class_2561.method_43471("lmlp.gui.preferred_replacement.title"));
        this.parent = parent;
        this.placement = placement;
        this.schematic = placement == null ? null : placement.getSchematic();
        this.targetFamily = (WoodFamily) Configs.ConfigForms.PREFERRED_WOOD_FAMILY.getOptionListValue();
        this.woodEnabled = Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue();
        this.woodEnabledDraft.setBooleanValue(this.woodEnabled);
        this.targetFamilyDraft.setOptionListValue(this.targetFamily);

        this.woodToggleButton = new ConfigButtonBoolean(0, 0, CONFIG_VALUE_WIDTH, 20, this.woodEnabledDraft);
        this.woodToggleButton.setActionListener((button, mouseButton) -> this.syncWoodEnabledFromDraft());
        this.targetButton = new ConfigButtonOptionList(0, 0, CONFIG_VALUE_WIDTH, 20, this.targetFamilyDraft);
        this.targetButton.setActionListener((button, mouseButton) -> this.syncTargetFamilyFromDraft());

        String reset = StringUtils.translate("malilib.gui.button.reset.caps");
        this.woodResetButton = new ButtonGeneric(0, 0, -1, 20, reset);
        this.woodResetButton.setActionListener((button, mouseButton) -> this.resetWoodEnabled());
        this.targetResetButton = new ButtonGeneric(0, 0, -1, 20, reset);
        this.targetResetButton.setActionListener((button, mouseButton) -> this.resetTargetFamily());
        this.confirmButton.setDisplayString(StringUtils.translate("lmlp.gui.preferred_replacement.confirm"));
        this.confirmButton.setTextCentered(true);
        this.confirmButton.setActionListener((button, mouseButton) -> this.confirm());
        this.cancelButton.setDisplayString(StringUtils.translate("lmlp.gui.preferred_replacement.cancel"));
        this.cancelButton.setTextCentered(true);
        this.cancelButton.setActionListener((button, mouseButton) -> this.method_25419());
        this.updateResetButtons();
        this.rebuildRows();
    }

    public static GuiPreferredMaterialForm forMaterialList(class_437 parent, MaterialListBase materialList) {
        return new GuiPreferredMaterialForm(parent, resolvePlacement(materialList));
    }

    public static GuiPreferredMaterialForm forConfig(class_437 parent) {
        return new GuiPreferredMaterialForm(parent, null);
    }

    public boolean hasSchematic() {
        return this.schematic != null;
    }

    @Override
    public void method_25419() {
        this.field_22787.method_1507(this.parent);
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    @Override
    public void method_25394(class_332 drawContext, int mouseX, int mouseY, float delta) {
        GuiContext context = GuiContext.fromGuiGraphics(drawContext);
        RenderUtils.drawRect(context, 0, 0, this.field_22789, this.field_22790, BACKGROUND);
        this.updateButtonPositions();
        this.updateScrollRange();

        context.method_51433(this.field_22793, StringUtils.translate("lmlp.gui.preferred_replacement.title"),
                20, 10, 0xFFFFFFFF, false);
        context.method_51433(this.field_22793, StringUtils.translate("lmlp.gui.preferred_replacement.wood_group"),
                MARGIN, GROUP_ROW_Y + 7, 0xFFFFFFFF, false);
        this.woodToggleButton.render(context, mouseX, mouseY, this.woodToggleButton.isMouseOver());
        if (this.woodEnabled) {
            ToggleArrowRenderer.render(
                    context,
                    this.groupArrowX(),
                    GROUP_ARROW_WIDTH,
                    GROUP_ROW_Y + 10,
                    this.groupAnimations.progress("preferred_wood", this.woodExpanded),
                    this.isGroupArrowHovered(mouseX, mouseY));
            this.groupAnimations.prune();
        }
        this.woodResetButton.render(context, mouseX, mouseY, this.woodResetButton.isMouseOver());

        if (this.woodEnabled && this.woodExpanded) {
            context.method_51433(this.field_22793, StringUtils.translate("lmlp.gui.preferred_replacement.target"),
                    MARGIN, TARGET_ROW_Y + 7, 0xFFFFFFFF, false);
            this.targetButton.render(context, mouseX, mouseY, this.targetButton.isMouseOver());
            this.targetResetButton.render(context, mouseX, mouseY, this.targetResetButton.isMouseOver());
        }

        int contentBottom = this.field_22790 - BOTTOM;
        context.method_44379(MARGIN, TOP, this.field_22789 - MARGIN, contentBottom);
        if (this.woodEnabled && this.woodExpanded) {
            if (this.schematic == null) {
                this.renderCenteredMessage(context, StringUtils.translate("lmlp.gui.preferred_replacement.config_only"), TOP + 24);
            } else if (this.rows.isEmpty()) {
                this.renderCenteredMessage(context, StringUtils.translate("lmlp.gui.preferred_replacement.none"), TOP + 24);
            } else {
                int y = TOP - this.scrollBar.getValue();
                for (RowState row : this.rows) {
                    if (y + ROW_HEIGHT >= TOP && y < contentBottom) {
                        this.renderRow(context, row, y, mouseX, mouseY);
                    }
                    y += ROW_HEIGHT + ROW_GAP;
                }
            }
        }
        context.method_44380();

        if (this.woodEnabled && this.woodExpanded && this.scrollBar.getMaxValue() > 0) {
            this.scrollBar.render(context, mouseX, mouseY, delta, this.field_22789 - 13, TOP, 8,
                    Math.max(1, contentBottom - TOP), this.contentHeight());
        }
        this.confirmButton.render(context, mouseX, mouseY, this.confirmButton.isMouseOver());
        this.cancelButton.render(context, mouseX, mouseY, this.cancelButton.isMouseOver());
    }

    @Override
    public boolean method_25402(net.minecraft.class_11909 event, boolean doubleClick) {
        int mouseButton = event.comp_4800().comp_4801();
        if (mouseButton == 0 || mouseButton == 1) {
            if (this.woodToggleButton.onMouseClicked(event, doubleClick)
                    || this.woodResetButton.onMouseClicked(event, doubleClick)
                    || this.confirmButton.onMouseClicked(event, doubleClick)
                    || this.cancelButton.onMouseClicked(event, doubleClick)) {
                return true;
            }
            int mouseX = (int) event.comp_4798();
            int mouseY = (int) event.comp_4799();
            if (this.woodEnabled && this.isGroupArrowHovered(mouseX, mouseY)) {
                this.toggleWoodExpanded();
                return true;
            }
            if (this.woodEnabled && this.woodExpanded
                    && (this.targetButton.onMouseClicked(event, doubleClick)
                    || this.targetResetButton.onMouseClicked(event, doubleClick))) {
                return true;
            }
            int y = TOP - this.scrollBar.getValue();
            int contentBottom = this.field_22790 - BOTTOM;
            if (this.woodEnabled && this.woodExpanded) {
                for (RowState row : this.rows) {
                    if (y + ROW_HEIGHT >= TOP && y < contentBottom
                            && row.button.onMouseClicked(event, doubleClick)) {
                        return true;
                    }
                    y += ROW_HEIGHT + ROW_GAP;
                }
            }
        }
        if (this.woodEnabled && this.woodExpanded && mouseButton == 0 && this.scrollBar.wasMouseOver()) {
            this.scrollBar.setIsDragging(true);
            this.draggingScrollbar = true;
            return true;
        }
        return super.method_25402(event, doubleClick);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.woodEnabled || !this.woodExpanded) {
            return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        double target = this.scrollRemainder - verticalAmount * WHEEL_PIXELS;
        int pixels = (int) target;
        this.scrollRemainder = target - pixels;
        this.scrollBar.offsetValue(pixels);
        return true;
    }

    @Override
    public boolean method_25403(net.minecraft.class_11909 event, double deltaX, double deltaY) {
        return this.draggingScrollbar || super.method_25403(event, deltaX, deltaY);
    }

    @Override
    public boolean method_25406(net.minecraft.class_11909 event) {
        this.draggingScrollbar = false;
        this.scrollBar.setIsDragging(false);
        return super.method_25406(event);
    }

    private void renderRow(GuiContext context, RowState row, int y, int mouseX, int mouseY) {
        RenderUtils.drawOutlinedBox(context, MARGIN, y, this.field_22789 - MARGIN * 2 - 12, ROW_HEIGHT, PANEL, BORDER);
        int iconY = y + 10;
        if (row.row.sourceBlock() != null) {
            context.method_51427(new class_1799(row.row.sourceBlock()), MARGIN + 8, iconY);
        }
        if (row.row.targetBlock() != null) {
            context.method_51427(new class_1799(row.row.targetBlock()), MARGIN + 34, iconY);
        }

        int textX = MARGIN + 58;
        String targetName = row.row.targetBlock() == null
                ? StringUtils.translate("lmlp.gui.preferred_replacement.no_target")
                : row.row.targetName();
        String mapping = row.row.sourceName() + "  →  " + targetName;
        context.method_51433(this.field_22793, mapping, textX, y + 7, 0xFFFFFFFF, false);
        String detail = StringUtils.translate("lmlp.gui.preferred_replacement.count", row.row.count())
                + "    " + StringUtils.translate(row.row.exact()
                ? "lmlp.gui.preferred_replacement.compatible"
                : "lmlp.gui.preferred_replacement.incompatible");
        context.method_51433(this.field_22793, detail, textX, y + 21,
                row.row.exact() ? 0xFF88DD88 : 0xFFFFAA55, false);

        row.button.setPosition(this.field_22789 - MARGIN - ACTION_WIDTH - 20, y + 8);
        row.button.render(context, mouseX, mouseY, row.button.isMouseOver());
    }

    private void renderCenteredMessage(GuiContext context, String message, int y) {
        int x = Math.max(MARGIN, (this.field_22789 - this.field_22793.method_1727(message)) / 2);
        context.method_51433(this.field_22793, message, x, y, 0xFFFFCC66, false);
    }

    private void rebuildRows() {
        this.rows.clear();
        if (this.woodEnabled && this.schematic != null) {
            for (ReplacementRow row : PreferredSchematicReplacement.scan(this.schematic, this.targetFamily)) {
                this.rows.add(new RowState(row));
            }
        }
    }

    private void confirm() {
        Configs.ConfigForms.PREFERRED_WOOD_ENABLED.setBooleanValue(this.woodEnabled);
        Configs.ConfigForms.PREFERRED_WOOD_FAMILY.setOptionListValue(this.targetFamily);
        Configs.saveToFile();
        if (this.schematic == null || !this.woodEnabled) {
            this.method_25419();
            return;
        }

        List<PreferredSchematicReplacement.ReplacementChoice> choices = this.rows.stream()
                .map(row -> row.row.choice(row.mode))
                .toList();
        Path source = this.placement.getSchematicFile();
        GuiPreferredSchematicSave saveGui = new GuiPreferredSchematicSave(
                this.schematic,
                choices,
                source,
                this.defaultSaveName(source));
        GuiBase.openGui(saveGui.setParent(this));
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

    private void updateButtonPositions() {
        int valueX = this.configValueX();
        this.woodToggleButton.setPosition(valueX, GROUP_ROW_Y);
        this.woodResetButton.setPosition(this.groupResetX(), GROUP_ROW_Y);
        this.targetButton.setPosition(valueX, TARGET_ROW_Y);
        this.targetResetButton.setPosition(valueX + CONFIG_VALUE_WIDTH + CONFIG_CONTROL_GAP, TARGET_ROW_Y);
        int y = this.field_22790 - 30;
        this.cancelButton.setPosition(this.field_22789 - MARGIN - 90, y);
        this.confirmButton.setPosition(this.field_22789 - MARGIN - 184, y);
    }

    private void updateScrollRange() {
        if (!this.woodEnabled || !this.woodExpanded) {
            this.scrollBar.setMaxValue(0);
            this.scrollBar.setValue(0);
            return;
        }
        int viewport = Math.max(1, this.field_22790 - BOTTOM - TOP);
        this.scrollBar.setMaxValue(Math.max(0, this.contentHeight() - viewport));
    }

    private int contentHeight() {
        return this.rows.isEmpty() ? ROW_HEIGHT : this.rows.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
    }

    private void syncWoodEnabledFromDraft() {
        this.woodEnabled = this.woodEnabledDraft.getBooleanValue();
        this.woodExpanded = false;
        this.groupAnimations.clear();
        this.scrollBar.setValue(0);
        this.updateResetButtons();
        this.rebuildRows();
    }

    private void resetWoodEnabled() {
        this.woodEnabledDraft.resetToDefault();
        this.woodToggleButton.updateDisplayString();
        this.syncWoodEnabledFromDraft();
    }

    private void syncTargetFamilyFromDraft() {
        this.targetFamily = (WoodFamily) this.targetFamilyDraft.getOptionListValue();
        this.scrollBar.setValue(0);
        this.updateResetButtons();
        this.rebuildRows();
    }

    private void resetTargetFamily() {
        this.targetFamilyDraft.resetToDefault();
        this.targetButton.updateDisplayString();
        this.syncTargetFamilyFromDraft();
    }

    private void updateResetButtons() {
        this.woodResetButton.setEnabled(this.woodEnabledDraft.isModified());
        this.targetResetButton.setEnabled(this.targetFamilyDraft.isModified());
    }

    private void toggleWoodExpanded() {
        float startProgress = this.groupAnimations.progress("preferred_wood", this.woodExpanded);
        this.woodExpanded = !this.woodExpanded;
        this.groupAnimations.start("preferred_wood", startProgress, this.woodExpanded ? 1.0F : 0.0F);
        this.scrollBar.setValue(0);
    }

    private int groupArrowX() {
        return this.configValueX() + CONFIG_VALUE_WIDTH + CONFIG_CONTROL_GAP;
    }

    private int groupResetX() {
        return this.groupArrowX() + GROUP_ARROW_WIDTH + CONFIG_CONTROL_GAP;
    }

    private int configValueX() {
        int groupWidth = this.field_22793.method_1727(StringUtils.translate("lmlp.gui.preferred_replacement.wood_group"));
        int targetWidth = this.field_22793.method_1727(StringUtils.translate("lmlp.gui.preferred_replacement.target"));
        return MARGIN + Math.max(groupWidth, targetWidth) + CONFIG_COLUMN_GAP;
    }

    private boolean isGroupArrowHovered(int mouseX, int mouseY) {
        int x = this.groupArrowX();
        return mouseX >= x && mouseX < x + GROUP_ARROW_WIDTH
                && mouseY >= GROUP_ROW_Y && mouseY < GROUP_ROW_Y + 20;
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
        private final ButtonGeneric button = new ButtonGeneric(0, 0, ACTION_WIDTH, 20, "");
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
                this.mode = this.mode == ReplacementMode.REPLACE ? ReplacementMode.SKIP : ReplacementMode.REPLACE;
            } else {
                this.mode = this.mode == ReplacementMode.FORCE ? ReplacementMode.SKIP : ReplacementMode.FORCE;
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
