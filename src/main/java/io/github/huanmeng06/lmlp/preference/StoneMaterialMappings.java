package io.github.huanmeng06.lmlp.preference;

import io.github.huanmeng06.lmlp.config.StoneMaterialFamily;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Explicit stone-family product mappings; no registry-name guessing is used at runtime. */
public final class StoneMaterialMappings {
    private static final Map<String, SourceProduct> SOURCES = new LinkedHashMap<>();
    private static final EnumMap<StoneMaterialFamily, Palette> PALETTES =
            new EnumMap<>(StoneMaterialFamily.class);
    private static final EnumMap<Shape, List<String>> ALLOWED_BY_SHAPE = new EnumMap<>(Shape.class);

    static {
        registerStone();
        registerCobblestone();
        registerSmoothStone();
        registerGranite();
        registerDiorite();
        registerAndesite();
        registerDeepslate();
        registerBlackstone();
        registerTuff();
        registerQuartz();
        buildAllowedTargets();
    }

    private StoneMaterialMappings() {
    }

    public static Match match(String sourceId, StoneMaterialFamily family) {
        SourceProduct source = SOURCES.get(sourceId);
        if (source == null) {
            return null;
        }

        Palette palette = PALETTES.get(family);
        String exactTarget = palette.byRole.get(source.role());
        String familyFallback = palette.fallbackByShape.get(source.shape());
        String target = exactTarget != null
                ? exactTarget
                : familyFallback != null ? familyFallback : missingShapeFallback(source.shape());
        return new Match(target, exactTarget != null, allowedTargets(source.shape()));
    }

    public static List<String> allowedTargetsForSource(String sourceId) {
        SourceProduct source = SOURCES.get(sourceId);
        return source == null ? List.of() : allowedTargets(source.shape());
    }

    private static List<String> allowedTargets(Shape shape) {
        return ALLOWED_BY_SHAPE.getOrDefault(shape, List.of());
    }

    private static String missingShapeFallback(Shape shape) {
        return switch (shape) {
            case STAIRS -> minecraft("stone_brick_stairs");
            case WALL -> minecraft("stone_brick_wall");
            default -> null;
        };
    }

    private static void registerStone() {
        Palette palette = palette(StoneMaterialFamily.STONE);
        product(palette, Role.RAW_BLOCK, "stone");
        product(palette, Role.RAW_STAIRS, "stone_stairs");
        product(palette, Role.RAW_SLAB, "stone_slab");
        product(palette, Role.COBBLED_BLOCK, "cobblestone");
        product(palette, Role.COBBLED_STAIRS, "cobblestone_stairs");
        product(palette, Role.COBBLED_SLAB, "cobblestone_slab");
        product(palette, Role.COBBLED_WALL, "cobblestone_wall");
        product(palette, Role.POLISHED_BLOCK, "smooth_stone");
        product(palette, Role.POLISHED_SLAB, "smooth_stone_slab");
        product(palette, Role.BRICK_BLOCK, "stone_bricks");
        product(palette, Role.BRICK_STAIRS, "stone_brick_stairs");
        product(palette, Role.BRICK_SLAB, "stone_brick_slab");
        product(palette, Role.BRICK_WALL, "stone_brick_wall");
        product(palette, Role.CHISELED_BLOCK, "chiseled_stone_bricks");
        product(palette, Role.CRACKED_BRICK_BLOCK, "cracked_stone_bricks");
        product(palette, Role.MOSSY_COBBLED_BLOCK, "mossy_cobblestone");
        product(palette, Role.MOSSY_COBBLED_STAIRS, "mossy_cobblestone_stairs");
        product(palette, Role.MOSSY_COBBLED_SLAB, "mossy_cobblestone_slab");
        product(palette, Role.MOSSY_COBBLED_WALL, "mossy_cobblestone_wall");
        product(palette, Role.MOSSY_BRICK_BLOCK, "mossy_stone_bricks");
        product(palette, Role.MOSSY_BRICK_STAIRS, "mossy_stone_brick_stairs");
        product(palette, Role.MOSSY_BRICK_SLAB, "mossy_stone_brick_slab");
        product(palette, Role.MOSSY_BRICK_WALL, "mossy_stone_brick_wall");
        fallback(palette, Shape.BLOCK, "stone");
        fallback(palette, Shape.STAIRS, "stone_stairs");
        fallback(palette, Shape.SLAB, "stone_slab");
        fallback(palette, Shape.WALL, "cobblestone_wall");
    }

