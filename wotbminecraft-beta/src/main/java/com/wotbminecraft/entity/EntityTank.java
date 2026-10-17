package com.wotbminecraft.entity;

import com.wotbminecraft.init.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Боевой танк в стиле World of Tanks Blitz.
 * Поддерживает:
 * - 3 типа снарядов (ББ / КС / ОФ) с боекомплектом;
 * - Бронирование лба, бортов и кормы с расчётом приведённой брони и рикошетов;
 * - Плавный поворот башни, УВН (-10°..+22°), откат орудия и вращение катков;
 * - Спаренный пулемёт, Форсаж двигателя, сбитие и ремонт гусениц, смену камуфляжа;
 * - Полную синхронизацию состояния между сервером и клиентом через DataManager.
 */
public class EntityTank extends Entity {

    // ---------- Параметры синхронизации (DataManager) ----------
    private static final DataParameter<Integer> DATA_HEALTH =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DATA_FIRE_COOLDOWN =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DATA_SHELL_TYPE =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DATA_AMMO_AP =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DATA_AMMO_HEAT =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DATA_AMMO_HE =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.VARINT);
    private static final DataParameter<Float> DATA_TURRET_YAW =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> DATA_TURRET_PITCH =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> DATA_TRACK_DAMAGED_TICKS =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DATA_ENGINE_BOOST_TICKS =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DATA_CAMO_TYPE =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DATA_RECOIL_TICKS =
            EntityDataManager.createKey(EntityTank.class, DataSerializers.VARINT);

    // ---------- Константы ТТХ танка ----------
    public static final int MAX_HEALTH = 200;
    public static final int FIRE_COOLDOWN_TICKS = 45; // 2.25 секунды

    public static final int MAX_AMMO_AP = 24;
    public static final int MAX_AMMO_HEAT = 12;
    public static final int MAX_AMMO_HE = 12;

    /** Номинальная толщина брони (в миллиметрах) */
    public static final int FRONT_ARMOR_MM = 155;
    public static final int SIDE_ARMOR_MM = 85;
    public static final int REAR_ARMOR_MM = 45;

    /** Скорость поворота башни (градусов за тик) */
    public static final float TURRET_TRAVERSE_SPEED = 6.5F;
    /** Ограничения УВН: в системе координат Minecraft отрицательный pitch — ствол вверх, положительный — вниз */
    public static final float MIN_PITCH_UP = -22.0F;
    public static final float MAX_PITCH_DOWN = 10.0F;

    public static final int MAX_RECOIL_TICKS = 10;

    // ---------- Клиентские / интерполяционные поля для рендера и модели ----------
    /** Абсолютный курс башни (используется моделью и стрельбой). */
    public float turretYaw = 0F;
    public float prevTurretYaw = 0F;
    /** Наклон ствола. */
    public float turretPitch = 0F;
    public float prevTurretPitch = 0F;

    /** Вращение опорных катков при движении */
    public float wheelRotation = 0F;
    public float prevWheelRotation = 0F;

    private float currentSpeed = 0F;
    private int mgCooldown = 0;
    private int engineBoostCooldown = 0;

    public EntityTank(World worldIn) {
        super(worldIn);
        setSize(2.6F, 1.65F);
        this.stepHeight = 1.25F;
        this.ignoreFrustumCheck = true;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(DATA_HEALTH, MAX_HEALTH);
        this.dataManager.register(DATA_FIRE_COOLDOWN, 0);
        this.dataManager.register(DATA_SHELL_TYPE, ShellType.AP.getId());
        this.dataManager.register(DATA_AMMO_AP, MAX_AMMO_AP);
        this.dataManager.register(DATA_AMMO_HEAT, MAX_AMMO_HEAT);
        this.dataManager.register(DATA_AMMO_HE, MAX_AMMO_HE);
        this.dataManager.register(DATA_TURRET_YAW, 0.0F);
        this.dataManager.register(DATA_TURRET_PITCH, 0.0F);
        this.dataManager.register(DATA_TRACK_DAMAGED_TICKS, 0);
        this.dataManager.register(DATA_ENGINE_BOOST_TICKS, 0);
        this.dataManager.register(DATA_CAMO_TYPE, 0);
        this.dataManager.register(DATA_RECOIL_TICKS, 0);
    }

    // ---------- Геттеры / сеттеры синхронизируемых параметров ----------

    public int getHealth() {
        return this.dataManager.get(DATA_HEALTH);
    }

    public void setHealth(int hp) {
        this.dataManager.set(DATA_HEALTH, MathHelper.clamp(hp, 0, MAX_HEALTH));
    }

    public int getFireCooldown() {
        return this.dataManager.get(DATA_FIRE_COOLDOWN);
    }

    public void setFireCooldown(int ticks) {
        this.dataManager.set(DATA_FIRE_COOLDOWN, Math.max(0, ticks));
    }

    public ShellType getSelectedShellType() {
        return ShellType.byId(this.dataManager.get(DATA_SHELL_TYPE));
    }

    public void setSelectedShellType(ShellType type) {
        this.dataManager.set(DATA_SHELL_TYPE, type.getId());
    }

    public int getAmmoCount(ShellType type) {
        switch (type) {
            case HEAT:
                return this.dataManager.get(DATA_AMMO_HEAT);
            case HE:
                return this.dataManager.get(DATA_AMMO_HE);
            case AP:
            default:
                return this.dataManager.get(DATA_AMMO_AP);
        }
    }

    public void setAmmoCount(ShellType type, int count) {
        switch (type) {
            case HEAT:
                this.dataManager.set(DATA_AMMO_HEAT, MathHelper.clamp(count, 0, MAX_AMMO_HEAT));
                break;
            case HE:
                this.dataManager.set(DATA_AMMO_HE, MathHelper.clamp(count, 0, MAX_AMMO_HE));
                break;
            case AP:
            default:
                this.dataManager.set(DATA_AMMO_AP, MathHelper.clamp(count, 0, MAX_AMMO_AP));
                break;
        }
    }

    public int getTrackDamagedTicks() {
        return this.dataManager.get(DATA_TRACK_DAMAGED_TICKS);
    }

    public void setTrackDamagedTicks(int ticks) {
        this.dataManager.set(DATA_TRACK_DAMAGED_TICKS, Math.max(0, ticks));
    }

    public int getEngineBoostTicks() {
        return this.dataManager.get(DATA_ENGINE_BOOST_TICKS);
    }

    public void setEngineBoostTicks(int ticks) {
        this.dataManager.set(DATA_ENGINE_BOOST_TICKS, Math.max(0, ticks));
    }

    public int getCamoType() {
        return this.dataManager.get(DATA_CAMO_TYPE);
    }

    public void setCamoType(int camo) {
        this.dataManager.set(DATA_CAMO_TYPE, ((camo % 3) + 3) % 3);
    }

    public int getRecoilTicks() {
        return this.dataManager.get(DATA_RECOIL_TICKS);
    }

    public void setRecoilTicks(int ticks) {
        this.dataManager.set(DATA_RECOIL_TICKS, MathHelper.clamp(ticks, 0, MAX_RECOIL_TICKS));
    }

    public float getCurrentSpeedBlocksPerTick() {
        return currentSpeed;
    }

    public float getSpeedKmh() {
        double dx = posX - prevPosX;
        double dz = posZ - prevPosZ;
        double blocksPerSec = Math.sqrt(dx * dx + dz * dz) * 20.0D;
        return (float) (blocksPerSec * 3.6D);
    }

    // ---------- Посадка и обслуживание снаружи ----------
    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) {
            return false;
        }
        ItemStack held = player.getHeldItem(hand);

        // Обслуживание ремкомплектом / гаечным ключом
        if (!held.isEmpty() && held.getItem() == ModItems.TANK_REPAIR_KIT) {
            if (!world.isRemote) {
                if (player.isSneaking() && !isBeingRidden()) {
                    // Сворачивание танка в предмет с сохранением состояния
                    ItemStack spawner = new ItemStack(ModItems.TANK_SPAWNER);
                    NBTTagCompound tag = new NBTTagCompound();
                    writeEntityToNBT(tag);
                    spawner.setTagInfo("TankData", tag);
                    entityDropItem(spawner, 0.5F);
                    world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_ANVIL_USE,
                            SoundCategory.PLAYERS, 0.8F, 1.2F);
                    setDead();
                    return true;
                }
                if (getHealth() < MAX_HEALTH || getTrackDamagedTicks() > 0) {
                    setHealth(getHealth() + 60);
                    setTrackDamagedTicks(0);
                    if (!player.capabilities.isCreativeMode) {
                        held.damageItem(1, player);
                    }
                    world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_ANVIL_USE,
                            SoundCategory.PLAYERS, 0.9F, 1.1F);
                    sendStatus(player, TextFormatting.GREEN + "[Ремкомплект] Танк отремонтирован: "
                            + getHealth() + "/" + MAX_HEALTH + " HP, гусеницы в норме!");
                } else {
                    sendStatus(player, TextFormatting.YELLOW + "Танк полностью исправен ("
                            + getHealth() + "/" + MAX_HEALTH + " HP). Shift+ПКМ — свернуть танк.");
                }
            }
            return true;
        }

        // Пополнение боекомплекта ящиком снарядов
        if (!held.isEmpty() && held.getItem() == ModItems.TANK_AMMO) {
            if (!world.isRemote) {
                if (refillAmmo()) {
                    if (!player.capabilities.isCreativeMode) {
                        held.shrink(1);
                    }
                    world.playSound(null, posX, posY, posZ, SoundEvents.ITEM_ARMOR_EQUIP_IRON,
                            SoundCategory.PLAYERS, 1.0F, 0.9F);
                    sendStatus(player, TextFormatting.AQUA + "[Боекомплект пополнен] ББ: "
                            + getAmmoCount(ShellType.AP) + " | КС: " + getAmmoCount(ShellType.HEAT)
                            + " | ОФ: " + getAmmoCount(ShellType.HE));
                } else {
                    sendStatus(player, TextFormatting.YELLOW + "Боекомплект танка уже полон!");
                }
            }
            return true;
        }

        // Быстрая починка железным слитком снаружи
        if (!held.isEmpty() && held.getItem() == Items.IRON_INGOT && getHealth() < MAX_HEALTH) {
            if (!world.isRemote) {
                setHealth(getHealth() + 20);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
                world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_ANVIL_PLACE,
                        SoundCategory.PLAYERS, 0.6F, 1.3F);
                sendStatus(player, TextFormatting.GREEN + "Полевой ремонт (+20 HP): "
                        + getHealth() + "/" + MAX_HEALTH + " HP");
            }
            return true;
        }

        // Смена камуфляжа красителем
        if (!held.isEmpty() && held.getItem() == Items.DYE && !isBeingRidden()) {
            if (!world.isRemote) {
                cycleCamo(player);
            }
            return true;
        }

        // Посадка в танк
        if (!isBeingRidden()) {
            if (!world.isRemote) {
                player.startRiding(this);
                sendStatus(player, TextFormatting.GOLD + "Танк готов к бою! "
                        + TextFormatting.WHITE + "ПКМ/G — Выстрел | R — Снаряд | V — Прицел | B — Пулемёт | F — Форсаж");
            }
            return true;
        }
        return super.processInitialInteract(player, hand);
    }

    public boolean refillAmmo() {
        int ap = getAmmoCount(ShellType.AP);
        int heat = getAmmoCount(ShellType.HEAT);
        int he = getAmmoCount(ShellType.HE);
        if (ap >= MAX_AMMO_AP && heat >= MAX_AMMO_HEAT && he >= MAX_AMMO_HE) {
            return false;
        }
        setAmmoCount(ShellType.AP, ap + 12);
        setAmmoCount(ShellType.HEAT, heat + 6);
        setAmmoCount(ShellType.HE, he + 6);
        return true;
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
        return 1.38D;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isDead;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    // ---------- Система бронирования и урона (WoTB) ----------

    /**
     * Вычисляет приведённую броню танка (в мм) для выстрела с заданным вектором направления луча/снаряда.
     * Используется как на сервере при попадании снаряда, так и на клиенте для индикатора бронепробития в прицеле!
     */
    public ArmorHitResult evaluateArmorAtVector(Vec3d incomingDir) {
        double len = Math.sqrt(incomingDir.x * incomingDir.x + incomingDir.z * incomingDir.z);
        if (len < 1.0E-4D) {
            return new ArmorHitResult("Крыша", REAR_ARMOR_MM, 0.0F, REAR_ARMOR_MM);
        }
        double dirX = incomingDir.x / len;
        double dirZ = incomingDir.z / len;

        // Вектор взгляда корпуса танка (куда смотрит лоб)
        double hullRad = Math.toRadians(this.rotationYaw);
        double frontX = -MathHelper.sin((float) hullRad);
        double frontZ = MathHelper.cos((float) hullRad);

        // Встречный вектор (от танка к стрелку): -dir
        double toShooterX = -dirX;
        double toShooterZ = -dirZ;

        // cos угла между направлением лба и направлением на стрелка:
        // > cos(45°) -> попадание в ЛОБ
        // < -cos(45°) -> попадание в КОРМУ
        // иначе -> попадание в БОРТ
        double dotFront = frontX * toShooterX + frontZ * toShooterZ;
        dotFront = MathHelper.clamp(dotFront, -1.0D, 1.0D);
        double angleFromFrontDeg = Math.toDegrees(Math.acos(dotFront));

        String zoneName;
        int nominalArmor;
        double normalAngleDeg;

        if (angleFromFrontDeg <= 48.0D) {
            zoneName = "Лоб";
            nominalArmor = FRONT_ARMOR_MM;
            // Учитываем конструктивный наклон ВЛД (+18 градусов к углу встречи)
            normalAngleDeg = Math.min(85.0D, angleFromFrontDeg + 15.0D);
        } else if (angleFromFrontDeg >= 132.0D) {
            zoneName = "Корма";
            nominalArmor = REAR_ARMOR_MM;
            normalAngleDeg = Math.min(82.0D, 180.0D - angleFromFrontDeg);
        } else {
            zoneName = "Борт";
            nominalArmor = SIDE_ARMOR_MM;
            normalAngleDeg = Math.min(85.0D, Math.abs(90.0D - angleFromFrontDeg));
        }

        double cosImpact = Math.max(0.18D, Math.cos(Math.toRadians(normalAngleDeg)));
        int effectiveArmor = (int) Math.round(nominalArmor / cosImpact);
        return new ArmorHitResult(zoneName, nominalArmor, (float) normalAngleDeg, effectiveArmor);
    }

    public static class ArmorHitResult {
        public final String zoneName;
        public final int nominalArmorMm;
        public final float impactAngleDeg;
        public final int effectiveArmorMm;

        public ArmorHitResult(String zoneName, int nominalArmorMm, float impactAngleDeg, int effectiveArmorMm) {
            this.zoneName = zoneName;
            this.nominalArmorMm = nominalArmorMm;
            this.impactAngleDeg = impactAngleDeg;
            this.effectiveArmorMm = effectiveArmorMm;
        }
    }

    /**
     * Обработка попадания танкового снаряда (с расчётом пробития, рикошета и критов модулей).
     * Возвращает true, если снаряд срикошетил и должен продолжить полёт.
     */
    public boolean handleShellImpact(EntityTankShell shell, @Nullable Entity shooterEntity) {
        if (world.isRemote || isDead) {
            return false;
        }

        ShellType shellType = shell.getShellType();
        Vec3d incoming = new Vec3d(shell.motionX, shell.motionY, shell.motionZ);
        ArmorHitResult armor = evaluateArmorAtVector(incoming);

        EntityPlayer shooterPlayer = null;
        if (shooterEntity instanceof EntityPlayer) {
            shooterPlayer = (EntityPlayer) shooterEntity;
        } else if (shooterEntity instanceof EntityTank) {
            Entity ctrl = ((EntityTank) shooterEntity).getControllingPassenger();
            if (ctrl instanceof EntityPlayer) {
                shooterPlayer = (EntityPlayer) ctrl;
            }
        }

        EntityPlayer victimPlayer = getControllingPassenger() instanceof EntityPlayer
                ? (EntityPlayer) getControllingPassenger() : null;

        // 1. Проверка на РИКОШЕТ (для ББ и КС при остром угле встречи)
        if (shellType != ShellType.HE && armor.impactAngleDeg >= shellType.getRicochetAngleDeg()) {
            world.playSound(null, posX, posY + 1.0D, posZ, SoundEvents.BLOCK_ANVIL_PLACE,
                    SoundCategory.PLAYERS, 1.0F, 1.85F);
            if (shooterPlayer != null) {
                sendStatus(shooterPlayer, TextFormatting.GOLD + "Рикошет! (" + armor.zoneName
                        + ", угол " + (int) armor.impactAngleDeg + "°)");
            }
            if (victimPlayer != null) {
                sendStatus(victimPlayer, TextFormatting.GREEN + "Рикошет от нашей брони! (" + armor.zoneName + ")");
            }
            return true; // снаряд рикошетит
        }

        // 2. Разброс бронепробития ±15% (как в WoTB)
        float penRoll = 0.85F + world.rand.nextFloat() * 0.30F;
        int actualPenetration = Math.round(shellType.getPenetrationMm() * penRoll);

        // 3. Проверка пробития
        if (actualPenetration < armor.effectiveArmorMm) {
            if (shellType == ShellType.HE) {
                // Осколочно-фугасный снаряд наносит сплэш-урон даже без пробития!
                int splashDamage = Math.max(8, Math.round(shellType.getAverageDamage() * 0.35F
                        - armor.nominalArmorMm * 0.08F));
                boolean trackCrit = world.rand.nextFloat() < 0.45F;
                if (trackCrit) {
                    setTrackDamagedTicks(100); // 5 секунд ремонта гусеницы
                }
                applyTankDamage(splashDamage, shooterPlayer);
                world.playSound(null, posX, posY + 0.8D, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE,
                        SoundCategory.PLAYERS, 0.9F, 1.2F);

                if (shooterPlayer != null) {
                    sendStatus(shooterPlayer, TextFormatting.YELLOW + "Осколочное накрытие (" + armor.zoneName
                            + "): -" + splashDamage + " HP"
                            + (trackCrit ? TextFormatting.GOLD + " [Гусеница сбита!]" : ""));
                }
                if (victimPlayer != null && trackCrit) {
                    sendStatus(victimPlayer, TextFormatting.RED + "Гусеница сбита! Движение ограничено!");
                }
            } else {
                // ББ / КС — Не пробил!
                world.playSound(null, posX, posY + 1.0D, posZ, SoundEvents.BLOCK_ANVIL_LAND,
                        SoundCategory.PLAYERS, 0.9F, 1.5F);
                if (shooterPlayer != null) {
                    sendStatus(shooterPlayer, TextFormatting.RED + "Броня не пробита! ("
                            + armor.zoneName + ": " + armor.effectiveArmorMm + " мм vs "
                            + actualPenetration + " мм)");
                }
                if (victimPlayer != null) {
                    sendStatus(victimPlayer, TextFormatting.GREEN + "Броня выдержала попадание ("
                            + armor.zoneName + ")!");
                }
            }
            return false;
        }

        // 4. ПРОБИТИЕ БРОНИ! Разброс урона ±15%
        float dmgRoll = 0.85F + world.rand.nextFloat() * 0.30F;
        int finalDamage = Math.max(10, Math.round(shellType.getAverageDamage() * dmgRoll));

        String critText = "";
        if ("Борт".equals(armor.zoneName) && world.rand.nextFloat() < 0.35F) {
            setTrackDamagedTicks(100);
            critText = " [Гусеница сбита!]";
        } else if ("Корма".equals(armor.zoneName) && world.rand.nextFloat() < 0.40F) {
            finalDamage += 12;
            setFire(4);
            critText = " [Пожар двигателя!]";
        }

        world.playSound(null, posX, posY + 0.9D, posZ, SoundEvents.ENTITY_ZOMBIE_BREAK_DOOR_WOOD,
                SoundCategory.PLAYERS, 1.0F, 0.8F);
        applyTankDamage(finalDamage, shooterPlayer);

        if (shooterPlayer != null) {
            sendStatus(shooterPlayer, TextFormatting.GREEN + "Пробитие! (" + armor.zoneName + ") "
                    + TextFormatting.RED + "-" + finalDamage + " HP"
                    + TextFormatting.GOLD + critText
                    + TextFormatting.GRAY + " [" + getHealth() + "/" + MAX_HEALTH + "]");
        }
        if (victimPlayer != null) {
            sendStatus(victimPlayer, TextFormatting.RED + "Пробитие (" + armor.zoneName + "): -"
                    + finalDamage + " HP!" + TextFormatting.GOLD + critText);
        }
        return false;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isEntityInvulnerable(source) || world.isRemote || isDead) {
            return false;
        }
        // Танковая броня игнорирует мелкий урон от стрел/кактусов/удушья
        if (source == DamageSource.CACTUS || source == DamageSource.IN_WALL || source.isProjectile()) {
            return false;
        }
        // Демонтаж пустого танка ударом гаечного ключа
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
            ItemStack held = attacker.getHeldItemMainhand();
            if (!held.isEmpty() && held.getItem() == ModItems.TANK_REPAIR_KIT && attacker.isSneaking() && !isBeingRidden()) {
                ItemStack spawner = new ItemStack(ModItems.TANK_SPAWNER);
                NBTTagCompound tag = new NBTTagCompound();
                writeEntityToNBT(tag);
                spawner.setTagInfo("TankData", tag);
                entityDropItem(spawner, 0.5F);
                setDead();
                return true;
            }
        }

        // Обычный урон в ближнем бою/от взрывов снижается танковой бронёй
        int dmg = Math.max(1, Math.round(amount * 0.65F));
        EntityPlayer attacker = source.getTrueSource() instanceof EntityPlayer
                ? (EntityPlayer) source.getTrueSource() : null;
        applyTankDamage(dmg, attacker);
        return true;
    }

    private void applyTankDamage(int dmg, @Nullable EntityPlayer attacker) {
        int newHp = getHealth() - dmg;
        setHealth(newHp);
        if (newHp <= 0 && !isDead) {
            // Уничтожение танка и взрыв БК
            world.createExplosion(this, posX, posY + 0.7D, posZ, 2.5F, false);
            if (!world.isRemote) {
                dropItem(Items.IRON_INGOT, 4 + world.rand.nextInt(4));
            }
            if (attacker != null) {
                sendStatus(attacker, TextFormatting.GOLD + "★ Танк противника уничтожен! ★");
            }
            setDead();
        }
    }

    // ---------- Стрельба из главного орудия (ПКМ или клавиша G) ----------
    public void fire(EntityPlayer shooter) {
        if (world.isRemote || isDead) {
            return;
        }
        if (getFireCooldown() > 0) {
            float secLeft = getFireCooldown() / 20.0F;
            sendStatus(shooter, String.format(TextFormatting.YELLOW + "Орудие перезаряжается: %.1f с", secLeft));
            return;
        }

        ShellType shellType = getSelectedShellType();
        int currentAmmo = getAmmoCount(shellType);

        // Если текущий тип снарядов закончился — автоматически ищем доступный
        if (currentAmmo <= 0) {
            for (ShellType candidate : ShellType.values()) {
                if (getAmmoCount(candidate) > 0) {
                    shellType = candidate;
                    setSelectedShellType(candidate);
                    currentAmmo = getAmmoCount(candidate);
                    sendStatus(shooter, TextFormatting.YELLOW + "Автосмена снаряда на " + candidate.getShortNameRu());
                    break;
                }
            }
        }

        if (currentAmmo <= 0) {
            world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_DISPENSER_FAIL,
                    SoundCategory.PLAYERS, 1.0F, 1.2F);
            sendStatus(shooter, TextFormatting.RED + "Боекомплект пуст! Пополните снаряды ящиком БК.");
            return;
        }

        // Расходуем снаряд и запускаем перезарядку и откат ствола
        setAmmoCount(shellType, currentAmmo - 1);
        setFireCooldown(FIRE_COOLDOWN_TICKS);
        setRecoilTicks(MAX_RECOIL_TICKS);

        // Выстрел идёт строго по текущему положению башни и ствола
        double yawRad = Math.toRadians(turretYaw);
        double pitchRad = Math.toRadians(turretPitch);
        double dirX = -MathHelper.sin((float) yawRad) * MathHelper.cos((float) pitchRad);
        double dirY = -MathHelper.sin((float) pitchRad);
        double dirZ = MathHelper.cos((float) yawRad) * MathHelper.cos((float) pitchRad);

        // Координаты дульного среза
        double mx = posX + dirX * 2.95D;
        double my = posY + 1.48D + dirY * 2.95D;
        double mz = posZ + dirZ * 2.95D;

        EntityTankShell shell = new EntityTankShell(world, this, mx, my, mz, shellType);
        shell.shootFrom(turretYaw, turretPitch, shellType.getSpeed());
        world.spawnEntity(shell);

        // Звук выстрела и дульная вспышка
        float pitchSound = shellType == ShellType.HE ? 1.15F : (shellType == ShellType.HEAT ? 1.45F : 1.35F);
        world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 0.95F, pitchSound);
        world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_FIREWORK_BLAST,
                SoundCategory.PLAYERS, 1.0F, 0.65F);

        // Отдача корпуса при выстреле
        motionX -= dirX * 0.08D;
        motionZ -= dirZ * 0.08D;
    }

    // ---------- Спаренный пулемёт (клавиша B) ----------
    public void fireMachineGun(EntityPlayer shooter) {
        if (world.isRemote || isDead || mgCooldown > 0) {
            return;
        }
        mgCooldown = 4; // 5 выстрелов в секунду

        double yawRad = Math.toRadians(turretYaw);
        double pitchRad = Math.toRadians(turretPitch);
        double dirX = -MathHelper.sin((float) yawRad) * MathHelper.cos((float) pitchRad);
        double dirY = -MathHelper.sin((float) pitchRad);
        double dirZ = MathHelper.cos((float) yawRad) * MathHelper.cos((float) pitchRad);

        Vec3d start = new Vec3d(posX + dirX * 1.9D, posY + 1.45D + dirY * 1.9D, posZ + dirZ * 1.9D);
        Vec3d end = start.addVector(dirX * 38.0D, dirY * 38.0D, dirZ * 38.0D);

        RayTraceResult blockHit = world.rayTraceBlocks(start, end);
        if (blockHit != null && blockHit.hitVec != null) {
            end = blockHit.hitVec;
        }

        Entity hitEntity = null;
        double closest = start.distanceTo(end);
        List<Entity> candidates = world.getEntitiesWithinAABBExcludingEntity(this,
                getEntityBoundingBox().expand(dirX * 38.0D, dirY * 38.0D, dirZ * 38.0D).grow(1.0D));

        for (Entity candidate : candidates) {
            if (candidate == shooter || !candidate.canBeCollidedWith()) {
                continue;
            }
            AxisAlignedBB aabb = candidate.getEntityBoundingBox().grow(0.3D);
            RayTraceResult intercept = aabb.calculateIntercept(start, end);
            if (intercept != null) {
                double dist = start.distanceTo(intercept.hitVec);
                if (dist < closest) {
                    closest = dist;
                    hitEntity = candidate;
                }
            }
        }

        if (hitEntity != null && !(hitEntity instanceof EntityTank)) {
            hitEntity.attackEntityFrom(DamageSource.causePlayerDamage(shooter), 4.0F);
            hitEntity.hurtResistantTime = 0;
        }

        world.playSound(null, posX, posY + 1.4D, posZ, SoundEvents.ENTITY_FIREWORK_BLAST,
                SoundCategory.PLAYERS, 0.55F, 1.85F);
    }

    // ---------- Смена типа снаряда (клавиша R) ----------
    public void cycleShellType(EntityPlayer player) {
        if (world.isRemote || isDead) {
            return;
        }
        ShellType next = getSelectedShellType().next();
        setSelectedShellType(next);
        // При смене снаряда во время заряженного орудия — короткая перезарядка (как без перка в WoT)
        if (getFireCooldown() < 15) {
            setFireCooldown(15);
        }
        world.playSound(null, posX, posY, posZ, SoundEvents.ITEM_ARMOR_EQUIP_IRON,
                SoundCategory.PLAYERS, 0.8F, 1.3F);
        sendStatus(player, TextFormatting.AQUA + "Выбран снаряд: " + TextFormatting.BOLD + next.getShortNameRu()
                + TextFormatting.RESET + TextFormatting.GRAY + " (Пробитие: " + next.getPenetrationMm()
                + " мм | Урон: " + next.getAverageDamage() + " | Остаток: " + getAmmoCount(next) + ")");
    }

    // ---------- Форсаж двигателя (клавиша F) ----------
    public void activateEngineBoost(EntityPlayer player) {
        if (world.isRemote || isDead) {
            return;
        }
        if (getEngineBoostTicks() > 0) {
            sendStatus(player, TextFormatting.GOLD + "Форсаж уже активен!");
            return;
        }
        if (engineBoostCooldown > 0) {
            sendStatus(player, String.format(TextFormatting.YELLOW + "Форсаж остывает: %.1f с",
                    engineBoostCooldown / 20.0F));
            return;
        }
        setEngineBoostTicks(160); // 8 секунд
        engineBoostCooldown = 400; // 20 секунд КД
        world.playSound(null, posX, posY, posZ, SoundEvents.ITEM_FIRECHARGE_USE,
                SoundCategory.PLAYERS, 0.9F, 0.85F);
        sendStatus(player, TextFormatting.GOLD + "⚡ Форсаж двигателя включён (+35% мощности на 8 с)!");
    }

    // ---------- Смена камуфляжа (клавиша C) ----------
    public void cycleCamo(@Nullable EntityPlayer player) {
        if (world.isRemote || isDead) {
            return;
        }
        int nextCamo = (getCamoType() + 1) % 3;
        setCamoType(nextCamo);
        String camoName = nextCamo == 0 ? "Лесной камуфляж"
                : (nextCamo == 1 ? "Пустынный камуфляж" : "Зимний камуфляж");
        world.playSound(null, posX, posY, posZ, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                SoundCategory.PLAYERS, 0.8F, 1.1F);
        if (player != null) {
            sendStatus(player, TextFormatting.GREEN + "Камуфляж: " + camoName);
        }
    }

    private void sendStatus(EntityPlayer player, String message) {
        player.sendStatusMessage(new TextComponentString(message), true);
    }

    // ---------- Обновление танка каждый тик ----------
    @Override
    public void onUpdate() {
        super.onUpdate();

        prevTurretYaw = turretYaw;
        prevTurretPitch = turretPitch;
        prevWheelRotation = wheelRotation;

        if (!world.isRemote) {
            if (getFireCooldown() > 0) {
                setFireCooldown(getFireCooldown() - 1);
            }
            if (mgCooldown > 0) {
                mgCooldown--;
            }
            if (engineBoostCooldown > 0) {
                engineBoostCooldown--;
            }
            if (getTrackDamagedTicks() > 0) {
                setTrackDamagedTicks(getTrackDamagedTicks() - 1);
                if (getTrackDamagedTicks() == 0 && getControllingPassenger() instanceof EntityPlayer) {
                    sendStatus((EntityPlayer) getControllingPassenger(),
                            TextFormatting.GREEN + "Гусеница восстановлена! Можно продолжать движение.");
                }
            }
            if (getEngineBoostTicks() > 0) {
                setEngineBoostTicks(getEngineBoostTicks() - 1);
            }
            if (getRecoilTicks() > 0) {
                setRecoilTicks(getRecoilTicks() - 1);
            }
        }

        Entity passenger = getControllingPassenger();
        if (passenger instanceof EntityLivingBase) {
            EntityLivingBase rider = (EntityLivingBase) passenger;

            // Плавный поворот башни со скоростью TURRET_TRAVERSE_SPEED
            float targetYaw = rider.rotationYawHead;
            float yawDiff = MathHelper.wrapDegrees(targetYaw - turretYaw);
            float clampedTurn = MathHelper.clamp(yawDiff, -TURRET_TRAVERSE_SPEED, TURRET_TRAVERSE_SPEED);
            turretYaw += clampedTurn;

            // Плавное наведение ствола с ограничением УВН (-22° вверх .. +10° вниз)
            float targetPitch = MathHelper.clamp(rider.rotationPitch, MIN_PITCH_UP, MAX_PITCH_DOWN);
            float pitchDiff = targetPitch - turretPitch;
            turretPitch += MathHelper.clamp(pitchDiff, -4.0F, 4.0F);
            turretPitch = MathHelper.clamp(turretPitch, MIN_PITCH_UP, MAX_PITCH_DOWN);

            if (!world.isRemote) {
                this.dataManager.set(DATA_TURRET_YAW, turretYaw);
                this.dataManager.set(DATA_TURRET_PITCH, turretPitch);
            }

            boolean tracksIntact = getTrackDamagedTicks() <= 0;
            float boostMultiplier = getEngineBoostTicks() > 0 ? 1.35F : 1.0F;

            // Поворот корпуса на месте и в движении (если гусеницы целы)
            if (tracksIntact && rider.moveStrafing != 0F) {
                float turn = rider.moveStrafing * 2.2F * boostMultiplier;
                if (rider.moveForward < 0F) {
                    turn = -turn;
                }
                rotationYaw -= turn;
            }

            // Разгон и торможение
            float targetSpeed = 0F;
            if (tracksIntact) {
                if (rider.moveForward > 0F) {
                    targetSpeed = 0.46F * boostMultiplier;
                } else if (rider.moveForward < 0F) {
                    targetSpeed = -0.22F * boostMultiplier;
                }
            }
            currentSpeed += (targetSpeed - currentSpeed) * 0.14F;
            if (Math.abs(currentSpeed) < 0.001F && targetSpeed == 0F) {
                currentSpeed = 0F;
            }
        } else {
            currentSpeed *= 0.82F;
            if (world.isRemote) {
                // Синхронизация башни для сторонних наблюдателей на клиенте
                float serverYaw = this.dataManager.get(DATA_TURRET_YAW);
                float yawDiff = MathHelper.wrapDegrees(serverYaw - turretYaw);
                turretYaw += yawDiff * 0.35F;
                turretPitch += (this.dataManager.get(DATA_TURRET_PITCH) - turretPitch) * 0.35F;
            }
        }

        double yawRad = Math.toRadians(rotationYaw);
        motionX = -MathHelper.sin((float) yawRad) * currentSpeed;
        motionZ = MathHelper.cos((float) yawRad) * currentSpeed;

        // Гравитация
        motionY -= 0.08D;
        if (motionY < -0.6D) {
            motionY = -0.6D;
        }

        prevRotationYaw = rotationYaw;
        move(MoverType.SELF, motionX, motionY, motionZ);

        if (onGround) {
            motionY = 0D;
        }

        // Анимация вращения катков
        wheelRotation += currentSpeed * 3.8F;

        // Таранный урон при столкновении на высокой скорости
        if (!world.isRemote && Math.abs(currentSpeed) > 0.28F) {
            List<Entity> collided = world.getEntitiesWithinAABBExcludingEntity(this,
                    getEntityBoundingBox().grow(0.25D, 0.0D, 0.25D));
            for (Entity entity : collided) {
                if (entity != passenger && entity instanceof EntityLivingBase && !(entity instanceof EntityPlayer)) {
                    entity.attackEntityFrom(DamageSource.causeThornsDamage(this), 6.0F);
                    entity.addVelocity(motionX * 1.4D, 0.25D, motionZ * 1.4D);
                }
            }
        }

        // Визуальные эффекты на клиенте (выхлоп двигателя, дульная вспышка, дым при повреждении)
        if (world.isRemote) {
            spawnClientEffects(yawRad);
        }
    }

    private void spawnClientEffects(double hullYawRad) {
        // Вспышка и дым из дула сразу после выстрела
        if (getRecoilTicks() == MAX_RECOIL_TICKS - 1) {
            double tYawRad = Math.toRadians(turretYaw);
            double tPitchRad = Math.toRadians(turretPitch);
            double dirX = -MathHelper.sin((float) tYawRad) * MathHelper.cos((float) tPitchRad);
            double dirY = -MathHelper.sin((float) tPitchRad);
            double dirZ = MathHelper.cos((float) tYawRad) * MathHelper.cos((float) tPitchRad);
            double mx = posX + dirX * 2.95D;
            double my = posY + 1.48D + dirY * 2.95D;
            double mz = posZ + dirZ * 2.95D;
            world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, mx, my, mz, 0, 0, 0);
            for (int i = 0; i < 8; i++) {
                world.spawnParticle(EnumParticleTypes.FLAME, mx, my, mz,
                        dirX * 0.25D + (world.rand.nextDouble() - 0.5D) * 0.1D,
                        dirY * 0.25D + (world.rand.nextDouble() - 0.5D) * 0.1D,
                        dirZ * 0.25D + (world.rand.nextDouble() - 0.5D) * 0.1D);
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, mx, my, mz,
                        dirX * 0.15D, dirY * 0.15D + 0.04D, dirZ * 0.15D);
            }
        }

        // Выхлопные трубы на корме
        boolean moving = Math.abs(currentSpeed) > 0.03F || getEngineBoostTicks() > 0;
        if (isBeingRidden() && (moving || ticksExisted % 5 == 0)) {
            double backX = posX + MathHelper.sin((float) hullYawRad) * 1.65D;
            double backZ = posZ - MathHelper.cos((float) hullYawRad) * 1.65D;
            EnumParticleTypes exhaustType = getEngineBoostTicks() > 0
                    ? EnumParticleTypes.FLAME : EnumParticleTypes.SMOKE_NORMAL;
            world.spawnParticle(exhaustType, backX, posY + 0.95D, backZ,
                    (world.rand.nextDouble() - 0.5D) * 0.03D, 0.04D,
                    (world.rand.nextDouble() - 0.5D) * 0.03D);
        }

        // Густой дым и искры при критическом повреждении танка (< 35% HP)
        if (getHealth() < MAX_HEALTH * 0.35F && ticksExisted % 2 == 0) {
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    posX + (world.rand.nextDouble() - 0.5D) * 1.2D,
                    posY + 1.2D,
                    posZ + (world.rand.nextDouble() - 0.5D) * 1.2D,
                    0.0D, 0.07D, 0.0D);
            if (getHealth() < MAX_HEALTH * 0.20F && ticksExisted % 4 == 0) {
                world.spawnParticle(EnumParticleTypes.LAVA, posX, posY + 1.2D, posZ, 0.0D, 0.05D, 0.0D);
            }
        }
    }

    // ---------- Сохранение в NBT ----------
    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("TankHealth", getHealth());
        compound.setFloat("TurretYaw", turretYaw);
        compound.setFloat("TurretPitch", turretPitch);
        compound.setInteger("ShellType", getSelectedShellType().getId());
        compound.setInteger("AmmoAP", getAmmoCount(ShellType.AP));
        compound.setInteger("AmmoHEAT", getAmmoCount(ShellType.HEAT));
        compound.setInteger("AmmoHE", getAmmoCount(ShellType.HE));
        compound.setInteger("CamoType", getCamoType());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("TankHealth")) {
            setHealth(compound.getInteger("TankHealth"));
        }
        if (compound.hasKey("TurretYaw")) {
            turretYaw = compound.getFloat("TurretYaw");
            prevTurretYaw = turretYaw;
            this.dataManager.set(DATA_TURRET_YAW, turretYaw);
        }
        if (compound.hasKey("TurretPitch")) {
            turretPitch = compound.getFloat("TurretPitch");
            prevTurretPitch = turretPitch;
            this.dataManager.set(DATA_TURRET_PITCH, turretPitch);
        }
        if (compound.hasKey("ShellType")) {
            setSelectedShellType(ShellType.byId(compound.getInteger("ShellType")));
        }
        if (compound.hasKey("AmmoAP")) {
            setAmmoCount(ShellType.AP, compound.getInteger("AmmoAP"));
        }
        if (compound.hasKey("AmmoHEAT")) {
            setAmmoCount(ShellType.HEAT, compound.getInteger("AmmoHEAT"));
        }
        if (compound.hasKey("AmmoHE")) {
            setAmmoCount(ShellType.HE, compound.getInteger("AmmoHE"));
        }
        if (compound.hasKey("CamoType")) {
            setCamoType(compound.getInteger("CamoType"));
        }
    }
}
