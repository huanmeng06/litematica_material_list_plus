package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicPlacements;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import io.github.huanmeng06.lmlp.access.WidgetListBoundsAccess;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.gui.KnownPlacementListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(value = WidgetListSchematicPlacements.class, remap = false)
public abstract class WidgetListSchematicPlacementsMixin {
    @Inject(method = "getAllEntries", at = @At("HEAD"), cancellable = true)
    private void lmlp$getKnownPlacementEntries(CallbackInfoReturnable<Collection<SchematicPlacement>> cir) {
        List<SchematicPlacement> placements = new ArrayList<>();
        for (KnownPlacementContext context : ChunkMissingMaterialListCache.knownPlacementContexts()) {
            if (context.placement() != null) {
                placements.add(context.placement());
            }
        }
        cir.setReturnValue(placements);
    }

    @Inject(method = "getEntryStringsForFilter(Lfi/dy/masa/litematica/schematic/placement/SchematicPlacement;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
    private void lmlp$getKnownPlacementFilterStrings(SchematicPlacement placement, CallbackInfoReturnable<List<String>> cir) {
        KnownPlacementContext context = ChunkMissingMaterialListCache.knownContextFor(placement);
        List<String> strings = new ArrayList<>();
        strings.add(placement == null ? "" : placement.getName().toLowerCase());
        if (context != null) {
            strings.add(context.displayDimension().toLowerCase());
            strings.add(context.dimension() == null ? "" : context.dimension().toLowerCase());
            strings.add(context.schematicPath().toLowerCase());
        }
        cir.setReturnValue(strings);
    }

    @Inject(method = "createListEntryWidget(IIIZLfi/dy/masa/litematica/schematic/placement/SchematicPlacement;)Lfi/dy/masa/litematica/gui/widgets/WidgetSchematicPlacement;", at = @At("HEAD"), cancellable = true)
    private void lmlp$createKnownPlacementEntry(
            int x,
            int y,
            int listIndex,
            boolean isOdd,
            SchematicPlacement placement,
            CallbackInfoReturnable<WidgetSchematicPlacement> cir) {
        int width = ((WidgetListBoundsAccess) (Object) this).lmlp$getEntryWidth();
        cir.setReturnValue(new KnownPlacementListEntry(
                x,
                y,
                width,
                isOdd,
                placement,
                listIndex,
                (WidgetListSchematicPlacements) (Object) this));
    }
}