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
            FluidNetworkManager manager = FluidNetworkManager.get((ServerLevel) this.level);
            if (!wasNetworked && isNetworked) {
                if (!manager.hasNode(this.getBlockPos())) {
                    manager.addNode(this.getBlockPos());
                }
            } else if (wasNetworked && !isNetworked) {
                manager.removeNode(this.getBlockPos());
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
            FluidNetworkManager manager = FluidNetworkManager.get((ServerLevel) this.level);
            if (!manager.hasNode(this.getBlockPos())) {
                manager.addNode(this.getBlockPos());
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide && isNetworkedRole(this.role)) {
            FluidNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }

    private static boolean isNetworkedRole(PartRole role) {
        return role == PartRole.FLUID_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (controllerPos != null) tag.putLong("ControllerPos", controllerPos.asLong());
        tag.putString("Role", role.getSerializedName());
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && role == PartRole.FLUID_CONNECTOR && controllerPos != null && level != null) {
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof IFluidTankProvider provider) {
                return provider.getFluidHandlerCapability().cast();
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