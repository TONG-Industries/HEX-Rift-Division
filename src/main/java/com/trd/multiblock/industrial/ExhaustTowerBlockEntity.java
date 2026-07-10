package com.trd.multiblock.industrial;

import com.trd.api.fluids.ModFluids;
import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
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

/**
 * Башня отведения газов (exhaust). Мультиблок 1x1x6 (тонкая, но высокая).
 *  - Принимает дым (труба к основанию) в бак, медленно выветривает.
 *  - Когда есть дым — наверху башни выделяется много дыма (частицы).
 */
public class ExhaustTowerBlockEntity extends BlockEntity {

    public static final int SMOKE_CAPACITY = 64_000;
    public static final int VENT_RATE = 40;      // mB/tick выветривания
    public static final int TOWER_HEIGHT = 6;    // высота (вершина = pos.above(TOWER_HEIGHT - 1))

    private boolean active = false;

    private final FluidTank smokeTank = new FluidTank(SMOKE_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) { return stack.getFluid() == ModFluids.SMOKE_SOURCE.get(); }
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    };

    // Приём дыма (только заливать)
    private final LazyOptional<IFluidHandler> smokeInHandler = LazyOptional.of(() -> new IFluidHandler() {
        @Override public int getTanks() { return smokeTank.getTanks(); }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return smokeTank.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return smokeTank.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return smokeTank.isFluidValid(tank, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return smokeTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    });

    public ExhaustTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXHAUST_TOWER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ExhaustTowerBlockEntity be) {
        boolean wasActive = be.active;
        if (!be.smokeTank.isEmpty()) {
            be.smokeTank.drain(VENT_RATE, IFluidHandler.FluidAction.EXECUTE); // дым выветривается
            be.active = true;
        } else {
            be.active = false;
        }
        if (wasActive != be.active) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ExhaustTowerBlockEntity be) {
        if (!be.active) return;
        RandomSource rnd = level.random;
        double topX = pos.getX() + 0.5;
        double topY = pos.getY() + TOWER_HEIGHT - 0.2;
        double topZ = pos.getZ() + 0.5;
        // Много дыма наверху
        for (int i = 0; i < 6; i++) {
            double ox = (rnd.nextDouble() - 0.5) * 0.6;
            double oz = (rnd.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.LARGE_SMOKE, topX + ox, topY, topZ + oz, 0.0, 0.06 + rnd.nextDouble() * 0.05, 0.0);
        }
        for (int i = 0; i < 3; i++) {
            double ox = (rnd.nextDouble() - 0.5) * 0.5;
            double oz = (rnd.nextDouble() - 0.5) * 0.5;
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, topX + ox, topY, topZ + oz, 0.0, 0.08, 0.0);
        }
    }

    public boolean isActive() { return active; }
    public FluidTank getSmokeTank() { return smokeTank; }

    // Принимаем дым со всех сторон основания (труба -> башня)
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return smokeInHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        smokeInHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("SmokeTank", smokeTank.writeToNBT(new CompoundTag()));
        tag.putBoolean("Active", active);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        smokeTank.readFromNBT(tag.getCompound("SmokeTank"));
        active = tag.getBoolean("Active");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) { load(tag); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) handleUpdateTag(tag);
    }
}
