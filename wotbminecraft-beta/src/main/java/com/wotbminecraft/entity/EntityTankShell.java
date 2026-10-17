package com.wotbminecraft.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Танковый снаряд (ББ / КС / ОФ).
 * ОФ (HE) разрушает блоки при взрыве, а ББ (AP) и КС (HEAT) наносят высокий бронебойный урон
 * без разрушения ландшафта и могут рикошетить от брони под острым углом.
 */
public class EntityTankShell extends Entity {

    private static final DataParameter<Integer> DATA_SHELL_TYPE =
            EntityDataManager.createKey(EntityTankShell.class, DataSerializers.VARINT);

    private Entity shooterTank;
    private boolean hasRicocheted = false;
    private int ricochetTicks = 0;

    public EntityTankShell(World worldIn) {
        super(worldIn);
        setSize(0.35F, 0.35F);
        this.ignoreFrustumCheck = true;
    }

    public EntityTankShell(World worldIn, Entity shooterTank, double x, double y, double z) {
        this(worldIn, shooterTank, x, y, z, ShellType.AP);
    }

    public EntityTankShell(World worldIn, Entity shooterTank, double x, double y, double z, ShellType shellType) {
        this(worldIn);
        this.shooterTank = shooterTank;
        setPosition(x, y, z);
        setShellType(shellType);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(DATA_SHELL_TYPE, ShellType.AP.getId());
    }

    public ShellType getShellType() {
        return ShellType.byId(this.dataManager.get(DATA_SHELL_TYPE));
    }

    public void setShellType(ShellType type) {
        this.dataManager.set(DATA_SHELL_TYPE, type.getId());
    }

