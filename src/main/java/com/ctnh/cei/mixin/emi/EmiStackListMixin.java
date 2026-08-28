package com.ctnh.cei.mixin.emi;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

@Mixin(value = EmiStackList.class, remap = false)
public class EmiStackListMixin {

    @Redirect(method = { "reload", "bake" },
              at = @At(value = "FIELD",
                       target = "Ldev/emi/emi/registry/EmiStackList;stacks:Ljava/util/List;",
                       opcode = Opcodes.PUTSTATIC,
                       remap = false),
              remap = false)
    private static void cei$reload(List<EmiStack> stacks) {
        EmiStackList.stacks = Collections.synchronizedList(stacks);
    }
}
