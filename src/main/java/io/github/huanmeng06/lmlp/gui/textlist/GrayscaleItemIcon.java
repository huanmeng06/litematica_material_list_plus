package io.github.huanmeng06.lmlp.gui.textlist;

import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1058;
import net.minecraft.class_1087;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_7764;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders a true black/white/gray copy of an item's icon for disabled list entries.
 * Vanilla item rendering resets its own shader color before drawing, so tinting or
 * overlaying a translucent rect can only darken the icon -- it can't remove hue. The
 * only way to get an actual grayscale result is to read the sprite's raw pixels and
 * replace each one with its luminance, which requires reaching into the sprite's
 * private source image via reflection (there's no public accessor for it).
 */
final class GrayscaleItemIcon {
    private static final Map<String, class_2960> CACHE = new HashMap<>();
    private static Field spriteImageField;

    private GrayscaleItemIcon() {
    }

    static void render(class_332 context, class_1799 stack, String cacheKey, int x, int y, int size) {
        class_2960 textureId = CACHE.computeIfAbsent(cacheKey, key -> buildGrayscaleTexture(stack));
        if (textureId == null) {
            return;
        }

        context.method_25290(textureId, x, y, 0.0F, 0.0F, size, size, size, size);
    }

    private static class_2960 buildGrayscaleTexture(class_1799 stack) {
        class_1011 sourceImage = extractSpriteImage(stack);
        if (sourceImage == null) {
            return null;
        }

        int width = sourceImage.method_4307();
        int height = sourceImage.method_4323();
        class_1011 grayImage = new class_1011(width, height, false);
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int color = sourceImage.method_4315(px, py);
                int r = color & 0xFF;
                int g = (color >>> 8) & 0xFF;
                int b = (color >>> 16) & 0xFF;
                int a = (color >>> 24) & 0xFF;
                int luminance = Math.round(0.299F * r + 0.587F * g + 0.114F * b);
                grayImage.method_4305(px, py, (a << 24) | (luminance << 16) | (luminance << 8) | luminance);
            }
        }

        class_1043 texture = new class_1043(grayImage);
        return class_310.method_1551().method_1531().method_4617("lmlp_gray_icon", texture);
    }

    private static class_1011 extractSpriteImage(class_1799 stack) {
        class_1087 model = class_310.method_1551().method_1480().method_4019(stack, null, null, 0);
        class_1058 sprite = model.method_4711();
        class_7764 contents = sprite.method_45851();
        try {
            if (spriteImageField == null) {
                Field field = class_7764.class.getDeclaredField("field_40539");
                field.setAccessible(true);
                spriteImageField = field;
            }
            return (class_1011) spriteImageField.get(contents);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }
}
