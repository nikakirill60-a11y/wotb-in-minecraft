package com.wotbminecraft.init;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.item.ItemTankSpawner;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = WotbMinecraft.MODID)
public class ModItems {

    public static final Item TANK_SPAWNER = new ItemTankSpawner();

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(TANK_SPAWNER);
    }
}
