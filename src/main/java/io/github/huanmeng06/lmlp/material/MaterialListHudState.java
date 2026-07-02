package io.github.huanmeng06.lmlp.material;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;

/**
 * Keeps the "toggle info HUD" state alive when the active material list
 * instance is swapped for a new one (page switches and cache refreshes
 * create fresh MaterialListBase instances, each with its own renderer
 * whose shouldRender flag defaults to off).
 */
public final class MaterialListHudState {
    private MaterialListHudState() {
    }

    public static MaterialListBase transfer(MaterialListBase from, MaterialListBase to) {
        if (from == null || to == null || from == to) {
            return to;
        }

        MaterialListHudRenderer fromRenderer = from.getHudRenderer();
        if (!fromRenderer.getShouldRenderCustom()) {
            return to;
        }

        fromRenderer.toggleShouldRender();
        InfoHud.getInstance().removeInfoHudRenderersOfType(fromRenderer.getClass(), true);

        MaterialListHudRenderer toRenderer = to.getHudRenderer();
        if (!toRenderer.getShouldRenderCustom()) {
            toRenderer.toggleShouldRender();
        }

        InfoHud.getInstance().addInfoHudRenderer(toRenderer, true);
        return to;
    }
}
