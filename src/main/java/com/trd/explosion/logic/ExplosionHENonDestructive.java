package com.trd.explosion.logic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Фугасный взрыв без разрушения блоков.
 * Наносит урон сущностям, создаёт эффекты и звуки как обычный HE,
 * но не разрушает блоки.
 */
public class ExplosionHENonDestructive {

    public static void explode(Level level, Vec3 center, Entity source, float radius, float damage) {
        if (level.isClientSide) return;

        ServerLevel serverLevel = (ServerLevel) level;

        // 1. Создаём взрыв с Mode.NONE — без разрушения блоков
        level.explode(
                source,
                center.x, center.y, center.z,
                radius,
                false,
                Level.ExplosionInteraction.NONE // НЕ разрушаем блоки
        );

        // 2. Дополнительный урон по сущностям (как у ExplosionHE)
        AABB damageBox = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );

        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, damageBox, e -> e != source && e.isAlive());

        for (LivingEntity entity : entities) {
            double distSqr = entity.distanceToSqr(center);
            double maxDistSqr = radius * radius;

            if (distSqr > maxDistSqr) continue;

            double dist = Math.sqrt(distSqr);
            double falloff = 1.0 - (dist / radius);
            falloff = Math.max(0.0, Math.min(1.0, falloff));

            float finalDamage = damage * (float) falloff;

            // Отбрасывание
            Vec3 knockbackDir = entity.position().subtract(center).normalize();
            double knockbackStrength = falloff * 2.0;
            entity.push(knockbackDir.x * knockbackStrength, knockbackDir.y * knockbackStrength + 0.5, knockbackDir.z * knockbackStrength);

            entity.hurt(level.damageSources().explosion(source, source), finalDamage);
        }
    }
}