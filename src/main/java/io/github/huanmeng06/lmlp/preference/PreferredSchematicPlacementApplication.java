package io.github.huanmeng06.lmlp.preference;

import com.google.gson.JsonObject;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.util.position.PositionUtils.CoordinateType;
import net.minecraft.class_2487;

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

    /**
     * The placement NBT preserves origin, rotation, mirror and the relative position/rotation/
     * mirror/enabled/entity state of every sub-region. The remaining display and editor state is
     * copied explicitly below. The old hash is deliberately removed so the new placement has its
     * own identity.
     */
    private static SchematicPlacement copyPlacement(
            SchematicPlacement source,
            LitematicaSchematic savedSchematic) {
        class_2487 placementNbt = source.toNbt(false);
        placementNbt.method_10551("HashCode");
        SchematicPlacement replacement = SchematicPlacement.createFromNbt(savedSchematic, placementNbt);
        if (replacement == null) {
            return null;
        }

        replacement.setName(preferredPlacementName(source.getName()));
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
        Path savedFile = normalize(savedSchematic.getFile());
        SchematicHolder holder = SchematicHolder.getInstance();
        if (savedFile != null) {
            holder.getAllSchematics().stream()
                    .filter(loaded -> loaded != savedSchematic)
                    .filter(loaded -> savedFile.equals(normalize(loaded.getFile())))
                    .toList()
                    .forEach(holder::removeSchematic);
        }
        holder.addSchematic(savedSchematic, false);
    }

    public static LitematicaSchematic savedSchematic(Path destination, LitematicaSchematic copy) {
        LitematicaSchematic saved = new LitematicaSchematic(
                destination.toAbsolutePath().normalize(),
                copy.writeToNBT(),
                FileType.LITEMATICA_SCHEMATIC);
        saved.getMetadata().clearModifiedSinceSaved();
        return saved;
    }

    private static Path normalize(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }
}
