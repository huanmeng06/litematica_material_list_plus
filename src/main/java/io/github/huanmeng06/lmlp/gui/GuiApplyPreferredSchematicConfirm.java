package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.interfaces.IConfirmationListener;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicPlacementApplication;
import net.minecraft.class_437;

final class GuiApplyPreferredSchematicConfirm extends GuiConfirmAction {
    private static final int DIALOG_WIDTH = 360;

    GuiApplyPreferredSchematicConfirm(
            SchematicPlacement sourcePlacement,
            LitematicaSchematic savedSchematic,
            class_437 parent) {
        super(
                DIALOG_WIDTH,
                "lmlp.gui.title.apply_preferred_schematic",
                listener(sourcePlacement, savedSchematic),
                parent,
                "lmlp.gui.confirm.apply_preferred_schematic");
        this.messageLines.clear();
        this.messageLines.add(StringUtils.translate("lmlp.gui.confirm.apply_preferred_schematic.saved"));
        this.messageLines.add(StringUtils.translate("lmlp.gui.confirm.apply_preferred_schematic.question"));
        this.messageLines.add(StringUtils.translate("lmlp.gui.confirm.apply_preferred_schematic.effect"));
        this.setWidthAndHeight(DIALOG_WIDTH, this.getMessageHeight() + 50);
        this.centerOnScreen();
    }

    @Override
    protected int getButtonWidth() {
        int confirm = this.getStringWidth(StringUtils.translate("lmlp.gui.preferred_replacement.confirm")) + 16;
        int cancel = this.getStringWidth(StringUtils.translate("lmlp.gui.preferred_replacement.cancel")) + 16;
        return Math.max(confirm, cancel);
    }

    @Override
    protected void createButton(int x, int y, int width, ButtonType type) {
        String key = type == ButtonType.OK
                ? "lmlp.gui.preferred_replacement.confirm"
                : "lmlp.gui.preferred_replacement.cancel";
        String color = type == ButtonType.OK ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        ButtonGeneric button = new ButtonGeneric(
                x,
                y,
                width,
                20,
                color + StringUtils.translate(key) + GuiBase.TXT_RST);
        this.addButton(button, (clickedButton, mouseButton) -> {
            if (type == ButtonType.OK) {
                boolean applied = this.listener.onActionConfirmed();
                if (applied) {
                    // The preferred placement is now selected. Open a fresh material-list GUI so
                    // the old placement's list does not remain visible behind this confirmation.
                    MaterialListOpener.open();
                    return;
                }
            } else {
                this.listener.onActionCancelled();
            }

            GuiBase.openGui(this.getParent());
        });
    }

    private static IConfirmationListener listener(
            SchematicPlacement sourcePlacement,
            LitematicaSchematic savedSchematic) {
        return new IConfirmationListener() {
            @Override
            public boolean onActionConfirmed() {
                boolean applied = PreferredSchematicPlacementApplication.apply(sourcePlacement, savedSchematic);
                InfoUtils.showGuiOrInGameMessage(
                        applied ? MessageType.SUCCESS : MessageType.ERROR,
                        applied
                                ? "lmlp.message.preferred_schematic_applied"
                                : "lmlp.error.preferred_schematic_apply_failed");
                return applied;
            }

            @Override
            public boolean onActionCancelled() {
                return true;
            }
        };
    }
}
