package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import io.github.huanmeng06.lmlp.config.BedMaterial;
import io.github.huanmeng06.lmlp.config.CandleMaterial;
import io.github.huanmeng06.lmlp.config.ConcreteMaterial;
import io.github.huanmeng06.lmlp.config.ConcretePowderMaterial;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.CarpetMaterial;
import io.github.huanmeng06.lmlp.config.GlassMaterial;
import io.github.huanmeng06.lmlp.config.GlazedTerracottaMaterial;
import io.github.huanmeng06.lmlp.config.StoneMaterialFamily;
import io.github.huanmeng06.lmlp.config.ShulkerBoxMaterial;
import io.github.huanmeng06.lmlp.config.TerracottaMaterial;
import io.github.huanmeng06.lmlp.config.WoodFamily;
import io.github.huanmeng06.lmlp.config.WoolMaterial;

import java.util.ArrayList;
import java.util.List;

/** Defines the exact JEI item whitelist and resulting value for picker-backed option lists. */
public final class RestrictedJeiOptionListConfigs {
    private static final Definition PREFERRED_WOOD = createPreferredWoodDefinition();
    private static final Definition PREFERRED_STONE = createPreferredStoneDefinition();
    private static final Definition PREFERRED_GLASS = createPreferredGlassDefinition();
    private static final Definition PREFERRED_WOOL = createPreferredWoolDefinition();
    private static final Definition PREFERRED_CARPET = createPreferredCarpetDefinition();
    private static final Definition PREFERRED_TERRACOTTA = createPreferredTerracottaDefinition();
    private static final Definition PREFERRED_GLAZED_TERRACOTTA = createPreferredGlazedTerracottaDefinition();
    private static final Definition PREFERRED_CONCRETE = createPreferredConcreteDefinition();
    private static final Definition PREFERRED_CONCRETE_POWDER = createPreferredConcretePowderDefinition();
    private static final Definition PREFERRED_BED = createPreferredBedDefinition();
    private static final Definition PREFERRED_CANDLE = createPreferredCandleDefinition();
    private static final Definition PREFERRED_SHULKER_BOX = createPreferredShulkerBoxDefinition();
    private static final List<Definition> DEFINITIONS = List.of(
            PREFERRED_WOOD,
            PREFERRED_STONE,
            PREFERRED_GLASS,
            PREFERRED_WOOL,
            PREFERRED_CARPET,
            PREFERRED_TERRACOTTA,
            PREFERRED_GLAZED_TERRACOTTA,
            PREFERRED_CONCRETE,
            PREFERRED_CONCRETE_POWDER,
            PREFERRED_BED,
            PREFERRED_CANDLE,
            PREFERRED_SHULKER_BOX
    );

    private RestrictedJeiOptionListConfigs() {
    }

    public static Definition find(IConfigOptionList config) {
        for (Definition definition : DEFINITIONS) {
            if (definition.config() == config) {
                return definition;
            }
        }
        return null;
    }

    private static Definition createPreferredWoodDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (WoodFamily family : WoodFamily.values()) {
            choices.add(new Choice(representativeItemId(family), family));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_WOOD_FAMILY, List.copyOf(choices));
    }

    private static Definition createPreferredGlassDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (GlassMaterial material : GlassMaterial.values()) {
            choices.add(new Choice(material.blockId(), material));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_GLASS_MATERIAL, List.copyOf(choices));
    }

    private static Definition createPreferredStoneDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (StoneMaterialFamily family : StoneMaterialFamily.values()) {
            choices.add(new Choice(family.representativeBlockId(), family));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_STONE_FAMILY, List.copyOf(choices));
    }

    private static Definition createPreferredCarpetDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (CarpetMaterial material : CarpetMaterial.values()) {
            choices.add(new Choice(material.blockId(), material));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_CARPET_MATERIAL, List.copyOf(choices));
    }

    private static Definition createPreferredWoolDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (WoolMaterial material : WoolMaterial.values()) {
            choices.add(new Choice(material.blockId(), material));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_WOOL_MATERIAL, List.copyOf(choices));
    }

    private static Definition createPreferredTerracottaDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (TerracottaMaterial material : TerracottaMaterial.values()) {
            choices.add(new Choice(material.blockId(), material));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_TERRACOTTA_MATERIAL, List.copyOf(choices));
    }

    private static Definition createPreferredGlazedTerracottaDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (GlazedTerracottaMaterial material : GlazedTerracottaMaterial.values()) {
            choices.add(new Choice(material.blockId(), material));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_MATERIAL, List.copyOf(choices));
    }

    private static Definition createPreferredConcreteDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (ConcreteMaterial material : ConcreteMaterial.values()) {
            choices.add(new Choice(material.blockId(), material));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_CONCRETE_MATERIAL, List.copyOf(choices));
    }

    private static Definition createPreferredConcretePowderDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (ConcretePowderMaterial material : ConcretePowderMaterial.values()) {
            choices.add(new Choice(material.blockId(), material));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_CONCRETE_POWDER_MATERIAL, List.copyOf(choices));
    }

    private static Definition createPreferredBedDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (BedMaterial material : BedMaterial.values()) {
            choices.add(new Choice(material.blockId(), material));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_BED_MATERIAL, List.copyOf(choices));
    }

    private static Definition createPreferredCandleDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (CandleMaterial material : CandleMaterial.values()) {
            choices.add(new Choice(material.blockId(), material));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_CANDLE_MATERIAL, List.copyOf(choices));
    }

    private static Definition createPreferredShulkerBoxDefinition() {
        List<Choice> choices = new ArrayList<>();
        for (ShulkerBoxMaterial material : ShulkerBoxMaterial.values()) {
            choices.add(new Choice(material.blockId(), material));
        }
        return new Definition(Configs.ConfigForms.PREFERRED_SHULKER_BOX_MATERIAL, List.copyOf(choices));
    }

    static String representativeItemId(WoodFamily family) {
        return switch (family) {
            case BAMBOO -> "minecraft:bamboo_block";
            case CRIMSON -> "minecraft:crimson_stem";
            case WARPED -> "minecraft:warped_stem";
            default -> "minecraft:" + family.id() + "_log";
        };
    }

    public record Definition(IConfigOptionList config, List<Choice> choices) {
        public List<String> allowedItemIds() {
            return this.choices.stream().map(Choice::itemId).toList();
        }

        public boolean select(String itemId) {
            for (Choice choice : this.choices) {
                if (choice.itemId().equals(itemId)) {
                    this.config.setOptionListValue(choice.value());
                    return true;
                }
            }
            return false;
        }

        public String selectedItemId() {
            IConfigOptionListEntry selected = this.config.getOptionListValue();
            for (Choice choice : this.choices) {
                if (choice.value().equals(selected)) {
                    return choice.itemId();
                }
            }
            return this.choices.isEmpty() ? "" : this.choices.get(0).itemId();
        }
    }

    public record Choice(String itemId, IConfigOptionListEntry value) {
    }
}
