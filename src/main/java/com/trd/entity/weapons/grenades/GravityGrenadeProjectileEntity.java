package com.trd.entity.weapons.grenades;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import com.trd.item.ModItems;

import java.util.List;
import java.util.Random;

public class GravityGrenadeProjectileEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> EFFECT_ACTIVE =
            SynchedEntityData.defineId(GravityGrenadeProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTIVE_TICKS =
            SynchedEntityData.defineId(GravityGrenadeProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CENTER_X =
            SynchedEntityData.defineId(GravityGrenadeProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CENTER_Y =
            SynchedEntityData.defineId(GravityGrenadeProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CENTER_Z =
            SynchedEntityData.defineId(GravityGrenadeProjectileEntity.class, EntityDataSerializers.FLOAT);

    private static final int PULL_DURATION = 40;
    private static final float EFFECT_RADIUS = 15.0f;
    private static final float PULL_STRENGTH = 0.6f;
    private static final float PUSH_STRENGTH = 2.5f;
    private static final float UP_BOOST = 2.25f;
    private static final Random RANDOM = new Random();

    private boolean exploded = false;

    public GravityGrenadeProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    @SuppressWarnings("unchecked")
    public GravityGrenadeProjectileEntity(EntityType<?> entityType, Level level, LivingEntity thrower) {
        super((EntityType<? extends ThrowableItemProjectile>) entityType, thrower, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(EFFECT_ACTIVE, false);
        this.entityData.define(ACTIVE_TICKS, 0);
        this.entityData.define(CENTER_X, 0.0f);
        this.entityData.define(CENTER_Y, 0.0f);
        this.entityData.define(CENTER_Z, 0.0f);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GRAVITY_GRENADE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (this.entityData.get(EFFECT_ACTIVE)) {
            int ticks = this.entityData.get(ACTIVE_TICKS);
            float cx = this.entityData.get(CENTER_X);
            float cy = this.entityData.get(CENTER_Y);
            float cz = this.entityData.get(CENTER_Z);
            Vec3 center = new Vec3(cx, cy, cz);

            if (ticks < PULL_DURATION) {
                applyPull(center);
                this.entityData.set(ACTIVE_TICKS, ticks + 1);
            } else if (ticks == PULL_DURATION) {
                applyPush(center);
                this.entityData.set(ACTIVE_TICKS, ticks + 1);
                this.discard();
            } else {
                this.discard();
            }
        }
    }

    /**
     * Притягиваем ВСЕ сущности, кроме гранаты и владельца.
     */
    private void applyPull(Vec3 center) {
        double radiusSq = EFFECT_RADIUS * EFFECT_RADIUS;
        AABB area = new AABB(center, center).inflate(EFFECT_RADIUS);
        List<Entity> entities = level().getEntitiesOfClass(Entity.class, area,
                e -> e != this && e != this.getOwner());

        for (Entity e : entities) {
            double distSq = e.distanceToSqr(center);
            if (distSq > radiusSq) continue;
            double dist = Math.sqrt(distSq);
            double factor = 1.0 - (dist / EFFECT_RADIUS);
            Vec3 direction = new Vec3(center.x - e.getX(), center.y - e.getY(), center.z - e.getZ()).normalize();
            double power = PULL_STRENGTH * factor;
            Vec3 newVel = e.getDeltaMovement().add(direction.scale(power));
            if (newVel.length() > 2.5) newVel = newVel.scale(2.5 / newVel.length());
            e.setDeltaMovement(newVel);
            e.hasImpulse = true;
        }
    }

    /**
     * Разбрасываем ВСЕ сущности с усиленным вертикальным импульсом.
     */
    private void applyPush(Vec3 center) {
        double radiusSq = EFFECT_RADIUS * EFFECT_RADIUS;
        AABB area = new AABB(center, center).inflate(EFFECT_RADIUS);
        List<Entity> entities = level().getEntitiesOfClass(Entity.class, area,
                e -> e != this && e != this.getOwner());

        for (Entity e : entities) {
            double distSq = e.distanceToSqr(center);
            if (distSq > radiusSq) continue;
            double dist = Math.sqrt(distSq);
            double factor = 1.0 - (dist / EFFECT_RADIUS);

            Vec3 dir = new Vec3(e.getX() - center.x, e.getY() - center.y, e.getZ() - center.z).normalize();
            double yaw = RANDOM.nextDouble() * Math.PI * 2;
            double spreadX = Math.cos(yaw) * 0.5 * factor;
            double spreadZ = Math.sin(yaw) * 0.5 * factor;
            double pushHor = PUSH_STRENGTH * factor;
            double pushUp = UP_BOOST + RANDOM.nextDouble() * 1.8; // увеличенный случайный бонус

            Vec3 impulse = new Vec3(
                    dir.x * pushHor + spreadX,
                    pushUp,
                    dir.z * pushHor + spreadZ
            );
            e.setDeltaMovement(impulse);
            e.hasImpulse = true;
            if (!(e instanceof LivingEntity)) {
                // Для неживых сущностей можно тихо, но для живых оставляем звук
                continue;
            }
            e.level().playSound(null, e.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
        level().playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 3.0f, 0.5f);
    }

    /**
     * Попадание в блок → активация в точке удара.
     */
    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide || exploded) return;
        activateEffect(result.getLocation());
    }

    /**
     * Попадание в любую сущность → активация без отскока (коллизия отключена).
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide || exploded) return;
        // Не вызываем super.onHitEntity(), чтобы не было отскока/коллизии
        activateEffect(result.getEntity().position());
    }

    private void activateEffect(Vec3 hitPos) {
        if (this.entityData.get(EFFECT_ACTIVE)) return;
        this.entityData.set(EFFECT_ACTIVE, true);
        this.entityData.set(ACTIVE_TICKS, 0);
        this.entityData.set(CENTER_X, (float) hitPos.x);
        this.entityData.set(CENTER_Y, (float) hitPos.y);
        this.entityData.set(CENTER_Z, (float) hitPos.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        level().playSound(null, hitPos.x, hitPos.y, hitPos.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.6f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("EffectActive", this.entityData.get(EFFECT_ACTIVE));
        tag.putInt("ActiveTicks", this.entityData.get(ACTIVE_TICKS));
        tag.putDouble("CenterX", this.entityData.get(CENTER_X));
        tag.putDouble("CenterY", this.entityData.get(CENTER_Y));
        tag.putDouble("CenterZ", this.entityData.get(CENTER_Z));
        tag.putBoolean("Exploded", exploded);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(EFFECT_ACTIVE, tag.getBoolean("EffectActive"));
        this.entityData.set(ACTIVE_TICKS, tag.getInt("ActiveTicks"));
        if (tag.contains("CenterX")) {
            this.entityData.set(CENTER_X, (float) tag.getDouble("CenterX"));
            this.entityData.set(CENTER_Y, (float) tag.getDouble("CenterY"));
            this.entityData.set(CENTER_Z, (float) tag.getDouble("CenterZ"));
        }
        this.exploded = tag.getBoolean("Exploded");
    }
}