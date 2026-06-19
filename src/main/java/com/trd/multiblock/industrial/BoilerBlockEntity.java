package com.trd.multiblock.industrial;

import com.trd.api.fluids.ModFluids;
import com.trd.block.entity.ModBlockEntities;
import com.trd.multiblock.system.PartRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BoilerBlockEntity extends BlockEntity {
    public static final float BOILING_POINT = 100.0f;
    public static final float MAX_TEMP = 1500.0f;
    public static final float HEAT_COST_PER_MB = 0.5f;
    public static final int STEAM_MULTIPLIER = 1;

    private float temperature = 20.0f;

    private final FluidTank waterTank = new FluidTank(16000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final FluidTank steamTank = new FluidTank(64000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ModFluids.STEAM_SOURCE.get();
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final LazyOptional<IFluidHandler> waterHandler = LazyOptional.of(() -> new IFluidHandler() {
        @Override
        public int getTanks() { return waterTank.getTanks(); }
        @Override
        public @NotNull FluidStack getFluidInTank(int tank) { return waterTank.getFluidInTank(tank); }
        @Override
        public int getTankCapacity(int tank) { return waterTank.getTankCapacity(tank); }
        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return waterTank.isFluidValid(tank, stack); }
        @Override
        public int fill(FluidStack resource, FluidAction action) { return waterTank.fill(resource, action); }
        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    });

    private final LazyOptional<IFluidHandler> steamHandler = LazyOptional.of(() -> new IFluidHandler() {
        @Override
        public int getTanks() { return steamTank.getTanks(); }
        @Override
        public @NotNull FluidStack getFluidInTank(int tank) { return steamTank.getFluidInTank(tank); }
        @Override
        public int getTankCapacity(int tank) { return steamTank.getTankCapacity(tank); }
        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return steamTank.isFluidValid(tank, stack); }
        @Override
        public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return steamTank.drain(resource, action); }
        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return steamTank.drain(maxDrain, action); }
    });

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity be) {
        boolean changed = false;

        // 1. Охлаждение
        if (be.temperature > 20.0f) {
            be.temperature = Math.max(20.0f, be.temperature - (be.temperature * be.temperature / 600000f));
            changed = true;
        }

        // 2. Нагрев от HeaterBlockEntity снизу
        BlockEntity belowEntity = level.getBlockEntity(pos.below());
        if (belowEntity instanceof HeaterBlockEntity heater) {
            float heaterTemp = heater.getTemperature();
            if (heaterTemp > be.temperature) {
                // Передаем часть тепла (например, 10% от разницы)
                float transfer = (heaterTemp - be.temperature) * 0.1f;
                be.temperature += transfer;
                changed = true;
            }
        }

        // 3. Кипение
        if (be.temperature > BOILING_POINT && !be.waterTank.isEmpty()) {
            float availableHeat = be.temperature - BOILING_POINT;
            int maxBoilAmount = (int) (availableHeat / HEAT_COST_PER_MB);

            if (maxBoilAmount > 0) {
                int waterAvailable = be.waterTank.getFluidAmount();
                int steamSpace = (be.steamTank.getCapacity() - be.steamTank.getFluidAmount()) / STEAM_MULTIPLIER;
                
                int boilAmount = Math.min(maxBoilAmount, Math.min(waterAvailable, steamSpace));

                if (boilAmount > 0) {
                    be.waterTank.drain(boilAmount, IFluidHandler.FluidAction.EXECUTE);
                    be.steamTank.fill(new FluidStack(ModFluids.STEAM_SOURCE.get(), boilAmount * STEAM_MULTIPLIER), IFluidHandler.FluidAction.EXECUTE);
                    be.temperature -= boilAmount * HEAT_COST_PER_MB;
                    changed = true;
                }
            }
        }

        // 4. Взрыв при превышении MAX_TEMP
        if (be.temperature >= MAX_TEMP) {
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5.0f, Level.ExplosionInteraction.BLOCK);
            level.removeBlock(pos, false);
            return;
        }

        if (changed) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public @NotNull <T> LazyOptional<T> getCapabilityForPart(@NotNull Capability<T> cap, @Nullable Direction side, PartRole role) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (role == PartRole.FLUID_INPUT) {
                return waterHandler.cast();
            } else if (role == PartRole.FLUID_OUTPUT) {
                return steamHandler.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        waterHandler.invalidate();
        steamHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("WaterTank", waterTank.writeToNBT(new CompoundTag()));
        tag.put("SteamTank", steamTank.writeToNBT(new CompoundTag()));
        tag.putFloat("Temperature", temperature);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        waterTank.readFromNBT(tag.getCompound("WaterTank"));
        steamTank.readFromNBT(tag.getCompound("SteamTank"));
        temperature = tag.getFloat("Temperature");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("WaterTank", waterTank.writeToNBT(new CompoundTag()));
        tag.put("SteamTank", steamTank.writeToNBT(new CompoundTag()));
        tag.putFloat("Temperature", temperature);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        waterTank.readFromNBT(tag.getCompound("WaterTank"));
        steamTank.readFromNBT(tag.getCompound("SteamTank"));
        temperature = tag.getFloat("Temperature");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    public FluidTank getWaterTank() { return waterTank; }
    public FluidTank getSteamTank() { return steamTank; }
    public float getTemperature() { return temperature; }
}
