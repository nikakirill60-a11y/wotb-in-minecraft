package com.wotbminecraft.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * 3D-модель танкового снаряда (головная часть + корпус снаряда + ведущий поясок).
 */
public class ModelTankShell extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer warhead;

    public ModelTankShell() {
        textureWidth = 16;
        textureHeight = 16;

        body = new ModelRenderer(this, 0, 0);
        body.setRotationPoint(0F, 0F, 0F);
        body.addBox(-1.5F, -1.5F, -3F, 3, 3, 6);

        warhead = new ModelRenderer(this, 0, 9);
        warhead.setRotationPoint(0F, 0F, 0F);
        warhead.addBox(-1.0F, -1.0F, -6F, 2, 2, 3);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        body.render(scale);
        warhead.render(scale);
    }
}
