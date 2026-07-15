package io.github.huanmeng06.lmlp.recipe.jei;

import java.util.List;

import io.github.huanmeng06.lmlp.gui.RecipeNativeDisplayBridge;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class JeiNativeDisplayBridge implements RecipeNativeDisplayBridge {
    private static final Identifier CATALYST_TAB = Identifier.fromNamespaceAndPath("litematica_material_list_plus", "textures/gui/catalyst_tab.png");

    @Override
    public boolean canRender(RecipeSummary summary) {
        return nativeRecipe(summary) != null;
    }

    @Override
    public int getDisplayWidth(RecipeSummary summary, int fallbackWidth) {
        JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe = nativeRecipe(summary);
        return nativeRecipe == null ? fallbackWidth : nativeRecipe.category().getWidth();
    }

    @Override
    public int getDisplayHeight(RecipeSummary summary, int fallbackHeight) {
        JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe = nativeRecipe(summary);
        return nativeRecipe == null ? fallbackHeight : nativeRecipe.category().getHeight();
    }

    @Override
    public void render(RecipeSummary summary, GuiGraphicsExtractor context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
        IRecipeLayoutDrawable<?> layout = requireNativeRecipe(summary).layout();
        layout.setPosition(x, y);
        layout.drawRecipe(context, mouseX, mouseY);
    }

    @Override
    public boolean renderTooltip(RecipeSummary summary, GuiGraphicsExtractor context, Font textRenderer, int x, int y, int width, int height, int mouseX, int mouseY) {
        IRecipeLayoutDrawable<?> layout = requireNativeRecipe(summary).layout();
        layout.setPosition(x, y);
        if (!layout.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        layout.drawOverlays(context, mouseX, mouseY);
        return true;
    }

    @Override
    public boolean renderCategoryTab(RecipeSummary summary, GuiGraphicsExtractor context, int x, int y, boolean hovered) {
        JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe = nativeRecipe(summary);
        if (nativeRecipe == null) {
            return false;
        }

        context.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, CATALYST_TAB, x, y, 0.0F, 0.0F, 28, 28, 28, 28);
        IJeiRuntime runtime = JeiRuntimeBridge.runtime().orElse(null);
        if (runtime != null) {
            runtime.getJeiHelpers().getGuiHelper().getSlotDrawable().draw(context, x + 5, y + 5);
        }
        IDrawable icon = nativeRecipe.category().getIcon();
        if (icon != null) {
            int iconX = x + 6 + (16 - icon.getWidth()) / 2;
            int iconY = y + 6 + (16 - icon.getHeight()) / 2;
            icon.draw(context, iconX, iconY);
            if (hovered) {
                context.fill(x + 6, y + 6, x + 22, y + 22, 0x80FFFFFF);
            }
            return true;
        }

        if (runtime != null) {
            ItemStack catalyst = catalyst(nativeRecipe, runtime);
            if (!catalyst.isEmpty()) {
                context.item(catalyst, x + 6, y + 6);
            }
        }
        if (hovered) {
            context.fill(x + 6, y + 6, x + 22, y + 22, 0x80FFFFFF);
        }
        return true;
    }

    @Override
    public List<Component> getCategoryTooltip(RecipeSummary summary) {
        return List.of();
    }

    @Override
    public ItemStack getCategoryIngredient(RecipeSummary summary) {
        JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe = nativeRecipe(summary);
        IJeiRuntime runtime = JeiRuntimeBridge.runtime().orElse(null);
        return nativeRecipe == null || runtime == null ? ItemStack.EMPTY : catalyst(nativeRecipe, runtime);
    }

    @Override
    public boolean openCategory(RecipeSummary summary) {
        JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe = nativeRecipe(summary);
        IJeiRuntime runtime = JeiRuntimeBridge.runtime().orElse(null);
        if (nativeRecipe == null || runtime == null) {
            return false;
        }

        // The catalyst tab represents the processing station itself. Open JEI
        // with that station as an OUTPUT focus, so clicking the crafting-table
        // icon lands on JEI's "planks -> crafting table" recipe page instead
        // of merely selecting the broad crafting category at an arbitrary
        // recipe. Fall back to category focus for non-item catalysts.
        ItemStack catalyst = catalyst(nativeRecipe, runtime);
        if (!catalyst.isEmpty()) {
            boolean opened = runtime.getIngredientManager()
                    .createTypedIngredient(catalyst)
                    .map(ingredient -> openIngredient(runtime, ingredient, RecipeIngredientRole.OUTPUT))
                    .orElse(false);
            if (opened) {
                return true;
            }
        }

        runtime.getRecipesGui().showTypes(List.of(nativeRecipe.category().getRecipeType()));
        return true;
    }

    @Override
    public boolean mouseClicked(RecipeSummary summary, double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) {
            return false;
        }

        JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe = nativeRecipe(summary);
        IJeiRuntime runtime = JeiRuntimeBridge.runtime().orElse(null);
        if (nativeRecipe == null || runtime == null) {
            return false;
        }

        return nativeRecipe.layout()
                .getSlotUnderMouse(mouseX, mouseY)
                .map(RecipeSlotUnderMouse::slot)
                .flatMap(IRecipeSlotDrawable::getDisplayedIngredient)
                .map(ingredient -> openIngredient(runtime, ingredient, button == 0
                        ? RecipeIngredientRole.OUTPUT
                        : RecipeIngredientRole.INPUT))
                .orElse(false);
    }

    private static JeiRecipeResolver.JeiNativeRecipe<?> requireNativeRecipe(RecipeSummary summary) {
        JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe = nativeRecipe(summary);
        if (nativeRecipe == null) {
            throw new IllegalArgumentException("Recipe summary has no JEI native recipe");
        }
        return nativeRecipe;
    }

    private static ItemStack catalyst(JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe, IJeiRuntime runtime) {
        return runtime.getRecipeManager()
                .createRecipeCatalystLookup(nativeRecipe.category().getRecipeType())
                .getItemStack()
                .findFirst()
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
    }

    private static <T> boolean openIngredient(IJeiRuntime runtime, ITypedIngredient<T> ingredient, RecipeIngredientRole role) {
        IFocus<T> focus = runtime.getJeiHelpers().getFocusFactory().createFocus(role, ingredient);
        runtime.getRecipesGui().show(List.of(focus));
        return true;
    }

    private static JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe(RecipeSummary summary) {
        return summary.nativeDisplay() instanceof JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe ? nativeRecipe : null;
    }
}
