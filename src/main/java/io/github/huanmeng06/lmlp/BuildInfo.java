package io.github.huanmeng06.lmlp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class BuildInfo {
    private static final Properties PROPERTIES = load();

    public static final String VERSION = value("version", "development");
    public static final String MINECRAFT = value("minecraft", "unknown");
    public static final String GIT_COMMIT = value("gitCommit", "unknown");
    public static final String GIT_DIRTY = value("gitDirty", "unknown");
    public static final String BUILD_TIME = value("buildTime", "unknown");

    private BuildInfo() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream stream = BuildInfo.class.getResourceAsStream("/lmlp-build.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException ignored) {
            // Keep safe fallback values so diagnostics never prevent startup.
        }
        return properties;
    }

    private static String value(String key, String fallback) {
        String value = PROPERTIES.getProperty(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