    private static void registerGranite() {
        registerOverworldRock(
                StoneMaterialFamily.GRANITE,
                "granite",
                "polished_granite",
                "granite_stairs",
                "granite_slab",
                "granite_wall",
                "polished_granite_stairs",
                "polished_granite_slab");
    }

    private static void registerCobblestone() {
        Palette palette = palette(StoneMaterialFamily.COBBLESTONE);
        target(palette, Role.RAW_BLOCK, "cobblestone");
        target(palette, Role.RAW_STAIRS, "cobblestone_stairs");
        target(palette, Role.RAW_SLAB, "cobblestone_slab");
        target(palette, Role.RAW_WALL, "cobblestone_wall");
        target(palette, Role.COBBLED_BLOCK, "cobblestone");
        target(palette, Role.COBBLED_STAIRS, "cobblestone_stairs");
        target(palette, Role.COBBLED_SLAB, "cobblestone_slab");
        target(palette, Role.COBBLED_WALL, "cobblestone_wall");
        fallback(palette, Shape.BLOCK, "cobblestone");
        fallback(palette, Shape.STAIRS, "cobblestone_stairs");
        fallback(palette, Shape.SLAB, "cobblestone_slab");
        fallback(palette, Shape.WALL, "cobblestone_wall");
    }

    private static void registerSmoothStone() {
        Palette palette = palette(StoneMaterialFamily.SMOOTH_STONE);
        target(palette, Role.RAW_BLOCK, "smooth_stone");
        target(palette, Role.POLISHED_BLOCK, "smooth_stone");
        target(palette, Role.RAW_SLAB, "smooth_stone_slab");
        target(palette, Role.POLISHED_SLAB, "smooth_stone_slab");
        fallback(palette, Shape.BLOCK, "smooth_stone");
        fallback(palette, Shape.SLAB, "smooth_stone_slab");
    }

    private static void registerDiorite() {
        registerOverworldRock(
                StoneMaterialFamily.DIORITE,
                "diorite",
                "polished_diorite",
                "diorite_stairs",
                "diorite_slab",
                "diorite_wall",
                "polished_diorite_stairs",
                "polished_diorite_slab");
    }

    private static void registerAndesite() {
        registerOverworldRock(
                StoneMaterialFamily.ANDESITE,
                "andesite",
                "polished_andesite",
                "andesite_stairs",
                "andesite_slab",
                "andesite_wall",
                "polished_andesite_stairs",
                "polished_andesite_slab");
    }

    private static void registerOverworldRock(
            StoneMaterialFamily family,
            String raw,
            String polished,
            String stairs,
            String slab,
            String wall,
            String polishedStairs,
            String polishedSlab) {
        Palette palette = palette(family);
        product(palette, Role.RAW_BLOCK, raw);
        product(palette, Role.RAW_STAIRS, stairs);
        product(palette, Role.RAW_SLAB, slab);
        product(palette, Role.RAW_WALL, wall);
        product(palette, Role.POLISHED_BLOCK, polished);
        product(palette, Role.POLISHED_STAIRS, polishedStairs);
        product(palette, Role.POLISHED_SLAB, polishedSlab);
        fallback(palette, Shape.BLOCK, raw);
        fallback(palette, Shape.STAIRS, stairs);
        fallback(palette, Shape.SLAB, slab);
        fallback(palette, Shape.WALL, wall);
    }

    private static void registerDeepslate() {
        Palette palette = palette(StoneMaterialFamily.DEEPSLATE);
        product(palette, Role.PILLAR, "deepslate");
        product(palette, Role.COBBLED_BLOCK, "cobbled_deepslate");
        product(palette, Role.COBBLED_STAIRS, "cobbled_deepslate_stairs");
        product(palette, Role.COBBLED_SLAB, "cobbled_deepslate_slab");
        product(palette, Role.COBBLED_WALL, "cobbled_deepslate_wall");
        product(palette, Role.POLISHED_BLOCK, "polished_deepslate");
        product(palette, Role.POLISHED_STAIRS, "polished_deepslate_stairs");
        product(palette, Role.POLISHED_SLAB, "polished_deepslate_slab");
        product(palette, Role.POLISHED_WALL, "polished_deepslate_wall");
        product(palette, Role.BRICK_BLOCK, "deepslate_bricks");
        product(palette, Role.BRICK_STAIRS, "deepslate_brick_stairs");
        product(palette, Role.BRICK_SLAB, "deepslate_brick_slab");
        product(palette, Role.BRICK_WALL, "deepslate_brick_wall");
        product(palette, Role.TILE_BLOCK, "deepslate_tiles");
        product(palette, Role.TILE_STAIRS, "deepslate_tile_stairs");
        product(palette, Role.TILE_SLAB, "deepslate_tile_slab");
        product(palette, Role.TILE_WALL, "deepslate_tile_wall");
        product(palette, Role.CHISELED_BLOCK, "chiseled_deepslate");
        product(palette, Role.CRACKED_BRICK_BLOCK, "cracked_deepslate_bricks");
        product(palette, Role.CRACKED_TILE_BLOCK, "cracked_deepslate_tiles");
        fallback(palette, Shape.BLOCK, "cobbled_deepslate");
        fallback(palette, Shape.STAIRS, "cobbled_deepslate_stairs");
        fallback(palette, Shape.SLAB, "cobbled_deepslate_slab");
        fallback(palette, Shape.WALL, "cobbled_deepslate_wall");
        fallback(palette, Shape.AXIS_BLOCK, "deepslate");
    }

