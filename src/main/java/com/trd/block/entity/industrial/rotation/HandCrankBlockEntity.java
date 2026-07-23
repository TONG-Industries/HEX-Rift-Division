package com.trd.block.entity.industrial.rotation;

import com.trd.api.rotation.KineticNetwork;
import com.trd.api.rotation.KineticNetworkManager;
import com.trd.api.rotation.Rotational;
import com.trd.api.rotation.ShaftDiameter;
import com.trd.block.basic.industrial.rotation.HandCrankBlock;
import com.trd.block.basic.industrial.rotation.ShaftBlock;
import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class HandCrankBlockEntity extends KineticNodeBlockEntity {

    public static final int MAX_RPM = 64;
    public static final long MAX_TORQUE = 5L;

    private int scrollBuffer = 0;
    private int idleTicks = 0;

    public HandCrankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HAND_CRANK_BE.get(), pos, state);
    }

    public void addScroll(int delta) {
        if (delta == 0) return;
        
        int amount = delta > 0 ? 16 : -16;
        this.scrollBuffer += amount;
        
        this.scrollBuffer = Math.max(-MAX_RPM, Math.min(MAX_RPM, this.scrollBuffer));
        
        this.idleTicks = 0;
        setChanged();
        requestKineticRecalculation();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HandCrankBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick((ServerLevel) level);
        }
    }

    private void serverTick(ServerLevel serverLevel) {
        if (scrollBuffer != 0) {
            idleTicks++;
            if (idleTicks > 5) { // 0.25 seconds idle before it starts decaying
                int oldBuffer = scrollBuffer;
                if (scrollBuffer > 0) {
                    scrollBuffer = Math.max(0, scrollBuffer - 4);
                } else {
                    scrollBuffer = Math.min(0, scrollBuffer + 4);
                }
                
                if (oldBuffer != scrollBuffer) {
                    setChanged();
                    requestKineticRecalculation();
                    serverLevel.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                }
            }
        }
    }

    private void requestKineticRecalculation() {
        if (level instanceof ServerLevel serverLevel) {
            KineticNetwork net = KineticNetworkManager.get(serverLevel).getNetworkFor(worldPosition);
            if (net != null) net.requestRecalculation();
        }
    }

    // ===================== Rotational =====================

    @Override
    public long getGeneratedSpeed() {
        return scrollBuffer;
    }

    @Override
    public long getVisualSpeed() {
        BlockState state = getBlockState();
        if (!state.hasProperty(HandCrankBlock.FACING)) return 0;
        Direction facing = state.getValue(HandCrankBlock.FACING);
        if (facing == Direction.SOUTH || facing == Direction.EAST || facing == Direction.UP) {
            return -this.speed;
        }
        return this.speed;
    }

    @Override
    public long getTorque() {
        return scrollBuffer != 0 ? MAX_TORQUE : 0L;
    }

    @Override
    public boolean isSource() { return true; }

    @Override
    public double getInertiaContribution() { return 5.0; } // Small inertia for the crank itself

    @Override
    public long getMaxTorqueTolerance() { return 128; } // Low structural tolerance, crank can break if attached to huge load maybe? Or high so it doesn't break easily. Let's make it robust enough for 128

    @Override
    public long getMaxSpeed() { return MAX_RPM; }

    @Override
    public long getMaxTorque() { return 1024; }

    @Override
    public boolean canConnectMechanically(BlockPos myPos, BlockPos neighborPos, Rotational neighbor) {
        if (neighbor instanceof ShaftBlockEntity shaftBE) {
            if (shaftBE.getBlockState().getBlock() instanceof ShaftBlock shaftBlock) {
                return shaftBlock.getDiameter() == ShaftDiameter.LIGHT;
            }
        }
        return true;
    }

    @Override
    public Direction[] getPropagationDirections() {
        BlockState state = getBlockState();
        if (!state.hasProperty(HandCrankBlock.FACING)) return new Direction[0];
        // Connects to the side OPPOSITE to its facing (if it faces North, the handle is on North, and it connects to South)
        return new Direction[]{state.getValue(HandCrankBlock.FACING).getOpposite()};
    }

    @Override
    public List<BlockPos> getPotentialConnections(Level lvl, BlockPos myPos) {
        BlockState state = getBlockState();
        if (!state.hasProperty(HandCrankBlock.FACING)) return List.of();
        Direction opposite = state.getValue(HandCrankBlock.FACING).getOpposite();
        return List.of(myPos.relative(opposite));
    }

    // ===================== NBT =====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ScrollBuffer", scrollBuffer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        scrollBuffer = tag.getInt("ScrollBuffer");
    }

    // ===================== RENDER =====================

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.0D);
    }

    public long getCurrentVisualSpeed() {
        return this.speed;
    }
}
