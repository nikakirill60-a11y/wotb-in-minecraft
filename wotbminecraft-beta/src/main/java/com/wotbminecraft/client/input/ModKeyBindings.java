package com.wotbminecraft.client.input;

import com.wotbminecraft.entity.EntityTank;
import com.wotbminecraft.network.ModNetwork;
import com.wotbminecraft.network.PacketTankAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Клиентское управление танком:
 * - ПКМ (Правая кнопка мыши) или G — выстрел из главного орудия;
 * - R — смена типа снаряда (ББ -> КС -> ОФ);
 * - V (и колесо мыши в прицеле) — снайперский режим WoTB (Выкл -> 2x -> 4x);
 * - B (удержание) — стрельба из спаренного пулемёта;
 * - F — форсаж двигателя;
 * - C — смена камуфляжа танка.
 */
public class ModKeyBindings {

    private static final String CATEGORY = "key.categories.wotbminecraft";

    public static KeyBinding FIRE;
    public static KeyBinding CYCLE_AMMO;
    public static KeyBinding SNIPER_MODE;
    public static KeyBinding MACHINE_GUN;
    public static KeyBinding ENGINE_BOOST;
    public static KeyBinding CAMO_SWITCH;

    /** 0 = обычный вид, 1 = снайперский прицел 2x, 2 = снайперский прицел 4x */
    private static int sniperZoomLevel = 0;
    private static int savedThirdPersonView = 0;
    private static int rmbHoldTicker = 0;

    public static void init() {
        FIRE = new KeyBinding("key.wotbminecraft.fire", Keyboard.KEY_G, CATEGORY);
        CYCLE_AMMO = new KeyBinding("key.wotbminecraft.cycle_ammo", Keyboard.KEY_R, CATEGORY);
        SNIPER_MODE = new KeyBinding("key.wotbminecraft.sniper_mode", Keyboard.KEY_V, CATEGORY);
        MACHINE_GUN = new KeyBinding("key.wotbminecraft.machine_gun", Keyboard.KEY_B, CATEGORY);
        ENGINE_BOOST = new KeyBinding("key.wotbminecraft.engine_boost", Keyboard.KEY_F, CATEGORY);
        CAMO_SWITCH = new KeyBinding("key.wotbminecraft.camo_switch", Keyboard.KEY_C, CATEGORY);

        ClientRegistry.registerKeyBinding(FIRE);
        ClientRegistry.registerKeyBinding(CYCLE_AMMO);
        ClientRegistry.registerKeyBinding(SNIPER_MODE);
        ClientRegistry.registerKeyBinding(MACHINE_GUN);
        ClientRegistry.registerKeyBinding(ENGINE_BOOST);
        ClientRegistry.registerKeyBinding(CAMO_SWITCH);

        MinecraftForge.EVENT_BUS.register(new ModKeyBindings());
    }

    public static int getSniperZoomLevel() {
        return sniperZoomLevel;
    }

    public static void resetSniperMode() {
        if (sniperZoomLevel > 0) {
            sniperZoomLevel = 0;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.gameSettings != null) {
                mc.gameSettings.thirdPersonView = savedThirdPersonView;
            }
        }
    }

    @SubscribeEvent
    public void onMouseInput(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }
        Entity riding = mc.player.getRidingEntity();
        if (!(riding instanceof EntityTank)) {
            return;
        }
        EntityTank tank = (EntityTank) riding;

        // Выстрел по нажатию ПКМ (Правая кнопка мыши = button 1)
        if (event.getButton() == 1 && event.isButtonstate()) {
            ModNetwork.INSTANCE.sendToServer(
                    new PacketTankAction(tank.getEntityId(), PacketTankAction.ACTION_FIRE_CANNON));
            rmbHoldTicker = 10;
            // Блокируем стандартное действие ПКМ, чтобы не ставить случайно блоки при стрельбе из танка
            event.setCanceled(true);
            return;
        }

        // Управление кратностью снайперского прицела колесом мыши
        if (sniperZoomLevel > 0 && event.getDwheel() != 0) {
            if (event.getDwheel() > 0 && sniperZoomLevel < 2) {
                sniperZoomLevel = 2;
                mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.4F, 1.5F);
            } else if (event.getDwheel() < 0 && sniperZoomLevel > 1) {
                sniperZoomLevel = 1;
                mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.4F, 1.2F);
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || FIRE == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            resetSniperMode();
            return;
        }

        Entity riding = mc.player.getRidingEntity();
        if (!(riding instanceof EntityTank)) {
            resetSniperMode();
            return;
        }
        EntityTank tank = (EntityTank) riding;
        int id = tank.getEntityId();

        if (rmbHoldTicker > 0) {
            rmbHoldTicker--;
        }

        // Удержание ПКМ в бою позволяет выстрелить сразу по готовности орудия
        if (mc.currentScreen == null && Mouse.isButtonDown(1) && rmbHoldTicker == 0 && tank.getFireCooldown() == 0) {
            ModNetwork.INSTANCE.sendToServer(new PacketTankAction(id, PacketTankAction.ACTION_FIRE_CANNON));
            rmbHoldTicker = 10;
        }

        // Выстрел по клавише G
        while (FIRE.isPressed()) {
            ModNetwork.INSTANCE.sendToServer(new PacketTankAction(id, PacketTankAction.ACTION_FIRE_CANNON));
        }

        // Смена типа снаряда (R)
        while (CYCLE_AMMO.isPressed()) {
            ModNetwork.INSTANCE.sendToServer(new PacketTankAction(id, PacketTankAction.ACTION_CYCLE_AMMO));
        }

        // Переключение снайперского прицела (V): Выкл -> 2x -> 4x -> Выкл
        while (SNIPER_MODE.isPressed()) {
            if (sniperZoomLevel == 0) {
                savedThirdPersonView = mc.gameSettings.thirdPersonView;
                mc.gameSettings.thirdPersonView = 0;
                sniperZoomLevel = 1;
            } else if (sniperZoomLevel == 1) {
                sniperZoomLevel = 2;
            } else {
                sniperZoomLevel = 0;
                mc.gameSettings.thirdPersonView = savedThirdPersonView;
            }
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.5F, sniperZoomLevel > 0 ? 1.4F : 0.9F);
        }

        // Форсаж двигателя (F)
        while (ENGINE_BOOST.isPressed()) {
            ModNetwork.INSTANCE.sendToServer(new PacketTankAction(id, PacketTankAction.ACTION_ENGINE_BOOST));
        }

        // Смена камуфляжа (C)
        while (CAMO_SWITCH.isPressed()) {
            ModNetwork.INSTANCE.sendToServer(new PacketTankAction(id, PacketTankAction.ACTION_CYCLE_CAMO));
        }

        // Стрельба очередью из спаренного пулемёта при удержании клавиши B
        if (mc.currentScreen == null && MACHINE_GUN.isKeyDown()) {
            ModNetwork.INSTANCE.sendToServer(new PacketTankAction(id, PacketTankAction.ACTION_FIRE_MG));
        }
    }
}
