package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.malilib.gui.GuiBase;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class MaterialListOpener {
    private static GuiMaterialList handledScreenMaterialListGui;
    private static AbstractContainerScreen<?> handledScreenParent;
    private static MaterialListBase handledScreenMaterialList;
    private static Screen handledScreenOverlay;

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
        return openContext(contextKey, reason, null);
    }

    public static boolean openContext(String contextKey, String reason, Screen parentGui) {
        MaterialListBase materialList = ChunkMissingMaterialListCache.getOrCreateMaterialListForExplicitContext(
                contextKey,
                DataManager.getMaterialList(),
                reason);

        if (materialList == null) {
            return true;
        }

        GuiMaterialList gui = new GuiMaterialList(materialList);
        if (parentGui != null) {
            gui.setParent(parentGui);
        }
        GuiBase.openGui(gui);
        return true;
    }

    public static boolean openFromHandledScreen(AbstractContainerScreen<?> parent) {
        MaterialListBase materialList = getOrCreateMaterialList();

        if (materialList == null) {
            return true;
        }

        GuiMaterialList gui = getHandledScreenGui(parent, materialList);
        Screen screen = handledScreenOverlay == null ? gui : handledScreenOverlay;
        HandledScreenMaterialListBridge.preserveOnce(parent);
        MaterialListHotkeyMatcher.suppressNextCharInput();
        GuiBase.openGui(screen);
        return true;
    }

    public static void rememberHandledScreenOverlay(Screen screen) {
        handledScreenOverlay = screen;
    }

    public static void forgetHandledScreenOverlay(Screen screen) {
        if (handledScreenOverlay == screen) {
            handledScreenOverlay = null;
        }
    }

    public static boolean closeToHandledScreenParent(GuiBase gui) {
        if (!(gui.getParent() instanceof AbstractContainerScreen<?> parent)) {
            return false;
        }

        GuiBase.openGui(parent);
        return true;
    }

    public static MaterialListBase getOrCreateMaterialList() {
        MaterialListBase materialList = DataManager.getMaterialList();
        return ChunkMissingMaterialListCache.getOrCreateMaterialListForOpen(materialList, "MaterialListOpener");
    }

    private static GuiMaterialList getHandledScreenGui(AbstractContainerScreen<?> parent, MaterialListBase materialList) {
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
