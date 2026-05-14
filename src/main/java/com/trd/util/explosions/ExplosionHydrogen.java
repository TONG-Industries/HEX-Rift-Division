package com.trd.util.explosions;

import com.trd.block.basic.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ExplosionHydrogen {

    private static final int RAYS = 800;
    private static final float MAX_RANGE = 100.0f;
    private static final float MAX_PENETRATION = 100.0f;
    private static final float BRANCH_CHANCE = 0.40f;
    private static final float BRANCH_ANGLE = (float) Math.toRadians(45);
    private static final float BRANCH_RANGE_MULTIPLIER = 0.50f;
    private static final float BRANCH_PENETRATION_MULTIPLIER = 0.50f;
    private static final float CENTER_EXPLOSION_RADIUS = 12.0f;
    private static final Random RANDOM = new Random();

    public static void explode(ServerLevel level, Vec3 center, Entity source) {
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                6.0F, 0.4F);

        // 1. Центральный ванильный взрыв (мощный)
        level.explode(source, center.x, center.y, center.z,
                CENTER_EXPLOSION_RADIUS, false, Level.ExplosionInteraction.TNT);

        // 2. Покрываем базальтом пустоты РЯДОМ с разрушенными блоками
        coverDestroyedEdgesWithBasalt(level, center, CENTER_EXPLOSION_RADIUS);

        // 3. Рейкаст-лучи
        List<LivingEntity> allEntities = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(MAX_RANGE));

        for (int i = 0; i < RAYS; i++) {
            Vec3 dir = randomDirection();
            boolean hasBranches = RANDOM.nextFloat() < BRANCH_CHANCE;
            castRay(level, center, dir, source, allEntities, MAX_RANGE, MAX_PENETRATION, hasBranches);
        }
    }

    /**
     * Ставит базальт только в пустотах, которые граничат с разрушенной поверхностью.
     * Не трогает воздух "в чистом поле" — только края кратера.
     */
    private static void coverDestroyedEdgesWithBasalt(ServerLevel level, Vec3 center, float radius) {
        int r = (int) Math.ceil(radius) + 1;
        BlockPos centerPos = new BlockPos((int)center.x, (int)center.y, (int)center.z);

        // Сначала собираем все воздушные блоки в радиусе
        Set<BlockPos> airBlocks = new HashSet<>();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x*x + y*y + z*z > radius*radius) continue;
                    BlockPos pos = new BlockPos((int)(center.x + x), (int)(center.y + y), (int)(center.z + z));
                    if (level.getBlockState(pos).isAir()) {
                        airBlocks.add(pos);
                    }
                }
            }
        }

        // Ставим базальт только в тех воздушных, что граничат с НЕ-воздухом (край кратера)
        for (BlockPos airPos : airBlocks) {
            boolean touchesSolid = false;

            // Проверяем 6 соседей
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = airPos.relative(dir);
                // Сосед должен быть НЕ воздухом и НЕ базальтом (чтобы не заливать всё)
                BlockState neighborState = level.getBlockState(neighbor);
                if (!neighborState.isAir() && !neighborState.is(ModBlocks.BASALT_ROUGH.get())) {
                    touchesSolid = true;
                    break;
                }
            }

            // Также проверяем: не слишком ли высоко (не выше центра + 3)
            if (touchesSolid && airPos.getY() <= centerPos.getY() + 3) {
                // С шансом 80% ставим базальт
                if (RANDOM.nextFloat() < 0.8f) {
                    level.setBlock(airPos, ModBlocks.BASALT_ROUGH.get().defaultBlockState(), 3);
                }
            }
        }
    }

    private static void castRay(ServerLevel level, Vec3 origin, Vec3 direction, Entity source,
                                List<LivingEntity> allEntities, float maxRange, float maxPenetration,
                                boolean canBranch) {
        float penetration = maxPenetration;
        float distance = 0;
        Set<Integer> hitIds = new HashSet<>();

        while (distance < maxRange && penetration > 0) {
            Vec3 current = origin.add(direction.scale(distance));
            BlockPos pos = new BlockPos(
                    (int) Math.floor(current.x),
                    (int) Math.floor(current.y),
                    (int) Math.floor(current.z)
            );

            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                float hardness = state.getDestroySpeed(level, pos);
                if (hardness < 0) break;

                float cost = Math.max(1.0f, hardness);
                if (penetration >= cost) {
                    penetration -= cost;
                    level.setBlock(pos, ModBlocks.BASALT_ROUGH.get().defaultBlockState(), 3);
                } else {
                    break;
                }
            }

            for (LivingEntity entity : allEntities) {
                if (entity == source || hitIds.contains(entity.getId())) continue;
                if (entity.distanceToSqr(current.x, current.y, current.z) < 2.25) {
                    float damage = penetration * 0.5f;
                    entity.hurt(level.damageSources().explosion(source, source), damage);
                    hitIds.add(entity.getId());
                }
            }

            if (canBranch && distance < 1.0f && distance + 1.0f >= 1.0f) {
                spawnBranches(level, origin, direction, source, allEntities);
                canBranch = false;
            }

            distance += 1.0f;
            penetration -= 1.0f;
        }
    }

    private static void spawnBranches(ServerLevel level, Vec3 branchOrigin, Vec3 parentDir,
                                      Entity source, List<LivingEntity> allEntities) {
        float branchRange = MAX_RANGE * BRANCH_RANGE_MULTIPLIER;
        float branchPenetration = MAX_PENETRATION * BRANCH_PENETRATION_MULTIPLIER;

        for (int b = 0; b < 2; b++) {
            Vec3 branchDir = deviateDirection(parentDir, BRANCH_ANGLE);
            castRay(level, branchOrigin, branchDir, source, allEntities, branchRange, branchPenetration, false);
        }
    }

    private static Vec3 randomDirection() {
        double theta = RANDOM.nextDouble() * 2.0 * Math.PI;
        double phi = Math.acos(2.0 * RANDOM.nextDouble() - 1.0);
        return new Vec3(
                Math.sin(phi) * Math.cos(theta),
                Math.sin(phi) * Math.sin(theta),
                Math.cos(phi)
        );
    }

    private static Vec3 deviateDirection(Vec3 original, double maxAngle) {
        double theta = RANDOM.nextDouble() * 2.0 * Math.PI;
        double phi = RANDOM.nextDouble() * maxAngle;

        Vec3 arbitrary = Math.abs(original.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 xAxis = original.cross(arbitrary).normalize();
        if (xAxis.lengthSqr() < 0.001) {
            xAxis = new Vec3(1, 0, 0);
        }
        Vec3 yAxis = original.cross(xAxis).normalize();

        double sinPhi = Math.sin(phi);
        double cosPhi = Math.cos(phi);

        Vec3 deviated = original.scale(cosPhi)
                .add(xAxis.scale(Math.cos(theta) * sinPhi))
                .add(yAxis.scale(Math.sin(theta) * sinPhi));

        return deviated.normalize();
    }
}