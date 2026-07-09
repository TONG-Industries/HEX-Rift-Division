package com.trd.multiblock.industrial;

import com.trd.api.rotation.KineticNetwork;
import com.trd.api.rotation.KineticNetworkManager;
import com.trd.api.rotation.Rotational;
import com.trd.block.entity.ModBlockEntities;
import com.trd.block.entity.industrial.rotation.KineticNodeBlockEntity;
import com.trd.multiblock.system.PartRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SteamEngineBlockEntity extends KineticNodeBlockEntity {

    public final FluidTank steamTank = new FluidTank(16_000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == com.trd.api.fluids.ModFluids.STEAM_SOURCE.get();
        }
    };
    public final FluidTank lowPressureSteamTank = new FluidTank(64_000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == com.trd.api.fluids.ModFluids.LOW_PRESSURE_STEAM_SOURCE.get();
        }
    };

    private final LazyOptional<IFluidHandler> steamCap = LazyOptional.of(() -> new IFluidHandler() {
        @Override
        public int getTanks() { return steamTank.getTanks(); }
        @Override
        public @NotNull FluidStack getFluidInTank(int tank) { return steamTank.getFluidInTank(tank); }
        @Override
        public int getTankCapacity(int tank) { return steamTank.getTankCapacity(tank); }
        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return steamTank.isFluidValid(tank, stack); }
        @Override
        public int fill(FluidStack resource, FluidAction action) { return steamTank.fill(resource, action); }
        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    });

    private final LazyOptional<IFluidHandler> lowPressureCap = LazyOptional.of(() -> new IFluidHandler() {
        @Override
        public int getTanks() { return lowPressureSteamTank.getTanks(); }
        @Override
        public @NotNull FluidStack getFluidInTank(int tank) { return lowPressureSteamTank.getFluidInTank(tank); }
        @Override
        public int getTankCapacity(int tank) { return lowPressureSteamTank.getTankCapacity(tank); }
        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return lowPressureSteamTank.isFluidValid(tank, stack); }
        @Override
        public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return lowPressureSteamTank.drain(resource, action); }
        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return lowPressureSteamTank.drain(maxDrain, action); }
    });

    private long currentSpeed = 0;
    private long currentTorque = 0;
    private boolean isGenerating = false;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE_BE.get(), pos, state);
    }

    public <T> LazyOptional<T> getCapabilityForPart(Capability<T> cap, @Nullable Direction side, PartRole role) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            Direction facing = this.getBlockState().getValue(SteamEngineBlock.FACING);

            if (role == PartRole.FLUID_OUTPUT) {
                // Ограничение: подключать трубы можно ТОЛЬКО по оси парового двигателя (вдоль направления facing)
                if (side == null || side.getAxis() == facing.getAxis()) {
                    return lowPressureCap.cast();
                }
            } else if (role == PartRole.FLUID_INPUT) {
                // Ограничение: подключать трубы можно ТОЛЬКО перпендикулярно оси (сбоку)
                if (side == null || (side.getAxis() != facing.getAxis() && side.getAxis() != Direction.Axis.Y)) {
                    return steamCap.cast();
                }
            }
        }
        return LazyOptional.empty();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        steamCap.invalidate();
        lowPressureCap.invalidate();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick((ServerLevel) level);
        }
    }

    private void serverTick(ServerLevel level) {
        int maxConvert = 100; // 2k/s is 100 per tick (20 ticks per second)
        int availableToConvert = Math.min(steamTank.getFluidAmount(), lowPressureSteamTank.getSpace());
        int converted = Math.min(availableToConvert, maxConvert);

        if (converted > 0 && steamTank.getFluidAmount() > 0 && lowPressureSteamTank.getSpace() > 0) {
            FluidStack drained = steamTank.drain(converted, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                int amount = drained.getAmount();
                FluidStack lowPressure = new FluidStack(com.trd.api.fluids.ModFluids.LOW_PRESSURE_STEAM_SOURCE.get(), amount);
                lowPressureSteamTank.fill(lowPressure, IFluidHandler.FluidAction.EXECUTE);

                long targetSpeed = Math.min(amount * 10L, 750L);
                long targetTorque = Math.min(amount * 2L, 80L);

                if (currentSpeed != targetSpeed || currentTorque != targetTorque || !isGenerating) {
                    currentSpeed = targetSpeed;
                    currentTorque = targetTorque;
                    isGenerating = true;
                    requestKineticRecalculation();
                    setChanged();
                }
            }
        } else {
            if (isGenerating) {
                currentSpeed = 0;
                currentTorque = 0;
                isGenerating = false;
                requestKineticRecalculation();
                setChanged();
                this.speed = 0;
                this.lastSyncedSpeed = 0;
            }
        }

        // Sync for rendering
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        setChanged();
    }

    private void requestKineticRecalculation() {
        if (level instanceof ServerLevel serverLevel) {
            KineticNetwork net = KineticNetworkManager.get(serverLevel).getNetworkFor(worldPosition);
            if (net != null) net.requestRecalculation();
        }
    }

    @Override
    public long getGeneratedSpeed() {
        return isGenerating ? currentSpeed : 0;
    }

    @Override
    public long getVisualSpeed() {
        if (!isGenerating) return 0;
        BlockState state = getBlockState();
        if (!state.hasProperty(SteamEngineBlock.FACING)) return 0;
        Direction facing = state.getValue(SteamEngineBlock.FACING);
        if (facing == Direction.SOUTH || facing == Direction.EAST || facing == Direction.UP) {
            return -this.speed;
        }
        return this.speed;
    }

    @Override
    public long getTorque() {
        return isGenerating ? currentTorque : 0;
    }

    @Override
    public boolean isSource() {
        return true;
    }

    @Override
    public double getInertiaContribution() {
        return 100.0;
    }

    @Override
    public long getMaxTorqueTolerance() {
        return getMaxTorque();
    }

    @Override
    public long getMaxSpeed() {
        return 16000;
    }

    @Override
    public long getMaxTorque() {
        return 8000;
    }

    @Override
    public boolean canConnectMechanically(BlockPos myPos, BlockPos neighborPos, Rotational neighbor) {
        return true;
    }

    @Override
    public Direction[] getPropagationDirections() {
        BlockState state = getBlockState();
        if (!state.hasProperty(SteamEngineBlock.FACING)) return new Direction[0];
        Direction facing = state.getValue(SteamEngineBlock.FACING);
        Direction.Axis crankshaftAxis = facing.getAxis();
        return new Direction[] {
            Direction.get(Direction.AxisDirection.POSITIVE, crankshaftAxis),
            Direction.get(Direction.AxisDirection.NEGATIVE, crankshaftAxis)
        };
    }

    @Override
    public java.util.List<BlockPos> getPotentialConnections(Level lvl, BlockPos myPos) {
        Direction[] dirs = getPropagationDirections();
        return java.util.List.of(myPos.relative(dirs[0]), myPos.relative(dirs[1]));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            KineticNetwork net = KineticNetworkManager
                    .get((ServerLevel) level)
                    .getNetworkFor(worldPosition);
            if (net != null) {
                this.speed = net.getSpeed();
                this.lastSyncedSpeed = this.speed;
                net.requestRecalculation();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("SteamTank", steamTank.writeToNBT(new CompoundTag()));
        tag.put("LowPressureSteamTank", lowPressureSteamTank.writeToNBT(new CompoundTag()));
        tag.putLong("CurrentSpeed", currentSpeed);
        tag.putLong("CurrentTorque", currentTorque);
        tag.putBoolean("IsGenerating", isGenerating);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("SteamTank")) steamTank.readFromNBT(tag.getCompound("SteamTank"));
        if (tag.contains("LowPressureSteamTank")) lowPressureSteamTank.readFromNBT(tag.getCompound("LowPressureSteamTank"));
        currentSpeed = tag.getLong("CurrentSpeed");
        currentTorque = tag.getLong("CurrentTorque");
        isGenerating = tag.getBoolean("IsGenerating");
    }
}
