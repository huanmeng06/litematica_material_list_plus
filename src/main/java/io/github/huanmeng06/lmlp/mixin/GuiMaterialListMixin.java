package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListAreaAnalyzer;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetLabel;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.gui.GuiConfigs;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.ReadStatus;
import io.github.huanmeng06.lmlp.export.SubMaterialExporter;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import io.github.huanmeng06.lmlp.material.CountFormatter;
import net.minecraft.class_437;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(value = GuiMaterialList.class, remap = false)
public abstract class GuiMaterialListMixin extends GuiListBase {
    private static final int BUTTON_SPACING = 1;
    private static final int BUTTON_Y = 24;
    private static final int WRAPPED_BUTTON_Y_OFFSET = 22;
    private static final int PROGRESS_LINE_HEIGHT = 12;

    @Shadow
    @Final
    private MaterialListBase materialList;

    // How many lines the "总计 / 进度 / 缺少 / 不匹配的" summary was last
    // wrapped into; read by lmlp$addSchematicCacheStatus to keep the read
    // status label above it instead of overlapping.
    private int lmlp$progressLineCount = 1;

    // How many 22px rows the wrapped bottom button block occupies in narrow
    // mode (>=1), or 0 in wide mode where those buttons stay on the top row.
    // The progress summary and read-status label are lifted above the topmost
    // of these rows, and getBrowserHeight reserves space for the extra rows.
    // Precomputed at initGui HEAD so it's available before super.initGui()
    // calls getBrowserHeight and before the progress label is placed.
    private int lmlp$buttonRowCount = 0;

