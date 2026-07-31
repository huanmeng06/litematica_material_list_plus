package io.github.huanmeng06.lmlp.cache;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.cache.WorldMaterialCacheIndex.PlacementRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class NativePlacementStorageIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(LitematicaMaterialListPlus.MOD_ID);
    private static final String FILE_PREFIX = "litematica_";
    private static final String FILE_SUFFIX = ".json";
    private static final String DIMENSION_SEPARATOR = "_dim_";

    private NativePlacementStorageIndex() {
    }

    static Snapshot load(String currentDimension, String reason) {
        if (currentDimension == null || currentDimension.isBlank()) {
            return Snapshot.unavailable();
        }

        Path configDirectory = DataManager.getCurrentConfigDirectory();
        String currentFileName = StringUtils.getStorageFileName(false, FILE_PREFIX, FILE_SUFFIX, "default");
        String currentSuffix = DIMENSION_SEPARATOR + safeDimension(currentDimension) + FILE_SUFFIX;
        if (configDirectory == null || !currentFileName.endsWith(currentSuffix)) {
            LOGGER.warn("[LMLP native-placement] index unavailable reason={} currentDimension={} currentFile={}",
                    reason, currentDimension, currentFileName);
            return Snapshot.unavailable();
        }

        String worldPrefix = currentFileName.substring(0, currentFileName.length() - currentSuffix.length());
        Map<String, Map<PlacementIdentity, JsonObject>> placementsByFile = new HashMap<>();
        try (Stream<Path> files = Files.list(configDirectory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> isDimensionFile(path.getFileName().toString(), worldPrefix))
                    .forEach(path -> placementsByFile.put(path.getFileName().toString(), readPlacements(path)));
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[LMLP native-placement] index read failed reason={} directory={}",
                    reason, configDirectory, exception);
            return Snapshot.unavailable();
        }

        LOGGER.info("[LMLP native-placement] index loaded reason={} currentDimension={} worldPrefix={} dimensionFiles={} placements={}",
                reason,
                currentDimension,
                worldPrefix,
                placementsByFile.size(),
                placementsByFile.values().stream().mapToInt(Map::size).sum());
        return new Snapshot(worldPrefix, currentDimension, Map.copyOf(placementsByFile), true);
    }

    private static boolean isDimensionFile(String fileName, String worldPrefix) {
        return fileName.startsWith(worldPrefix + DIMENSION_SEPARATOR) && fileName.endsWith(FILE_SUFFIX);
    }

    private static Map<PlacementIdentity, JsonObject> readPlacements(Path path) {
        try {
            JsonElement rootElement = JsonUtils.parseJsonFileAsPath(path);
            if (rootElement == null || !rootElement.isJsonObject()) {
                return Map.of();
            }

            JsonObject root = rootElement.getAsJsonObject();
            if (!JsonUtils.hasObject(root, "placements")) {
                return Map.of();
            }

            JsonObject placementsObject = root.getAsJsonObject("placements");
            if (!JsonUtils.hasArray(placementsObject, "placements")) {
                return Map.of();
            }

            Map<PlacementIdentity, JsonObject> placements = new HashMap<>();
            for (JsonElement element : placementsObject.getAsJsonArray("placements")) {
                if (element.isJsonObject()) {
                    JsonObject placementObject = element.getAsJsonObject();
                    PlacementIdentity identity = PlacementIdentity.fromJson(placementObject);
                    if (identity != null) {
                        placements.put(identity, placementObject.deepCopy());
                    }
                }
            }
            return Map.copyOf(placements);
        } catch (RuntimeException exception) {
            LOGGER.warn("[LMLP native-placement] placement file read failed path={}", path, exception);
            return Map.of();
        }
    }

    private static String safeDimension(String dimension) {
        return FileNameUtils.generateSafeFileName(dimension.replace(':', '_'));
    }

    record Snapshot(
            String worldPrefix,
            String currentDimension,
            Map<String, Map<PlacementIdentity, JsonObject>> placementsByFile,
            boolean available) {
        private static Snapshot unavailable() {
            return new Snapshot("", "", Map.of(), false);
        }

        boolean contains(PlacementRecord record) {
            if (!this.available || record == null || record.dimension() == null || record.dimension().isBlank()) {
                return false;
            }

            String fileName = this.worldPrefix + DIMENSION_SEPARATOR + safeDimension(record.dimension()) + FILE_SUFFIX;
            Map<PlacementIdentity, JsonObject> placements = this.placementsByFile.get(fileName);
            return placements != null && placements.containsKey(PlacementIdentity.fromRecord(record));
        }

        SchematicPlacement restorePlacement(PlacementRecord record) {
            if (!this.available || record == null || record.dimension() == null || record.dimension().isBlank()) {
                return null;
            }

            String fileName = this.worldPrefix + DIMENSION_SEPARATOR + safeDimension(record.dimension()) + FILE_SUFFIX;
            Map<PlacementIdentity, JsonObject> placements = this.placementsByFile.get(fileName);
            JsonObject placementObject = placements == null ? null : placements.get(PlacementIdentity.fromRecord(record));
            if (placementObject == null) {
                return null;
            }

            try {
                return SchematicPlacement.fromJson(placementObject.deepCopy());
            } catch (RuntimeException exception) {
                LOGGER.warn("[LMLP native-placement] placement restore failed dimension={} name={} schematic={}",
                        record.dimension(), record.placementName(), record.schematicPath(), exception);
                return null;
            }
        }

        boolean isOtherDimension(PlacementRecord record) {
            return record != null
                    && record.dimension() != null
                    && !ChunkMissingMaterialListCache.normalizedDimension(record.dimension())
                    .equals(ChunkMissingMaterialListCache.normalizedDimension(this.currentDimension));
        }
    }

    private record PlacementIdentity(String schematicPath, String name, String placementIdentity) {
        private static PlacementIdentity fromRecord(PlacementRecord record) {
            return new PlacementIdentity(normalizeSchematicPath(record.schematicPath()), record.placementName(), record.placementIdentity());
        }

        private static PlacementIdentity fromJson(JsonObject object) {
            String schematicPath = JsonUtils.getStringOrDefault(object, "schematic", "");
            String name = JsonUtils.getStringOrDefault(object, "name", "");
            String identity = identitySignature(object);
            if (schematicPath.isBlank() || name.isBlank() || identity == null) {
                return null;
            }
            return new PlacementIdentity(normalizeSchematicPath(schematicPath), name, identity);
        }

        private static String identitySignature(JsonObject object) {
            String origin = position(object.get("origin"));
            if (origin == null) {
                return null;
            }

            StringBuilder builder = new StringBuilder(origin);
            builder.append('|').append(JsonUtils.getStringOrDefault(object, "rotation", "NONE"));
            builder.append('|').append(JsonUtils.getStringOrDefault(object, "mirror", "NONE")).append('|');

            List<RegionIdentity> regions = new ArrayList<>();
            if (JsonUtils.hasArray(object, "placements")) {
                for (JsonElement element : object.getAsJsonArray("placements")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject regionObject = element.getAsJsonObject();
                    if (!JsonUtils.hasObject(regionObject, "placement")) {
                        continue;
                    }
                    JsonObject placementObject = regionObject.getAsJsonObject("placement");
                    String position = position(placementObject.get("pos"));
                    if (position == null) {
                        return null;
                    }
                    regions.add(new RegionIdentity(
                            JsonUtils.getStringOrDefault(regionObject, "name", ""),
                            position,
                            JsonUtils.getStringOrDefault(placementObject, "rotation", "NONE"),
                            JsonUtils.getStringOrDefault(placementObject, "mirror", "NONE")));
                }
            }

            regions.sort(Comparator.comparing(RegionIdentity::name));
            for (RegionIdentity region : regions) {
                builder.append(region.name()).append(':')
                        .append(region.position()).append(':')
                        .append(region.rotation()).append(':')
                        .append(region.mirror()).append(';');
            }
            return builder.toString();
        }

        private static String position(JsonElement element) {
            if (element == null || !element.isJsonArray()) {
                return null;
            }
            JsonArray array = element.getAsJsonArray();
            if (array.size() != 3) {
                return null;
            }
            return array.get(0).getAsInt() + "," + array.get(1).getAsInt() + "," + array.get(2).getAsInt();
        }

        private static String normalizeSchematicPath(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            try {
                Path path = Path.of(value);
                if (!path.isAbsolute()) {
                    path = DataManager.getSchematicsBaseDirectory().resolve(path);
                }
                return path.toAbsolutePath().normalize().toString();
            } catch (RuntimeException exception) {
                return value;
            }
        }
    }

    private record RegionIdentity(String name, String position, String rotation, String mirror) {
    }
}
