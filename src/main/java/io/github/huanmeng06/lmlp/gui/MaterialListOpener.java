package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import io.github.huanmeng06.lmlp.cache.CachedMaterialList;
import io.github.huanmeng06.lmlp.cache.PlacementMaterialListCache;
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

        if (materialList instanceof CachedMaterialList cachedList) {
            if (cachedList.canRefreshLive()) {
                return PlacementMaterialListCache.refreshLive(cachedList.placement(), cachedList);
            }

            if (cachedList.matchesCurrentPlacementState()) {
                return cachedList;
            }

            materialList = null;
        }

        if (materialList instanceof MaterialListPlacement && materialList instanceof MaterialListPlacementAccess access) {
            SchematicPlacement placement = access.lmlp$getPlacement();
            if (placement != null && !PlacementMaterialListCache.arePlacementChunksLoaded(placement)) {
                return PlacementMaterialListCache.getCachedOrShowMissing(placement);
            }
        }

        if (materialList == null) {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
            if (placement == null) {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
                return null;
            }

            materialList = PlacementMaterialListCache.getOrCreate(placement);
        }

        return materialList;
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
