package io.github.huanmeng06.lmlp.material;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single global "toggle info HUD" switch. Litematica stores the flag on each
 * MaterialListBase instance's renderer, but this mod's cache layer swaps
 * list instances freely (GUI open, page switches, read-mode changes,
 * selected-context resolution), and the placement-context keys those
 * instances resolve to can drift between resolutions. A single global flag
 * matches the user-facing model — the HUD stays on until toggled off — and
 * removes instance/key identity from the equation entirely.
 */
public final class MaterialListHudState {
    private static final Logger LOGGER = LoggerFactory.getLogger(LitematicaMaterialListPlus.MOD_ID);

    private static boolean enabled;

    private MaterialListHudState() {
    }

    public static boolean isEnabled(MaterialListBase materialList) {
        return enabled;
    }

    public static void toggle(MaterialListBase materialList) {
        enabled = !enabled;
        LOGGER.info("[lmlp hud] toggled enabled={} listClass={}",
                enabled, materialList == null ? "null" : materialList.getClass().getSimpleName());
    }

    /**
     * Keeps InfoHud pointing at the current list instance's renderer so the
     * HUD keeps rendering (with fresh data) after the active list is swapped.
     * Uses disableIfEmpty=false so re-syncing never flips InfoHud's global
     * enabled flag off in the window between remove and add.
     */
    public static void syncCurrentList(MaterialListBase materialList) {
        if (materialList == null) {
            return;
        }

        MaterialListHudRenderer renderer = materialList.getHudRenderer();
        InfoHud.getInstance().removeInfoHudRenderersOfType(renderer.getClass(), false);
        if (enabled) {
            InfoHud.getInstance().addInfoHudRenderer(renderer, true);
        }
    }

    /**
     * Re-applies the HUD state after a scheduled scan task wrote its results
     * back: the task ran against the list instance that was current at
     * schedule time, which may have been replaced since.
     */
    public static void resyncAfterScan(MaterialListBase taskList) {
        MaterialListBase current = DataManager.getMaterialList();
        syncCurrentList(current != null ? current : taskList);
    }
}
