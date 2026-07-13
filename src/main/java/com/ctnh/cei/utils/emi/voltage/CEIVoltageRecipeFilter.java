package com.ctnh.cei.utils.emi.voltage;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.integration.emi.recipe.GTEmiRecipe;

import net.minecraftforge.fml.loading.FMLPaths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ctnh.cei.CreateEnoughItems;
import com.ctnh.cei.mixin.emi.accessor.GTEmiRecipeAccessor;
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
import java.util.OptionalInt;

/** 管理 EMI 配方界面的 GT 电压区间过滤。 */
public class CEIVoltageRecipeFilter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_FILE = FMLPaths.CONFIGDIR.get().resolve("cei/voltage_emi_recipes.json");

    private static int minTier = GTValues.LV;
    private static int maxTier = GTValues.MAX;
    private static boolean stateLoaded = false;

    public static int getMinTier() {
        loadState();
        return minTier;
    }

    public static int getMaxTier() {
        loadState();
        return maxTier;
    }

    public static String getMinTierName() {
        return getTierName(getMinTier());
    }

    public static String getMaxTierName() {
        return getTierName(getMaxTier());
    }

    public static void setMinTier(int tier) {
        loadState();
        minTier = clampTier(tier);
        if (minTier > maxTier) {
            maxTier = minTier;
        }
        saveState();
    }

    public static void adjustMinTier(int delta) {
        setMinTier(cycleTier(getMinTier(), delta));
    }

    public static void setMaxTier(int tier) {
        loadState();
        maxTier = clampTier(tier);
        if (maxTier < minTier) {
            minTier = maxTier;
        }
        saveState();
    }

    public static void adjustMaxTier(int delta) {
        setMaxTier(cycleTier(getMaxTier(), delta));
    }

    public static Map<EmiRecipeCategory, List<EmiRecipe>> apply(Map<EmiRecipeCategory, List<EmiRecipe>> recipes) {
        if (recipes == null || recipes.isEmpty()) return recipes;
        loadState();

        Map<EmiRecipeCategory, List<EmiRecipe>> filtered = new LinkedHashMap<>();
        for (Map.Entry<EmiRecipeCategory, List<EmiRecipe>> entry : recipes.entrySet()) {
            List<EmiRecipe> kept = new ArrayList<>();
            for (EmiRecipe recipe : entry.getValue()) {
                OptionalInt tier = voltageTier(recipe);
                if (tier.isEmpty() || tier.getAsInt() >= minTier && tier.getAsInt() <= maxTier) {
                    kept.add(recipe);
                }
            }
            if (!kept.isEmpty()) {
                filtered.put(entry.getKey(), List.copyOf(kept));
            }
        }
        return filtered.isEmpty() ? recipes : filtered;
    }

    private static OptionalInt voltageTier(EmiRecipe recipe) {
        if (!(recipe instanceof GTEmiRecipe gtEmiRecipe)) {
            return OptionalInt.empty();
        }

        var gtRecipe = ((GTEmiRecipeAccessor) gtEmiRecipe).cei$getRecipe();
        if (gtRecipe == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(gtRecipe.tier);
    }

    private static int clampTier(int tier) {
        if (tier < GTValues.LV) return GTValues.LV;
        if (tier > GTValues.MAX) return GTValues.MAX;
        return tier;
    }

    private static int cycleTier(int tier, int delta) {
        int next = clampTier(tier) + delta;
        if (next < GTValues.LV) return GTValues.MAX;
        if (next > GTValues.MAX) return GTValues.LV;
        return next;
    }

    public static String getTierName(int tier) {
        int normalized = clampTier(tier);
        if (normalized >= 0 && normalized < GTValues.VN.length) {
            return GTValues.VN[normalized];
        }
        return "LV";
    }

    private static void loadState() {
        if (stateLoaded) return;
        stateLoaded = true;
        if (!Files.isRegularFile(STATE_FILE)) return;

        try (Reader reader = Files.newBufferedReader(STATE_FILE)) {
            JsonObject state = JsonParser.parseReader(reader).getAsJsonObject();
            if (state.has("minTier")) {
                minTier = clampTier(state.get("minTier").getAsInt());
            }
            if (state.has("maxTier")) {
                maxTier = clampTier(state.get("maxTier").getAsInt());
            }
            if (minTier > maxTier) {
                minTier = GTValues.LV;
                maxTier = GTValues.MAX;
            }
        } catch (RuntimeException | IOException e) {
            CreateEnoughItems.LOGGER.warn("Failed to load EMI voltage recipe state from {}", STATE_FILE, e);
        }
    }

    private static void saveState() {
        if (!stateLoaded) return;

        try {
            Files.createDirectories(STATE_FILE.getParent());
            JsonObject state = new JsonObject();
            state.addProperty("minTier", minTier);
            state.addProperty("maxTier", maxTier);
            try (Writer writer = Files.newBufferedWriter(STATE_FILE)) {
                GSON.toJson(state, writer);
            }
        } catch (IOException e) {
            CreateEnoughItems.LOGGER.warn("Failed to save EMI voltage recipe state to {}", STATE_FILE, e);
        }
    }
}
