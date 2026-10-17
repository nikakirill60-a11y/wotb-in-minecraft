package com.wotbminecraft.network;

import com.wotbminecraft.entity.EntityTank;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Универсальный пакет действий экипажа танка (выстрел, смена снаряда, пулемёт, форсаж, камуфляж).
 */
public class PacketTankAction implements IMessage {

    public static final byte ACTION_FIRE_CANNON = 0;
    public static final byte ACTION_CYCLE_AMMO = 1;
    public static final byte ACTION_FIRE_MG = 2;
    public static final byte ACTION_ENGINE_BOOST = 3;
    public static final byte ACTION_CYCLE_CAMO = 4;

    private int entityId;
    private byte action;

    public PacketTankAction() { }

    public PacketTankAction(int entityId, byte action) {
        this.entityId = entityId;
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.action = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeByte(this.action);
    }

    public static class Handler implements IMessageHandler<PacketTankAction, IMessage> {
        @Override
        public IMessage onMessage(PacketTankAction message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Entity entity = player.world.getEntityByID(message.entityId);
                if (entity instanceof EntityTank && player.getRidingEntity() == entity) {
                    EntityTank tank = (EntityTank) entity;
                    switch (message.action) {
                        case ACTION_FIRE_CANNON:
                            tank.fire(player);
                            break;
                        case ACTION_CYCLE_AMMO:
                            tank.cycleShellType(player);
                            break;
                        case ACTION_FIRE_MG:
                            tank.fireMachineGun(player);
                            break;
                        case ACTION_ENGINE_BOOST:
                            tank.activateEngineBoost(player);
                            break;
                        case ACTION_CYCLE_CAMO:
                            tank.cycleCamo(player);
                            break;
                        default:
                            break;
                    }
                }
            });
            return null;
        }
    }
}
