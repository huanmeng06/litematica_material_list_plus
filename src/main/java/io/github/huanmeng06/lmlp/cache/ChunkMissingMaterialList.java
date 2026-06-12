package io.github.huanmeng06.lmlp.cache;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.util.StringUtils;

public class ChunkMissingMaterialList extends MaterialListBase {
    private final SchematicPlacement placement;
    private final String signature;

    ChunkMissingMaterialList(SchematicPlacement placement, MaterialListBase optionsSource) {
        this.placement = placement;
        this.signature = ChunkMissingMaterialListCache.signature(placement);
        if (optionsSource != null) {
            this.fromJson(optionsSource.toJson());
        }
        this.reCreateMaterialList();
    }

    public SchematicPlacement placement() {
        return this.placement;
    }

    public boolean matchesCurrentPlacementState() {
        return this.signature.equals(ChunkMissingMaterialListCache.signature(this.placement));
    }

    @Override
    public boolean supportsRenderLayers() {
        return true;
    }

    @Override
    public String getName() {
        return this.placement.getName();
    }

    @Override
    public String getTitle() {
        return StringUtils.translate("litematica.gui.title.material_list.placement", this.getName());
    }

    @Override
    public void reCreateMaterialList() {
        ChunkMissingMaterialListCache.refresh(this);
    }
}
