package com.ctnh.cei.event;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.ctnh.cei.CreateEnoughItems;
import com.ctnh.cei.utils.emi.TooltipBakeQueue;
import dev.emi.emi.search.EmiSearch;

@Mod.EventBusSubscriber(modid = CreateEnoughItems.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeClientEventHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        TooltipBakeQueue queue = TooltipBakeQueue.INSTANCE;
        if (queue != null && !TooltipBakeQueue.ready && GTValues.CLIENT_TIME % 20 == 0) {
            boolean done = queue.tick();
            if (done) {
                TooltipBakeQueue.ready = true;
                queue.tooltips.generate();
                EmiSearch.tooltips = queue.tooltips;
            }
        }
    }
}
