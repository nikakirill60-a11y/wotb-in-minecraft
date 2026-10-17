package com.wotbminecraft.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class EntityTankShell extends Entity {

    private Entity shooterTank;

    public EntityTankShell(World worldIn) {
        super(worldIn);
        setSize(0.35F, 0.35F);
        this.ignoreFrustumCheck = true;
    }

    public EntityTankShell(World worldIn, Entity shooterTank, double x, double y, double z) {
        this(worldIn);
        this.shooterTank = shooterTank;
        setPosition(x, y, z);
    }

    public void shootFrom(float yaw, float pitch, float speed) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        motionX = -MathHelper.sin((float) yawRad) * MathHelper.cos((float) pitchRad) * speed;
        motionY = -MathHelper.sin((float) pitchRad) * speed;
        motionZ = MathHelper.cos((float) yawRad) * MathHelper.cos((float) pitchRad) * speed;
    }

    @Override
    protected void entityInit() { }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (ticksExisted > 120) {
            setDead();
            return;
        }

        Vec3d start = new Vec3d(posX, posY, posZ);
        Vec3d end = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);

        // попадание в блок
        RayTraceResult blockHit = world.rayTraceBlocks(start, end);
        if (blockHit != null) {
            explode();
            return;
        }

        // попадание в сущность
        List<Entity> targets = world.getEntitiesWithinAABBExcludingEntity(this,
                getEntityBoundingBox().expand(motionX, motionY, motionZ).grow(0.4D));
        for (Entity target : targets) {
            if (target != shooterTank && !(target instanceof EntityTankShell)
                    && target.getEntityBoundingBox().intersects(getEntityBoundingBox())) {
                explode();
                return;
            }
        }

        motionY -= 0.03D; // лёгкая баллистика
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
    }

    private void explode() {
        if (!world.isRemote) {
            world.createExplosion(shooterTank, posX, posY, posZ, 2.8F, true);
        }
        setDead();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) { }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) { }
}
