package io.github.huanmeng06.lmlp.material;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class ItemStackTexts {
    private ItemStackTexts() {
    }

    public static String id(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "unknown" : id.toString();
    }

    public static String name(ItemStack stack) {
        return stack.getHoverName().getString();
    }
}
