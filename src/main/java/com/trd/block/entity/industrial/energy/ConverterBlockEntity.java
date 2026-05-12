package com.trd.block.entity.industrial.energy;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.trd.api.energy.ForgeWrapper;
import com.trd.api.energy.IEnergyProvider;
import com.trd.api.energy.IEnergyReceiver;
import com.trd.block.entity.ModBlockEntities;
import com.trd.capability.ModCapabilities;

public class ConverterBlockEntity extends EnergyNodeBlockEntity {


    // Тиры
    private static final long[] TIERS = { 1_000L, 10_000L, 50_000L, 100_000L, 1_000_000L, 100_000_000L, (long)Integer.MAX_VALUE };
    private int tierIndex = 2;
    private long currentLimit = TIERS[tierIndex];

    // Режимы: 0=Bi, 1=Export(H->F), 2=Import(F->H)
    // Используем поле 'mode' из базового класса

    public ConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVERTER_BE.get(), pos, state);
        this.capacity = currentLimit;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
    }

    // --- Управление ---
    public void cycleLimit() {
        tierIndex = (tierIndex + 1) % TIERS.length;
        currentLimit = TIERS[tierIndex];
        this.capacity = currentLimit;
        setChanged();
    }

    public void cycleMode() {
        mode = (mode + 1) % 3;
        setChanged();
    }

    public String getModeName() {
        return switch (mode) {
            case 1 -> "HBM -> FE (Export Only)";
            case 2 -> "FE -> HBM (Import Only)";
            default -> "Bi-Directional";
        };
    }
    public long getCurrentLimit() { return currentLimit; }

    // --- Логика ---
    public static void serverTick(Level level, BlockPos pos, BlockState state, ConverterBlockEntity be) {
        if (be.energy <= 0) return;
        if (be.mode == 2) return; // Импорт онли

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                    if (storage.canReceive()) {
                        long canExtract = Math.min(be.energy, be.currentLimit);
                        int toPush = (int) Math.min(canExtract, Integer.MAX_VALUE);
                        int accepted = storage.receiveEnergy(toPush, false);
                        if (accepted > 0) {
                            be.energy -= accepted;
                            be.setChanged();
                        }
                    }
                });
            }
        }
    }

    @Override public long getProvideSpeed() { return currentLimit; }
    @Override public long getReceiveSpeed() { return currentLimit; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("tierIndex", tierIndex);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("tierIndex")) {
            tierIndex = tag.getInt("tierIndex");
            if (tierIndex >= 0 && tierIndex < TIERS.length) currentLimit = TIERS[tierIndex];
        }
    }
}