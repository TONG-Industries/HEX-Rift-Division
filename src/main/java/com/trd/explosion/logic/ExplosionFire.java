package com.trd.explosion.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ExplosionFire {

    private static final int RAYS = 40;
    private static final float MAX_RANGE = 30.0f;
    private static final float MAX_PENETRATION = 100.0f;
    private static final Random RANDOM = new Random();

    public static void explode(ServerLevel level, Vec3 center, Entity source, float radius) {
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS,
                2.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        List<LivingEntity> allEntities = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(MAX_RANGE));

        for (int i = 0; i < RAYS; i++) {
            Vec3 dir = randomDirection();
            castFireRay(level, center, dir, source, allEntities, radius);
        }
    }

    private static void castFireRay(ServerLevel level, Vec3 origin, Vec3 direction, Entity source,
                                    List<LivingEntity> allEntities, float radius) {
        float penetration = MAX_PENETRATION;
        float distance = 0;
        Set<Integer> hitIds = new HashSet<>();

        while (distance < MAX_RANGE && penetration > 0) {
            Vec3 current = origin.add(direction.scale(distance));

            float currentRadius = radius * (1.0f - (distance / MAX_RANGE));
            currentRadius = Math.max(0.5f, currentRadius);

            int r = (int) Math.ceil(currentRadius);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            float stepCost = 0.2f; // базовое затухание за шаг
            boolean blocked = false;

            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        if (x*x + y*y + z*z > currentRadius*currentRadius) continue;

                        pos.set(current.x + x, current.y + y, current.z + z);
                        BlockState state = level.getBlockState(pos);

                        if (state.isAir()) {
                            // Воздух → огонь, минимальный расход энергии
                            level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                            stepCost += 0.1f;
                        } else if (state.isFlammable(level, pos, Direction.UP)) {
                            // Горючее → огонь, небольшой расход
                            level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                            stepCost += 0.3f;
                        } else {
                            // Негорючий твёрдый блок — препятствие для луча
                            float hardness = state.getDestroySpeed(level, pos);
                            if (hardness < 0) {
                                // Непробиваемый (бедрок и т.п.) — луч останавливается
                                blocked = true;
                                break;
                            }
                            // Значительно слабее чем у водородного взрыва
                            stepCost += Math.max(0.3f, hardness * 0.15f);
                        }
                    }
                    if (blocked) break;
                }
                if (blocked) break;
            }

            if (blocked) break;

            penetration -= stepCost;
            if (penetration <= 0) break;

            // Урон мобам в сечении
            for (LivingEntity entity : allEntities) {
                if (entity == source || hitIds.contains(entity.getId())) continue;
                double distToRay = entity.distanceToSqr(current.x, current.y, current.z);
                if (distToRay < currentRadius * currentRadius * 2) {
                    float damage = penetration * 0.8f;
                    entity.hurt(level.damageSources().explosion(source, source), damage);
                    entity.setSecondsOnFire((int)(penetration * 1.5f));
                    hitIds.add(entity.getId());
                }
            }

            distance += 1.5f;
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
}