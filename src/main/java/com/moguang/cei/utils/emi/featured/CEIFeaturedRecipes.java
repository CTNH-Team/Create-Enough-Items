package com.moguang.cei.utils.emi.featured;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.moguang.cei.CreateEnoughItems;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** 管理 EMI 配方界面的“精选配方”过滤规则和开关状态。 */
public class CEIFeaturedRecipes {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_FILE = FMLPaths.CONFIGDIR.get().resolve("cei/featured_emi_recipes.json");
    private static final String DEFAULT_RULE_RESOURCE = "/assets/cei/emi/emi_featured_recipes.json";

    private static boolean enabled = false;
    private static boolean stateLoaded = false;
    private static boolean rulesLoaded = false;
    private static List<RecipeRuleDefinition> configuredRules = List.of();

    public static boolean isEnabled() {
        loadState();
        return enabled;
    }

    public static void toggleEnabled() {
        loadState();
        enabled = !enabled;
        saveState();
    }

    public static boolean hasRules() {
        loadRules();
        return !configuredRules.isEmpty();
    }

    public static Map<EmiRecipeCategory, List<EmiRecipe>> copyOf(Map<EmiRecipeCategory, List<EmiRecipe>> recipes) {
        Map<EmiRecipeCategory, List<EmiRecipe>> copied = new LinkedHashMap<>();
        if (recipes == null) return copied;
        for (Map.Entry<EmiRecipeCategory, List<EmiRecipe>> entry : recipes.entrySet()) {
            copied.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copied;
    }

    public static Map<EmiRecipeCategory, List<EmiRecipe>> apply(Map<EmiRecipeCategory, List<EmiRecipe>> recipes) {
        if (recipes == null || recipes.isEmpty()) return recipes;
        loadState();
        loadRules();
        if (!enabled || configuredRules.isEmpty()) return recipes;

        Map<EmiRecipeCategory, List<EmiRecipe>> filtered = new LinkedHashMap<>();
        for (Map.Entry<EmiRecipeCategory, List<EmiRecipe>> entry : recipes.entrySet()) {
            List<EmiRecipe> kept = new ArrayList<>();
            for (EmiRecipe recipe : entry.getValue()) {
                if (!shouldHide(recipe)) {
                    kept.add(recipe);
                }
            }
            if (!kept.isEmpty()) {
                filtered.put(entry.getKey(), List.copyOf(kept));
            }
        }
        return filtered.isEmpty() ? recipes : filtered;
    }

    private static boolean shouldHide(EmiRecipe recipe) {
        if (recipe == null) return false;
        for (RecipeRuleDefinition definition : configuredRules) {
            if (definition.matches(recipe)) {
                return true;
            }
        }
        return false;
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
            CreateEnoughItems.LOGGER.warn("Failed to load EMI featured recipe state from {}", STATE_FILE, e);
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
            CreateEnoughItems.LOGGER.warn("Failed to save EMI featured recipe state to {}", STATE_FILE, e);
        }
    }

    private static void loadRules() {
        if (rulesLoaded) return;
        rulesLoaded = true;

        List<RecipeRuleDefinition> definitions = new ArrayList<>();
        addRuleDefinitions(DEFAULT_RULE_RESOURCE, defaultRuleRoot(), definitions);
        configuredRules = List.copyOf(definitions);
    }

