package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.gui.IgnoredMaterialRegistry;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(targets = "fi.dy.masa.litematica.gui.GuiMaterialList$ButtonListener", remap = false)
public abstract class GuiMaterialListButtonListenerMixin {
    @Inject(method = "actionPerformedWithButton", at = @At("HEAD"))
    private void lmlp$clearMinimalIgnoredOnClearIgnoredButton(ButtonBase button, int mouseButton, CallbackInfo ci) {
        if (!this.lmlp$isClearIgnoredButton()) {
            return;
        }

        MaterialListBase materialList = this.lmlp$materialListFromParent();
        if (materialList != null) {
            if (MinimalSubMaterialListView.isActive(materialList)) {
                MinimalSubMaterialListView.clearIgnored(materialList);
            } else {
                IgnoredMaterialRegistry.clearIgnored(materialList);
            }
        }
    }

    @Redirect(
            method = "actionPerformedWithButton",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/materials/MaterialListBase;setMaterialListType(Lfi/dy/masa/litematica/util/BlockInfoListType;)V"))
    private void lmlp$cycleMaterialListDisplayType(MaterialListBase materialList, BlockInfoListType ignoredType, ButtonBase button, int mouseButton) {
        MinimalSubMaterialListView.cycle(materialList, mouseButton == 0);
    }

    @Redirect(
            method = "actionPerformedWithButton",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/materials/MaterialListBase;reCreateMaterialList()V"))
    private void lmlp$refreshChunkMissingMaterialList(MaterialListBase materialList) {
        if (ChunkMissingMaterialListCache.refreshForCurrentState(materialList, true)) {
            return;
        }

        materialList.reCreateMaterialList();
    }

    private boolean lmlp$isClearIgnoredButton() {
        try {
            Field field = this.getClass().getDeclaredField("type");
            field.setAccessible(true);
            Object value = field.get(this);
            return value instanceof Enum<?> type && "CLEAR_IGNORED".equals(type.name());
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private MaterialListBase lmlp$materialListFromParent() {
        Object parent = this.lmlp$getFieldValue("parent");
        if (parent == null) {
            return null;
        }

        Object materialList = this.lmlp$getFieldValue(parent, "materialList");
        return materialList instanceof MaterialListBase list ? list : null;
    }

    private Object lmlp$getFieldValue(String name) {
        return this.lmlp$getFieldValue(this, name);
    }

    private Object lmlp$getFieldValue(Object owner, String name) {
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        return null;
    }
}
