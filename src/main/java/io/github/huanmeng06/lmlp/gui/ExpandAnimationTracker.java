package io.github.huanmeng06.lmlp.gui;

import java.util.HashMap;
import java.util.Map;

final class ExpandAnimationTracker {
    static final long DURATION_MS = 150L;

    private final Map<String, ExpandAnimation> animations = new HashMap<>();

    float progress(String path, boolean expanded) {
        ExpandAnimation animation = this.animations.get(path);
        if (animation != null) {
            return animation.progress(now());
        }

        return expanded ? 1.0F : 0.0F;
    }

    void start(String path, float startProgress, float targetProgress) {
        if (Math.abs(startProgress - targetProgress) < 0.001F) {
            this.animations.remove(path);
            return;
        }

        this.animations.put(path, new ExpandAnimation(now(), startProgress, targetProgress));
    }

    void prune() {
        long now = now();
        this.animations.entrySet().removeIf(entry -> entry.getValue().isFinished(now));
    }

    boolean isActive() {
        return !this.animations.isEmpty();
    }

    void remove(String path) {
        this.animations.remove(path);
    }

    void removeDescendants(String path) {
        this.animations.keySet().removeIf(animatedPath -> animatedPath.startsWith(path + "/"));
    }

    void clear() {
        this.animations.clear();
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static float easeOutCubic(float progress) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        float inverted = 1.0F - clamped;
        return 1.0F - inverted * inverted * inverted;
    }

    private record ExpandAnimation(long startTimeMs, float startProgress, float targetProgress) {
        private float progress(long nowMs) {
            if (nowMs <= this.startTimeMs) {
                return this.startProgress;
            }

            float elapsed = (float) (nowMs - this.startTimeMs) / (float) DURATION_MS;
            return this.startProgress + (this.targetProgress - this.startProgress) * easeOutCubic(elapsed);
        }

        private boolean isFinished(long nowMs) {
            return nowMs - this.startTimeMs >= DURATION_MS;
        }
    }
}
