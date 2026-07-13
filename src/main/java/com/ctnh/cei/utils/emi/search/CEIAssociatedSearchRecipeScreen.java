package com.ctnh.cei.utils.emi.search;

/** RecipeScreen mixin 暴露给 EmiScreenManager mixin 的关联搜索按钮接口。 */
public interface CEIAssociatedSearchRecipeScreen {

    int cei$getAssociatedSearchButtonX();

    int cei$getAssociatedSearchButtonY();

    void cei$toggleAssociatedSearch();
}
