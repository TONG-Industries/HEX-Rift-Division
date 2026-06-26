package com.trd.entity.mobs.depth_worm;

import com.trd.api.hive.HiveNetwork;
import com.trd.api.hive.HiveNetworkManager;
import com.trd.api.hive.HiveNetworkMember;
import com.trd.block.basic.ModBlocks;
import com.trd.block.entity.hive.DepthWormNestBlockEntity;
import com.trd.block.entity.hive.HiveSoilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

public class ReturnToHiveGoal extends Goal {
    private final DepthWormEntity worm;
    private BlockPos targetPos;
    private int nextSearchTick;
    private boolean targetIsSoil = false;
    private int stuckTicks = 0;
    private BlockPos lastPos = BlockPos.ZERO;
    private static final int STUCK_THRESHOLD = 40;

    private enum ApproachPhase { NAVIGATING, SLIDING, ENTERING }
    private ApproachPhase phase = ApproachPhase.NAVIGATING;

    // ⭐ НОВОЕ: маршрутизатор
    private boolean routerActive = false;
    private BlockPos routerTarget = null;
    private static final double ROUTER_DISABLE_DISTANCE_SQ = 256.0; // 16 blocks
    private static final double ROUTER_ARRIVE_DISTANCE_SQ = 4.0;      // 2 blocks

    public ReturnToHiveGoal(DepthWormEntity worm) {
        this.worm = worm;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (worm.isRetreating()) {
            if (worm.getTarget() != null) worm.setTarget(null);

            BlockPos boundNest = worm.getBoundNestPos();
            if (boundNest != null && isValidEntryPoint(boundNest)) {
                this.targetPos = boundNest;
                this.targetIsSoil = isSoil(boundNest);
                return true;
            }

            BlockPos entry = findNearestEntryPoint();
            if (entry != null) {
                this.targetPos = entry;
                this.targetIsSoil = isSoil(entry);
                worm.bindToNest(findNearestNest(entry));
                return true;
            }
            return false;
        }

        LivingEntity target = worm.getTarget();
        if (target != null && target.isAlive()) return false;
        if (worm.tickCount < nextSearchTick) return false;

        nextSearchTick = worm.tickCount + 10 + worm.getRandom().nextInt(10);

        BlockPos boundNest = worm.getBoundNestPos();
        if (boundNest != null && isValidEntryPoint(boundNest)) {
            this.targetPos = boundNest;
            this.targetIsSoil = isSoil(boundNest);
            return true;
        }

        BlockPos entry = findNearestEntryPoint();
        if (entry != null) {
            this.targetPos = entry;
            this.targetIsSoil = isSoil(entry);
            worm.bindToNest(findNearestNest(entry));
            return true;
        }
        return false;
    }

    private boolean isValidEntryPoint(BlockPos pos) {
        BlockEntity be = worm.level().getBlockEntity(pos);
        if (be instanceof DepthWormNestBlockEntity nest) return !nest.isFull() && nest.getNetworkId() != null;
        if (be instanceof HiveSoilBlockEntity soil) {
            UUID netId = soil.getNetworkId();
            if (netId == null) return false;
            HiveNetworkManager manager = HiveNetworkManager.get(worm.level());
            if (manager != null) {
                HiveNetwork network = manager.getNetwork(netId);
                return network != null && !network.wormCounts.isEmpty();
            }
        }
        return false;
    }

    private boolean isSoil(BlockPos pos) {
        return worm.level().getBlockState(pos).is(ModBlocks.HIVE_SOIL.get());
    }

    private BlockPos findNearestNest(BlockPos entryPos) {
        HiveNetworkManager manager = HiveNetworkManager.get(worm.level());
        if (manager == null) return entryPos;
        BlockEntity be = worm.level().getBlockEntity(entryPos);
        if (!(be instanceof HiveNetworkMember member) || member.getNetworkId() == null) return entryPos;

        HiveNetwork network = manager.getNetwork(member.getNetworkId());
        if (network == null) return entryPos;

        BlockPos nearest = null;
        double minDist = Double.MAX_VALUE;
        for (BlockPos nestPos : network.wormCounts.keySet()) {
            double dist = entryPos.distSqr(nestPos);
            if (dist < minDist) { minDist = dist; nearest = nestPos; }
        }
        return nearest != null ? nearest : entryPos;
    }

