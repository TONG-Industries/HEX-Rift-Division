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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class GrenadeProjectileEntity extends ThrowableItemProjectile {

    static final EntityDataAccessor<String> GRENADE_TYPE_ID =
            SynchedEntityData.defineId(GrenadeProjectileEntity.class, EntityDataSerializers.STRING);

    private int bounceCount = 0;
    private GrenadeType grenadeType;
    private boolean stuck = false;
    private int stuckTimer = 0;
    private boolean exploded = false;

    private static final Random RANDOM = new Random();

    // Конструктор для регистрации в ModEntities
    public GrenadeProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    // Конструктор для броска из Item
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

        if (stuck) {
            stuckTimer--;
            if (stuckTimer <= 0) explode(false);
        }

        // SMART: если не встретил моба — взрываем обычным через 10 сек (200 тиков)
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
            stick(result.getLocation());
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
        this.stuckTimer = 40; // 2 секунды
        this.setNoGravity(true);
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
                    ExplosionFireRaycast.explode((ServerLevel) level, pos, this.getOwner(), 4.0f);
                } else {
                    ExplosionStandard.explode(level, pos, this.getOwner(), 3.5f, 20.0f);
                }
            }  case SLIME -> ExplosionStandard.explode(level, pos, this.getOwner(), 3.5f, grenadeType.getCustomDamage());
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
    }
}