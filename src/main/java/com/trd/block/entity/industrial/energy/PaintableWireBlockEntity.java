package com.trd.block.entity.industrial.energy;

import com.trd.api.paint.IPaintableConduit;
import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity окрашиваемого провода.
 * Наследует WireBlockEntity => полностью работает в энергосети
 * (капабилити ENERGY_CONNECTOR, onLoad/setRemoved), плюс хранит "мимик" + синк на клиент.
 */
public class PaintableWireBlockEntity extends WireBlockEntity implements IPaintableConduit {

    private BlockState mimic = null;

    public PaintableWireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PAINTABLE_WIRE_BE.get(), pos, state);
    }

    @Override
    public BlockState getMimicState() {
        return this.mimic;
    }

    public void setMimic(BlockState state) {
        this.mimic = (state != null && state.isAir()) ? null : state;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.mimic != null) {
            tag.put("Mimic", NbtUtils.writeBlockState(this.mimic));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Mimic")) {
            BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Mimic"));
            this.mimic = (loaded != null && loaded.isAir()) ? null : loaded;
        } else {
            this.mimic = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) this.load(tag);
        if (this.level != null && this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
    @Override
    public net.minecraft.resources.ResourceLocation getCoreTexture() {
        return new net.minecraft.resources.ResourceLocation("trd", "block/conduit_core_wire");
    }
}
