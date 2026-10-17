package com.wotbminecraft.proxy;

import com.wotbminecraft.client.input.ModKeyBindings;
import com.wotbminecraft.client.render.RenderTank;
import com.wotbminecraft.client.render.RenderTankShell;
import com.wotbminecraft.entity.EntityTank;
import com.wotbminecraft.entity.EntityTankShell;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(EntityTank.class, RenderTank::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityTankShell.class, RenderTankShell::new);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ModKeyBindings.init();
    }
}
