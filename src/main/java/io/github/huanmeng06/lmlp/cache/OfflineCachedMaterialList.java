package io.github.huanmeng06.lmlp.cache;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.util.StringUtils;

import java.util.List;

public class OfflineCachedMaterialList extends MaterialListBase {
    private final String contextKey;
    private final String name;

    OfflineCachedMaterialList(String contextKey, String name) {
        this.contextKey = contextKey;
        this.name = name;
        this.reCreateMaterialList();
    }

    String contextKey() {
        return this.contextKey;
    }

    void applyCachedEntries(List<MaterialListEntry> entries, BlockInfoListType listType) {
        this.materialListType = listType;
        this.setMaterialListEntries(entries);
    }

    @Override
    public boolean supportsRenderLayers() {
        return false;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getTitle() {
        return StringUtils.translate("litematica.gui.title.material_list.placement", this.getName());
    }

    @Override
    public void reCreateMaterialList() {
        ChunkMissingMaterialListCache.refreshOfflineMaterialList(this.contextKey, this);
    }
}
