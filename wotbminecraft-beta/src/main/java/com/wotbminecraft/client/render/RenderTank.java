package com.wotbminecraft.client.render;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.client.input.ModKeyBindings;
import com.wotbminecraft.client.model.ModelTankPlaceholder;
import com.wotbminecraft.entity.EntityTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

public class RenderTank extends Render<EntityTank> {

    private static final ResourceLocation TEXTURE_FOREST =
            new ResourceLocation(WotbMinecraft.MODID, "textures/entity/tank_placeholder.png");
    private static final ResourceLocation TEXTURE_DESERT =
            new ResourceLocation(WotbMinecraft.MODID, "textures/entity/tank_desert.png");
    private static final ResourceLocation TEXTURE_WINTER =
            new ResourceLocation(WotbMinecraft.MODID, "textures/entity/tank_winter.png");

    private final ModelTankPlaceholder model = new ModelTankPlaceholder();

    public RenderTank(RenderManager manager) {
        super(manager);
        this.shadowSize = 1.5F;
    }

    @Override
    public void doRender(EntityTank entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && mc.player.getRidingEntity() == entity
                && ModKeyBindings.getSniperZoomLevel() > 0) {
            // В снайперском прицеле скрываем модель своего танка, чтобы не перекрывать оптику
            return;
        }

        // Плавная интерполяция углов корпуса, башни, ствола, катков и отката
        float interpHullYaw = entity.prevRotationYaw
                + MathHelper.wrapDegrees(entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        float interpTurretYaw = entity.prevTurretYaw
                + MathHelper.wrapDegrees(entity.turretYaw - entity.prevTurretYaw) * partialTicks;
        float interpTurretPitch = entity.prevTurretPitch
                + (entity.turretPitch - entity.prevTurretPitch) * partialTicks;
        float interpWheelRot = entity.prevWheelRotation
                + (entity.wheelRotation - entity.prevWheelRotation) * partialTicks;

        float relTurretYaw = MathHelper.wrapDegrees(interpTurretYaw - interpHullYaw);
        float recoilProgress = (float) entity.getRecoilTicks() / (float) EntityTank.MAX_RECOIL_TICKS;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        // Ориентация модели: фронт смотрит в -Z
        GlStateManager.rotate(180.0F - interpHullYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        GlStateManager.translate(0.0D, -1.501D, 0.0D);

        bindEntityTexture(entity);
        model.render(entity, interpWheelRot, 0F, recoilProgress, relTurretYaw, interpTurretPitch, 0.0625F);
        GlStateManager.popMatrix();

        // Отображение полоски HP над танком для игроков снаружи
        if (mc.player != null && mc.player.getRidingEntity() != entity
                && entity.getDistanceSq(mc.player) < 576.0D) {
            String label = String.format("Танк [%d/%d HP]", entity.getHealth(), EntityTank.MAX_HEALTH);
            renderLivingLabel(entity, label, x, y + 0.45D, z, 24);
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityTank entity) {
        switch (entity.getCamoType()) {
            case 1:
                return TEXTURE_DESERT;
            case 2:
                return TEXTURE_WINTER;
            case 0:
            default:
                return TEXTURE_FOREST;
        }
    }
}
