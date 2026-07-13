package com.ctnh.cei.utils.emi.featured;

/** RecipeScreen mixin 暴露给 EmiScreenManager mixin 的小接口。 */
public interface CEIFeaturedRecipeScreen {

    int cei$getFeaturedButtonX();

    int cei$getFeaturedButtonY();

    void cei$toggleFeaturedRecipes();
}
