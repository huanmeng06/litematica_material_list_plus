package io.github.huanmeng06.lmlp.gui.textlist;

import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class CreativeItemOrderResolver {
    private CreativeItemOrderResolver() {
    }

    static Map<String, Integer> createOrder() {
        Map<String, Integer> order = new HashMap<>();
        int nextRank = addCreativeGroups(order, CreativeModeTabs.tabs(), 0);
        if (order.isEmpty()) {
            nextRank = addCreativeGroups(order, CreativeModeTabs.allTabs(), 0);
        }
        if (order.isEmpty()) {
            addRegistryOrder(order, nextRank);
        }
        return order;
    }

    private static int addCreativeGroups(Map<String, Integer> order, List<CreativeModeTab> groups, int nextRank) {
        try {
            for (CreativeModeTab group : groups) {
                Collection<ItemStack> displayStacks = group.getDisplayItems();
                nextRank = addStacks(order, displayStacks, nextRank);
                if (displayStacks.isEmpty()) {
                    nextRank = addStacks(order, group.getSearchTabDisplayItems(), nextRank);
                }
            }
        } catch (RuntimeException ignored) {
            return nextRank;
        }
        return nextRank;
    }

    private static int addStacks(Map<String, Integer> order, Collection<ItemStack> stacks, int nextRank) {
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            String id = ItemStackTexts.id(stack);
            if (!id.isEmpty()) {
                order.putIfAbsent(id, nextRank);
                nextRank++;
            }
        }
        return nextRank;
    }

    private static void addRegistryOrder(Map<String, Integer> order, int nextRank) {
        for (Item item : BuiltInRegistries.ITEM.stream().toList()) {
            ItemStack stack = item.getDefaultInstance();
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            String id = ItemStackTexts.id(stack);
            if (!id.isEmpty()) {
                order.putIfAbsent(id, nextRank);
                nextRank++;
            }
        }
    }
}