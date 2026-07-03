package io.github.huanmeng06.lmlp.gui;

public final class MaterialListColumnLayout {
    private static final long ANIMATION_DURATION_MS = 150L;
    private static final int NAME_TO_TOTAL_GAP = 44;
    private static final int COUNT_COLUMN_GAP = 40;
    private static final int MIN_NAME_TO_TOTAL_GAP = 16;
    private static final int MIN_COUNT_COLUMN_GAP = 12;
    private static final int MIN_NAME_WIDTH = 60;
    private static int availableEntryWidth = Integer.MAX_VALUE;
    private static int nameWidth = 1;
    private static int totalWidth = 1;
    private static int missingWidth = 1;
    private static int availableWidth = 1;
    private static int startNameWidth = 1;
    private static int startTotalWidth = 1;
    private static int startMissingWidth = 1;
    private static int startAvailableWidth = 1;
    private static int targetNameWidth = 1;
    private static int targetTotalWidth = 1;
    private static int targetMissingWidth = 1;
    private static int targetAvailableWidth = 1;
    private static long animationStartMs;
    private static boolean initialized;
    private static boolean animating;
    private static boolean animateShrinkForNextUpdate = true;

    private MaterialListColumnLayout() {
    }

    public static void updateRequiredEntryWidth(int requiredNameWidth, int requiredTotalWidth, int requiredMissingWidth, int requiredAvailableWidth) {
        boolean animateShrink = animateShrinkForNextUpdate;
        animateShrinkForNextUpdate = true;
        updateRequiredEntryWidth(requiredNameWidth, requiredTotalWidth, requiredMissingWidth, requiredAvailableWidth, animateShrink);
    }

    public static void updateRequiredEntryWidth(int requiredNameWidth, int requiredTotalWidth, int requiredMissingWidth, int requiredAvailableWidth, boolean animateShrink) {
        if (!initialized) {
            setImmediate(requiredNameWidth, requiredTotalWidth, requiredMissingWidth, requiredAvailableWidth);
            initialized = true;
            return;
        }

        if (requiredNameWidth == targetNameWidth
                && requiredTotalWidth == targetTotalWidth
                && requiredMissingWidth == targetMissingWidth
                && requiredAvailableWidth == targetAvailableWidth) {
            return;
        }

        if (!animateShrink) {
            setImmediate(requiredNameWidth, requiredTotalWidth, requiredMissingWidth, requiredAvailableWidth);
            return;
        }

        advanceAnimation();

        int currentNameWidth = Math.max(nameWidth, requiredNameWidth);
        int currentTotalWidth = Math.max(totalWidth, requiredTotalWidth);
        int currentMissingWidth = Math.max(missingWidth, requiredMissingWidth);
        int currentAvailableWidth = Math.max(availableWidth, requiredAvailableWidth);
        setWidths(currentNameWidth, currentTotalWidth, currentMissingWidth, currentAvailableWidth);

        targetNameWidth = requiredNameWidth;
        targetTotalWidth = requiredTotalWidth;
        targetMissingWidth = requiredMissingWidth;
        targetAvailableWidth = requiredAvailableWidth;

        if (currentNameWidth == requiredNameWidth
                && currentTotalWidth == requiredTotalWidth
                && currentMissingWidth == requiredMissingWidth
                && currentAvailableWidth == requiredAvailableWidth) {
            animating = false;
            return;
        }

        startNameWidth = currentNameWidth;
        startTotalWidth = currentTotalWidth;
        startMissingWidth = currentMissingWidth;
        startAvailableWidth = currentAvailableWidth;
        animationStartMs = System.currentTimeMillis();
        animating = true;
    }

    public static void updateAvailableEntryWidth(int available) {
        availableEntryWidth = available <= 0 ? Integer.MAX_VALUE : available;
    }

    public static int requiredEntryWidth() {
        return 4 + nameWidth() + nameToTotalGap() + totalWidth() + countColumnGap() + missingWidth() + countColumnGap() + availableWidth() + countColumnGap();
    }

    public static int nameWidth() {
        advanceAnimation();
        int overflow = compressionOverflow();
        if (overflow <= 0) {
            return nameWidth;
        }

        int gapCapacity = gapReductionCapacity();
        if (overflow <= gapCapacity) {
            return nameWidth;
        }

        return Math.max(MIN_NAME_WIDTH, nameWidth - (overflow - gapCapacity));
    }

