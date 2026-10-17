package com.wotbminecraft.client.input;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.entity.EntityTank;
import com.wotbminecraft.network.ModNetwork;
import com.wotbminecraft.network.PacketFireTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class ModKeyBindings {

    public static KeyBinding FIRE;

    public static void init() {
        FIRE = new KeyBinding("key.wotbminecraft.fire", Keyboard.KEY_G,
                "key.categories.wotbminecraft");
        ClientRegistry.registerKeyBinding(FIRE);
        MinecraftForge.EVENT_BUS.register(new ModKeyBindings());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || FIRE == null) {
            return;
        }
        while (FIRE.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player != null) {
                Entity riding = mc.player.getRidingEntity();
                if (riding instanceof EntityTank) {
                    ModNetwork.INSTANCE.sendToServer(new PacketFireTank(riding.getEntityId()));
                }
            }
        }
    }
}
