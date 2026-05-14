package com.trd.util.explosions;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ExplosionStandard {

    public static void explode(Level level, Vec3 center, Entity source, float radius, float damage) {
        if (level.isClientSide) return;

        // Ванильный взрыв с разрушением блоков и уроном сущностям
        level.explode(
                source,
                center.x, center.y, center.z,
                radius,
                false, // без огня
                Level.ExplosionInteraction.BLOCK
        );
    }
}