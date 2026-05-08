package com.cim.block.entity.industrial.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import com.cim.api.energy.EnergyNetworkManager;
import com.cim.api.energy.IEnergyConnector;
import com.cim.block.basic.industrial.energy.SwitchBlock;
import com.cim.block.entity.ModBlockEntities;
import com.cim.capability.ModCapabilities;

import javax.annotation.Nullable;

public class SwitchBlockEntity extends BlockEntity implements IEnergyConnector {

    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
    public boolean isTriggered = false;

    public SwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWITCH_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SwitchBlockEntity entity) {
        if (level.isClientSide) return;

        if (state.getValue(SwitchBlock.POWERED)) {
            ServerLevel serverLevel = (ServerLevel) level;
            EnergyNetworkManager manager = EnergyNetworkManager.get(serverLevel);

            if (!manager.hasNode(pos)) {
                manager.addNode(pos);
            }
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.ENERGY_CONNECTOR) {
            if (isValidSide(side)) {
                return hbmConnector.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    private boolean isValidSide(@Nullable Direction side) {
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SwitchBlock)) return false;
        if (side == null) return true;

        Direction facing = state.getValue(SwitchBlock.FACING);
        if (side.getAxis() == Direction.Axis.Y) return false;
        return side != facing && side != facing.getOpposite();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        hbmConnector.invalidate();
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return isValidSide(side);
    }

    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
        hbmConnector.invalidate();
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("isTriggered", isTriggered);
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        isTriggered = tag.getBoolean("isTriggered");
    }
}