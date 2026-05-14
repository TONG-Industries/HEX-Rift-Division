package com.trd.util.explosions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ExplosionFire {

    public static void explode(Level level, Vec3 center, Entity source, float radius, float damage) {
        if (level.isClientSide) return;

        // Ванильный взрыв с поджогом
        level.explode(
                source,
                center.x, center.y, center.z,
                radius,
                true, // с огнём
                Level.ExplosionInteraction.BLOCK
        );

        // Дополнительно покрываем огнём окрестности воронки
        if (level instanceof ServerLevel serverLevel) {
            int fireRadius = (int) (radius + 2);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int x = -fireRadius; x <= fireRadius; x++) {
                for (int y = -fireRadius; y <= fireRadius; y++) {
                    for (int z = -fireRadius; z <= fireRadius; z++) {
                        if (x * x + y * y + z * z > fireRadius * fireRadius) continue;
                        pos.set(center.x + x, center.y + y, center.z + z);

                        // Только если блок воздух или горючий — ставим огонь
                        if (serverLevel.getBlockState(pos).isAir()) {
                            serverLevel.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }
}