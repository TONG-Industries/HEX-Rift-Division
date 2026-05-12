package com.trd.network.packet.energy;

import com.trd.api.rotation.KineticNetwork;
import com.trd.api.rotation.KineticNetworkManager;
import com.trd.block.entity.industrial.rotation.MotorElectroBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S пакет: клиент → сервер, синхронизирует targetRpm мотора.
 */
public class SyncMotorRpmPacket {

    private final BlockPos pos;
    private final int rpm;

    public SyncMotorRpmPacket(BlockPos pos, int rpm) {
        this.pos = pos;
        this.rpm = rpm;
    }

    // ===================== КОДИРОВАНИЕ =====================

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(rpm);
    }

    public static SyncMotorRpmPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int rpm = buf.readInt();
        return new SyncMotorRpmPacket(pos, rpm);
    }

    // ===================== ОБРАБОТЧИК =====================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            ServerLevel level = sender.serverLevel();

            if (!(level.getBlockEntity(pos) instanceof MotorElectroBlockEntity motor)) return;

            // Клamp и rounded до десятков выполнен внутри setTargetRpm
            motor.setTargetRpm(rpm);
            motor.setChanged();

            // Пересчитываем кинетическую сеть
            KineticNetwork net = KineticNetworkManager.get(level).getNetworkFor(pos);
            if (net != null) {
                net.requestRecalculation();
            }
        });
        ctx.setPacketHandled(true);
    }
}
