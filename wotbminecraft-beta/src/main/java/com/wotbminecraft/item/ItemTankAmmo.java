package com.wotbminecraft.item;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.entity.EntityTank;
import com.wotbminecraft.entity.ShellType;
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
 * Ящик танкового боекомплекта — пополняет снаряды ББ, КС и ОФ.
 */
public class ItemTankAmmo extends Item {

    public ItemTankAmmo() {
        setRegistryName(WotbMinecraft.MODID, "tank_ammo");
        setUnlocalizedName(WotbMinecraft.MODID + ".tank_ammo");
        setCreativeTab(CreativeTabs.COMBAT);
        setMaxStackSize(16);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (playerIn.getRidingEntity() instanceof EntityTank && playerIn.isSneaking()) {
            EntityTank tank = (EntityTank) playerIn.getRidingEntity();
            if (!worldIn.isRemote) {
                if (tank.refillAmmo()) {
                    if (!playerIn.capabilities.isCreativeMode) {
                        stack.shrink(1);
                    }
                    worldIn.playSound(null, tank.posX, tank.posY, tank.posZ,
                            SoundEvents.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 1.0F, 0.9F);
                    playerIn.sendStatusMessage(new TextComponentString(
                            TextFormatting.AQUA + "[БК пополнен] ББ: " + tank.getAmmoCount(ShellType.AP)
                                    + " | КС: " + tank.getAmmoCount(ShellType.HEAT)
                                    + " | ОФ: " + tank.getAmmoCount(ShellType.HE)), true);
                } else {
                    playerIn.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "Боекомплект танка уже полон!"), true);
                }
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.AQUA + "Пополняет боекомплект танка:");
        tooltip.add(TextFormatting.GRAY + " +12 ББ (Бронебойных)");
        tooltip.add(TextFormatting.GOLD + " +6 КС (Кумулятивных)");
        tooltip.add(TextFormatting.RED + " +6 ОФ (Осколочно-фугасных)");
        tooltip.add(TextFormatting.DARK_GRAY + "ПКМ по танку снаружи (или Shift+ПКМ внутри)");
    }
}