    public void shootFrom(float yaw, float pitch, float speed) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        motionX = -MathHelper.sin((float) yawRad) * MathHelper.cos((float) pitchRad) * speed;
        motionY = -MathHelper.sin((float) pitchRad) * speed;
        motionZ = MathHelper.cos((float) yawRad) * MathHelper.cos((float) pitchRad) * speed;
        updateRotationFromMotion();
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
    }

    private void updateRotationFromMotion() {
        double horiz = Math.sqrt(motionX * motionX + motionZ * motionZ);
        this.rotationYaw = (float) (MathHelper.atan2(-motionX, motionZ) * (180.0D / Math.PI));
        this.rotationPitch = (float) (MathHelper.atan2(-motionY, horiz) * (180.0D / Math.PI));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (ticksExisted > 120 || (hasRicocheted && ++ricochetTicks > 25)) {
            setDead();
            return;
        }

        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
        updateRotationFromMotion();

        // Трассер снаряда на клиенте
        if (world.isRemote) {
            ShellType type = getShellType();
            if (type == ShellType.HEAT) {
                world.spawnParticle(EnumParticleTypes.FLAME, posX, posY, posZ,
                        -motionX * 0.08D, -motionY * 0.08D, -motionZ * 0.08D);
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
            } else if (type == ShellType.HE) {
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX, posY, posZ,
                        -motionX * 0.05D, -motionY * 0.05D, -motionZ * 0.05D);
                world.spawnParticle(EnumParticleTypes.FLAME, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
            } else {
                world.spawnParticle(EnumParticleTypes.CRIT, posX, posY, posZ,
                        -motionX * 0.1D, -motionY * 0.1D, -motionZ * 0.1D);
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
            }
        }

        Vec3d start = new Vec3d(posX, posY, posZ);
        Vec3d end = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);

        // 1. Проверка попадания в блок
        RayTraceResult blockHit = world.rayTraceBlocks(start, end);
        Vec3d effectiveEnd = (blockHit != null && blockHit.hitVec != null) ? blockHit.hitVec : end;

        // 2. Проверка попадания в сущность на траектории
        Entity hitEntity = null;
        double closestDist = start.distanceTo(effectiveEnd);
        List<Entity> targets = world.getEntitiesWithinAABBExcludingEntity(this,
                getEntityBoundingBox().expand(motionX, motionY, motionZ).grow(0.65D));

        for (Entity candidate : targets) {
            if (candidate == shooterTank || candidate instanceof EntityTankShell) {
                continue;
            }
            if (shooterTank != null && candidate.isRidingSameEntity(shooterTank)) {
                continue;
            }
            AxisAlignedBB box = candidate.getEntityBoundingBox().grow(0.3D);
            RayTraceResult intercept = box.calculateIntercept(start, effectiveEnd);
            if (intercept != null) {
                double dist = start.distanceTo(intercept.hitVec);
                if (dist < closestDist) {
                    closestDist = dist;
                    hitEntity = candidate;
                }
            } else if (box.intersects(getEntityBoundingBox())) {
                hitEntity = candidate;
                break;
            }
        }

        if (hitEntity != null) {
            onHitEntity(hitEntity);
            return;
        }

        if (blockHit != null) {
            if (blockHit.hitVec != null) {
                setPosition(blockHit.hitVec.x, blockHit.hitVec.y, blockHit.hitVec.z);
            }
            explode(false);
            return;
        }

        // Баллистика (у более быстрых ББ настильная траектория, у ОФ более навесная)
        double gravity = getShellType() == ShellType.AP ? 0.022D
                : (getShellType() == ShellType.HEAT ? 0.028D : 0.036D);
        motionY -= gravity;
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
    }

    private void onHitEntity(Entity target) {
        if (target instanceof EntityTank) {
            EntityTank hitTank = (EntityTank) target;
            if (!world.isRemote) {
                boolean ricocheted = hitTank.handleShellImpact(this, shooterTank);
                if (ricocheted && !hasRicocheted) {
                    // Отражаем вектор полёта при рикошете!
                    hasRicocheted = true;
                    shooterTank = hitTank;
                    motionX = -motionX * 0.45D + (world.rand.nextDouble() - 0.5D) * 0.5D;
                    motionY = Math.abs(motionY) * 0.4D + 0.25D;
                    motionZ = -motionZ * 0.45D + (world.rand.nextDouble() - 0.5D) * 0.5D;
                    updateRotationFromMotion();
                    return;
                }
            } else {
                for (int i = 0; i < 10; i++) {
                    world.spawnParticle(EnumParticleTypes.CRIT_MAGIC, posX, posY, posZ,
                            (world.rand.nextDouble() - 0.5D) * 0.4D,
                            0.15D,
                            (world.rand.nextDouble() - 0.5D) * 0.4D);
                }
            }
            explode(true);
            return;
        }

        // Попадание по обычной сущности (мобу / игроку вне танка)
        if (!world.isRemote && target instanceof EntityLivingBase) {
            ShellType type = getShellType();
            float directDamage = type == ShellType.AP ? 26.0F : (type == ShellType.HEAT ? 24.0F : 20.0F);
            target.attackEntityFrom(DamageSource.causeThrownDamage(this, shooterTank), directDamage);
            if (type == ShellType.HEAT) {
                target.setFire(5);
            }
        }
        explode(false);
    }

    private void explode(boolean hitTank) {
        if (!world.isRemote) {
            ShellType type = getShellType();
            // Разрушение блоков зависит от типа снаряда: ОФ (HE) ломает блоки, ББ (AP) и КС (HEAT) — нет!
            boolean breakBlocks = type.isBreaksBlocks() && !hitTank;
            float explosionStrength = type == ShellType.HE ? 2.6F : (type == ShellType.HEAT ? 1.8F : 1.5F);
            world.createExplosion(shooterTank, posX, posY, posZ, explosionStrength, breakBlocks);
        }
        setDead();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("ShellType", getShellType().getId());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("ShellType")) {
            setShellType(ShellType.byId(compound.getInteger("ShellType")));
        }
    }
}
