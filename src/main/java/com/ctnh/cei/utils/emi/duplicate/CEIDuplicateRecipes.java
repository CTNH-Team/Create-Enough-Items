package com.ctnh.cei.utils.emi.duplicate;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import com.ctnh.cei.CreateEnoughItems;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 管理 EMI 配方界面的机械动力衍生重复配方过滤。 */
public class CEIDuplicateRecipes {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_FILE = FMLPaths.CONFIGDIR.get().resolve("cei/duplicate_emi_recipes.json");

    private static final Set<ResourceLocation> DERIVED_CREATE_CATEGORIES = Set.of(
            createId("automatic_shaped"),
            createId("automatic_shapeless"),
            createId("automatic_brewing"),
            createId("automatic_packing"),
            createId("block_cutting"),
            createId("fan_smoking"),
            createId("fan_blasting"));

    private static boolean hidden = false;
    private static boolean stateLoaded = false;

    private static ResourceLocation createId(String path) {
        return ResourceLocation.tryBuild("create", path);
    }

    public static boolean isHidden() {
        loadState();
        return hidden;
    }

    public static void toggleHidden() {
        loadState();
        hidden = !hidden;
        saveState();
    }

    public static Map<EmiRecipeCategory, List<EmiRecipe>> apply(Map<EmiRecipeCategory, List<EmiRecipe>> recipes) {
        if (recipes == null || recipes.isEmpty()) return recipes;
        loadState();
        if (!hidden) return recipes;

        Map<EmiRecipeCategory, List<EmiRecipe>> filtered = new LinkedHashMap<>();
        for (Map.Entry<EmiRecipeCategory, List<EmiRecipe>> entry : recipes.entrySet()) {
            EmiRecipeCategory category = entry.getKey();
            if (isDerivedCategory(category)) continue;

            List<EmiRecipe> kept = new ArrayList<>();
            for (EmiRecipe recipe : entry.getValue()) {
                if (!isGeneratedContainerRecipe(category, recipe)) {
                    kept.add(recipe);
                }
            }
            if (!kept.isEmpty()) {
                filtered.put(category, List.copyOf(kept));
            }
        }
        return filtered.isEmpty() ? recipes : filtered;
    }

    private static boolean isDerivedCategory(EmiRecipeCategory category) {
        return category != null && DERIVED_CREATE_CATEGORIES.contains(category.getId());
    }

    private static boolean isGeneratedContainerRecipe(EmiRecipeCategory category, EmiRecipe recipe) {
        if (category == null || recipe == null || recipe.getId() == null) return false;

        ResourceLocation categoryId = category.getId();
        ResourceLocation recipeId = recipe.getId();
        if (!"create".equals(categoryId.getNamespace()) || !"create".equals(recipeId.getNamespace())) {
            return false;
        }

        String categoryPath = categoryId.getPath();
        String recipePath = recipeId.getPath();
        return ("spout_filling".equals(categoryPath) &&
                (recipePath.startsWith("fill_") || "potions".equals(recipePath))) ||
                ("draining".equals(categoryPath) &&
                        (recipePath.startsWith("empty_") || "potions".equals(recipePath)));
    }

    private static void loadState() {
        if (stateLoaded) return;
        stateLoaded = true;
        if (!Files.isRegularFile(STATE_FILE)) return;

        try (Reader reader = Files.newBufferedReader(STATE_FILE)) {
            JsonObject state = JsonParser.parseReader(reader).getAsJsonObject();
            if (state.has("hidden")) {
                hidden = state.get("hidden").getAsBoolean();
            }
        } catch (RuntimeException | IOException e) {
            CreateEnoughItems.LOGGER.warn("Failed to load EMI duplicate recipe state from {}", STATE_FILE, e);
        }
    }

    private static void saveState() {
        if (!stateLoaded) return;

        try {
            Files.createDirectories(STATE_FILE.getParent());
            JsonObject state = new JsonObject();
            state.addProperty("hidden", hidden);
            try (Writer writer = Files.newBufferedWriter(STATE_FILE)) {
                GSON.toJson(state, writer);
            }
        } catch (IOException e) {
            CreateEnoughItems.LOGGER.warn("Failed to save EMI duplicate recipe state to {}", STATE_FILE, e);
        }
    }
}
