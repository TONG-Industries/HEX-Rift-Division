package com.trd.entity.weapons.grenades;

import com.trd.explosion.logic.ExplosionFireRaycast;
import com.trd.explosion.logic.ExplosionHE;
import com.trd.explosion.logic.ExplosionStandard;
import com.trd.sound.ModSounds;
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

public class GrenadeIfProjectileEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> TIMER_ACTIVATED =
            SynchedEntityData.defineId(GrenadeIfProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DETONATION_TIME =
            SynchedEntityData.defineId(GrenadeIfProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> GRENADE_IF_TYPE_ID =
            SynchedEntityData.defineId(GrenadeIfProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_STUCK =
            SynchedEntityData.defineId(GrenadeIfProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STUCK_ENTITY_ID =
            SynchedEntityData.defineId(GrenadeIfProjectileEntity.class, EntityDataSerializers.INT);

    private static final int FUSE_SECONDS = 4;
    private static final float MIN_BOUNCE_SPEED = 0.1f;
    private static final float BOUNCE_MULTIPLIER = 0.4f;

    private GrenadeIfType grenadeType;
    private boolean exploded = false;
    private int stuckEntityId = -1;
    private Vec3 stuckOffset = Vec3.ZERO;

    public GrenadeIfProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    @SuppressWarnings("unchecked")
    public GrenadeIfProjectileEntity(EntityType<?> entityType, Level level,
                                     LivingEntity thrower, GrenadeIfType type) {
        super((EntityType<? extends ThrowableItemProjectile>) entityType, thrower, level);
        this.grenadeType = type;
        this.entityData.set(GRENADE_IF_TYPE_ID, type.name());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TIMER_ACTIVATED, false);
        this.entityData.define(DETONATION_TIME, 0);
        this.entityData.define(GRENADE_IF_TYPE_ID, GrenadeIfType.GRENADE_IF.name());
        this.entityData.define(DATA_STUCK, false);
        this.entityData.define(STUCK_ENTITY_ID, -1);
    }

    @Override
    protected Item getDefaultItem() {
        if (grenadeType == null) {
            try {
                grenadeType = GrenadeIfType.valueOf(this.entityData.get(GRENADE_IF_TYPE_ID));
            } catch (Exception e) {
                grenadeType = GrenadeIfType.GRENADE_IF;
            }
        }
        return grenadeType != null ? grenadeType.getItem() : Items.SNOWBALL;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (this.entityData.get(DATA_STUCK) && stuckEntityId != -1) {
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
                explode(this.blockPosition());
                return;
            }
        }

        if (this.entityData.get(DATA_STUCK) && stuckEntityId == -1) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (this.entityData.get(TIMER_ACTIVATED)) {
            if (this.tickCount >= this.entityData.get(DETONATION_TIME)) {
                explode(this.blockPosition());
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // НЕ вызываем super.onHitBlock для неслаймовых, иначе граната удалится (discard)
        if (level().isClientSide || exploded) return;

        activateTimer();

        if (grenadeType == null) {
            grenadeType = GrenadeIfType.valueOf(this.entityData.get(GRENADE_IF_TYPE_ID));
        }

        if (grenadeType == GrenadeIfType.GRENADE_IF_SLIME) {
            super.onHitBlock(result); // слаймовую не трогаем, оставляем старую логику
            this.entityData.set(DATA_STUCK, true);
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
            Vec3 pos = result.getLocation();
            this.setPos(pos.x, pos.y, pos.z);
        } else {
            handleBounce(result);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide || exploded) return;

        activateTimer();

        if (grenadeType == null) {
            grenadeType = GrenadeIfType.valueOf(this.entityData.get(GRENADE_IF_TYPE_ID));
        }

        if (grenadeType == GrenadeIfType.GRENADE_IF_SLIME) {
            super.onHitEntity(result); // слаймовую не трогаем
            stickToEntity(result.getEntity());
        }
    }

    private void activateTimer() {
        if (!this.entityData.get(TIMER_ACTIVATED)) {
            this.entityData.set(TIMER_ACTIVATED, true);
            this.entityData.set(DETONATION_TIME, this.tickCount + (FUSE_SECONDS * 20));
        }
    }

    private void handleBounce(BlockHitResult result) {
        Vec3 velocity = this.getDeltaMovement();
        float speed = (float) velocity.length();

        if (speed < MIN_BOUNCE_SPEED) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
            return;
        }

        BlockPos blockPos = result.getBlockPos();
        level().playSound(null, blockPos, ModSounds.BOUNCE_RANDOM.get(), SoundSource.NEUTRAL, 2.1F, 1.0F);

        Vec3 currentVelocity = this.getDeltaMovement();
        Vec3 hitNormal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        Vec3 reflectedVelocity = currentVelocity.subtract(hitNormal.scale(2 * currentVelocity.dot(hitNormal)));
        this.setDeltaMovement(reflectedVelocity.scale(BOUNCE_MULTIPLIER));
        this.hasImpulse = true;
    }

    private void stickToEntity(Entity entity) {
        this.entityData.set(DATA_STUCK, true);
        this.stuckEntityId = entity.getId();
        this.setNoGravity(true);
        this.entityData.set(STUCK_ENTITY_ID, entity.getId());

        Vec3 mobCenter = entity.getBoundingBox().getCenter();
        Vec3 toGrenade = this.position().subtract(mobCenter);
        double dist = toGrenade.length();
        if (dist < 0.001) {
            toGrenade = new Vec3(0, 1, 0);
            dist = 1.0;
        }
        Vec3 dir = toGrenade.scale(1.0 / dist);

        double desiredDist = entity.getBbWidth() * 0.5 + this.getBbWidth() * 0.5;
        desiredDist = Math.max(desiredDist, dist);

        Vec3 attachPos = mobCenter.add(dir.scale(desiredDist));
        this.stuckOffset = attachPos.subtract(mobCenter);

        this.setPos(attachPos.x, attachPos.y - this.getBbHeight() * 0.5 + entity.getBbHeight() * 0.5, attachPos.z);
    }

    private void explode(BlockPos pos) {
        if (level().isClientSide || this.isRemoved() || exploded) return;
        exploded = true;
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Level level = level();
        switch (grenadeType) {
            case GRENADE_IF -> ExplosionStandard.explode(level, center, this.getOwner(), 5.0f, 45.0f);
            case GRENADE_IF_HE -> ExplosionHE.explode(level, center, this.getOwner(), 8.0f, 80.0f);
            case GRENADE_IF_FIRE -> ExplosionFireRaycast.explode((ServerLevel) level, center, this.getOwner(), 3.0f);
            case GRENADE_IF_SLIME -> ExplosionStandard.explode(level, center, this.getOwner(), 6.0f, 60.0f);
        }
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("TimerActivated", this.entityData.get(TIMER_ACTIVATED));
        tag.putInt("DetonationTime", this.entityData.get(DETONATION_TIME));
        if (grenadeType != null) tag.putString("GrenadeType", grenadeType.name());
        tag.putBoolean("Exploded", exploded);
        tag.putBoolean("Stuck", this.entityData.get(DATA_STUCK));
        tag.putInt("StuckEntityId", stuckEntityId);
        if (stuckOffset != null) {
            tag.putDouble("StuckOffsetX", stuckOffset.x);
            tag.putDouble("StuckOffsetY", stuckOffset.y);
            tag.putDouble("StuckOffsetZ", stuckOffset.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(TIMER_ACTIVATED, tag.getBoolean("TimerActivated"));
        this.entityData.set(DETONATION_TIME, tag.getInt("DetonationTime"));
        if (tag.contains("GrenadeType")) {
            String typeName = tag.getString("GrenadeType");
            this.grenadeType = GrenadeIfType.valueOf(typeName);
            this.entityData.set(GRENADE_IF_TYPE_ID, typeName);
        }
        this.exploded = tag.getBoolean("Exploded");
        this.entityData.set(DATA_STUCK, tag.getBoolean("Stuck"));
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