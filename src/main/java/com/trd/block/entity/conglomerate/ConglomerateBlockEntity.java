package com.trd.block.entity.conglomerate;

import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

public class ConglomerateBlockEntity extends BlockEntity {
    public static final int OU_PER_CHARGE = 81;
    public static final int MAX_CHARGES = 10;

    private UUID veinId;
    private byte stage = 0;
    private byte charges = MAX_CHARGES;
    private boolean depleted = false;

    public ConglomerateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONGLOMERATE.get(), pos, state);
    }

    public int getRemainingOu() {
        return charges * OU_PER_CHARGE;
    }

    public byte getCharges() {
        return charges;
    }

    public void consumeCharge() {
        if (charges > 0) {
            charges--;
            stage = (byte) Math.min(3, (MAX_CHARGES - charges) / 3);
            setChanged();

            if (charges <= 0) {
                markDepleted();
            } else if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
    }

    public void setVeinId(UUID id) {
        this.veinId = id;
        setChanged();
    }

    public UUID getVeinId() {
        return veinId;
    }

    public void setStage(byte stage) {
        this.stage = (byte) Math.min(3, Math.max(0, stage));
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public byte getStage() {
        return stage;
    }

    public void markDepleted() {
        this.depleted = true;
        this.charges = 0;
        this.stage = 3;
        setChanged();
    }

    public boolean isDepleted() {
        return depleted || charges <= 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (veinId != null) tag.putUUID("VeinId", veinId);
        tag.putByte("Stage", stage);
        tag.putByte("Charges", charges);
        tag.putBoolean("Depleted", depleted);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("VeinId")) veinId = tag.getUUID("VeinId");

        if (tag.contains("BlockOu", CompoundTag.TAG_INT)) {
            int oldOu = tag.getInt("BlockOu");
            this.charges = (byte) Math.max(0, Math.min(MAX_CHARGES,
                    (oldOu + OU_PER_CHARGE - 1) / OU_PER_CHARGE));
            this.stage = (byte) Math.min(3, (MAX_CHARGES - this.charges) / 3);
        } else {
            this.charges = tag.contains("Charges") ? tag.getByte("Charges") : MAX_CHARGES;
            this.stage = tag.contains("Stage") ? tag.getByte("Stage") : 0;
        }

        depleted = tag.getBoolean("Depleted");
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