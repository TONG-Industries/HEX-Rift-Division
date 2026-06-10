package com.trd.entity.weapons.missiles;

import com.trd.explosion.logic.ExplosionHE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class MissileLightEntity extends Projectile implements IEntityAdditionalSpawnData {

    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(MissileLightEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ARMED =
            SynchedEntityData.defineId(MissileLightEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BOOST_PHASE =
            SynchedEntityData.defineId(MissileLightEntity.class, EntityDataSerializers.BOOLEAN);

    // === КОНФИГ ===
    public static final float SPEED = 1.67f;           // ~5/3 блоков/сек
    public static final float MAX_TURN_RATE = 0.45f;   // ВОЗВРАЩЕНА старая манёвренность (~25-30 град/сек)
    public static final int MAX_LIFETIME = 200;         // 10 секунд
    public static final int BOOST_DURATION = 40;       // === НОВОЕ: 2 секунды набора высоты ===
    public static final float DETONATION_RADIUS = 4.0f;
    public static final float DETONATION_DAMAGE = 25.0f;
    public static final float ARMING_DISTANCE = 2.0f;

    // === STATE ===
    private Vec3 launchPos;
    private int age = 0;
    private boolean exploded = false;
    private LivingEntity cachedTarget = null;
    private int targetCacheTimer = 0;
    private int lostTargetTimer = 0;

    public MissileLightEntity(EntityType<? extends MissileLightEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public MissileLightEntity(Level level, Vec3 startPos, LivingEntity target, Entity owner) {
        this(com.trd.entity.ModEntities.MISSILE_LIGHT.get(), level);
        this.setPos(startPos.x, startPos.y, startPos.z);
        this.launchPos = startPos;
        this.setTarget(target);
        this.setOwner(owner);
        this.entityData.set(BOOST_PHASE, true); // Начинаем с фазы набора высоты

        // Начальная скорость ВВЕРХ
        this.setDeltaMovement(0, SPEED * 0.8, 0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(ARMED, false);
        this.entityData.define(BOOST_PHASE, true);
    }

    public void setTarget(LivingEntity target) {
        this.entityData.set(TARGET_ID, target != null ? target.getId() : -1);
        this.cachedTarget = target;
        this.targetCacheTimer = 5;
        this.lostTargetTimer = 0;
    }

    public LivingEntity getTarget() {
        int id = this.entityData.get(TARGET_ID);
        if (id == -1) return null;

        if (cachedTarget != null && cachedTarget.isAlive() && cachedTarget.getId() == id) {
            return cachedTarget;
        }

        if (targetCacheTimer-- <= 0) {
            targetCacheTimer = 5;
            Entity e = this.level().getEntity(id);
            if (e instanceof LivingEntity living && living.isAlive()) {
                cachedTarget = living;
                return living;
            }
            cachedTarget = null;
        }
        return cachedTarget;
    }

    public boolean isArmed() {
        return this.entityData.get(ARMED);
    }

    public boolean isBoostPhase() {
        return this.entityData.get(BOOST_PHASE);
    }

    // === TICK ===

    @Override
    public void tick() {
        if (this.isRemoved() || exploded) return;

        super.tick();
        age++;

        // Таймер жизни
        if (age > MAX_LIFETIME) {
            explode();
            return;
        }

        // === НОВОЕ: фаза набора высоты (первые 2 секунды) ===
        if (isBoostPhase()) {
            if (age >= BOOST_DURATION) {
                // Заканчиваем фазу набора высоты, начинаем наведение
                this.entityData.set(BOOST_PHASE, false);
                this.entityData.set(ARMED, true);
            } else {
                // Продолжаем лететь вертикально вверх
                this.setDeltaMovement(0, SPEED * 0.8, 0);

                // === ФИКС: поворот во время набора высоты (вертикально) ===
                this.setYRot(0);
                this.setXRot(-90); // Смотрим вверх

                // Движение
                Vec3 motion = this.getDeltaMovement();
                this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

                spawnTrailParticles();
                return;
            }
        }

        // Обычная фаза наведения
        Vec3 currentPos = this.position();
        Vec3 currentVel = this.getDeltaMovement();
        float currentSpeed = (float) currentVel.length();

        if (currentSpeed < 0.05f) {
            explode();
            return;
        }

        // Наведение
        if (isArmed()) {
            LivingEntity target = getTarget();

            // Самоуничтожение при потере цели
            if (target == null || !target.isAlive()) {
                lostTargetTimer++;
                if (lostTargetTimer > 10) {
                    explode();
                    return;
                }
            } else {
                lostTargetTimer = 0;
            }

            if (target != null && target.isAlive()) {
                Vec3 targetPos = target.getBoundingBox().getCenter();
                Vec3 toTarget = targetPos.subtract(currentPos).normalize();
                Vec3 currentDir = currentVel.normalize();

                // Плавный поворот
                double dot = currentDir.dot(toTarget);
                double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
                double maxAngle = MAX_TURN_RATE;

                Vec3 newDir;
                if (angle <= maxAngle) {
                    newDir = toTarget;
                } else {
                    Vec3 cross = currentDir.cross(toTarget);
                    if (cross.lengthSqr() < 0.0001) {
                        newDir = toTarget;
                    } else {
                        cross = cross.normalize();
                        newDir = rotateVector(currentDir, cross, maxAngle);
                    }
                }

                this.setDeltaMovement(newDir.scale(SPEED));
            }
        }

        // Движение
        Vec3 motion = this.getDeltaMovement();
        Vec3 nextPos = currentPos.add(motion);

        // Проверка столкновения с блоками
        BlockHitResult blockHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                currentPos, nextPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                this
        ));

        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            this.setPos(blockHit.getLocation());
            explode();
            return;
        }

        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        // Проверка столкновения с сущностями
        checkEntityCollision(currentPos, nextPos);

        // === ФИКС: правильный поворот модели ===
        alignRotationToVelocity();

        // Дымовой след
        spawnTrailParticles();
    }

    private void spawnTrailParticles() {
        if (this.level().isClientSide) {
            Vec3 pos = this.position();
            Vec3 vel = this.getDeltaMovement();

            this.level().addParticle(
                    ParticleTypes.SMOKE,
                    pos.x, pos.y, pos.z,
                    -vel.x * 0.1 + (random.nextDouble() - 0.5) * 0.05,
                    -vel.y * 0.1 + (random.nextDouble() - 0.5) * 0.05,
                    -vel.z * 0.1 + (random.nextDouble() - 0.5) * 0.05
            );

            if (this.tickCount % 3 == 0) {
                this.level().addParticle(
                        ParticleTypes.FLAME,
                        pos.x, pos.y - 0.3, pos.z,
                        (random.nextDouble() - 0.5) * 0.02,
                        -0.05,
                        (random.nextDouble() - 0.5) * 0.02
                );
            }
        }
    }

    /**
     * === ФИКС: верхушка блока (+Y) = лицо сущности, смотрит по направлению движения ===
     */
    private void alignRotationToVelocity() {
        Vec3 vel = this.getDeltaMovement();
        if (vel.lengthSqr() < 0.001) return;

        double horizontalDist = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        // Yaw: куда смотрим по горизонтали (0 = +Z)
        float yaw = (float) (Math.atan2(vel.x, vel.z) * (180D / Math.PI));

        // Pitch: наклон от вертикали
        // vel.y = 0, horizontalDist = 1 → pitch = 0 (горизонтально)
        // vel.y = 1, horizontalDist = 0 → pitch = 90 (вертикально вверх)
        float pitch = (float) (Math.atan2(vel.y, horizontalDist) * (180D / Math.PI));

        this.setYRot(yaw);
        this.setXRot(pitch); // ПОЛОЖИТЕЛЬНЫЙ = вверх, как надо для верхушки блока

        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    private Vec3 rotateVector(Vec3 vec, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dot = vec.dot(axis);

        return new Vec3(
                axis.x * dot * (1 - cos) + vec.x * cos + (-axis.z * vec.y + axis.y * vec.z) * sin,
                axis.y * dot * (1 - cos) + vec.y * cos + ( axis.z * vec.x - axis.x * vec.z) * sin,
                axis.z * dot * (1 - cos) + vec.z * cos + (-axis.y * vec.x + axis.x * vec.y) * sin
        );
    }

    private void checkEntityCollision(Vec3 start, Vec3 end) {
        AABB searchBox = this.getBoundingBox().inflate(0.5);
        var entities = this.level().getEntities(this, searchBox, e ->
                e instanceof LivingEntity && e != this.getOwner() && e.isPickable()
        );

        for (Entity entity : entities) {
            if (entity.getBoundingBox().intersects(this.getBoundingBox())) {
                explode();
                return;
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide && !exploded) {
            explode();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide && !exploded) {
            explode();
        }
    }

    public void explode() {
        if (exploded) return;
        exploded = true;

        if (!this.level().isClientSide) {
            ExplosionHE.explode(
                    this.level(),
                    this.position(),
                    this.getOwner(),
                    DETONATION_RADIUS,
                    DETONATION_DAMAGE
            );
        }
        this.discard();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        Vec3 motion = this.getDeltaMovement();
        buffer.writeDouble(motion.x);
        buffer.writeDouble(motion.y);
        buffer.writeDouble(motion.z);
        buffer.writeDouble(this.getX());
        buffer.writeDouble(this.getY());
        buffer.writeDouble(this.getZ());
        buffer.writeFloat(this.getYRot());
        buffer.writeFloat(this.getXRot());
        buffer.writeInt(this.entityData.get(TARGET_ID));
        buffer.writeBoolean(this.entityData.get(ARMED));
        buffer.writeBoolean(this.entityData.get(BOOST_PHASE));
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        double vx = buffer.readDouble();
        double vy = buffer.readDouble();
        double vz = buffer.readDouble();
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        float yaw = buffer.readFloat();
        float pitch = buffer.readFloat();
        int targetId = buffer.readInt();
        boolean armed = buffer.readBoolean();
        boolean boost = buffer.readBoolean();

        this.setDeltaMovement(vx, vy, vz);
        this.setPos(x, y, z);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
        this.entityData.set(TARGET_ID, targetId);
        this.entityData.set(ARMED, armed);
        this.entityData.set(BOOST_PHASE, boost);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Age", age);
        tag.putBoolean("Exploded", exploded);
        tag.putBoolean("Armed", isArmed());
        tag.putBoolean("BoostPhase", isBoostPhase());
        if (launchPos != null) {
            tag.putDouble("LaunchX", launchPos.x);
            tag.putDouble("LaunchY", launchPos.y);
            tag.putDouble("LaunchZ", launchPos.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        age = tag.getInt("Age");
        exploded = tag.getBoolean("Exploded");
        this.entityData.set(ARMED, tag.getBoolean("Armed"));
        this.entityData.set(BOOST_PHASE, tag.getBoolean("BoostPhase"));
        if (tag.contains("LaunchX")) {
            launchPos = new Vec3(
                    tag.getDouble("LaunchX"),
                    tag.getDouble("LaunchY"),
                    tag.getDouble("LaunchZ")
            );
        }
    }
}