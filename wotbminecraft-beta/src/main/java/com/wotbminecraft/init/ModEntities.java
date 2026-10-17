package com.wotbminecraft.init;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.entity.EntityTank;
import com.wotbminecraft.entity.EntityTankShell;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

@Mod.EventBusSubscriber(modid = WotbMinecraft.MODID)
public class ModEntities {

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().registerAll(
                EntityEntryBuilder.create()
                        .entity(EntityTank.class)
                        .id(new ResourceLocation(WotbMinecraft.MODID, "tank"), 0)
                        .name(WotbMinecraft.MODID + ".tank")
                        .tracker(96, 3, true)
                        .build(),
                EntityEntryBuilder.create()
                        .entity(EntityTankShell.class)
                        .id(new ResourceLocation(WotbMinecraft.MODID, "tank_shell"), 1)
                        .name(WotbMinecraft.MODID + ".tank_shell")
                        .tracker(96, 3, true)
                        .build()
        );
    }
}
