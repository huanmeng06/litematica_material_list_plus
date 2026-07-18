package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import io.github.huanmeng06.lmlp.config.Configs;
import io.github.huanmeng06.lmlp.config.WoodFamily;

import java.util.ArrayList;
import java.util.List;

/** Defines the exact JEI item whitelist and resulting value for picker-backed option lists. */
public final class RestrictedJeiOptionListConfigs {
    private static final Definition PREFERRED_WOOD = createPreferredWoodDefinition();
    private static final List<Definition> DEFINITIONS = List.of(PREFERRED_WOOD);

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

    private static String representativeItemId(WoodFamily family) {
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
    }

    public record Choice(String itemId, IConfigOptionListEntry value) {
    }
}
