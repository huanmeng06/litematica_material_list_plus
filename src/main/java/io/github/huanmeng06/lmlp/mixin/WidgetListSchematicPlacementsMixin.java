package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicPlacements;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import io.github.huanmeng06.lmlp.access.WidgetListBoundsAccess;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache.KnownPlacementContext;
import io.github.huanmeng06.lmlp.gui.KnownPlacementListEntry;
import io.github.huanmeng06.lmlp.gui.KnownPlacementListRowEntry;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.KnownPlacementRow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(value = WidgetListSchematicPlacements.class, remap = false)
public abstract class WidgetListSchematicPlacementsMixin {
    private static final String PAGE_ID = "schematic_placements";

    @Inject(method = "getAllEntries", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void lmlp$getKnownPlacementEntries(CallbackInfoReturnable<Collection<SchematicPlacement>> cir) {
        cir.setReturnValue((Collection) KnownPlacementRows.rows(PAGE_ID));
    }

    @Inject(method = "getEntryStringsForFilter(Lfi/dy/masa/litematica/schematic/placement/SchematicPlacement;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
    private void lmlp$getKnownPlacementFilterStrings(SchematicPlacement placement, CallbackInfoReturnable<List<String>> cir) {
        KnownPlacementContext context = ChunkMissingMaterialListCache.knownContextFor(placement);
        List<String> strings = new ArrayList<>();
        strings.add(placement == null ? "" : placement.getName().toLowerCase());
        if (context != null) {
            strings.add(KnownPlacementRows.displayName(context.dimension()).toLowerCase());
            strings.add(KnownPlacementRows.normalizedDimension(context.dimension()).toLowerCase());
            strings.add(context.schematicPath().toLowerCase());
        }
        cir.setReturnValue(strings);
    }

    @Inject(method = "getEntryStringsForFilter(Ljava/lang/Object;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
    private void lmlp$getKnownPlacementRowFilterStrings(Object entry, CallbackInfoReturnable<List<String>> cir) {
        if (entry instanceof KnownPlacementRow row) {
            cir.setReturnValue(KnownPlacementRows.filterStrings(row));
        }
    }

    @Inject(
            method = "createListEntryWidget(IIIZLjava/lang/Object;)Lfi/dy/masa/malilib/gui/widgets/WidgetListEntryBase;",
            at = @At("HEAD"),
            cancellable = true)
    @SuppressWarnings("rawtypes")
    private void lmlp$createKnownPlacementRowEntry(
            int x,
            int y,
            int listIndex,
            boolean isOdd,
            Object entry,
            CallbackInfoReturnable<WidgetListEntryBase> cir) {
        if (entry instanceof KnownPlacementRow row) {
            int width = ((WidgetListBoundsAccess) (Object) this).lmlp$getEntryWidth();
            cir.setReturnValue(new KnownPlacementListRowEntry(
                    x,
                    y,
                    width,
                    isOdd,
                    row,
                    listIndex,
                    (WidgetListSchematicPlacements) (Object) this));
        }
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
