package io.github.huanmeng06.lmlp.preference;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.util.FileType;
import io.github.huanmeng06.lmlp.config.CarpetMaterial;
import io.github.huanmeng06.lmlp.config.GlassMaterial;
import io.github.huanmeng06.lmlp.config.GlazedTerracottaMaterial;
import io.github.huanmeng06.lmlp.config.StoneMaterialFamily;
import io.github.huanmeng06.lmlp.config.TerracottaMaterial;
import io.github.huanmeng06.lmlp.config.WoodFamily;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.ticks.ScheduledTick;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PreferredSchematicReplacement {
    private PreferredSchematicReplacement() {
    }

    public static List<ReplacementRow> scan(LitematicaSchematic schematic, Targets targets) {
        Map<String, MutableRow> rows = new LinkedHashMap<>();
        for (String region : schematic.getAreaSizes().keySet()) {
            LitematicaBlockStateContainer container = schematic.getSubRegionContainer(region);
            if (container == null) {
                continue;
            }
            Vec3i size = container.getSize();
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    for (int x = 0; x < size.getX(); x++) {
                        BlockState state = container.get(x, y, z);
                        String sourceId = blockId(state.getBlock());
                        ReplacementTarget target = targets.targetFor(sourceId);
                        if (target == null || sourceId.equals(target.blockId())) {
                            continue;
                        }
                        String targetId = target.blockId() == null ? "" : target.blockId();
                        String key = sourceId + "\u0000" + targetId;
                        MutableRow row = rows.computeIfAbsent(
                                key,
                                ignored -> createRow(
                                        target.category(),
                                        sourceId,
                                        target.blockId(),
                                        target.roleExact(),
                                        target.allowedTargets(),
                                        state));
                        row.count++;
                        if (row.targetBlock != null && !isExactStateMapping(state, row.targetBlock.defaultBlockState())) {
                            row.exact = false;
                        }
                    }
                }
            }
        }

        return rows.values().stream()
                .map(MutableRow::freeze)
                .sorted(Comparator.comparing(ReplacementRow::sourceName))
                .toList();
    }

    public static LitematicaSchematic createCopy(LitematicaSchematic original, Collection<ReplacementChoice> choices) {
        LitematicaSchematic copy = new LitematicaSchematic(null, original.writeToNBT(), FileType.LITEMATICA_SCHEMATIC);
        Map<String, ReplacementChoice> bySourceId = new LinkedHashMap<>();
        for (ReplacementChoice choice : choices) {
            if (choice.mode() != ReplacementMode.SKIP && choice.targetBlock() != null) {
                bySourceId.put(choice.sourceId(), choice);
            }
        }

        for (String region : copy.getAreaSizes().keySet()) {
            LitematicaBlockStateContainer container = copy.getSubRegionContainer(region);
            if (container == null) {
                continue;
            }
            Vec3i size = container.getSize();
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    for (int x = 0; x < size.getX(); x++) {
                        BlockState state = container.get(x, y, z);
                        ReplacementChoice choice = bySourceId.get(blockId(state.getBlock()));
                        if (choice == null) {
                            continue;
                        }
                        BlockState mapped = mapState(state, choice.targetBlock().defaultBlockState(), choice.mode() == ReplacementMode.FORCE);
                        if (mapped != null) {
                            container.set(x, y, z, mapped);
                        }
                    }
                }
            }
            copy.getScheduledBlockTicksForRegion(region).replaceAll((position, tick) -> {
                ReplacementChoice choice = bySourceId.get(blockId(tick.type()));
                if (choice == null) {
                    return tick;
                }
                return new ScheduledTick<>(choice.targetBlock(), tick.pos(), tick.triggerTick(), tick.priority(), tick.subTickOrder());
            });
        }

        copy.getMetadata().setName(original.getMetadata().getName() + " - " + targetSuffix());
        copy.getMetadata().setTimeModifiedToNow();
        copy.getMetadata().setModifiedSinceSaved();
        return copy;
    }

    private static MutableRow createRow(
            PreferredMaterialCategory category,
            String sourceId,
            String targetId,
            boolean roleExact,
            List<ReplacementCandidate> allowedTargets,
            BlockState sampleState) {
        Block source = sampleState.getBlock();
        Block target = block(targetId);
        boolean exact = target != null && isExactStateMapping(sampleState, target.defaultBlockState());
        return new MutableRow(
                category,
                sourceId,
                targetId,
                source,
                target,
                exact,
                roleExact,
                List.copyOf(allowedTargets));
    }

    private static BlockState mapState(BlockState source, BlockState target, boolean force) {
        BlockState result = target;
        Map<String, Property<?>> targetProperties = new LinkedHashMap<>();
        for (Property<?> property : target.getProperties()) {
            targetProperties.put(property.getName(), property);
        }

        for (Property.Value<?> entry : source.getValues().toList()) {
            Property<?> targetProperty = targetProperties.get(entry.property().getName());
            if (targetProperty == null) {
                if (!force) {
                    return null;
                }
                continue;
            }
            String valueName = entry.valueName();
            Comparable<?> targetValue = targetProperty.getValue(valueName).orElse(null);
            if (targetValue == null) {
                if (!force) {
                    return null;
                }
                continue;
            }
            result = withProperty(result, targetProperty, targetValue);
        }
        return result;
    }

    private static boolean isExactStateMapping(BlockState source, BlockState target) {
        return mapState(source, target, false) != null
                && source.getProperties().size() == target.getProperties().size();
    }

    public static boolean isExactReplacement(Block source, Block target) {
        return source != null
                && target != null
                && isExactStateMapping(source.defaultBlockState(), target.defaultBlockState());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState withProperty(BlockState state, Property property, Comparable value) {
        return state.setValue(property, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValueName(Property property, Comparable value) {
        return property.getName(value);
    }

    private static Block block(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(null);
    }

    private static String blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static String targetSuffix() {
        return "Preferred";
    }

    public enum ReplacementMode {
        REPLACE,
        SKIP,
        FORCE
    }

    public record ReplacementRow(
            PreferredMaterialCategory category,
            String sourceId,
            String targetId,
            Block sourceBlock,
            Block targetBlock,
            String sourceName,
            String targetName,
            int count,
            boolean exact,
            boolean roleExact,
            List<ReplacementCandidate> allowedTargets
    ) {
        public ReplacementChoice choice(ReplacementMode mode) {
            return new ReplacementChoice(this.sourceId, this.targetId, this.targetBlock, mode);
        }
    }

    public record ReplacementChoice(String sourceId, String targetId, Block targetBlock, ReplacementMode mode) {
    }

    private static final class MutableRow {
        private final PreferredMaterialCategory category;
        private final String sourceId;
        private final String targetId;
        private final Block sourceBlock;
        private final Block targetBlock;
        private final boolean roleExact;
        private final List<ReplacementCandidate> allowedTargets;
        private boolean exact;
        private int count;

        private MutableRow(
                PreferredMaterialCategory category,
                String sourceId,
                String targetId,
                Block sourceBlock,
                Block targetBlock,
                boolean exact,
                boolean roleExact,
                List<ReplacementCandidate> allowedTargets) {
            this.category = category;
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.sourceBlock = sourceBlock;
            this.targetBlock = targetBlock;
            this.exact = exact;
            this.roleExact = roleExact;
            this.allowedTargets = allowedTargets;
        }

        private ReplacementRow freeze() {
            String sourceName = this.sourceBlock == null ? this.sourceId : this.sourceBlock.getName().getString();
            String targetName = this.targetBlock == null ? "" : this.targetBlock.getName().getString();
            return new ReplacementRow(this.category, this.sourceId, this.targetId, this.sourceBlock, this.targetBlock,
                    sourceName, targetName, this.count, this.exact && this.targetBlock != null,
                    this.roleExact, this.allowedTargets);
        }
    }

    public enum PreferredMaterialCategory {
        WOOD,
        STONE,
        GLASS,
        CARPET,
        TERRACOTTA,
        GLAZED_TERRACOTTA
    }

    public record Targets(
            WoodFamily wood,
            StoneMaterialFamily stone,
            GlassMaterial glass,
            CarpetMaterial carpet,
            TerracottaMaterial terracotta,
            GlazedTerracottaMaterial glazedTerracotta) {

        private ReplacementTarget targetFor(String sourceId) {
            if (this.wood != null) {
                WoodBlockFamilies.ReplacementTarget woodTarget = WoodBlockFamilies.targetFor(sourceId, this.wood);
                if (woodTarget != null) {
                    List<ReplacementCandidate> candidates = WoodBlockFamilies.targetsForSource(sourceId).stream()
                            .map(target -> new ReplacementCandidate(target.itemId(), target.blockId()))
                            .toList();
                    return ReplacementTarget.exact(
                            PreferredMaterialCategory.WOOD,
                            woodTarget.blockId(),
                            candidates);
                }
            }

            if (this.stone != null) {
                StoneMaterialMappings.Match match = StoneMaterialMappings.match(sourceId, this.stone);
                if (match != null) {
                    return new ReplacementTarget(
                            PreferredMaterialCategory.STONE,
                            match.targetId(),
                            match.exactRole(),
                            directCandidates(match.allowedTargetIds()));
                }
            }

            if (this.glass != null) {
                for (GlassMaterial material : GlassMaterial.values()) {
                    if (sourceId.equals(material.blockId())) {
                        return ReplacementTarget.exact(
                                PreferredMaterialCategory.GLASS,
                                this.glass.blockId(),
                                directCandidates(java.util.Arrays.stream(GlassMaterial.values())
                                        .map(GlassMaterial::blockId)
                                        .toList()));
                    }
                    if (sourceId.equals(material.paneId())) {
                        return ReplacementTarget.exact(
                                PreferredMaterialCategory.GLASS,
                                this.glass.paneId(),
                                directCandidates(java.util.Arrays.stream(GlassMaterial.values())
                                        .map(GlassMaterial::paneId)
                                        .toList()));
                    }
                }
            }

            if (this.carpet != null) {
                for (CarpetMaterial material : CarpetMaterial.values()) {
                    if (sourceId.equals(material.blockId())) {
                        return ReplacementTarget.exact(
                                PreferredMaterialCategory.CARPET,
                                this.carpet.blockId(),
                                directCandidates(java.util.Arrays.stream(CarpetMaterial.values())
                                        .map(CarpetMaterial::blockId)
                                        .toList()));
                    }
                }
            }

            if (this.terracotta != null) {
                for (TerracottaMaterial material : TerracottaMaterial.values()) {
                    if (sourceId.equals(material.blockId())) {
                        return ReplacementTarget.exact(
                                PreferredMaterialCategory.TERRACOTTA,
                                this.terracotta.blockId(),
                                directCandidates(java.util.Arrays.stream(TerracottaMaterial.values())
                                        .map(TerracottaMaterial::blockId)
                                        .toList()));
                    }
                }
            }

            if (this.glazedTerracotta != null) {
                for (GlazedTerracottaMaterial material : GlazedTerracottaMaterial.values()) {
                    if (sourceId.equals(material.blockId())) {
                        return ReplacementTarget.exact(
                                PreferredMaterialCategory.GLAZED_TERRACOTTA,
                                this.glazedTerracotta.blockId(),
                                directCandidates(java.util.Arrays.stream(GlazedTerracottaMaterial.values())
                                        .map(GlazedTerracottaMaterial::blockId)
                                        .toList()));
                    }
                }
            }
            return null;
        }
    }

    private record ReplacementTarget(
            PreferredMaterialCategory category,
            String blockId,
            boolean roleExact,
            List<ReplacementCandidate> allowedTargets) {
        private static ReplacementTarget exact(
                PreferredMaterialCategory category,
                String blockId,
                List<ReplacementCandidate> allowedTargets) {
            return new ReplacementTarget(category, blockId, true, allowedTargets);
        }
    }

    public record ReplacementCandidate(String itemId, String targetId) {
    }

    private static List<ReplacementCandidate> directCandidates(Collection<String> ids) {
        return ids.stream().map(id -> new ReplacementCandidate(id, id)).toList();
    }
}
