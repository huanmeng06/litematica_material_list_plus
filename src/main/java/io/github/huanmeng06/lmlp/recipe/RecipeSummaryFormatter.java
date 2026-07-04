package io.github.huanmeng06.lmlp.recipe;

import java.util.List;
import java.util.StringJoiner;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.huanmeng06.lmlp.material.CountFormatter;

public final class RecipeSummaryFormatter {
    private RecipeSummaryFormatter() {
    }

    public static String header(RecipeSummary summary, int index) {
        return headerBase(summary, index) + " / " + headerMissing(summary);
    }

    // The " / missing" suffix carries color codes as a single unit; callers
    // that need to fit this header into a narrow width should fall back to
    // just the plain base text (or wrap the two onto separate lines) rather
    // than truncating mid-color-code.
    public static String headerBase(RecipeSummary summary, int index) {
        return StringUtils.translate("lmlp.label.recipe.header", index, summary.outputCount(), summary.craftsTotal());
    }

    public static String headerMissing(RecipeSummary summary) {
        String missing = StringUtils.translate("lmlp.label.recipe.header.missing", summary.craftsMissing());
        if (summary.craftsMissing() == 0) {
            missing = GuiBase.TXT_GREEN + missing + GuiBase.TXT_RST;
        }
        return missing;
    }

    public static String ingredientName(IngredientSummary ingredient) {
        List<String> alternatives = ingredient.alternatives();
        String shortName = AlternativeItemDisplay.name(ingredient.icons(), alternatives);
        if (!shortName.isEmpty()) {
            return shortName;
        }

        return joinedAlternatives(alternatives);
    }

    public static String slotName(RecipeSlotSummary slot) {
        List<String> alternatives = slot.alternatives();
        String shortName = AlternativeItemDisplay.name(slot.icons(), alternatives);
        if (!shortName.isEmpty()) {
            return shortName;
        }

        return joinedAlternatives(alternatives);
    }

    public static String totalCount(IngredientSummary ingredient) {
        return CountFormatter.format(ingredient.countTotal(), ingredient.maxStackSize());
    }

    public static String missingCount(IngredientSummary ingredient) {
        return CountFormatter.format(ingredient.countMissing(), ingredient.maxStackSize());
    }

    private static String joinedAlternatives(List<String> alternatives) {
        if (alternatives.isEmpty()) {
            return "";
        }

        if (alternatives.size() == 1) {
            return alternatives.get(0);
        }

        StringJoiner joiner = new StringJoiner(" / ");
        int limit = Math.min(alternatives.size(), 4);
        for (int i = 0; i < limit; i++) {
            joiner.add(alternatives.get(i));
        }

        if (alternatives.size() > limit) {
            joiner.add("...");
        }

        return StringUtils.translate("lmlp.label.recipe.any", joiner.toString());
    }
}
