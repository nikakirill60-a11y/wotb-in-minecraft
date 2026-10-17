package com.wotbminecraft.init;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.item.ItemTankAmmo;
import com.wotbminecraft.item.ItemTankRepairKit;
import com.wotbminecraft.item.ItemTankSpawner;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = WotbMinecraft.MODID)
public class ModItems {

    public static final Item TANK_SPAWNER = new ItemTankSpawner();
    public static final Item TANK_AMMO = new ItemTankAmmo();
    public static final Item TANK_REPAIR_KIT = new ItemTankRepairKit();

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                TANK_SPAWNER,
                TANK_AMMO,
                TANK_REPAIR_KIT
        );
    }
}
