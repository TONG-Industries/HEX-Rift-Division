package com.trd.api.rotation;

import com.trd.block.basic.industrial.rotation.ShaftBlock;
import com.trd.block.entity.industrial.rotation.ShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BeltConnectionHelper {

    public static boolean isPosBetween(BlockPos mid, BlockPos a, BlockPos b) {
        if (a.getX() == b.getX() && mid.getX() == a.getX() && a.getY() == b.getY() && mid.getY() == a.getY()) {
            return (mid.getZ() > Math.min(a.getZ(), b.getZ())) && (mid.getZ() < Math.max(a.getZ(), b.getZ()));
        }
        if (a.getX() == b.getX() && mid.getX() == a.getX() && a.getZ() == b.getZ() && mid.getZ() == a.getZ()) {
            return (mid.getY() > Math.min(a.getY(), b.getY())) && (mid.getY() < Math.max(a.getY(), b.getY()));
        }
        if (a.getY() == b.getY() && mid.getY() == a.getY() && a.getZ() == b.getZ() && mid.getZ() == a.getZ()) {
            return (mid.getX() > Math.min(a.getX(), b.getX())) && (mid.getX() < Math.max(a.getX(), b.getX()));
        }
        return false;
    }

    public static void breakBelt(Level level, BlockPos brokenPos) {
        int radius = 16;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= radius) {
                        if (dx == 0 && dy == 0 && dz == 0)
                            continue;
                        BlockPos scanPos = brokenPos.offset(dx, dy, dz);
                        if (level.isLoaded(scanPos)) {
                            BlockEntity be = level.getBlockEntity(scanPos);
                            if (be instanceof ShaftBlockEntity shaft && shaft.hasPulley()) {
                                BlockPos target = shaft.getConnectedPulley();
                                if (target != null) {
                                    if (target.equals(brokenPos)) {
                                        shaft.setConnectedPulley(null);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Вычисляет кратчайшее расстояние между двумя 3D-отрезками
    public static double distanceBetweenSegments(Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4) {
        Vec3 u = p2.subtract(p1);
        Vec3 v = p4.subtract(p3);
        Vec3 w = p1.subtract(p3);
        double a = u.dot(u);
        double b = u.dot(v);
        double c = v.dot(v);
        double d = u.dot(w);
        double e = v.dot(w);
        double D = a * c - b * b;
        double sc, sN, sD = D;
        double tc, tN, tD = D;

        if (D < 1e-8) {
            sN = 0.0;
            sD = 1.0;
            tN = e;
            tD = c;
        } else {
            sN = (b * e - c * d);
            tN = (a * e - b * d);
            if (sN < 0.0) {
                sN = 0.0;
                tN = e;
                tD = c;
            } else if (sN > sD) {
                sN = sD;
                tN = e + b;
                tD = c;
            }
        }
        if (tN < 0.0) {
            tN = 0.0;
            if (-d < 0.0)
                sN = 0.0;
            else if (-d > a)
                sN = sD;
            else {
                sN = -d;
                sD = a;
            }
        } else if (tN > tD) {
            tN = tD;
            if ((-d + b) < 0.0)
                sN = 0;
            else if ((-d + b) > a)
                sN = sD;
            else {
                sN = (-d + b);
                sD = a;
            }
        }

        sc = (Math.abs(sN) < 1e-8 ? 0.0 : sN / sD);
        tc = (Math.abs(tN) < 1e-8 ? 0.0 : tN / tD);
        Vec3 dP = w.add(u.scale(sc)).subtract(v.scale(tc));
        return dP.length();
    }

    public static InteractionResult tryConnectPulleys(Level level, Player player, BlockPos posA, BlockPos posB) {
        if (posA.equals(posB))
            return InteractionResult.FAIL;

        int dx = Integer.compare(posB.getX(), posA.getX());
        int dy = Integer.compare(posB.getY(), posA.getY());
        int dz = Integer.compare(posB.getZ(), posA.getZ());

        boolean isStraight = Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1;

        // 2. Ограничение дистанции
        if (posA.distManhattan(posB) > 16) {
            if (player != null)
                player.displayClientMessage(Component.literal("Слишком большое расстояние!"), true);
            return InteractionResult.FAIL;
        }

        // 3. Проверка коллизий (Стены)
        if (isStraight) {
            BlockPos current = posA.offset(dx, dy, dz);
            while (!current.equals(posB)) {
                BlockState state = level.getBlockState(current);
                if (!(state.getBlock() instanceof ShaftBlock)) {
                    if (state.isCollisionShapeFullBlock(level, current)
                            || !state.getCollisionShape(level, current).isEmpty()) {
                        if (player != null)
                            player.displayClientMessage(Component.literal("Путь преграждает блок!"), true);
                        return InteractionResult.FAIL;
                    }
                }
                current = current.offset(dx, dy, dz);
            }
        }

        // 4. Проверка пересечения ремней
        Vec3 vecA = Vec3.atCenterOf(posA);
        Vec3 vecB = Vec3.atCenterOf(posB);
        AABB searchBox = new AABB(posA, posB).inflate(16.0);

        for (BlockPos bp : BlockPos.betweenClosed(
                net.minecraft.util.Mth.floor(searchBox.minX), net.minecraft.util.Mth.floor(searchBox.minY),
                net.minecraft.util.Mth.floor(searchBox.minZ),
                net.minecraft.util.Mth.floor(searchBox.maxX), net.minecraft.util.Mth.floor(searchBox.maxY),
                net.minecraft.util.Mth.floor(searchBox.maxZ))) {

            if (level.isLoaded(bp)) {
                BlockEntity be = level.getBlockEntity(bp);
                if (be instanceof ShaftBlockEntity shaft && shaft.hasPulley() && shaft.getConnectedPulley() != null) {
                    BlockPos target = shaft.getConnectedPulley();

                    if (!bp.equals(posA) && !bp.equals(posB) && !target.equals(posA) && !target.equals(posB)) {
                        Vec3 vecC = Vec3.atCenterOf(bp);
                        Vec3 vecD = Vec3.atCenterOf(target);

                        if (distanceBetweenSegments(vecA, vecB, vecC, vecD) < 0.8) {
                            if (player != null)
                                player.displayClientMessage(Component.literal("Ремни не могут пересекаться!"), true);
                            return InteractionResult.FAIL;
                        }
                    }
                }
            }
        }

        // 5. Успешное соединение
        // Очищаем связи всех промежуточных шкивов, чтобы избежать визуальных глитчей и
        // багов графа
        if (isStraight) {
            BlockPos current = posA.offset(dx, dy, dz);
            while (!current.equals(posB)) {
                BlockEntity be = level.getBlockEntity(current);
                if (be instanceof ShaftBlockEntity shaft && shaft.hasPulley()) {
                    shaft.setConnectedPulley(null);
                }
                current = current.offset(dx, dy, dz);
            }
        }

        // Устанавливаем конечную цель для стартового шкива
        // Примечание: связь устанавливается как односторонняя (Master-Target),
        // чтобы избежать дублирования рендера и упростить логику.
        if (level.getBlockEntity(posA) instanceof ShaftBlockEntity shaftA) {
            shaftA.setConnectedPulley(posB);
        }

        return InteractionResult.SUCCESS;
    }
}
