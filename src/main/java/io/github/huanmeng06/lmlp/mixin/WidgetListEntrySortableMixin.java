package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.malilib.gui.widgets.WidgetListEntrySortable;
import fi.dy.masa.malilib.render.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WidgetListEntrySortable.class, remap = false)
@SuppressWarnings("rawtypes")
public abstract class WidgetListEntrySortableMixin {
    // Columns collapsed onto a neighbor (from priority-based column hiding on
    // narrow windows, used by both the material list and the schematic
    // placements screens) share the same X as that neighbor, so this box's
    // logical width here is zero or negative. drawOutline still renders a
    // thin sliver for it regardless (its four border rects use the raw
    // width), so skip drawing when there's no real column left to outline.
    @Redirect(
            method = "renderColumnHeader",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/render/RenderUtils;drawOutline(IIIII)V"))
    private void lmlp$skipDegenerateColumnOutline(int x, int y, int width, int height, int color) {
        if (width <= 0) {
            return;
        }

        RenderUtils.drawOutline(x, y, width, height, color);
    }
}
