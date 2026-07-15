package io.github.huanmeng06.lmlp.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.class_10799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_10799.class)
public interface RenderPipelinesAccessor {
    @Accessor("field_56860")
    static RenderPipeline.Snippet lmlp$getPositionColorSnippet() {
        throw new AssertionError();
    }

    @Accessor("field_56855")
    static RenderPipeline.Snippet lmlp$getBeaconBeamSnippet() {
        throw new AssertionError();
    }

    @Accessor("field_56864")
    static RenderPipeline.Snippet lmlp$getGuiTexturedSnippet() {
        throw new AssertionError();
    }
}
