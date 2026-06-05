package com.trd.entity.weapons.grenades;

import com.trd.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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

    private static final EntityDataAccessor<Boolean> TIMER_ACTIVATED =
            SynchedEntityData.defineId(GravityGrenadeProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DETONATION_TIME =
            SynchedEntityData.defineId(GravityGrenadeProjectileEntity.class, EntityDataSerializers.INT);

    private static final int FUSE_SECONDS = 4;
    private static final float MIN_BOUNCE_SPEED = 0.1f;
    private static final float BOUNCE_MULTIPLIER = 0.4f;

    private static final int PULL_DURATION = 80;
    private static final double ORBIT_RADIUS = 1.5;
    private static final double ORBITS_COUNT = 4.0;

    private static final float EFFECT_RADIUS = 15.0f;
    private static final float PUSH_STRENGTH = 2.5f;
    private static final float UP_BOOST = 2.25f;
    private static final Random RANDOM = new Random();

    // === ПАРАМЕТРЫ ЧАСТИЦ ===
    private static final int PARTICLES_PER_TICK = 12;      // сколько частиц спавнить за тик
    private static final double PARTICLE_ORBIT_RADIUS = 3.0; // радиус орбиты частиц
    private static final double PARTICLE_SPIRAL_SPEED = 0.5; // скорость сжатия спирали частиц

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
        this.entityData.define(TIMER_ACTIVATED, false);
        this.entityData.define(DETONATION_TIME, 0);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GRAVITY_GRENADE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // === КЛИЕНТСКАЯ СТОРОНА: частицы вокруг гранаты ===
            if (this.entityData.get(EFFECT_ACTIVE)) {
                spawnVortexParticles();
            }
            return;
        }

        if (this.entityData.get(TIMER_ACTIVATED)) {
            if (this.tickCount >= this.entityData.get(DETONATION_TIME)) {
                if (!this.entityData.get(EFFECT_ACTIVE)) {
                    activateEffect(this.position());
                }
            }
        }

        if (this.entityData.get(EFFECT_ACTIVE)) {
            int ticks = this.entityData.get(ACTIVE_TICKS);
            float cx = this.entityData.get(CENTER_X);
            float cy = this.entityData.get(CENTER_Y);
            float cz = this.entityData.get(CENTER_Z);
            Vec3 center = new Vec3(cx, cy, cz);

            if (ticks < PULL_DURATION) {
                applyPull(center, ticks);
                spawnServerParticles(center, ticks, false);
                this.entityData.set(ACTIVE_TICKS, ticks + 1);
            } else if (ticks == PULL_DURATION) {
                applyGather(center);
                spawnServerParticles(center, ticks, true);
                this.entityData.set(ACTIVE_TICKS, ticks + 1);
            } else if (ticks == PULL_DURATION + 1) {
                applyPush(center);
                spawnExplosionParticles(center);
                this.entityData.set(ACTIVE_TICKS, ticks + 1);
                this.discard();
            } else {
                this.discard();
            }
        }
    }

    // === ЧАСТИЦЫ НА СЕРВЕРЕ (синхронизируются с клиентом) ===
    private void spawnServerParticles(Vec3 center, int tick, boolean isGather) {
        ServerLevel serverLevel = (ServerLevel) this.level();

        if (isGather) {
            // Фаза сжатия — вспышка и стягивание всех частиц к центру
            serverLevel.sendParticles(
                    ParticleTypes.FLASH,
                    center.x, center.y + 0.5, center.z,
                    1, 0, 0, 0, 0
            );
            // Множество частиц, летящих к центру
            for (int i = 0; i < 30; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double dist = RANDOM.nextDouble() * EFFECT_RADIUS;
                double px = center.x + Math.cos(angle) * dist;
                double pz = center.z + Math.sin(angle) * dist;
                double py = center.y + RANDOM.nextDouble() * 4 - 2;

                Vec3 toCenter = new Vec3(center.x - px, center.y + 0.5 - py, center.z - pz);
                Vec3 vel = toCenter.normalize().scale(0.8);

                serverLevel.sendParticles(
                        ParticleTypes.PORTAL, // можно заменить на кастомные
                        px, py, pz,
                        0, vel.x, vel.y, vel.z, 0.5
                );
            }
        } else {
            // Фаза вращения — спиральные частицы
            double progress = (double) tick / PULL_DURATION;
            double currentOrbitRadius = PARTICLE_ORBIT_RADIUS * (1.0 - progress * 0.7);

            for (int i = 0; i < PARTICLES_PER_TICK; i++) {
                double angle = (tick * 0.5 + i * (Math.PI * 2 / PARTICLES_PER_TICK)) % (Math.PI * 2);
                double px = center.x + Math.cos(angle) * currentOrbitRadius;
                double pz = center.z + Math.sin(angle) * currentOrbitRadius;
                double py = center.y + 0.5 + Math.sin(tick * 0.2 + i) * 0.3;

                // Скорость частицы по касательной (вращение) + к центру (сжатие)
                double tangentX = -Math.sin(angle);
                double tangentZ = Math.cos(angle);
                double toCenterFactor = PARTICLE_SPIRAL_SPEED * progress;

                serverLevel.sendParticles(
                        ParticleTypes.PORTAL, // можно заменить на DRAGON_BREATH, WITCH, SOUL_FIRE_FLAME и т.д.
                        px, py, pz,
                        0,
                        tangentX * 0.15 - Math.cos(angle) * toCenterFactor,
                        RANDOM.nextDouble() * 0.05,
                        tangentZ * 0.15 - Math.sin(angle) * toCenterFactor,
                        0.3
                );
            }

            // Дополнительные частицы "потока" от дальних мобов к центру
            if (tick % 5 == 0) {
                AABB area = new AABB(center, center).inflate(EFFECT_RADIUS);
                List<Entity> entities = level().getEntitiesOfClass(Entity.class, area, e -> e != this);
                for (Entity e : entities) {
                    if (e.distanceToSqr(center) > 4.0) {
                        Vec3 toCenter = center.subtract(e.position()).normalize();
                        serverLevel.sendParticles(
                                ParticleTypes.SMOKE,
                                e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(),
                                0,
                                toCenter.x * 0.2,
                                toCenter.y * 0.2,
                                toCenter.z * 0.2,
                                0.1
                        );
                    }
                }
            }
        }
    }

    // === КЛИЕНТСКИЕ ЧАСТИЦЫ (локально, без сети) ===
    private void spawnVortexParticles() {
        // Дополнительный локальный эффект вихря вокруг самой гранаты
        double angle = (this.tickCount * 0.8) % (Math.PI * 2);
        double radius = 0.4;
        for (int i = 0; i < 3; i++) {
            double a = angle + i * (Math.PI * 2 / 3);
            double px = this.getX() + Math.cos(a) * radius;
            double py = this.getY() + 0.2 + Math.sin(this.tickCount * 0.3 + i) * 0.1;
            double pz = this.getZ() + Math.sin(a) * radius;

            this.level().addParticle(
                    ParticleTypes.PORTAL,
                    px, py, pz,
                    Math.cos(a + Math.PI / 2) * 0.05,
                    0.02,
                    Math.sin(a + Math.PI / 2) * 0.05
            );
        }
    }

    // === ВЗРЫВНЫЕ ЧАСТИЦЫ ===
    private void spawnExplosionParticles(Vec3 center) {
        ServerLevel serverLevel = (ServerLevel) this.level();

        // Центральный взрыв (как у TNT)
        serverLevel.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y + 0.5, center.z,
                1, 0, 0, 0, 0
        );

        // Множество мелких взрывов по радиусу (ударная волна)
        for (int i = 0; i < 20; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = RANDOM.nextDouble() * 3.0;
            double px = center.x + Math.cos(angle) * dist;
            double pz = center.z + Math.sin(angle) * dist;
            double py = center.y + 0.3 + RANDOM.nextDouble() * 0.5;

            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION,
                    px, py, pz,
                    0, 0, 0, 0, 0
            );
        }

        // Дым и обломки, летящие наружу
        for (int i = 0; i < 40; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double speed = 0.3 + RANDOM.nextDouble() * 0.5;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = 0.2 + RANDOM.nextDouble() * 0.4;

            serverLevel.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    center.x, center.y + 0.5, center.z,
                    0, vx, vy, vz, 0.2
            );
        }

        // Искры
        for (int i = 0; i < 15; i++) {
            serverLevel.sendParticles(
                    ParticleTypes.FIREWORK,
                    center.x, center.y + 0.5, center.z,
                    0,
                    (RANDOM.nextDouble() - 0.5) * 0.8,
                    (RANDOM.nextDouble() - 0.5) * 0.8,
                    (RANDOM.nextDouble() - 0.5) * 0.8,
                    0.3
            );
        }
    }

    // === ОСТАЛЬНЫЕ МЕТОДЫ БЕЗ ИЗМЕНЕНИЙ ===

    private void applyPull(Vec3 center, int tick) {
        double radiusSq = EFFECT_RADIUS * EFFECT_RADIUS;
        AABB area = new AABB(center, center).inflate(EFFECT_RADIUS);
        List<Entity> entities = level().getEntitiesOfClass(Entity.class, area, e -> e != this);

        double angularSpeed = (2.0 * Math.PI * ORBITS_COUNT) / PULL_DURATION;
        double tangentialSpeed = angularSpeed * ORBIT_RADIUS;

        for (Entity e : entities) {
            double distSq = e.distanceToSqr(center);
            if (distSq > radiusSq) continue;

            double dx = e.getX() - center.x;
            double dz = e.getZ() - center.z;
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            double angle = Math.atan2(dz, dx);
            Vec3 tangent = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle));

            Vec3 radialDir;
            if (horizontalDist < 0.001) {
                radialDir = new Vec3(1.0, 0.0, 0.0);
            } else {
                radialDir = new Vec3(dx / horizontalDist, 0.0, dz / horizontalDist);
            }

            double radialDiff = ORBIT_RADIUS - horizontalDist;
            double radialSpeed = radialDiff * 0.25;
            if (radialSpeed > 0.5) radialSpeed = 0.5;
            if (radialSpeed < -0.5) radialSpeed = -0.5;

            double yTarget = center.y + 0.5;
            double yDiff = yTarget - e.getY();
            double ySpeed = yDiff * 0.15;
            if (ySpeed > 0.4) ySpeed = 0.4;
            if (ySpeed < -0.4) ySpeed = -0.4;

            Vec3 newVel = new Vec3(
                    tangent.x * tangentialSpeed + radialDir.x * radialSpeed,
                    ySpeed,
                    tangent.z * tangentialSpeed + radialDir.z * radialSpeed
            );

            e.setDeltaMovement(newVel);
            e.hasImpulse = true;
            e.fallDistance = 0;
            e.setOnGround(false);
        }
    }

    private void applyGather(Vec3 center) {
        double radiusSq = EFFECT_RADIUS * EFFECT_RADIUS;
        AABB area = new AABB(center, center).inflate(EFFECT_RADIUS);
        List<Entity> entities = level().getEntitiesOfClass(Entity.class, area, e -> e != this);

        for (Entity e : entities) {
            double distSq = e.distanceToSqr(center);
            if (distSq > radiusSq) continue;

            Vec3 toCenter = new Vec3(center.x - e.getX(), center.y - e.getY(), center.z - e.getZ());
            double dist = toCenter.length();
            if (dist > 0.01) {
                Vec3 gather = toCenter.normalize().scale(3.0);
                e.setDeltaMovement(gather);
                e.hasImpulse = true;
            } else {
                e.setDeltaMovement(Vec3.ZERO);
            }
            e.fallDistance = 0;
        }
    }

    private void applyPush(Vec3 center) {
        double radiusSq = EFFECT_RADIUS * EFFECT_RADIUS;
        AABB area = new AABB(center, center).inflate(EFFECT_RADIUS);
        List<Entity> entities = level().getEntitiesOfClass(Entity.class, area, e -> e != this);

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
            double pushUp = UP_BOOST + RANDOM.nextDouble() * 1.8;

            Vec3 impulse = new Vec3(
                    dir.x * pushHor + spreadX,
                    pushUp,
                    dir.z * pushHor + spreadZ
            );
            e.setDeltaMovement(impulse);
            e.hasImpulse = true;
            if (!(e instanceof LivingEntity)) continue;
            e.level().playSound(null, e.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
        level().playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 3.0f, 0.5f);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide || exploded || this.entityData.get(EFFECT_ACTIVE)) return;
        activateTimer();
        handleBounce(result);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide || exploded || this.entityData.get(EFFECT_ACTIVE)) return;
        activateTimer();
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
        tag.putBoolean("TimerActivated", this.entityData.get(TIMER_ACTIVATED));
        tag.putInt("DetonationTime", this.entityData.get(DETONATION_TIME));
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
        this.entityData.set(TIMER_ACTIVATED, tag.getBoolean("TimerActivated"));
        this.entityData.set(DETONATION_TIME, tag.getInt("DetonationTime"));
    }
}