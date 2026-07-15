package io.github.huanmeng06.lmlp.gui;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.mixin.RenderLayerMultiPhaseAccessor;
import io.github.huanmeng06.lmlp.mixin.RenderPipelinesAccessor;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_822;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class OriginMarkerRenderLayers {
    private static final Method CREATE_LAYER = findCreateLayerMethod();
    static final class_2960 TARGET_TEXTURE = class_2960.method_60655(
            LitematicaMaterialListPlus.MOD_ID,
            "textures/gui/origin_target.png");

    private static final RenderPipeline FILL_PIPELINE = RenderPipeline.builder(
                    RenderPipelinesAccessor.lmlp$getPositionColorSnippet())
            .withLocation(class_2960.method_60655(LitematicaMaterialListPlus.MOD_ID, "pipeline/origin_marker_fill"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();

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

    static final class_1921 FILL = withPipeline(
            "lmlp_origin_marker_fill",
            class_1921.method_49042(),
            FILL_PIPELINE);

    static final class_1921 BEAM_OPAQUE = withPipeline(
            "lmlp_origin_marker_beam_opaque",
            class_1921.method_23592(class_822.field_4338, false),
            BEAM_OPAQUE_PIPELINE);

    static final class_1921 BEAM_TRANSLUCENT = withPipeline(
            "lmlp_origin_marker_beam_translucent",
            class_1921.method_23592(class_822.field_4338, true),
            BEAM_TRANSLUCENT_PIPELINE);

    static final class_1921 ICON = withPipeline(
            "lmlp_origin_marker_icon",
            class_1921.method_23572(TARGET_TEXTURE),
            ICON_PIPELINE);

    private static class_1921 withPipeline(String name, class_1921 vanilla, RenderPipeline pipeline) {
        class_1921.class_4688 phases = ((RenderLayerMultiPhaseAccessor) (Object) vanilla).lmlp$getPhases();
        try {
            return (class_1921) CREATE_LAYER.invoke(null, name, vanilla.method_22722(), pipeline, phases);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create the origin marker render layer", exception);
        }
    }

    private static Method findCreateLayerMethod() {
        for (Method method : class_1921.class.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (Modifier.isStatic(method.getModifiers())
                    && parameters.length == 4
                    && parameters[0] == String.class
                    && parameters[1] == int.class
                    && parameters[2] == RenderPipeline.class
                    && parameters[3] == class_1921.class_4688.class) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new IllegalStateException("Minecraft 1.21.10 render-layer factory was not found");
    }

    private OriginMarkerRenderLayers() {
    }
}