    private static void registerBlackstone() {
        Palette palette = palette(StoneMaterialFamily.BLACKSTONE);
        product(palette, Role.RAW_BLOCK, "blackstone");
        product(palette, Role.RAW_STAIRS, "blackstone_stairs");
        product(palette, Role.RAW_SLAB, "blackstone_slab");
        product(palette, Role.RAW_WALL, "blackstone_wall");
        product(palette, Role.POLISHED_BLOCK, "polished_blackstone");
        product(palette, Role.POLISHED_STAIRS, "polished_blackstone_stairs");
        product(palette, Role.POLISHED_SLAB, "polished_blackstone_slab");
        product(palette, Role.POLISHED_WALL, "polished_blackstone_wall");
        product(palette, Role.BRICK_BLOCK, "polished_blackstone_bricks");
        product(palette, Role.BRICK_STAIRS, "polished_blackstone_brick_stairs");
        product(palette, Role.BRICK_SLAB, "polished_blackstone_brick_slab");
        product(palette, Role.BRICK_WALL, "polished_blackstone_brick_wall");
        product(palette, Role.CHISELED_BLOCK, "chiseled_polished_blackstone");
        product(palette, Role.CRACKED_BRICK_BLOCK, "cracked_polished_blackstone_bricks");
        sourceOnly(Role.DECORATIVE_BLOCK, "gilded_blackstone");
        fallback(palette, Shape.BLOCK, "blackstone");
        fallback(palette, Shape.STAIRS, "blackstone_stairs");
        fallback(palette, Shape.SLAB, "blackstone_slab");
        fallback(palette, Shape.WALL, "blackstone_wall");
    }

    private static void registerTuff() {
        Palette palette = palette(StoneMaterialFamily.TUFF);
        product(palette, Role.RAW_BLOCK, "tuff");
        product(palette, Role.RAW_STAIRS, "tuff_stairs");
        product(palette, Role.RAW_SLAB, "tuff_slab");
        product(palette, Role.RAW_WALL, "tuff_wall");
        product(palette, Role.POLISHED_BLOCK, "polished_tuff");
        product(palette, Role.POLISHED_STAIRS, "polished_tuff_stairs");
        product(palette, Role.POLISHED_SLAB, "polished_tuff_slab");
        product(palette, Role.POLISHED_WALL, "polished_tuff_wall");
        product(palette, Role.BRICK_BLOCK, "tuff_bricks");
        product(palette, Role.BRICK_STAIRS, "tuff_brick_stairs");
        product(palette, Role.BRICK_SLAB, "tuff_brick_slab");
        product(palette, Role.BRICK_WALL, "tuff_brick_wall");
        product(palette, Role.CHISELED_BLOCK, "chiseled_tuff");
        product(palette, Role.CHISELED_BRICK_BLOCK, "chiseled_tuff_bricks");
        fallback(palette, Shape.BLOCK, "tuff");
        fallback(palette, Shape.STAIRS, "tuff_stairs");
        fallback(palette, Shape.SLAB, "tuff_slab");
        fallback(palette, Shape.WALL, "tuff_wall");
    }

