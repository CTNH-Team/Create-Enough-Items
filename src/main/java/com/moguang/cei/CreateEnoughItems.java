package com.moguang.cei;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import com.moguang.cei.client.ClientProxy;
import com.moguang.cei.common.CommonProxy;
import com.moguang.cei.registry.CEIRegistrate;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@SuppressWarnings("removal")
@Mod(CreateEnoughItems.MODID)
public class CreateEnoughItems {

    public static final String MODID = "cei";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final CEIRegistrate REGISTRATE = CEIRegistrate.create();

    public CreateEnoughItems() {
        DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.tryParse(MODID + ":" + name);
    }
}
