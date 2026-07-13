package com.ctnh.cei.mixin.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.ctnh.cei.utils.emi.duplicate.CEIDuplicateRecipeScreen;
import com.ctnh.cei.utils.emi.duplicate.CEIDuplicateRecipes;
import com.ctnh.cei.utils.emi.featured.CEIFeaturedRecipeScreen;
import com.ctnh.cei.utils.emi.featured.CEIFeaturedRecipes;
import com.ctnh.cei.utils.emi.search.CEIAssociatedSearch;
import com.ctnh.cei.utils.emi.search.CEIAssociatedSearchRecipeScreen;
import com.ctnh.cei.utils.emi.voltage.CEIVoltageRecipeFilter;
import com.ctnh.cei.utils.emi.voltage.CEIVoltageRecipeScreen;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.RecipeTab;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;

/** 为 EMI 配方界面添加回收配方过滤和关联搜索按钮。 */
@Mixin(value = RecipeScreen.class, remap = false)
public abstract class RecipeScreenMixin extends Screen
                                        implements CEIFeaturedRecipeScreen, CEIAssociatedSearchRecipeScreen,
                                        CEIDuplicateRecipeScreen,
                                        CEIVoltageRecipeScreen {

    @Shadow
    private Map<EmiRecipeCategory, List<EmiRecipe>> recipes;

    @Shadow
    private List<RecipeTab> tabs;

    @Shadow
    private int tabPage;

    @Shadow
    private int tab;

    @Shadow
    private int page;

    @Shadow
    int backgroundWidth;

    @Shadow
    int backgroundHeight;

    @Shadow
    int x;

    @Shadow
    int y;

    @Unique
    private Map<EmiRecipeCategory, List<EmiRecipe>> cei$allRecipes;

    protected RecipeScreenMixin(Component title) {
        super(title);
    }

    @Redirect(method = "<init>",
              at = @At(value = "FIELD",
                       target = "Ldev/emi/emi/screen/RecipeScreen;recipes:Ljava/util/Map;",
                       opcode = Opcodes.PUTFIELD))
    private void cei$captureOriginalRecipes(RecipeScreen instance,
                                            Map<EmiRecipeCategory, List<EmiRecipe>> recipes) {
        this.cei$allRecipes = CEIFeaturedRecipes.copyOf(recipes);
        this.recipes = cei$applyRecipeFilters(this.cei$allRecipes);
    }

    @Override
    public void cei$toggleFeaturedRecipes() {
        CEIFeaturedRecipes.toggleEnabled();
        cei$refreshFilteredRecipes();
    }

    @Override
    public int cei$getFeaturedButtonX() {
        return this.x + Math.max(0, (this.backgroundWidth - 56) / 2);
    }

    @Override
    public int cei$getFeaturedButtonY() {
        return this.cei$getAssociatedSearchButtonY();
    }

    @Override
    public int cei$getAssociatedSearchButtonX() {
        return this.x;
    }

    @Override
    public int cei$getAssociatedSearchButtonY() {
        return Math.min(this.y + this.backgroundHeight + 4, this.height - 20);
    }

    @Override
    public void cei$toggleAssociatedSearch() {
        CEIAssociatedSearch.toggleEnabled();
        CEIAssociatedSearch.refreshCurrentLookup();
    }

    @Override
    public int cei$getDuplicateRecipeButtonX() {
        return this.x + Math.max(0, this.backgroundWidth - 56);
    }

    @Override
    public int cei$getDuplicateRecipeButtonY() {
        return this.cei$getAssociatedSearchButtonY();
    }

    @Override
    public void cei$toggleDuplicateRecipes() {
        CEIDuplicateRecipes.toggleHidden();
        cei$refreshFilteredRecipes();
    }

    @Override
    public int cei$getVoltageMinButtonX() {
        return this.x + Math.max(0, (this.backgroundWidth - 88) / 2);
    }

    @Override
    public int cei$getVoltageMinButtonY() {
        return Math.min(this.cei$getAssociatedSearchButtonY() + 18, this.height - 20);
    }

    @Override
    public int cei$getVoltageMaxButtonX() {
        return this.cei$getVoltageMinButtonX() + 56;
    }

    @Override
    public int cei$getVoltageMaxButtonY() {
        return this.cei$getVoltageMinButtonY();
    }

    @Override
    public void cei$adjustVoltageMinTier(int delta) {
        CEIVoltageRecipeFilter.adjustMinTier(delta);
        cei$refreshFilteredRecipes();
    }

    @Override
    public void cei$adjustVoltageMaxTier(int delta) {
        CEIVoltageRecipeFilter.adjustMaxTier(delta);
        cei$refreshFilteredRecipes();
    }

    @Unique
    private Map<EmiRecipeCategory, List<EmiRecipe>> cei$applyRecipeFilters(
                                                                           Map<EmiRecipeCategory, List<EmiRecipe>> recipes) {
        return CEIVoltageRecipeFilter.apply(CEIDuplicateRecipes.apply(CEIFeaturedRecipes.apply(recipes)));
    }

    @Unique
    private void cei$refreshFilteredRecipes() {
        if (this.cei$allRecipes == null) {
            this.cei$allRecipes = CEIFeaturedRecipes.copyOf(this.recipes);
        }
        this.recipes = cei$applyRecipeFilters(this.cei$allRecipes);
        this.tabPage = 0;
        this.tab = 0;
        this.page = 0;
        Minecraft.getInstance().setScreen(this);
    }
}
