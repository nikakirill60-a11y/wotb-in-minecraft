package com.wotbminecraft.client.model;

import com.wotbminecraft.entity.EntityTank;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * Детализированная модель боевого танка (корпус с ВЛД/МТО/баками, гусеницы с вращающимися катками,
 * башня с командирской башенкой, нишей и антенной, орудие с маской, эжектором, дульным тормозом и откатом).
 * Соглашения: фронт танка = -Z, башня крутится вокруг Y, ствол — вокруг X.
 */
public class ModelTankPlaceholder extends ModelBase {

    // Ходовая часть
    private final ModelRenderer trackLeft;
    private final ModelRenderer trackRight;
    private final ModelRenderer fenderLeft;
    private final ModelRenderer fenderRight;
    private final ModelRenderer[] wheelsLeft = new ModelRenderer[5];
    private final ModelRenderer[] wheelsRight = new ModelRenderer[5];

    // Корпус
    private final ModelRenderer hullMain;
    private final ModelRenderer glacisUpper;
    private final ModelRenderer glacisLower;
    private final ModelRenderer engineDeck;
    private final ModelRenderer fuelDrumLeft;
    private final ModelRenderer fuelDrumRight;

    // Башня (все детали башни вращаются вместе с ней)
    private final ModelRenderer turret;

    // Ствол и маска орудия (вращаются по Y вместе с башней и наклоняются по X)
    private final ModelRenderer barrelGroup;
    private final ModelRenderer barrelMain;
    private final ModelRenderer boreEvacuator;
    private final ModelRenderer muzzleBrake;

