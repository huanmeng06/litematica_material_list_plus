package io.github.huanmeng06.lmlp.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import io.github.huanmeng06.lmlp.access.SchematicPlacementAccess;
import net.minecraft.class_1923;
import net.minecraft.class_2338;
import net.minecraft.class_310;
import net.minecraft.class_638;

public final class PlacementMaterialListCache {
    private static final Map<SchematicPlacement, PlacementMaterialListSnapshot> SNAPSHOTS = new IdentityHashMap<>();

    private PlacementMaterialListCache() {
    }

    public static void rememberIfPlacementList(MaterialListBase materialList) {
        if (materialList instanceof MaterialListPlacement && materialList instanceof MaterialListPlacementAccess access) {
            SchematicPlacement placement = access.lmlp$getPlacement();
            if (placement != null && !materialList.getMaterialsAll().isEmpty()) {
                remember(placement, materialList);
            }
        }
    }

    public static void remember(SchematicPlacement placement, MaterialListBase materialList) {
        SNAPSHOTS.put(placement, PlacementMaterialListSnapshot.from(placement, materialList));
    }

    public static Optional<CachedMaterialList> cachedListFor(SchematicPlacement placement) {
        PlacementMaterialListSnapshot snapshot = SNAPSHOTS.get(placement);
        if (snapshot != null && !snapshot.matchesCurrentState()) {
            return Optional.empty();
        }
        return snapshot == null ? Optional.empty() : Optional.of(new CachedMaterialList(snapshot));
    }

    public static MaterialListBase getCachedOrShowMissing(SchematicPlacement placement) {
        Optional<CachedMaterialList> cached = cachedListFor(placement);
        if (cached.isPresent()) {
            CachedMaterialList cachedList = cached.get();
            DataManager.setMaterialList(cachedList);
            return cachedList;
        }

        InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "lmlp.message.material_list_cache.no_cache_unloaded");
        return null;
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

    public static MaterialListBase refreshLive(SchematicPlacement placement, MaterialListBase cachedList) {
        MaterialListBase liveList = getLiveList(placement);
        copyDisplayOptions(cachedList, liveList);
        DataManager.setMaterialList(liveList);
        liveList.reCreateMaterialList();
        return liveList;
    }

    public static MaterialListBase getOrCreate(SchematicPlacement placement) {
        if (arePlacementChunksLoaded(placement)) {
            MaterialListBase materialList = getLiveList(placement);
            DataManager.setMaterialList(materialList);
            materialList.reCreateMaterialList();
            return materialList;
        }

        return getCachedOrShowMissing(placement);
    }

    private static MaterialListBase getLiveList(SchematicPlacement placement) {
        if (placement instanceof SchematicPlacementAccess access) {
            MaterialListBase materialList = access.lmlp$getMaterialList();
            if (materialList == null) {
                materialList = new MaterialListPlacement(placement, false);
                access.lmlp$setMaterialList(materialList);
            }
            return materialList;
        }

        return placement.getMaterialList();
    }

    private static void copyDisplayOptions(MaterialListBase source, MaterialListBase target) {
        target.fromJson(source.toJson());
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

    static List<MaterialListEntry> copyEntries(List<MaterialListEntry> entries) {
        List<MaterialListEntry> copies = new ArrayList<>(entries.size());
        for (MaterialListEntry entry : entries) {
            copies.add(new MaterialListEntry(
                    entry.getStack().method_7972(),
                    entry.getCountTotal(),
                    entry.getCountMissing(),
                    entry.getCountMismatched(),
                    entry.getCountAvailable()));
        }
        return copies;
    }

    static JsonObject copyOptions(MaterialListBase materialList) {
        return materialList.toJson().deepCopy();
    }
}
