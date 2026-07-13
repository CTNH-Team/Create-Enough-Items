package com.ctnh.cei.registry;

import com.ctnh.cei.CreateEnoughItems;
import tech.vixhentx.mcmod.ctnhlib.registrate.CNRegistrate;

public class CEIRegistrate extends CNRegistrate {

    protected CEIRegistrate() {
        super(CreateEnoughItems.MODID);
    }

    public static CEIRegistrate create() {
        return new CEIRegistrate();
    }
}
