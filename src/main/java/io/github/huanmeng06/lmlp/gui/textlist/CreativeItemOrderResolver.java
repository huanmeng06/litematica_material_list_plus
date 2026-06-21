package io.github.huanmeng06.lmlp.gui.textlist;

import io.github.huanmeng06.lmlp.material.ItemStackTexts;
import net.minecraft.class_1761;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_7706;
import net.minecraft.class_7923;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CreativeItemOrderResolver {
    private CreativeItemOrderResolver() {
    }

    static Map<String, Integer> createOrder() {
        Map<String, Integer> order = new HashMap<>();
        int nextRank = addCreativeGroups(order, class_7706.method_47335(), 0);
        if (order.isEmpty()) {
            nextRank = addCreativeGroups(order, class_7706.method_47341(), 0);
        }
        if (order.isEmpty()) {
            addRegistryOrder(order, nextRank);
        }
        return order;
    }

    private static int addCreativeGroups(Map<String, Integer> order, List<class_1761> groups, int nextRank) {
        try {
            for (class_1761 group : groups) {
                Collection<class_1799> displayStacks = group.method_47313();
                nextRank = addStacks(order, displayStacks, nextRank);
                if (displayStacks.isEmpty()) {
                    nextRank = addStacks(order, group.method_45414(), nextRank);
                }
            }
        } catch (RuntimeException ignored) {
            return nextRank;
        }
        return nextRank;
    }

    private static int addStacks(Map<String, Integer> order, Collection<class_1799> stacks, int nextRank) {
        for (class_1799 stack : stacks) {
            if (stack == null || stack.method_7960()) {
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
        for (class_1792 item : class_7923.field_41178.method_10220().toList()) {
            class_1799 stack = item.method_7854();
            if (stack == null || stack.method_7960()) {
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