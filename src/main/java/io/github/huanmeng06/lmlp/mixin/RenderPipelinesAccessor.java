package io.github.huanmeng06.lmlp.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
    @Accessor("DEBUG_FILLED_SNIPPET")
    static RenderPipeline.Snippet lmlp$getPositionColorSnippet() {
        throw new AssertionError();
    }

    @Accessor("BEACON_BEAM_SNIPPET")
    static RenderPipeline.Snippet lmlp$getBeaconBeamSnippet() {
        throw new AssertionError();
    }

    @Accessor("GUI_TEXTURED_SNIPPET")
    static RenderPipeline.Snippet lmlp$getGuiTexturedSnippet() {
        throw new AssertionError();
    }
}
