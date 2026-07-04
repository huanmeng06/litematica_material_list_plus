package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// GuiBase.drawTitle() draws the title as a single unclamped drawString call
// at a fixed (20, 10): on the material list screen the title carries the
// schematic name plus "Design:"/"Edit:" metadata, which can run wide enough
// to be hidden behind the fixed info icon at (width-23, 10). Truncate it with
// "..." and show the full text on hover instead — the same treatment already
// used for overlong item names elsewhere in this mod (see
// WidgetMaterialListEntryMixin's truncateToWidth/postRenderHovered). Wrapping
// to a second line was tried first but that line collides with the top
// button row, whose Y is a vanilla constant with no awareness of title
// height; reflowing the whole button row for this would be a much larger,
// riskier change for a cosmetic edge case. Scoped to GuiMaterialList only via
// an instanceof guard: GuiBase is malilib's shared screen base used by every
// screen from every mod, and this mixin (registered once for the whole game)
// must not change unrelated screens' titles.
@Mixin(value = GuiBase.class, remap = false)
public abstract class GuiBaseTitleMixin {
    private static final int TITLE_HEIGHT = 9;
    private static final int TITLE_RIGHT_RESERVE = 27; // info icon sits at width-23, plus a small gap

    private boolean lmlp$titleTruncated;
    private String lmlp$titleFullText;
    private int lmlp$titleDrawX;
    private int lmlp$titleDrawY;
    private int lmlp$titleDrawWidth;

    @Redirect(
            method = "drawTitle",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/gui/GuiBase;drawString(Lnet/minecraft/class_332;Ljava/lang/String;III)V"))
    private void lmlp$truncateLongMaterialListTitle(GuiBase self, class_332 context, String text, int x, int y, int color) {
        if (!(((Object) self) instanceof GuiMaterialList)) {
            self.drawString(context, text, x, y, color);
            return;
        }

        int budget = ((net.minecraft.class_437) (Object) self).field_22789 - x - TITLE_RIGHT_RESERVE;
        if (self.getStringWidth(text) <= budget) {
            this.lmlp$titleTruncated = false;
            self.drawString(context, text, x, y, color);
            return;
        }

        String shown = lmlp$truncateToWidth(self, text, budget);
        this.lmlp$titleTruncated = true;
        this.lmlp$titleFullText = text;
        this.lmlp$titleDrawX = x;
        this.lmlp$titleDrawY = y;
        this.lmlp$titleDrawWidth = self.getStringWidth(shown);
        self.drawString(context, shown, x, y, color);
    }

    @Inject(method = "method_25394", at = @At("TAIL"))
    private void lmlp$renderTitleHoverTooltip(class_332 context, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!this.lmlp$titleTruncated || !(((Object) this) instanceof GuiMaterialList)) {
            return;
        }

        if (mouseX >= this.lmlp$titleDrawX && mouseX <= this.lmlp$titleDrawX + this.lmlp$titleDrawWidth
                && mouseY >= this.lmlp$titleDrawY && mouseY <= this.lmlp$titleDrawY + TITLE_HEIGHT) {
            RenderUtils.drawHoverText(mouseX, mouseY, List.of(this.lmlp$titleFullText), context);
        }
    }

    private static String lmlp$truncateToWidth(GuiBase self, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }

        if (self.getStringWidth(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int suffixWidth = self.getStringWidth(suffix);
        if (suffixWidth > maxWidth) {
            return "";
        }

        int end = text.length();
        while (end > 0 && self.getStringWidth(text.substring(0, end)) + suffixWidth > maxWidth) {
            end--;
        }

        return end > 0 ? text.substring(0, end) + suffix : suffix;
    }
}
