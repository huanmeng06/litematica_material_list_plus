package io.github.huanmeng06.lmlp.mixin;

import net.minecraft.class_1921;
import net.minecraft.class_12247;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(class_1921.class)
public interface RenderLayerInvoker {
    @Invoker("method_75940")
    static class_1921 lmlp$create(String name, class_12247 parameters) {
        throw new AssertionError();
    }
}
