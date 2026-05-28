package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;

public final class MaterialListOpener {
    private MaterialListOpener() {
    }

    public static boolean open() {
        MaterialListBase materialList = DataManager.getMaterialList();

        if (materialList == null) {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
            if (placement == null) {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
                return true;
            }

            materialList = placement.getMaterialList();
            materialList.reCreateMaterialList();
        }

        GuiBase.openGui(new GuiMaterialList(materialList));
        return true;
    }
}
