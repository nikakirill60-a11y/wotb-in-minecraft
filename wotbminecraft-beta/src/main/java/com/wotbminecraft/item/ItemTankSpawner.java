package com.wotbminecraft.item;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.entity.EntityTank;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

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
            ItemStack stack = player.getHeldItem(hand);
            BlockPos spawnPos = pos.offset(facing);
            EntityTank tank = new EntityTank(worldIn);

            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("TankData")) {
                tank.readEntityFromNBT(stack.getTagCompound().getCompoundTag("TankData"));
            }

            tank.setPosition(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.05D, spawnPos.getZ() + 0.5D);
            tank.rotationYaw = player.rotationYaw;
            tank.prevRotationYaw = player.rotationYaw;
            tank.turretYaw = player.rotationYaw;
            tank.prevTurretYaw = player.rotationYaw;

            worldIn.spawnEntity(tank);
            worldIn.playSound(null, spawnPos, SoundEvents.BLOCK_ANVIL_PLACE,
                    SoundCategory.BLOCKS, 0.8F, 0.85F);

            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "Средний боевой танк (WoTB)");
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("TankData")) {
            NBTTagCompound data = stack.getTagCompound().getCompoundTag("TankData");
            int hp = data.hasKey("TankHealth") ? data.getInteger("TankHealth") : EntityTank.MAX_HEALTH;
            int ap = data.hasKey("AmmoAP") ? data.getInteger("AmmoAP") : EntityTank.MAX_AMMO_AP;
            int heat = data.hasKey("AmmoHEAT") ? data.getInteger("AmmoHEAT") : EntityTank.MAX_AMMO_HEAT;
            int he = data.hasKey("AmmoHE") ? data.getInteger("AmmoHE") : EntityTank.MAX_AMMO_HE;
            tooltip.add(TextFormatting.GREEN + "Прочность: " + hp + "/" + EntityTank.MAX_HEALTH + " HP");
            tooltip.add(TextFormatting.AQUA + "Боекомплект: ББ " + ap + " | КС " + heat + " | ОФ " + he);
        } else {
            tooltip.add(TextFormatting.GREEN + "Прочность: " + EntityTank.MAX_HEALTH + " HP | Броня: 155/85/45 мм");
            tooltip.add(TextFormatting.AQUA + "Снаряды: ББ (165мм), КС (235мм), ОФ (фугас)");
        }
        tooltip.add(TextFormatting.GRAY + "ПКМ в танке — выстрел | R — смена снаряда | V — прицел");
    }
}
