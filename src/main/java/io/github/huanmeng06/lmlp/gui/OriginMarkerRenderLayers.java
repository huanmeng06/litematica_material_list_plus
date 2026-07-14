package io.github.huanmeng06.lmlp.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DepthTestFunction;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.mixin.RenderLayerInvoker;
import io.github.huanmeng06.lmlp.mixin.RenderPipelinesAccessor;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_12245;
import net.minecraft.class_12247;
import net.minecraft.class_822;

final class OriginMarkerRenderLayers {
    private static final RenderPipeline FILL_PIPELINE = RenderPipeline.builder(
                    RenderPipelinesAccessor.lmlp$getPositionColorSnippet())
            .withLocation(class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_fill"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();

    static final class_2960 TARGET_TEXTURE = class_2960.method_60655(
            LitematicaMaterialListPlus.MOD_ID,
            "textures/gui/origin_target.png");

    private static final RenderPipeline BEAM_OPAQUE_PIPELINE = RenderPipeline.builder(
                    RenderPipelinesAccessor.lmlp$getBeaconBeamSnippet())
            .withLocation(class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_beam_opaque"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();

    private static final RenderPipeline BEAM_TRANSLUCENT_PIPELINE = RenderPipeline.builder(
                    RenderPipelinesAccessor.lmlp$getBeaconBeamSnippet())
            .withLocation(class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_beam_translucent"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .build();

    private static final RenderPipeline ICON_PIPELINE = RenderPipeline.builder(
                    RenderPipelinesAccessor.lmlp$getGuiTexturedSnippet())
            .withLocation(class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_icon"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();

    static final class_1921 FILL = RenderLayerInvoker.lmlp$create(
            "lmlp_origin_marker_fill",
            class_12247.method_75927(FILL_PIPELINE)
                    .method_75937()
                    .method_75930(class_12245.field_63976)
                    .method_75938());

    static final class_1921 BEAM_OPAQUE = RenderLayerInvoker.lmlp$create(
            "lmlp_origin_marker_beam_opaque",
            class_12247.method_75927(BEAM_OPAQUE_PIPELINE)
                    .method_75934("Sampler0", class_822.field_4338)
                    .method_75937()
                    .method_75930(class_12245.field_63976)
                    .method_75938());

    static final class_1921 BEAM_TRANSLUCENT = RenderLayerInvoker.lmlp$create(
            "lmlp_origin_marker_beam_translucent",
            class_12247.method_75927(BEAM_TRANSLUCENT_PIPELINE)
                    .method_75934("Sampler0", class_822.field_4338)
                    .method_75937()
                    .method_75930(class_12245.field_63976)
                    .method_75938());

    static final class_1921 ICON = RenderLayerInvoker.lmlp$create(
            "lmlp_origin_marker_icon",
            class_12247.method_75927(ICON_PIPELINE)
                    .method_75934("Sampler0", TARGET_TEXTURE)
                    .method_75937()
                    .method_75930(class_12245.field_63976)
                    .method_75938());

    private OriginMarkerRenderLayers() {
    }
}