    private static void registerQuartz() {
        Palette palette = palette(StoneMaterialFamily.QUARTZ);
        product(palette, Role.RAW_BLOCK, "quartz_block");
        product(palette, Role.RAW_STAIRS, "quartz_stairs");
        product(palette, Role.RAW_SLAB, "quartz_slab");
        product(palette, Role.POLISHED_BLOCK, "smooth_quartz");
        product(palette, Role.POLISHED_STAIRS, "smooth_quartz_stairs");
        product(palette, Role.POLISHED_SLAB, "smooth_quartz_slab");
        product(palette, Role.BRICK_BLOCK, "quartz_bricks");
        product(palette, Role.CHISELED_BLOCK, "chiseled_quartz_block");
        product(palette, Role.PILLAR, "quartz_pillar");
        fallback(palette, Shape.BLOCK, "quartz_block");
        fallback(palette, Shape.STAIRS, "quartz_stairs");
        fallback(palette, Shape.SLAB, "quartz_slab");
        fallback(palette, Shape.AXIS_BLOCK, "quartz_pillar");
    }

    private static Palette palette(StoneMaterialFamily family) {
        Palette palette = new Palette();
        PALETTES.put(family, palette);
        return palette;
    }

    private static void product(Palette palette, Role role, String path) {
        String id = minecraft(path);
        palette.byRole.put(role, id);
        SOURCES.put(id, new SourceProduct(role, role.shape));
    }

    private static void target(Palette palette, Role role, String path) {
        palette.byRole.put(role, minecraft(path));
    }

    private static void sourceOnly(Role role, String path) {
        SOURCES.put(minecraft(path), new SourceProduct(role, role.shape));
    }

    private static void fallback(Palette palette, Shape shape, String path) {
        palette.fallbackByShape.put(shape, minecraft(path));
    }

    private static void buildAllowedTargets() {
        for (Shape shape : Shape.values()) {
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            for (Palette palette : PALETTES.values()) {
                for (Map.Entry<Role, String> entry : palette.byRole.entrySet()) {
                    if (entry.getKey().shape == shape) {
                        ids.add(entry.getValue());
                    }
                }
                String fallback = palette.fallbackByShape.get(shape);
                if (fallback != null) {
                    ids.add(fallback);
                }
            }
            ALLOWED_BY_SHAPE.put(shape, List.copyOf(new ArrayList<>(ids)));
        }
    }

    private static String minecraft(String path) {
        return "minecraft:" + path;
    }

    public record Match(String targetId, boolean exactRole, List<String> allowedTargetIds) {
    }

    private record SourceProduct(Role role, Shape shape) {
    }

    private static final class Palette {
        private final EnumMap<Role, String> byRole = new EnumMap<>(Role.class);
        private final EnumMap<Shape, String> fallbackByShape = new EnumMap<>(Shape.class);
    }

    private enum Shape {
        BLOCK,
        STAIRS,
        SLAB,
        WALL,
        AXIS_BLOCK
    }

    private enum Role {
        RAW_BLOCK(Shape.BLOCK), RAW_STAIRS(Shape.STAIRS), RAW_SLAB(Shape.SLAB), RAW_WALL(Shape.WALL),
        COBBLED_BLOCK(Shape.BLOCK), COBBLED_STAIRS(Shape.STAIRS), COBBLED_SLAB(Shape.SLAB), COBBLED_WALL(Shape.WALL),
        POLISHED_BLOCK(Shape.BLOCK), POLISHED_STAIRS(Shape.STAIRS), POLISHED_SLAB(Shape.SLAB), POLISHED_WALL(Shape.WALL),
        BRICK_BLOCK(Shape.BLOCK), BRICK_STAIRS(Shape.STAIRS), BRICK_SLAB(Shape.SLAB), BRICK_WALL(Shape.WALL),
        TILE_BLOCK(Shape.BLOCK), TILE_STAIRS(Shape.STAIRS), TILE_SLAB(Shape.SLAB), TILE_WALL(Shape.WALL),
        CHISELED_BLOCK(Shape.BLOCK), CHISELED_BRICK_BLOCK(Shape.BLOCK),
        CRACKED_BRICK_BLOCK(Shape.BLOCK), CRACKED_TILE_BLOCK(Shape.BLOCK), DECORATIVE_BLOCK(Shape.BLOCK),
        MOSSY_COBBLED_BLOCK(Shape.BLOCK), MOSSY_COBBLED_STAIRS(Shape.STAIRS),
        MOSSY_COBBLED_SLAB(Shape.SLAB), MOSSY_COBBLED_WALL(Shape.WALL),
        MOSSY_BRICK_BLOCK(Shape.BLOCK), MOSSY_BRICK_STAIRS(Shape.STAIRS),
        MOSSY_BRICK_SLAB(Shape.SLAB), MOSSY_BRICK_WALL(Shape.WALL),
        PILLAR(Shape.AXIS_BLOCK);

        private final Shape shape;

        Role(Shape shape) {
            this.shape = shape;
        }
    }
}
