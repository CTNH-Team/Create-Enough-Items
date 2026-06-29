package com.moguang.cei.mixin.create;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.compat.jei.CreateJEI;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("all")
@Mixin(value = CreateJEI.class, remap = false)
public class CreateJEIMixin {

    /**
     * @author lucky_block
     * @reason
     */
    @Overwrite
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {}

    @Mixin(targets = "com.simibubi.create.compat.jei.CreateJEI$CategoryBuilder", remap = false)
    public static abstract class CategoryBuilderMixin<T extends Recipe<?>> {

        @Shadow
        @Final
        private List<Consumer<List<T>>> recipeListConsumers;

        @Inject(method = "addTypedRecipesExcluding", at = @At("HEAD"), cancellable = true)
        public void addTypedRecipesExcluding(Supplier<RecipeType<? extends T>> recipeType,
                                             Supplier<RecipeType<? extends T>> excluded,
                                             CallbackInfoReturnable<Object> cir) {
            var type = recipeType.get();
            if (type == AllRecipeTypes.MILLING.getType() || type == RecipeType.SMELTING) {
                recipeListConsumers.add(recipes -> {
                    CreateJEI.<T>consumeTypedRecipes(recipes::add, type);
                });
                cir.setReturnValue(this);
            }
        }

        @Inject(method = "addTypedRecipes(Ljava/util/function/Supplier;)Lcom/simibubi/create/compat/jei/CreateJEI$CategoryBuilder;",
                at = @At("HEAD"),
                cancellable = true)
        public void addTypedRecipes(Supplier<RecipeType<? extends T>> recipeType, CallbackInfoReturnable<Object> cir) {
            var type = recipeType.get();
            if (type == RecipeType.BLASTING)
                cir.setReturnValue(this);
        }

        @Inject(method = "removeRecipes", at = @At("HEAD"), cancellable = true)
        public void removeRecipes(Supplier<RecipeType<? extends T>> recipeType, CallbackInfoReturnable<Object> cir) {
            recipeListConsumers.add(recipes -> {
                List<Recipe<?>> excludedRecipes = new ArrayList<>();
                CreateJEI.<T>consumeTypedRecipes(excludedRecipes::add, recipeType.get());

                Set<Item> excludedInputs = new ReferenceOpenHashSet<>();
                for (Recipe<?> r : excludedRecipes) {
                    if (!r.getIngredients().isEmpty()) {
                        for (ItemStack stack : r.getIngredients().get(0).getItems()) {
                            excludedInputs.add(stack.getItem());
                        }
                    }
                }

                recipes.removeIf(recipe -> {
                    if (recipe.getIngredients().isEmpty())
                        return false;

                    ItemStack[] inputs = recipe.getIngredients().get(0).getItems();
                    if (inputs.length == 0)
                        return false;

                    return excludedInputs.contains(inputs[0].getItem());
                });
            });
            cir.setReturnValue(this);
        }
    }
}