    private static JsonObject defaultRuleRoot() {
        try (InputStream stream = CEIFeaturedRecipes.class.getResourceAsStream(DEFAULT_RULE_RESOURCE)) {
            if (stream == null) {
                CreateEnoughItems.LOGGER.warn("Missing default EMI featured recipe rule resource {}",
                        DEFAULT_RULE_RESOURCE);
                return new JsonObject();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonObject()) {
                    return root.getAsJsonObject();
                }
                CreateEnoughItems.LOGGER.warn("Default EMI featured recipe rule resource {} must be a JSON object",
                        DEFAULT_RULE_RESOURCE);
            }
        } catch (RuntimeException | IOException e) {
            CreateEnoughItems.LOGGER.warn("Failed to read default EMI featured recipe rules from {}",
                    DEFAULT_RULE_RESOURCE,
                    e);
        }
        return new JsonObject();
    }

    private static void addRuleDefinitions(String sourceName, JsonElement root,
                                           List<RecipeRuleDefinition> definitions) {
        if (!root.isJsonObject()) {
            CreateEnoughItems.LOGGER.warn("EMI featured recipe rule file {} must be a JSON object", sourceName);
            return;
        }

        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
            RecipeRuleDefinition definition = parseRuleDefinition(entry.getKey(), entry.getValue());
            if (definition != null && !definition.rules().isEmpty()) {
                definitions.add(definition);
            }
        }
    }

    @Nullable
    private static RecipeRuleDefinition parseRuleDefinition(String guid, JsonElement element) {
        List<RecipeRule> rules = new ArrayList<>();
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            addRule(guid, element.getAsString(), rules);
        } else if (element.isJsonArray()) {
            for (JsonElement ruleElement : element.getAsJsonArray()) {
                if (ruleElement.isJsonPrimitive() && ruleElement.getAsJsonPrimitive().isString()) {
                    addRule(guid, ruleElement.getAsString(), rules);
                } else {
                    CreateEnoughItems.LOGGER.warn("Ignoring non-string EMI featured recipe rule in group {}", guid);
                }
            }
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement rulesElement = object.has("key") ? object.get("key") : object.get("rules");
            if (rulesElement == null) {
                CreateEnoughItems.LOGGER.warn("Ignoring EMI featured recipe group {} because it has no key/rules",
                        guid);
            } else if (rulesElement.isJsonPrimitive() && rulesElement.getAsJsonPrimitive().isString()) {
                addRule(guid, rulesElement.getAsString(), rules);
            } else if (rulesElement.isJsonArray()) {
                for (JsonElement ruleElement : rulesElement.getAsJsonArray()) {
                    if (ruleElement.isJsonPrimitive() && ruleElement.getAsJsonPrimitive().isString()) {
                        addRule(guid, ruleElement.getAsString(), rules);
                    } else {
                        CreateEnoughItems.LOGGER.warn("Ignoring non-string EMI featured recipe rule in group {}", guid);
                    }
                }
            } else {
                CreateEnoughItems.LOGGER.warn(
                        "Ignoring EMI featured recipe group {} because key/rules is not a string or string array",
                        guid);
            }
        } else {
            CreateEnoughItems.LOGGER.warn(
                    "Ignoring EMI featured recipe group {} because its rule is not a string, string array, or object",
                    guid);
        }

        if (rules.isEmpty()) return null;
        return new RecipeRuleDefinition(guid, List.copyOf(rules));
    }

    private static void addRule(String guid, String rule, List<RecipeRule> rules) {
        rule = rule.trim();
        if (rule.isEmpty()) return;

        try {
            if (rule.startsWith("recipe_regex:")) {
                rules.add(new RecipeIdRegexRule(Pattern.compile(rule.substring("recipe_regex:".length()))));
            } else if (rule.startsWith("recipe:")) {
                ResourceLocation id = ResourceLocation.tryParse(rule.substring("recipe:".length()));
                if (id == null) throw new IllegalArgumentException("Invalid recipe id");
                rules.add(new RecipeIdRule(id));
            } else if (rule.startsWith("category_regex:")) {
                rules.add(new CategoryRegexRule(Pattern.compile(rule.substring("category_regex:".length()))));
            } else if (rule.startsWith("category:")) {
                ResourceLocation id = ResourceLocation.tryParse(rule.substring("category:".length()));
                if (id == null) throw new IllegalArgumentException("Invalid category id");
                rules.add(new CategoryRule(id));
            } else if (rule.startsWith("input:")) {
                rules.add(new IngredientRule(SlotType.INPUT, parseExpressionRule(rule.substring("input:".length()))));
            } else if (rule.startsWith("output:")) {
                rules.add(new IngredientRule(SlotType.OUTPUT, parseExpressionRule(rule.substring("output:".length()))));
            } else if (rule.startsWith("catalyst:")) {
                rules.add(new IngredientRule(SlotType.CATALYST,
                        parseExpressionRule(rule.substring("catalyst:".length()))));
            } else if (rule.startsWith("item:")) {
                rules.add(new IngredientRule(SlotType.ANY, parseExpressionRule(rule.substring("item:".length()))));
            } else {
                rules.add(new IngredientRule(SlotType.OUTPUT, parseExpressionRule(rule)));
            }
        } catch (IllegalArgumentException e) {
            CreateEnoughItems.LOGGER.warn("Ignoring invalid EMI featured recipe rule {} in group {}", rule, guid, e);
        }
    }

    private static ItemRule parseExpressionRule(String rule) {
        List<ItemRule> alternatives = new ArrayList<>();
        for (String part : rule.split("\\|")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                alternatives.add(parseAllRule(trimmed));
            }
        }
        if (alternatives.isEmpty()) throw new IllegalArgumentException("Empty rule");
        if (alternatives.size() == 1) return alternatives.get(0);
        return new AnyItemRule(List.copyOf(alternatives));
    }

    private static ItemRule parseAllRule(String rule) {
        String[] tokens = rule.split("\\s+");
        List<ItemRule> rules = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isBlank()) {
                rules.add(parseTokenRule(token));
            }
        }
        if (rules.isEmpty()) throw new IllegalArgumentException("Empty rule");
        if (rules.size() == 1) return rules.get(0);
        return new AllItemRule(List.copyOf(rules));
    }

    private static ItemRule parseTokenRule(String token) {
        List<ItemRule> alternatives = new ArrayList<>();
        for (String part : token.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                alternatives.add(parseSingleRule(trimmed));
            }
        }
        if (alternatives.isEmpty()) throw new IllegalArgumentException("Empty token");
        if (alternatives.size() == 1) return alternatives.get(0);
        return new AnyItemRule(List.copyOf(alternatives));
    }

    private static ItemRule parseSingleRule(String token) {
        if (token.startsWith("!")) {
            return new NotItemRule(parseSingleRule(token.substring(1)));
        }
        if (token.startsWith("$")) {
            ResourceLocation tagId = ResourceLocation.tryParse("forge:" + token.substring(1).toLowerCase(Locale.ROOT));
            if (tagId == null) throw new IllegalArgumentException("Invalid ore/tag token " + token);
            return tagRule(tagId);
        }
        if (token.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(token.substring(1));
            if (tagId == null) throw new IllegalArgumentException("Invalid tag token " + token);
            return tagRule(tagId);
        }
        if (token.startsWith("regex:")) {
            return new ItemRegexRule(Pattern.compile(token.substring("regex:".length())));
        }
        if (token.startsWith("r/") && token.endsWith("/") && token.length() > 3) {
            return new ItemRegexRule(Pattern.compile(token.substring(2, token.length() - 1)));
        }
        if (token.matches("\\d+(?:-\\d+)?")) {
            return parseDamageRule(token);
        }

        ResourceLocation id = ResourceLocation.tryParse(token);
        if (id == null) {
            throw new IllegalArgumentException("Invalid item id " + token);
        }
        return new ItemIdRule(id);
    }

    private static TagItemRule tagRule(ResourceLocation tagId) {
        return new TagItemRule(TagKey.create(Registries.ITEM, tagId), TagKey.create(Registries.BLOCK, tagId));
    }

    private static ItemRule parseDamageRule(String token) {
        if (token.contains("-")) {
            String[] parts = token.split("-", 2);
            return new DamageRule(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        int damage = Integer.parseInt(token);
        return new DamageRule(damage, damage);
    }

    private record RecipeRuleDefinition(String guid, List<RecipeRule> rules) {

        boolean matches(EmiRecipe recipe) {
            for (RecipeRule rule : rules) {
                if (rule.matches(recipe)) return true;
            }
            return false;
        }
    }

    private interface RecipeRule {

        boolean matches(EmiRecipe recipe);
    }

    private record RecipeIdRule(ResourceLocation id) implements RecipeRule {

        @Override
        public boolean matches(EmiRecipe recipe) {
            return Objects.equals(recipe.getId(), id);
        }
    }

    private record RecipeIdRegexRule(Pattern pattern) implements RecipeRule {

        @Override
        public boolean matches(EmiRecipe recipe) {
            ResourceLocation id = recipe.getId();
            return id != null && pattern.matcher(id.toString()).matches();
        }
    }

    private record CategoryRule(ResourceLocation id) implements RecipeRule {

        @Override
        public boolean matches(EmiRecipe recipe) {
            EmiRecipeCategory category = recipe.getCategory();
            return category != null && Objects.equals(category.getId(), id);
        }
    }

    private record CategoryRegexRule(Pattern pattern) implements RecipeRule {

        @Override
        public boolean matches(EmiRecipe recipe) {
            EmiRecipeCategory category = recipe.getCategory();
            ResourceLocation id = category == null ? null : category.getId();
            return id != null && pattern.matcher(id.toString()).matches();
        }
    }

    private record IngredientRule(SlotType type, ItemRule rule) implements RecipeRule {

        @Override
        public boolean matches(EmiRecipe recipe) {
            if (type == SlotType.INPUT || type == SlotType.ANY) {
                for (EmiIngredient ingredient : recipe.getInputs()) {
                    if (matchesIngredient(ingredient, rule)) return true;
                }
            }
            if (type == SlotType.OUTPUT || type == SlotType.ANY) {
                for (EmiStack stack : recipe.getOutputs()) {
                    if (matchesStack(stack, rule)) return true;
                }
            }
            if (type == SlotType.CATALYST || type == SlotType.ANY) {
                for (EmiIngredient ingredient : recipe.getCatalysts()) {
                    if (matchesIngredient(ingredient, rule)) return true;
                }
            }
            return false;
        }
    }

    private static boolean matchesIngredient(EmiIngredient ingredient, ItemRule rule) {
        if (ingredient == null) return false;
        for (EmiStack stack : ingredient.getEmiStacks()) {
            if (matchesStack(stack, rule)) return true;
        }
        return false;
    }

    private static boolean matchesStack(EmiStack stack, ItemRule rule) {
        if (stack == null) return false;
        ItemStack itemStack = stack.getItemStack();
        return !itemStack.isEmpty() && rule.matches(itemStack);
    }

    private enum SlotType {
        INPUT,
        OUTPUT,
        CATALYST,
        ANY
    }

    private interface ItemRule {

        boolean matches(ItemStack stack);
    }

    private record AnyItemRule(List<ItemRule> rules) implements ItemRule {

        @Override
        public boolean matches(ItemStack stack) {
            for (ItemRule rule : rules) {
                if (rule.matches(stack)) return true;
            }
            return false;
        }
    }

    private record AllItemRule(List<ItemRule> rules) implements ItemRule {

        @Override
        public boolean matches(ItemStack stack) {
            for (ItemRule rule : rules) {
                if (!rule.matches(stack)) return false;
            }
            return true;
        }
    }

    private record NotItemRule(ItemRule rule) implements ItemRule {

        @Override
        public boolean matches(ItemStack stack) {
            return !rule.matches(stack);
        }
    }

    private record TagItemRule(TagKey<Item> itemTag, TagKey<Block> blockTag) implements ItemRule {

        @Override
        public boolean matches(ItemStack stack) {
            if (stack.is(itemTag)) return true;
            return stack.getItem() instanceof BlockItem blockItem &&
                    blockItem.getBlock().defaultBlockState().is(blockTag);
        }
    }

    private record ItemRegexRule(Pattern pattern) implements ItemRule {

        @Override
        public boolean matches(ItemStack stack) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id != null && pattern.matcher(id.toString()).matches();
        }
    }

    private record ItemIdRule(ResourceLocation id) implements ItemRule {

        @Override
        public boolean matches(ItemStack stack) {
            ResourceLocation stackId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (stackId == null || !Objects.equals(stackId.getNamespace(), id.getNamespace())) return false;
            if (Objects.equals(stackId, id)) return true;

            Item item = ForgeRegistries.ITEMS.getValue(id);
            return item == null || item == Items.AIR ? stackId.getPath().startsWith(id.getPath()) : false;
        }
    }

    private record DamageRule(int min, int max) implements ItemRule {

        @Override
        public boolean matches(ItemStack stack) {
            int damage = stack.getDamageValue();
            return damage >= min && damage <= max;
        }
    }
}
