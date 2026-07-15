package io.github.huanmeng06.lmlp.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.class_299;
import net.minecraft.class_10297;
import net.minecraft.class_10298;

@Mixin(class_299.class)
public interface ClientRecipeBookAccessor {
    @Accessor("field_54810")
    Map<class_10298, class_10297> lmlp$getDisplayEntries();
}
