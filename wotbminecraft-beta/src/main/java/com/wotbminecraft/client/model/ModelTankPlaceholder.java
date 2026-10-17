package com.wotbminecraft.client.model;

import com.wotbminecraft.entity.EntityTank;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * ВРЕМЕННАЯ модель из коробок.
 * Когда придёт настоящая модель, заменяется ТОЛЬКО этот класс.
 * Соглашения: фронт танка = -Z, башня крутится вокруг Y, ствол - вокруг X.
 */
public class ModelTankPlaceholder extends ModelBase {

    private final ModelRenderer trackLeft;
    private final ModelRenderer trackRight;
    private final ModelRenderer hull;
    private final ModelRenderer turret;
    private final ModelRenderer barrel;

    public ModelTankPlaceholder() {
        textureWidth = 128;
        textureHeight = 64;

        trackLeft = new ModelRenderer(this, 0, 0);
        trackLeft.setRotationPoint(-14F, 13F, 0F);
        trackLeft.addBox(-5F, 0F, -24F, 10, 11, 48);

        trackRight = new ModelRenderer(this, 0, 0);
        trackRight.setRotationPoint(14F, 13F, 0F);
        trackRight.addBox(-5F, 0F, -24F, 10, 11, 48);

        hull = new ModelRenderer(this, 0, 12);
        hull.setRotationPoint(0F, 6F, 0F);
        hull.addBox(-13F, 3F, -22F, 26, 8, 44);

        turret = new ModelRenderer(this, 64, 0);
        turret.setRotationPoint(0F, 8F, 2F);
        turret.addBox(-10F, -6F, -12F, 20, 6, 24);

        barrel = new ModelRenderer(this, 64, 32);
        barrel.setRotationPoint(0F, 5F, -8F);
        barrel.addBox(-1.5F, -1.5F, -30F, 3, 3, 32);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (entityIn instanceof EntityTank) {
            EntityTank tank = (EntityTank) entityIn;
            float relYaw = (float) Math.toRadians(tank.turretYaw - tank.rotationYaw);
            turret.rotateAngleY = relYaw;
            barrel.rotateAngleY = relYaw;
            barrel.rotateAngleX = (float) Math.toRadians(tank.turretPitch);
        }
        trackLeft.render(scale);
        trackRight.render(scale);
        hull.render(scale);
        turret.render(scale);
        barrel.render(scale);
    }
}
