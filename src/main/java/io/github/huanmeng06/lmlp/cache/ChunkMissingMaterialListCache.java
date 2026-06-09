package io.github.huanmeng06.lmlp.cache;

import com.google.gson.JsonObject;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.LayerRange;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.class_1923;
import net.minecraft.class_2338;
import net.minecraft.class_2382;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_638;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ChunkMissingMaterialListCache {
    private static final Map<SchematicPlacement, ChunkMissingMaterialList> LISTS = new IdentityHashMap<>();

    private ChunkMissingMaterialListCache() {
    }

    public static MaterialListBase getOrCreate(SchematicPlacement placement, MaterialListBase optionsSource) {
        ChunkMissingMaterialList list = LISTS.get(placement);
        if (list == null || !list.matchesCurrentPlacementState()) {
            list = new ChunkMissingMaterialList(placement, optionsSource);
            LISTS.put(placement, list);
        } else if (optionsSource != null) {
            JsonObject options = optionsSource.toJson();
            list.fromJson(options);
            list.reCreateMaterialList();
        } else {
            list.reCreateMaterialList();
        }

        DataManager.setMaterialList(list);
        return list;
    }

    public static void rememberIfPlacementList(MaterialListBase materialList) {
        if (materialList instanceof MaterialListPlacement && materialList instanceof MaterialListPlacementAccess access) {
            SchematicPlacement placement = access.lmlp$getPlacement();
            if (placement != null && !materialList.getMaterialsAll().isEmpty()) {
                LISTS.put(placement, new ChunkMissingMaterialList(placement, materialList));
            }
        }
    }

    static void refresh(ChunkMissingMaterialList list) {
        list.setMaterialListEntries(createEntries(list.placement(), list.getMaterialListType()));
    }

    public static boolean arePlacementChunksLoaded(SchematicPlacement placement) {
        class_638 world = class_310.method_1551().field_1687;
        if (world == null) {
            return false;
        }

        for (class_1923 chunk : placement.getTouchedChunks()) {
            if (!world.method_8393(chunk.field_9181, chunk.field_9180)) {
                return false;
            }
        }

        return true;
    }

    private static List<MaterialListEntry> createEntries(SchematicPlacement placement, BlockInfoListType type) {
        LitematicaSchematic schematic = placement.getSchematic();
        if (schematic == null) {
            return List.of();
        }

        Collection<String> enabledRegions = enabledRegionNames(placement);
        if (type == BlockInfoListType.ALL) {
            return MaterialListUtils.createMaterialListFor(schematic, enabledRegions);
        }

        Object2IntOpenHashMap<class_2680> counts = new Object2IntOpenHashMap<>();
        LayerRange layerRange = DataManager.getRenderLayerRange();
        for (String regionName : enabledRegions) {
            countRegionInLayer(placement, schematic, regionName, layerRange, counts);
        }

        return MaterialListUtils.getMaterialList(
                counts,
                counts,
                new Object2IntOpenHashMap<>(),
                class_310.method_1551().field_1724);
    }

    private static Collection<String> enabledRegionNames(SchematicPlacement placement) {
        List<String> names = new ArrayList<>();
        for (SubRegionPlacement region : placement.getAllSubRegionsPlacements()) {
            if (region.isEnabled()) {
                names.add(region.getName());
            }
        }
        names.sort(String::compareTo);
        return names;
    }

    private static void countRegionInLayer(
            SchematicPlacement placement,
            LitematicaSchematic schematic,
            String regionName,
            LayerRange layerRange,
            Object2IntOpenHashMap<class_2680> counts) {
        LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);
        SubRegionPlacement region = placement.getRelativeSubRegionPlacement(regionName);
        if (container == null || region == null) {
            return;
        }

        class_2382 size = container.getSize();
        class_2338 regionOrigin = PositionUtils.getTransformedBlockPos(region.getPos(), placement.getMirror(), placement.getRotation())
                .method_10081(placement.getOrigin());
        for (int y = 0; y < size.method_10264(); y++) {
            for (int z = 0; z < size.method_10260(); z++) {
                for (int x = 0; x < size.method_10263(); x++) {
                    class_2338 localPos = new class_2338(x, y, z);
                    class_2338 worldPos = PositionUtils.getTransformedPlacementPosition(localPos, placement, region)
                            .method_10081(regionOrigin);
                    if (layerRange.isPositionWithinRange(worldPos)) {
                        counts.addTo(container.get(x, y, z), 1);
                    }
                }
            }
        }
    }

    static String signature(SchematicPlacement placement) {
        StringBuilder builder = new StringBuilder();
        builder.append(schematicPath(placement)).append('|');
        builder.append(placement.getName()).append('|');
        appendPos(builder, placement.getOrigin());
        builder.append('|').append(placement.getRotation()).append('|').append(placement.getMirror()).append('|');
        builder.append(placement.isEnabled()).append('|').append(placement.ignoreEntities()).append('|');
        builder.append(regionsKey(placement.getAllSubRegionsPlacements()));
        return builder.toString();
    }

    private static String schematicPath(SchematicPlacement placement) {
        File file = placement.getSchematicFile();
        return file == null ? "" : file.getAbsolutePath();
    }

    private static String regionsKey(Collection<SubRegionPlacement> regions) {
        List<SubRegionPlacement> sorted = new ArrayList<>(regions);
        sorted.sort(Comparator.comparing(SubRegionPlacement::getName));
        StringBuilder builder = new StringBuilder();
        for (SubRegionPlacement region : sorted) {
            builder.append(region.getName()).append(':');
            builder.append(region.isEnabled()).append(':');
            builder.append(region.ignoreEntities()).append(':');
            appendPos(builder, region.getPos());
            builder.append(':').append(region.getRotation()).append(':').append(region.getMirror()).append(';');
        }
        return builder.toString();
    }

    private static void appendPos(StringBuilder builder, class_2338 pos) {
        builder.append(pos.method_10263()).append(',').append(pos.method_10264()).append(',').append(pos.method_10260());
    }
}
