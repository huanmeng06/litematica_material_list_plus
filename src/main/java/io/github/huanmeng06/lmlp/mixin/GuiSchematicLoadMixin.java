package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.GuiSchematicLoad;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.gui.GuiPreferredMaterialForm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Mixin(value = GuiSchematicLoad.class, remap = false)
public abstract class GuiSchematicLoadMixin extends GuiSchematicBrowserBase {
    private static final int BUTTON_SPACING = 4;

    protected GuiSchematicLoadMixin() {
        super(12, 24);
    }

    @Inject(method = "createButtons", at = @At("TAIL"))
    private void lmlp$addPreferredReplacementButton(CallbackInfo ci) {
        WidgetSchematicBrowser browser = this.getListWidget();
        if (browser == null) {
            return;
        }

        String materialListHover = StringUtils.translate(
                "litematica.gui.button.hover.material_list_shift_to_select_sub_regions");
        List<ButtonBase> buttons = ((GuiBaseHoverAccess) (Object) this).lmlp$getButtons();
        ButtonBase materialListButton = buttons.stream()
                .filter(button -> button.getHoverStrings().contains(materialListHover))
                .findFirst()
                .orElse(null);
        if (materialListButton == null) {
            return;
        }

        int materialListX = materialListButton.getX();
        int y = materialListButton.getY();
        ButtonBase rightAlignedButton = buttons.get(buttons.size() - 1);
        String label = StringUtils.translate("lmlp.gui.button.material_list.preferred_replacement");
        int width = this.getStringWidth(label) + 10;
        int shift = width + BUTTON_SPACING;

        for (ButtonBase button : buttons) {
            if (button != rightAlignedButton
                    && button.getY() == y
                    && button.getX() >= materialListX) {
                button.setX(button.getX() + shift);
            }
        }

        ButtonGeneric button = new ButtonGeneric(
                materialListX,
                y,
                width,
                20,
                label
        );
        button.setHoverStrings("lmlp.gui.button.hover.material_list.preferred_replacement");
        this.addButton(button, (clickedButton, mouseButton) -> this.lmlp$openPreferredReplacement());
    }

    private void lmlp$openPreferredReplacement() {
        WidgetSchematicBrowser browser = this.getListWidget();
        DirectoryEntry entry = browser != null ? browser.getLastSelectedEntry() : null;
        if (entry == null) {
            this.addMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_selected");
            return;
        }

        Path source = entry.getFullPath();
        if (FileType.fromFile(source) != FileType.LITEMATICA_SCHEMATIC) {
            this.addMessage(
                    MessageType.ERROR,
                    "litematica.error.schematic_read_from_file_failed.cant_read",
                    source.getFileName());
            return;
        }
        if (!Files.isReadable(source)) {
            this.addMessage(
                    MessageType.ERROR,
                    "litematica.error.schematic_load.cant_read_file",
                    source.getFileName());
            return;
        }

        LitematicaSchematic schematic;
        try {
            schematic = LitematicaSchematic.createFromFile(
                    entry.getDirectory(),
                    entry.name());
        } catch (RuntimeException exception) {
            schematic = null;
        }
        if (schematic == null) {
            this.addMessage(
                    MessageType.ERROR,
                    "litematica.error.schematic_read_from_file_failed.cant_read",
                    source.getFileName());
            return;
        }

        GuiBase.openGui(GuiPreferredMaterialForm.forSchematicFile(
                (GuiSchematicLoad) (Object) this,
                schematic,
                source));
    }
}
