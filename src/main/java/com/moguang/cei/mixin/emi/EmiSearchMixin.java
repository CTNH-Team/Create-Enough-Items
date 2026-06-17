package com.moguang.cei.mixin.emi;

import com.moguang.cei.utils.emi.collapsible.CEICollapsibleGroups;
import dev.emi.emi.search.EmiSearch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiSearch.class, remap = false)
public class EmiSearchMixin {

    @Inject(method = "bake", at = @At("TAIL"))
    private static void cei$markCollapsibleGroupsDirty(CallbackInfo ci) {
        CEICollapsibleGroups.markDirty();
    }
}
