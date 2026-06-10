package io.github.huanmeng06.lmlp.cache;

import com.google.gson.JsonObject;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacement;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.LayerRange;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import io.github.huanmeng06.lmlp.access.MaterialListSourceAccess;
import io.github.huanmeng06.lmlp.access.SchematicPlacementMaterialListAccess;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.class_1923;
import net.minecraft.class_2338;
import net.minecraft.class_2382;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_638;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ChunkMissingMaterialListCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(LitematicaMaterialListPlus.MOD_ID);
    private static final Map<SchematicPlacement, ChunkMissingMaterialList> LISTS = new IdentityHashMap<>();
    private static final Map<SchematicPlacement, LiveScanState> LIVE_SCANS = new IdentityHashMap<>();
    private static final Map<SchematicPlacement, PlacementContext> PLACEMENT_CONTEXTS = new IdentityHashMap<>();
    private static final ThreadLocal<Boolean> APPLYING_SCHEMATIC_CACHE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<MaterialListDataSource> APPLYING_CACHE_SOURCE = ThreadLocal.withInitial(() -> MaterialListDataSource.UNKNOWN);
    private static PlacementContext lastKnownContext;

    private ChunkMissingMaterialListCache() {
    }

    public static MaterialListBase getOrCreate(SchematicPlacement placement, MaterialListBase optionsSource) {
        rememberPlacement(placement, "cache.get_or_create");

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
            if (placement != null) {
                PlacementContext context = rememberPlacement(placement, "material_list.entries");
                if (!materialList.getMaterialsAll().isEmpty()) {
                    if (context != null) {
                        context.markCacheGenerated();
                    }
                    LISTS.put(placement, new ChunkMissingMaterialList(placement, materialList));
                }
            }
        }
    }

    static void refresh(ChunkMissingMaterialList list) {
        setSchematicEntries(list, list.placement(), list.getMaterialListType());
    }

    public static void refreshPlacementList(SchematicPlacement placement, MaterialListBase materialList) {
        rememberPlacement(placement, "material_list.refresh_cache");
        setSchematicEntries(materialList, placement, materialList.getMaterialListType());
    }

    public static MaterialListBase refreshForPlacementState(SchematicPlacement placement, MaterialListBase materialList) {
        PlacementResolution resolution = PlacementResolution.direct(placement, "refreshForPlacementState");
        return refreshForPlacementState(resolution, materialList, false);
    }

    public static MaterialListBase getOrCreateMaterialListForOpen(MaterialListBase materialList, String caller) {
        PlacementResolution resolution = resolvePlacementForMaterialList(materialList, caller);
        if (!resolution.hasPlacement()) {
            logNoPlacement(caller, resolution);
            return null;
        }

        return refreshForPlacementState(resolution, materialList, false);
    }

    public static MaterialListBase refreshLastKnownPlacement(MaterialListBase materialList) {
        PlacementResolution resolution = resolvePlacementForMaterialList(materialList, "refreshLastKnownPlacement");
        if (!resolution.hasPlacement()) {
            return null;
        }

        return refreshForPlacementState(resolution, materialList, false);
    }

    public static boolean refreshForCurrentState(MaterialListBase materialList, boolean showLiveMessage) {
        SchematicPlacement placement = placementFor(materialList);
        if (placement == null) {
            return false;
        }

        PlacementResolution resolution = PlacementResolution.direct(placement, "refreshForCurrentState");
        refreshForPlacementState(resolution, materialList, showLiveMessage);
        return true;
    }

    public static boolean shouldUseSchematicCache(SchematicPlacement placement, MaterialListBase materialList) {
        rememberPlacement(placement, "shouldUseSchematicCache");
        return !arePlacementChunksLoaded(placement);
    }

    public static void rememberPlacementContext(SchematicPlacement placement, String reason) {
        rememberPlacement(placement, reason);
    }

    public static void rememberCurrentPlacements(String reason) {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        if (manager == null) {
            return;
        }

        for (SchematicPlacement placement : manager.getAllSchematicsPlacements()) {
            rememberPlacement(placement, reason + ".available");
        }
        rememberPlacement(manager.getSelectedSchematicPlacement(), reason + ".selected");
    }

    public static MaterialListDataSource applyingCacheSource() {
        return APPLYING_CACHE_SOURCE.get();
    }

    private static MaterialListBase refreshForPlacementState(PlacementResolution resolution, MaterialListBase materialList, boolean showLiveMessage) {
        SchematicPlacement placement = resolution.placement();
        PlacementContext context = rememberPlacement(placement, resolution.caller() + ".use");
        boolean useCache = shouldUseSchematicCache(placement, materialList);
        logRoute(resolution, context, useCache);

        if (useCache) {
            return getOrCreate(placement, materialList);
        }

        if (materialList == null || placementFor(materialList) != placement) {
            materialList = getOrCreatePlacementMaterialList(placement, materialList);
        }

        DataManager.setMaterialList(materialList);
        fillSchematicCacheIfEmpty(placement, materialList);
        scheduleLiveScan(placement, materialList, true, showLiveMessage);
        return materialList;
    }

    public static void markLiveScanCompleted(MaterialListBase materialList) {
        SchematicPlacement placement = placementFor(materialList);
        if (placement != null) {
            LIVE_SCANS.put(placement, new LiveScanState(liveScanSignature(placement, materialList), LiveScanStatus.COMPLETED));
        }
    }

    public static boolean isApplyingSchematicCache() {
        return APPLYING_SCHEMATIC_CACHE.get();
    }

    public static MaterialListDataSource dataSource(MaterialListBase materialList) {
        return materialList instanceof MaterialListSourceAccess access ? access.lmlp$getDataSource() : MaterialListDataSource.UNKNOWN;
    }

    public static boolean hasKnownDataSource(MaterialListBase materialList) {
        return dataSource(materialList) != MaterialListDataSource.UNKNOWN;
    }

    public static boolean isSchematicCacheSource(MaterialListBase materialList) {
        MaterialListDataSource dataSource = dataSource(materialList);
        return dataSource == MaterialListDataSource.SCHEMATIC_CACHE || dataSource == MaterialListDataSource.CROSS_DIMENSION_CACHE;
    }

    public static boolean isWorldScanSource(MaterialListBase materialList) {
        return dataSource(materialList) == MaterialListDataSource.WORLD_SCAN;
    }

    public static boolean isChunkMissingState(MaterialListBase materialList) {
        return isSchematicCacheSource(materialList);
    }

    public static boolean arePlacementChunksLoaded(SchematicPlacement placement) {
        class_638 world = class_310.method_1551().field_1687;
        if (world == null || !isPlacementInCurrentDimension(placement)) {
            return false;
        }

        Collection<class_1923> touchedChunks = placement.getTouchedChunks();
        if (touchedChunks.isEmpty()) {
            return false;
        }

        for (class_1923 chunk : touchedChunks) {
            if (!world.method_8393(chunk.field_9181, chunk.field_9180)) {
                return false;
            }
        }

        return true;
    }

    private static PlacementResolution resolvePlacementForMaterialList(MaterialListBase materialList, String caller) {
        SchematicPlacement materialListPlacement = placementFor(materialList);
        if (materialListPlacement != null) {
            rememberPlacement(materialListPlacement, caller + ".material_list");
        }

        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        SchematicPlacement selected = manager == null ? null : manager.getSelectedSchematicPlacement();
        List<SchematicPlacement> placements = manager == null ? List.of() : manager.getAllSchematicsPlacements();

        for (SchematicPlacement placement : placements) {
            rememberPlacement(placement, caller + ".available");
        }
        rememberPlacement(selected, caller + ".selected");

        ResolveSnapshot snapshot = new ResolveSnapshot(currentDimensionId(), selected != null, placements.size(), lastKnownContext);
        if (selected != null) {
            return PlacementResolution.resolved(caller, selected, PlacementSource.CURRENT_SELECTED, snapshot);
        }

        if (!placements.isEmpty()) {
            SchematicPlacement placement = placements.get(0);
            manager.setSelectedSchematicPlacement(placement);
            rememberPlacement(placement, caller + ".auto_selected_available");
            return PlacementResolution.resolved(caller, placement, PlacementSource.CURRENT_AVAILABLE, snapshot);
        }

        if (lastKnownContext != null && lastKnownContext.placement() != null) {
            return PlacementResolution.resolved(caller, lastKnownContext.placement(), PlacementSource.LAST_KNOWN, snapshot);
        }

        return PlacementResolution.missing(caller, snapshot);
    }

    private static PlacementContext rememberPlacement(SchematicPlacement placement, String reason) {
        if (placement == null) {
            return null;
        }

        boolean inCurrentManager = isPlacementInCurrentManager(placement);
        String currentDimension = currentDimensionId();
        PlacementContext context = PLACEMENT_CONTEXTS.get(placement);
        if (context == null) {
            context = new PlacementContext(placement, inCurrentManager ? currentDimension : null, reason);
            PLACEMENT_CONTEXTS.put(placement, context);
            LOGGER.info("[LMLP material-list] remember placement reason={} name={} dimension={} inCurrentManager={} schematic={} signature={}",
                    reason, context.name(), context.dimension(), inCurrentManager, context.schematicPath(), context.signature());
        } else {
            context.refresh(reason, inCurrentManager ? currentDimension : null);
        }

        lastKnownContext = context;
        return context;
    }

    private static void logRoute(PlacementResolution resolution, PlacementContext context, boolean useCache) {
        ResolveSnapshot snapshot = resolution.snapshot();
        LOGGER.info("[LMLP material-list] route caller={} currentDimension={} selectedExists={} placementCount={} lastKnownExists={} lastKnownDimension={} source={} placement={} placementDimension={} cacheGenerated={} result={}",
                resolution.caller(),
                snapshot.currentDimension(),
                snapshot.selectedExists(),
                snapshot.placementCount(),
                snapshot.lastKnownExists(),
                snapshot.lastKnownDimension(),
                resolution.source(),
                placementDebugName(resolution.placement()),
                context == null ? null : context.dimension(),
                context != null && context.cacheGenerated(),
                useCache ? cacheSourceFor(resolution.placement()) : MaterialListDataSource.WORLD_SCAN);
    }

    private static void logNoPlacement(String caller, PlacementResolution resolution) {
        ResolveSnapshot snapshot = resolution.snapshot();
        LOGGER.warn("[LMLP material-list] no_placement_selected path=LMLP caller={} currentDimension={} selectedExists={} placementCount={} lastKnownExists={} lastKnownDimension={}",
                caller,
                snapshot.currentDimension(),
                snapshot.selectedExists(),
                snapshot.placementCount(),
                snapshot.lastKnownExists(),
                snapshot.lastKnownDimension());
        InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
    }

    private static MaterialListDataSource cacheSourceFor(SchematicPlacement placement) {
        return isPlacementInCurrentDimension(placement) ? MaterialListDataSource.SCHEMATIC_CACHE : MaterialListDataSource.CROSS_DIMENSION_CACHE;
    }

    private static String placementDebugName(SchematicPlacement placement) {
        return placement == null ? "<none>" : placement.getName() + "@" + schematicPath(placement);
    }

    private static boolean isPlacementInCurrentDimension(SchematicPlacement placement) {
        if (placement == null || !isPlacementInCurrentManager(placement)) {
            return false;
        }

        PlacementContext context = PLACEMENT_CONTEXTS.get(placement);
        if (context == null || context.dimension() == null) {
            return false;
        }

        String currentDimension = currentDimensionId();
        if (currentDimension == null) {
            return false;
        }

        return currentDimension.equals(context.dimension());
    }

    private static boolean isPlacementInCurrentManager(SchematicPlacement placement) {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        return manager != null && manager.getAllSchematicsPlacements().contains(placement);
    }

    private static String currentDimensionId() {
        class_638 world = class_310.method_1551().field_1687;
        return world == null ? null : world.method_27983().toString();
    }

    private static List<MaterialListEntry> createEntries(SchematicPlacement placement, BlockInfoListType type) {
        LitematicaSchematic schematic = schematicFor(placement);
        if (schematic == null) {
            return List.of();
        }

        Collection<String> enabledRegions = enabledRegionNames(placement, schematic);
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

    private static void setSchematicEntries(MaterialListBase materialList, SchematicPlacement placement, BlockInfoListType type) {
        PlacementContext context = rememberPlacement(placement, "set_schematic_entries");
        MaterialListDataSource dataSource = cacheSourceFor(placement);
        if (context != null) {
            context.markCacheGenerated();
        }
        APPLYING_SCHEMATIC_CACHE.set(true);
        APPLYING_CACHE_SOURCE.set(dataSource);
        try {
            materialList.setMaterialListEntries(createEntries(placement, type));
        } finally {
            APPLYING_CACHE_SOURCE.set(MaterialListDataSource.UNKNOWN);
            APPLYING_SCHEMATIC_CACHE.set(false);
        }

        if (materialList instanceof MaterialListSourceAccess access) {
            access.lmlp$setDataSource(dataSource);
        }
        LIVE_SCANS.remove(placement);
    }

    private static void scheduleLiveScan(SchematicPlacement placement, MaterialListBase materialList, boolean manual, boolean showMessage) {
        String signature = liveScanSignature(placement, materialList);
        LiveScanState state = LIVE_SCANS.get(placement);
        if (state != null && state.matches(signature)) {
            if (state.status == LiveScanStatus.SCHEDULED || (!manual && state.status == LiveScanStatus.COMPLETED)) {
                return;
            }
        }

        boolean ignoreState = Configs.Generic.MATERIAL_LIST_IGNORE_STATE.getBooleanValue();
        TaskCountBlocksPlacement task = new TaskCountBlocksPlacement(placement, materialList, ignoreState);
        TaskScheduler.getInstanceClient().scheduleTask(task, 20);
        LIVE_SCANS.put(placement, new LiveScanState(signature, LiveScanStatus.SCHEDULED));

        if (showMessage) {
            InfoUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
        }
    }

    private static SchematicPlacement placementFor(MaterialListBase materialList) {
        if (materialList instanceof ChunkMissingMaterialList list) {
            return list.placement();
        }

        if (materialList instanceof MaterialListPlacement && materialList instanceof MaterialListPlacementAccess access) {
            return access.lmlp$getPlacement();
        }

        return null;
    }

    private static MaterialListBase getOrCreatePlacementMaterialList(SchematicPlacement placement, MaterialListBase optionsSource) {
        if (placement instanceof SchematicPlacementMaterialListAccess access) {
            MaterialListBase materialList = access.lmlp$getMaterialList();
            if (materialList == null) {
                materialList = new MaterialListPlacement(placement);
                if (optionsSource != null) {
                    materialList.fromJson(optionsSource.toJson());
                }
                access.lmlp$setMaterialList(materialList);
            }
            return materialList;
        }

        return placement.getMaterialList();
    }

    private static void fillSchematicCacheIfEmpty(SchematicPlacement placement, MaterialListBase materialList) {
        if (materialList.getMaterialsAll().isEmpty()) {
            refreshPlacementList(placement, materialList);
        }
    }

    private static String liveScanSignature(SchematicPlacement placement, MaterialListBase materialList) {
        return signature(placement) + '|' + materialList.getMaterialListType().getStringValue();
    }

    private static LitematicaSchematic schematicFor(SchematicPlacement placement) {
        LitematicaSchematic schematic = placement.getSchematic();
        if (schematic != null) {
            return schematic;
        }

        File file = placement.getSchematicFile();
        if (file != null) {
            return SchematicHolder.getInstance().getOrLoad(file);
        }

        return null;
    }

    private static Collection<String> enabledRegionNames(SchematicPlacement placement, LitematicaSchematic schematic) {
        List<String> names = new ArrayList<>();
        for (SubRegionPlacement region : placement.getAllSubRegionsPlacements()) {
            if (region.isEnabled()) {
                names.add(region.getName());
            }
        }

        if (names.isEmpty() && placement.getAllSubRegionsPlacements().isEmpty()) {
            names.addAll(schematic.getAreas().keySet());
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

    private enum LiveScanStatus {
        SCHEDULED,
        COMPLETED
    }

    private enum PlacementSource {
        CURRENT_SELECTED,
        CURRENT_AVAILABLE,
        LAST_KNOWN,
        DIRECT,
        NONE
    }

    private record ResolveSnapshot(String currentDimension, boolean selectedExists, int placementCount, PlacementContext lastKnownContext) {
        private boolean lastKnownExists() {
            return this.lastKnownContext != null;
        }

        private String lastKnownDimension() {
            return this.lastKnownContext == null ? null : this.lastKnownContext.dimension();
        }
    }

    private record PlacementResolution(
            String caller,
            SchematicPlacement placement,
            PlacementSource source,
            ResolveSnapshot snapshot) {
        private static PlacementResolution direct(SchematicPlacement placement, String caller) {
            PlacementContext lastKnown = lastKnownContext;
            return new PlacementResolution(
                    caller,
                    placement,
                    placement == null ? PlacementSource.NONE : PlacementSource.DIRECT,
                    new ResolveSnapshot(currentDimensionId(), placement != null, placement == null ? 0 : 1, lastKnown));
        }

        private static PlacementResolution resolved(String caller, SchematicPlacement placement, PlacementSource source, ResolveSnapshot snapshot) {
            return new PlacementResolution(caller, placement, source, snapshot);
        }

        private static PlacementResolution missing(String caller, ResolveSnapshot snapshot) {
            return new PlacementResolution(caller, null, PlacementSource.NONE, snapshot);
        }

        private boolean hasPlacement() {
            return this.placement != null;
        }
    }

    private static final class PlacementContext {
        private final SchematicPlacement placement;
        private final String signature;
        private final String schematicPath;
        private String name;
        private String dimension;
        private String lastReason;
        private boolean cacheGenerated;

        private PlacementContext(SchematicPlacement placement, String dimension, String reason) {
            this.placement = placement;
            this.signature = ChunkMissingMaterialListCache.signature(placement);
            this.schematicPath = ChunkMissingMaterialListCache.schematicPath(placement);
            this.name = placement.getName();
            this.dimension = dimension;
            this.lastReason = reason;
        }

        private void refresh(String reason, String currentDimension) {
            this.name = this.placement.getName();
            this.lastReason = reason;
            if (this.dimension == null && currentDimension != null) {
                this.dimension = currentDimension;
                LOGGER.info("[LMLP material-list] placement dimension learned reason={} name={} dimension={} schematic={} signature={}",
                        reason, this.name, this.dimension, this.schematicPath, this.signature);
            }
        }

        private void markCacheGenerated() {
            this.cacheGenerated = true;
        }

        private SchematicPlacement placement() {
            return this.placement;
        }

        private String name() {
            return this.name;
        }

        private String dimension() {
            return this.dimension;
        }

        private String schematicPath() {
            return this.schematicPath;
        }

        private String signature() {
            return this.signature;
        }

        private boolean cacheGenerated() {
            return this.cacheGenerated;
        }
    }

    private static final class LiveScanState {
        private final String signature;
        private final LiveScanStatus status;

        private LiveScanState(String signature, LiveScanStatus status) {
            this.signature = signature;
            this.status = status;
        }

        private boolean matches(String signature) {
            return this.signature.equals(signature);
        }
    }
}
