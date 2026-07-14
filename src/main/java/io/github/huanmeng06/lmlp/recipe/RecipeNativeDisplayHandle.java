package io.github.huanmeng06.lmlp.recipe;

/**
 * A third-party native recipe display whose mutable render state must not be
 * shared by separate recipe-summary consumers.
 */
public interface RecipeNativeDisplayHandle {
    Object fork();
}
