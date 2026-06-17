package com.moguang.cei.common;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import net.minecraft.resources.ResourceLocation;

import com.moguang.cei.CreateEnoughItems;
import com.moguang.cei.data.CEIDatagen;

@SuppressWarnings("removal")
public class CommonProxy {

    public CommonProxy() {
        init();
    }

    public void init() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        CreateEnoughItems.REGISTRATE.registerRegistrate();
        CEIDatagen.init();
        modEventBus.addGenericListener(MachineDefinition.class, this::registerMachines);
        modEventBus.addGenericListener(GTRecipeType.class, this::registerRecipeTypes);
        modEventBus.addGenericListener(RecipeConditionType.class, this::registerRecipeConditions);
    }

    public void registerMachines(GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {}

    public void registerRecipeTypes(GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) {}

    public void registerRecipeConditions(GTCEuAPI.RegisterEvent<ResourceLocation, RecipeConditionType> event) {}
}
