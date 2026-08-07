package com.ctnh.cei.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

import com.ctnh.cei.CreateEnoughItems;
import com.ctnh.cei.common.CommonProxy;
import com.ctnh.cei.utils.emi.collapsible.CEICollapsibleGroups;

@Mod.EventBusSubscriber(modid = CreateEnoughItems.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientProxy extends CommonProxy {

    public ClientProxy() {
        super();
        CEICollapsibleGroups.loadRules();
    }
}
