package com.wotbminecraft.network;

import com.wotbminecraft.entity.EntityTank;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketFireTank implements IMessage {

    private int entityId;

    public PacketFireTank() { }

    public PacketFireTank(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
    }

    public static class Handler implements IMessageHandler<PacketFireTank, IMessage> {
        @Override
        public IMessage onMessage(PacketFireTank message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Entity entity = player.world.getEntityByID(message.entityId);
                if (entity instanceof EntityTank && player.getRidingEntity() == entity) {
                    ((EntityTank) entity).fire(player);
                }
            });
            return null;
        }
    }
}
