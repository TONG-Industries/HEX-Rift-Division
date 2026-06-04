package com.trd.explosion.logic;

import com.trd.block.basic.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ExplosionHydrogen {

    // ========== НАСТРОЙКИ ==========
    public static float CENTER_EXPLOSION_RADIUS = 20.0f;
    public static float CENTER_EXPLOSION_STRENGTH = 20.0f;
    public static int RAY_COUNT = 8000;
    public static float MAX_RANGE = 50.0f;
    public static float MAX_PENETRATION = 50.0f;
    public static float VERTICAL_PENALTY = 0.75f;
    public static float BRANCH_CHANCE = 0.40f;
    public static float BRANCH_ANGLE = (float) Math.toRadians(45);
    public static float BRANCH_RANGE_MULTIPLIER = 0.50f;
    public static float BRANCH_PENETRATION_MULTIPLIER = 0.50f;
    public static float BASALT_EDGE_CHANCE = 0.0f;

    private static final Random RANDOM = new Random();

    public static void explode(ServerLevel level, Vec3 center, Entity source) {
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                6.0F, 0.4F);

        boolean destroyedAnyBlock = performCentralExplosion(level, center, source);

        if (destroyedAnyBlock) {
            coverDestroyedEdgesWithBasalt(level, center, CENTER_EXPLOSION_RADIUS);
        }

        scheduleRays(level, center, source);
    }

    private static boolean performCentralExplosion(ServerLevel level, Vec3 center, Entity source) {
        BlockPos centerPos = new BlockPos((int) center.x, (int) center.y, (int) center.z);
        Set<BlockPos> testPositions = new HashSet<>();
        testPositions.add(centerPos);
        int r = (int) (CENTER_EXPLOSION_RADIUS * 0.8);
        Random rand = new Random();
        for (int i = 0; i < 12; i++) {
            int dx = rand.nextInt(r * 2 + 1) - r;
            int dz = rand.nextInt(r * 2 + 1) - r;
            int dy = rand.nextInt(r * 2 + 1) - r;
            testPositions.add(centerPos.offset(dx, dy, dz));
        }
        Map<BlockPos, BlockState> before = new HashMap<>();
        for (BlockPos p : testPositions) {
            before.put(p, level.getBlockState(p));
        }

        level.explode(source, center.x, center.y, center.z,
                CENTER_EXPLOSION_RADIUS, false, Level.ExplosionInteraction.TNT);

        for (BlockPos p : testPositions) {
            BlockState after = level.getBlockState(p);
            BlockState prev = before.get(p);
            if (!after.equals(prev) && (after.isAir() || prev.getDestroySpeed(level, p) >= 0)) {
                return true;
            }
        }
        return false;
    }

    private static void coverDestroyedEdgesWithBasalt(ServerLevel level, Vec3 center, float radius) {
        int r = (int) Math.ceil(radius) + 1;
        BlockPos centerPos = new BlockPos((int) center.x, (int) center.y, (int) center.z);

        Set<BlockPos> airBlocks = new HashSet<>();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > radius * radius) continue;
                    BlockPos pos = new BlockPos((int) (center.x + x), (int) (center.y + y), (int) (center.z + z));
                    if (level.getBlockState(pos).isAir()) {
                        airBlocks.add(pos);
                    }
                }
            }
        }

        for (BlockPos airPos : airBlocks) {
            boolean touchesSolid = false;
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = airPos.relative(dir);
                BlockState neighborState = level.getBlockState(neighbor);
                if (!neighborState.isAir() && !neighborState.is(ModBlocks.BASALT_ROUGH.get())) {
                    touchesSolid = true;
                    break;
                }
            }
            if (touchesSolid && airPos.getY() <= centerPos.getY() + 3) {
                if (RANDOM.nextFloat() < BASALT_EDGE_CHANCE) {
                    level.setBlock(airPos, ModBlocks.BASALT_ROUGH.get().defaultBlockState(), 3);
                }
            }
        }
    }

    private static void scheduleRays(ServerLevel level, Vec3 center, Entity source) {
        List<Vec3> directions = new ArrayList<>();
        for (int i = 0; i < RAY_COUNT; i++) {
            directions.add(randomDirection());
        }
        processRayBatch(level, center, source, directions, 0, 200);
    }

    private static void processRayBatch(ServerLevel level, Vec3 center, Entity source,
                                        List<Vec3> directions, int startIdx, int batchSize) {
        int endIdx = Math.min(startIdx + batchSize, directions.size());
        List<LivingEntity> allEntities = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(MAX_RANGE));

        for (int i = startIdx; i < endIdx; i++) {
            Vec3 dir = directions.get(i);
            double verticalFactor = Math.abs(dir.y);
            double multiplier = 1.0 - VERTICAL_PENALTY * verticalFactor;
            float penetration = (float) (MAX_PENETRATION * Math.max(0.0, multiplier));
            boolean hasBranches = RANDOM.nextFloat() < BRANCH_CHANCE;
            castRay(level, center, dir, source, allEntities, MAX_RANGE, penetration, hasBranches);
        }

        if (endIdx < directions.size()) {
            level.getServer().tell(new net.minecraft.server.TickTask(1, () ->
                    processRayBatch(level, center, source, directions, endIdx, batchSize)));
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
            BlockPos pos = new BlockPos((int) Math.floor(current.x), (int) Math.floor(current.y), (int) Math.floor(current.z));

            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().isSource() || state.getBlock() instanceof LiquidBlock) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                distance += 1.0f;
                continue;
            }

            if (!state.isAir()) {
                float hardness = state.getDestroySpeed(level, pos);
                if (hardness < 0) break;

                float cost = Math.max(1.0f, hardness);
                if (penetration >= cost) {
                    penetration -= cost;
                    // === ИСПРАВЛЕНИЕ: неполные блоки (цветы, снег, трава) просто удаляем ===
                    if (!state.isCollisionShapeFullBlock(level, pos)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        level.setBlock(pos, ModBlocks.BASALT_ROUGH.get().defaultBlockState(), 3);
                    }
                    // =======================================================================
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
                spawnBranches(level, origin, direction, source, allEntities, maxPenetration);
                canBranch = false;
            }

            distance += 1.0f;
            penetration -= 1.0f;
        }
    }

    private static void spawnBranches(ServerLevel level, Vec3 branchOrigin, Vec3 parentDir,
                                      Entity source, List<LivingEntity> allEntities, float parentPenetration) {
        float branchRange = MAX_RANGE * BRANCH_RANGE_MULTIPLIER;
        float branchPenetration = parentPenetration * BRANCH_PENETRATION_MULTIPLIER;

        for (int b = 0; b < 2; b++) {
            Vec3 branchDir = deviateDirection(parentDir, BRANCH_ANGLE);
            double verticalFactor = Math.abs(branchDir.y);
            double multiplier = 1.0 - VERTICAL_PENALTY * verticalFactor;
            float finalPenetration = branchPenetration * (float) multiplier;
            castRay(level, branchOrigin, branchDir, source, allEntities, branchRange, finalPenetration, false);
        }
    }

    private static Vec3 randomDirection() {
        double theta = RANDOM.nextDouble() * 2.0 * Math.PI;
        double phi = Math.acos(2.0 * RANDOM.nextDouble() - 1.0);
        return new Vec3(Math.sin(phi) * Math.cos(theta), Math.sin(phi) * Math.sin(theta), Math.cos(phi));
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