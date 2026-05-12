package com.trd.block.entity.industrial.energy;

import com.trd.api.energy.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.trd.capability.ModCapabilities;

/**
 * Абстрактный базовый класс для всех энергетических BlockEntity.
 * 
 * Содержит общую логику:
 * - Хранение энергии (energy, capacity)
 * - Режимы работы (mode) и приоритеты (priority)
 * - Регистрация в EnergyNetworkManager
 * - Совместимость с Forge Energy через PackedEnergyCapabilityProvider
 * - Синхронизация данных с клиентом
 */
public abstract class EnergyNodeBlockEntity extends BlockEntity implements IEnergyProvider, IEnergyReceiver {

    protected long energy = 0;
    protected long capacity = 0;
    protected int mode = 0; // 0=BOTH, 1=INPUT, 2=OUTPUT, 3=DISABLED
    protected Priority priority = Priority.NORMAL;

    protected final PackedEnergyCapabilityProvider feCapabilityProvider;
    protected final LazyOptional<IEnergyProvider> hbmProvider = LazyOptional.of(() -> this);
    protected final LazyOptional<IEnergyReceiver> hbmReceiver = LazyOptional.of(() -> this);
    protected final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);

    protected EnergyNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.feCapabilityProvider = new PackedEnergyCapabilityProvider(this);
    }

    // ===================== IEnergyProvider & IEnergyReceiver =====================

    @Override
    public long getEnergyStored() {
        return this.energy;
    }

    @Override
    public long getMaxEnergyStored() {
        return this.capacity;
    }

    @Override
    public void setEnergyStored(long energy) {
        this.energy = Math.max(0, Math.min(this.capacity, energy));
        setChanged();
    }

    @Override
    public Priority getPriority() {
        return this.priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
        setChanged();
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true; // По умолчанию соединяется со всех сторон, можно переопределить
    }

    @Override
    public abstract long getProvideSpeed();

    @Override
    public abstract long getReceiveSpeed();

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        long energyExtracted = Math.min(this.energy, Math.min(getProvideSpeed(), maxExtract));
        if (!simulate && energyExtracted > 0) {
            setEnergyStored(this.energy - energyExtracted);
        }
        return energyExtracted;
    }

    @Override
    public boolean canExtract() {
        return (mode == 0 || mode == 2) && this.energy > 0 && getProvideSpeed() > 0;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        long energyReceived = Math.min(this.capacity - this.energy, Math.min(getReceiveSpeed(), maxReceive));
        if (!simulate && energyReceived > 0) {
            setEnergyStored(this.energy + energyReceived);
        }
        return energyReceived;
    }

    @Override
    public boolean canReceive() {
        return (mode == 0 || mode == 1) && this.energy < this.capacity && getReceiveSpeed() > 0;
    }

    // ===================== ЖИЗНЕННЫЙ ЦИКЛ =====================

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) level).addNode(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) level).removeNode(worldPosition);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        hbmProvider.invalidate();
        hbmReceiver.invalidate();
        hbmConnector.invalidate();
        feCapabilityProvider.invalidate();
    }

    // ===================== CAPABILITIES =====================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.ENERGY_CONNECTOR) {
            return hbmConnector.cast();
        }
        if (cap == ModCapabilities.ENERGY_PROVIDER && (mode == 0 || mode == 2)) {
            return hbmProvider.cast();
        }
        if (cap == ModCapabilities.ENERGY_RECEIVER && (mode == 0 || mode == 1)) {
            return hbmReceiver.cast();
        }
        
        LazyOptional<T> feCap = feCapabilityProvider.getCapability(cap, side);
        if (feCap.isPresent()) return feCap;

        return super.getCapability(cap, side);
    }

    // ===================== NBT & SYNC =====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", this.energy);
        tag.putLong("Capacity", this.capacity);
        tag.putInt("EnergyMode", this.mode);
        tag.putInt("Priority", this.priority.ordinal());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.energy = tag.getLong("Energy");
        this.capacity = tag.getLong("Capacity");
        this.mode = tag.getInt("EnergyMode");
        if (tag.contains("Priority")) {
            this.priority = Priority.values()[Math.min(tag.getInt("Priority"), Priority.values().length - 1)];
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    protected void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
