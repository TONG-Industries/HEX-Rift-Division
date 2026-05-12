package com.cim.block.entity.industrial.rotation;

import com.cim.api.rotation.KineticNetwork;
import com.cim.api.rotation.KineticNetworkManager;
import com.cim.api.rotation.Rotational;
import com.cim.api.rotation.ShaftDiameter;
import com.cim.api.rotation.ShaftMaterial;
import com.cim.block.basic.industrial.rotation.ShaftBlock;
import com.cim.block.basic.industrial.rotation.TachometerBlock;
import com.cim.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

public class TachometerBlockEntity extends BlockEntity implements Rotational {

    // --- Данные вала (копия из BearingBlockEntity) ---
    private long speed = 0;
    private long lastSyncedSpeed = 0;
    private float networkScale = 1.0f;

    private boolean hasShaft = false;
    private ShaftMaterial shaftMaterial = null;
    private ShaftDiameter shaftDiameter = null;

    // --- Данные сети для HUD (синхронизируются на клиент) ---
    private long networkSpeed = 0;
    private long networkTorque = 0;
    private long networkInertia = 0;
    private long networkFriction = 0;

    public TachometerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TACHOMETER_BE.get(), pos, state);
    }

    // --- Геттеры вала ---
    public boolean hasShaft() { return hasShaft; }
    public ShaftMaterial getShaftMaterial() { return shaftMaterial; }
    public ShaftDiameter getShaftDiameter() { return shaftDiameter; }

    // --- Геттеры данных сети для HUD ---
    public long getNetworkSpeed() { return networkSpeed; }
    public long getNetworkTorque() { return networkTorque; }
    public long getNetworkInertia() { return networkInertia; }
    public long getNetworkFriction() { return networkFriction; }

    // --- Управление валом ---
    public void insertShaft(ShaftMaterial material, ShaftDiameter diameter) {
        this.hasShaft = true;
        this.shaftMaterial = material;
        this.shaftDiameter = diameter;
        setChanged();
        syncToClient();
    }

    public void removeShaft() {
        this.hasShaft = false;
        this.shaftMaterial = null;
        this.shaftDiameter = null;
        // Сбрасываем данные сети
        this.networkSpeed = 0;
        this.networkTorque = 0;
        this.networkInertia = 0;
        this.networkFriction = 0;
        setChanged();
        syncToClient();
    }

    // --- Серверный тик: чтение параметров сети ---
    public static void serverTick(Level level, BlockPos pos, BlockState state, TachometerBlockEntity be) {
        if (level.isClientSide) return;

        if (!be.hasShaft) {
            // Без вала обнуляем всё
            if (be.networkSpeed != 0 || be.networkTorque != 0 || be.networkInertia != 0 || be.networkFriction != 0) {
                be.networkSpeed = 0;
                be.networkTorque = 0;
                be.networkInertia = 0;
                be.networkFriction = 0;
                be.setChanged();
                be.syncToClient();
            }
            return;
        }

        KineticNetwork net = KineticNetworkManager.get((ServerLevel) level).getNetworkFor(pos);
        if (net != null) {
            // Показываем масштабированную скорость этого блока (Bug #5 fix: не raw скорость сети)
            long newSpeed = be.speed; // скорость уже обновляется через setSpeed()

            // Момент на этом валу = totalGeneratedTorque / |networkScale|
            // Редуктор (scale < 1) увеличивает момент: T_shaft = T_primary / scale
            float absScale = Math.abs(be.networkScale);
            long newTorque = (absScale > 0.001f)
                    ? (long) (net.getTotalTorque() / absScale)
                    : net.getTotalTorque();

            long newInertia = net.getTotalInertia();
            long newFriction = net.getTotalFriction();

            // Обновляем только если что-то изменилось
            if (newSpeed != be.networkSpeed || newTorque != be.networkTorque ||
                    newInertia != be.networkInertia || newFriction != be.networkFriction) {
                be.networkSpeed = newSpeed;
                be.networkTorque = newTorque;
                be.networkInertia = newInertia;
                be.networkFriction = newFriction;
                be.setChanged();
                be.syncToClient();
            }
        }
    }

    // --- Rotational interface ---

    @Override
    public boolean canConnectMechanically(BlockPos myPos, BlockPos neighborPos, Rotational neighbor) {
        if (!this.hasShaft()) return false;

        if (neighbor instanceof ShaftBlockEntity shaftBE) {
            if (shaftBE.getBlockState().getBlock() instanceof ShaftBlock shaftBlock) {
                return shaftBlock.getDiameter() == this.getShaftDiameter();
            }
        }
        if (neighbor instanceof BearingBlockEntity otherBearing) {
            return otherBearing.hasShaft() && otherBearing.getShaftDiameter() == this.getShaftDiameter();
        }
        if (neighbor instanceof TachometerBlockEntity otherTach) {
            return otherTach.hasShaft() && otherTach.getShaftDiameter() == this.getShaftDiameter();
        }
        return true;
    }

    @Override
    public void setSpeed(long speed) {
        long actualSpeed = (long) (speed * this.networkScale);
        if (this.speed != actualSpeed) {
            this.speed = actualSpeed;
            setChanged();
            if (shouldSyncSpeed()) {
                this.lastSyncedSpeed = this.speed;
                syncToClient();
            }
        }
    }

    private boolean shouldSyncSpeed() {
        if (this.speed == 0 && this.lastSyncedSpeed != 0) return true;
        if (this.speed != 0 && this.lastSyncedSpeed == 0) return true;
        long diff = Math.abs(this.speed - this.lastSyncedSpeed);
        long threshold = Math.max(2, Math.abs(this.lastSyncedSpeed) / 20);
        return diff >= threshold;
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Direction[] getPropagationDirections() {
        if (!hasShaft) return new Direction[0];
        Direction facing = getBlockState().getValue(TachometerBlock.FACING);
        return new Direction[]{facing, facing.getOpposite()};
    }

    @Override
    public long getInertiaContribution() {
        long baseInertia = 10;
        if (hasShaft && shaftMaterial != null && shaftDiameter != null) {
            baseInertia += (long) (shaftMaterial.baseInertia() * shaftDiameter.inertiaMod);
        }
        return baseInertia;
    }

    @Override
    public long getFrictionContribution() { return 1; }

    @Override
    public long getMaxTorqueTolerance() { return 10000; }
    @Override
    public long getMaxSpeed() {
        if (hasShaft() && getShaftMaterial() != null && getShaftDiameter() != null) {
            return (long) (getShaftMaterial().getBaseMaxSpeed() * getShaftDiameter().getSpeedMultiplier());
        }
        return 1024;
    }

    @Override
    public long getMaxTorque() {
        if (hasShaft() && getShaftMaterial() != null && getShaftDiameter() != null) {
            return (long) (getShaftMaterial().getBaseMaxTorque() * getShaftDiameter().getTorqueMultiplier());
        }
        return 10000;
    }

    @Override
    public long getSpeed() { return speed; }

    @Override
    public long getVisualSpeed() {
        if (!this.hasShaft) return 0;
        BlockState state = getBlockState();
        if (!state.hasProperty(TachometerBlock.FACING)) return 0;

        Direction facing = state.getValue(TachometerBlock.FACING);
        if (facing == Direction.SOUTH || facing == Direction.EAST || facing == Direction.UP) {
            return -this.speed;
        }
        return this.speed;
    }

    @Override
    public long getTorque() { return 0; }
    @Override
    public boolean isSource() { return false; }

    @Override
    public List<BlockPos> getPotentialConnections(Level level, BlockPos myPos) {
        if (!hasShaft) return Collections.emptyList();
        Direction facing = getBlockState().getValue(TachometerBlock.FACING);
        return List.of(myPos.relative(facing), myPos.relative(facing.getOpposite()));
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", this.speed);
        tag.putLong("LastSyncedSpeed", this.lastSyncedSpeed);
        tag.putBoolean("HasShaft", this.hasShaft);
        tag.putFloat("NetworkScale", this.networkScale);

        // Данные сети
        tag.putLong("NetSpeed", this.networkSpeed);
        tag.putLong("NetTorque", this.networkTorque);
        tag.putLong("NetInertia", this.networkInertia);
        tag.putLong("NetFriction", this.networkFriction);

        if (this.hasShaft && this.shaftMaterial != null && this.shaftDiameter != null) {
            tag.putString("ShaftMaterial", this.shaftMaterial.name());
            tag.putString("ShaftDiameter", this.shaftDiameter.name());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.speed = tag.getLong("Speed");
        this.lastSyncedSpeed = tag.getLong("LastSyncedSpeed");
        this.hasShaft = tag.getBoolean("HasShaft");
        this.networkScale = tag.contains("NetworkScale") ? tag.getFloat("NetworkScale") : 1.0f;

        // Данные сети
        this.networkSpeed = tag.getLong("NetSpeed");
        this.networkTorque = tag.getLong("NetTorque");
        this.networkInertia = tag.getLong("NetInertia");
        this.networkFriction = tag.getLong("NetFriction");

        if (this.hasShaft) {
            String matName = tag.getString("ShaftMaterial").toLowerCase();
            String diaName = tag.getString("ShaftDiameter");

            this.shaftMaterial = switch (matName) {
                case "duralumin" -> ShaftMaterial.DURALUMIN;
                case "steel" -> ShaftMaterial.STEEL;
                case "titanium" -> ShaftMaterial.TITANIUM;
                case "tungsten_carbide" -> ShaftMaterial.TUNGSTEN_CARBIDE;
                default -> ShaftMaterial.IRON;
            };

            try {
                this.shaftDiameter = ShaftDiameter.valueOf(diaName);
            } catch (IllegalArgumentException e) {
                this.shaftDiameter = ShaftDiameter.MEDIUM;
            }
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

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            KineticNetwork net = KineticNetworkManager.get((ServerLevel) level).getNetworkFor(worldPosition);
            if (net != null) {
                // Применяем networkScale, чтобы восстановить правильную масштабированную скорость (Bug #1 fix)
                this.speed = (long) (net.getSpeed() * this.networkScale);
                this.lastSyncedSpeed = this.speed;
                net.requestRecalculation();
            }
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(1.5D);
    }

    @Override
    public void setNetworkScale(float scale) { this.networkScale = scale; }

    @Override
    public float getNetworkScale() { return this.networkScale; }
}