    public static int totalWidth() {
        advanceAnimation();
        return totalWidth;
    }

    public static int missingWidth() {
        advanceAnimation();
        return missingWidth;
    }

    public static int availableWidth() {
        advanceAnimation();
        return availableWidth;
    }

    public static boolean hasActiveAnimation() {
        advanceAnimation();
        return animating;
    }

    public static void setAnimateShrinkForNextUpdate(boolean animateShrink) {
        animateShrinkForNextUpdate = animateShrink;
    }

    public static int nameToTotalGap() {
        int overflow = compressionOverflow();
        if (overflow <= 0) {
            return NAME_TO_TOTAL_GAP;
        }

        float factor = Math.min(1.0F, (float) overflow / (float) gapReductionCapacity());
        return NAME_TO_TOTAL_GAP - Math.round((NAME_TO_TOTAL_GAP - MIN_NAME_TO_TOTAL_GAP) * factor);
    }

    public static int countColumnGap() {
        int overflow = compressionOverflow();
        if (overflow <= 0) {
            return COUNT_COLUMN_GAP;
        }

        float factor = Math.min(1.0F, (float) overflow / (float) gapReductionCapacity());
        return COUNT_COLUMN_GAP - Math.round((COUNT_COLUMN_GAP - MIN_COUNT_COLUMN_GAP) * factor);
    }

    /**
     * How much the uncompressed layout overflows the available widget width.
     * Positive values trigger gap shrinking, then name column clamping.
     */
    private static int compressionOverflow() {
        if (availableEntryWidth == Integer.MAX_VALUE) {
            return 0;
        }

        advanceAnimation();
        int fullWidth = 4 + nameWidth + NAME_TO_TOTAL_GAP + totalWidth + COUNT_COLUMN_GAP
                + missingWidth + COUNT_COLUMN_GAP + availableWidth + COUNT_COLUMN_GAP;
        return fullWidth - availableEntryWidth;
    }

    private static int gapReductionCapacity() {
        return (NAME_TO_TOTAL_GAP - MIN_NAME_TO_TOTAL_GAP) + 3 * (COUNT_COLUMN_GAP - MIN_COUNT_COLUMN_GAP);
    }

    private static void setWidths(int nameWidth, int totalWidth, int missingWidth, int availableWidth) {
        MaterialListColumnLayout.nameWidth = nameWidth;
        MaterialListColumnLayout.totalWidth = totalWidth;
        MaterialListColumnLayout.missingWidth = missingWidth;
        MaterialListColumnLayout.availableWidth = availableWidth;
    }

    private static void setImmediate(int nameWidth, int totalWidth, int missingWidth, int availableWidth) {
        setWidths(nameWidth, totalWidth, missingWidth, availableWidth);
        startNameWidth = nameWidth;
        startTotalWidth = totalWidth;
        startMissingWidth = missingWidth;
        startAvailableWidth = availableWidth;
        targetNameWidth = nameWidth;
        targetTotalWidth = totalWidth;
        targetMissingWidth = missingWidth;
        targetAvailableWidth = availableWidth;
        animating = false;
    }

    private static void advanceAnimation() {
        if (!animating) {
            return;
        }

        long elapsedMs = System.currentTimeMillis() - animationStartMs;
        if (elapsedMs >= ANIMATION_DURATION_MS) {
            setWidths(targetNameWidth, targetTotalWidth, targetMissingWidth, targetAvailableWidth);
            animating = false;
            return;
        }

        float progress = (float) elapsedMs / (float) ANIMATION_DURATION_MS;
        float eased = easeOutCubic(progress);
        setWidths(
                animatedWidth(startNameWidth, targetNameWidth, eased),
                animatedWidth(startTotalWidth, targetTotalWidth, eased),
                animatedWidth(startMissingWidth, targetMissingWidth, eased),
                animatedWidth(startAvailableWidth, targetAvailableWidth, eased));
    }

    private static int animatedWidth(int start, int target, float progress) {
        if (start <= target) {
            return target;
        }

        return Math.max(target, Math.round(start + (target - start) * progress));
    }

    private static float easeOutCubic(float progress) {
        float inverted = 1.0F - Math.max(0.0F, Math.min(1.0F, progress));
        return 1.0F - inverted * inverted * inverted;
    }
}
