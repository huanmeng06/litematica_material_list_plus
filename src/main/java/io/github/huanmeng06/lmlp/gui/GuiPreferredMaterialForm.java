package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiScrollBar;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
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
    private static final int BACKGROUND = 0xE0101010;
    private static final int PANEL = 0xE0202020;
    private static final int BORDER = 0xFF777777;
    private static final int ROW_HEIGHT = 36;
    private static final int ROW_GAP = 4;
    private static final int MARGIN = 18;
    private static final int TOP = 62;
    private static final int BOTTOM = 42;
    private static final int ACTION_WIDTH = 92;
    private static final int WHEEL_PIXELS = 32;

    private final class_437 parent;
    private final SchematicPlacement placement;
    private final LitematicaSchematic schematic;
    private final GuiScrollBar scrollBar = new GuiScrollBar();
    private final ButtonGeneric targetButton = new ButtonGeneric(0, 0, 150, 20, "");
    private final ButtonGeneric confirmButton = new ButtonGeneric(0, 0, 90, 20, "");
    private final ButtonGeneric cancelButton = new ButtonGeneric(0, 0, 90, 20, "");
    private final List<RowState> rows = new ArrayList<>();
    private WoodFamily targetFamily;
    private boolean draggingScrollbar;
    private double scrollRemainder;

    private GuiPreferredMaterialForm(class_437 parent, SchematicPlacement placement) {
        super(class_2561.method_43471("lmlp.gui.preferred_replacement.title"));
        this.parent = parent;
        this.placement = placement;
        this.schematic = placement == null ? null : placement.getSchematic();
        this.targetFamily = (WoodFamily) Configs.ConfigForms.PREFERRED_WOOD_FAMILY.getOptionListValue();

        this.targetButton.setTextCentered(true);
        this.targetButton.setActionListener((button, mouseButton) -> this.cycleTarget(mouseButton == 0));
        this.confirmButton.setDisplayString(StringUtils.translate("lmlp.gui.preferred_replacement.confirm"));
        this.confirmButton.setTextCentered(true);
        this.confirmButton.setActionListener((button, mouseButton) -> this.confirm());
        this.cancelButton.setDisplayString(StringUtils.translate("lmlp.gui.preferred_replacement.cancel"));
        this.cancelButton.setTextCentered(true);
        this.cancelButton.setActionListener((button, mouseButton) -> this.method_25419());
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
                MARGIN, 12, 0xFFFFFFFF, false);
        context.method_51433(this.field_22793, StringUtils.translate("lmlp.gui.preferred_replacement.target"),
                MARGIN, 38, 0xFFCCCCCC, false);
        this.targetButton.render(context, mouseX, mouseY, this.targetButton.isMouseOver());

        int contentBottom = this.field_22790 - BOTTOM;
        context.method_44379(MARGIN, TOP, this.field_22789 - MARGIN, contentBottom);
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
        context.method_44380();

        if (this.scrollBar.getMaxValue() > 0) {
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
            if (this.targetButton.onMouseClicked(event, doubleClick)
                    || this.confirmButton.onMouseClicked(event, doubleClick)
                    || this.cancelButton.onMouseClicked(event, doubleClick)) {
                return true;
            }
            int y = TOP - this.scrollBar.getValue();
            int contentBottom = this.field_22790 - BOTTOM;
            for (RowState row : this.rows) {
                if (y + ROW_HEIGHT >= TOP && y < contentBottom
                        && row.button.onMouseClicked(event, doubleClick)) {
                    return true;
                }
                y += ROW_HEIGHT + ROW_GAP;
            }
        }
        if (mouseButton == 0 && this.scrollBar.wasMouseOver()) {
            this.scrollBar.setIsDragging(true);
            this.draggingScrollbar = true;
            return true;
        }
        return super.method_25402(event, doubleClick);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
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

    private void cycleTarget(boolean forward) {
        this.targetFamily = (WoodFamily) this.targetFamily.cycle(forward);
        this.rebuildRows();
        this.scrollBar.setValue(0);
    }

    private void rebuildRows() {
        this.rows.clear();
        if (this.schematic != null) {
            for (ReplacementRow row : PreferredSchematicReplacement.scan(this.schematic, this.targetFamily)) {
                this.rows.add(new RowState(row));
            }
        }
        this.updateTargetButton();
    }

    private void confirm() {
        Configs.ConfigForms.PREFERRED_WOOD_FAMILY.setOptionListValue(this.targetFamily);
        Configs.saveToFile();
        if (this.schematic == null) {
            this.method_25419();
            return;
        }

        List<PreferredSchematicReplacement.ReplacementChoice> choices = this.rows.stream()
                .map(row -> row.row.choice(row.mode))
                .toList();
        LitematicaSchematic copy = PreferredSchematicReplacement.createCopy(this.schematic, choices);
        Path source = this.placement.getSchematicFile();
        GuiPreferredSchematicSave saveGui = new GuiPreferredSchematicSave(copy, source);
        saveGui.setString(this.defaultSaveName(source));
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

    private void updateTargetButton() {
        this.targetButton.setDisplayString(this.targetFamily.getDisplayName());
    }

    private void updateButtonPositions() {
        this.targetButton.setPosition(MARGIN + 78, 31);
        int y = this.field_22790 - 30;
        this.cancelButton.setPosition(this.field_22789 - MARGIN - 90, y);
        this.confirmButton.setPosition(this.field_22789 - MARGIN - 184, y);
    }

    private void updateScrollRange() {
        int viewport = Math.max(1, this.field_22790 - BOTTOM - TOP);
        this.scrollBar.setMaxValue(Math.max(0, this.contentHeight() - viewport));
    }

    private int contentHeight() {
        return this.rows.isEmpty() ? ROW_HEIGHT : this.rows.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
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
