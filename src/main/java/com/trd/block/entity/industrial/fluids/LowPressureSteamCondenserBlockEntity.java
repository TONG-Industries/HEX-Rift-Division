package com.trd.block.entity.industrial.fluids;

import com.trd.api.energy.EnergyNetworkManager;
import com.trd.api.energy.IEnergyReceiver;
import com.trd.api.fluids.ModFluids;
import com.trd.api.fluids.system.FluidNetworkManager;
import com.trd.block.entity.ModBlockEntities;
import com.trd.capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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

/**
 * Конденсатор пара низкого давления. Вход: low_pressure_steam, выход: water (по трубам, со всех сторон).
 * Питание: энергия из EnergyNetwork (ModCapabilities.ENERGY_RECEIVER). Конверсия 1:1.
 * Регистрация узла — как у FluidBarrelBlockEntity (onLoad/setRemoved).
 */
public class LowPressureSteamCondenserBlockEntity extends BlockEntity implements IEnergyReceiver {

    // ---- Баланс (правь под себя) ----
    public static final int  TANK_CAPACITY        = 10_000;   // буфер каждого танка, mB
    public static final long ENERGY_CAPACITY      = 100_000L; // внутренний буфер энергии
    public static final long ENERGY_RECEIVE_SPEED = 2_000L;   // макс. приём энергии/тик
    public static final int  CONVERT_RATE         = 10;       // mB пара -> воды за тик (1:1)
    public static final long ENERGY_PER_TICK      = 10L;      // расход энергии за тик работы

    private long energy = 0L;

    // ВХОД: только пар низкого давления
    private final FluidTank steamTank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ModFluids.LOW_PRESSURE_STEAM_SOURCE.get();
        }
        @Override protected void onContentsChanged() { setChanged(); sync(); }
    };

    // ВЫХОД: вода
    private final FluidTank waterTank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.WATER;
        }
        @Override protected void onContentsChanged() { setChanged(); sync(); }
    };

    // Единый FLUID_HANDLER (со всех сторон): fill = только пар, drain = только вода.
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? steamTank.getFluid() : waterTank.getFluid();
        }
        @Override public int getTankCapacity(int tank) { return TANK_CAPACITY; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 ? steamTank.isFluidValid(stack) : waterTank.isFluidValid(stack);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!steamTank.isFluidValid(resource)) return 0; // принимаем только пар
            return steamTank.fill(resource, action);
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid() != Fluids.WATER) return FluidStack.EMPTY; // отдаём только воду
            return waterTank.drain(resource, action);
        }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return waterTank.drain(maxDrain, action);
        }
    });

    private final LazyOptional<IEnergyReceiver> energyCap = LazyOptional.of(() -> this);

    public LowPressureSteamCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOW_PRESSURE_STEAM_CONDENSER_BE.get(), pos, state);
    }

    // ===== Регистрация в сетях (как у FluidBarrelBlockEntity) =====
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && level instanceof ServerLevel sl) {
            FluidNetworkManager.get(sl).addNode(worldPosition);   // addNode сам защищён от дублей
            EnergyNetworkManager.get(sl).addNode(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && level instanceof ServerLevel sl) {
            FluidNetworkManager.get(sl).removeNode(worldPosition);
            EnergyNetworkManager.get(sl).removeNode(worldPosition);
        }
    }

    // ===== Главная логика =====
    public static void serverTick(Level level, BlockPos pos, BlockState state, LowPressureSteamCondenserBlockEntity be) {
        if (level.isClientSide) return;

        int waterSpace = be.waterTank.getCapacity() - be.waterTank.getFluidAmount();
        boolean canRun = be.energy >= ENERGY_PER_TICK
                && be.steamTank.getFluidAmount() >= CONVERT_RATE
                && waterSpace >= CONVERT_RATE;

        if (canRun) {
            be.steamTank.drain(CONVERT_RATE, IFluidHandler.FluidAction.EXECUTE);
            be.waterTank.fill(new FluidStack(Fluids.WATER, CONVERT_RATE), IFluidHandler.FluidAction.EXECUTE);
            be.energy -= ENERGY_PER_TICK;
            be.setChanged();
        }
    }

    // ===== Capabilities (со всех сторон) =====
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        if (cap == ModCapabilities.ENERGY_RECEIVER) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCap.invalidate();
        energyCap.invalidate();
    }

    // ===== IEnergyReceiver =====
    @Override public long getEnergyStored()    { return energy; }
    @Override public long getMaxEnergyStored() { return ENERGY_CAPACITY; }
    @Override public void setEnergyStored(long e) {
        energy = Math.max(0, Math.min(e, ENERGY_CAPACITY));
        setChanged();
    }
    @Override public long getReceiveSpeed()    { return ENERGY_RECEIVE_SPEED; }
    @Override public Priority getPriority()     { return Priority.NORMAL; }
    @Override public boolean canReceive()       { return energy < ENERGY_CAPACITY; }
    @Override public long receiveEnergy(long maxReceive, boolean simulate) {
        long received = Math.min(ENERGY_CAPACITY - energy, Math.min(ENERGY_RECEIVE_SPEED, maxReceive));
        if (!simulate && received > 0) { energy += received; setChanged(); }
        return received;
    }
    @Override public boolean canConnectEnergy(Direction side) { return true; }

    // ===== NBT (saveAdditional public — нужно для getDrops в блоке) =====
    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("SteamTank", steamTank.writeToNBT(new CompoundTag()));
        tag.put("WaterTank", waterTank.writeToNBT(new CompoundTag()));
        tag.putLong("Energy", energy);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        steamTank.readFromNBT(tag.getCompound("SteamTank"));
        waterTank.readFromNBT(tag.getCompound("WaterTank"));
        energy = tag.getLong("Energy");
    }

    // ===== Синхронизация клиента =====
    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) handleUpdateTag(pkt.getTag());
    }

    // ===== Геттеры (для GUI / тултипа) =====
    public FluidTank getSteamTank() { return steamTank; }
    public FluidTank getWaterTank() { return waterTank; }
    public long getEnergy()         { return energy; }
}
