package io.github.huanmeng06.lmlp.gui;

import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import java.util.List;
import net.minecraft.class_327;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_332;

public interface RecipeNativeDisplayBridge {
    RecipeNativeDisplayBridge DISABLED = new RecipeNativeDisplayBridge() {
    };

    default boolean canRender(RecipeSummary summary) {
        return false;
    }

    default int getDisplayWidth(RecipeSummary summary, int fallbackWidth) {
        return fallbackWidth;
    }

    default int getDisplayHeight(RecipeSummary summary, int fallbackHeight) {
        return fallbackHeight;
    }

    default void tick(RecipeSummary summary) {
    }

    default void render(RecipeSummary summary, class_332 context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
    }

    default boolean renderTooltip(RecipeSummary summary, class_332 context, class_327 textRenderer, int x, int y, int width, int height, int mouseX, int mouseY) {
        return false;
    }

    default boolean renderCategoryTab(RecipeSummary summary, class_332 context, int x, int y, boolean hovered) {
        return false;
    }

    default List<class_2561> getCategoryTooltip(RecipeSummary summary) {
        return List.of();
    }

    default class_1799 getCategoryIngredient(RecipeSummary summary) {
        return class_1799.field_8037;
    }

    default boolean openCategory(RecipeSummary summary) {
        return false;
    }

    default boolean mouseClicked(RecipeSummary summary, double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(RecipeSummary summary, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    default boolean mouseReleased(RecipeSummary summary, double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseScrolled(RecipeSummary summary, double mouseX, double mouseY, double amount) {
        return false;
    }
}
