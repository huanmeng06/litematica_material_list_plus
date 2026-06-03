package io.github.huanmeng06.lmlp.gui;

public final class MaterialListColumnLayout {
    private static int requiredEntryWidth = 1;

    private MaterialListColumnLayout() {
    }

    public static void updateRequiredEntryWidth(int nameWidth, int totalWidth, int missingWidth, int availableWidth) {
        requiredEntryWidth = 4 + nameWidth + 40 + totalWidth + 24 + missingWidth + 24 + availableWidth + 24;
    }

    public static int requiredEntryWidth() {
        return requiredEntryWidth;
    }
}
