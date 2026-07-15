package io.github.huanmeng06.lmlp.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.CompareOp;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.mixin.RenderLayerInvoker;
import io.github.huanmeng06.lmlp.mixin.RenderPipelinesAccessor;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

final class OriginMarkerRenderLayers {
    private static final DepthStencilState NO_DEPTH = new DepthStencilState(CompareOp.ALWAYS_PASS, false);
    private static final RenderPipeline FILL_PIPELINE = RenderPipeline.builder(
                    RenderPipelinesAccessor.lmlp$getPositionColorSnippet())
            .withLocation(Identifier.fromNamespaceAndPath(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_fill"))
            .withDepthStencilState(NO_DEPTH)
            .build();

    static final Identifier TARGET_TEXTURE = Identifier.fromNamespaceAndPath(
            LitematicaMaterialListPlus.MOD_ID,
            "textures/gui/origin_target.png");

    private static final RenderPipeline BEAM_OPAQUE_PIPELINE = RenderPipeline.builder(
                    RenderPipelinesAccessor.lmlp$getBeaconBeamSnippet())
            .withLocation(Identifier.fromNamespaceAndPath(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_beam_opaque"))
            .withDepthStencilState(NO_DEPTH)
            .build();

    private static final RenderPipeline BEAM_TRANSLUCENT_PIPELINE = RenderPipeline.builder(
                    RenderPipelinesAccessor.lmlp$getBeaconBeamSnippet())
            .withLocation(Identifier.fromNamespaceAndPath(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_beam_translucent"))
            .withDepthStencilState(NO_DEPTH)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();

    private static final RenderPipeline ICON_PIPELINE = RenderPipeline.builder(
                    RenderPipelinesAccessor.lmlp$getGuiTexturedSnippet())
            .withLocation(Identifier.fromNamespaceAndPath(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_icon"))
            .withDepthStencilState(NO_DEPTH)
            .build();

    static final RenderType FILL = RenderLayerInvoker.lmlp$create(
            "lmlp_origin_marker_fill",
            RenderSetup.builder(FILL_PIPELINE)
                    .sortOnUpload()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup());

    static final RenderType BEAM_OPAQUE = RenderLayerInvoker.lmlp$create(
            "lmlp_origin_marker_beam_opaque",
            RenderSetup.builder(BEAM_OPAQUE_PIPELINE)
                    .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
                    .sortOnUpload()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup());

    static final RenderType BEAM_TRANSLUCENT = RenderLayerInvoker.lmlp$create(
            "lmlp_origin_marker_beam_translucent",
            RenderSetup.builder(BEAM_TRANSLUCENT_PIPELINE)
                    .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
                    .sortOnUpload()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup());

    static final RenderType ICON = RenderLayerInvoker.lmlp$create(
            "lmlp_origin_marker_icon",
            RenderSetup.builder(ICON_PIPELINE)
                    .withTexture("Sampler0", TARGET_TEXTURE)
                    .sortOnUpload()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup());

    private OriginMarkerRenderLayers() {
    }
}
