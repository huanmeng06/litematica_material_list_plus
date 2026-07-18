package io.github.huanmeng06.lmlp.preference;

import io.github.huanmeng06.lmlp.config.WoodFamily;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class WoodBlockFamilies {
    private static final Map<String, BlockKind> KINDS_BY_ID = new LinkedHashMap<>();
    private static final Map<WoodFamily, Map<BlockKind, String>> IDS_BY_FAMILY = new EnumMap<>(WoodFamily.class);

    static {
        for (WoodFamily family : WoodFamily.values()) {
            IDS_BY_FAMILY.put(family, new EnumMap<>(BlockKind.class));
        }

        registerRegular(WoodFamily.OAK);
        registerRegular(WoodFamily.SPRUCE);
        registerRegular(WoodFamily.BIRCH);
        registerRegular(WoodFamily.JUNGLE);
        registerRegular(WoodFamily.ACACIA);
        registerRegular(WoodFamily.DARK_OAK);
        registerRegular(WoodFamily.MANGROVE);
        registerRegular(WoodFamily.CHERRY);
        registerRegular(WoodFamily.PALE_OAK);
        registerBamboo();
        registerNether(WoodFamily.CRIMSON);
        registerNether(WoodFamily.WARPED);
    }

    private WoodBlockFamilies() {
    }

    static ReplacementTarget targetFor(String sourceId, WoodFamily targetFamily) {
        BlockKind kind = KINDS_BY_ID.get(sourceId);
        if (kind == null) {
            return null;
        }
        String targetId = IDS_BY_FAMILY.get(targetFamily).get(kind);
        return new ReplacementTarget(kind, targetId);
    }

    private static void registerRegular(WoodFamily family) {
        String wood = family.id();
        put(family, BlockKind.PLANKS, wood + "_planks");
        put(family, BlockKind.LOG, wood + "_log");
        put(family, BlockKind.STRIPPED_LOG, "stripped_" + wood + "_log");
        put(family, BlockKind.WOOD, wood + "_wood");
        put(family, BlockKind.STRIPPED_WOOD, "stripped_" + wood + "_wood");
        put(family, BlockKind.LEAVES, wood + "_leaves");
        put(family, BlockKind.SAPLING, wood + "_sapling");
        putStandardProducts(family, wood);
    }

    private static void registerBamboo() {
        WoodFamily family = WoodFamily.BAMBOO;
        put(family, BlockKind.PLANKS, "bamboo_planks");
        put(family, BlockKind.LOG, "bamboo_block");
        put(family, BlockKind.STRIPPED_LOG, "stripped_bamboo_block");
        put(family, BlockKind.MOSAIC, "bamboo_mosaic");
        put(family, BlockKind.MOSAIC_SLAB, "bamboo_mosaic_slab");
        put(family, BlockKind.MOSAIC_STAIRS, "bamboo_mosaic_stairs");
        putStandardProducts(family, "bamboo");
    }

    private static void registerNether(WoodFamily family) {
        String wood = family.id();
        put(family, BlockKind.PLANKS, wood + "_planks");
        put(family, BlockKind.LOG, wood + "_stem");
        put(family, BlockKind.STRIPPED_LOG, "stripped_" + wood + "_stem");
        put(family, BlockKind.WOOD, wood + "_hyphae");
        put(family, BlockKind.STRIPPED_WOOD, "stripped_" + wood + "_hyphae");
        putStandardProducts(family, wood);
    }

    private static void putStandardProducts(WoodFamily family, String wood) {
        put(family, BlockKind.SLAB, wood + "_slab");
        put(family, BlockKind.STAIRS, wood + "_stairs");
        put(family, BlockKind.FENCE, wood + "_fence");
        put(family, BlockKind.FENCE_GATE, wood + "_fence_gate");
        put(family, BlockKind.DOOR, wood + "_door");
        put(family, BlockKind.TRAPDOOR, wood + "_trapdoor");
        put(family, BlockKind.PRESSURE_PLATE, wood + "_pressure_plate");
        put(family, BlockKind.BUTTON, wood + "_button");
        put(family, BlockKind.SIGN, wood + "_sign");
        put(family, BlockKind.WALL_SIGN, wood + "_wall_sign");
        put(family, BlockKind.HANGING_SIGN, wood + "_hanging_sign");
        put(family, BlockKind.WALL_HANGING_SIGN, wood + "_wall_hanging_sign");
    }

    private static void put(WoodFamily family, BlockKind kind, String path) {
        String id = "minecraft:" + path;
        IDS_BY_FAMILY.get(family).put(kind, id);
        KINDS_BY_ID.put(id, kind);
    }

    enum BlockKind {
        PLANKS,
        LOG,
        STRIPPED_LOG,
        WOOD,
        STRIPPED_WOOD,
        LEAVES,
        SAPLING,
        SLAB,
        STAIRS,
        FENCE,
        FENCE_GATE,
        DOOR,
        TRAPDOOR,
        PRESSURE_PLATE,
        BUTTON,
        SIGN,
        WALL_SIGN,
        HANGING_SIGN,
        WALL_HANGING_SIGN,
        MOSAIC,
        MOSAIC_SLAB,
        MOSAIC_STAIRS
    }

    record ReplacementTarget(BlockKind kind, String blockId) {
    }
}