    protected GuiMaterialListMixin(int listX, int listY) {
        super(listX, listY);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void lmlp$refreshForCurrentStateOnEntry(MaterialListBase materialList, CallbackInfo ci) {
        if (ChunkMissingMaterialListCache.refreshForCurrentState(this.materialList, false) && this.mc.field_1724 != null) {
            MaterialListUtils.updateAvailableCounts(this.materialList.getMaterialsAll(), this.mc.field_1724);
        }
    }

    @Redirect(
            method = {"<init>", "onTaskCompleted"},
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/gui/widgets/WidgetMaterialListEntry;setMaxNameLength(Ljava/util/List;I)V"))
    private void lmlp$skipGlobalMaterialListColumnSizing(List<MaterialListEntry> entries, int multiplier) {
        // LMLP fits columns after WidgetListBase has built the current page's visible rows.
    }

    @ModifyArg(
            method = "createButton",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/gui/button/ButtonGeneric;<init>(IIIILjava/lang/String;[Ljava/lang/String;)V"),
            index = 4)
    private String lmlp$useMinimalSubMaterialListTypeLabel(String label) {
        String currentListTypeLabel = StringUtils.translate(
                "litematica.gui.button.material_list.list_type",
                this.materialList.getMaterialListType().getDisplayName());
        if (label.equals(currentListTypeLabel)) {
            return MinimalSubMaterialListView.displayName(this.materialList, label);
        }

        return label;
    }

    @Inject(method = "getBrowserHeight", at = @At("RETURN"), cancellable = true)
    private void lmlp$makeRoomForChunkMissingStatus(CallbackInfoReturnable<Integer> cir) {
        int reserve = KnownPlacementRows.readStatus(this.materialList) != null ? 12 : 0;
        reserve += PROGRESS_LINE_HEIGHT * (this.lmlp$progressLineCount - 1);
        // Vanilla's list height already clears one wrapped button row (it drops
        // the bottom buttons to height-22); reserve only for each additional
        // row so a two-row-tall button block doesn't cover the list bottom.
        if (this.lmlp$buttonRowCount > 1) {
            reserve += WRAPPED_BUTTON_Y_OFFSET * (this.lmlp$buttonRowCount - 1);
        }
        if (reserve > 0) {
            cir.setReturnValue(Math.max(0, cir.getReturnValue() - reserve));
        }
    }

    // getBrowserHeight() (called from super.initGui(), before this pass' own
    // progress label is rebuilt near the end of the method) needs this pass'
    // wrap decision, not the previous pass' — otherwise the row list keeps
    // its old (too-tall) height for one resize, overlapping the now-taller
    // label block below it. Mirrors vanilla's exact string construction so
    // the predicted width matches what will actually be measured later.
    @Inject(method = "initGui", at = @At("HEAD"))
    private void lmlp$precomputeProgressLineCount(CallbackInfo ci) {
        this.lmlp$progressLineCount = this.lmlp$computeProgressLineCount();
        this.lmlp$buttonRowCount = this.lmlp$computeButtonRowCount();
        // Reset the count-string memo on GUI (re)open so a language switch —
        // which forces the screen to close and reopen — can't surface stale
        // localized count text from a previous language.
        CountFormatter.clearCache();
    }

    // How many 22px rows the narrow-mode wrapped bottom button block occupies.
    // Mirrors lmlp$reflowWrappedBottomButtons' packing exactly, computed from
    // label widths so it's available at initGui HEAD (before the buttons and
    // the progress label exist). Returns 0 in wide mode where nothing wraps.
    private int lmlp$computeButtonRowCount() {
        if (this.field_22789 >= this.lmlp$fullTopRowWidth()) {
            return 0;
        }

        // The buttons that vanilla + this mod drop onto the bottom rows in
        // narrow mode, left to right (same order the reflow sorts them by X):
        // clear_ignored, clear_cache, write_to_file, then our export button —
        // followed by any top-row toggles pushed down because they'd overlap
        // the multiplier label (hide_available before toggle_info_hud, matching
        // the sentinel X order set in lmlp$wrapOverflowingTopButtons).
        List<Integer> widths = new ArrayList<>();
        widths.add(this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.clear_ignored")));
        widths.add(this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.clear_cache")));
        widths.add(this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.write_to_file")));
        widths.add(this.lmlp$genericButtonWidth(StringUtils.translate("lmlp.gui.button.material_list.write_sub_materials")));
        int movedToggles = this.lmlp$movedTopToggleCount();
        if (movedToggles >= 2) {
            widths.add(this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.hide_available", this.materialList.getHideAvailable()));
        }
        if (movedToggles >= 1) {
            widths.add(this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.toggle_info_hud", this.materialList.getHudRenderer().getShouldRenderCustom()));
        }

        int fullLimit = this.field_22789 - 12;
        int pinnedLimit = this.lmlp$openConfigButtonX() - 4;
        int rows = 1;
        int x = 12;
        int limit = pinnedLimit;
        boolean atBottom = true;
        for (int w : widths) {
            boolean fits = x + w <= limit;
            if (!fits && x > 12) {
                rows++;
                x = 12;
                limit = fullLimit;
                atBottom = false;
                fits = x + w <= limit;
            }
            if (!fits && atBottom) {
                rows++;
                limit = fullLimit;
                atBottom = false;
            }
            x += w + BUTTON_SPACING;
        }

        return rows;
    }

    // How many of the two always-top toggle buttons (隐藏可用 / 信息HUD, in
    // right-to-left move order) must be pushed down to the wrapped bottom flow
    // because the top row (刷新 + 显示 + these toggles) would otherwise run
    // under the right-aligned multiplier label. Vanilla never wraps these four,
    // so a long list-type label ("最小子材料") made 信息HUD cover "倍数".
    // Computed from label widths so it's available at initGui HEAD and shared
    // by lmlp$wrapOverflowingTopButtons and lmlp$computeButtonRowCount.
    private int lmlp$movedTopToggleCount() {
        // Left edge of vanilla's multiplier label (field_22789 - labelW - 56),
        // minus a small gap the buttons must stay clear of.
        int multiplierLabelWidth = this.getStringWidth(StringUtils.translate("litematica.gui.label.material_list.multiplier"));
        int available = this.field_22789 - multiplierLabelWidth - 56 - 4;

        int x = 12;
        x += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.refresh_list")) + BUTTON_SPACING;
        if (this.materialList.supportsRenderLayers()) {
            x += this.lmlp$genericButtonWidth(this.lmlp$listTypeDisplayName()) + BUTTON_SPACING;
        }
        int hideRight = x + this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.hide_available", this.materialList.getHideAvailable());
        int infoLeft = hideRight + BUTTON_SPACING;
        int infoRight = infoLeft + this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.toggle_info_hud", this.materialList.getHudRenderer().getShouldRenderCustom());

        int moved = 0;
        if (infoRight > available) {
            moved = 1;
            if (hideRight > available) {
                moved = 2;
            }
        }
        return moved;
    }

    // Bottom (last) line of the progress summary. In wide mode and when the
    // bottom buttons occupy a single row, this stays at vanilla's height-36.
    // Once the buttons wrap to two or more rows, the topmost row climbs to
    // height-22*rows, so the summary is lifted one line above it rather than
    // being overlapped by the stacked buttons.
    private int lmlp$progressBottomLineY() {
        int vanilla = this.field_22790 - 36;
        if (this.lmlp$buttonRowCount >= 2) {
            int topButtonRowY = this.field_22790 - WRAPPED_BUTTON_Y_OFFSET * this.lmlp$buttonRowCount;
            return Math.min(vanilla, topButtonRowY - PROGRESS_LINE_HEIGHT);
        }

        return vanilla;
    }

    // Top line of the (possibly multi-line) progress summary.
    private int lmlp$progressBlockTopLineY() {
        return this.lmlp$progressBottomLineY() - PROGRESS_LINE_HEIGHT * (this.lmlp$progressLineCount - 1);
    }

    private int lmlp$computeProgressLineCount() {
        long total = this.materialList.getCountTotal();
        if (total == 0 || (Object) this.materialList instanceof MaterialListAreaAnalyzer) {
            return 1;
        }

        long missing = this.materialList.getCountMissing() - this.materialList.getCountMismatched();
        long mismatch = this.materialList.getCountMismatched();
        double pctDone = ((double) (total - (missing + mismatch)) / (double) total) * 100;
        double pctMissing = ((double) missing / (double) total) * 100;
        double pctMismatch = ((double) mismatch / (double) total) * 100;
        String strt = StringUtils.translate("litematica.gui.label.material_list.total", total);
        String strp;
        if (missing == 0 && mismatch == 0) {
            strp = StringUtils.translate("litematica.gui.label.material_list.progress.done", String.format("%.0f %%%%", pctDone));
        } else {
            String str1 = StringUtils.translate("litematica.gui.label.material_list.progress.done", String.format("%.1f %%%%", pctDone));
            String str2 = StringUtils.translate("litematica.gui.label.material_list.progress.missing", String.format("%.1f %%%%", pctMissing));
            String str3 = StringUtils.translate("litematica.gui.label.material_list.progress.mismatch", String.format("%.1f %%%%", pctMismatch));
            strp = String.format("%s / %s / %s", str1, str2, str3);
        }

        String full = strt + " / " + StringUtils.translate("litematica.gui.label.material_list.progress", strp);
        int budget = this.field_22789 - 12 - 4;
        if (this.getStringWidth(full) <= budget) {
            return 1;
        }

        return this.lmlp$wrapBySeparator(full, budget).size();
    }

    // Vanilla draws the "总计 / 进度: 完成 x% / 缺少 x% / 不匹配的 x%" summary
    // as a single unwrapped line sized to its full content width, which runs
    // past the window edge on narrow windows. Wrap it at the " / " separators
    // instead of letting it overflow or blindly truncating it (truncation
    // would cut off the tail — exactly the "不匹配的" part users want to see).
    @Redirect(
            method = "initGui",
            at = @At(
                    value = "INVOKE",
                    // javac compiles this.addLabel(...) (an inherited GuiBase
                    // method) with GuiMaterialList itself as the invokevirtual
                    // owner in the constant pool, not the declaring class;
                    // targeting GuiBase here found zero call sites and made
                    // the whole mixin fail to apply (crash on launch).
                    target = "Lfi/dy/masa/litematica/gui/GuiMaterialList;addLabel(IIIII[Ljava/lang/String;)Lfi/dy/masa/malilib/gui/widgets/WidgetLabel;",
                    ordinal = 1))
    private WidgetLabel lmlp$wrapProgressSummary(GuiMaterialList self, int x, int y, int width, int height, int color, String[] text) {
        // Defensive fallback in case a future vanilla change shifts which
        // addLabel call this ordinal lands on: only wrap the one at the
        // expected progress-line position, pass everything else through.
        if (y != this.field_22790 - 36) {
            return this.addLabel(x, y, width, height, color, text);
        }

        String full = text.length > 0 ? text[0] : "";
        int budget = this.field_22789 - x - 4;
        if (this.getStringWidth(full) <= budget) {
            this.lmlp$progressLineCount = 1;
            return this.addLabel(x, this.lmlp$progressBottomLineY(), width, height, color, full);
        }

        List<String> lines = this.lmlp$wrapBySeparator(full, budget);
        this.lmlp$progressLineCount = lines.size();
        int topY = this.lmlp$progressBlockTopLineY();
        // A single WidgetLabel packs its lines at a fixed 9px pitch
        // (malilib's WidgetBase.fontHeight), noticeably tighter than the
        // 12px rhythm the rest of this bottom block uses (row height, the
        // read-status label gap, etc.) — one multi-line label made the wrap
        // look cramped next to everything around it. Give each line its own
        // label instead, spaced at the same 12px pitch as the surrounding
        // layout math already assumes.
        WidgetLabel first = this.addLabel(x, topY, width, height, color, lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            this.addLabel(x, topY + PROGRESS_LINE_HEIGHT * i, width, height, color, lines.get(i));
        }

        return first;
    }

    private List<String> lmlp$wrapBySeparator(String text, int maxWidth) {
        String[] tokens = text.split(" / ");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String token : tokens) {
            String candidate = current.isEmpty() ? token : current + " / " + token;
            if (!current.isEmpty() && this.getStringWidth(candidate) > maxWidth) {
                lines.add(current.toString());
                current = new StringBuilder(token);
            } else {
                current = new StringBuilder(candidate);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines.isEmpty() ? List.of(text) : lines;
    }

    // Vanilla's getElementTotalWidth() estimates the top button row from bare
    // string widths plus a fixed 130px buffer. It knows nothing about the
    // mod's extra export button and underestimates per-button padding and the
    // multiplier label/textfield zone, so the row overlapped the multiplier
    // before vanilla's isNarrow check wrapped it. Raise the threshold to the
    // real row width so the wrap happens first.
    @Inject(method = "getElementTotalWidth", at = @At("RETURN"), cancellable = true)
    private void lmlp$useRealTopRowWidth(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.max(cir.getReturnValue(), this.lmlp$fullTopRowWidth()));
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void lmlp$addSubMaterialExportButton(CallbackInfo ci) {
        this.lmlp$removeVanillaRecipeExportButtons();
        int x = this.lmlp$subMaterialExportButtonX();
        int y = this.field_22789 < this.lmlp$fullTopRowWidth() ? this.field_22790 - WRAPPED_BUTTON_Y_OFFSET : BUTTON_Y;
        String label = StringUtils.translate("lmlp.gui.button.material_list.write_sub_materials");
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, label, new String[0]);
        button.setHoverStrings("lmlp.gui.button.hover.material_list.write_sub_materials");
        this.addButton(button, new SubMaterialExportButtonListener((GuiMaterialList) (Object) this));
    }

    private void lmlp$removeVanillaRecipeExportButtons() {
        List<ButtonBase> buttons = ((GuiBaseHoverAccess) (Object) this).lmlp$getButtons();
        buttons.removeIf(button ->
                this.lmlp$hasHoverKey(button, "litematica.gui.button.hover.material_list.json_hold_shift_for_missing_only")
                        || this.lmlp$hasHoverKey(button, "litematica.gui.button.hover.material_list.export_custom_json"));
    }

    private boolean lmlp$hasHoverKey(ButtonBase button, String translationKey) {
        List<String> hoverStrings = button.getHoverStrings();
        if (hoverStrings.isEmpty()) {
            return false;
        }

        return this.lmlp$firstHoverLine(hoverStrings.get(0))
                .equals(this.lmlp$firstHoverLine(StringUtils.translate(translationKey)));
    }

    private String lmlp$firstHoverLine(String text) {
        String normalized = text == null ? "" : text.replace("\\n", "\n");
        int newline = normalized.indexOf('\n');
        return newline >= 0 ? normalized.substring(0, newline) : normalized;
    }

    // Add an "LMLP 配置" button immediately left of vanilla's bottom-right
    // "主菜单" button so the mod's config screen is reachable from the material
    // list directly. Always placed at height-36 next to the main-menu button —
    // the two form a right-aligned pair. Declared before
    // lmlp$reflowWrappedBottomButtons so it's already in the buttons list when
    // the narrow-mode reflow runs (mixin applies TAIL injectors in declaration
    // order); in narrow mode the reflow pulls this pair down onto the wrapped
    // row together, keeping them right-aligned.
    @Inject(method = "initGui", at = @At("TAIL"))
    private void lmlp$addOpenConfigButton(CallbackInfo ci) {
        String label = StringUtils.translate("lmlp.gui.button.material_list.open_config");
        int width = this.getStringWidth(label) + 20;
        ButtonGeneric button = new ButtonGeneric(this.lmlp$openConfigButtonX(), this.field_22790 - 36, width, 20, label, new String[0]);
        button.setHoverStrings("lmlp.gui.button.hover.material_list.open_config");
        this.addButton(button, new OpenConfigButtonListener((GuiMaterialList) (Object) this));
    }

    // Wide-mode X of the "LMLP 配置" button: immediately left of vanilla's
    // main-menu button. Also used by lmlp$computeButtonRowCount to know where
    // the pinned right-side pair starts before any button exists yet.
    private int lmlp$openConfigButtonX() {
        String label = StringUtils.translate("lmlp.gui.button.material_list.open_config");
        int width = this.getStringWidth(label) + 20;
        // Sit BUTTON_SPACING (1px) left of the main-menu button so the gap
        // between the two right-side buttons matches the left buttons' spacing.
        return this.lmlp$mainMenuButtonX() - width - BUTTON_SPACING;
    }

    // Mirrors vanilla's own placement of the bottom-right main-menu button
    // (width = label width + 20, x = window width - width - 10).
    private int lmlp$mainMenuButtonX() {
        String menuLabel = StringUtils.translate("litematica.gui.button.change_menu.to_main_menu");
        int menuWidth = this.getStringWidth(menuLabel) + 20;
        return this.field_22789 - menuWidth - 10;
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void lmlp$addSchematicCacheStatus(CallbackInfo ci) {
        ReadStatus readStatus = KnownPlacementRows.readStatus(this.materialList);
        if (readStatus != null) {
            String status = StringUtils.translate("lmlp.gui.material_list.read_status", readStatus.label());
            int width = this.getStringWidth(status);
            // Wide mode: keep vanilla's spare row below the progress line
            // (height-24). Narrow mode: the bottom is crowded by wrapped button
            // rows and the lifted progress block, so sit one line above the top
            // of that block instead of overlapping it.
            boolean narrow = this.field_22789 < this.lmlp$fullTopRowWidth();
            int y = narrow
                    ? this.lmlp$progressBlockTopLineY() - PROGRESS_LINE_HEIGHT
                    : this.field_22790 - 24;
            this.addLabel(12, y, width, 12, readStatus.color(), status);
        }
    }

    // Push the always-top toggle buttons (信息HUD, then 隐藏可用) down to the
    // wrapped bottom flow when they'd otherwise run under the multiplier label.
    // Declared before lmlp$reflowWrappedBottomButtons so the moved buttons are
    // already at the wrapped row when the reflow packs it. They get large,
    // increasing sentinel X values so the reflow (which sorts by X and re-lays
    // from x=12) orders them last in the wrapped row — after clear/cache/write,
    // before the pinned pair — while keeping 隐藏可用 before 信息HUD.
    @Inject(method = "initGui", at = @At("TAIL"))
    private void lmlp$wrapOverflowingTopButtons(CallbackInfo ci) {
        int moved = this.lmlp$movedTopToggleCount();
        if (moved <= 0) {
            return;
        }

        List<ButtonBase> topRow = new ArrayList<>();
        for (ButtonBase button : ((GuiBaseHoverAccess) (Object) this).lmlp$getButtons()) {
            if (button.getY() == BUTTON_Y) {
                topRow.add(button);
            }
        }
        topRow.sort(Comparator.comparingInt(ButtonBase::getX));

        int rowY = this.field_22790 - WRAPPED_BUTTON_Y_OFFSET;
        int count = Math.min(moved, Math.max(0, topRow.size() - 1));
        for (int k = 0; k < count; k++) {
            ButtonBase button = topRow.get(topRow.size() - count + k);
            button.setPosition(this.field_22789 + k, rowY);
        }
    }

    // When the narrow layout wraps buttons to the bottom row, that row itself
    // can overflow the window width; re-lay it left to right and continue on
    // an additional row above when a button no longer fits. The right-aligned
    // pair fixed at height-36 (vanilla's main-menu button plus the mod's "LMLP
    // 配置" button, both misaligned with the wrapped row at height-22 and
    // overlapping the progress line) is pulled down onto the wrapped row as a
    // group, and the left-to-right flow stops short of the leftmost of them.
    @Inject(method = "initGui", at = @At("TAIL"))
    private void lmlp$reflowWrappedBottomButtons(CallbackInfo ci) {
        if (this.field_22789 >= this.lmlp$fullTopRowWidth()) {
            return;
        }

        int rowY = this.field_22790 - WRAPPED_BUTTON_Y_OFFSET;
        int menuRowY = this.field_22790 - 36;
        List<ButtonBase> pinned = new ArrayList<>();
        List<ButtonBase> row = new ArrayList<>();
        for (ButtonBase button : ((GuiBaseHoverAccess) (Object) this).lmlp$getButtons()) {
            if (button.getY() == rowY) {
                row.add(button);
            } else if (button.getY() == menuRowY) {
                pinned.add(button);
            }
        }

        // Pull the right-aligned pair down to the wrapped row, keeping their
        // relative X so they stay side by side; the left flow on this bottom
        // row must stop before the leftmost of them.
        int fullLimit = this.field_22789 - 12;
        int pinnedLimit = fullLimit;
        for (ButtonBase button : pinned) {
            button.setPosition(button.getX(), rowY);
            pinnedLimit = Math.min(pinnedLimit, button.getX() - 4);
        }

        // Bottom row shares its width with the pinned pair; rows stacked above
        // it are unobstructed and use the full window width. When a wrapped
        // button can't even fit as the first item beside the pinned pair, hand
        // the pinned pair the bottom row to itself and start flowing above it,
        // rather than dropping the button at x=12 on top of the pair (the
        // occlusion that reappeared as the window kept shrinking).
        row.sort(Comparator.comparingInt(ButtonBase::getX));
        int x = 12;
        int y = rowY;
        int limit = pinnedLimit;
        for (ButtonBase button : row) {
            boolean fits = x + button.getWidth() <= limit;
            if (!fits && x > 12) {
                y -= WRAPPED_BUTTON_Y_OFFSET;
                x = 12;
                limit = fullLimit;
                fits = x + button.getWidth() <= limit;
            }
            if (!fits && y == rowY) {
                y -= WRAPPED_BUTTON_Y_OFFSET;
                limit = fullLimit;
            }

            button.setPosition(x, y);
            x += button.getWidth() + BUTTON_SPACING;
        }
    }

    private int lmlp$subMaterialExportButtonX() {
        int x = 12;
        x += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.refresh_list")) + BUTTON_SPACING;
        if (this.materialList.supportsRenderLayers()) {
            x += this.lmlp$genericButtonWidth(this.lmlp$listTypeDisplayName()) + BUTTON_SPACING;
        }
        x += this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.hide_available", this.materialList.getHideAvailable()) + BUTTON_SPACING;
        x += this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.toggle_info_hud", this.materialList.getHudRenderer().getShouldRenderCustom()) + BUTTON_SPACING;
        if (this.field_22789 < this.lmlp$fullTopRowWidth()) {
            x = 12;
        }

        x += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.clear_ignored")) + BUTTON_SPACING;
        x += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.clear_cache")) + BUTTON_SPACING;
        x += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.write_to_file")) + BUTTON_SPACING;
        return x;
    }

    // The real single-row width of the top button row: actual button widths
    // (including the mod's export button) plus spacing, plus the multiplier
    // label/textfield zone vanilla reserves at the right edge (label width +
    // 56px for the 40px textfield and margins).
    private int lmlp$fullTopRowWidth() {
        int width = 12;
        width += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.refresh_list")) + BUTTON_SPACING;
        if (this.materialList.supportsRenderLayers()) {
            width += this.lmlp$genericButtonWidth(this.lmlp$listTypeDisplayName()) + BUTTON_SPACING;
        }
        width += this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.hide_available", this.materialList.getHideAvailable()) + BUTTON_SPACING;
        width += this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.toggle_info_hud", this.materialList.getHudRenderer().getShouldRenderCustom()) + BUTTON_SPACING;
        width += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.clear_ignored")) + BUTTON_SPACING;
        width += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.clear_cache")) + BUTTON_SPACING;
        width += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.write_to_file")) + BUTTON_SPACING;
        width += this.lmlp$genericButtonWidth(StringUtils.translate("lmlp.gui.button.material_list.write_sub_materials")) + BUTTON_SPACING;
        width += this.getStringWidth(StringUtils.translate("litematica.gui.label.material_list.multiplier")) + 56;
        return width + 6;
    }

    private int lmlp$genericButtonWidth(String label) {
        return new ButtonGeneric(0, 0, -1, 20, label, new String[0]).getWidth();
    }

    private String lmlp$listTypeDisplayName() {
        BlockInfoListType listType = this.materialList.getMaterialListType();
        String label = StringUtils.translate("litematica.gui.button.material_list.list_type", listType.getDisplayName());
        return MinimalSubMaterialListView.displayName(this.materialList, label);
    }

    private int lmlp$onOffButtonWidth(String translationKey, boolean value) {
        return new ButtonOnOff(0, 0, -1, false, translationKey, value, new String[0]).getWidth();
    }

    private static final class SubMaterialExportButtonListener implements IButtonActionListener {
        private final GuiMaterialList parent;

        private SubMaterialExportButtonListener(GuiMaterialList parent) {
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            File file = SubMaterialExporter.export(this.parent.getMaterialList());
            if (file != null) {
                String messageKey = "litematica.message.material_list_written_to_file";
                this.parent.addMessage(MessageType.SUCCESS, messageKey, file.getName());
                StringUtils.sendOpenFileChatMessage(this.parent.mc.field_1724, messageKey, file);
            } else {
                this.parent.addMessage(MessageType.ERROR, "lmlp.message.sub_material_list_export_failed");
            }

            this.parent.initGui();
        }
    }

    private static final class OpenConfigButtonListener implements IButtonActionListener {
        private final GuiMaterialList parent;

        private OpenConfigButtonListener(GuiMaterialList parent) {
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            GuiBase.openGui(new GuiConfigs().setParent(this.parent));
        }
    }
}
