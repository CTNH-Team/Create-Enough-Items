package com.ctnh.cei.utils.emi.voltage;

/** RecipeScreen mixin 暴露给 EmiScreenManager mixin 的电压区间过滤按钮接口。 */
public interface CEIVoltageRecipeScreen {

    int cei$getVoltageMinButtonX();

    int cei$getVoltageMinButtonY();

    int cei$getVoltageMaxButtonX();

    int cei$getVoltageMaxButtonY();

    int cei$getVoltageResetButtonX();

    int cei$getVoltageResetButtonY();

    void cei$adjustVoltageMinTier(int delta);

    void cei$adjustVoltageMaxTier(int delta);

    void cei$resetVoltageFilter();
}
