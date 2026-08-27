package com.ctnh.cei.falseMixin;

import net.minecraft.client.Minecraft;

import com.google.common.collect.Lists;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.jemi.JemiPlugin;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.registry.*;
import dev.emi.emi.runtime.*;
import dev.emi.emi.screen.EmiScreenBase;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.search.EmiSearch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Mixin(targets = "dev.emi.emi.runtime.EmiReloadManager$ReloadWorker", remap = false)
public abstract class PreReloadWorkerMixin {

    @Shadow
    private static int lambda$run$0(EmiPluginContainer par1, EmiPluginContainer par2) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    /**
     * @author n1
     * @reason 我怎么知道
     */
    @Overwrite(remap = false)
    public void run() {
        int retries = 3;

        label121:
        do {
            try {
                if (!EmiReloadManager.clear) {
                    EmiLog.info("Starting EMI reload..._");
                }

                long reloadStart = System.currentTimeMillis();
                EmiReloadManager.restart = false;
                EmiReloadManager.step(EmiPort.literal("Clearing data"));
                EmiRecipes.clear();
                EmiStackList.clear();
                EmiIngredientSerializers.clear();
                EmiExclusionAreas.clear();
                EmiDragDropHandlers.clear();
                EmiStackProviders.clear();
                EmiRecipeFiller.clear();
                EmiHidden.clear();
                EmiTags.ADAPTERS_BY_CLASS.map().clear();
                EmiTags.ADAPTERS_BY_REGISTRY.clear();
                EmiScreenBase.clearScreenBoundsProviders();
                if (EmiReloadManager.clear) {
                    EmiReloadManager.clear = false;
                } else {
                    Minecraft client = Minecraft.getInstance();
                    if (client.level == null) {
                        EmiReloadLog.warn("World is null");
                        break;
                    }

                    if (client.level.getRecipeManager() == null) {
                        EmiReloadLog.warn("Recipe Manager is null");
                        break;
                    }

                    List<EmiPluginContainer> plugins = Lists.newArrayList();
                    plugins.addAll(EmiAgnos.getPlugins().stream().sorted(PreReloadWorkerMixin::lambda$run$0).toList());
                    if (EmiAgnos.isModLoaded("jei")) {
                        plugins.add(new EmiPluginContainer(new JemiPlugin(), "jemi"));
                    }

                    EmiInitRegistry initRegistry = new EmiInitRegistryImpl();

                    for (EmiPluginContainer container : plugins) {
                        EmiReloadManager.step(EmiPort.literal("Initializing plugin from " + container.id()), 5000L);
                        long start = System.currentTimeMillis();

                        try {
                            container.plugin().initialize(initRegistry);
                        } catch (Throwable e) {
                            EmiReloadLog.warn("Exception initializing plugin provided by " + container.id(), e);
                            if (EmiReloadManager.restart) {
                                continue label121;
                            }
                            continue;
                        }

                        EmiLog.info("Initialized plugin from " + container.id() + " in " +
                                (System.currentTimeMillis() - start) + "ms");
                    }

                    EmiHidden.reload();
                    EmiReloadManager.step(EmiPort.literal("Processing tags"));
                    EmiTags.reload();
                    EmiReloadManager.step(EmiPort.literal("Constructing index"));
                    EmiComparisonDefaults.comparisons = new HashMap();
                    EmiStackList.reload();
                    if (!EmiReloadManager.restart) {
                        EmiRegistry registry = new EmiRegistryImpl();

                        for (EmiPluginContainer container : plugins) {
                            EmiReloadManager.step(EmiPort.literal("Loading plugin from " + container.id()), 10000L);
                            long start = System.currentTimeMillis();

                            try {
                                container.plugin().register(registry);
                            } catch (Throwable e) {
                                EmiReloadLog.warn("Exception loading plugin provided by " + container.id(), e);
                                if (EmiReloadManager.restart) {
                                    continue label121;
                                }
                                continue;
                            }

                            EmiLog.info("Reloaded plugin from " + container.id() + " in " +
                                    (System.currentTimeMillis() - start) + "ms");
                            if (EmiReloadManager.restart) {
                                continue label121;
                            }
                        }

                        if (!EmiReloadManager.restart) {
                            EmiReloadManager.step(EmiPort.literal("Baking index"));
                            EmiStackList.bake();
                            EmiReloadManager.step(EmiPort.literal("Registering late recipes"), 10000L);
                            Objects.requireNonNull(registry);
                            Consumer<EmiRecipe> registerLateRecipe = registry::addRecipe;

                            for (Consumer<Consumer<EmiRecipe>> consumer : EmiRecipes.lateRecipes) {
                                try {
                                    consumer.accept(registerLateRecipe);
                                } catch (Exception e) {
                                    EmiReloadLog.warn("Exception loading late recipes for plugins:", e);
                                    if (EmiReloadManager.restart) {
                                        continue label121;
                                    }
                                }
                            }

                            EmiReloadManager.step(EmiPort.literal("Baking recipes"), 15000L);
                            EmiRecipes.bake();
                            BoM.reload();
                            EmiPersistentData.load();
                            EmiReloadManager.step(EmiPort.literal("Baking search"), 15000L);
                            EmiSearch.bake();
                            EmiReloadManager.step(EmiPort.literal("Finishing up"));
                            EmiScreenManager.search.update();
                            EmiScreenManager.forceRecalculate();
                            EmiReloadLog.bake();
                            EmiLog.info("Reloaded EMI in " + (System.currentTimeMillis() - reloadStart) + "ms");
                            EmiReloadManager.status = 2;
                        }
                    }
                }
            } catch (Throwable e) {
                EmiReloadLog.warn("Critical error occured during reload:", e);
                EmiReloadManager.status = -1;
                if (retries-- > 0) {
                    EmiReloadManager.restart = true;
                }
            }
        } while (EmiReloadManager.restart);

        EmiReloadManager.thread = null;
    }
}
