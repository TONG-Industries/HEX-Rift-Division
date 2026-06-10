package com.trd.block.entity.weapons;

import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MissileTurretBlockEntity extends BlockEntity {

    public static final double SEARCH_RADIUS = 100.0;
    public static final double SEARCH_RADIUS_SQR = SEARCH_RADIUS * SEARCH_RADIUS;
    public static final int COOLDOWN_TICKS = 200;
    public static final int SALVO_SIZE = 3;
    public static final int SALVO_INTERVAL = 20; // 1 секунда
    public static final float LAUNCH_HEIGHT = 1.0f;

    private int cooldownTimer = 0;
    private int salvoCounter = 0;
    private int salvoTimer = 0;
    private boolean isSalvoActive = false;
    private LivingEntity currentTarget = null;
    private int scanTimer = 0;
    private static final int SCAN_INTERVAL = 5;

    public MissileTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISSILE_TURRET_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MissileTurretBlockEntity turret) {
        if (level.isClientSide) return;
        turret.tickServer((ServerLevel) level, pos, state);
    }

    private void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        if (cooldownTimer > 0) cooldownTimer--;

        if (isSalvoActive) {
            salvoTimer--;
            if (salvoTimer <= 0) {
                fireMissile(level, pos, state);
                salvoCounter++;
                if (salvoCounter >= SALVO_SIZE) {
                    isSalvoActive = false;
                    salvoCounter = 0;
                    cooldownTimer = COOLDOWN_TICKS;
                } else {
                    salvoTimer = SALVO_INTERVAL;
                }
            }
            return;
        }

        if (cooldownTimer <= 0) {
            scanTimer--;
            if (scanTimer <= 0) {
                scanTimer = SCAN_INTERVAL;
                currentTarget = findBestTarget(level, pos);
                if (currentTarget != null) startSalvo();
            }
        }
    }

    /**
     * === ФИКС: проверяем открытое небо НАД мобом, а не от турели до моба ===
     */
    private boolean hasOpenSky(ServerLevel level, Vec3 mobPos) {
        // Проверяем от позиции головы моба до build limit
        Vec3 from = mobPos.add(0, 1.5, 0); // над головой моба
        Vec3 to = new Vec3(from.x, level.getMaxBuildHeight(), from.z);

        HitResult hit = level.clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
        ));

        return hit.getType() == HitResult.Type.MISS;
    }

    /**
     * === ФИКС: проверяем линию видимости от турели до моба ===
     */
    private boolean hasLineOfSight(ServerLevel level, Vec3 from, Vec3 to) {
        HitResult hit = level.clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
        ));

        if (hit.getType() == HitResult.Type.MISS) return true;
        double hitDist = hit.getLocation().distanceToSqr(to);
        return hitDist < 4.0;
    }

    private LivingEntity findBestTarget(ServerLevel level, BlockPos pos) {
        Vec3 turretCenter = Vec3.atCenterOf(pos).add(0, 0.5, 0);
        AABB searchBox = new AABB(pos).inflate(SEARCH_RADIUS);

        // === ФИКС: ищем ВСЕХ монстров, независимо от высоты ===
        List<Monster> monsters = level.getEntitiesOfClass(
                Monster.class,
                searchBox,
                entity -> {
                    if (!entity.isAlive() || entity.isRemoved()) return false;
                    // Моб должен быть в радиусе по горизонтали
                    double dx = entity.getX() - pos.getX();
                    double dz = entity.getZ() - pos.getZ();
                    return (dx * dx + dz * dz) <= SEARCH_RADIUS_SQR;
                }
        );

        if (monsters.isEmpty()) return null;

        List<TargetScore> scoredTargets = new ArrayList<>();

        for (Monster monster : monsters) {
            Vec3 targetCenter = monster.getBoundingBox().getCenter();

            // === ФИКС: проверяем открытое небо НАД мобом ===
            if (!hasOpenSky(level, targetCenter)) continue;

            // Проверяем линию видимости от турели до моба
            if (!hasLineOfSight(level, turretCenter, targetCenter)) continue;

            double distSqr = monster.distanceToSqr(turretCenter.x, turretCenter.y, turretCenter.z);
            double score = calculateTargetScore(monster, distSqr, turretCenter);
            scoredTargets.add(new TargetScore(monster, score));
        }

        if (scoredTargets.isEmpty()) return null;

        scoredTargets.sort(Comparator.comparingDouble(ts -> ts.score));
        return scoredTargets.get(0).entity;
    }

    private double calculateTargetScore(Monster monster, double distSqr, Vec3 turretPos) {
        double score = Math.sqrt(distSqr);

        if (monster.getTarget() instanceof net.minecraft.world.entity.player.Player) {
            score *= 0.3;
        }

        if (monster.getLastHurtByMob() != null) {
            score *= 0.6;
        }

        if (!monster.onGround()) {
            score *= 1.3;
        }

        score *= (1.0 + monster.getHealth() / monster.getMaxHealth());

        return score;
    }

    private void startSalvo() {
        isSalvoActive = true;
        salvoCounter = 0;
        salvoTimer = 0;
    }

    private void fireMissile(ServerLevel level, BlockPos pos, BlockState state) {
        if (currentTarget == null || !currentTarget.isAlive()) {
            isSalvoActive = false;
            return;
        }

        // Звук запуска
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.BLOCKS, 2.0F, 0.8F + level.random.nextFloat() * 0.4F);

        net.minecraft.core.Direction facing = state.getValue(
                com.trd.block.basic.weapons.MissileTurretBlock.FACING);
        Vec3 launchPos = Vec3.atCenterOf(pos)
                .add(0, LAUNCH_HEIGHT, 0)
                .add(facing.getStepX() * 0.3, 0, facing.getStepZ() * 0.3);

        double spreadX = (level.random.nextDouble() - 0.5) * 0.3;
        double spreadZ = (level.random.nextDouble() - 0.5) * 0.3;
        launchPos = launchPos.add(spreadX, 0, spreadZ);

        com.trd.entity.weapons.missiles.MissileLightEntity missile =
                new com.trd.entity.weapons.missiles.MissileLightEntity(
                        level, launchPos, currentTarget, null);
        level.addFreshEntity(missile);
    }

    public int getCooldownProgress() {
        if (isSalvoActive) return 15;
        if (cooldownTimer <= 0) return 0;
        return (int) (15.0 * (1.0 - (double) cooldownTimer / COOLDOWN_TICKS));
    }

    public void onPlace() {
        cooldownTimer = 40;
    }

    public void onRemove() {}

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Cooldown", cooldownTimer);
        tag.putBoolean("SalvoActive", isSalvoActive);
        tag.putInt("SalvoCounter", salvoCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cooldownTimer = tag.getInt("Cooldown");
        isSalvoActive = tag.getBoolean("SalvoActive");
        salvoCounter = tag.getInt("SalvoCounter");
    }

    private record TargetScore(Monster entity, double score) {}
}