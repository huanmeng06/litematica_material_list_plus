package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.malilib.gui.GuiBase;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import net.minecraft.class_437;
import net.minecraft.class_465;

public final class MaterialListOpener {
    private static GuiMaterialList handledScreenMaterialListGui;
    private static class_465<?> handledScreenParent;
    private static MaterialListBase handledScreenMaterialList;
    private static class_437 handledScreenOverlay;

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

    public static boolean openContext(String contextKey, String reason) {
        ChunkMissingMaterialListCache.selectMaterialListContext(contextKey, reason);
        MaterialListBase materialList = ChunkMissingMaterialListCache.getOrCreateMaterialListForOpen(DataManager.getMaterialList(), reason);

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
        class_437 screen = handledScreenOverlay == null ? gui : handledScreenOverlay;
        HandledScreenMaterialListBridge.preserveOnce(parent);
        MaterialListHotkeyMatcher.suppressNextCharInput();
        GuiBase.openGui(screen);
        return true;
    }

    public static void rememberHandledScreenOverlay(class_437 screen) {
        handledScreenOverlay = screen;
    }

    public static void forgetHandledScreenOverlay(class_437 screen) {
        if (handledScreenOverlay == screen) {
            handledScreenOverlay = null;
        }
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
        return ChunkMissingMaterialListCache.getOrCreateMaterialListForOpen(materialList, "MaterialListOpener");
    }

    private static GuiMaterialList getHandledScreenGui(class_465<?> parent, MaterialListBase materialList) {
        if (handledScreenMaterialListGui == null || handledScreenParent != parent || handledScreenMaterialList != materialList) {
            handledScreenMaterialListGui = new GuiMaterialList(materialList);
            handledScreenParent = parent;
            handledScreenMaterialList = materialList;
            handledScreenOverlay = null;
        }

        handledScreenMaterialListGui.setParent(parent);
        return handledScreenMaterialListGui;
    }
}
