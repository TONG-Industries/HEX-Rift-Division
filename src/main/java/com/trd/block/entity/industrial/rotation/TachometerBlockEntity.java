package com.trd.block.entity.industrial.rotation;

import com.trd.api.rotation.KineticNetwork;
import com.trd.api.rotation.KineticNetworkManager;
import com.trd.api.rotation.Rotational;
import com.trd.api.rotation.ShaftDiameter;
import com.trd.api.rotation.ShaftMaterial;
import com.trd.block.basic.industrial.rotation.ShaftBlock;
import com.trd.block.basic.industrial.rotation.TachometerBlock;
import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

public class TachometerBlockEntity extends KineticNodeBlockEntity {

    // --- Данные вала ---
    private boolean hasShaft = false;
    private ShaftMaterial shaftMaterial = null;
    private ShaftDiameter shaftDiameter = null;

    // --- Данные сети для HUD (синхронизируются на клиент) ---
    private long networkSpeed = 0;
    private long networkTorque = 0;
    private long networkConsumedTorque = 0;
    private double networkInertia = 0;
    private double networkFrictionMultiplier = 1.0;
    private double networkLoad = 0;

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
    public long getNetworkConsumedTorque() { return networkConsumedTorque; }
    public double getNetworkInertia() { return networkInertia; }
    public double getNetworkFrictionMultiplier() { return networkFrictionMultiplier; }
    public double getNetworkLoad() { return networkLoad; }

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
        this.networkSpeed = 0;
        this.networkTorque = 0;
        this.networkConsumedTorque = 0;
        this.networkInertia = 0;
        this.networkFrictionMultiplier = 1.0;
        this.networkLoad = 0;
        setChanged();
        syncToClient();
    }

    // --- Серверный тик: чтение параметров сети ---
    public static void serverTick(Level level, BlockPos pos, BlockState state, TachometerBlockEntity be) {
        if (level.isClientSide) return;

        if (!be.hasShaft) {
            if (be.networkSpeed != 0 || be.networkTorque != 0 || be.networkConsumedTorque != 0 || be.networkInertia != 0 || be.networkFrictionMultiplier != 1.0) {
                be.networkSpeed = 0;
                be.networkTorque = 0;
                be.networkConsumedTorque = 0;
                be.networkInertia = 0;
                be.networkFrictionMultiplier = 1.0;
                be.networkLoad = 0;
                be.setChanged();
                be.syncToClient();
            }
            return;
        }

        KineticNetwork net = KineticNetworkManager.get((ServerLevel) level).getNetworkFor(pos);
        if (net != null) {
            long newSpeed = be.speed;
            float absScale = Math.abs(be.networkScale);
            long newTorque = (absScale > 0.001f)
                    ? (long) (net.getTotalTorque() / absScale)
                    : net.getTotalTorque();
            long newConsumedTorque = (absScale > 0.001f)
                    ? (long) (net.getTotalConsumedTorque() / absScale)
                    : net.getTotalConsumedTorque();
            double newInertia = net.getTotalInertia();
            double newFrictionMult = net.getFrictionMultiplier();
            double newLoad = net.getLoadFactor();
            
            if (newSpeed != be.networkSpeed || newTorque != be.networkTorque || newConsumedTorque != be.networkConsumedTorque ||
                    newInertia != be.networkInertia || newFrictionMult != be.networkFrictionMultiplier || newLoad != be.networkLoad) {
                be.networkSpeed = newSpeed;
                be.networkTorque = newTorque;
                be.networkConsumedTorque = newConsumedTorque;
                be.networkInertia = newInertia;
                be.networkFrictionMultiplier = newFrictionMult;
                be.networkLoad = newLoad;
                be.setChanged();
                be.syncToClient();
            }
        }
    }

    // --- Rotational ---

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
    public Direction[] getPropagationDirections() {
        if (!hasShaft) return new Direction[0];
        Direction facing = getBlockState().getValue(TachometerBlock.FACING);
        return new Direction[]{facing, facing.getOpposite()};
    }

    @Override
    public List<BlockPos> getPotentialConnections(Level level, BlockPos myPos) {
        if (!hasShaft) return Collections.emptyList();
        Direction facing = getBlockState().getValue(TachometerBlock.FACING);
        return List.of(myPos.relative(facing), myPos.relative(facing.getOpposite()));
    }

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
    public double getInertiaContribution() {
        if (hasShaft && shaftMaterial != null && shaftDiameter != null) {
            return (double) (shaftMaterial.baseInertia() * shaftDiameter.inertiaMod);
        }
        return 2.0; // Как у статора, если пустой
    }

    @Override
    public long getMaxTorqueTolerance() { return 10000; }

    @Override
    public long getMaxSpeed() {
        if (hasShaft() && getShaftMaterial() != null && getShaftDiameter() != null) {
            return (long) (getShaftMaterial().baseSpeed() * getShaftDiameter().getSpeedMultiplier());
        }
        return 1024;
    }

    @Override
    public long getMaxTorque() {
        if (hasShaft() && getShaftMaterial() != null && getShaftDiameter() != null) {
            return (long) (getShaftMaterial().baseTorque() * getShaftDiameter().getTorqueMultiplier());
        }
        return 10000;
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); // speed, lastSyncedSpeed, networkScale
        tag.putBoolean("HasShaft", this.hasShaft);
        tag.putLong("NetSpeed", this.networkSpeed);
        tag.putLong("NetTorque", this.networkTorque);
        tag.putLong("NetConsumedTorque", this.networkConsumedTorque);
        tag.putDouble("NetInertia", this.networkInertia);
        tag.putDouble("NetFrictionMult", this.networkFrictionMultiplier);
        tag.putDouble("NetLoad", this.networkLoad);
        if (this.hasShaft && this.shaftMaterial != null && this.shaftDiameter != null) {
            tag.putString("ShaftMaterial", this.shaftMaterial.name());
            tag.putString("ShaftDiameter", this.shaftDiameter.name());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag); // speed, lastSyncedSpeed, networkScale
        this.hasShaft = tag.getBoolean("HasShaft");
        this.networkSpeed = tag.getLong("NetSpeed");
        this.networkTorque = tag.getLong("NetTorque");
        this.networkConsumedTorque = tag.getLong("NetConsumedTorque");
        this.networkInertia = tag.getDouble("NetInertia");
        this.networkFrictionMultiplier = tag.getDouble("NetFrictionMult");
        this.networkLoad = tag.getDouble("NetLoad");
        if (this.hasShaft) {
            String matName = tag.getString("ShaftMaterial").toLowerCase();
            String diaName = tag.getString("ShaftDiameter");
            this.shaftMaterial = switch (matName) {
                case "duralumin"       -> ShaftMaterial.DURALUMIN;
                case "steel"           -> ShaftMaterial.STEEL;
                case "titanium"        -> ShaftMaterial.TITANIUM;
                case "tungsten_carbide"-> ShaftMaterial.TUNGSTEN_CARBIDE;
                default                -> ShaftMaterial.IRON;
            };
            try {
                this.shaftDiameter = ShaftDiameter.valueOf(diaName);
            } catch (IllegalArgumentException e) {
                this.shaftDiameter = ShaftDiameter.MEDIUM;
            }
        }
    }

    // --- Рендер ---

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(1.5D);
    }
}
