package io.github.huanmeng06.lmlp.preference;

import com.google.gson.JsonObject;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.malilib.util.PositionUtils.CoordinateType;

import java.nio.file.Path;

/** Applies a saved preferred schematic using an exact copy of an existing placement's state. */
public final class PreferredSchematicPlacementApplication {
    private PreferredSchematicPlacementApplication() {
    }

    public static boolean apply(SchematicPlacement source, LitematicaSchematic savedSchematic) {
        if (source == null || savedSchematic == null) {
            return false;
        }

        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        if (manager == null || !manager.getAllSchematicsPlacements().contains(source)) {
            return false;
        }

        SchematicPlacement replacement = copyPlacement(source, savedSchematic);
        if (replacement == null) {
            return false;
        }

        replaceLoadedSchematic(savedSchematic);
        manager.addSchematicPlacement(replacement, false);
        source.setEnabled(false);
        manager.setSelectedSchematicPlacement(replacement);
        return true;
    }

    /** Copies every placement and sub-region state through the legacy public placement API. */
    private static SchematicPlacement copyPlacement(
            SchematicPlacement source,
            LitematicaSchematic savedSchematic) {
        SchematicPlacement replacement = SchematicPlacement.createFor(
                savedSchematic,
                source.getOrigin(),
                preferredPlacementName(source.getName()),
                source.isEnabled(),
                source.shouldBeSaved());
        if (replacement == null) {
            return null;
        }

        replacement.setRotation(source.getRotation(), null);
        replacement.setMirror(source.getMirror(), null);
        replacement.setEnabled(source.isEnabled());
        replacement.setRenderSchematic(source.isRenderingEnabled());
        replacement.setShouldBeSaved(source.shouldBeSaved());
        replacement.setSchematicVerifierType(source.getSchematicVerifierType());
        replacement.setSelectedSubRegionName(source.getSelectedSubRegionName());

        if (replacement.ignoreEntities() != source.ignoreEntities()) {
            replacement.toggleIgnoreEntities(null);
        }
        if (replacement.shouldRenderEnclosingBox() != source.shouldRenderEnclosingBox()) {
            replacement.toggleRenderEnclosingBox();
        }

        for (CoordinateType coordinate : CoordinateType.values()) {
            replacement.setCoordinateLocked(coordinate, source.isCoordinateLocked(coordinate));
        }

        JsonObject sourceJson = source.toJson();
        if (sourceJson != null && sourceJson.has("bb_color")) {
            replacement.setBoxesBBColor(sourceJson.get("bb_color").getAsInt());
        }

        for (SubRegionPlacement sourceRegion : source.getAllSubRegionsPlacements()) {
            SubRegionPlacement replacementRegion = replacement.getRelativeSubRegionPlacement(sourceRegion.getName());
            if (replacementRegion == null) {
                return null;
            }
            replacement.moveSubRegionTo(sourceRegion.getName(), sourceRegion.getPos(), null);
            replacement.setSubRegionRotation(sourceRegion.getName(), sourceRegion.getRotation(), null);
            replacement.setSubRegionMirror(sourceRegion.getName(), sourceRegion.getMirror(), null);
            if (replacementRegion.isEnabled() != sourceRegion.isEnabled()) {
                replacement.toggleSubRegionEnabled(sourceRegion.getName(), null);
            }
            if (replacementRegion.ignoreEntities() != sourceRegion.ignoreEntities()) {
                replacement.toggleSubRegionIgnoreEntities(sourceRegion.getName(), null);
            }
            replacementRegion.setRenderingEnabled(sourceRegion.isRenderingEnabled());
            for (CoordinateType coordinate : CoordinateType.values()) {
                replacementRegion.setCoordinateLocked(coordinate, sourceRegion.isCoordinateLocked(coordinate));
            }
        }

        if (source.isLocked() != replacement.isLocked()) {
            replacement.toggleLocked();
        }
        return replacement;
    }

    private static String preferredPlacementName(String sourceName) {
        String name = sourceName == null ? "" : sourceName;
        return name.endsWith("_preferred") ? name : name + "_preferred";
    }

    private static void replaceLoadedSchematic(LitematicaSchematic savedSchematic) {
        Path savedFile = normalize(savedSchematic.getFile() == null ? null : savedSchematic.getFile().toPath());
        SchematicHolder holder = SchematicHolder.getInstance();
        if (savedFile != null) {
            holder.getAllSchematics().stream()
                    .filter(loaded -> loaded != savedSchematic)
                    .filter(loaded -> savedFile.equals(normalize(
                            loaded.getFile() == null ? null : loaded.getFile().toPath())))
                    .toList()
                    .forEach(holder::removeSchematic);
        }
        holder.addSchematic(savedSchematic, false);
    }

    public static LitematicaSchematic savedSchematic(Path destination, LitematicaSchematic copy) {
        Path normalized = destination.toAbsolutePath().normalize();
        LitematicaSchematic saved = LitematicaSchematic.createFromFile(
                normalized.getParent().toFile(),
                normalized.getFileName().toString());
        if (saved == null) {
            throw new IllegalStateException("Failed to load saved preferred schematic");
        }
        saved.getMetadata().clearModifiedSinceSaved();
        return saved;
    }

    private static Path normalize(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }
}
