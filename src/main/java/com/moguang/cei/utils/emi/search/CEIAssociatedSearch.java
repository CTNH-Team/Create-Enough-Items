package com.moguang.cei.utils.emi.search;

import net.minecraftforge.fml.loading.FMLPaths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.moguang.cei.CreateEnoughItems;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** 管理 EMI 配方关联搜索的开关状态，并记录当前配方页的查询对象。 */
public class CEIAssociatedSearch {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_FILE = FMLPaths.CONFIGDIR.get().resolve("cei/associated_emi_search.json");

    private static boolean enabled = true;
    private static boolean stateLoaded = false;
    private static EmiIngredient lastIngredient = EmiStack.EMPTY;
    private static LookupMode lastMode = LookupMode.RECIPES;

    public static boolean isEnabled() {
        loadState();
        return enabled;
    }

    public static void toggleEnabled() {
        loadState();
        enabled = !enabled;
        saveState();
    }

    public static void rememberRecipes(EmiIngredient ingredient) {
        remember(LookupMode.RECIPES, ingredient);
    }

    public static void rememberUses(EmiIngredient ingredient) {
        remember(LookupMode.USES, ingredient);
    }

    public static void refreshCurrentLookup() {
        if (lastIngredient == null || lastIngredient.isEmpty()) return;
        if (lastMode == LookupMode.USES) {
            EmiApi.displayUses(lastIngredient);
        } else {
            EmiApi.displayRecipes(lastIngredient);
        }
    }

    private static void remember(LookupMode mode, EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return;
        lastMode = mode;
        lastIngredient = ingredient;
    }

    private static void loadState() {
        if (stateLoaded) return;
        stateLoaded = true;
        if (!Files.isRegularFile(STATE_FILE)) return;

        try (Reader reader = Files.newBufferedReader(STATE_FILE)) {
            JsonObject state = JsonParser.parseReader(reader).getAsJsonObject();
            if (state.has("enabled")) {
                enabled = state.get("enabled").getAsBoolean();
            }
        } catch (RuntimeException | IOException e) {
            CreateEnoughItems.LOGGER.warn("Failed to load EMI associated search state from {}", STATE_FILE, e);
        }
    }

    private static void saveState() {
        if (!stateLoaded) return;

        try {
            Files.createDirectories(STATE_FILE.getParent());
            JsonObject state = new JsonObject();
            state.addProperty("enabled", enabled);
            try (Writer writer = Files.newBufferedWriter(STATE_FILE)) {
                GSON.toJson(state, writer);
            }
        } catch (IOException e) {
            CreateEnoughItems.LOGGER.warn("Failed to save EMI associated search state to {}", STATE_FILE, e);
        }
    }

    private enum LookupMode {
        RECIPES,
        USES
    }
}
