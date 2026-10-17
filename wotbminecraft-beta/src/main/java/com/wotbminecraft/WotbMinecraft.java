package com.wotbminecraft;

import com.wotbminecraft.network.ModNetwork;
import com.wotbminecraft.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = WotbMinecraft.MODID, name = WotbMinecraft.NAME, version = WotbMinecraft.VERSION,
        acceptedMinecraftVersions = "[1.12.2]")
public class WotbMinecraft {
    public static final String MODID = "wotbminecraft";
    public static final String NAME = "WoTB x Minecraft";
    public static final String VERSION = "0.2.0-beta";

    @Mod.Instance(MODID)
    public static WotbMinecraft instance;

    @SidedProxy(clientSide = "com.wotbminecraft.proxy.ClientProxy",
            serverSide = "com.wotbminecraft.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("WoTB x Minecraft BETA v" + VERSION + ": preInit");
        ModNetwork.init();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
