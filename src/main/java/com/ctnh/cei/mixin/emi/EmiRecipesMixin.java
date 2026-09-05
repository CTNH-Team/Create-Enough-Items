package com.ctnh.cei.mixin.emi;

import com.ctnh.cei.utils.emi.search.FastRecipeManager;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.runtime.EmiLog;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = EmiRecipes.class, remap = false)
public class EmiRecipesMixin {

    @Shadow
    public static EmiRecipeManager manager;

    @Shadow
    public static List<EmiRecipeCategory> categories;

    @Shadow
    private static Map<EmiRecipeCategory, List<EmiIngredient>> workstations;

    @Shadow
    private static List<EmiRecipe> recipes;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void bake() {
        long start = System.currentTimeMillis();
        manager = new FastRecipeManager(categories, workstations, recipes);
        EmiLog.info("Fast baked recipes in " + (System.currentTimeMillis() - start) + "ms");
    }
    @Redirect(method = "<clinit>", at = @At(value = "FIELD", target = "Ldev/emi/emi/registry/EmiRecipes;workstations:Ljava/util/Map;", opcode = Opcodes.PUTSTATIC))
    private static void cei$newHashMap(Map<EmiRecipeCategory, List<EmiIngredient>> value) {
        workstations = new ConcurrentHashMap<>(value);
    }
}
