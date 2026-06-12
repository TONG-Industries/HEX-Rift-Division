package com.trd.network.packet.turrets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import com.trd.block.entity.weapons.MissileTurretBlockEntity;

import java.util.function.Supplier;

public class PacketToggleExtraButton {
    private final BlockPos pos;
    private final int buttonId; // 1 или 2

    public PacketToggleExtraButton(BlockPos pos, int buttonId) {
        this.pos = pos;
        this.buttonId = buttonId;
    }

    public PacketToggleExtraButton(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.buttonId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(buttonId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof MissileTurretBlockEntity turretBE) {
                    turretBE.toggleExtraButton(buttonId);
                }
            }
        });
        return true;
    }
}