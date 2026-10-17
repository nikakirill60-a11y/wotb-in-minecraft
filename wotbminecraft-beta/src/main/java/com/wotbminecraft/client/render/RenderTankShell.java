package com.wotbminecraft.client.render;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.client.model.ModelTankShell;
import com.wotbminecraft.entity.EntityTankShell;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class RenderTankShell extends Render<EntityTankShell> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(WotbMinecraft.MODID, "textures/entity/tank_shell.png");
    private final ModelTankShell model = new ModelTankShell();

    public RenderTankShell(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(EntityTankShell entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        bindEntityTexture(entity);
        model.render(entity, 0F, 0F, 0F, 0F, 0F, 0.0625F);
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityTankShell entity) {
        return TEXTURE;
    }
}
