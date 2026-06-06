package io.github.huanmeng06.lmlp.gui;

public final class MaterialListColumnLayout {
    private static int nameWidth = 1;
    private static int totalWidth = 1;
    private static int missingWidth = 1;
    private static int availableWidth = 1;

    private MaterialListColumnLayout() {
    }

    public static void updateRequiredEntryWidth(int nameWidth, int totalWidth, int missingWidth, int availableWidth) {
        MaterialListColumnLayout.nameWidth = nameWidth;
        MaterialListColumnLayout.totalWidth = totalWidth;
        MaterialListColumnLayout.missingWidth = missingWidth;
        MaterialListColumnLayout.availableWidth = availableWidth;
    }

    public static int requiredEntryWidth() {
        return 4 + nameWidth() + 40 + totalWidth() + 24 + missingWidth() + 24 + availableWidth() + 24;
    }

    public static int nameWidth() {
        return nameWidth;
    }

    public static int totalWidth() {
        return totalWidth;
    }

    public static int missingWidth() {
        return missingWidth;
    }

    public static int availableWidth() {
        return availableWidth;
    }

    public static boolean hasActiveAnimation() {
        return false;
    }
}
