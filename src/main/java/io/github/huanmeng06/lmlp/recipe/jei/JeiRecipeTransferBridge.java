package io.github.huanmeng06.lmlp.recipe.jei;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.huanmeng06.lmlp.gui.RecipeTransferBridge;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class JeiRecipeTransferBridge implements RecipeTransferBridge {
    // REI TransferHandler.Result's original failed-transfer tint.
    private static final int REI_FAILED_TRANSFER_TINT = 0x67FF0000;

    @Override
    public TransferState evaluate(RecipeSummary summary, AbstractContainerScreen<?> containerScreen) {
        try {
            TransferLookup lookup = transferLookup(summary, containerScreen);
            if (lookup == null) {
                return TransferState.UNSUPPORTED;
            }

            if (lookup.handler().isEmpty()) {
                return new TransferState(
                        true,
                        false,
                        false,
                        0,
                        "!",
                        tooltip(summary, "error.rei.not.supported.move.items"),
                        null);
            }

            TransferContext context = new TransferContext(lookup.handler().get(), lookup.menu(), lookup.nativeRecipe());
            IRecipeTransferError error = context.check(false, false);
            if (error == null) {
                return new TransferState(true, true, false, 0, "+", transferTooltip(), null);
            }

            if (error.getType() == IRecipeTransferError.Type.INTERNAL) {
                return new TransferState(
                        true,
                        false,
                        false,
                        0,
                        "!",
                        tooltip(summary, "error.rei.not.supported.move.items"),
                        error);
            }

            boolean enabled = error.getType().allowsTransfer;
            return new TransferState(
                    true,
                    enabled,
                    !enabled,
                    enabled ? 0 : REI_FAILED_TRANSFER_TINT,
                    "+",
                    enabled ? transferTooltip() : errorTooltip(error),
                    error);
        } catch (Throwable throwable) {
            return TransferState.UNSUPPORTED;
        }
    }

    @Override
    public Bounds buttonBounds(RecipeSummary summary, int panelX, int panelY, int panelWidth, int panelHeight) {
        // Keep the transfer control outside the recipe panel with enough clearance
        // that the 10 x 10 button does not cover the visible background edge.
        return new Bounds(panelX + panelWidth + 5, panelY + panelHeight - 8, 10, 10);
    }

    @Override
    public boolean transfer(RecipeSummary summary, AbstractContainerScreen<?> containerScreen, boolean maxTransfer) {
        try {
            if (!maxTransfer && summary.craftsMissing() <= 0) {
                return false;
            }
            TransferLookup lookup = transferLookup(summary, containerScreen);
            if (lookup == null || lookup.handler().isEmpty()) {
                return false;
            }

            TransferContext context = new TransferContext(lookup.handler().get(), lookup.menu(), lookup.nativeRecipe());
            IRecipeTransferError error = context.transfer(summary.craftsMissing(), maxTransfer);
            return error == null || error.getType().allowsTransfer;
        } catch (Throwable throwable) {
            return false;
        }
    }

    @Override
    public void renderError(RecipeSummary summary, TransferState state, GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (!(state.nativeState() instanceof IRecipeTransferError error)) {
            return;
        }

        JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe = nativeRecipe(summary);
        if (nativeRecipe == null) {
            return;
        }

        Rect2i recipeBounds = nativeRecipe.layout().getRect();
        error.showError(
                context,
                mouseX,
                mouseY,
                nativeRecipe.layout().getRecipeSlotsView(),
                recipeBounds.getX(),
                recipeBounds.getY());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static TransferLookup transferLookup(RecipeSummary summary, AbstractContainerScreen<?> containerScreen) {
        IJeiRuntime runtime = JeiRuntimeBridge.runtime().orElse(null);
        JeiRecipeResolver.JeiNativeRecipe nativeRecipe = nativeRecipe(summary);
        if (runtime == null || nativeRecipe == null || containerScreen == null || Minecraft.getInstance().player == null) {
            return null;
        }

        AbstractContainerMenu menu = containerScreen.getMenu();
        Optional<IRecipeTransferHandler<AbstractContainerMenu, Object>> handler = (Optional) runtime.getRecipeTransferManager()
                .getRecipeTransferHandler(menu, nativeRecipe.category());
        return new TransferLookup(handler, menu, nativeRecipe);
    }

    private static List<Component> tooltip(RecipeSummary summary, String primaryKey) {
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable(primaryKey));
        if (Minecraft.getInstance().options != null && Minecraft.getInstance().options.advancedItemTooltips) {
            String recipeIdLabel = Component.translatable("lmlp.label.recipe.recipe_id").getString();
            lines.add(Component.literal(recipeIdLabel + ": " + summary.recipeId())
                    .withStyle(ChatFormatting.GRAY));
        }
        return List.copyOf(lines);
    }

    private static List<Component> transferTooltip() {
        return List.of(
                Component.translatable("lmlp.tooltip.recipe.transfer_missing"),
                Component.translatable("lmlp.tooltip.recipe.transfer_all"));
    }

    private static List<Component> errorTooltip(IRecipeTransferError error) {
        Class<?> type = error.getClass();
        while (type != null) {
            try {
                Field messageField = type.getDeclaredField("message");
                messageField.setAccessible(true);
                if (messageField.get(error) instanceof List<?> rawLines) {
                    List<Component> lines = new ArrayList<>();
                    for (Object rawLine : rawLines) {
                        if (rawLine instanceof Component line) {
                            lines.add(line);
                        }
                    }
                    if (!lines.isEmpty()) {
                        return List.copyOf(lines);
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // Older and newer JEI versions expose this message differently.
            }
            type = type.getSuperclass();
        }

        return List.of(
                Component.translatable("jei.tooltip.transfer"),
                Component.translatable("jei.tooltip.error.recipe.transfer.missing")
                        .withStyle(ChatFormatting.RED));
    }

    private static JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe(RecipeSummary summary) {
        return summary.nativeDisplay() instanceof JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe ? nativeRecipe : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private record TransferLookup(Optional<IRecipeTransferHandler<AbstractContainerMenu, Object>> handler, AbstractContainerMenu menu, JeiRecipeResolver.JeiNativeRecipe nativeRecipe) {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private record TransferContext(IRecipeTransferHandler handler, AbstractContainerMenu menu, JeiRecipeResolver.JeiNativeRecipe nativeRecipe) {
        private IRecipeTransferError check(boolean maxTransfer, boolean doTransfer) {
            return this.handler.transferRecipe(
                    this.menu,
                    this.nativeRecipe.recipe(),
                    this.nativeRecipe.layout().getRecipeSlotsView(),
                    Minecraft.getInstance().player,
                    maxTransfer,
                    doTransfer);
        }

        private IRecipeTransferError transfer(int craftsMissing, boolean maxTransfer) {
            if (maxTransfer || craftsMissing <= 1) {
                return this.check(maxTransfer, true);
            }
            int low = 1;
            int high = craftsMissing;
            int best = 0;
            IRecipeTransferError lastError = null;
            while (low <= high) {
                int candidate = low + (high - low) / 2;
                IRecipeTransferError error = this.checkScaled(candidate, false);
                if (error == null || error.getType().allowsTransfer) {
                    best = candidate;
                    low = candidate + 1;
                } else {
                    lastError = error;
                    high = candidate - 1;
                }
            }
            return best > 0 ? this.checkScaled(best, true) : lastError;
        }

        private IRecipeTransferError checkScaled(int crafts, boolean doTransfer) {
            java.util.Map<net.minecraft.world.item.ItemStack, Integer> originalCounts = new java.util.IdentityHashMap<>();
            this.nativeRecipe.layout().getRecipeSlotsView()
                    .getSlotViews(mezz.jei.api.recipe.RecipeIngredientRole.INPUT)
                    .forEach(slot -> slot.getAllIngredients().forEach(ingredient -> {
                        if (ingredient.getIngredient() instanceof net.minecraft.world.item.ItemStack stack
                                && !stack.isEmpty()) {
                            originalCounts.putIfAbsent(stack, stack.getCount());
                            long scaled = (long) originalCounts.get(stack) * crafts;
                            stack.setCount((int) Math.min(stack.getMaxStackSize(), scaled));
                        }
                    }));
            try {
                return this.check(false, doTransfer);
            } finally {
                originalCounts.forEach(net.minecraft.world.item.ItemStack::setCount);
            }
        }
    }
}
