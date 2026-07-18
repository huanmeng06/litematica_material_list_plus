package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialList;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.WoodFamily;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementMode;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementRow;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
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

    private final class_437 materialListParent;
    private final SchematicPlacement placement;
    private final LitematicaSchematic schematic;
    private final boolean initialWoodEnabled;
    private final WoodFamily initialWoodFamily;
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
    }

    @Override
    public boolean onMouseClicked(class_11909 mouseClick, boolean doubleClick) {
        boolean preferredWoodWasEnabled = Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue();
        boolean handled = super.onMouseClicked(mouseClick, doubleClick);
        boolean preferredWoodIsEnabled = Configs.ConfigForms.PREFERRED_WOOD_ENABLED.getBooleanValue();

        if (preferredWoodWasEnabled != preferredWoodIsEnabled
                && this.getListWidget() instanceof PreferenceWidgetListConfigOptions preferenceList) {
            preferenceList.setGroupExpanded(Configs.ConfigForms.PREFERRED_WOOD_ENABLED, preferredWoodIsEnabled);
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

        WoodFamily targetFamily = (WoodFamily) Configs.ConfigForms.PREFERRED_WOOD_FAMILY.getOptionListValue();
        List<PreferredSchematicReplacement.ReplacementChoice> choices = PreferredSchematicReplacement
                .scan(this.schematic, targetFamily)
                .stream()
                .map(GuiPreferredMaterialForm::defaultChoice)
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

    private static PreferredSchematicReplacement.ReplacementChoice defaultChoice(ReplacementRow row) {
        ReplacementMode mode = row.exact() ? ReplacementMode.REPLACE : ReplacementMode.SKIP;
        return row.choice(mode);
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
}