    public ModelTankPlaceholder() {
        textureWidth = 128;
        textureHeight = 64;

        // ---------- Ходовая часть и гусеницы ----------
        trackLeft = new ModelRenderer(this, 0, 0);
        trackLeft.setRotationPoint(-15F, 13F, 0F);
        trackLeft.addBox(-4F, 1F, -24F, 8, 10, 48);

        trackRight = new ModelRenderer(this, 0, 0);
        trackRight.setRotationPoint(15F, 13F, 0F);
        trackRight.addBox(-4F, 1F, -24F, 8, 10, 48);

        // Надгусеничные полки (бортовые экраны)
        fenderLeft = new ModelRenderer(this, 0, 12);
        fenderLeft.setRotationPoint(-15F, 11.5F, 0F);
        fenderLeft.addBox(-5F, 0F, -25F, 9, 2, 50);

        fenderRight = new ModelRenderer(this, 0, 12);
        fenderRight.setRotationPoint(15F, 11.5F, 0F);
        fenderRight.addBox(-4F, 0F, -25F, 9, 2, 50);

        // Опорные катки (по 5 с каждой стороны, вращаются при движении)
        float[] wheelZ = new float[]{-18F, -9F, 0F, 9F, 18F};
        for (int i = 0; i < 5; i++) {
            wheelsLeft[i] = new ModelRenderer(this, 0, 0);
            wheelsLeft[i].setRotationPoint(-19.2F, 19.5F, wheelZ[i]);
            wheelsLeft[i].addBox(-1F, -3.5F, -3.5F, 2, 7, 7);

            wheelsRight[i] = new ModelRenderer(this, 0, 0);
            wheelsRight[i].setRotationPoint(19.2F, 19.5F, wheelZ[i]);
            wheelsRight[i].addBox(-1F, -3.5F, -3.5F, 2, 7, 7);
        }

        // ---------- Корпус танка ----------
        hullMain = new ModelRenderer(this, 0, 12);
        hullMain.setRotationPoint(0F, 6F, 0F);
        hullMain.addBox(-12F, 2F, -20F, 24, 9, 42);

        // Наклонная верхняя лобовая деталь (ВЛД)
        glacisUpper = new ModelRenderer(this, 0, 20);
        glacisUpper.setRotationPoint(0F, 8F, -20F);
        glacisUpper.addBox(-12F, 0F, -7F, 24, 5, 8);
        glacisUpper.rotateAngleX = 0.38F;

        // Нижняя лобовая деталь (НЛД)
        glacisLower = new ModelRenderer(this, 0, 20);
        glacisLower.setRotationPoint(0F, 13F, -25F);
        glacisLower.addBox(-11.5F, 0F, 0F, 23, 4, 6);
        glacisLower.rotateAngleX = -0.32F;

        // Возвышение моторно-трансмиссионного отделения (МТО)
        engineDeck = new ModelRenderer(this, 8, 16);
        engineDeck.setRotationPoint(0F, 6F, 8F);
        engineDeck.addBox(-10F, 0.5F, 0F, 20, 2, 13);

        // Внешние кормовые топливные баки
        fuelDrumLeft = new ModelRenderer(this, 64, 18);
        fuelDrumLeft.setRotationPoint(-7F, 8.5F, 23F);
        fuelDrumLeft.addBox(-4F, -2F, -2F, 7, 5, 5);

        fuelDrumRight = new ModelRenderer(this, 64, 18);
        fuelDrumRight.setRotationPoint(7F, 8.5F, 23F);
        fuelDrumRight.addBox(-3F, -2F, -2F, 7, 5, 5);

        // ---------- Башня ----------
        turret = new ModelRenderer(this, 64, 0);
        turret.setRotationPoint(0F, 7F, -1F);
        // Основной бронекорпус башни
        turret.addBox(-10F, -6F, -11F, 20, 7, 22);
        // Передняя суженная часть башни (скулы)
        turret.setTextureOffset(68, 2).addBox(-8F, -5.5F, -15F, 16, 6, 4);
        // Кормовая ниша башни (забашенный ящик БК)
        turret.setTextureOffset(66, 4).addBox(-8.5F, -5.5F, 11F, 17, 6, 6);
        // Командирская башенка с триплексами
        turret.setTextureOffset(72, 0).addBox(2.5F, -8.5F, -2F, 6, 3, 6);
        // Люк заряжающего
        turret.setTextureOffset(72, 0).addBox(-7.5F, -7.2F, -3F, 5, 2, 5);
        // Радиоантенна на корме башни
        turret.setTextureOffset(64, 32).addBox(-7.5F, -18F, 9F, 1, 12, 1);

        // ---------- Орудие и маска пушки ----------
        barrelGroup = new ModelRenderer(this, 64, 32);
        barrelGroup.setRotationPoint(0F, 4.2F, -14F);
        // Бронемаска орудия
        barrelGroup.setTextureOffset(68, 8).addBox(-4.5F, -2.5F, -3F, 9, 5, 5);
        // Спаренный пулемёт справа от маски
        barrelGroup.setTextureOffset(64, 32).addBox(5.2F, -0.5F, -7F, 1, 1, 7);

        // Ствол (откатывается при выстреле вдоль оси Z)
        barrelMain = new ModelRenderer(this, 64, 32);
        barrelMain.setRotationPoint(0F, 0F, 0F);
        barrelMain.addBox(-1.5F, -1.5F, -32F, 3, 3, 30);

        // Эжектор ствола (утолщение для продувки пороховых газов)
        boreEvacuator = new ModelRenderer(this, 64, 32);
        boreEvacuator.setRotationPoint(0F, 0F, 0F);
        boreEvacuator.addBox(-2.0F, -2.0F, -21F, 4, 4, 7);

        // Двухкамерный дульный тормоз
        muzzleBrake = new ModelRenderer(this, 64, 32);
        muzzleBrake.setRotationPoint(0F, 0F, 0F);
        muzzleBrake.addBox(-2.5F, -2.0F, -36F, 5, 4, 4);

        barrelGroup.addChild(barrelMain);
        barrelGroup.addChild(boreEvacuator);
        barrelGroup.addChild(muzzleBrake);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (entityIn instanceof EntityTank) {
            EntityTank tank = (EntityTank) entityIn;
            // netHeadYaw передаётся из RenderTank как интерполированный относительный угол башни,
            // headPitch — интерполированный наклон ствола, ageInTicks — откат орудия (0..1), limbSwing — вращение катков
            float relYawRad = (float) Math.toRadians(netHeadYaw);
            float pitchRad = (float) Math.toRadians(headPitch);

            turret.rotateAngleY = relYawRad;

            // Точка крепления маски орудия смещена от оси башни вперёд на 13 единиц модели:
            // вычисляем её положение при повороте башни, чтобы маска сидела точно в амбразуре башни!
            float pivotDist = 13.0F;
            barrelGroup.rotationPointX = -MathHelper.sin(relYawRad) * pivotDist;
            barrelGroup.rotationPointZ = -1.0F - MathHelper.cos(relYawRad) * pivotDist;
            barrelGroup.rotateAngleY = relYawRad;
            barrelGroup.rotateAngleX = pitchRad;

            // Анимация отката ствола при выстреле
            float recoilZ = MathHelper.sin(ageInTicks * (float) Math.PI) * 5.5F;
            barrelMain.offsetZ = recoilZ * scale;
            boreEvacuator.offsetZ = recoilZ * scale;
            muzzleBrake.offsetZ = recoilZ * scale;

            // Вращение опорных катков
            for (int i = 0; i < 5; i++) {
                wheelsLeft[i].rotateAngleX = limbSwing;
                wheelsRight[i].rotateAngleX = limbSwing;
            }
        }

        trackLeft.render(scale);
        trackRight.render(scale);
        fenderLeft.render(scale);
        fenderRight.render(scale);
        for (int i = 0; i < 5; i++) {
            wheelsLeft[i].render(scale);
            wheelsRight[i].render(scale);
        }

        hullMain.render(scale);
        glacisUpper.render(scale);
        glacisLower.render(scale);
        engineDeck.render(scale);
        fuelDrumLeft.render(scale);
        fuelDrumRight.render(scale);

        turret.render(scale);
        barrelGroup.render(scale);
    }
}
