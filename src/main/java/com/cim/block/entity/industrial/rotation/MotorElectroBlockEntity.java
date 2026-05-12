package com.cim.block.entity.industrial.rotation;

import com.cim.api.energy.IEnergyConnector;
import com.cim.api.energy.IEnergyReceiver;
import com.cim.api.rotation.KineticNetwork;
import com.cim.api.rotation.KineticNetworkManager;
import com.cim.api.rotation.Rotational;
import com.cim.api.rotation.ShaftDiameter;
import com.cim.block.basic.industrial.rotation.MotorElectroBlock;
import com.cim.block.basic.industrial.rotation.ShaftBlock;
import com.cim.block.entity.ModBlockEntities;
import com.cim.capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MotorElectroBlockEntity extends BlockEntity implements Rotational, IEnergyReceiver {

    // ===================== КОНСТАНТЫ =====================
    public static final long MAX_ENERGY = 10_000L;
    public static final long RECEIVE_SPEED = 1_000L;
    public static final int MIN_RPM = 100;
    public static final int MAX_RPM = 1_000;

    // ===================== ПОЛЯ =====================
    // Энергия
    private long energyStored = 0L;

    // Параметры мотора
    private int targetRpm = MAX_RPM; // 100–1000 RPM
    private boolean reversed = false;
    private boolean hasEnergy = false;

    // Кинетика (визуальная/сетевая скорость)
    private long currentSpeed = 0;
    private long lastSyncedSpeed = 0;

    /**
     * Флаг фронта редстоун-сигнала — аналогично SwitchBlockEntity.isTriggered.
     * Хранится в BlockEntity (а не в Block-синглтоне!), поэтому:
     *  - персистентен между перезагрузками
     *  - независим для каждого мотора в мире
     */
    public boolean isTriggered = false;

    // Capability — предоставляем только со стороны задней грани
    private final LazyOptional<IEnergyReceiver> receiverCap = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyConnector> connectorCap = LazyOptional.of(() -> this);

    // ContainerData для синхронизации GUI (4 int-слота)
    // [0] targetRpm [1] energyStored/10 [2] JE/s [3] torque
    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> targetRpm;
                case 1 -> (int) (energyStored / 10); // max 1000 — в int помещается
                case 2 -> getConsumptionPerSecond();
                case 3 -> (int) getTorqueNm();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0)
                targetRpm = clampRpm(value);
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    // ===================== КОНСТРУКТОР =====================
    public MotorElectroBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOTOR_ELECTRO_BE.get(), pos, state);
    }

    // ===================== CAPABILITY =====================
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        Direction backSide = getBackSide();
        if (side == null || side == backSide) {
            if (cap == ModCapabilities.ENERGY_RECEIVER)
                return receiverCap.cast();
            if (cap == ModCapabilities.ENERGY_CONNECTOR)
                return connectorCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        receiverCap.invalidate();
        connectorCap.invalidate();
    }

    /** Сторона, с которой принимается энергия: противоположная FACING */
    private Direction getBackSide() {
        BlockState state = getBlockState();
        if (!state.hasProperty(MotorElectroBlock.FACING))
            return Direction.NORTH;
        return state.getValue(MotorElectroBlock.FACING).getOpposite();
    }

    // ===================== IEnergyConnector =====================
    @Override
    public boolean canConnectEnergy(Direction side) {
        return side == getBackSide();
    }

    // ===================== IEnergyReceiver =====================
    @Override
    public long getEnergyStored() {
        return energyStored;
    }

    @Override
    public long getMaxEnergyStored() {
        return MAX_ENERGY;
    }

    @Override
    public void setEnergyStored(long energy) {
        this.energyStored = Math.max(0, Math.min(energy, MAX_ENERGY));
    }

    @Override
    public long getReceiveSpeed() {
        return RECEIVE_SPEED;
    }

    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        long canReceive = Math.min(MAX_ENERGY - energyStored, Math.min(maxReceive, RECEIVE_SPEED));
        if (!simulate && canReceive > 0) {
            energyStored += canReceive;
            setChanged();
        }
        return canReceive;
    }

    @Override
    public boolean canReceive() {
        return energyStored < MAX_ENERGY;
    }

    // ===================== ПАРАМЕТРЫ МОТОРА =====================

    /** Крутящий момент в Нм: targetRpm / 5 */
    public long getTorqueNm() {
        return targetRpm / 5L;
    }

    /** Потребление в JE/тик */
    public int getConsumptionPerTick() {
        // При 1000 RPM = 25 JE/тик; линейно
        return Math.max(1, targetRpm * 25 / MAX_RPM);
    }

    /** Потребление в JE/сек (для GUI) */
    public int getConsumptionPerSecond() {
        return getConsumptionPerTick() * 20;
    }

    // ===================== GETTERS / SETTERS =====================

    public int getTargetRpm() {
        return targetRpm;
    }

    public void setTargetRpm(int rpm) {
        int newRpm = clampRpm(rpm);
        if (this.targetRpm != newRpm) {
            this.targetRpm = newRpm;
            setChanged();
            requestKineticRecalculation();
        }
    }

    public boolean isReversed() {
        return reversed;
    }

    /**
     * Переключает направление вращения.
     * Вызывается только при редстоун-сигнале из MotorElectroBlock.neighborChanged.
     */
    public void toggleDirection() {
        this.reversed = !this.reversed;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            requestKineticRecalculation();
        }
    }

    /** Округление до десятков, зажим в [MIN_RPM, MAX_RPM] */
    private static int clampRpm(int rpm) {
        rpm = Math.round(rpm / 10.0f) * 10;
        return Math.max(MIN_RPM, Math.min(MAX_RPM, rpm));
    }

    // ===================== TICK =====================

    /** Точка входа для тика — вызывается через Block.getTicker() */
    public static <T extends BlockEntity> BlockEntityTicker<T> createTicker() {
        return (level, pos, state, be) -> {
            if (!level.isClientSide && be instanceof MotorElectroBlockEntity motor) {
                motor.serverTick((ServerLevel) level);
            }
        };
    }

    private void serverTick(ServerLevel serverLevel) {
        int consumption = getConsumptionPerTick();
        boolean wasRunning = hasEnergy;

        if (energyStored >= consumption) {
            energyStored -= consumption;
            if (!hasEnergy) {
                hasEnergy = true;
                // Сброс накопленной скорости при включении — мотор стартует с нуля
                this.currentSpeed = 0;
                this.lastSyncedSpeed = 0;
                requestKineticRecalculation();
            }
        } else {
            energyStored = 0;
            if (hasEnergy) {
                hasEnergy = false;
                // Принудительно обнуляем скорость — вал должен остановиться
                this.currentSpeed = 0;
                this.lastSyncedSpeed = 0;
                requestKineticRecalculation();
            }
        }

        // Всегда синхронизируем клиент каждый тик (для плавного HUD)
        serverLevel.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        setChanged();
    }

    private void requestKineticRecalculation() {
        if (level instanceof ServerLevel serverLevel) {
            KineticNetwork net = KineticNetworkManager.get(serverLevel).getNetworkFor(worldPosition);
            if (net != null) {
                net.requestRecalculation();
            }
        }
    }

    // ===================== Rotational =====================

    @Override
    public long getGeneratedSpeed() {
        if (!hasEnergy)
            return 0;
        return reversed ? -targetRpm : targetRpm;
    }

    @Override
    public long getSpeed() {
        return currentSpeed;
    }

    @Override
    public long getVisualSpeed() {
        // Если нет энергии — Flywheel плавно остановит анимацию
        if (!hasEnergy)
            return 0;

        BlockState state = getBlockState();
        if (!state.hasProperty(MotorElectroBlock.FACING))
            return 0;
        Direction facing = state.getValue(MotorElectroBlock.FACING);
        // Инвертируем направление в зависимости от ориентации блока
        if (facing == Direction.SOUTH || facing == Direction.EAST || facing == Direction.UP) {
            return -this.currentSpeed;
        }
        return this.currentSpeed;
    }

    @Override
    public long getTorque() {
        // Без энергии мотор не создаёт момент → сеть тормозит по инерции
        return hasEnergy ? getTorqueNm() : 0L;
    }

    @Override
    public boolean isSource() {
        return true;
    }

    @Override
    public long getInertiaContribution() {
        return 50;
    }

    @Override
    public long getFrictionContribution() {
        return 5;
    }

    @Override
    public long getMaxTorqueTolerance() {
        return getMaxTorque();
    }

    @Override
    public long getMaxSpeed() {
        return MAX_RPM;
    }

    @Override
    public long getMaxTorque() {
        return 1024;
    }

    @Override
    public void setSpeed(long speed) {
        if (this.currentSpeed != speed) {
            this.currentSpeed = speed;
            super.setChanged();
            if (shouldSyncSpeed()) {
                this.lastSyncedSpeed = this.currentSpeed;
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                }
            }
        }
    }

    private boolean shouldSyncSpeed() {
        if (this.currentSpeed == 0 && this.lastSyncedSpeed != 0)
            return true;
        if (this.currentSpeed != 0 && this.lastSyncedSpeed == 0)
            return true;
        long diff = Math.abs(this.currentSpeed - this.lastSyncedSpeed);
        long threshold = Math.max(2, Math.abs(this.lastSyncedSpeed) / 20);
        return diff >= threshold;
    }

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
        if (!state.hasProperty(MotorElectroBlock.FACING))
            return new Direction[0];
        return new Direction[] { state.getValue(MotorElectroBlock.FACING) };
    }

    @Override
    public java.util.List<BlockPos> getPotentialConnections(Level lvl, BlockPos myPos) {
        BlockState state = getBlockState();
        if (!state.hasProperty(MotorElectroBlock.FACING))
            return java.util.List.of();
        Direction facing = state.getValue(MotorElectroBlock.FACING);
        return java.util.List.of(myPos.relative(facing));
    }

    // ===================== LIFECYCLE =====================

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            KineticNetwork net = KineticNetworkManager.get((ServerLevel) level)
                    .getNetworkFor(worldPosition);
            if (net != null) {
                this.currentSpeed = net.getSpeed();
                this.lastSyncedSpeed = this.currentSpeed;
                net.requestRecalculation();
            }
        }
    }

    // ===================== NBT =====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("EnergyStored", energyStored);
        tag.putInt("TargetRpm", targetRpm);
        tag.putBoolean("Reversed", reversed);
        tag.putBoolean("HasEnergy", hasEnergy);
        tag.putLong("CurrentSpeed", currentSpeed);
        tag.putLong("LastSyncedSpeed", lastSyncedSpeed);
        tag.putBoolean("IsTriggered", isTriggered);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStored = tag.getLong("EnergyStored");
        targetRpm = clampRpm(tag.getInt("TargetRpm") == 0 ? MAX_RPM : tag.getInt("TargetRpm"));
        reversed = tag.getBoolean("Reversed");
        hasEnergy = tag.getBoolean("HasEnergy");
        currentSpeed = tag.getLong("CurrentSpeed");
        lastSyncedSpeed = tag.getLong("LastSyncedSpeed");
        isTriggered = tag.getBoolean("IsTriggered");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null)
            load(tag);
    }

    // ===================== RENDER =====================

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.5D);
    }

    public long getCurrentVisualSpeed() {
        return this.currentSpeed;
    }
}