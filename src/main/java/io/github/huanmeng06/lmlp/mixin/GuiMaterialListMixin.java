package io.github.huanmeng06.lmlp.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.access.MaterialListPlacementAccess;
import io.github.huanmeng06.lmlp.cache.ChunkMissingMaterialListCache;
import io.github.huanmeng06.lmlp.export.SubMaterialExporter;
import io.github.huanmeng06.lmlp.gui.MinimalSubMaterialListView;
import net.minecraft.class_437;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.List;

@Mixin(value = GuiMaterialList.class, remap = false)
public abstract class GuiMaterialListMixin extends GuiListBase {
    private static final int BUTTON_SPACING = 1;
    private static final int BUTTON_Y = 24;
    private static final int WRAPPED_BUTTON_Y_OFFSET = 22;

    @Shadow
    @Final
    private MaterialListBase materialList;

    protected GuiMaterialListMixin(int listX, int listY) {
        super(listX, listY);
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static MaterialListBase lmlp$useChunkMissingListForGui(MaterialListBase materialList) {
        if (materialList instanceof MaterialListPlacement && materialList instanceof MaterialListPlacementAccess access) {
            SchematicPlacement placement = access.lmlp$getPlacement();
            if (placement != null && !ChunkMissingMaterialListCache.arePlacementChunksLoaded(placement)) {
                return ChunkMissingMaterialListCache.getOrCreate(placement, materialList);
            }
        }

        return materialList;
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

    @Inject(method = "initGui", at = @At("TAIL"))
    private void lmlp$addSubMaterialExportButton(CallbackInfo ci) {
        int x = this.lmlp$subMaterialExportButtonX();
        int y = this.field_22789 < this.lmlp$originalElementTotalWidth() ? this.field_22790 - WRAPPED_BUTTON_Y_OFFSET : BUTTON_Y;
        String label = StringUtils.translate("lmlp.gui.button.material_list.write_sub_materials");
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, label, new String[0]);
        button.setHoverStrings("lmlp.gui.button.hover.material_list.write_sub_materials");
        this.addButton(button, new SubMaterialExportButtonListener((GuiMaterialList) (Object) this));
    }

    private int lmlp$subMaterialExportButtonX() {
        int x = 12;
        x += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.refresh_list")) + BUTTON_SPACING;
        if (this.materialList.supportsRenderLayers()) {
            x += this.lmlp$genericButtonWidth(this.lmlp$listTypeDisplayName()) + BUTTON_SPACING;
        }
        x += this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.hide_available", this.materialList.getHideAvailable()) + BUTTON_SPACING;
        x += this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.toggle_info_hud", this.materialList.getHudRenderer().getShouldRenderCustom()) + BUTTON_SPACING;
        if (this.field_22789 < this.lmlp$originalElementTotalWidth()) {
            x = 12;
        }

        x += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.clear_ignored")) + BUTTON_SPACING;
        x += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.clear_cache")) + BUTTON_SPACING;
        x += this.lmlp$genericButtonWidth(StringUtils.translate("litematica.gui.button.material_list.write_to_file")) + BUTTON_SPACING;
        return x;
    }

    private int lmlp$originalElementTotalWidth() {
        int width = 0;
        width += this.getStringWidth(StringUtils.translate("litematica.gui.button.material_list.refresh_list"));
        width += this.getStringWidth(this.lmlp$listTypeDisplayName());
        width += this.getStringWidth(StringUtils.translate("litematica.gui.button.material_list.clear_ignored"));
        width += this.getStringWidth(StringUtils.translate("litematica.gui.button.material_list.clear_cache"));
        width += this.getStringWidth(StringUtils.translate("litematica.gui.button.material_list.write_to_file"));
        width += this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.hide_available", false);
        width += this.lmlp$onOffButtonWidth("litematica.gui.button.material_list.toggle_info_hud", false);
        width += this.getStringWidth(StringUtils.translate("litematica.gui.label.material_list.multiplier"));
        return width + 130;
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
