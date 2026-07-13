package com.trd.block.entity.industrial;

import com.trd.block.entity.industrial.rotation.KineticNodeBlockEntity;
import com.trd.block.industrial.WaterPumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.trd.block.entity.ModBlockEntities;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class WaterPumpBlockEntity extends KineticNodeBlockEntity {
    private final FluidTank waterTank = new FluidTank(4000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.WATER;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // Разрешаем заполнение только изнутри механизма
            if (action == FluidAction.EXECUTE && isInternalCall) {
                return super.fill(resource, action);
            }
            return 0; // Снаружи нельзя заливать жидкость в помпу
        }
    };
    
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> waterTank);
    
    private int cachedWaterVolume = 0;
    private int handCrankTicks = 0;
    private int lastPumpedVolume = 0; // Для HUD
    private boolean isInternalCall = false;

    public WaterPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_PUMP_BE.get(), pos, state);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, WaterPumpBlockEntity pEntity) {
        // --- Client-side Visuals ---
        if (level.isClientSide) {
            float speedC = Math.abs(pEntity.getSpeed());
            if (speedC > 0) {
                float vBase = speedC * 0.4f;
                float eff = pEntity.cachedWaterVolume < 20 ? 0.0f : (pEntity.cachedWaterVolume / 1000.0f);
                eff = Math.min(eff, 1.0f);
                if (vBase * eff > 0) {
                    pEntity.lastPumpedVolume = Math.min((int) Math.ceil(vBase * eff), 300);
                } else {
                    pEntity.lastPumpedVolume = 0;
                }
            } else {
                pEntity.lastPumpedVolume = 0;
            }
            return;
        }

        // --- Server-side Logic ---
        // Десинхронизированный таймер (раз в 600 тиков / 30 сек)
        if ((level.getGameTime() + pos.getX() + pos.getZ()) % 600 == 0) {
            pEntity.scanWaterVolume();
        }

        float speed = Math.abs(pEntity.getSpeed());
        
        // Логика ручного привода
        if (speed == 0 && pEntity.handCrankTicks > 0) {
            speed = 32.0f; // Условная скорость от ручки
            pEntity.handCrankTicks--;
        }

        if (speed > 0) {
            float vBase = speed * 0.4f;
            float eff = pEntity.cachedWaterVolume < 20 ? 0.0f : (pEntity.cachedWaterVolume / 1000.0f);
            eff = Math.min(eff, 1.0f); // Ограничиваем эффективность до 100%
            
            int v = 0;
            if (vBase * eff > 0) {
                v = Math.min((int) Math.ceil(vBase * eff), 300);
            }
            pEntity.lastPumpedVolume = v;

            if (v > 0 && pEntity.waterTank.getSpace() > 0) {
                pEntity.isInternalCall = true;
                pEntity.waterTank.fill(new FluidStack(Fluids.WATER, v), IFluidHandler.FluidAction.EXECUTE);
                pEntity.isInternalCall = false;
            }
        } else {
            pEntity.lastPumpedVolume = 0;
        }
    }

    private void scanWaterVolume() {
        BlockPos startPos = this.worldPosition.below(1);
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        
        queue.add(startPos);
        visited.add(startPos);
        
        int volume = 0;
        
        while (!queue.isEmpty() && volume < 1000) {
            BlockPos current = queue.poll();
            
            if (level.getFluidState(current).is(Fluids.WATER) || level.getFluidState(current).is(Fluids.FLOWING_WATER)) {
                volume++;
                
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        
        int oldVolume = this.cachedWaterVolume;
        this.cachedWaterVolume = volume;
        
        if (oldVolume != this.cachedWaterVolume) {
            this.setChanged();
            this.syncToClient();
        }
    }

    public void crank() {
        this.handCrankTicks = 40;
    }

    public int getLastPumpedVolume() {
        return lastPumpedVolume;
    }

    public int getCachedWaterVolume() {
        return cachedWaterVolume;
    }

    // --- Kinetic API ---

    @Override
    public long getMaxTorqueTolerance() {
        return 4096L;
    }

    @Override
    public long getMaxTorque() {
        return 4096L;
    }

    @Override
    public double getInertiaContribution() {
        return 100.0;
    }

    @Override
    public long getMaxSpeed() {
        return 4096L;
    }

    @Override
    public long getTorque() {
        return 0L;
    }

    @Override
    public boolean isSource() {
        return false;
    }

    @Override
    public long getConsumedTorque() {
        if (lastPumpedVolume > 0 && waterTank.getSpace() > 0) {
            return (long) (1 + (lastPumpedVolume * 0.05));
        }
        return 1L;
    }

    @Override
    public long getVisualSpeed() {
        BlockState state = getBlockState();
        if (!state.hasProperty(WaterPumpBlock.FACING)) return this.speed;
        Direction facing = state.getValue(WaterPumpBlock.FACING);
        if (facing == Direction.SOUTH || facing == Direction.EAST || facing == Direction.UP) {
            return -this.speed;
        }
        return this.speed;
    }

    @Override
    public Direction[] getPropagationDirections() {
        BlockState state = getBlockState();
        if (state.hasProperty(WaterPumpBlock.FACING)) {
            Direction facing = state.getValue(WaterPumpBlock.FACING);
            return new Direction[] { facing, facing.getOpposite() };
        }
        return new Direction[0];
    }

    @Override
    public java.util.List<BlockPos> getPotentialConnections(net.minecraft.world.level.Level level, BlockPos myPos) {
        java.util.List<BlockPos> list = new java.util.ArrayList<>();
        BlockState state = getBlockState();
        if (state.hasProperty(WaterPumpBlock.FACING)) {
            Direction facing = state.getValue(WaterPumpBlock.FACING);
            list.add(myPos.relative(facing));
            list.add(myPos.relative(facing.getOpposite()));
        }
        return list;
    }

    // --- Capabilities ---

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            Direction facing = this.getBlockState().getValue(WaterPumpBlock.FACING);
            Direction rightSide = facing.getCounterClockWise();
            
            if (side == rightSide) {
                return fluidHandler.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("WaterVolume", cachedWaterVolume);
        tag.putInt("HandCrankTicks", handCrankTicks);
        tag.put("WaterTank", waterTank.writeToNBT(new CompoundTag()));
        tag.putInt("LastPumpedVolume", lastPumpedVolume);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.cachedWaterVolume = tag.getInt("WaterVolume");
        this.handCrankTicks = tag.getInt("HandCrankTicks");
        this.waterTank.readFromNBT(tag.getCompound("WaterTank"));
        this.lastPumpedVolume = tag.getInt("LastPumpedVolume");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            scanWaterVolume();
        }
    }
}
