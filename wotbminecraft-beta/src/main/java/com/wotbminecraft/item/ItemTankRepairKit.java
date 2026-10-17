package com.wotbminecraft.item;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.entity.EntityTank;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Танковый ремкомплект (гаечный ключ):
 * - ПКМ по танку: ремонтирует +60 HP и чинит сбитые гусеницы;
 * - Shift+ПКМ по пустому танку: сворачивает танк обратно в предмет с сохранением HP и БК;
 * - Shift+ПКМ внутри танка: экстренный ремонт гусениц и корпуса в бою.
 */
public class ItemTankRepairKit extends Item {

    public ItemTankRepairKit() {
        setRegistryName(WotbMinecraft.MODID, "tank_repair_kit");
        setUnlocalizedName(WotbMinecraft.MODID + ".tank_repair_kit");
        setCreativeTab(CreativeTabs.TOOLS);
        setMaxStackSize(1);
        setMaxDamage(16);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (playerIn.getRidingEntity() instanceof EntityTank && playerIn.isSneaking()) {
            EntityTank tank = (EntityTank) playerIn.getRidingEntity();
            if (!worldIn.isRemote) {
                if (tank.getHealth() < EntityTank.MAX_HEALTH || tank.getTrackDamagedTicks() > 0) {
                    tank.setHealth(tank.getHealth() + 60);
                    tank.setTrackDamagedTicks(0);
                    if (!playerIn.capabilities.isCreativeMode) {
                        stack.damageItem(1, playerIn);
                    }
                    worldIn.playSound(null, tank.posX, tank.posY, tank.posZ,
                            SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.9F, 1.2F);
                    playerIn.sendStatusMessage(new TextComponentString(
                            TextFormatting.GREEN + "[Ремкомплект] Гусеницы и корпус восстановлены ("
                                    + tank.getHealth() + "/" + EntityTank.MAX_HEALTH + " HP)!"), true);
                } else {
                    playerIn.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "Танк не нуждается в ремонте!"), true);
                }
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GREEN + "Ремонт танка (+60 HP) и починка гусениц");
        tooltip.add(TextFormatting.YELLOW + "ПКМ по танку — ремонт");
        tooltip.add(TextFormatting.GOLD + "Shift+ПКМ по пустому танку — свернуть в предмет");
        tooltip.add(TextFormatting.AQUA + "Shift+ПКМ внутри танка — экстренный ремонт в бою");
    }
}
