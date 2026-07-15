package io.github.huanmeng06.lmlp.mixin;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.class)
public interface RenderLayerInvoker {
    @Invoker("create")
    static RenderType lmlp$create(String name, RenderSetup parameters) {
        throw new AssertionError();
    }
}
