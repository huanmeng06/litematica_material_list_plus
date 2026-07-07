package io.github.huanmeng06.lmlp.material;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.class_1799;

/**
 * Time-based icon cycling for choice-group ("任意X") rows, grouped by wood
 * family so parallel lists stay material-matched.
 *
 * <p>A choice group like 任意木板 carries one icon per wood family (oak_planks,
 * spruce_planks, …), while its decomposed 任意原木 child carries several per
 * family (oak_log, oak_wood, stripped_oak_log, stripped_oak_wood, then the
 * spruce set, …) — the icon lists are already grouped by family in the same
 * family order (see MaterialTreeBuilder.buildChoiceGroupChildren).
 *
 * <p>Rather than advancing one icon per fixed interval (which drifts out of
 * sync when families have different variant counts), the cycle spends a fixed
 * window on each family and subdivides that window evenly among the family's
 * variants. Because both lists share the same family order and the same window
 * length, the planks icon and the log icon always sit on the SAME family at the
 * same instant — the materials line up no matter how many log variants a family
 * has (1 planks : N logs).
 *
 * <p>Icons whose id doesn't map to a wood family (e.g. 沙子/红沙) each form their
 * own single-item group, so a non-wood list simply advances one icon per family
 * window — equivalent to a plain cycle at the family interval.
 */
public final class FamilyIconCycle {
    // Order doesn't affect grouping (grouping keys off the family stem), only
    // which stem a path is attributed to. Longer/more-specific stems first so
    // e.g. "dark_oak" wins over "oak".
    private static final List<String> WOOD_FAMILIES = List.of(
            "dark_oak",
            "pale_oak",
            "oak",
            "spruce",
            "birch",
            "jungle",
            "acacia",
            "mangrove",
            "cherry",
            "bamboo",
            "crimson",
            "warped");

    /**
     * How long each wood family stays on screen before the cycle advances to
     * the next family. Shared by every call site so parallel "任意X" rows in
     * different UI contexts (recipe panel, minimal sub-material target rows,
     * inline "所需" upstream icons) all advance families on the SAME clock and
     * stay material-matched. Do not redefine this per file — a mismatch here is
     * exactly what desynced the target row (2s) from the inline row (1s).
     */
    public static final long FAMILY_WINDOW_MILLIS = 1000L;

    /**
     * Per-icon step used when a list has no wood families to group by (e.g.
     * 沙子/红沙): cycle the whole list one icon per this interval. Kept equal to
     * {@link #FAMILY_WINDOW_MILLIS} so every choice-group row — wood-family or
     * not — visibly changes at the same cadence (a non-wood group flickering
     * faster/slower than a wood-family group next to it reads as a bug).
     */
    public static final long FALLBACK_STEP_MILLIS = FAMILY_WINDOW_MILLIS;

    private FamilyIconCycle() {
    }

    /**
     * Pick the icon to show at {@code nowMillis}.
     *
     * @param icons              the flat, family-grouped icon list
     * @param nowMillis          current time
     * @param familyWindowMillis how long each family stays on screen
     * @param fallbackStepMillis per-icon step used when the list has no wood
     *                           families to group by (single mixed group)
     */
    public static class_1799 pick(List<class_1799> icons, long nowMillis, long familyWindowMillis, long fallbackStepMillis) {
        if (icons.isEmpty()) {
            return class_1799.field_8037;
        }
        if (icons.size() == 1) {
            return icons.get(0);
        }

        // Fall back to a plain one-icon-per-step cycle ONLY when the list has
        // no wood families at all (e.g. 沙子/红沙). A list that DOES carry wood
        // families must use the family window even if every family has just one
        // variant — that's exactly the 任意木板 list (one planks per family),
        // and it has to advance families on the same 2s clock as its 任意原木
        // child (several logs per family) so the two stay material-matched.
        // Keying the fallback off "groups.size() == icons.size()" was the bug:
        // the planks list has one icon per family, so every group was size 1
        // and it wrongly fell back to the 900ms cycle while the logs list ran
        // on the 2s family window — the two drifted apart.
        boolean hasWoodFamily = false;
        for (class_1799 stack : icons) {
            if (!familyOf(stack).isEmpty()) {
                hasWoodFamily = true;
                break;
            }
        }
        if (!hasWoodFamily) {
            int index = (int) (Math.floorDiv(nowMillis, fallbackStepMillis) % icons.size());
            return icons.get(index);
        }

        // Contiguous runs of the same family become one group. Non-wood icons
        // ("") each stand alone as their own single-variant group.
        List<int[]> groups = new ArrayList<>();
        int runStart = 0;
        String runFamily = familyOf(icons.get(0));
        for (int i = 1; i <= icons.size(); i++) {
            String family = i < icons.size() ? familyOf(icons.get(i)) : null;
            boolean sameRun = i < icons.size() && !family.isEmpty() && family.equals(runFamily) && !runFamily.isEmpty();
            if (!sameRun) {
                groups.add(new int[] {runStart, i});
                runStart = i;
                runFamily = family;
            }
        }

        long windowIndex = Math.floorDiv(nowMillis, familyWindowMillis);
        int groupIndex = (int) (windowIndex % groups.size());
        int[] group = groups.get(groupIndex);
        int variantCount = group[1] - group[0];
        if (variantCount <= 1) {
            return icons.get(group[0]);
        }

        // Hold one variant steady for the whole family window (no flicker within
        // it), advancing to the next variant only the next time this family's
        // window comes around. A family with N variants (e.g. 任意原木's
        // log/wood/stripped_log/stripped_wood) previously subdivided the window
        // into N flashes, which felt too fast when this list is the primary,
        // large row icon rather than a small inline hint.
        int variant = (int) (windowIndex % variantCount);
        return icons.get(group[0] + variant);
    }

    private static String familyOf(class_1799 stack) {
        String id = ItemStackTexts.id(stack);
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        if (path.startsWith("stripped_")) {
            path = path.substring("stripped_".length());
        }

        for (String family : WOOD_FAMILIES) {
            if (path.equals(family) || path.startsWith(family + "_")) {
                return family;
            }
        }

        return "";
    }
}
