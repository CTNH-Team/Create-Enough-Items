package com.moguang.cei.mixin.emi;

import net.minecraft.network.chat.Component;

import com.moguang.cei.utils.emi.TooltipBakeQueue;
import com.moguang.cei.utils.emi.collapsible.CEICollapsibleGroups;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.search.EmiSearch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = EmiSearch.class, remap = false)
public class EmiSearchMixin {

    @Redirect(method = "bake",
              at = @At(value = "INVOKE", target = "Ldev/emi/emi/api/stack/EmiStack;getTooltipText()Ljava/util/List;"))
    private static List<Component> noTooltip(EmiStack instance) {
        return null;
    }

    @Inject(method = "bake", at = @At("TAIL"))
    private static void cei$markCollapsibleGroupsDirty(CallbackInfo ci) {
        CEICollapsibleGroups.markDirty();
        TooltipBakeQueue.INSTANCE = new TooltipBakeQueue(EmiStackList.stacks);
        TooltipBakeQueue.ready = false;
    }

    /**
     * @author gpt-5
     * @reason fast
     */
    @Overwrite(remap = false)
    public static void search(String query) {
        final List<? extends EmiIngredient> source = EmiScreenManager.getSearchSource();

        final EmiSearch.CompiledQuery compiled = new EmiSearch.CompiledQuery(query);
        EmiSearch.compiledQuery = compiled;

        if (compiled.isEmpty()) {
            synchronized (EmiSearch.class) {
                EmiSearch.stacks = source;
            }
            return;
        }

        final ArrayList<EmiIngredient> fastResult = new ArrayList<>(source.size());

        final boolean bakedReady = EmiSearch.bakedStacks != null;

        for (EmiIngredient ingredient : source) {
            List<EmiStack> stacks = ingredient.getEmiStacks();
            if (stacks.size() != 1) {
                continue;
            }

            EmiStack stack = stacks.get(0);

            boolean matched = false;

            if (bakedReady && EmiSearch.bakedStacks.contains(stack)) {
                matched = compiled.fullQuery.matches(stack);
            }

            if (matched) {
                fastResult.add(ingredient);
            }
        }

        synchronized (EmiSearch.class) {
            EmiSearch.stacks = fastResult;
        }
    }
}
