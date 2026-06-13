package com.trd.block.entity.industrial.rotation;

import com.trd.api.energy.IEnergyConnector;
import com.trd.api.energy.IEnergyProvider;
import com.trd.api.rotation.KineticNetworkManager;
import com.trd.api.rotation.Rotational;
import com.trd.block.basic.industrial.rotation.StatorBlock;
import com.trd.block.entity.ModBlockEntities;
import com.trd.capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Статор — кинетический потребитель и электрический генератор.
 *
 * В кинетической сети:
 *   - Реализует Rotational с NodeRole.CONSUMER
 *   - Потребляет {@value #TORQUE_CONSUMPTION} единиц момента из кинетической сети
 *   - Если суммарное потребление сети превысит мощность генераторов, сеть перегрузится
 *
 * В электрической сети:
 *   - Реализует IEnergyProvider: генерирует JE пропорционально скорости вала с ротором
 */
public class StatorBlockEntity extends KineticNodeBlockEntity implements IEnergyProvider, IEnergyConnector {

    public static final long MAX_EXTRACT = 2000;

    // ===================== ПОЛЯ =====================
    private long energyStored = 0;
    private boolean wasFull = false;
    private long lastSyncedEnergy = -1;

    private final ItemStackHandler coilsInventory = new ItemStackHandler(12) {
        @Override
        protected void onContentsChanged(int slot) {
            long maxEn = getMaxEnergyDynamic();
            if (energyStored > maxEn) {
                energyStored = maxEn;
            }
            checkFullStateChange();
            requestKineticRecalculation();
            setChanged();
            syncToClient();
        }
    };
    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> coilsInventory);

    // ===================== CAPABILITY =====================
    private final LazyOptional<IEnergyProvider> providerCap = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyConnector> connectorCap = LazyOptional.of(() -> this);

    public StatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATOR_BE.get(), pos, state);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (canConnectEnergy(side)) {
            if (cap == ModCapabilities.ENERGY_PROVIDER) return providerCap.cast();
            if (cap == ModCapabilities.ENERGY_CONNECTOR) return connectorCap.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        providerCap.invalidate();
        connectorCap.invalidate();
        itemHandlerCap.invalidate();
    }

    // ===================== IEnergyConnector =====================
    @Override
    public boolean canConnectEnergy(Direction side) {
        BlockState state = getBlockState();
        if (!state.hasProperty(StatorBlock.FACING)) return true;
        return side != state.getValue(StatorBlock.FACING);
    }

    // ===================== IEnergyProvider =====================
    @Override
    public long getEnergyStored() { return energyStored; }

    @Override
    public long getMaxEnergyStored() { return getMaxEnergyDynamic(); }

    @Override
    public void setEnergyStored(long energy) {
        this.energyStored = Math.max(0, Math.min(energy, getMaxEnergyDynamic()));
        checkFullStateChange();
    }

    @Override
    public long getProvideSpeed() { return MAX_EXTRACT; }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        long toExtract = Math.min(energyStored, Math.min(maxExtract, MAX_EXTRACT));
        if (!simulate) {
            energyStored -= toExtract;
            setChanged();
            checkFullStateChange();
        }
        return toExtract;
    }

    @Override
    public boolean canExtract() { return energyStored > 0; }

    // ===================== Rotational: КИНЕТИЧЕСКОЕ ПОТРЕБЛЕНИЕ =====================

    /**
     * Роль статора в кинетической сети — CONSUMER.
     * Он тормозит сеть (потребляет момент), преобразуя механическую энергию в электрическую.
     */
    @Override
    public NodeRole getNodeRole() {
        return NodeRole.CONSUMER;
    }

    /**
     * Потребляемый момент из кинетической сети.
     * Активен только когда статор работает (есть ротор на соседнем валу).
     */
    @Override
    public long getConsumedTorque() {
        if (energyStored >= getMaxEnergyDynamic()) return 0L;
        if (!hasActiveRotor()) return 0L;

        long totalLoad = 0;
        for (int i = 0; i < 12; i++) {
            net.minecraft.world.item.ItemStack stack = coilsInventory.getStackInSlot(i);
            if (stack.getItem() instanceof com.trd.item.energy.StatorCoilItem coil) {
                long load = coil.getBaseTorqueLoad();
                net.minecraft.world.item.ItemStack opposite = coilsInventory.getStackInSlot((i + 6) % 12);
                if (opposite.isEmpty()) {
                    load *= coil.getAsymmetryMultiplier();
                }
                totalLoad += load;
            }
        }
        return totalLoad;
    }

    /** Проверяет, есть ли активный ротор на валу, к которому прикреплён статор. */
    private boolean hasActiveRotor() {
        if (level == null) return false;
        BlockState state = getBlockState();
        if (!state.hasProperty(StatorBlock.FACING)) return false;
        Direction facing = state.getValue(StatorBlock.FACING);
        Direction.Axis axis = state.getValue(StatorBlock.AXIS);
        BlockPos holeOffset = com.trd.multiblock.system.MultiblockStructureHelper.rotateStatorPos(new BlockPos(0, 1, 0), facing, axis);
        BlockPos shaftPos = worldPosition.offset(holeOffset);
        if (!level.isLoaded(shaftPos)) return false;
        var be = level.getBlockEntity(shaftPos);
        return be instanceof ShaftBlockEntity shaft && shaft.hasRotor();
    }

    // Базовые кинетические методы (вал статора не участвует в передаче момента напрямую)
    @Override
    public long getTorque() { return 0; }

    @Override
    public boolean isSource() { return false; }

    @Override
    public double getInertiaContribution() { return 2.0; }

    @Override
    public long getMaxTorqueTolerance() { return Long.MAX_VALUE; }

    @Override
    public long getMaxSpeed() { return Long.MAX_VALUE; }

    @Override
    public long getMaxTorque() { return Long.MAX_VALUE; }

    @Override
    public Direction[] getPropagationDirections() {
        // Статор не распространяет кинетику дальше себя
        return new Direction[0];
    }

    @Override
    public java.util.List<BlockPos> getPotentialConnections(Level level, BlockPos myPos) {
        // Статор соединяется с валом, к которому прикреплён — чтобы попасть в его кинетическую сеть
        // и учитываться в recalculate() через getConsumedTorque()
        BlockState state = getBlockState();
        if (!state.hasProperty(StatorBlock.FACING)) return java.util.Collections.emptyList();
        Direction facing = state.getValue(StatorBlock.FACING);
        Direction.Axis axis = state.getValue(StatorBlock.AXIS);
        BlockPos holeOffset = com.trd.multiblock.system.MultiblockStructureHelper.rotateStatorPos(new BlockPos(0, 1, 0), facing, axis);
        return java.util.List.of(myPos.offset(holeOffset));
    }

    @Override
    public boolean canConnectMechanically(BlockPos myPos, BlockPos neighborPos, Rotational neighbor) {
        // Статор подключается только к валу, на который смотрит FACING
        BlockState state = getBlockState();
        if (!state.hasProperty(StatorBlock.FACING)) return false;
        Direction facing = state.getValue(StatorBlock.FACING);
        Direction.Axis axis = state.getValue(StatorBlock.AXIS);
        BlockPos holeOffset = com.trd.multiblock.system.MultiblockStructureHelper.rotateStatorPos(new BlockPos(0, 1, 0), facing, axis);
        return neighborPos.equals(myPos.offset(holeOffset));
    }

    // ===================== TICK =====================

    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityTicker<T> createTicker() {
        return (level, pos, state, be) -> {
            if (!level.isClientSide && be instanceof StatorBlockEntity stator) {
                stator.tick();
            }
        };
    }

    private void tick() {
        if (level == null || level.isClientSide) return;

        Direction facing = getBlockState().getValue(StatorBlock.FACING);
        Direction.Axis axis = getBlockState().getValue(StatorBlock.AXIS);
        BlockPos holeOffset = com.trd.multiblock.system.MultiblockStructureHelper.rotateStatorPos(new BlockPos(0, 1, 0), facing, axis);
        BlockPos shaftPos = worldPosition.offset(holeOffset);

        if (level.getBlockEntity(shaftPos) instanceof ShaftBlockEntity shaft) {
            if (shaft.hasRotor()) {
                long speed = Math.abs(shaft.getSpeed());
                long maxEn = getMaxEnergyDynamic();
                if (speed > 0 && energyStored < maxEn) {
                    float rotorEfficiency = 1.0f;
                    com.trd.api.rotation.RotorType rotorType = shaft.getRotorType();
                    if (rotorType != null) rotorEfficiency = rotorType.getEfficiency();

                    long totalConversion = 0;
                    for (int i = 0; i < 12; i++) {
                        net.minecraft.world.item.ItemStack stack = coilsInventory.getStackInSlot(i);
                        if (stack.getItem() instanceof com.trd.item.energy.StatorCoilItem coil) {
                            totalConversion += coil.getEnergyConversionRate();
                        }
                    }

                    // Formula: Speed * Total Conversion * Rotor Efficiency
                    long generated = (long) ((speed * totalConversion * rotorEfficiency) / 20.0f);
                    if (generated > 0) {
                        energyStored = Math.min(maxEn, energyStored + generated);
                        setChanged();
                        checkFullStateChange();
                    }
                }
            }
        }

        if (energyStored != lastSyncedEnergy && level.getGameTime() % 10 == 0) {
            lastSyncedEnergy = energyStored;
            syncToClient();
        }
    }

    public long getMaxEnergyDynamic() {
        long sum = 0;
        for (int i = 0; i < coilsInventory.getSlots(); i++) {
            net.minecraft.world.item.ItemStack stack = coilsInventory.getStackInSlot(i);
            if (stack.getItem() instanceof com.trd.item.energy.StatorCoilItem coil) {
                sum += coil.getEnergyBuffer();
            }
        }
        return sum;
    }

    // ===================== ВНУТРЕННИЕ МЕТОДЫ =====================

    private void checkFullStateChange() {
        boolean isFull = energyStored >= getMaxEnergyDynamic();
        if (isFull != wasFull) {
            wasFull = isFull;
            requestKineticRecalculation();
        }
    }

    private void requestKineticRecalculation() {
        if (level instanceof ServerLevel serverLevel) {
            Direction facing = getBlockState().getValue(StatorBlock.FACING);
            Direction.Axis axis = getBlockState().getValue(StatorBlock.AXIS);
            BlockPos holeOffset = com.trd.multiblock.system.MultiblockStructureHelper.rotateStatorPos(new BlockPos(0, 1, 0), facing, axis);
            BlockPos shaftPos = worldPosition.offset(holeOffset);
            var net = KineticNetworkManager.get(serverLevel).getNetworkFor(shaftPos);
            if (net != null) {
                net.requestRecalculation();
            }
        }
    }

    // ===================== NBT =====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); // speed, lastSyncedSpeed, networkScale
        tag.putLong("EnergyStored", energyStored);
        tag.putBoolean("WasFull", wasFull);
        tag.put("CoilsInventory", coilsInventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag); // speed, lastSyncedSpeed, networkScale
        energyStored = tag.getLong("EnergyStored");
        wasFull = tag.getBoolean("WasFull");
        if (tag.contains("CoilsInventory")) {
            coilsInventory.deserializeNBT(tag.getCompound("CoilsInventory"));
        }
    }
}
