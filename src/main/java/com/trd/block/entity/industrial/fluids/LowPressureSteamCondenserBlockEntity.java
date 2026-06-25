package com.trd.block.entity.industrial.fluids;

import com.trd.api.fluids.ModFluids;
import com.trd.api.fluids.system.FluidNetworkManager;
import com.trd.block.entity.ModBlockEntities;
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
 * Конденсатор пара низкого давления. Вход: low_pressure_steam, выход: water (со всех сторон).
 * Энергию НЕ тратит: конверсия 1:1 идёт, пока есть пар и место под воду.
 */
public class LowPressureSteamCondenserBlockEntity extends BlockEntity {

    // ---- Баланс ----
    public static final int TANK_CAPACITY = 10_000;
    public static final int CONVERT_RATE  = 10; // mB пара -> воды за тик (1:1)

    private final FluidTank steamTank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ModFluids.LOW_PRESSURE_STEAM_SOURCE.get();
        }
        @Override protected void onContentsChanged() { setChanged(); sync(); }
    };

    private final FluidTank waterTank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.WATER;
        }
        @Override protected void onContentsChanged() { setChanged(); sync(); }
    };

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
            if (!steamTank.isFluidValid(resource)) return 0;
            return steamTank.fill(resource, action);
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid() != Fluids.WATER) return FluidStack.EMPTY;
            return waterTank.drain(resource, action);
        }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return waterTank.drain(maxDrain, action);
        }
    });

    public LowPressureSteamCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOW_PRESSURE_STEAM_CONDENSER_BE.get(), pos, state);
    }

    // ===== Регистрация в флюид-сети (как у FluidBarrelBlockEntity) =====
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && level instanceof ServerLevel sl) {
            FluidNetworkManager.get(sl).addNode(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && level instanceof ServerLevel sl) {
            FluidNetworkManager.get(sl).removeNode(worldPosition);
        }
    }

    // ===== Логика (без энергии) =====
    public static void serverTick(Level level, BlockPos pos, BlockState state, LowPressureSteamCondenserBlockEntity be) {
        if (level.isClientSide) return;

        int waterSpace = be.waterTank.getCapacity() - be.waterTank.getFluidAmount();
        if (be.steamTank.getFluidAmount() >= CONVERT_RATE && waterSpace >= CONVERT_RATE) {
            be.steamTank.drain(CONVERT_RATE, IFluidHandler.FluidAction.EXECUTE);
            be.waterTank.fill(new FluidStack(Fluids.WATER, CONVERT_RATE), IFluidHandler.FluidAction.EXECUTE);
            be.setChanged();
        }
    }

    // ===== Capabilities (со всех сторон) =====
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCap.invalidate();
    }

    // ===== NBT (saveAdditional public — нужно для getDrops) =====
    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("SteamTank", steamTank.writeToNBT(new CompoundTag()));
        tag.put("WaterTank", waterTank.writeToNBT(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        steamTank.readFromNBT(tag.getCompound("SteamTank"));
        waterTank.readFromNBT(tag.getCompound("WaterTank"));
    }

    // ===== Синхронизация =====
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

    // ===== Геттеры (для оверлея / тултипа) =====
    public FluidTank getSteamTank() { return steamTank; }
    public FluidTank getWaterTank() { return waterTank; }
}
