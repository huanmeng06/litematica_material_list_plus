package io.github.huanmeng06.lmlp.cache;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.util.JsonUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class WorldMaterialCacheIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(LitematicaMaterialListPlus.MOD_ID);
    private static final int CACHE_FORMAT_VERSION = 1;
    private static final String LEVEL_STORAGE_SESSION_FIELD = "storageSource";
    private static final String LEVEL_STORAGE_SESSION_NAME_METHOD = "getLevelId";

    private WorldMaterialCacheIndex() {
    }

    public static String resolveWorldId(Minecraft client) {
        if (client == null) {
            client = Minecraft.getInstance();
        }

        if (client == null) {
            return null;
        }

        IntegratedServer server = client.getSingleplayerServer();
        if (server != null) {
            return resolveLocalWorldId(client, server);
        }

        ServerData serverInfo = client.getCurrentServer();
        if (serverInfo != null) {
            String address = serverInfo.ip == null ? "" : serverInfo.ip;
            String name = serverInfo.name == null ? "" : serverInfo.name;
            return "server:" + address + "|" + name;
        }

        return null;
    }

    private static String resolveLocalWorldId(Minecraft client, IntegratedServer server) {
        try {
            Field sessionField = MinecraftServer.class.getDeclaredField(LEVEL_STORAGE_SESSION_FIELD);
            sessionField.setAccessible(true);
            Object session = sessionField.get(server);
            if (session == null) {
                LOGGER.warn("[LMLP cache-index] local world id skipped reason=missing_level_storage_session");
                return null;
            }

            Method saveNameMethod = session.getClass().getDeclaredMethod(LEVEL_STORAGE_SESSION_NAME_METHOD);
            saveNameMethod.setAccessible(true);
            Object saveNameValue = saveNameMethod.invoke(session);
            if (!(saveNameValue instanceof String saveName) || saveName.isBlank()) {
                LOGGER.warn("[LMLP cache-index] local world id skipped reason=missing_save_name value={}", saveNameValue);
                return null;
            }

            File saveDir = new File(new File(client.gameDirectory, "saves"), saveName);
            return "local:" + canonicalPath(saveDir);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("[LMLP cache-index] local world id skipped reason=reflection_failed", exception);
            return null;
        }
    }

    public static File fileFor(String worldId) {
        Path dir = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(LitematicaMaterialListPlus.MOD_ID)
                .resolve("world-cache");
        String scope = worldId == null || !worldId.contains(":") ? "world" : worldId.substring(0, worldId.indexOf(':'));
        return dir.resolve(scope + "-" + sha1(worldId == null ? "unknown" : worldId) + ".json").toFile();
    }

    public static LoadResult load(String worldId) {
        File file = fileFor(worldId);
        if (!file.isFile()) {
            LOGGER.info("[LMLP cache-index] no existing cache index worldId={} path={}", worldId, file.getAbsolutePath());
            return new LoadResult(file, List.of(), null);
        }

        try {
            JsonElement element = JsonUtils.parseJsonFile(file.toPath());
            if (element == null || !element.isJsonObject()) {
                LOGGER.warn("[LMLP cache-index] cache read failed reason=not_json_object worldId={} path={}", worldId, file.getAbsolutePath());
                return new LoadResult(file, List.of(), null);
            }

            JsonObject root = element.getAsJsonObject();
            int version = JsonUtils.getIntegerOrDefault(root, "cacheFormatVersion", 0);
            if (version != CACHE_FORMAT_VERSION) {
                LOGGER.warn("[LMLP cache-index] cache read skipped reason=incompatible_version expected={} actual={} worldId={} path={}",
                        CACHE_FORMAT_VERSION, version, worldId, file.getAbsolutePath());
                return new LoadResult(file, List.of(), null);
            }

            String fileWorldId = JsonUtils.getStringOrDefault(root, "worldId", "");
            if (!worldId.equals(fileWorldId)) {
                LOGGER.warn("[LMLP cache-index] cache read skipped reason=world_mismatch expected={} actual={} path={}",
                        worldId, fileWorldId, file.getAbsolutePath());
                return new LoadResult(file, List.of(), null);
            }

            List<PlacementRecord> records = new ArrayList<>();
            if (JsonUtils.hasArray(root, "placements")) {
                for (JsonElement child : root.getAsJsonArray("placements")) {
                    if (child.isJsonObject()) {
                        PlacementRecord record = PlacementRecord.fromJson(child.getAsJsonObject(), worldId);
                        if (record != null) {
                            records.add(record);
                        }
                    }
                }
            }

            String selectedKey = JsonUtils.getStringOrDefault(root, "selectedKey", null);
            LOGGER.info("[LMLP cache-index] cache read ok worldId={} path={} restoredContexts={}",
                    worldId, file.getAbsolutePath(), records.size());
            return new LoadResult(file, records, selectedKey);
        } catch (RuntimeException exception) {
            LOGGER.warn("[LMLP cache-index] cache read failed reason=exception worldId={} path={}",
                    worldId, file.getAbsolutePath(), exception);
            return new LoadResult(file, List.of(), null);
        }
    }

    public static void save(String worldId, List<PlacementRecord> records, String selectedKey) {
        if (worldId == null || worldId.isEmpty()) {
            LOGGER.warn("[LMLP cache-index] cache write skipped reason=missing_world_id entries={}", records.size());
            return;
        }

        File file = fileFor(worldId);
        JsonObject root = new JsonObject();
        root.addProperty("cacheFormatVersion", CACHE_FORMAT_VERSION);
        root.addProperty("modVersion", LitematicaMaterialListPlus.MOD_VERSION);
        root.addProperty("worldId", worldId);
        root.addProperty("updatedAt", System.currentTimeMillis());
        if (selectedKey != null) {
            root.addProperty("selectedKey", selectedKey);
        }

        JsonArray placements = new JsonArray();
        for (PlacementRecord record : records) {
            placements.add(record.toJson());
        }
        root.add("placements", placements);

        try {
            Files.createDirectories(file.toPath().getParent());
            JsonUtils.writeJsonToFile(root, file.toPath());
            LOGGER.info("[LMLP cache-index] cache write path={} entries={}", file.getAbsolutePath(), records.size());
        } catch (Exception exception) {
            LOGGER.warn("[LMLP cache-index] cache write failed path={} entries={}",
                    file.getAbsolutePath(), records.size(), exception);
        }
    }

    public static List<EntryRecord> captureEntries(List<MaterialListEntry> entries) {
        List<EntryRecord> records = new ArrayList<>();
        for (MaterialListEntry entry : entries) {
            EntryRecord record = EntryRecord.fromEntry(entry);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    public record LoadResult(File file, List<PlacementRecord> records, String selectedKey) {
    }

    public record PlacementRecord(
            String worldId,
            String key,
            String dimension,
            String placementName,
            String schematicName,
            String schematicPath,
            String originPosition,
            String placementIdentity,
            String placementSignature,
            long lastSeenTime,
            long lastMaterialCacheUpdateTime,
            String materialListType,
            boolean cacheGenerated,
            boolean selected,
            List<EntryRecord> entries) {
        private static PlacementRecord fromJson(JsonObject object, String expectedWorldId) {
            String key = JsonUtils.getStringOrDefault(object, "key", "");
            if (key.isEmpty()) {
                LOGGER.warn("[LMLP cache-index] placement skipped reason=missing_key worldId={}", expectedWorldId);
                return null;
            }

            List<EntryRecord> entries = new ArrayList<>();
            if (JsonUtils.hasArray(object, "entries")) {
                for (JsonElement child : object.getAsJsonArray("entries")) {
                    if (child.isJsonObject()) {
                        EntryRecord entry = EntryRecord.fromJson(child.getAsJsonObject());
                        if (entry != null) {
                            entries.add(entry);
                        }
                    }
                }
            }

            return new PlacementRecord(
                    JsonUtils.getStringOrDefault(object, "worldId", expectedWorldId),
                    key,
                    JsonUtils.getStringOrDefault(object, "dimension", null),
                    JsonUtils.getStringOrDefault(object, "placementName", key),
                    JsonUtils.getStringOrDefault(object, "schematicName", ""),
                    JsonUtils.getStringOrDefault(object, "schematicPath", ""),
                    JsonUtils.getStringOrDefault(object, "originPosition", ""),
                    JsonUtils.getStringOrDefault(object, "placementIdentity", ""),
                    JsonUtils.getStringOrDefault(object, "placementSignature", ""),
                    JsonUtils.getLongOrDefault(object, "lastSeenTime", 0L),
                    JsonUtils.getLongOrDefault(object, "lastMaterialCacheUpdateTime", 0L),
                    JsonUtils.getStringOrDefault(object, "materialListType", BlockInfoListType.ALL.getStringValue()),
                    JsonUtils.getBooleanOrDefault(object, "cacheGenerated", !entries.isEmpty()),
                    JsonUtils.getBooleanOrDefault(object, "selected", false),
                    List.copyOf(entries));
        }

        private JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("worldId", this.worldId);
            object.addProperty("key", this.key);
            object.addProperty("dimension", this.dimension);
            object.addProperty("placementName", this.placementName);
            object.addProperty("schematicName", this.schematicName);
            object.addProperty("schematicPath", this.schematicPath);
            object.addProperty("originPosition", this.originPosition);
            object.addProperty("placementIdentity", this.placementIdentity);
            object.addProperty("placementSignature", this.placementSignature);
            object.addProperty("lastSeenTime", this.lastSeenTime);
            object.addProperty("lastMaterialCacheUpdateTime", this.lastMaterialCacheUpdateTime);
            object.addProperty("materialListType", this.materialListType);
            object.addProperty("cacheGenerated", this.cacheGenerated);
            object.addProperty("selected", this.selected);

            JsonArray entries = new JsonArray();
            for (EntryRecord entry : this.entries) {
                entries.add(entry.toJson());
            }
            object.add("entries", entries);
            return object;
        }
    }

    public record EntryRecord(
            String itemId,
            int stackCount,
            int countTotal,
            int countMissing,
            int countMismatched,
            int countAvailable) {
        private static EntryRecord fromEntry(MaterialListEntry entry) {
            ItemStack stack = entry.getStack();
            if (stack == null || stack.isEmpty()) {
                return null;
            }

            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId == null) {
                return null;
            }

            return new EntryRecord(
                    itemId.toString(),
                    Math.max(1, stack.getCount()),
                    entry.getCountTotal(),
                    entry.getCountMissing(),
                    entry.getCountMismatched(),
                    entry.getCountAvailable());
        }

        private static EntryRecord fromJson(JsonObject object) {
            String itemId = JsonUtils.getStringOrDefault(object, "itemId", "");
            if (itemId.isEmpty()) {
                return null;
            }

            return new EntryRecord(
                    itemId,
                    JsonUtils.getIntegerOrDefault(object, "stackCount", 1),
                    JsonUtils.getIntegerOrDefault(object, "countTotal", 0),
                    JsonUtils.getIntegerOrDefault(object, "countMissing", 0),
                    JsonUtils.getIntegerOrDefault(object, "countMismatched", 0),
                    JsonUtils.getIntegerOrDefault(object, "countAvailable", 0));
        }

        private JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("itemId", this.itemId);
            object.addProperty("stackCount", this.stackCount);
            object.addProperty("countTotal", this.countTotal);
            object.addProperty("countMissing", this.countMissing);
            object.addProperty("countMismatched", this.countMismatched);
            object.addProperty("countAvailable", this.countAvailable);
            return object;
        }

        public MaterialListEntry toEntry() {
            try {
                Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(this.itemId)).orElse(null);
                if (item == null) {
                    LOGGER.warn("[LMLP cache-index] material entry skipped reason=unknown_item itemId={}", this.itemId);
                    return null;
                }

                ItemStack stack = new ItemStack(item, Math.max(1, this.stackCount));
                return new MaterialListEntry(stack, this.countTotal, this.countMissing, this.countMismatched, this.countAvailable);
            } catch (RuntimeException exception) {
                LOGGER.warn("[LMLP cache-index] material entry skipped reason=invalid_item itemId={}", this.itemId, exception);
                return null;
            }
        }
    }

    private static String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Exception ignored) {
            return file.getAbsolutePath();
        }
    }

    private static String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
