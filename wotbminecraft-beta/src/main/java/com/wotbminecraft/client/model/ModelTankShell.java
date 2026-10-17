package com.wotbminecraft.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ModelTankShell extends ModelBase {

    private final ModelRenderer shell;

    public ModelTankShell() {
        textureWidth = 16;
        textureHeight = 16;
        shell = new ModelRenderer(this, 0, 0);
        shell.setRotationPoint(0F, 0F, 0F);
        shell.addBox(-2F, -2F, -2F, 4, 4, 4);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        shell.render(scale);
    }
}
