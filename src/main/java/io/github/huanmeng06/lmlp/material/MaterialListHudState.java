package io.github.huanmeng06.lmlp.material;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shares the "toggle info HUD" state per placement context instead of per
 * MaterialListBase instance. The mod's cache layer swaps in fresh list
 * instances on GUI open, page switches and read-mode changes; each fresh
 * instance carries its own MaterialListHudRenderer whose shouldRender flag
 * would otherwise reset to off. Any instance resolving to the same context
 * key reads and writes the same flag here.
 */
public final class MaterialListHudState {
    private static final Map<String, Boolean> ENABLED_BY_KEY = new HashMap<>();
    private static final Map<MaterialListBase, Boolean> ENABLED_BY_INSTANCE = new WeakHashMap<>();
    private static final Map<MaterialListBase, String> KEY_CACHE = new WeakHashMap<>();

    private MaterialListHudState() {
    }

    public static boolean isEnabled(MaterialListBase materialList) {
        if (materialList == null) {
            return false;
        }

        String key = keyFor(materialList);
        if (key.isEmpty()) {
            return Boolean.TRUE.equals(ENABLED_BY_INSTANCE.get(materialList));
        }

        return Boolean.TRUE.equals(ENABLED_BY_KEY.get(key));
    }

    public static void toggle(MaterialListBase materialList) {
        if (materialList == null) {
            return;
        }

        boolean enabled = !isEnabled(materialList);
        String key = keyFor(materialList);
        if (key.isEmpty()) {
            ENABLED_BY_INSTANCE.put(materialList, enabled);
        } else {
            ENABLED_BY_KEY.put(key, enabled);
        }
    }

    /**
     * Keeps InfoHud pointing at the current list instance's renderer so the
     * HUD keeps rendering (with fresh data) after the active list is swapped.
     */
    public static void syncCurrentList(MaterialListBase materialList) {
        if (materialList == null) {
            return;
        }

        MaterialListHudRenderer renderer = materialList.getHudRenderer();
        InfoHud.getInstance().removeInfoHudRenderersOfType(renderer.getClass(), true);
        if (isEnabled(materialList)) {
            InfoHud.getInstance().addInfoHudRenderer(renderer, true);
        }
    }

    private static String keyFor(MaterialListBase materialList) {
        String cached = KEY_CACHE.get(materialList);
        if (cached != null) {
            return cached;
        }

        String key = ChunkMissingMaterialListCache.materialListContextKey(materialList, "hud_state");
        if (key == null) {
            key = "";
        }

        KEY_CACHE.put(materialList, key);
        return key;
    }
}
