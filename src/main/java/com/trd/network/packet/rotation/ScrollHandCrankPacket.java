package com.trd.network.packet.rotation;

import com.trd.block.entity.industrial.rotation.HandCrankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScrollHandCrankPacket {

    private final BlockPos pos;
    private final int scrollDelta;

    public ScrollHandCrankPacket(BlockPos pos, int scrollDelta) {
        this.pos = pos;
        this.scrollDelta = scrollDelta;
    }

    public ScrollHandCrankPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.scrollDelta = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.scrollDelta);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // Security check: ensure player is somewhat near the block
                if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0) { // 8 blocks distance
                    BlockEntity be = player.serverLevel().getBlockEntity(pos);
                    if (be instanceof HandCrankBlockEntity crank) {
                        crank.addScroll(this.scrollDelta);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
