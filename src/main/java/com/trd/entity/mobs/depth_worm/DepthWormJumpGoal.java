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
        // ⭐ Не прыгаем во время отступления
        if (this.worm.isRetreating()) return false;

        this.target = this.worm.getTarget();
        if (this.target == null || !this.target.isAlive()) return false;
        if (this.worm.isInWater() || this.target.isInWater()) return false;
        double dist = this.worm.distanceTo(this.target);
        return dist >= this.jumpRangeMin && dist <= this.jumpRangeMax;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.worm.isRetreating()) return false;
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
        Vec3 wormPos = this.worm.position();
        Vec3 targetPos = this.target.position();

        double targetY = targetPos.y + this.target.getBbHeight() * 0.5;

        double dx = targetPos.x - wormPos.x;
        double dy = targetY - wormPos.y;
        double dz = targetPos.z - wormPos.z;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        double baseSpeed = 0.9;
        double speed = baseSpeed + (horizontalDist * 0.08);
        speed = Math.min(speed, 1.8);

        double verticalBoost;
        if (dy > 2.0) {
            verticalBoost = 0.6 + (dy * 0.15);
        } else if (dy > 0) {
            verticalBoost = 0.4 + (dy * 0.1);
        } else if (dy > -1.0) {
            verticalBoost = 0.35;
        } else {
            verticalBoost = 0.25;
        }

        Vec3 horizontalDir = new Vec3(dx, 0, dz).normalize();

        Vec3 velocity = horizontalDir.scale(speed).add(0, verticalBoost, 0);

        double yaw = Math.atan2(dz, dx) * (180 / Math.PI) - 90;
        this.worm.setYRot((float) yaw);
        this.worm.yHeadRot = (float) yaw;
        this.worm.yBodyRot = (float) yaw;

        this.worm.setDeltaMovement(velocity);
        this.worm.setFlying(true);
        this.worm.ignoreFallDamageTicks = 30;
    }
}