package com.moguang.cei.utils.emi.search;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.registry.EmiStackList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FastRecipeManager implements EmiRecipeManager {

    private final List<EmiRecipeCategory> categories;
    private final Map<EmiRecipeCategory, List<EmiIngredient>> workstations;

    private final List<EmiRecipe> recipes;
    private final Map<EmiRecipeCategory, List<EmiRecipe>> byCategory;
    private final Map<EmiStack, List<EmiRecipe>> byInput;
    private final Map<EmiStack, List<EmiRecipe>> byOutput;
    private final Map<ResourceLocation, EmiRecipe> byId;

    public FastRecipeManager(List<EmiRecipeCategory> rawCategories,
                             Map<EmiRecipeCategory, List<EmiIngredient>> rawWorkstations,
                             List<EmiRecipe> recipes) {
        this.categories = rawCategories;
        this.workstations = rawWorkstations;

        int estimated = recipes.size();
        this.recipes = recipes;
        this.byCategory = new HashMap<>();
        this.byInput = new Object2ObjectOpenCustomHashMap<>(new EmiStackList.ComparisonHashStrategy());
        this.byOutput = new Object2ObjectOpenCustomHashMap<>(new EmiStackList.ComparisonHashStrategy());
        this.byId = new HashMap<>(estimated);

        for (var recipe : recipes) {
            EmiRecipeCategory cat = recipe.getCategory();
            byCategory.computeIfAbsent(cat, c -> new ArrayList<>()).add(recipe);

            ResourceLocation id = recipe.getId();
            if (id != null && !byId.containsKey(id)) {
                byId.put(id, recipe);
            }
        }

        for (EmiRecipe recipe : recipes) {
            for (EmiIngredient ing : recipe.getInputs()) {
                for (EmiStack stack : ing.getEmiStacks()) {
                    byInput.computeIfAbsent(stack, s -> new ArrayList<>()).add(recipe);
                }
            }

            for (EmiIngredient ing : recipe.getCatalysts()) {
                for (EmiStack stack : ing.getEmiStacks()) {
                    byInput.computeIfAbsent(stack, s -> new ArrayList<>()).add(recipe);
                }
            }

            Set<EmiStack> uniqueOutputs = new ObjectOpenCustomHashSet<>(new EmiStackList.ComparisonHashStrategy());
            for (EmiStack stack : recipe.getOutputs()) {
                if (!uniqueOutputs.add(stack)) {
                    continue;
                }
                byOutput.computeIfAbsent(stack, s -> new ArrayList<>()).add(recipe);
            }
        }

        for (Map.Entry<EmiRecipeCategory, List<EmiIngredient>> e : workstations.entrySet()) {
            EmiRecipeCategory category = e.getKey();
            List<EmiRecipe> catRecipes = byCategory.get(category);
            if (catRecipes == null || catRecipes.isEmpty()) {
                continue;
            }

            for (EmiIngredient ingredient : e.getValue()) {
                for (EmiStack stack : ingredient.getEmiStacks()) {
                    EmiRecipes.byWorkstation.computeIfAbsent(stack, s -> new ArrayList<>()).addAll(catRecipes);
                }
            }
        }
    }

    @Override
    public List<EmiRecipeCategory> getCategories() {
        return categories;
    }

    @Override
    public List<EmiIngredient> getWorkstations(EmiRecipeCategory category) {
        return workstations.getOrDefault(category, List.of());
    }

    @Override
    public List<EmiRecipe> getRecipes() {
        return recipes;
    }

    @Override
    public List<EmiRecipe> getRecipes(EmiRecipeCategory category) {
        return byCategory.getOrDefault(category, List.of());
    }

    @Override
    public @Nullable EmiRecipe getRecipe(ResourceLocation id) {
        return byId.get(id);
    }

    @Override
    public List<EmiRecipe> getRecipesByInput(EmiStack stack) {
        return byInput.getOrDefault(stack, List.of());
    }

    @Override
    public List<EmiRecipe> getRecipesByOutput(EmiStack stack) {
        return byOutput.getOrDefault(stack, List.of());
    }
}
