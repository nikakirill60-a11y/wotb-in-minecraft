package com.wotbminecraft.client.gui;

import com.wotbminecraft.client.input.ModKeyBindings;
import com.wotbminecraft.entity.EntityTank;
import com.wotbminecraft.entity.ShellType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * Боевой интерфейс танкиста и снайперский прицел с индикатором бронепробития в стиле World of Tanks Blitz.
 */
public class TankHudOverlay extends Gui {

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new TankHudOverlay());
    }

    @SubscribeEvent
    public void onFovModifier(EntityViewRenderEvent.FOVModifier event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && mc.player.getRidingEntity() instanceof EntityTank) {
            int zoom = ModKeyBindings.getSniperZoomLevel();
            if (zoom == 1) {
                event.setFOV(event.getFOV() * 0.45F);
            } else if (zoom == 2) {
                event.setFOV(event.getFOV() * 0.22F);
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && mc.player.getRidingEntity() instanceof EntityTank) {
            if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS
                    && ModKeyBindings.getSniperZoomLevel() > 0) {
                // В снайперском режиме заменяем стандартное перекрестие на прицельную сетку WoTB
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.gameSettings.hideGUI) {
            return;
        }
        Entity riding = mc.player.getRidingEntity();
        if (!(riding instanceof EntityTank)) {
            return;
        }

        EntityTank tank = (EntityTank) riding;
        ScaledResolution res = event.getResolution();
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();
        FontRenderer fr = mc.fontRenderer;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();

        int zoom = ModKeyBindings.getSniperZoomLevel();
        if (zoom > 0) {
            renderSniperScope(mc, fr, tank, width, height, zoom);
        } else {
            renderArcadeAimIndicator(fr, tank, mc.player.rotationYawHead, width, height);
        }

        renderVehicleStatusPanel(fr, tank, width, height);
        renderAmmoAndControlsPanel(fr, tank, width, height);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Снайперский оптический прицел с дальномером и расчётом бронепробития.
     */
    private void renderSniperScope(Minecraft mc, FontRenderer fr, EntityTank tank,
                                   int width, int height, int zoom) {
        int cx = width / 2;
        int cy = height / 2;
        int scopeHalfH = Math.min(width, height) / 2 - 18;
        int scopeHalfW = (int) (scopeHalfH * 1.35F);

        // Затемнение по краям оптического прицела
        int vignetteColor = 0xCC080C0A;
        drawRect(0, 0, width, cy - scopeHalfH, vignetteColor);
        drawRect(0, cy + scopeHalfH, width, height, vignetteColor);
        drawRect(0, cy - scopeHalfH, cx - scopeHalfW, cy + scopeHalfH, vignetteColor);
        drawRect(cx + scopeHalfW, cy - scopeHalfH, width, cy + scopeHalfH, vignetteColor);

        // Тонкая зелёная рамка оптики
        int borderColor = 0xAA28E060;
        drawRect(cx - scopeHalfW, cy - scopeHalfH, cx + scopeHalfW, cy - scopeHalfH + 1, borderColor);
        drawRect(cx - scopeHalfW, cy + scopeHalfH - 1, cx + scopeHalfW, cy + scopeHalfH, borderColor);
        drawRect(cx - scopeHalfW, cy - scopeHalfH, cx - scopeHalfW + 1, cy + scopeHalfH, borderColor);
        drawRect(cx + scopeHalfW - 1, cy - scopeHalfH, cx + scopeHalfW, cy + scopeHalfH, borderColor);

        // Перекрестие и шкала тысячных (mil-dots)
        int lineColor = 0x9930FF70;
        drawRect(cx - scopeHalfW + 15, cy, cx - 10, cy + 1, lineColor);
        drawRect(cx + 11, cy, cx + scopeHalfW - 15, cy + 1, lineColor);
        drawRect(cx, cy - scopeHalfH + 15, cx + 1, cy - 10, lineColor);
        drawRect(cx, cy + 11, cx + 1, cy + scopeHalfH - 15, lineColor);

        for (int i = 1; i <= 5; i++) {
            int offset = i * 16;
            drawRect(cx - offset, cy - 3, cx - offset + 1, cy + 4, lineColor);
            drawRect(cx + offset, cy - 3, cx + offset + 1, cy + 4, lineColor);
            drawRect(cx - 3, cy + offset, cx + 4, cy + offset + 1, lineColor);
        }

        // Рейтрейс вдоль ствола для дальномера и индикатора бронепробития
        double yawRad = Math.toRadians(tank.turretYaw);
        double pitchRad = Math.toRadians(tank.turretPitch);
        double dirX = -MathHelper.sin((float) yawRad) * MathHelper.cos((float) pitchRad);
        double dirY = -MathHelper.sin((float) pitchRad);
        double dirZ = MathHelper.cos((float) yawRad) * MathHelper.cos((float) pitchRad);

        Vec3d start = new Vec3d(tank.posX + dirX * 2.2D, tank.posY + 1.48D + dirY * 2.2D, tank.posZ + dirZ * 2.2D);
        Vec3d end = start.addVector(dirX * 80.0D, dirY * 80.0D, dirZ * 80.0D);

        RayTraceResult blockHit = mc.world.rayTraceBlocks(start, end);
        Vec3d hitVec = (blockHit != null && blockHit.hitVec != null) ? blockHit.hitVec : end;
        double distance = start.distanceTo(hitVec);

        Entity targetEntity = null;
        List<Entity> list = mc.world.getEntitiesWithinAABBExcludingEntity(tank,
                tank.getEntityBoundingBox().expand(dirX * 80.0D, dirY * 80.0D, dirZ * 80.0D).grow(1.0D));
        for (Entity e : list) {
            if (e == mc.player || !e.canBeCollidedWith()) {
                continue;
            }
            AxisAlignedBB aabb = e.getEntityBoundingBox().grow(0.35D);
            RayTraceResult intercept = aabb.calculateIntercept(start, hitVec);
            if (intercept != null) {
                double d = start.distanceTo(intercept.hitVec);
                if (d < distance) {
                    distance = d;
                    targetEntity = e;
                }
            }
        }

        // Цвет центрального маркера (зелёный = пробитие, жёлтый = 50/50, красный = не пробьёт / рикошет)
        int centerMarkerColor = 0xFF30FF60;
        String armorInfo = null;
        ShellType currentShell = tank.getSelectedShellType();

        if (targetEntity instanceof EntityTank) {
            EntityTank enemyTank = (EntityTank) targetEntity;
            EntityTank.ArmorHitResult armor = enemyTank.evaluateArmorAtVector(new Vec3d(dirX, dirY, dirZ));
            int pen = currentShell.getPenetrationMm();
            if (currentShell != ShellType.HE && armor.impactAngleDeg >= currentShell.getRicochetAngleDeg()) {
                centerMarkerColor = 0xFFFF3333;
                armorInfo = String.format("§cРИКОШЕТ (%s, %d°) | HP цели: %d/%d",
                        armor.zoneName, (int) armor.impactAngleDeg, enemyTank.getHealth(), EntityTank.MAX_HEALTH);
            } else if (pen >= armor.effectiveArmorMm * 1.1F) {
                centerMarkerColor = 0xFF30FF60;
                armorInfo = String.format("§aПРОБИТИЕ: %s (%d мм vs %d мм) | HP: %d/%d",
                        armor.zoneName, armor.effectiveArmorMm, pen, enemyTank.getHealth(), EntityTank.MAX_HEALTH);
            } else if (pen >= armor.effectiveArmorMm * 0.85F || currentShell == ShellType.HE) {
                centerMarkerColor = 0xFFFFCC22;
                armorInfo = String.format("§eБРОНЯ: %s (%d мм vs %d мм) | HP: %d/%d",
                        armor.zoneName, armor.effectiveArmorMm, pen, enemyTank.getHealth(), EntityTank.MAX_HEALTH);
            } else {
                centerMarkerColor = 0xFFFF3333;
                armorInfo = String.format("§cНЕ ПРОБЬЁТ: %s (%d мм vs %d мм) | HP: %d/%d",
                        armor.zoneName, armor.effectiveArmorMm, pen, enemyTank.getHealth(), EntityTank.MAX_HEALTH);
            }
        } else if (targetEntity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) targetEntity;
            armorInfo = String.format("§aЦель: %s (%.0f HP)", living.getName(), living.getHealth());
        }

        // Центральный маркер бронепробития
        drawRect(cx - 4, cy - 4, cx + 5, cy - 3, centerMarkerColor);
        drawRect(cx - 4, cy + 4, cx + 5, cy + 5, centerMarkerColor);
        drawRect(cx - 4, cy - 3, cx - 3, cy + 4, centerMarkerColor);
        drawRect(cx + 4, cy - 3, cx + 5, cy + 4, centerMarkerColor);

        // Информация о кратности, дистанции и перезарядке внутри прицела
        String zoomStr = String.format("ПРИЦЕЛ %dx | %.1f м", zoom == 1 ? 2 : 4, distance);
        fr.drawStringWithShadow(zoomStr, cx - scopeHalfW + 10, cy - scopeHalfH + 8, 0x30FF70);

        String pitchStr = String.format("УВН: %+.1f°", -tank.turretPitch);
        fr.drawStringWithShadow(pitchStr, cx + scopeHalfW - fr.getStringWidth(pitchStr) - 10,
                cy - scopeHalfH + 8, 0x30FF70);

        if (armorInfo != null) {
            fr.drawStringWithShadow(armorInfo, cx - fr.getStringWidth(armorInfo) / 2, cy - 24, 0xFFFFFF);
        }

        renderReloadBarAt(fr, tank, cx, cy + 20);
    }

    /**
     * Индикатор сведения башни и перезарядки в аркадном режиме.
     */
    private void renderArcadeAimIndicator(FontRenderer fr, EntityTank tank,
                                          float playerLookYaw, int width, int height) {
        int cx = width / 2;
        int cy = height / 2;

        // Если башня ещё доворачивается к взгляду игрока — показываем маркер положения ствола
        float yawDiff = MathHelper.wrapDegrees(tank.turretYaw - playerLookYaw);
        int turretMarkerX = cx + MathHelper.clamp((int) (yawDiff * 2.2F), -70, 70);
        int markerColor = Math.abs(yawDiff) < 2.5F ? 0xCC30FF60 : 0xCCFFAA20;

        drawRect(turretMarkerX - 5, cy - 5, turretMarkerX + 6, cy - 4, markerColor);
        drawRect(turretMarkerX - 5, cy + 5, turretMarkerX + 6, cy + 6, markerColor);
        drawRect(turretMarkerX - 5, cy - 4, turretMarkerX - 4, cy + 5, markerColor);
        drawRect(turretMarkerX + 5, cy - 4, turretMarkerX + 6, cy + 5, markerColor);

        renderReloadBarAt(fr, tank, cx, cy + 14);
    }

    private void renderReloadBarAt(FontRenderer fr, EntityTank tank, int cx, int y) {
        int cooldown = tank.getFireCooldown();
        int barW = 54;
        drawRect(cx - barW / 2 - 1, y - 1, cx + barW / 2 + 1, y + 5, 0xAA000000);

        if (cooldown <= 0) {
            drawRect(cx - barW / 2, y, cx + barW / 2, y + 4, 0xCC28E850);
            String ready = "ГОТОВО";
            fr.drawStringWithShadow(ready, cx - fr.getStringWidth(ready) / 2, y + 7, 0x38FF68);
        } else {
            float progress = 1.0F - MathHelper.clamp((float) cooldown / EntityTank.FIRE_COOLDOWN_TICKS, 0.0F, 1.0F);
            int fillW = (int) (barW * progress);
            drawRect(cx - barW / 2, y, cx - barW / 2 + fillW, y + 4, 0xCCFF9820);
            String secStr = String.format("%.1f с", cooldown / 20.0F);
            fr.drawStringWithShadow(secStr, cx - fr.getStringWidth(secStr) / 2, y + 7, 0xFFAE30);
        }
    }

    /**
     * Левая нижняя панель состояния танка (HP, скорость, модули, ориентация башни).
     */
    private void renderVehicleStatusPanel(FontRenderer fr, EntityTank tank, int width, int height) {
        int panelX = 8;
        int panelY = height - 76;
        int panelW = 148;
        int panelH = 68;

        // Полупрозрачный фон с рамкой
        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, 0xB8101614);
        drawRect(panelX, panelY, panelX + panelW, panelY + 1, 0xCC3A5246);
        drawRect(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xCC3A5246);

        // Полоска прочности (HP)
        int hp = tank.getHealth();
        float hpRatio = MathHelper.clamp((float) hp / EntityTank.MAX_HEALTH, 0.0F, 1.0F);
        int hpColor = hpRatio > 0.5F ? 0xFF28E050 : (hpRatio > 0.25F ? 0xFFFFB820 : 0xFFFF3030);

        int barX = panelX + 6;
        int barY = panelY + 6;
        int barW = panelW - 12;
        drawRect(barX, barY, barX + barW, barY + 11, 0xCC050806);
        drawRect(barX + 1, barY + 1, barX + 1 + (int) ((barW - 2) * hpRatio), barY + 10, hpColor);

        String hpText = "HP: " + hp + " / " + EntityTank.MAX_HEALTH;
        fr.drawStringWithShadow(hpText, barX + (barW - fr.getStringWidth(hpText)) / 2, barY + 2, 0xFFFFFF);

        // Скорость и передача
        float kmh = tank.getSpeedKmh();
        float spdRaw = tank.getCurrentSpeedBlocksPerTick();
        String gear = spdRaw > 0.01F ? "FWD" : (spdRaw < -0.01F ? "REV" : "N");
        fr.drawStringWithShadow(String.format("Скор: §f%.0f км/ч §7[%s]", kmh, gear),
                panelX + 6, panelY + 22, 0xA8C0B0);

        // Угол поворота башни относительно корпуса
        float relTurret = MathHelper.wrapDegrees(tank.turretYaw - tank.rotationYaw);
        fr.drawStringWithShadow(String.format("Башня: §f%+d° §7| УВН: §f%+d°",
                Math.round(relTurret), Math.round(-tank.turretPitch)),
                panelX + 6, panelY + 34, 0xA8C0B0);

        // Статус гусениц
        if (tank.getTrackDamagedTicks() > 0) {
            fr.drawStringWithShadow(String.format("§cГусеница сбита (%.1fс)!",
                    tank.getTrackDamagedTicks() / 20.0F), panelX + 6, panelY + 46, 0xFF4040);
        } else {
            fr.drawStringWithShadow("§aГусеницы: ОК", panelX + 6, panelY + 46, 0x50E070);
        }

        // Статус двигателя / форсажа
        if (tank.getEngineBoostTicks() > 0) {
            fr.drawStringWithShadow(String.format("§6⚡ Форсаж: %.1fс",
                    tank.getEngineBoostTicks() / 20.0F), panelX + 6, panelY + 57, 0xFFCC20);
        } else {
            fr.drawStringWithShadow("§7Броня: 155/85/45 мм", panelX + 6, panelY + 57, 0x90A098);
        }
    }

    /**
     * Правая нижняя панель снарядов (ББ / КС / ОФ) и подсказок управления.
     */
    private void renderAmmoAndControlsPanel(FontRenderer fr, EntityTank tank, int width, int height) {
        ShellType selected = tank.getSelectedShellType();
        ShellType[] types = ShellType.values();

        int slotW = 46;
        int slotH = 34;
        int gap = 4;
        int totalW = types.length * slotW + (types.length - 1) * gap;
        int startX = width - totalW - 8;
        int startY = height - slotH - 22;

        for (int i = 0; i < types.length; i++) {
            ShellType type = types[i];
            int sx = startX + i * (slotW + gap);
            boolean isSel = (type == selected);

            int bg = isSel ? 0xD01C2822 : 0xA00E1210;
            int border = isSel ? (0xFF000000 | type.getHudColor()) : 0x8838443E;

            drawRect(sx, startY, sx + slotW, startY + slotH, bg);
            drawRect(sx, startY, sx + slotW, startY + 1, border);
            drawRect(sx, startY + slotH - 1, sx + slotW, startY + slotH, border);
            drawRect(sx, startY, sx + 1, startY + slotH, border);
            drawRect(sx + slotW - 1, startY, sx + slotW, startY + slotH, border);

            int count = tank.getAmmoCount(type);
            String title = type.getShortNameRu() + ": " + count;
            fr.drawStringWithShadow(title, sx + 4, startY + 4, type.getHudColor());
            fr.drawStringWithShadow(type.getPenetrationMm() + "мм", sx + 4, startY + 14, 0xCCCCCC);
            fr.drawStringWithShadow(type.getAverageDamage() + " ур", sx + 4, startY + 23, 0x99AAAA);
        }

        // Компактная строка управления под слотами снарядов
        String controls = "§eПКМ/G§7:Огонь §eR§7:Снаряд §eV§7:Прицел §eB§7:Пулемёт §eF§7:Форсаж";
        fr.drawStringWithShadow(controls, width - fr.getStringWidth(controls) - 8, height - 12, 0xFFFFFF);
    }
}
