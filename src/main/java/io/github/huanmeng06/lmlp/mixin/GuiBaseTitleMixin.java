package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

// GuiBase.drawTitle() draws the title as a single unclamped drawString call
// at a fixed (20, 10): on the material list screen the title carries the
// schematic name plus "Design:"/"Edit:" metadata, which can run wide enough
// to be hidden behind the fixed info icon at (width-23, 10). Word-wrap it
// onto extra lines instead, the same way the bottom progress summary is
// wrapped. Scoped to GuiMaterialList only via an instanceof guard: GuiBase
// is malilib's shared screen base used by every screen from every mod, and
// this mixin (registered once for the whole game) must not change unrelated
// screens' titles.
@Mixin(value = GuiBase.class, remap = false)
public abstract class GuiBaseTitleMixin {
    private static final int TITLE_LINE_HEIGHT = 9;
    private static final int TITLE_RIGHT_RESERVE = 27; // info icon sits at width-23, plus a small gap

    @Redirect(
            method = "drawTitle",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/gui/GuiBase;drawString(Lnet/minecraft/class_332;Ljava/lang/String;III)V"))
    private void lmlp$wrapLongMaterialListTitle(GuiBase self, class_332 context, String text, int x, int y, int color) {
        if (!(((Object) self) instanceof GuiMaterialList)) {
            self.drawString(context, text, x, y, color);
            return;
        }

        int budget = ((net.minecraft.class_437) (Object) self).field_22789 - x - TITLE_RIGHT_RESERVE;
        if (self.getStringWidth(text) <= budget) {
            self.drawString(context, text, x, y, color);
            return;
        }

        List<String> lines = lmlp$wrapByWord(self, text, budget);
        for (int i = 0; i < lines.size(); i++) {
            self.drawString(context, lines.get(i), x, y + TITLE_LINE_HEIGHT * i, color);
        }
    }

    private static List<String> lmlp$wrapByWord(GuiBase self, String text, int maxWidth) {
        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && self.getStringWidth(candidate) > maxWidth) {
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
}
