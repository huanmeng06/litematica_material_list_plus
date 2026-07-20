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
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import io.github.huanmeng06.lmlp.access.MaterialListSourceAccess;
import io.github.huanmeng06.lmlp.access.SchematicPlacementMaterialListAccess;
import io.github.huanmeng06.lmlp.cache.WorldMaterialCacheIndex.EntryRecord;
import io.github.huanmeng06.lmlp.cache.WorldMaterialCacheIndex.LoadResult;
import io.github.huanmeng06.lmlp.cache.WorldMaterialCacheIndex.PlacementRecord;
import io.github.huanmeng06.lmlp.gui.IgnoredMaterialRegistry;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.class_1923;
import net.minecraft.class_2338;
import net.minecraft.class_2382;
import net.minecraft.class_2680;
import net.minecraft.class_2791;
import net.minecraft.class_2806;
import net.minecraft.class_2818;
import net.minecraft.class_310;
import net.minecraft.class_631;
import net.minecraft.class_638;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ChunkMissingMaterialListCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(LitematicaMaterialListPlus.MOD_ID);
    private static final Map<SchematicPlacement, ChunkMissingMaterialList> LISTS = new IdentityHashMap<>();
    private static final Map<String, OfflineCachedMaterialList> OFFLINE_LISTS = new LinkedHashMap<>();
    private static final Map<SchematicPlacement, LiveScanState> LIVE_SCANS = new IdentityHashMap<>();
    private static final Map<SchematicPlacement, PlacementContext> PLACEMENT_CONTEXTS = new IdentityHashMap<>();
    private static final Map<PlacementKey, PlacementContext> PLACEMENT_CONTEXTS_BY_KEY = new LinkedHashMap<>();
    private static final ThreadLocal<Boolean> APPLYING_SCHEMATIC_CACHE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<MaterialListDataSource> APPLYING_CACHE_SOURCE = ThreadLocal.withInitial(() -> MaterialListDataSource.UNKNOWN);
    private static PlacementContext lastKnownContext;
    private static PlacementContext selectedMaterialListContext;
    private static String currentWorldId;
    private static boolean loadingWorldIndex;
    private static boolean pendingPersist;
    private static String pendingPersistReason;

    private ChunkMissingMaterialListCache() {
    }

    public static void onWorldJoined(class_310 client, String reason) {
        String worldId = WorldMaterialCacheIndex.resolveWorldId(client);
        if (worldId == null) {
            LOGGER.warn("[LMLP cache-index] world join skipped reason=missing_world_id currentDimension={}", currentDimensionId());
            abandonWorldSession(reason + ".missing_world_id");
            return;
        }

        startWorldSession(worldId, reason);
        rememberCurrentPlacements(reason + ".online_scan");
        persistKnownContexts(reason + ".post_join_scan");
    }

    public static void onWorldDisconnected(String reason) {
        flushKnownContexts(reason + ".before_clear");
        clearRuntimeState(reason, true);
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
                        context.updateMaterialCache(materialList.getMaterialListType(), materialList.getMaterialsAll());
                    }
                    LISTS.put(placement, new ChunkMissingMaterialList(placement, materialList));
                    persistKnownContexts("material_list.entries.cache_snapshot");
                }
            }
        }
    }

    public static String materialListContextKey(MaterialListBase materialList, String caller) {
        if (materialList == null) {
            return "";
        }

        if (materialList instanceof OfflineCachedMaterialList offlineList) {
            return offlineList.contextKey();
        }

        SchematicPlacement placement = placementFor(materialList);
        if (placement != null) {
            PlacementContext context = rememberPlacement(placement, caller + ".material_list_context_key");
            return context == null ? "" : context.key().value();
        }

        if (materialList == DataManager.getMaterialList()
                && selectedMaterialListContext != null
                && selectedMaterialListContext.canOpenMaterialList()) {
            return selectedMaterialListContext.key().value();
        }

        LOGGER.info("[LMLP material-list] context key unresolved caller={} listClass={} currentDimension={} selectedContext={}",
                caller,
                materialList.getClass().getName(),
                currentDimensionId(),
                selectedMaterialListContext == null ? null : selectedMaterialListContext.key());
        return "";
    }

    static void refresh(ChunkMissingMaterialList list) {
        setSchematicEntries(list, list.placement(), list.getMaterialListType());
    }

    static void refreshOfflineMaterialList(String contextKey, OfflineCachedMaterialList materialList) {
        PlacementContext context = PLACEMENT_CONTEXTS_BY_KEY.get(PlacementKey.fromString(contextKey));
        List<MaterialListEntry> entries = context == null ? List.of() : context.materialEntries().stream()
                .map(EntryRecord::toEntry)
                .filter(Objects::nonNull)
                .toList();
        BlockInfoListType listType = context == null
                ? materialList.getMaterialListType()
                : materialList.getMaterialsAll().isEmpty() ? context.materialListType() : materialList.getMaterialListType();

        APPLYING_SCHEMATIC_CACHE.set(true);
        APPLYING_CACHE_SOURCE.set(MaterialListDataSource.OFFLINE_CACHE);
        try {
            materialList.applyCachedEntries(entries, listType);
        } finally {
            APPLYING_CACHE_SOURCE.set(MaterialListDataSource.UNKNOWN);
            APPLYING_SCHEMATIC_CACHE.set(false);
        }

        if (materialList instanceof MaterialListSourceAccess access) {
            access.lmlp$setDataSource(MaterialListDataSource.OFFLINE_CACHE);
        }

        LOGGER.info("[LMLP material-list] offline cache material list refreshed key={} entries={} type={} currentDimension={}",
                contextKey, entries.size(), listType.getStringValue(), currentDimensionId());
    }

    public static void refreshPlacementList(SchematicPlacement placement, MaterialListBase materialList) {
        rememberPlacement(placement, "material_list.refresh_cache");
        setSchematicEntries(materialList, placement, materialList.getMaterialListType());
    }

    public static MaterialListBase refreshForPlacementState(SchematicPlacement placement, MaterialListBase materialList) {
        PlacementContext context = rememberPlacement(placement, "refreshForPlacementState.direct");
        PlacementResolution resolution = PlacementResolution.direct(context, "refreshForPlacementState");
        return refreshForPlacementState(resolution, materialList, false);
    }

    // A context can point at a stale SchematicPlacement instance (Litematica
    // rebuilds its placement list on dimension change, handing out new objects)
    // or be a disk-restored OFFLINE_CACHE with no placement, while a live
    // placement of the same identity is actually loaded right now. Before
    // opening or treating a context as offline, re-link the current live
    // in-manager placement of the same identity so it opens live instead of
    // showing "离线缓存". No-op when the context is already backed by a live
    // in-manager placement, and a genuine offline context (no live match) is
    // left untouched.
    private static PlacementContext relinkLivePlacement(PlacementContext context) {
        if (context == null) {
            return null;
        }
        if (context.placement() != null && isPlacementInCurrentManager(context.placement())) {
            return context;
        }

        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        if (manager == null) {
            return context;
        }

        for (SchematicPlacement placement : manager.getAllSchematicsPlacements()) {
            if (PlacementKey.of(placement).equals(context.key())) {
                PlacementContext relinked = rememberPlacement(placement, "relink.live_by_identity");
                LOGGER.info("[LMLP cache-index] relinked live placement by identity key={} name={} sourceState={} placementDimension={} currentDimension={}",
                        context.key(), context.name(), relinked == null ? null : relinked.sourceState(), context.dimension(), currentDimensionId());
                return relinked == null ? context : relinked;
            }
        }

        return context;
    }

    public static MaterialListBase getOrCreateMaterialListForOpen(MaterialListBase materialList, String caller) {
        PlacementResolution resolution = resolvePlacementForMaterialList(materialList, caller);
        if (!resolution.hasTarget()) {
            logNoPlacement(caller, resolution);
            return null;
        }

        PlacementContext context = relinkLivePlacement(resolution.context());
        if (context.isOfflineCache()) {
            logRoute(resolution, context, ReadMode.OFFLINE_CACHE);
            return getOrCreateOffline(context, caller);
        }

        return refreshForPlacementState(resolution, materialList, false);
    }

    public static MaterialListBase getOrCreateMaterialListForExplicitContext(String contextKey, MaterialListBase materialList, String caller) {
        ensureWorldSession(caller + ".explicit_context");
        PlacementContext context = PLACEMENT_CONTEXTS_BY_KEY.get(PlacementKey.fromString(contextKey));
        if (context == null) {
            LOGGER.warn("[LMLP material-list] explicit context open failed reason=missing_context caller={} key={} currentDimension={} knownContexts={}",
                    caller, contextKey, currentDimensionId(), PLACEMENT_CONTEXTS_BY_KEY.size());
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "lmlp.message.known_placement_context_missing");
            return null;
        }

        context = relinkLivePlacement(context);
        selectContext(context, caller + ".explicit_context");
        PlacementResolution resolution = PlacementResolution.direct(context, caller + ".explicit_context");
        if (context.isOfflineCache()) {
            logRoute(resolution, context, ReadMode.OFFLINE_CACHE);
            if (!context.hasMaterialCache()) {
                LOGGER.warn("[LMLP material-list] explicit context open failed reason=missing_entries caller={} key={} name={} dimension={} sourceState={}",
                        caller, context.key(), context.name(), context.dimension(), context.sourceState());
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "lmlp.message.known_placement_no_material_cache");
                return null;
            }

            return getOrCreateOffline(context, caller + ".explicit_context");
        }

        return refreshForPlacementState(resolution, materialList, false);
    }

    public static MaterialListBase refreshLastKnownPlacement(MaterialListBase materialList) {
        PlacementResolution resolution = resolvePlacementForMaterialList(materialList, "refreshLastKnownPlacement");
        if (!resolution.hasTarget()) {
            return null;
        }

        if (resolution.context().isOfflineCache()) {
            return getOrCreateOffline(resolution.context(), "refreshLastKnownPlacement");
        }

        return refreshForPlacementState(resolution, materialList, false);
    }

    public static boolean refreshForCurrentState(MaterialListBase materialList, boolean showLiveMessage) {
        if (materialList instanceof OfflineCachedMaterialList offlineList) {
            offlineList.reCreateMaterialList();
            return true;
        }

        SchematicPlacement placement = placementFor(materialList);
        if (placement == null) {
            return false;
        }

        PlacementContext context = rememberPlacement(placement, "refreshForCurrentState");
        PlacementResolution resolution = PlacementResolution.direct(context, "refreshForCurrentState");
        refreshForPlacementState(resolution, materialList, showLiveMessage);
        return true;
    }

    public static boolean shouldUseSchematicCache(SchematicPlacement placement, MaterialListBase materialList) {
        PlacementContext context = rememberPlacement(placement, "shouldUseSchematicCache");
        return resolveReadMode(context) != ReadMode.LIVE;
    }

    public static void rememberPlacementContext(SchematicPlacement placement, String reason) {
        rememberPlacement(placement, reason);
    }

    public static void selectMaterialListPlacement(SchematicPlacement placement, String reason) {
        PlacementContext context = rememberPlacement(placement, reason);
        if (context != null) {
            selectContext(context, reason);
        }
    }

    public static void selectCurrentPlacementForMaterialListOpen(String reason) {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        if (manager == null) {
            return;
        }

        SchematicPlacement selected = manager.getSelectedSchematicPlacement();
        if (selected != null) {
            selectMaterialListPlacement(selected, reason);
        }
    }

    public static void selectEditablePlacement(SchematicPlacement placement, String reason) {
        if (placement == null) {
            return;
        }

        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        if (manager != null && isPlacementInCurrentManager(placement) && manager.getSelectedSchematicPlacement() != placement) {
            manager.setSelectedSchematicPlacement(placement);
        }

        selectMaterialListPlacement(placement, reason);
    }

    public static void selectMaterialListContext(String contextKey, String reason) {
        ensureWorldSession(reason + ".select_context");
        PlacementContext context = PLACEMENT_CONTEXTS_BY_KEY.get(PlacementKey.fromString(contextKey));
        if (context == null) {
            LOGGER.warn("[LMLP material-list] explicit context selection failed reason={} key={} currentDimension={} knownContexts={}",
                    reason, contextKey, currentDimensionId(), PLACEMENT_CONTEXTS_BY_KEY.size());
            return;
        }

        selectContext(context, reason);
    }

    public static List<KnownPlacementContext> knownPlacementContexts() {
        ensureWorldSession("known_contexts.snapshot");
        rememberCurrentPlacements("known_contexts.snapshot");
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        SchematicPlacement selected = manager == null ? null : manager.getSelectedSchematicPlacement();
        int placementCount = manager == null ? 0 : manager.getAllSchematicsPlacements().size();
        LOGGER.debug("[LMLP placement-list] known contexts snapshot worldId={} currentDimension={} selected={} placementCount={} knownContextCount={} selectedContext={}",
                currentWorldId, currentDimensionId(), placementDebugName(selected), placementCount, PLACEMENT_CONTEXTS_BY_KEY.size(),
                selectedMaterialListContext == null ? null : selectedMaterialListContext.key());
        return PLACEMENT_CONTEXTS_BY_KEY.values().stream()
                .sorted(Comparator
                        .comparing((PlacementContext context) -> displayDimension(context.dimension()))
                        .thenComparing(PlacementContext::name)
                        .thenComparing(context -> context.key().value()))
                .map(PlacementContext::view)
                .toList();
    }

    public static KnownPlacementContext contextFor(SchematicPlacement placement) {
        PlacementContext context = rememberPlacement(placement, "context_for");
        return context == null ? null : context.view();
    }

    public static KnownPlacementContext knownContextFor(SchematicPlacement placement) {
        PlacementContext context = PLACEMENT_CONTEXTS.get(placement);
        return context == null ? null : context.view();
    }

    public static LitematicaSchematic schematicForPlacement(SchematicPlacement placement) {
        return placement == null ? null : schematicFor(placement);
    }

    public static boolean isExplicitlySelected(SchematicPlacement placement) {
        return selectedMaterialListContext != null && selectedMaterialListContext.placement() == placement;
    }

    public static boolean isMaterialListContextSelected(String contextKey) {
        return selectedMaterialListContext != null
                && selectedMaterialListContext.key().equals(PlacementKey.fromString(contextKey));
    }

    public static boolean canEditPlacement(SchematicPlacement placement) {
        PlacementContext context = rememberPlacement(placement, "can_edit");
        boolean inCurrentManager = isPlacementInCurrentManager(placement);
        boolean canEdit = canEditPlacement(context, inCurrentManager);
        LOGGER.debug("[LMLP placement-list] canModifyPlacement key={} name={} sourceState={} placementDimension={} currentDimension={} placementRef={} inCurrentManager={} chunksLoaded={} result={}",
                context == null ? null : context.key(), placementDebugName(placement), context == null ? null : context.sourceState(),
                context == null ? null : context.dimension(), currentDimensionId(), placement != null, inCurrentManager,
                placement != null && arePlacementChunksLoaded(placement), canEdit);
        return canEdit;
    }

    public static boolean removeKnownPlacement(SchematicPlacement placement, boolean allowCurrentDimensionRemoval, String reason) {
        PlacementContext context = rememberPlacement(placement, reason + ".before_remove");
        if (context == null) {
            return false;
        }

        return removeKnownContext(context, allowCurrentDimensionRemoval, reason);
    }

    public static boolean removeKnownPlacementContext(String contextKey, boolean allowCurrentDimensionRemoval, String reason) {
        PlacementContext context = PLACEMENT_CONTEXTS_BY_KEY.get(PlacementKey.fromString(contextKey));
        if (context == null) {
            return false;
        }

        return removeKnownContext(context, allowCurrentDimensionRemoval, reason);
    }

    public static boolean clearKnownPlacementCache(String contextKey, String reason) {
        ensureWorldSession(reason + ".clear_cache");
        PlacementContext context = PLACEMENT_CONTEXTS_BY_KEY.get(PlacementKey.fromString(contextKey));
        if (context == null) {
            LOGGER.warn("[LMLP cache-clear] skipped reason=missing_context caller={} key={} currentDimension={} knownContexts={}",
                    reason, contextKey, currentDimensionId(), PLACEMENT_CONTEXTS_BY_KEY.size());
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "lmlp.message.known_placement_context_missing");
            return false;
        }

        String key = context.key().value();
        boolean removeOfflineContext = context.isOfflineCache();
        LOGGER.info("[LMLP cache-clear] requested caller={} key={} name={} sourceState={} placementDimension={} currentDimension={} placementRef={} materialEntries={} removeOfflineContext={}",
                reason, context.key(), context.name(), context.sourceState(), context.dimension(), currentDimensionId(),
                context.placement() != null, context.materialEntries().size(), removeOfflineContext);

        if (context.placement() != null) {
            LISTS.remove(context.placement());
            LIVE_SCANS.remove(context.placement());
        }
        OFFLINE_LISTS.remove(key);
        IgnoredMaterialRegistry.clearPlacement(key);
        MaterialListPlusState.clearRecipeCaches();

        if (removeOfflineContext) {
            forgetContext(context, reason + ".clear_offline_cache", true);
        } else {
            context.clearMaterialCache(reason + ".clear_online_cache");
            persistKnownContexts(reason + ".cache_cleared");
        }

        LOGGER.info("[LMLP cache-clear] completed caller={} key={} removedOfflineContext={} remainingKnownContexts={}",
                reason, key, removeOfflineContext, PLACEMENT_CONTEXTS_BY_KEY.size());
        InfoUtils.showGuiOrInGameMessage(MessageType.INFO, "lmlp.message.cache_cleared");
        return true;
    }

    public static void forgetPlacementContext(SchematicPlacement placement, String reason) {
        PlacementContext context = PLACEMENT_CONTEXTS.get(placement);
        if (context != null) {
            forgetContext(context, reason, true);
        }
    }

    public static void rememberCurrentPlacements(String reason) {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        if (manager == null) {
            return;
        }

        int count = 0;
        for (SchematicPlacement placement : manager.getAllSchematicsPlacements()) {
            rememberPlacement(placement, reason + ".available");
            count++;
        }
        rememberPlacement(manager.getSelectedSchematicPlacement(), reason + ".selected");
        LOGGER.debug("[LMLP cache-index] current dimension online placement scan reason={} worldId={} currentDimension={} onlinePlacements={}",
                reason, currentWorldId, currentDimensionId(), count);
    }

    public static MaterialListDataSource applyingCacheSource() {
        return APPLYING_CACHE_SOURCE.get();
    }

    private static MaterialListBase refreshForPlacementState(PlacementResolution resolution, MaterialListBase materialList, boolean showLiveMessage) {
        PlacementContext context = resolution.context();
        SchematicPlacement placement = context == null ? null : context.placement();
        if (placement == null) {
            logNoPlacement(resolution.caller(), resolution);
            return null;
        }

        context.refreshOnline(placement, resolution.caller() + ".use", null);
        ReadMode readMode = resolveReadMode(context);
        boolean useCache = readMode != ReadMode.LIVE;
        logRoute(resolution, context, readMode);

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

    private static MaterialListBase getOrCreateOffline(PlacementContext context, String caller) {
        if (!context.hasMaterialCache()) {
            LOGGER.warn("[LMLP material-list] offline cache open failed reason=missing_entries caller={} key={} name={} dimension={}",
                    caller, context.key(), context.name(), context.dimension());
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "lmlp.message.offline_cache_missing");
            return null;
        }

        OfflineCachedMaterialList list = OFFLINE_LISTS.get(context.key().value());
        if (list == null) {
            list = new OfflineCachedMaterialList(context.key().value(), context.name());
            OFFLINE_LISTS.put(context.key().value(), list);
        } else {
            list.reCreateMaterialList();
        }

        DataManager.setMaterialList(list);
        LOGGER.info("[LMLP material-list] opened offline cache caller={} key={} name={} dimension={} entries={} currentDimension={}",
                caller, context.key(), context.name(), context.dimension(), context.materialEntries().size(), currentDimensionId());
        return list;
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

    public static ReadMode resolveReadMode(KnownPlacementContext context) {
        if (context == null) {
            return ReadMode.OFFLINE_CACHE;
        }

        return resolveReadMode(PlacementKey.fromString(context.key()), context);
    }

    public static ReadMode resolveReadMode(String contextKey) {
        return resolveReadMode(PlacementKey.fromString(contextKey), null);
    }

    private static ReadMode resolveReadMode(PlacementKey key, KnownPlacementContext snapshot) {
        ReadMode activeMaterialListMode = activeMaterialListReadMode(key, snapshot == null ? null : snapshot.placement());
        if (activeMaterialListMode != null && activeMaterialListMode != ReadMode.LIVE) {
            return activeMaterialListMode;
        }

        PlacementContext runtimeContext = PLACEMENT_CONTEXTS_BY_KEY.get(key);
        if (runtimeContext == null && snapshot != null && snapshot.placement() != null) {
            runtimeContext = PLACEMENT_CONTEXTS.get(snapshot.placement());
        }

        if (runtimeContext != null) {
            return resolveReadMode(runtimeContext);
        }

        if (snapshot != null) {
            return resolveReadMode(snapshot.sourceState(), snapshot.placement(), snapshot.dimension());
        }

        return ReadMode.OFFLINE_CACHE;
    }

    public static ReadMode resolveReadMode(MaterialListBase materialList) {
        SchematicPlacement placement = placementFor(materialList);
        PlacementContext context = placement == null ? null : PLACEMENT_CONTEXTS.get(placement);
        if (context != null) {
            return resolveReadMode(context);
        }

        return resolveReadMode(dataSource(materialList));
    }

    public static ReadMode resolveReadMode(MaterialListDataSource dataSource) {
        if (dataSource == null) {
            return null;
        }

        return switch (dataSource) {
            case WORLD_SCAN -> ReadMode.LIVE;
            case SCHEMATIC_CACHE -> ReadMode.CHUNK_CACHE;
            case CROSS_DIMENSION_CACHE -> ReadMode.DIMENSION_CACHE;
            case OFFLINE_CACHE -> ReadMode.OFFLINE_CACHE;
            default -> null;
        };
    }

    private static ReadMode activeMaterialListReadMode(PlacementKey key, SchematicPlacement snapshotPlacement) {
        if (key == null) {
            return null;
        }

        MaterialListBase materialList = DataManager.getMaterialList();
        ReadMode readMode = resolveReadMode(dataSource(materialList));
        if (readMode == null) {
            return null;
        }

        PlacementKey materialListKey = materialListKey(materialList);
        if (key.equals(materialListKey)) {
            return readMode;
        }

        SchematicPlacement materialListPlacement = placementFor(materialList);
        return snapshotPlacement != null && materialListPlacement == snapshotPlacement ? readMode : null;
    }

    private static PlacementKey materialListKey(MaterialListBase materialList) {
        if (materialList instanceof OfflineCachedMaterialList offlineList) {
            return PlacementKey.fromString(offlineList.contextKey());
        }

        SchematicPlacement placement = placementFor(materialList);
        PlacementContext context = placement == null ? null : PLACEMENT_CONTEXTS.get(placement);
        return context == null ? null : context.key();
    }

    public static boolean hasKnownDataSource(MaterialListBase materialList) {
        return dataSource(materialList) != MaterialListDataSource.UNKNOWN;
    }

    public static boolean isSchematicCacheSource(MaterialListBase materialList) {
        MaterialListDataSource dataSource = dataSource(materialList);
        return dataSource == MaterialListDataSource.SCHEMATIC_CACHE
                || dataSource == MaterialListDataSource.CROSS_DIMENSION_CACHE
                || dataSource == MaterialListDataSource.OFFLINE_CACHE;
    }

    public static boolean isWorldScanSource(MaterialListBase materialList) {
        return dataSource(materialList) == MaterialListDataSource.WORLD_SCAN;
    }

    public static boolean isChunkMissingState(MaterialListBase materialList) {
        return isSchematicCacheSource(materialList);
    }

    public static boolean arePlacementChunksLoaded(SchematicPlacement placement) {
        class_310 client = class_310.method_1551();
        class_638 world = client.field_1687;
        if (world == null || client.field_1724 == null || !isPlacementInCurrentDimension(placement)) {
            return false;
        }

        Collection<class_1923> touchedChunks;
        try {
            touchedChunks = placement.getTouchedChunks();
        } catch (RuntimeException exception) {
            LOGGER.debug("[LMLP placement-list] touched chunks unavailable name={} reason={}", placementDebugName(placement), exception.toString());
            return false;
        }

        if (touchedChunks == null || touchedChunks.isEmpty()) {
            return false;
        }

        class_631 chunkManager = world.method_2935();
        if (chunkManager == null) {
            return false;
        }

        for (class_1923 chunkPos : touchedChunks) {
            if (chunkPos == null || !world.method_8393(chunkPos.field_9181, chunkPos.field_9180)) {
                return false;
            }

            class_2791 chunk = chunkManager.method_12121(chunkPos.field_9181, chunkPos.field_9180, class_2806.field_12803, false);
            if (!(chunk instanceof class_2818) || !chunk.method_12009().method_12165(class_2806.field_12803)) {
                return false;
            }
        }

        return true;
    }

    private static PlacementResolution resolvePlacementForMaterialList(MaterialListBase materialList, String caller) {
        ensureWorldSession(caller + ".resolve");

        if (materialList instanceof OfflineCachedMaterialList offlineList) {
            PlacementContext context = PLACEMENT_CONTEXTS_BY_KEY.get(PlacementKey.fromString(offlineList.contextKey()));
            if (context != null) {
                ResolveSnapshot snapshot = currentResolveSnapshot(caller);
                return PlacementResolution.resolved(caller, context, PlacementSource.EXPLICIT_SELECTED, snapshot);
            }
        }

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

        if (selectedMaterialListContext != null) {
            selectedMaterialListContext.refresh(caller + ".selected_context_validate", null);
            if (selectedMaterialListContext.canOpenMaterialList()) {
                return PlacementResolution.resolved(caller, selectedMaterialListContext, PlacementSource.EXPLICIT_SELECTED, currentResolveSnapshot(caller));
            }

            LOGGER.warn("[LMLP material-list] explicit selection invalid key={} reason={} sourceState={} fallback=current_dimension",
                    selectedMaterialListContext.key(), caller, selectedMaterialListContext.sourceState());
            selectedMaterialListContext.setSelected(false);
            selectedMaterialListContext = null;
        }

        ResolveSnapshot snapshot = currentResolveSnapshot(caller);
        if (selected != null) {
            PlacementContext context = PLACEMENT_CONTEXTS.get(selected);
            if (context != null) {
                return PlacementResolution.resolved(caller, context, PlacementSource.CURRENT_SELECTED, snapshot);
            }
        }

        if (!placements.isEmpty()) {
            SchematicPlacement placement = placements.get(0);
            PlacementContext context = rememberPlacement(placement, caller + ".current_available_without_selecting_original");
            if (context != null) {
                return PlacementResolution.resolved(caller, context, PlacementSource.CURRENT_AVAILABLE, snapshot);
            }
        }

        if (lastKnownContext != null && lastKnownContext.canOpenMaterialList()) {
            return PlacementResolution.resolved(caller, lastKnownContext, PlacementSource.LAST_KNOWN, snapshot);
        }

        return PlacementResolution.missing(caller, snapshot);
    }

    private static PlacementContext rememberPlacement(SchematicPlacement placement, String reason) {
        if (placement == null) {
            return null;
        }

        ensureWorldSession(reason + ".remember");

        boolean inCurrentManager = isPlacementInCurrentManager(placement);
        String currentDimension = currentDimensionId();
        String dimension = inCurrentManager ? currentDimension : null;
        PlacementContext context = PLACEMENT_CONTEXTS.get(placement);
        if (context == null) {
            PlacementKey key = PlacementKey.of(placement);
            context = PLACEMENT_CONTEXTS_BY_KEY.get(key);
            if (context != null) {
                context.upgradeOnline(placement, dimension, reason);
                PLACEMENT_CONTEXTS.put(placement, context);
                LOGGER.debug("[LMLP cache-index] offline context upgraded to online reason={} key={} name={} dimension={} currentDimension={}",
                        reason, context.key(), context.name(), context.dimension(), currentDimensionId());
            } else {
                context = new PlacementContext(placement, dimension, reason);
                PLACEMENT_CONTEXTS.put(placement, context);
                PLACEMENT_CONTEXTS_BY_KEY.put(context.key(), context);
                LOGGER.debug("[LMLP material-list] remember placement reason={} key={} name={} dimension={} inCurrentManager={} schematic={} signature={}",
                        reason, context.key(), context.name(), context.dimension(), inCurrentManager, context.schematicPath(), context.signature());
            }
        } else {
            context.refreshOnline(placement, reason, dimension);
        }

        lastKnownContext = context;
        persistKnownContexts(reason + ".remembered");
        return context;
    }

    private static void selectContext(PlacementContext context, String reason) {
        selectedMaterialListContext = context;
        context.setSelected(true);
        for (PlacementContext other : PLACEMENT_CONTEXTS_BY_KEY.values()) {
            if (other != context) {
                other.setSelected(false);
            }
        }

        lastKnownContext = context;
        persistKnownContexts(reason + ".selected");
        LOGGER.info("[LMLP material-list] explicit selection reason={} key={} name={} sourceState={} dimension={} currentDimension={} knownContexts={}",
                reason, context.key(), context.name(), context.sourceState(), context.dimension(), currentDimensionId(), PLACEMENT_CONTEXTS_BY_KEY.size());
    }

    private static boolean removeKnownContext(PlacementContext context, boolean allowCurrentDimensionRemoval, String reason) {
        boolean canEdit = canEditPlacement(context, context.placement() != null && isPlacementInCurrentManager(context.placement()));
        LOGGER.info("[LMLP placement-list] remove requested reason={} key={} name={} sourceState={} placementDimension={} currentDimension={} canEdit={} allowCurrentDimensionRemoval={}",
                reason, context.key(), context.name(), context.sourceState(), context.dimension(), currentDimensionId(), canEdit, allowCurrentDimensionRemoval);

        if (context.placement() != null && canEdit && allowCurrentDimensionRemoval) {
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            boolean removed = manager != null && manager.removeSchematicPlacement(context.placement());
            if (removed) {
                forgetContext(context, "remove.current_dimension", true);
            }
            return removed;
        }

        forgetContext(context, context.isOfflineCache() ? "remove.offline_cache_only" : "remove.known_context_only", true);
        InfoUtils.showGuiOrInGameMessage(MessageType.INFO, context.isOfflineCache()
                ? "lmlp.message.offline_cache_removed"
                : "lmlp.message.known_placement_removed_return_to_dimension");
        return true;
    }

    private static void logRoute(PlacementResolution resolution, PlacementContext context, ReadMode readMode) {
        ResolveSnapshot snapshot = resolution.snapshot();
        LOGGER.debug("[LMLP material-list] route caller={} worldId={} currentDimension={} selectedExists={} placementCount={} knownContextCount={} selectedContext={} lastKnownExists={} lastKnownDimension={} source={} sourceState={} placement={} placementDimension={} cacheGenerated={} materialCacheEntries={} readMode={} result={}",
                resolution.caller(),
                currentWorldId,
                snapshot.currentDimension(),
                snapshot.selectedExists(),
                snapshot.placementCount(),
                snapshot.knownContextCount(),
                snapshot.selectedContextKey(),
                snapshot.lastKnownExists(),
                snapshot.lastKnownDimension(),
                resolution.source(),
                context == null ? null : context.sourceState(),
                placementDebugName(resolution.placement()),
                context == null ? null : context.dimension(),
                context != null && context.cacheGenerated(),
                context == null ? 0 : context.materialEntries().size(),
                readMode,
                dataSourceForReadMode(readMode));
    }

    private static void logNoPlacement(String caller, PlacementResolution resolution) {
        ResolveSnapshot snapshot = resolution.snapshot();
        LOGGER.warn("[LMLP material-list] no_placement_selected path=LMLP caller={} worldId={} currentDimension={} selectedExists={} placementCount={} knownContextCount={} selectedContext={} lastKnownExists={} lastKnownDimension={}",
                caller,
                currentWorldId,
                snapshot.currentDimension(),
                snapshot.selectedExists(),
                snapshot.placementCount(),
                snapshot.knownContextCount(),
                snapshot.selectedContextKey(),
                snapshot.lastKnownExists(),
                snapshot.lastKnownDimension());
        InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
    }

    private static void forgetContext(PlacementContext context, String reason, boolean persist) {
        if (context.placement() != null) {
            PLACEMENT_CONTEXTS.remove(context.placement());
            LISTS.remove(context.placement());
            LIVE_SCANS.remove(context.placement());
        }
        PLACEMENT_CONTEXTS_BY_KEY.remove(context.key());
        OFFLINE_LISTS.remove(context.key().value());
        if (lastKnownContext == context) {
            lastKnownContext = latestKnownContext();
        }
        if (selectedMaterialListContext == context) {
            selectedMaterialListContext = null;
        }
        LOGGER.info("[LMLP placement-list] context forgotten reason={} key={} name={} sourceState={} dimension={} remainingKnownContexts={}",
                reason, context.key(), context.name(), context.sourceState(), context.dimension(), PLACEMENT_CONTEXTS_BY_KEY.size());
        if (persist) {
            persistKnownContexts(reason + ".forgotten");
        }
    }

    private static void rekeyContext(PlacementContext context, PlacementKey oldKey) {
        if (!oldKey.equals(context.key())) {
            PLACEMENT_CONTEXTS_BY_KEY.remove(oldKey);
            PLACEMENT_CONTEXTS_BY_KEY.put(context.key(), context);
            LOGGER.info("[LMLP material-list] placement key updated oldKey={} newKey={} name={} dimension={}",
                    oldKey, context.key(), context.name(), context.dimension());
        }
    }

    private static PlacementContext latestKnownContext() {
        PlacementContext latest = null;
        for (PlacementContext context : PLACEMENT_CONTEXTS_BY_KEY.values()) {
            if (latest == null || context.lastSeen() > latest.lastSeen()) {
                latest = context;
            }
        }
        return latest;
    }

    private static boolean canEditPlacement(PlacementContext context, boolean inCurrentManager) {
        SchematicPlacement placement = context == null ? null : context.placement();
        return context != null
                && context.sourceState() == SourceState.ONLINE
                && placement != null
                && Objects.equals(context.dimension(), currentDimensionId())
                && inCurrentManager;
    }

    private static ReadMode resolveReadMode(PlacementContext context) {
        if (context == null) {
            return ReadMode.OFFLINE_CACHE;
        }

        return resolveReadMode(context.sourceState(), context.placement(), context.dimension());
    }

    private static ReadMode resolveReadMode(SourceState sourceState, SchematicPlacement placement, String dimension) {
        if (sourceState != SourceState.ONLINE || placement == null) {
            return ReadMode.OFFLINE_CACHE;
        }

        String currentDimension = currentDimensionId();
        if (!Objects.equals(normalizedDimension(dimension), normalizedDimension(currentDimension))) {
            return ReadMode.DIMENSION_CACHE;
        }

        if (!isPlacementInCurrentManager(placement)) {
            return ReadMode.OFFLINE_CACHE;
        }

        return arePlacementChunksLoaded(placement) ? ReadMode.LIVE : ReadMode.CHUNK_CACHE;
    }

    private static MaterialListDataSource dataSourceForReadMode(ReadMode readMode) {
        if (readMode == null) {
            return MaterialListDataSource.UNKNOWN;
        }

        return switch (readMode) {
            case LIVE -> MaterialListDataSource.WORLD_SCAN;
            case CHUNK_CACHE -> MaterialListDataSource.SCHEMATIC_CACHE;
            case DIMENSION_CACHE -> MaterialListDataSource.CROSS_DIMENSION_CACHE;
            case OFFLINE_CACHE -> MaterialListDataSource.OFFLINE_CACHE;
        };
    }

    private static MaterialListDataSource cacheSourceFor(SchematicPlacement placement, PlacementContext context) {
        ReadMode readMode = resolveReadMode(context);
        return readMode == ReadMode.LIVE ? MaterialListDataSource.SCHEMATIC_CACHE : dataSourceForReadMode(readMode);
    }

    private static String placementDebugName(SchematicPlacement placement) {
        return placement == null ? "<offline-cache>" : placement.getName() + "@" + schematicPath(placement);
    }

    private static boolean isPlacementInCurrentDimension(SchematicPlacement placement) {
        if (placement == null || !isPlacementInCurrentManager(placement)) {
            return false;
        }

        PlacementContext context = PLACEMENT_CONTEXTS.get(placement);
        if (context == null || context.dimension() == null || context.sourceState() != SourceState.ONLINE) {
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
        return placement != null && manager != null && manager.getAllSchematicsPlacements().contains(placement);
    }

    private static String currentDimensionId() {
        class_638 world = class_310.method_1551().field_1687;
        return world == null ? null : world.method_27983().method_29177().toString();
    }

    private static String displayDimension(String dimension) {
        String normalized = normalizedDimension(dimension);
        if (normalized.isEmpty()) {
            return "?";
        }
        if (normalized.equals("minecraft:overworld")) {
            return "Overworld";
        }
        if (normalized.equals("minecraft:the_nether")) {
            return "Nether";
        }
        if (normalized.equals("minecraft:the_end")) {
            return "End";
        }
        int colon = normalized.indexOf(':');
        return colon >= 0 ? normalized.substring(colon + 1) : normalized;
    }

    private static String normalizedDimension(String dimension) {
        if (dimension == null) {
            return "";
        }

        String normalized = dimension.trim();
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1).trim();
        }

        while (normalized.endsWith("]")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }

        int space = normalized.lastIndexOf(' ');
        if (space >= 0 && normalized.substring(space + 1).contains(":")) {
            normalized = normalized.substring(space + 1).trim();
        }

        return normalized;
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
        MaterialListDataSource dataSource = cacheSourceFor(placement, context);
        List<MaterialListEntry> entries = createEntries(placement, type);

        APPLYING_SCHEMATIC_CACHE.set(true);
        APPLYING_CACHE_SOURCE.set(dataSource);
        try {
            materialList.setMaterialListEntries(entries);
        } finally {
            APPLYING_CACHE_SOURCE.set(MaterialListDataSource.UNKNOWN);
            APPLYING_SCHEMATIC_CACHE.set(false);
        }

        if (materialList instanceof MaterialListSourceAccess access) {
            access.lmlp$setDataSource(dataSource);
        }
        if (context != null) {
            context.updateMaterialCache(type, entries);
            persistKnownContexts("set_schematic_entries.cache_snapshot");
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
            if (arePlacementChunksLoaded(placement) && materialList instanceof MaterialListSourceAccess access) {
                access.lmlp$setDataSource(MaterialListDataSource.WORLD_SCAN);
            }
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

        Path file = placement.getSchematicFile();
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

    private static String identitySignature(SchematicPlacement placement) {
        StringBuilder builder = new StringBuilder();
        appendPos(builder, placement.getOrigin());
        builder.append('|').append(placement.getRotation()).append('|').append(placement.getMirror()).append('|');
        builder.append(regionIdentityKey(placement.getAllSubRegionsPlacements()));
        return builder.toString();
    }

    private static String schematicPath(SchematicPlacement placement) {
        Path file = placement.getSchematicFile();
        return file == null ? "" : file.toAbsolutePath().toString();
    }

    private static String schematicName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return new File(path).getName();
    }

    private static String originPosition(SchematicPlacement placement) {
        return formatPos(placement.getOrigin());
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

    private static String regionIdentityKey(Collection<SubRegionPlacement> regions) {
        List<SubRegionPlacement> sorted = new ArrayList<>(regions);
        sorted.sort(Comparator.comparing(SubRegionPlacement::getName));
        StringBuilder builder = new StringBuilder();
        for (SubRegionPlacement region : sorted) {
            builder.append(region.getName()).append(':');
            appendPos(builder, region.getPos());
            builder.append(':').append(region.getRotation()).append(':').append(region.getMirror()).append(';');
        }
        return builder.toString();
    }

    private static void appendPos(StringBuilder builder, class_2338 pos) {
        builder.append(pos.method_10263()).append(',').append(pos.method_10264()).append(',').append(pos.method_10260());
    }

    private static String formatPos(class_2338 pos) {
        return "[" + pos.method_10263() + ", " + pos.method_10264() + ", " + pos.method_10260() + "]";
    }

    private static void ensureWorldSession(String reason) {
        if (loadingWorldIndex) {
            return;
        }

        String worldId = WorldMaterialCacheIndex.resolveWorldId(class_310.method_1551());
        if (worldId == null) {
            if (currentWorldId != null || !PLACEMENT_CONTEXTS_BY_KEY.isEmpty()) {
                LOGGER.warn("[LMLP cache-index] world session cleared reason=missing_world_id caller={} previousWorldId={}",
                        reason, currentWorldId);
                abandonWorldSession(reason + ".missing_world_id");
            }
            return;
        }

        if (!worldId.equals(currentWorldId)) {
            startWorldSession(worldId, reason);
        }
    }

    private static void startWorldSession(String worldId, String reason) {
        if (worldId == null || worldId.equals(currentWorldId)) {
            return;
        }

        flushKnownContexts(reason + ".before_world_switch");
        clearRuntimeState(reason + ".world_switch", false);
        currentWorldId = worldId;

        loadingWorldIndex = true;
        try {
            LoadResult result = WorldMaterialCacheIndex.load(worldId);
            for (PlacementRecord record : result.records()) {
                restoreOfflineContext(record);
            }

            if (result.selectedKey() != null) {
                PlacementContext selected = PLACEMENT_CONTEXTS_BY_KEY.get(PlacementKey.fromString(result.selectedKey()));
                if (selected != null) {
                    selectContextWithoutPersist(selected);
                }
            }

            LOGGER.info("[LMLP cache-index] world session loaded reason={} worldId={} path={} restoredOfflineContexts={} selectedKey={}",
                    reason, worldId, result.file().getAbsolutePath(), result.records().size(), result.selectedKey());
        } finally {
            loadingWorldIndex = false;
        }
    }

    private static void abandonWorldSession(String reason) {
        flushKnownContexts(reason + ".before_clear");
        clearRuntimeState(reason, true);
    }

    private static void restoreOfflineContext(PlacementRecord record) {
        PlacementKey key = PlacementKey.ofIdentity(record.schematicPath(), record.placementName(), record.placementIdentity());
        PlacementContext existing = PLACEMENT_CONTEXTS_BY_KEY.get(key);
        if (existing != null) {
            // A pre-fix disk file can hold several records for the same physical
            // placement under different dimensions; they now collapse onto one
            // key. Keep whichever restored record carries the richer material
            // cache so the migration doesn't drop cached entries.
            if (record.entries().size() > existing.materialEntries().size()) {
                PlacementContext replacement = new PlacementContext(record);
                PLACEMENT_CONTEXTS_BY_KEY.put(replacement.key(), replacement);
                if (replacement.selected()) {
                    selectContextWithoutPersist(replacement);
                }
                lastKnownContext = latestKnownContext();
            }
            return;
        }

        PlacementContext context = new PlacementContext(record);
        PLACEMENT_CONTEXTS_BY_KEY.put(context.key(), context);
        if (context.selected()) {
            selectContextWithoutPersist(context);
        }
        lastKnownContext = latestKnownContext();
        LOGGER.info("[LMLP cache-index] restored offline context key={} name={} dimension={} entries={} schematicMissing={}",
                context.key(), context.name(), context.dimension(), context.materialEntries().size(), context.schematicMissing());
    }

    private static void clearRuntimeState(String reason, boolean clearWorldId) {
        LISTS.clear();
        OFFLINE_LISTS.clear();
        LIVE_SCANS.clear();
        PLACEMENT_CONTEXTS.clear();
        PLACEMENT_CONTEXTS_BY_KEY.clear();
        lastKnownContext = null;
        selectedMaterialListContext = null;
        if (clearWorldId) {
            currentWorldId = null;
        }
        LOGGER.info("[LMLP cache-index] runtime state cleared reason={} clearWorldId={}", reason, clearWorldId);
    }

    // Marks the world cache dirty instead of writing immediately. The actual
    // write is coalesced to at most once per client tick (flushPendingPersistence),
    // because rememberCurrentPlacements calls rememberPlacement -> persist in a
    // loop over every placement, which otherwise rewrote the whole JSON N times
    // per GUI open. Critical persist-then-clear paths flush synchronously.
    private static void persistKnownContexts(String reason) {
        pendingPersist = true;
        pendingPersistReason = reason;
    }

    public static void flushPendingPersistence() {
        if (pendingPersist) {
            flushKnownContexts(pendingPersistReason);
        }
    }

    private static void flushKnownContexts(String reason) {
        pendingPersist = false;
        if (loadingWorldIndex || currentWorldId == null) {
            return;
        }

        List<PlacementRecord> records = PLACEMENT_CONTEXTS_BY_KEY.values().stream()
                .map(context -> context.toRecord(currentWorldId))
                .toList();
        WorldMaterialCacheIndex.save(currentWorldId, records, selectedMaterialListContext == null ? null : selectedMaterialListContext.key().value());
        LOGGER.debug("[LMLP cache-index] persisted contexts reason={} worldId={} entries={}", reason, currentWorldId, records.size());
    }

    private static ResolveSnapshot currentResolveSnapshot(String caller) {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        SchematicPlacement selected = manager == null ? null : manager.getSelectedSchematicPlacement();
        int placementCount = manager == null ? 0 : manager.getAllSchematicsPlacements().size();
        return new ResolveSnapshot(currentDimensionId(), selected != null, placementCount,
                PLACEMENT_CONTEXTS_BY_KEY.size(), lastKnownContext, selectedMaterialListContext);
    }

    private static void selectContextWithoutPersist(PlacementContext context) {
        selectedMaterialListContext = context;
        context.setSelected(true);
        for (PlacementContext other : PLACEMENT_CONTEXTS_BY_KEY.values()) {
            if (other != context) {
                other.setSelected(false);
            }
        }
        lastKnownContext = context;
    }

    private enum LiveScanStatus {
        SCHEDULED,
        COMPLETED
    }

    public enum SourceState {
        ONLINE,
        OFFLINE_CACHE
    }

    public enum ReadMode {
        LIVE,
        CHUNK_CACHE,
        DIMENSION_CACHE,
        OFFLINE_CACHE
    }

    private enum PlacementSource {
        EXPLICIT_SELECTED,
        CURRENT_SELECTED,
        CURRENT_AVAILABLE,
        LAST_KNOWN,
        DIRECT,
        NONE
    }

    private record ResolveSnapshot(
            String currentDimension,
            boolean selectedExists,
            int placementCount,
            int knownContextCount,
            PlacementContext lastKnownContext,
            PlacementContext selectedContext) {
        private boolean lastKnownExists() {
            return this.lastKnownContext != null;
        }

        private String lastKnownDimension() {
            return this.lastKnownContext == null ? null : this.lastKnownContext.dimension();
        }

        private String selectedContextKey() {
            return this.selectedContext == null ? null : this.selectedContext.key().value();
        }
    }

    private record PlacementResolution(
            String caller,
            PlacementContext context,
            PlacementSource source,
            ResolveSnapshot snapshot) {
        private static PlacementResolution direct(PlacementContext context, String caller) {
            PlacementContext lastKnown = lastKnownContext;
            return new PlacementResolution(
                    caller,
                    context,
                    context == null ? PlacementSource.NONE : PlacementSource.DIRECT,
                    new ResolveSnapshot(currentDimensionId(), context != null && context.placement() != null,
                            context == null ? 0 : 1, PLACEMENT_CONTEXTS_BY_KEY.size(), lastKnown, selectedMaterialListContext));
        }

        private static PlacementResolution resolved(String caller, PlacementContext context, PlacementSource source, ResolveSnapshot snapshot) {
            return new PlacementResolution(caller, context, source, snapshot);
        }

        private static PlacementResolution missing(String caller, ResolveSnapshot snapshot) {
            return new PlacementResolution(caller, null, PlacementSource.NONE, snapshot);
        }

        private boolean hasTarget() {
            return this.context != null && this.context.canOpenMaterialList();
        }

        private SchematicPlacement placement() {
            return this.context == null ? null : this.context.placement();
        }
    }

    public record KnownPlacementContext(
            SchematicPlacement placement,
            String key,
            SourceState sourceState,
            String name,
            String schematicPath,
            String schematicName,
            String originPosition,
            String dimension,
            String displayDimension,
            long lastSeen,
            long lastMaterialCacheUpdateTime,
            boolean selected,
            boolean cacheGenerated,
            boolean canEdit,
            boolean schematicMissing,
            boolean hasMaterialCache,
            String materialListType) {
        public boolean offlineCache() {
            return this.sourceState == SourceState.OFFLINE_CACHE;
        }
    }

    private record PlacementKey(String value) {
        // The identity key is deliberately dimension-independent. Litematica
        // placements are not dimension-scoped (getAllSchematicsPlacements()
        // returns them all regardless of the player's dimension), and the
        // dimension LMLP records is only the heuristic "where the player was
        // when this placement was first remembered". Embedding it here used to
        // let the same physical placement key differently across dimensions,
        // orphaning its persisted record into a ghost OFFLINE_CACHE row.
        private static PlacementKey of(SchematicPlacement placement) {
            return ofIdentity(schematicPath(placement), placement.getName(), identitySignature(placement));
        }

        private static PlacementKey ofIdentity(String schematicPath, String name, String identitySignature) {
            return new PlacementKey(nz(schematicPath) + '|' + nz(name) + '|' + nz(identitySignature));
        }

        private static String nz(String value) {
            return value == null ? "" : value;
        }

        private static PlacementKey fromString(String value) {
            return new PlacementKey(value == null ? "" : value);
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    private static final class PlacementContext {
        private SchematicPlacement placement;
        private PlacementKey key;
        private String signature;
        private String schematicPath;
        private String schematicName;
        private String originPosition;
        private String placementIdentity;
        private String name;
        private String dimension;
        private String lastReason;
        private long lastSeen;
        private long lastMaterialCacheUpdateTime;
        private boolean selected;
        private boolean cacheGenerated;
        private SourceState sourceState;
        private BlockInfoListType materialListType = BlockInfoListType.ALL;
        private List<EntryRecord> materialEntries = List.of();

        private PlacementContext(SchematicPlacement placement, String dimension, String reason) {
            this.placement = placement;
            this.sourceState = SourceState.ONLINE;
            this.signature = ChunkMissingMaterialListCache.signature(placement);
            this.placementIdentity = ChunkMissingMaterialListCache.identitySignature(placement);
            this.schematicPath = ChunkMissingMaterialListCache.schematicPath(placement);
            this.schematicName = ChunkMissingMaterialListCache.schematicName(this.schematicPath);
            this.originPosition = ChunkMissingMaterialListCache.originPosition(placement);
            this.key = PlacementKey.of(placement);
            this.name = placement.getName();
            this.dimension = dimension;
            this.lastReason = reason;
            this.lastSeen = System.currentTimeMillis();
        }

        private PlacementContext(PlacementRecord record) {
            this.placement = null;
            this.sourceState = SourceState.OFFLINE_CACHE;
            // Recompute the key from the record's stable identity fields rather
            // than trusting record.key(): old records carry a dimension prefix,
            // and rebuilding here migrates them to the dimension-independent
            // format (and collapses per-dimension duplicates on load).
            this.key = PlacementKey.ofIdentity(record.schematicPath(), record.placementName(), record.placementIdentity());
            this.dimension = record.dimension();
            this.name = record.placementName();
            this.schematicName = record.schematicName();
            this.schematicPath = record.schematicPath();
            this.originPosition = record.originPosition();
            this.placementIdentity = record.placementIdentity();
            this.signature = record.placementSignature();
            this.lastSeen = record.lastSeenTime();
            this.lastMaterialCacheUpdateTime = record.lastMaterialCacheUpdateTime();
            this.cacheGenerated = record.cacheGenerated();
            this.selected = record.selected();
            this.materialListType = BlockInfoListType.fromStringStatic(record.materialListType());
            this.materialEntries = List.copyOf(record.entries());
            this.lastReason = "cache_index.restore";
        }

        private void refresh(String reason, String currentDimension) {
            if (this.placement != null) {
                refreshOnline(this.placement, reason, currentDimension);
            } else {
                this.lastReason = reason;
                this.lastSeen = Math.max(this.lastSeen, System.currentTimeMillis());
            }
        }

        private void upgradeOnline(SchematicPlacement placement, String currentDimension, String reason) {
            this.placement = placement;
            this.sourceState = SourceState.ONLINE;
            this.refreshOnline(placement, reason, currentDimension);
        }

        private void refreshOnline(SchematicPlacement placement, String reason, String currentDimension) {
            this.placement = placement;
            this.sourceState = SourceState.ONLINE;
            this.name = placement.getName();
            this.lastReason = reason;
            this.lastSeen = System.currentTimeMillis();
            this.signature = ChunkMissingMaterialListCache.signature(placement);
            this.placementIdentity = ChunkMissingMaterialListCache.identitySignature(placement);
            this.schematicPath = ChunkMissingMaterialListCache.schematicPath(placement);
            this.schematicName = ChunkMissingMaterialListCache.schematicName(this.schematicPath);
            this.originPosition = ChunkMissingMaterialListCache.originPosition(placement);
            if (this.dimension == null && currentDimension != null) {
                this.dimension = currentDimension;
                LOGGER.info("[LMLP material-list] placement dimension learned reason={} name={} dimension={} schematic={} signature={}",
                        reason, this.name, this.dimension, this.schematicPath, this.signature);
            }

            PlacementKey oldKey = this.key;
            this.key = PlacementKey.of(placement);
            rekeyContext(this, oldKey);
        }

        private void updateMaterialCache(BlockInfoListType type, List<MaterialListEntry> entries) {
            this.materialListType = type;
            this.materialEntries = List.copyOf(WorldMaterialCacheIndex.captureEntries(entries));
            this.cacheGenerated = !this.materialEntries.isEmpty();
            this.lastMaterialCacheUpdateTime = System.currentTimeMillis();
            LOGGER.info("[LMLP cache-index] material cache updated key={} name={} sourceState={} type={} entries={}",
                    this.key, this.name, this.sourceState, this.materialListType.getStringValue(), this.materialEntries.size());
        }

        private void clearMaterialCache(String reason) {
            int beforeEntries = this.materialEntries.size();
            this.materialEntries = List.of();
            this.cacheGenerated = false;
            this.lastMaterialCacheUpdateTime = 0L;
            this.lastReason = reason;
            LOGGER.info("[LMLP cache-index] material cache cleared reason={} key={} name={} sourceState={} previousEntries={}",
                    reason, this.key, this.name, this.sourceState, beforeEntries);
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
        }

        private KnownPlacementContext view() {
            boolean inCurrentManager = this.placement != null && isPlacementInCurrentManager(this.placement);
            boolean canEdit = canEditPlacement(this, inCurrentManager);
            LOGGER.debug("[LMLP placement-list] row canModify key={} name={} sourceState={} placementDimension={} currentDimension={} placementRef={} inCurrentManager={} chunksLoaded={} result={}",
                    this.key, this.name, this.sourceState, this.dimension, currentDimensionId(), this.placement != null,
                    inCurrentManager, this.placement != null && arePlacementChunksLoaded(this.placement), canEdit);
            return new KnownPlacementContext(
                    this.placement,
                    this.key.value(),
                    this.sourceState,
                    this.name,
                    this.schematicPath,
                    this.schematicName,
                    this.originPosition,
                    this.dimension,
                    displayDimension(this.dimension),
                    this.lastSeen,
                    this.lastMaterialCacheUpdateTime,
                    this.selected,
                    this.cacheGenerated,
                    canEdit,
                    this.schematicMissing(),
                    this.hasMaterialCache(),
                    this.materialListType.getStringValue());
        }

        private PlacementRecord toRecord(String worldId) {
            return new PlacementRecord(
                    worldId,
                    this.key.value(),
                    this.dimension,
                    this.name,
                    this.schematicName,
                    this.schematicPath,
                    this.originPosition,
                    this.placementIdentity,
                    this.signature,
                    this.lastSeen,
                    this.lastMaterialCacheUpdateTime,
                    this.materialListType.getStringValue(),
                    this.cacheGenerated,
                    this.selected,
                    this.materialEntries);
        }

        private boolean canOpenMaterialList() {
            return this.placement != null || this.hasMaterialCache();
        }

        private boolean isOfflineCache() {
            return this.sourceState == SourceState.OFFLINE_CACHE || this.placement == null;
        }

        private boolean schematicMissing() {
            return this.schematicPath != null && !this.schematicPath.isEmpty() && !new File(this.schematicPath).isFile();
        }

        private boolean hasMaterialCache() {
            return !this.materialEntries.isEmpty();
        }

        private SchematicPlacement placement() {
            return this.placement;
        }

        private PlacementKey key() {
            return this.key;
        }

        private SourceState sourceState() {
            return this.sourceState;
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

        private boolean selected() {
            return this.selected;
        }

        private long lastSeen() {
            return this.lastSeen;
        }

        private BlockInfoListType materialListType() {
            return this.materialListType;
        }

        private List<EntryRecord> materialEntries() {
            return this.materialEntries;
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
