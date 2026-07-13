package com.ctnh.cei.utils.emi.duplicate;

/** RecipeScreen mixin 暴露给 EmiScreenManager mixin 的重复配方按钮接口。 */
public interface CEIDuplicateRecipeScreen {

    int cei$getDuplicateRecipeButtonX();

    int cei$getDuplicateRecipeButtonY();

    void cei$toggleDuplicateRecipes();
}
