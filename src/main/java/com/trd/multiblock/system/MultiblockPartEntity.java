package com.trd.multiblock.system;

import com.trd.api.fluids.system.FluidNetworkManager;
import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public class MultiblockPartEntity extends BlockEntity implements IMultiblockPart {

    private BlockPos controllerPos;
    private PartRole role = PartRole.DEFAULT;
    private Set<Direction> allowedClimbSides = EnumSet.noneOf(Direction.class);

    public MultiblockPartEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTIBLOCK_PART.get(), pos, state);
    }

    @Nullable
    @Override
    public BlockPos getControllerPos() { return controllerPos; }

    @Override
    public void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void setPartRole(PartRole role) {
        boolean wasNetworked = isNetworkedRole(this.role);
        boolean isNetworked  = isNetworkedRole(role);

        this.role = role;
        setChanged();

        if (this.level != null && !this.level.isClientSide) {
            FluidNetworkManager fluidManager = FluidNetworkManager.get((ServerLevel) this.level);
            com.trd.api.energy.EnergyNetworkManager energyManager = com.trd.api.energy.EnergyNetworkManager.get((ServerLevel) this.level);
            
            if (!wasNetworked && isNetworked) {
                if (role == PartRole.FLUID_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR || role == PartRole.FLUID_INPUT || role == PartRole.FLUID_OUTPUT || role == PartRole.FLUID_LADDER) {
                    if (!fluidManager.hasNode(this.getBlockPos())) fluidManager.addNode(this.getBlockPos());
                }
                if (role == PartRole.ENERGY_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR) {
                    if (!energyManager.hasNode(this.getBlockPos())) energyManager.addNode(this.getBlockPos());
                }
            } else if (wasNetworked && !isNetworked) {
                fluidManager.removeNode(this.getBlockPos());
                energyManager.removeNode(this.getBlockPos());
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public PartRole getPartRole() { return role; }

    @Override
    public void setAllowedClimbSides(Set<Direction> sides) { this.allowedClimbSides = sides; }

    @Override
    public Set<Direction> getAllowedClimbSides() { return allowedClimbSides; }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide && isNetworkedRole(this.role)) {
            if (role == PartRole.FLUID_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR || role == PartRole.FLUID_INPUT || role == PartRole.FLUID_OUTPUT || role == PartRole.FLUID_LADDER) {
                FluidNetworkManager fluidManager = FluidNetworkManager.get((ServerLevel) this.level);
                if (!fluidManager.hasNode(this.getBlockPos())) fluidManager.addNode(this.getBlockPos());
            }
            if (role == PartRole.ENERGY_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR) {
                com.trd.api.energy.EnergyNetworkManager energyManager = com.trd.api.energy.EnergyNetworkManager.get((ServerLevel) this.level);
                if (!energyManager.hasNode(this.getBlockPos())) energyManager.addNode(this.getBlockPos());
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide && isNetworkedRole(this.role)) {
            FluidNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
            com.trd.api.energy.EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }

    private static boolean isNetworkedRole(PartRole role) {
        return role == PartRole.FLUID_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR || role == PartRole.ENERGY_CONNECTOR || role == PartRole.FLUID_INPUT || role == PartRole.FLUID_OUTPUT || role == PartRole.FLUID_LADDER;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (controllerPos != null) tag.putLong("ControllerPos", controllerPos.asLong());
        tag.putString("Role", role.getSerializedName());
        
        byte climbMask = 0;
        for (Direction d : allowedClimbSides) {
            climbMask |= (1 << d.ordinal());
        }
        tag.putByte("ClimbSides", climbMask);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (controllerPos != null && level != null) {
            if (cap == ForgeCapabilities.FLUID_HANDLER && (role == PartRole.FLUID_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR || role == PartRole.FLUID_INPUT || role == PartRole.FLUID_OUTPUT || role == PartRole.FLUID_LADDER)) {
                BlockEntity be = level.getBlockEntity(controllerPos);
                if (be instanceof com.trd.multiblock.industrial.BoilerBlockEntity boiler) {
                    return boiler.getCapabilityForPart(cap, side, role);
                } else if (be instanceof com.trd.multiblock.industrial.FuelTankSmallBlockEntity smallTank) {
                    return smallTank.getCapabilityForPart(cap, side, role);
                } else if (be instanceof IFluidTankProvider provider) {
                    return provider.getFluidHandlerCapability().cast();
                }
            } else if ((cap == com.trd.capability.ModCapabilities.ENERGY_PROVIDER || cap == com.trd.capability.ModCapabilities.ENERGY_CONNECTOR) 
                        && (role == PartRole.ENERGY_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR)) {
                BlockEntity be = level.getBlockEntity(controllerPos);
                if (be != null) {
                    return be.getCapability(cap, side);
                }
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("ControllerPos")) controllerPos = BlockPos.of(tag.getLong("ControllerPos"));
        String roleName = tag.getString("Role");
        for (PartRole r : PartRole.values()) {
            if (r.getSerializedName().equals(roleName)) {
                this.role = r; break;
            }
        }
        
        if (tag.contains("ClimbSides")) {
            byte mask = tag.getByte("ClimbSides");
            allowedClimbSides.clear();
            for (Direction d : Direction.values()) {
                if ((mask & (1 << d.ordinal())) != 0) {
                    allowedClimbSides.add(d);
                }
            }
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}