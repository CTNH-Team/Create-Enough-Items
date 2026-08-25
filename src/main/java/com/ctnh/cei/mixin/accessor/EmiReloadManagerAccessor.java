package com.ctnh.cei.mixin.accessor;

import dev.emi.emi.runtime.EmiReloadManager;
import lombok.experimental.Accessors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EmiReloadManager.class, remap = false)
public interface EmiReloadManagerAccessor {
    @Accessor(value = "status", remap = false)
    static int status(){return  0;};
    @Accessor(value = "status", remap = false)
    static void status(int u){};
    @Accessor(value = "clear",remap = false)
    static boolean clear(){return  true;};
    @Accessor(value = "clear",remap = false)
    static void clear(boolean b){};
    @Accessor(value = "restart",remap = false)
    static boolean restart(){return  true;};
    @Accessor(value = "restart",remap = false)
    static void restart(boolean b){};
    @Accessor(value = "thread",remap = false)
    static Thread thread(){return null;};
    @Accessor(value = "thread",remap = false)
    static void thread(Thread t){};
}