package com.wotbminecraft.item;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.entity.EntityTank;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemTankSpawner extends Item {

    public ItemTankSpawner() {
        setRegistryName(WotbMinecraft.MODID, "tank_spawner");
        setUnlocalizedName(WotbMinecraft.MODID + ".tank_spawner");
        setCreativeTab(CreativeTabs.TRANSPORTATION);
        setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            EntityTank tank = new EntityTank(worldIn);
            tank.setPosition(pos.getX() + 0.5D, pos.getY() + 1.01D, pos.getZ() + 0.5D);
            tank.rotationYaw = player.rotationYaw;
            tank.turretYaw = player.rotationYaw;
            worldIn.spawnEntity(tank);
            if (!player.capabilities.isCreativeMode) {
                player.getHeldItem(hand).shrink(1);
            }
        }
        return EnumActionResult.SUCCESS;
    }
}
