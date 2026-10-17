package com.wotbminecraft.network;

import com.wotbminecraft.WotbMinecraft;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class ModNetwork {

    public static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel(WotbMinecraft.MODID);

    public static void init() {
        INSTANCE.registerMessage(PacketFireTank.Handler.class, PacketFireTank.class, 0, Side.SERVER);
        INSTANCE.registerMessage(PacketTankAction.Handler.class, PacketTankAction.class, 1, Side.SERVER);
    }
}
