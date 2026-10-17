package com.wotbminecraft.client.render;

import com.wotbminecraft.WotbMinecraft;
import com.wotbminecraft.client.model.ModelTankPlaceholder;
import com.wotbminecraft.entity.EntityTank;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class RenderTank extends Render<EntityTank> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(WotbMinecraft.MODID, "textures/entity/tank_placeholder.png");
    private final ModelTankPlaceholder model = new ModelTankPlaceholder();

    public RenderTank(RenderManager manager) {
        super(manager);
        this.shadowSize = 1.4F;
    }

    @Override
    public void doRender(EntityTank entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        // ориентация как у живых сущностей: модель смотрит в -Z
        GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        GlStateManager.translate(0.0D, -1.501D, 0.0D);
        bindEntityTexture(entity);
        model.render(entity, 0F, 0F, 0F, 0F, 0F, 0.0625F);
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityTank entity) {
        return TEXTURE;
    }
}
