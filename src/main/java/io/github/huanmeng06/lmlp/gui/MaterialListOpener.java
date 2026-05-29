package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.class_465;

public final class MaterialListOpener {
    private static GuiMaterialList handledScreenMaterialListGui;
    private static class_465<?> handledScreenParent;
    private static MaterialListBase handledScreenMaterialList;

    private MaterialListOpener() {
    }

    public static boolean open() {
        MaterialListBase materialList = getOrCreateMaterialList();

        if (materialList == null) {
            return true;
        }

        GuiBase.openGui(new GuiMaterialList(materialList));
        return true;
    }

    public static boolean openFromHandledScreen(class_465<?> parent) {
        MaterialListBase materialList = getOrCreateMaterialList();

        if (materialList == null) {
            return true;
        }

        GuiMaterialList gui = getHandledScreenGui(parent, materialList);
        HandledScreenMaterialListBridge.preserveOnce(parent);
        GuiBase.openGui(gui);
        return true;
    }

    public static boolean closeToHandledScreenParent(GuiBase gui) {
        if (!(gui.getParent() instanceof class_465<?> parent)) {
            return false;
        }

        GuiBase.openGui(parent);
        return true;
    }

    public static MaterialListBase getOrCreateMaterialList() {
        MaterialListBase materialList = DataManager.getMaterialList();

        if (materialList == null) {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
            if (placement == null) {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
                return null;
            }

            materialList = placement.getMaterialList();
            materialList.reCreateMaterialList();
        }

        return materialList;
    }

    private static GuiMaterialList getHandledScreenGui(class_465<?> parent, MaterialListBase materialList) {
        if (handledScreenMaterialListGui == null || handledScreenParent != parent || handledScreenMaterialList != materialList) {
            handledScreenMaterialListGui = new GuiMaterialList(materialList);
            handledScreenParent = parent;
            handledScreenMaterialList = materialList;
        }

        handledScreenMaterialListGui.setParent(parent);
        return handledScreenMaterialListGui;
    }
}
