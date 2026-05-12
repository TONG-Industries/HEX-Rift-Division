package com.trd.entity.mobs.depth_worm;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class DepthWormJumpGoal extends Goal {
    private final DepthWormEntity worm;
    private LivingEntity target;
    private final double speedModifier;
    private final float jumpRangeMin, jumpRangeMax;
    private int jumpTimer;
    private boolean jumpPerformed;
    private final int PREPARE_TIME = 30;

    public DepthWormJumpGoal(DepthWormEntity worm, double speedModifier, float jumpRangeMin, float jumpRangeMax) {
        this.worm = worm;
        this.speedModifier = speedModifier;
        this.jumpRangeMin = jumpRangeMin;
        this.jumpRangeMax = jumpRangeMax;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.target = this.worm.getTarget();
        if (this.target == null || !this.target.isAlive()) return false;
        double dist = this.worm.distanceTo(this.target);
        return dist >= this.jumpRangeMin && dist <= this.jumpRangeMax;
    }

    @Override
    public boolean canContinueToUse() {
        return !jumpPerformed && jumpTimer > 0;
    }

    @Override
    public void start() {
        this.jumpTimer = PREPARE_TIME;
        this.jumpPerformed = false;
        this.worm.setAttacking(true);
        this.worm.getNavigation().stop();
        this.worm.hasImpulse = true;
    }

    @Override
    public void stop() {
        this.target = null;
        this.worm.setAttacking(false);
        this.jumpPerformed = false;
    }

    @Override
    public void tick() {
        if (this.target == null || !this.target.isAlive()) {
            this.worm.setAttacking(false);
            this.jumpTimer = 0;
            this.jumpPerformed = true;
            return;
        }

        double dist = this.worm.distanceTo(this.target);
        if (dist > this.jumpRangeMax + 2.0F) {
            this.worm.setAttacking(false);
            this.jumpTimer = 0;
            this.jumpPerformed = true;
            return;
        }

        this.worm.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        if (--this.jumpTimer <= 0 && !jumpPerformed) {
            doJump();
            jumpPerformed = true;
            this.worm.ignoreFallDamageTicks = 30;
        }
    }

    private void doJump() {
        // === УЛУЧШЕННЫЙ РАСЧЁТ С УЧЁТОМ ВЕРТИКАЛИ ===

        Vec3 wormPos = this.worm.position();
        Vec3 targetPos = this.target.position();

        // Целимся в центр тела цели, а не в ноги
        double targetY = targetPos.y + this.target.getBbHeight() * 0.5;

        // Разница по всем осям
        double dx = targetPos.x - wormPos.x;
        double dy = targetY - wormPos.y;
        double dz = targetPos.z - wormPos.z;

        // Горизонтальное расстояние
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Расчёт скорости: чем дальше, тем быстрее, но с ограничением
        double baseSpeed = 0.9;
        double speed = baseSpeed + (horizontalDist * 0.08);
        speed = Math.min(speed, 1.8); // Максимальная скорость

        // Расчёт вертикального импульса с учётом высоты цели
        // Если цель выше - добавляем больше вертикали
        double verticalBoost;
        if (dy > 2.0) {
            // Цель значительно выше - сильный вертикальный бросок
            verticalBoost = 0.6 + (dy * 0.15);
        } else if (dy > 0) {
            // Цель немного выше - умеренный бросок
            verticalBoost = 0.4 + (dy * 0.1);
        } else if (dy > -1.0) {
            // Цель на той же высоте или чуть ниже - небольшой бросок вверх
            verticalBoost = 0.35;
        } else {
            // Цель ниже - минимальный бросок вверх (червь упадёт сам)
            verticalBoost = 0.25;
        }

        // Нормализуем горизонтальное направление
        Vec3 horizontalDir = new Vec3(dx, 0, dz).normalize();

        // Итоговая скорость: горизонтальная составляющая + вертикальная
        Vec3 velocity = horizontalDir.scale(speed).add(0, verticalBoost, 0);

        // Поворачиваем червя мордой к цели
        double yaw = Math.atan2(dz, dx) * (180 / Math.PI) - 90;
        this.worm.setYRot((float) yaw);
        this.worm.yHeadRot = (float) yaw;
        this.worm.yBodyRot = (float) yaw;

        // Применяем движение
        this.worm.setDeltaMovement(velocity);
        this.worm.setFlying(true);
        this.worm.ignoreFallDamageTicks = 30;
    }
}