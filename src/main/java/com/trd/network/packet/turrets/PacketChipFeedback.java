package com.trd.network.packet.turrets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.trd.client.overlay.gui.GUITurretAmmo;
import com.trd.client.overlay.gui.GUITrombone;

import java.util.function.Supplier;

public class PacketChipFeedback {
    private final boolean success;

    public PacketChipFeedback(boolean success) {
        this.success = success;
    }

    public PacketChipFeedback(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            net.minecraft.client.gui.screens.Screen currentScreen = Minecraft.getInstance().screen;
            if (currentScreen instanceof GUITurretAmmo gui) {
                gui.handleFeedback(success);
            } else if (currentScreen instanceof GUITrombone gui) {
                gui.handleFeedback(success);
            }
        });
        context.setPacketHandled(true);
    }
}