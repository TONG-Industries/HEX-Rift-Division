package com.trd.entity.weapons.grenades;

import com.trd.sound.ModSounds;
import com.trd.util.explosions.ExplosionFireRaycast;
import com.trd.util.explosions.ExplosionHE;
import com.trd.util.explosions.ExplosionStandard;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class GrenadeProjectileEntity extends ThrowableItemProjectile {

    static final EntityDataAccessor<String> GRENADE_TYPE_ID =
            SynchedEntityData.defineId(GrenadeProjectileEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Integer> STUCK_ENTITY_ID =
            SynchedEntityData.defineId(GrenadeProjectileEntity.class, EntityDataSerializers.INT);

    private int bounceCount = 0;
    private GrenadeType grenadeType;
    private boolean stuck = false;
    private int stuckTimer = 0;
    private boolean exploded = false;
    private int stuckEntityId = -1;
    private Vec3 stuckOffset = Vec3.ZERO;

    private static final Random RANDOM = new Random();

    public GrenadeProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    @SuppressWarnings("unchecked")
    public GrenadeProjectileEntity(EntityType<?> entityType, Level level,
                                   LivingEntity livingEntity, GrenadeType type) {
        super((EntityType<? extends ThrowableItemProjectile>) entityType, livingEntity, level);
        this.grenadeType = type;
        this.entityData.set(GRENADE_TYPE_ID, type.name());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GRENADE_TYPE_ID, GrenadeType.STANDARD.name());
        this.entityData.define(STUCK_ENTITY_ID, -1);
    }

    @Override
    protected Item getDefaultItem() {
        if (grenadeType == null) {
            try {
                grenadeType = GrenadeType.valueOf(this.entityData.get(GRENADE_TYPE_ID));
            } catch (Exception e) {
                grenadeType = GrenadeType.STANDARD;
            }
        }
        return grenadeType != null ? grenadeType.getItem() : Items.SNOWBALL;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        // Плавное прилипание к сущности — логика как у червя
        if (stuck && stuckEntityId != -1) {
            Entity entity = level().getEntity(stuckEntityId);
            if (entity != null && entity.isAlive()) {
                Vec3 mobCenter = entity.getBoundingBox().getCenter();
                Vec3 desiredPos = mobCenter.add(stuckOffset);

                Vec3 currentPos = this.position();
                double lerp = 0.5;
                double newX = currentPos.x + (desiredPos.x - currentPos.x) * lerp;
                double newY = currentPos.y + (desiredPos.y - currentPos.y) * lerp;
                double newZ = currentPos.z + (desiredPos.z - currentPos.z) * lerp;

                this.setPos(newX, newY, newZ);
                this.setDeltaMovement(entity.getDeltaMovement());
            } else {
                explode(false);
                return;
            }
            stuckTimer--;
            if (stuckTimer <= 0) {
                explode(false);
                return;
            }
        }

        if (stuck && stuckEntityId == -1) {
            stuckTimer--;
            if (stuckTimer <= 0) explode(false);
        }

        if (grenadeType == GrenadeType.SMART && !stuck && !exploded && this.tickCount > 200) {
            explode(false);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide || exploded || stuck) return;
        if (grenadeType == null) {
            grenadeType = GrenadeType.valueOf(this.entityData.get(GRENADE_TYPE_ID));
        }

        BlockPos blockPos = result.getBlockPos();
        level().playSound(null, blockPos, ModSounds.BOUNCE_RANDOM.get(), SoundSource.NEUTRAL, 2.1F, 1.0F);

        if (grenadeType == GrenadeType.SLIME) {
            stick(result.getLocation());
        } else if (grenadeType == GrenadeType.SMART) {
            bounceCount++;
            if (bounceCount >= grenadeType.getMaxBounces() || this.tickCount > 200) {
                explode(false);
            } else {
                bounce(result);
            }
        } else {
            this.bounceCount++;
            if (this.bounceCount >= grenadeType.getMaxBounces()) {
                explode(blockPos);
            } else {
                bounce(result);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide || exploded || stuck) return;
        if (grenadeType == null) {
            grenadeType = GrenadeType.valueOf(this.entityData.get(GRENADE_TYPE_ID));
        }

        if (grenadeType == GrenadeType.SMART) {
            explode(true);
        } else if (grenadeType == GrenadeType.SLIME) {
            stickToEntity(result.getEntity());
        } else if (grenadeType.explodesOnEntity()) {
            explode(result.getEntity().blockPosition());
        }
    }

    private void bounce(BlockHitResult result) {
        Vec3 currentVelocity = this.getDeltaMovement();
        Vec3 hitNormal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        Vec3 reflectedVelocity = currentVelocity.subtract(hitNormal.scale(2 * currentVelocity.dot(hitNormal)));
        this.setDeltaMovement(reflectedVelocity.scale(grenadeType.getBounceMultiplier()));
        this.hasImpulse = true;
    }

    private void stick(Vec3 pos) {
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(pos.x, pos.y, pos.z);
        this.stuck = true;
        this.stuckTimer = 40;
        this.setNoGravity(true);
    }

    private void stickToEntity(Entity entity) {
        this.stuck = true;
        this.stuckEntityId = entity.getId();
        this.setNoGravity(true);
        this.entityData.set(STUCK_ENTITY_ID, entity.getId());

        // Вычисляем смещение от центра моба так, чтобы граната касалась его хитбокса снаружи
        Vec3 mobCenter = entity.getBoundingBox().getCenter();
        Vec3 toGrenade = this.position().subtract(mobCenter);
        double dist = toGrenade.length();
        if (dist < 0.001) {
            toGrenade = new Vec3(0, 1, 0);
            dist = 1.0;
        }
        Vec3 dir = toGrenade.scale(1.0 / dist);

        // Расстояние от центра моба до точки касания
        double desiredDist = entity.getBbWidth() * 0.5 + this.getBbWidth() * 0.5;
        // Если граната уже дальше — не притягиваем внутрь
        desiredDist = Math.max(desiredDist, dist);

        Vec3 attachPos = mobCenter.add(dir.scale(desiredDist));
        this.stuckOffset = attachPos.subtract(mobCenter);

        // Корректируем Y чтобы граната "сидела" на поверхности, а не в центре
        this.setPos(attachPos.x, attachPos.y - this.getBbHeight() * 0.5 + entity.getBbHeight() * 0.5, attachPos.z);

        this.stuckTimer = 40;
    }

    private void explode(boolean smartEntityHit) {
        if (exploded) return;
        exploded = true;
        Vec3 pos = this.position();
        Level level = level();
        switch (grenadeType) {
            case STANDARD -> ExplosionStandard.explode(level, pos, this.getOwner(), 3.5f, grenadeType.getCustomDamage());
            case HE -> ExplosionHE.explode(level, pos, this.getOwner(), 7.0f, grenadeType.getCustomDamage());
            case FIRE -> ExplosionFireRaycast.explode((ServerLevel) level, pos, this.getOwner(), 3.0f);
            case SMART -> {
                if (smartEntityHit) {
                    ExplosionHE.explode(level, pos, this.getOwner(), 7.0f, 40.0f);
                    ExplosionFireRaycast.explode((ServerLevel) level, pos, this.getOwner(), 2.0f);
                } else {
                    ExplosionStandard.explode(level, pos, this.getOwner(), 3.5f, 20.0f);
                }
            }
            case SLIME -> ExplosionStandard.explode(level, pos, this.getOwner(), 3.5f, grenadeType.getCustomDamage());
        }
        this.discard();
    }

    private void explode(BlockPos pos) {
        explode(false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BounceCount", this.bounceCount);
        tag.putString("GrenadeType", this.entityData.get(GRENADE_TYPE_ID));
        tag.putBoolean("Stuck", this.stuck);
        tag.putInt("StuckTimer", this.stuckTimer);
        tag.putBoolean("Exploded", this.exploded);
        tag.putInt("StuckEntityId", this.stuckEntityId);
        if (stuckOffset != null) {
            tag.putDouble("StuckOffsetX", stuckOffset.x);
            tag.putDouble("StuckOffsetY", stuckOffset.y);
            tag.putDouble("StuckOffsetZ", stuckOffset.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.bounceCount = tag.getInt("BounceCount");
        if (tag.contains("GrenadeType")) {
            this.entityData.set(GRENADE_TYPE_ID, tag.getString("GrenadeType"));
            this.grenadeType = GrenadeType.valueOf(tag.getString("GrenadeType"));
        }
        this.stuck = tag.getBoolean("Stuck");
        this.stuckTimer = tag.getInt("StuckTimer");
        this.exploded = tag.getBoolean("Exploded");
        this.stuckEntityId = tag.getInt("StuckEntityId");
        if (tag.contains("StuckOffsetX")) {
            this.stuckOffset = new Vec3(
                    tag.getDouble("StuckOffsetX"),
                    tag.getDouble("StuckOffsetY"),
                    tag.getDouble("StuckOffsetZ")
            );
        }
    }
}