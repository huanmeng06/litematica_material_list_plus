package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import io.github.huanmeng06.lmlp.gui.IgnoredMaterialRegistry;
import io.github.huanmeng06.lmlp.gui.MaterialListPlusState;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(targets = "fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry$ButtonListener", remap = false)
public abstract class WidgetMaterialListEntryButtonListenerMixin {
    @Inject(method = "actionPerformedWithButton", at = @At("HEAD"), cancellable = true)
    private void lmlp$handleStableMaterialIgnore(ButtonBase button, int mouseButton, CallbackInfo ci) {
        MaterialListBase materialList = this.lmlp$getField("materialList", MaterialListBase.class);
        MaterialListEntry entry = this.lmlp$getField("entry", MaterialListEntry.class);
        WidgetListMaterialList listWidget = this.lmlp$getField("listWidget", WidgetListMaterialList.class);

        if (materialList == null
                || entry == null
                || listWidget == null
                || !this.lmlp$isIgnoreButton()) {
            return;
        }

        if (MinimalSubMaterialListView.isActive(materialList)) {
            if (MinimalSubMaterialListView.isMinimalEntry(entry)) {
                MinimalSubMaterialListView.ignoreEntry(materialList, entry, "original-button-listener", true);
                MaterialListPlusState.clear();
                listWidget.refreshEntries();
                ci.cancel();
            }
            return;
        }

        IgnoredMaterialRegistry.ignore(materialList, entry);
    }

    private boolean lmlp$isIgnoreButton() {
        Object value = this.lmlp$getRawField("type");
        return value instanceof Enum<?> type && "IGNORE".equals(type.name());
    }

    private <T> T lmlp$getField(String name, Class<T> type) {
        Object value = this.lmlp$getRawField(name);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    private Object lmlp$getRawField(String name) {
        Class<?> type = this.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(this);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        return null;
    }
}
