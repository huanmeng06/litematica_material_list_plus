package io.github.huanmeng06.lmlp.mixin;

import net.minecraft.class_1921;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.class_1921$class_4687")
public interface RenderLayerMultiPhaseAccessor {
    @Accessor("field_21403")
    class_1921.class_4688 lmlp$getPhases();
}
