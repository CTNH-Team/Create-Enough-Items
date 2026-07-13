package com.ctnh.cei.mixin.emi.accessor;

import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.integration.emi.recipe.GTEmiRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GTEmiRecipe.class, remap = false)
public interface GTEmiRecipeAccessor {

    @Accessor("recipe")
    GTRecipeDefinition cei$getRecipe();
}
