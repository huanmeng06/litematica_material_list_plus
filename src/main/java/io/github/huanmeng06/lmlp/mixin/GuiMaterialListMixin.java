package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetLabel;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows;
import io.github.huanmeng06.lmlp.gui.KnownPlacementRows.ReadStatus;
import io.github.huanmeng06.lmlp.export.SubMaterialExporter;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
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
        // lmlp$progressLineCount reflects the previous initGui pass (getBrowserHeight
        // runs before this pass' progress label is rebuilt), which just means a
        // one-resize lag if the wrap count changes; acceptable for this cosmetic reserve.
        reserve += PROGRESS_LINE_HEIGHT * (this.lmlp$progressLineCount - 1);
        if (reserve > 0) {
            cir.setReturnValue(Math.max(0, cir.getReturnValue() - reserve));
        }
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
            return this.addLabel(x, y, width, height, color, full);
        }

        List<String> lines = this.lmlp$wrapBySeparator(full, budget);
        this.lmlp$progressLineCount = lines.size();
        int topY = y - PROGRESS_LINE_HEIGHT * (lines.size() - 1);
        return this.addLabel(x, topY, width, height, color, lines.toArray(new String[0]));
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
        int x = this.lmlp$subMaterialExportButtonX();
        int y = this.field_22789 < this.lmlp$fullTopRowWidth() ? this.field_22790 - WRAPPED_BUTTON_Y_OFFSET : BUTTON_Y;
        String label = StringUtils.translate("lmlp.gui.button.material_list.write_sub_materials");
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, label, new String[0]);
        button.setHoverStrings("lmlp.gui.button.hover.material_list.write_sub_materials");
        this.addButton(button, new SubMaterialExportButtonListener((GuiMaterialList) (Object) this));
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void lmlp$addSchematicCacheStatus(CallbackInfo ci) {
        ReadStatus readStatus = KnownPlacementRows.readStatus(this.materialList);
        if (readStatus != null) {
            String status = StringUtils.translate("lmlp.gui.material_list.read_status", readStatus.label());
            int width = this.getStringWidth(status);
            // In narrow mode the vanilla button row wraps down to height-22,
            // right where this label normally sits; move it above the
            // progress line (vanilla puts that at height-36) so nothing
            // overlaps. The progress line's own top can shift further up when
            // it wraps to extra lines, so this label needs to follow it.
            boolean narrow = this.field_22789 < this.lmlp$fullTopRowWidth();
            int y = narrow
                    ? this.field_22790 - 48 - PROGRESS_LINE_HEIGHT * (this.lmlp$progressLineCount - 1)
                    : this.field_22790 - 24;
            this.addLabel(12, y, width, 12, readStatus.color(), status);
        }
    }

    // When the narrow layout wraps buttons to the bottom row, that row itself
    // can overflow the window width; re-lay it left to right and continue on
    // an additional row above when a button no longer fits. The vanilla main
    // menu button (fixed at height-36, overlapping the progress line and
    // misaligned with the wrapped row at height-22) is pulled down onto the
    // wrapped row, and the row limit stops short of it.
    @Inject(method = "initGui", at = @At("TAIL"))
    private void lmlp$reflowWrappedBottomButtons(CallbackInfo ci) {
        if (this.field_22789 >= this.lmlp$fullTopRowWidth()) {
            return;
        }

        int rowY = this.field_22790 - WRAPPED_BUTTON_Y_OFFSET;
        int menuRowY = this.field_22790 - 36;
        ButtonBase menuButton = null;
        List<ButtonBase> row = new ArrayList<>();
        for (ButtonBase button : ((GuiBaseHoverAccess) (Object) this).lmlp$getButtons()) {
            if (button.getY() == rowY) {
                row.add(button);
            } else if (button.getY() == menuRowY) {
                menuButton = button;
            }
        }

        int limit = this.field_22789 - 12;
        if (menuButton != null) {
            menuButton.setPosition(menuButton.getX(), rowY);
            limit = menuButton.getX() - 4;
        }

        row.sort(Comparator.comparingInt(ButtonBase::getX));
        int x = 12;
        int y = rowY;
        for (ButtonBase button : row) {
            if (x > 12 && x + button.getWidth() > limit) {
                y -= WRAPPED_BUTTON_Y_OFFSET;
                x = 12;
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
}
