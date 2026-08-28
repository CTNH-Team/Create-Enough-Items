package com.ctnh.cei.mixin.emi;

import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.registry.EmiStackProviders;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = EmiStackProviders.class)
public class EmiStackProvidersMixin {

    @Redirect(method = "<clinit>",
              at = @At(value = "FIELD",
                       target = "Ldev/emi/emi/registry/EmiStackProviders;fromClass:Ljava/util/Map;",
                       opcode = Opcodes.PUTSTATIC))
    private static void cei$reload(Map<String, EmiStackProvider> fromClass) {
        EmiStackProviders.fromClass = new ConcurrentHashMap<>();
    }
}
