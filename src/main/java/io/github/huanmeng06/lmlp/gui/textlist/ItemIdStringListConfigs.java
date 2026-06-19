package io.github.huanmeng06.lmlp.gui.textlist;

import fi.dy.masa.malilib.config.IConfigStringList;

import java.util.Locale;

public final class ItemIdStringListConfigs {
    private ItemIdStringListConfigs() {
    }

    public static boolean isSupported(IConfigStringList config) {
        if (config == null || config.getName() == null) {
            return false;
        }

        String name = config.getName();
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("mapping") || lower.contains("entity")) {
            return false;
        }

        if (lower.equals("recipestopitems") || lower.contains("recipestopitem")) {
            return true;
        }

        boolean itemOrBlock = lower.contains("item") || lower.contains("block");
        boolean idList = lower.contains("blacklist") || lower.contains("whitelist")
                || lower.contains("black_list") || lower.contains("white_list")
                || lower.contains("black-list") || lower.contains("white-list");
        return itemOrBlock && idList;
    }
}