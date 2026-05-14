package com.trd.entity.weapons.grenades;

import com.trd.item.ModItems;
import com.trd.sound.ModSounds;
import com.trd.util.explosions.ExplosionFire;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class GrenadeIfProjectileEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> TIMER_ACTIVATED = SynchedEntityData.defineId(GrenadeIfProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DETONATION_TIME = SynchedEntityData.defineId(GrenadeIfProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> GRENADE_IF_TYPE_ID = SynchedEntityData.defineId(GrenadeIfProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_STUCK = SynchedEntityData.defineId(GrenadeIfProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int FUSE_SECONDS = 4;
    private static final float MIN_BOUNCE_SPEED = 0.15f;
    private static final Random RANDOM = new Random();

    private GrenadeIfType grenadeType;
    private boolean exploded = false;

    // Конструктор для регистрации
    public GrenadeIfProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    // Конструктор для броска из Item
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

        if (this.entityData.get(DATA_STUCK)) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (this.entityData.get(TIMER_ACTIVATED)) {
            if (this.tickCount >= this.entityData.get(DETONATION_TIME)) {
                explode(this.blockPosition());
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide || exploded) return;

        if (!this.entityData.get(TIMER_ACTIVATED)) {
            this.entityData.set(TIMER_ACTIVATED, true);
            this.entityData.set(DETONATION_TIME, this.tickCount + (FUSE_SECONDS * 20));
        }

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) result;
            if (grenadeType == null) {
                grenadeType = GrenadeIfType.valueOf(this.entityData.get(GRENADE_IF_TYPE_ID));
            }

            if (grenadeType == GrenadeIfType.GRENADE_IF_SLIME) {
                this.entityData.set(DATA_STUCK, true);
                this.setDeltaMovement(Vec3.ZERO);
                this.setNoGravity(true);
                Vec3 pos = result.getLocation();
                this.setPos(pos.x, pos.y, pos.z);
            } else {
                handleBounce(blockHit);
            }
        }
    }

    private void handleBounce(BlockHitResult result) {
        Vec3 velocity = this.getDeltaMovement();
        float speed = (float) velocity.length();

        if (speed < MIN_BOUNCE_SPEED) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
            this.level().playSound(null, this.blockPosition(), ModSounds.BOUNCE_RANDOM.get(), SoundSource.NEUTRAL, 0.5F, 0.8F);
            return;
        }

        BlockPos blockPos = result.getBlockPos();
        this.level().playSound(null, blockPos, ModSounds.BOUNCE_RANDOM.get(), SoundSource.NEUTRAL, 2.1F, 1.0F);

        Vec3 currentVelocity = this.getDeltaMovement();
        Vec3 hitNormal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        Vec3 reflectedVelocity = currentVelocity.subtract(hitNormal.scale(2 * currentVelocity.dot(hitNormal)));
        this.setDeltaMovement(reflectedVelocity.scale(grenadeType.getBounceMultiplier()));
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
        if (grenadeType != null) {
            tag.putString("GrenadeType", grenadeType.name());
        }
        tag.putBoolean("Exploded", exploded);
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
    }
}