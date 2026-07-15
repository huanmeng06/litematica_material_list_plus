package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.render.GuiContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

// GuiBase.drawTitle() draws the title as a single unclamped drawString call
// at a fixed (20, 10): on the material list screen the title carries the
// schematic name plus "Design:"/"Edit:" metadata, which can run wide enough
// to be hidden behind the fixed info icon at (width-23, 10). Truncate it with
// "..." and always show the meaningful part (the quoted name Litematica's own
// title templates wrap it in, e.g. "litematica.gui.title.material_list.placement"
// = "放置材料列表 '%s'" — dropping the generic prefix) on hover, wrapped onto
// extra lines if it's long — the tooltip has no button row below it to
// collide with, unlike the title bar itself. Wrapping the title bar itself to
// a second line was tried first but that line collides with the top button
// row, whose Y is a vanilla constant with no awareness of title height;
// reflowing the whole button row for this would be a much larger, riskier
// change for a cosmetic edge case. Scoped to GuiMaterialList only via an
// instanceof guard: GuiBase is malilib's shared screen base used by every
// screen from every mod, and this mixin (registered once for the whole game)
// must not change unrelated screens' titles.
@Mixin(value = GuiBase.class, remap = false)
public abstract class GuiBaseTitleMixin {
    private static final int TITLE_HEIGHT = 9;
    private static final int TITLE_RIGHT_RESERVE = 27; // info icon sits at width-23, plus a small gap
    private static final int TOOLTIP_MAX_WIDTH = 300;

    private String lmlp$titleFullText;
    private int lmlp$titleDrawX;
    private int lmlp$titleDrawY;
    private int lmlp$titleDrawWidth;

    @Redirect(
            method = "drawTitle",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/gui/GuiBase;drawString(Lfi/dy/masa/malilib/render/GuiContext;Ljava/lang/String;III)V"))
    private void lmlp$truncateLongMaterialListTitle(GuiBase self, GuiContext context, String text, int x, int y, int color) {
        if (!(((Object) self) instanceof GuiMaterialList)) {
            self.drawString(context, text, x, y, color);
            return;
        }

        int budget = ((net.minecraft.client.gui.screens.Screen) (Object) self).width - x - TITLE_RIGHT_RESERVE;
        String shown = self.getStringWidth(text) <= budget ? text : lmlp$truncateToWidth(self, text, budget);

        this.lmlp$titleFullText = text;
        this.lmlp$titleDrawX = x;
        this.lmlp$titleDrawY = y;
        this.lmlp$titleDrawWidth = self.getStringWidth(shown);
        self.drawString(context, shown, x, y, color);
    }

    // Always available on hover (not just when the title was actually
    // truncated) so the full name is a hover away no matter the window width.
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void lmlp$renderTitleHoverTooltip(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (this.lmlp$titleFullText == null || !(((Object) this) instanceof GuiBase self)) {
            return;
        }

        if (!(mouseX >= this.lmlp$titleDrawX && mouseX <= this.lmlp$titleDrawX + this.lmlp$titleDrawWidth
                && mouseY >= this.lmlp$titleDrawY && mouseY <= this.lmlp$titleDrawY + TITLE_HEIGHT)) {
            return;
        }

        String name = lmlp$extractQuotedName(this.lmlp$titleFullText);
        GuiContext context = GuiContext.fromGuiGraphics(drawContext);
        RenderUtils.drawHoverText(context, mouseX, mouseY, lmlp$wrapTooltipText(self, name));
    }

    // Litematica's title templates all wrap the meaningful name in single
    // quotes (e.g. "放置材料列表 '%s'", "Material List for placement '%s'") —
    // pull just that part out so the tooltip skips the generic screen-title
    // prefix. Falls back to the raw title if no quotes are found.
    private static String lmlp$extractQuotedName(String title) {
        int start = title.indexOf('\'');
        if (start < 0) {
            return title;
        }

        int end = title.indexOf('\'', start + 1);
        return end < 0 ? title : title.substring(start + 1, end);
    }

    private static List<String> lmlp$wrapTooltipText(GuiBase self, String text) {
        if (self.getStringWidth(text) <= TOOLTIP_MAX_WIDTH) {
            return List.of(text);
        }

        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && self.getStringWidth(candidate) > TOOLTIP_MAX_WIDTH) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines.isEmpty() ? List.of(text) : lines;
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