    private BlockPos findNearestEntryPoint() {
        BlockPos wormPos = worm.blockPosition();
        BlockPos bestEntry = null;
        double bestDist = Double.MAX_VALUE;
        int radius = 20;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = wormPos.offset(x, y, z);
                    BlockEntity be = worm.level().getBlockEntity(p);
                    if (be instanceof DepthWormNestBlockEntity nest && !nest.isFull()) {
                        double d = worm.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                        if (d < bestDist) { bestDist = d; bestEntry = p.immutable(); }
                    }
                }
            }
        }

        if (bestEntry == null) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -8; y <= 8; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos p = wormPos.offset(x, y, z);
                        if (isValidSoilEntry(p)) {
                            double d = worm.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                            if (d < bestDist * 1.5) { bestDist = d; bestEntry = p.immutable(); }
                        }
                    }
                }
            }
        }
        return bestEntry;
    }

    private boolean isValidSoilEntry(BlockPos pos) {
        BlockEntity be = worm.level().getBlockEntity(pos);
        if (!(be instanceof HiveSoilBlockEntity soil) || soil.getNetworkId() == null) return false;
        HiveNetworkManager manager = HiveNetworkManager.get(worm.level());
        if (manager == null) return false;
        HiveNetwork network = manager.getNetwork(soil.getNetworkId());
        return network != null && !network.wormCounts.isEmpty();
    }

    @Override
    public void start() {
        this.phase = ApproachPhase.NAVIGATING;
        this.stuckTicks = 0;
        this.lastPos = worm.blockPosition();
        this.routerActive = false;
        this.routerTarget = null;
    }

    @Override
    public void tick() {
        if (worm.isRetreating() && worm.getTarget() != null) {
            worm.setTarget(null);
        }

        if (targetPos == null) return;

        double targetX = targetPos.getX() + 0.5;
        double targetY = targetPos.getY() + (targetIsSoil ? 0.5 : 0.8);
        double targetZ = targetPos.getZ() + 0.5;

        Vec3 wormPos = worm.position();
        double distSq = wormPos.distanceToSqr(targetX, targetY, targetZ);
        BlockPos currentBlockPos = worm.blockPosition();

        // ⭐ Если гнездо в зоне видимости (16 блоков) — отключаем маршрутизатор
        if (routerActive && distSq < ROUTER_DISABLE_DISTANCE_SQ) {
            routerActive = false;
            routerTarget = null;
            phase = ApproachPhase.NAVIGATING;
        }

        if (currentBlockPos.equals(lastPos)) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPos = currentBlockPos;
        }

        if (distSq < 1.5) {
            phase = ApproachPhase.ENTERING;
        } else if (distSq < 8.0) {
            phase = ApproachPhase.SLIDING;
        } else {
            phase = ApproachPhase.NAVIGATING;
        }

        switch (phase) {
            case NAVIGATING -> {
                boolean pathFound;
                if (routerActive && routerTarget != null) {
                    // Идём на точку маршрутизатора
                    pathFound = worm.getNavigation().moveTo(routerTarget.getX() + 0.5, routerTarget.getY() + 0.5, routerTarget.getZ() + 0.5, 1.2D);

                    // ⭐ Достигли точки маршрутизатора — отключаем и пробуем найти путь до гнезда
                    double routerDistSq = worm.distanceToSqr(routerTarget.getX() + 0.5, routerTarget.getY() + 0.5, routerTarget.getZ() + 0.5);
                    if (routerDistSq < ROUTER_ARRIVE_DISTANCE_SQ) {
                        routerActive = false;
                        routerTarget = null;
                        pathFound = worm.getNavigation().moveTo(targetX, targetY, targetZ, 1.2D);
                    }
                } else {
                    pathFound = worm.getNavigation().moveTo(targetX, targetY, targetZ, 1.2D);

                    // ⭐ Путь до гнезда не найден — активируем маршрутизатор
                    if (!pathFound) {
                        BlockPos lastExit = worm.getLastExitPos();
                        if (lastExit != null && !lastExit.equals(targetPos)) {
                            routerActive = true;
                            routerTarget = lastExit;
                            System.out.println("[Worm] Router activated to last exit " + lastExit + " (cannot reach nest at " + targetPos + ")");
                            worm.getNavigation().moveTo(routerTarget.getX() + 0.5, routerTarget.getY() + 0.5, routerTarget.getZ() + 0.5, 1.2D);
                        }
                    }
                }
                worm.getLookControl().setLookAt(targetX, targetY + 0.5, targetZ);
            }

            case SLIDING -> {
                worm.getNavigation().stop();

                Vec3 toTarget = new Vec3(targetX - wormPos.x, targetY - wormPos.y, targetZ - wormPos.z);
                double dist = Math.sqrt(distSq);

                double speed = Math.min(0.15, dist * 0.03);

                Vec3 move = toTarget.normalize().scale(speed);

                worm.setPos(wormPos.x + move.x, wormPos.y + move.y, wormPos.z + move.z);
                worm.setDeltaMovement(Vec3.ZERO);

                worm.getLookControl().setLookAt(targetX, targetY, targetZ, 30.0F, 30.0F);
            }

            case ENTERING -> {
                worm.getNavigation().stop();
                worm.setDeltaMovement(Vec3.ZERO);

                if (stuckTicks > 15 || distSq < 0.8) {
                    enterNetwork(targetPos);
                }
            }
        }
    }

    private void enterNetwork(BlockPos entryPos) {
        HiveNetworkManager manager = HiveNetworkManager.get(worm.level());
        if (manager == null) return;

        BlockEntity be = worm.level().getBlockEntity(entryPos);
        UUID netId = (be instanceof HiveNetworkMember member) ? member.getNetworkId() : null;

        if (netId == null) {
            BlockEntity be2 = worm.level().getBlockEntity(worm.blockPosition());
            if (be2 instanceof HiveNetworkMember member2) {
                netId = member2.getNetworkId();
            }
        }

        if (netId == null) return;

        HiveNetwork network = manager.getNetwork(netId);
        if (network == null) return;

        int kills = worm.getKills();
        if (kills > 0) network.addPoints(kills, worm.level());

        BlockPos boundNest = worm.getBoundNestPos();
        BlockPos actualNest = (boundNest == null || targetIsSoil) ? findNearestNest(entryPos) : boundNest;

        CompoundTag tag = new CompoundTag();
        worm.saveWithoutId(tag);
        tag.putString("id", net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(worm.getType()).toString());
        tag.putInt("Kills", 0);

        boolean success = manager.addWormToNetwork(netId, tag, entryPos, worm.level());

        if (success) {
            worm.setRetreating(false);
            worm.setKills(0);
            network.removeActiveWorm();
            worm.discard();
        } else {
            this.targetPos = null;
            this.stuckTicks = 0;
            this.phase = ApproachPhase.NAVIGATING;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPos == null) return false;
        if (worm.isRetreating()) {
            // продолжаем отступление
        } else if (worm.getTarget() != null && worm.getTarget().isAlive()) {
            return false;
        }
        if (stuckTicks > STUCK_THRESHOLD * 3) return false;

        return isValidEntryPoint(targetPos);
    }

    @Override
    public void stop() {
        this.targetPos = null;
        this.targetIsSoil = false;
        this.stuckTicks = 0;
        this.lastPos = BlockPos.ZERO;
        this.phase = ApproachPhase.NAVIGATING;
        this.routerActive = false;
        this.routerTarget = null;
        worm.getNavigation().stop();
    }
}