package io.github.huanmeng06.lmlp.gui;

public final class MaterialListColumnLayout {
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

    private MaterialListColumnLayout() {
    }

    public static void updateRequiredEntryWidth(int nameWidth, int totalWidth, int missingWidth, int availableWidth) {
        if (!initialized) {
            startNameWidth = targetNameWidth = nameWidth;
            startTotalWidth = targetTotalWidth = totalWidth;
            startMissingWidth = targetMissingWidth = missingWidth;
            startAvailableWidth = targetAvailableWidth = availableWidth;
            initialized = true;
            animating = false;
            return;
        }

        if (nameWidth == targetNameWidth
                && totalWidth == targetTotalWidth
                && missingWidth == targetMissingWidth
                && availableWidth == targetAvailableWidth) {
            return;
        }

        int currentNameWidth = nameWidth();
        int currentTotalWidth = totalWidth();
        int currentMissingWidth = missingWidth();
        int currentAvailableWidth = availableWidth();
        if (nameWidth > currentNameWidth
                || totalWidth > currentTotalWidth
                || missingWidth > currentMissingWidth
                || availableWidth > currentAvailableWidth) {
            startNameWidth = targetNameWidth = nameWidth;
            startTotalWidth = targetTotalWidth = totalWidth;
            startMissingWidth = targetMissingWidth = missingWidth;
            startAvailableWidth = targetAvailableWidth = availableWidth;
            animating = false;
            return;
        }

        startNameWidth = currentNameWidth;
        startTotalWidth = currentTotalWidth;
        startMissingWidth = currentMissingWidth;
        startAvailableWidth = currentAvailableWidth;
        targetNameWidth = nameWidth;
        targetTotalWidth = totalWidth;
        targetMissingWidth = missingWidth;
        targetAvailableWidth = availableWidth;
        animationStartMs = System.currentTimeMillis();
        animating = true;
    }

    public static int requiredEntryWidth() {
        return 4 + nameWidth() + 40 + totalWidth() + 24 + missingWidth() + 24 + availableWidth() + 24;
    }

    public static int nameWidth() {
        return animatedWidth(startNameWidth, targetNameWidth);
    }

    public static int totalWidth() {
        return animatedWidth(startTotalWidth, targetTotalWidth);
    }

    public static int missingWidth() {
        return animatedWidth(startMissingWidth, targetMissingWidth);
    }

    public static int availableWidth() {
        return animatedWidth(startAvailableWidth, targetAvailableWidth);
    }

    public static boolean hasActiveAnimation() {
        return animating;
    }

    private static int animatedWidth(int start, int target) {
        if (!animating) {
            return target;
        }

        long elapsedMs = System.currentTimeMillis() - animationStartMs;
        if (elapsedMs >= ExpandAnimationTracker.DURATION_MS) {
            animating = false;
            return target;
        }

        float progress = (float) elapsedMs / (float) ExpandAnimationTracker.DURATION_MS;
        float eased = ExpandAnimationTracker.easeOutCubic(progress);
        return Math.round(start + (target - start) * eased);
    }
}
