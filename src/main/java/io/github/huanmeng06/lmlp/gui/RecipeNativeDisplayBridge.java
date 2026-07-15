package io.github.huanmeng06.lmlp.gui;

import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

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

    default void render(RecipeSummary summary, GuiGraphicsExtractor context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
    }

    default boolean renderTooltip(RecipeSummary summary, GuiGraphicsExtractor context, Font textRenderer, int x, int y, int width, int height, int mouseX, int mouseY) {
        return false;
    }

    default boolean renderCategoryTab(RecipeSummary summary, GuiGraphicsExtractor context, int x, int y, boolean hovered) {
        return false;
    }

    default List<Component> getCategoryTooltip(RecipeSummary summary) {
        return List.of();
    }

    default ItemStack getCategoryIngredient(RecipeSummary summary) {
        return ItemStack.EMPTY;
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
