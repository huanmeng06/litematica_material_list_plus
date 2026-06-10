package io.github.huanmeng06.lmlp.mixin;

import com.google.gson.JsonObject;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SchematicPlacementManager.class, remap = false)
public abstract class SchematicPlacementManagerMixin {
    @Inject(method = "addSchematicPlacement", at = @At("TAIL"))
    private void lmlp$rememberAddedPlacement(SchematicPlacement placement, boolean printMessage, CallbackInfo ci) {
        ChunkMissingMaterialListCache.rememberPlacementContext(placement, "placement_manager.add");
    }

    @Inject(method = "setSelectedSchematicPlacement", at = @At("TAIL"))
    private void lmlp$rememberSelectedPlacement(SchematicPlacement placement, CallbackInfo ci) {
        if (placement != null) {
            ChunkMissingMaterialListCache.selectMaterialListPlacement(placement, "placement_manager.select");
        }
    }

    @Inject(method = "removeSchematicPlacement(Lfi/dy/masa/litematica/schematic/placement/SchematicPlacement;Z)Z", at = @At("TAIL"))
    private void lmlp$forgetRemovedPlacement(SchematicPlacement placement, boolean update, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ChunkMissingMaterialListCache.forgetPlacementContext(placement, "placement_manager.remove");
        }
    }

    @Inject(method = "loadFromJson", at = @At("TAIL"))
    private void lmlp$rememberLoadedPlacements(JsonObject obj, CallbackInfo ci) {
        ChunkMissingMaterialListCache.rememberCurrentPlacements("placement_manager.load");
    }
}
