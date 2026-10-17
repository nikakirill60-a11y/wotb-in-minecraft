package com.wotbminecraft.client;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.init.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = WotbMinecraft.MODID, value = Side.CLIENT)
public class ClientRegistration {

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(ModItems.TANK_SPAWNER, 0,
                new ModelResourceLocation(WotbMinecraft.MODID + ":tank_spawner", "inventory"));
    }
}
