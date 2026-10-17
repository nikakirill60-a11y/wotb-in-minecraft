package com.wotbminecraft.client;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.init.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = WotbMinecraft.MODID, value = Side.CLIENT)
public class ClientRegistration {

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerItemModel(ModItems.TANK_SPAWNER);
        registerItemModel(ModItems.TANK_AMMO);
        registerItemModel(ModItems.TANK_REPAIR_KIT);
    }

    private static void registerItemModel(Item item) {
        if (item.getRegistryName() != null) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
