package com.moguang.cei.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

import com.moguang.cei.CreateEnoughItems;
import com.moguang.cei.common.CommonProxy;

@Mod.EventBusSubscriber(modid = CreateEnoughItems.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientProxy extends CommonProxy {

    public ClientProxy() {
        super();
    }
}
