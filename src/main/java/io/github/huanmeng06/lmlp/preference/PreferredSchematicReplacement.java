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
import net.minecraft.class_2248;
import net.minecraft.class_2382;
import net.minecraft.class_2680;
import net.minecraft.class_2769;
import net.minecraft.class_2960;
import net.minecraft.class_6760;
import net.minecraft.class_7923;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
            class_2382 size = container.getSize();
            for (int y = 0; y < size.method_10264(); y++) {
                for (int z = 0; z < size.method_10260(); z++) {
                    for (int x = 0; x < size.method_10263(); x++) {
                        class_2680 state = container.get(x, y, z);
                        String sourceId = blockId(state.method_26204());
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
                        if (row.targetBlock != null && !isExactStateMapping(state, row.targetBlock.method_9564())) {
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
        LitematicaSchematic copy = createDeepCopy(original);
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
            class_2382 size = container.getSize();
            for (int y = 0; y < size.method_10264(); y++) {
                for (int z = 0; z < size.method_10260(); z++) {
                    for (int x = 0; x < size.method_10263(); x++) {
                        class_2680 state = container.get(x, y, z);
                        ReplacementChoice choice = bySourceId.get(blockId(state.method_26204()));
                        if (choice == null) {
                            continue;
                        }
                        class_2680 mapped = mapState(state, choice.targetBlock().method_9564(), choice.mode() == ReplacementMode.FORCE);
                        if (mapped != null) {
                            container.set(x, y, z, mapped);
                        }
                    }
                }
            }
            copy.getScheduledBlockTicksForRegion(region).replaceAll((position, tick) -> {
                ReplacementChoice choice = bySourceId.get(blockId(tick.comp_252()));
                if (choice == null) {
                    return tick;
                }
                return new class_6760<>(choice.targetBlock(), tick.comp_253(), tick.comp_254(), tick.comp_255(), tick.comp_256());
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
            class_2680 sampleState) {
        class_2248 source = sampleState.method_26204();
        class_2248 target = block(targetId);
        boolean exact = target != null && isExactStateMapping(sampleState, target.method_9564());
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

    private static class_2680 mapState(class_2680 source, class_2680 target, boolean force) {
        class_2680 result = target;
        Map<String, class_2769<?>> targetProperties = new LinkedHashMap<>();
        for (class_2769<?> property : target.method_28501()) {
            targetProperties.put(property.method_11899(), property);
        }

        for (Map.Entry<class_2769<?>, Comparable<?>> entry : source.method_11656().entrySet()) {
            class_2769<?> targetProperty = targetProperties.get(entry.getKey().method_11899());
            if (targetProperty == null) {
                if (!force) {
                    return null;
                }
                continue;
            }
            String valueName = propertyValueName(entry.getKey(), entry.getValue());
            Comparable<?> targetValue = targetProperty.method_11900(valueName).orElse(null);
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

    private static boolean isExactStateMapping(class_2680 source, class_2680 target) {
        return mapState(source, target, false) != null
                && source.method_28501().size() == target.method_28501().size();
    }

    public static boolean isExactReplacement(class_2248 source, class_2248 target) {
        return source != null
                && target != null
                && isExactStateMapping(source.method_9564(), target.method_9564());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static class_2680 withProperty(class_2680 state, class_2769 property, Comparable value) {
        return state.method_11657(property, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValueName(class_2769 property, Comparable value) {
        return property.method_11901(value);
    }

    private static class_2248 block(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return class_7923.field_41175.method_17966(new class_2960(id)).orElse(null);
    }

    private static LitematicaSchematic createDeepCopy(LitematicaSchematic original) {
        Path directory = null;
        boolean wasModified = original.getMetadata().wasModifiedSinceSaved();
        try {
            directory = Files.createTempDirectory("lmlp-preferred-");
            String name = "source";
            if (!original.writeToFile(directory.toFile(), name, true)) {
                throw new IllegalStateException("Failed to serialize schematic copy");
            }
            LitematicaSchematic copy = LitematicaSchematic.createFromFile(
                    directory.toFile(),
                    name + LitematicaSchematic.FILE_EXTENSION);
            if (copy == null) {
                throw new IllegalStateException("Failed to load schematic copy");
            }
            return copy;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create schematic copy", exception);
        } finally {
            if (wasModified) {
                original.getMetadata().setModifiedSinceSaved();
            } else {
                original.getMetadata().clearModifiedSinceSaved();
            }
            if (directory != null) {
                try {
                    Files.deleteIfExists(directory.resolve("source" + LitematicaSchematic.FILE_EXTENSION));
                    Files.deleteIfExists(directory);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static String blockId(class_2248 block) {
        return class_7923.field_41175.method_10221(block).toString();
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
            class_2248 sourceBlock,
            class_2248 targetBlock,
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

    public record ReplacementChoice(String sourceId, String targetId, class_2248 targetBlock, ReplacementMode mode) {
    }

    private static final class MutableRow {
        private final PreferredMaterialCategory category;
        private final String sourceId;
        private final String targetId;
        private final class_2248 sourceBlock;
        private final class_2248 targetBlock;
        private final boolean roleExact;
        private final List<ReplacementCandidate> allowedTargets;
        private boolean exact;
        private int count;

        private MutableRow(
                PreferredMaterialCategory category,
                String sourceId,
                String targetId,
                class_2248 sourceBlock,
                class_2248 targetBlock,
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
            String sourceName = this.sourceBlock == null ? this.sourceId : this.sourceBlock.method_9518().getString();
            String targetName = this.targetBlock == null ? "" : this.targetBlock.method_9518().getString();
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
