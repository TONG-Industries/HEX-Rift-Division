package com.trd.block.entity.industrial.rotation;

import com.trd.api.rotation.Rotational;
import com.trd.api.rotation.ShaftDiameter;
import com.trd.api.rotation.ShaftMaterial;
import com.trd.block.basic.industrial.rotation.BearingBlock;
import com.trd.block.basic.industrial.rotation.ShaftBlock;
import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BearingBlockEntity extends KineticNodeBlockEntity {

    private boolean hasShaft = false;
    private ShaftMaterial shaftMaterial = null;
    private ShaftDiameter shaftDiameter = null;
    private boolean lubricated = false;


    public BearingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEARING_BE.get(), pos, state);
    }

    public boolean hasShaft() { return hasShaft; }
    public ShaftMaterial getShaftMaterial() { return shaftMaterial; }
    public ShaftDiameter getShaftDiameter() { return shaftDiameter; }

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
        setChanged();
        syncToClient();
    }

    public boolean isLubricated() { return lubricated; }
    public void setLubricated(boolean lubricated) {
        this.lubricated = lubricated;
        setChanged();
        syncToClient();
    }

    // ===================== Rotational =====================

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
        return true;
    }

    @Override
    public Direction[] getPropagationDirections() {
        if (!hasShaft) return new Direction[0];
        Direction facing = getBlockState().getValue(BearingBlock.FACING);
        return new Direction[]{facing, facing.getOpposite()};
    }

    @Override
    public java.util.List<BlockPos> getPotentialConnections(Level level, BlockPos myPos) {
        if (!hasShaft) return java.util.Collections.emptyList();
        Direction facing = getBlockState().getValue(BearingBlock.FACING);
        return java.util.List.of(myPos.relative(facing), myPos.relative(facing.getOpposite()));
    }

    @Override
    public long getVisualSpeed() {
        if (!this.hasShaft) return 0;
        BlockState state = getBlockState();
        if (!state.hasProperty(BearingBlock.FACING)) return 0;
        Direction facing = state.getValue(BearingBlock.FACING);
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
        double baseInertia = 10.0;
        if (hasShaft && shaftMaterial != null && shaftDiameter != null) {
            baseInertia += (double) (shaftMaterial.baseInertia() * shaftDiameter.inertiaMod);
        }
        return baseInertia;
    }

    @Override
    public float getBearingFrictionCoefficient() {
        return lubricated ? 0.0f : 0.2f;
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

    // ===================== NBT =====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); // speed, lastSyncedSpeed, networkScale
        tag.putBoolean("HasShaft", this.hasShaft);
        tag.putBoolean("Lubricated", this.lubricated);
        if (this.hasShaft && this.shaftMaterial != null && this.shaftDiameter != null) {
            tag.putString("ShaftMaterial", this.shaftMaterial.name());
            tag.putString("ShaftDiameter", this.shaftDiameter.name());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag); // speed, lastSyncedSpeed, networkScale
        this.hasShaft = tag.getBoolean("HasShaft");
        this.lubricated = tag.getBoolean("Lubricated");
        if (this.hasShaft) {
            String matName = tag.getString("ShaftMaterial").toLowerCase();
            String diaName = tag.getString("ShaftDiameter");
            this.shaftMaterial = switch (matName) {
                case "duralumin" -> ShaftMaterial.DURALUMIN;
                case "steel"     -> ShaftMaterial.STEEL;
                case "titanium"  -> ShaftMaterial.TITANIUM;
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

    // ===================== РЕНДЕР =====================

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(1.5D);
    }
}