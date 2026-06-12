package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.util.LayerRange;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class IgnoredMaterialRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("LMLP IgnoredMaterialRegistry");
    private static final Map<String, Map<ViewType, Set<String>>> IGNORED_BY_PLACEMENT = new LinkedHashMap<>();

    private IgnoredMaterialRegistry() {
    }

    public static boolean ignore(MaterialListBase materialList, MaterialListEntry entry) {
        String placementKey = placementKey(materialList);
        ViewType viewType = viewType(materialList);
        String materialKey = materialKey(materialList, entry);
        int beforeSize = scopedIgnoredSize(placementKey, viewType, materialList);
        if (placementKey.isEmpty() || materialKey.isEmpty()) {
            LOGGER.info("[material ignore] placementKey={} viewType={} materialKey={} beforeIgnoredSize={} afterIgnoredSize={}",
                    placementKey, viewType, materialKey, beforeSize, beforeSize);
            return false;
        }

        Set<String> ignored = IGNORED_BY_PLACEMENT
                .computeIfAbsent(placementKey, ignoredKey -> new EnumMap<>(ViewType.class))
                .computeIfAbsent(viewType, ignoredKey -> new HashSet<>());
        boolean changed = ignored.add(materialKey);
        LOGGER.info("[material ignore] placementKey={} viewType={} materialKey={} beforeIgnoredSize={} afterIgnoredSize={}",
                placementKey, viewType, materialKey, beforeSize, scopedIgnoredSize(placementKey, viewType, materialList));
        return changed;
    }

    public static boolean ignoreMinimal(MaterialListBase materialList, String subMaterialKey) {
        String placementKey = placementKey(materialList);
        int beforeSize = ignoredSize(placementKey, ViewType.MINIMAL_SUB_MATERIAL);
        if (placementKey.isEmpty() || subMaterialKey == null || subMaterialKey.isEmpty()) {
            LOGGER.info("[minimal ignore] placementKey={} subMaterialKey={} beforeIgnoredSize={} afterIgnoredSize={}",
                    placementKey, subMaterialKey, beforeSize, beforeSize);
            return false;
        }

        boolean changed = ignoredSetForWrite(placementKey, ViewType.MINIMAL_SUB_MATERIAL).add(subMaterialKey);
        LOGGER.info("[minimal ignore] placementKey={} subMaterialKey={} beforeIgnoredSize={} afterIgnoredSize={}",
                placementKey, subMaterialKey, beforeSize, ignoredSize(placementKey, ViewType.MINIMAL_SUB_MATERIAL));
        return changed;
    }

    public static void clearIgnored(MaterialListBase materialList) {
        String placementKey = placementKey(materialList);
        ViewType viewType = viewType(materialList);
        int beforeSize = scopedIgnoredSize(placementKey, viewType, materialList);
        clearIgnored(placementKey, viewType, layerPrefix(materialList));
        LOGGER.info("[material clear] placementKey={} viewType={} beforeIgnoredSize={} afterIgnoredSize={}",
                placementKey, viewType, beforeSize, scopedIgnoredSize(placementKey, viewType, materialList));
    }

    public static void clearMinimalIgnored(MaterialListBase materialList) {
        String placementKey = placementKey(materialList);
        int beforeSize = ignoredSize(placementKey, ViewType.MINIMAL_SUB_MATERIAL);
        clearIgnored(placementKey, ViewType.MINIMAL_SUB_MATERIAL);
        LOGGER.info("[minimal clear] placementKey={} beforeIgnoredSize={} afterIgnoredSize={}",
                placementKey, beforeSize, ignoredSize(placementKey, ViewType.MINIMAL_SUB_MATERIAL));
    }

    public static void clearPlacement(String placementKey) {
        int beforeSize = 0;
        Map<ViewType, Set<String>> byView = IGNORED_BY_PLACEMENT.get(placementKey);
        if (byView != null) {
            for (Set<String> ignored : byView.values()) {
                beforeSize += ignored.size();
            }
        }

        if (placementKey != null && !placementKey.isEmpty()) {
            IGNORED_BY_PLACEMENT.remove(placementKey);
        }

        LOGGER.info("[material clear placement] placementKey={} beforeIgnoredSize={} afterIgnoredSize=0",
                placementKey, beforeSize);
    }

    public static Collection<MaterialListEntry> filter(MaterialListBase materialList, Collection<MaterialListEntry> entries) {
        String placementKey = placementKey(materialList);
        ViewType viewType = viewType(materialList);
        Set<String> ignored = ignoredSet(placementKey, viewType);
        if (ignored == null || ignored.isEmpty()) {
            return entries;
        }

        List<MaterialListEntry> filtered = new ArrayList<>(entries.size());
        int filteredEntryCount = 0;
        for (MaterialListEntry entry : entries) {
            if (ignored.contains(materialKey(materialList, entry))) {
                filteredEntryCount++;
                continue;
            }
            filtered.add(entry);
        }

        LOGGER.info("[material ignore filter] placementKey={} viewType={} ignoredSize={} filteredEntryCount={}",
                placementKey, viewType, ignored.size(), filteredEntryCount);
        return filtered;
    }

    public static String materialKey(MaterialListEntry entry) {
        return entry == null ? "" : "item:" + ItemStackTexts.id(entry.getStack());
    }

    private static String materialKey(MaterialListBase materialList, MaterialListEntry entry) {
        String materialKey = materialKey(entry);
        if (materialKey.isEmpty() || viewType(materialList) != ViewType.RENDER_LAYER) {
            return materialKey;
        }

        return layerPrefix(materialList) + materialKey;
    }

    public static String placementKey(MaterialListBase materialList) {
        return ChunkMissingMaterialListCache.materialListContextKey(materialList, "ignored_material_registry");
    }

    public static int minimalIgnoredSize(MaterialListBase materialList) {
        return ignoredSize(placementKey(materialList), ViewType.MINIMAL_SUB_MATERIAL);
    }

    public static Set<String> minimalIgnoredKeys(MaterialListBase materialList) {
        Set<String> ignored = ignoredSet(placementKey(materialList), ViewType.MINIMAL_SUB_MATERIAL);
        return ignored == null || ignored.isEmpty() ? Set.of() : Set.copyOf(ignored);
    }

    private static ViewType viewType(MaterialListBase materialList) {
        return materialList != null && materialList.getMaterialListType() == BlockInfoListType.RENDER_LAYERS
                ? ViewType.RENDER_LAYER
                : ViewType.ALL;
    }

    private static int ignoredSize(String placementKey, ViewType viewType) {
        Set<String> ignored = ignoredSet(placementKey, viewType);
        return ignored == null ? 0 : ignored.size();
    }

    private static int scopedIgnoredSize(String placementKey, ViewType viewType, MaterialListBase materialList) {
        Set<String> ignored = ignoredSet(placementKey, viewType);
        if (ignored == null || ignored.isEmpty()) {
            return 0;
        }
        if (viewType != ViewType.RENDER_LAYER) {
            return ignored.size();
        }

        String prefix = layerPrefix(materialList);
        int count = 0;
        for (String key : ignored) {
            if (key.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    private static Set<String> ignoredSetForWrite(String placementKey, ViewType viewType) {
        return IGNORED_BY_PLACEMENT
                .computeIfAbsent(placementKey, ignoredKey -> new EnumMap<>(ViewType.class))
                .computeIfAbsent(viewType, ignoredKey -> new HashSet<>());
    }

    private static Set<String> ignoredSet(String placementKey, ViewType viewType) {
        if (placementKey == null || placementKey.isEmpty()) {
            return null;
        }

        Map<ViewType, Set<String>> byView = IGNORED_BY_PLACEMENT.get(placementKey);
        return byView == null ? null : byView.get(viewType);
    }

    private static void clearIgnored(String placementKey, ViewType viewType) {
        clearIgnored(placementKey, viewType, "");
    }

    private static void clearIgnored(String placementKey, ViewType viewType, String keyPrefix) {
        if (placementKey == null || placementKey.isEmpty()) {
            return;
        }

        Map<ViewType, Set<String>> byView = IGNORED_BY_PLACEMENT.get(placementKey);
        if (byView == null) {
            return;
        }

        if (keyPrefix == null || keyPrefix.isEmpty()) {
            byView.remove(viewType);
        } else {
            Set<String> ignored = byView.get(viewType);
            if (ignored != null) {
                ignored.removeIf(key -> key.startsWith(keyPrefix));
                if (ignored.isEmpty()) {
                    byView.remove(viewType);
                }
            }
        }
        if (byView.isEmpty()) {
            IGNORED_BY_PLACEMENT.remove(placementKey);
        }
    }

    private static String layerPrefix(MaterialListBase materialList) {
        if (viewType(materialList) != ViewType.RENDER_LAYER) {
            return "";
        }

        LayerRange range = DataManager.getRenderLayerRange();
        if (range == null) {
            return "layer:unknown|";
        }

        return "layer:"
                + range.getLayerMode().getStringValue()
                + ':'
                + range.getAxis().name()
                + ':'
                + range.getLayerSingle()
                + ':'
                + range.getLayerMin()
                + ':'
                + range.getLayerMax()
                + '|';
    }

    private enum ViewType {
        ALL,
        RENDER_LAYER,
        MINIMAL_SUB_MATERIAL
    }
}
