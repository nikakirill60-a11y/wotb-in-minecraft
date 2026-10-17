package com.wotbminecraft.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Beta-танк (заглушка). Позже сюда встанет пользовательская модель:
 * корпус / башня (turretYaw) / ствол (turretPitch) уже разделены.
 */
public class EntityTank extends Entity {

    /** Абсолютный курс башни (куда смотрит игрок). Используется моделью и стрельбой. */
    public float turretYaw = 0F;
    /** Наклон ствола. */
    public float turretPitch = 0F;

    private float currentSpeed = 0F;
    private int fireCooldown = 0;
    private int health = 100;

    public static final int MAX_HEALTH = 100;
    public static final int FIRE_COOLDOWN_TICKS = 40; // 2 секунды

    public EntityTank(World worldIn) {
        super(worldIn);
        setSize(2.6F, 1.5F);
        this.stepHeight = 1.0F;
        this.ignoreFrustumCheck = true;
    }

    @Override
    protected void entityInit() { }

    // ---------- посадка ----------
    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (hand == EnumHand.MAIN_HAND && !isBeingRidden()) {
            if (!world.isRemote) {
                player.startRiding(this);
            }
            return true;
        }
        return super.processInitialInteract(player, hand);
    }

    @Nullable
    @Override
    public Entity getControllingPassenger() {
        return getPassengers().isEmpty() ? null : getPassengers().get(0);
    }

    public boolean canBeSteered() {
        return getControllingPassenger() != null;
    }

    @Override
    public double getMountedYOffset() {
        return 1.45D;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isDead;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    // ---------- урон ----------
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isEntityInvulnerable(source) || world.isRemote) {
            return false;
        }
        health -= (int) amount;
        if (health <= 0) {
            world.createExplosion(null, posX, posY + 0.5D, posZ, 2.0F, true);
            setDead();
        }
        return true;
    }

    public int getHealth() {
        return health;
    }

    // ---------- стрельба (вызывается с сервера из пакета по клавише G) ----------
    public void fire(EntityPlayer shooter) {
        if (world.isRemote || fireCooldown > 0 || isDead) {
            return;
        }
        fireCooldown = FIRE_COOLDOWN_TICKS;
        turretYaw = shooter.rotationYawHead;
        turretPitch = shooter.rotationPitch;

        double yawRad = Math.toRadians(turretYaw);
        double pitchRad = Math.toRadians(turretPitch);
        double dirX = -MathHelper.sin((float) yawRad) * MathHelper.cos((float) pitchRad);
        double dirY = -MathHelper.sin((float) pitchRad);
        double dirZ = MathHelper.cos((float) yawRad) * MathHelper.cos((float) pitchRad);

        // дуло: чуть впереди и выше корпуса
        double mx = posX + dirX * 2.8D;
        double my = posY + 1.55D + dirY * 2.8D;
        double mz = posZ + dirZ * 2.8D;

        EntityTankShell shell = new EntityTankShell(world, this, mx, my, mz);
        shell.shootFrom(turretYaw, turretPitch, 3.2F);
        world.spawnEntity(shell);

        world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 0.7F, 1.6F);
        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, mx, my, mz, 0, 0, 0);

        // отдача
        motionX -= dirX * 0.06D;
        motionZ -= dirZ * 0.06D;
    }

    // ---------- движение ----------
    @Override
    public void onUpdate() {
        super.onUpdate();

        if (fireCooldown > 0) {
            fireCooldown--;
        }

        Entity passenger = getControllingPassenger();
        if (passenger instanceof EntityLivingBase) {
            EntityLivingBase rider = (EntityLivingBase) passenger;

            turretYaw = rider.rotationYawHead;
            turretPitch = rider.rotationPitch;

            // Поворот на месте и в движении (как у танка, а не у машины)
            if (rider.moveStrafing != 0F) {
                float turn = rider.moveStrafing * 2.0F;
                if (rider.moveForward < 0F) {
                    turn = -turn;
                }
                rotationYaw -= turn;
            }

            // Разгон / торможение
            float target = 0F;
            if (rider.moveForward > 0F) {
                target = 0.45F;
            } else if (rider.moveForward < 0F) {
                target = -0.22F;
            }
            currentSpeed += (target - currentSpeed) * 0.12F;
            if (Math.abs(currentSpeed) < 0.001F && target == 0F) {
                currentSpeed = 0F;
            }
        } else {
            currentSpeed *= 0.8F;
        }

        double yawRad = Math.toRadians(rotationYaw);
        motionX = -MathHelper.sin((float) yawRad) * currentSpeed;
        motionZ = MathHelper.cos((float) yawRad) * currentSpeed;

        // гравитация
        motionY -= 0.08D;
        if (motionY < -0.5D) {
            motionY = -0.5D;
        }

        move(MoverType.SELF, motionX, motionY, motionZ);

        if (onGround) {
            motionY = 0D;
        }

        prevRotationYaw = rotationYaw;
    }

    // ---------- NBT ----------
    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("TankHealth", health);
        compound.setFloat("TurretYaw", turretYaw);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("TankHealth")) {
            health = compound.getInteger("TankHealth");
        }
        turretYaw = compound.getFloat("TurretYaw");
    }
}
