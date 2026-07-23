package com.trd.network;


import com.trd.network.packet.fluids.*;
//import com.trd.network.packet.rotation.PacketToggleRetractMode;
import com.trd.network.packet.turrets.*;
import com.trd.network.packet.rotation.ScrollHandCrankPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import com.trd.main.MainRegistry;
import com.trd.network.packet.activators.ClearPointPacket;
import com.trd.network.packet.activators.DetonateAllPacket;
import com.trd.network.packet.activators.SetActivePointPacket;
import com.trd.network.packet.activators.SyncPointPacket;
import com.trd.network.packet.energy.PacketSyncEnergy;
import com.trd.network.packet.energy.UpdateBatteryC2SPacket;
import com.trd.network.packet.guns.PacketReloadGun;
import com.trd.network.packet.guns.PacketShoot;
import com.trd.network.packet.guns.PacketUnloadGun;
//import com.trd.network.packet.rotation.PacketToggleMotor;
//import com.trd.network.packet.rotation.PacketToggleMotorMode;
//import com.trd.network.packet.rotation.PacketToggleShaftPlacer;


public class ModPacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MainRegistry.MOD_ID, "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;


        INSTANCE.registerMessage(id++, UpdateBatteryC2SPacket.class, UpdateBatteryC2SPacket::toBytes, UpdateBatteryC2SPacket::new, (msg, ctx) -> msg.handle(ctx));

        INSTANCE.registerMessage(id++,
                PacketSyncEnergy.class,
                PacketSyncEnergy::encode,
                PacketSyncEnergy::decode,
                PacketSyncEnergy::handle
        );

        INSTANCE.registerMessage(id++, UpdateBarrelModeC2SPacket.class,
                UpdateBarrelModeC2SPacket::toBytes,
                UpdateBarrelModeC2SPacket::new,
                UpdateBarrelModeC2SPacket::handle);

        INSTANCE.registerMessage(id++,
                PacketReloadGun.class,
                PacketReloadGun::toBytes,
                PacketReloadGun::new,
                PacketReloadGun::handle
        );

        // === ДОБАВЬТЕ ЭТОТ ПАКЕТ ДЛЯ СТРЕЛЬБЫ ===
        INSTANCE.registerMessage(id++,
                PacketShoot.class,
                PacketShoot::toBytes,
                PacketShoot::new,
                PacketShoot::handle
        );

        INSTANCE.registerMessage(id++,
                PacketUnloadGun.class,
                PacketUnloadGun::toBytes,
                PacketUnloadGun::new,
                PacketUnloadGun::handle
        );

//        INSTANCE.registerMessage(id++,
//                PacketToggleMotor.class,
//                PacketToggleMotor::encode,
//                PacketToggleMotor::new,
//                PacketToggleMotor::handle
//        );
//
//        INSTANCE.registerMessage(id++,
//                PacketToggleMotorMode.class,
//                PacketToggleMotorMode::encode,
//                PacketToggleMotorMode::new,
//                PacketToggleMotorMode::handle
//        );

        INSTANCE.registerMessage(id++,
                PacketModifyTurretChip.class,
                PacketModifyTurretChip::toBytes,
                PacketModifyTurretChip::new,
                PacketModifyTurretChip::handle
        );

        INSTANCE.registerMessage(id++,
                PacketChipFeedback.class,
                PacketChipFeedback::toBytes,
                PacketChipFeedback::new,
                PacketChipFeedback::handle
        );

        INSTANCE.registerMessage(id++,
                PacketUpdateTurretSettings.class,
                PacketUpdateTurretSettings::toBytes,
                PacketUpdateTurretSettings::new,
                PacketUpdateTurretSettings::handle
        );

        INSTANCE.registerMessage(id++,
                PacketToggleTurret.class,
                PacketToggleTurret::toBytes,
                PacketToggleTurret::new,
                PacketToggleTurret::handle
        );

        INSTANCE.registerMessage(id++,
                DetonateAllPacket.class,
                DetonateAllPacket::encode,
                DetonateAllPacket::decode,
                DetonateAllPacket::handle
        );

        INSTANCE.registerMessage(id++,
                SetActivePointPacket.class,
                SetActivePointPacket::encode,
                SetActivePointPacket::decode,
                SetActivePointPacket::handle
        );

        INSTANCE.registerMessage(id++,
                ClearPointPacket.class,
                ClearPointPacket::encode,
                ClearPointPacket::decode,
                ClearPointPacket::handle
        );

        INSTANCE.registerMessage(id++,
                SyncPointPacket.class,
                SyncPointPacket::encode,
                SyncPointPacket::decode,
                SyncPointPacket::handle
        );

//        INSTANCE.registerMessage(id++,
//                PacketToggleShaftPlacer.class,
//                PacketToggleShaftPlacer::encode,
//                PacketToggleShaftPlacer::decode,
//                PacketToggleShaftPlacer::handle
//        );
//        INSTANCE.registerMessage(id++,
//                PacketToggleRetractMode.class,
//                PacketToggleRetractMode::encode,
//                PacketToggleRetractMode::decode,
//                PacketToggleRetractMode::handle);

        INSTANCE.registerMessage(id++,
                ClearFluidHistoryPacket.class,
                ClearFluidHistoryPacket::toBytes,
                ClearFluidHistoryPacket::new,
                ClearFluidHistoryPacket::handle);

        INSTANCE.registerMessage(id++,
                ToggleFavoriteFluidPacket.class,
                ToggleFavoriteFluidPacket::toBytes,
                ToggleFavoriteFluidPacket::new,
                ToggleFavoriteFluidPacket::handle);

        INSTANCE.registerMessage(id++,
                SelectFluidPacket.class,
                SelectFluidPacket::toBytes,
                SelectFluidPacket::new,
                SelectFluidPacket::handle);

        INSTANCE.registerMessage(id++,
                com.trd.network.packet.energy.SyncMotorRpmPacket.class,
                com.trd.network.packet.energy.SyncMotorRpmPacket::encode,
                com.trd.network.packet.energy.SyncMotorRpmPacket::decode,
                com.trd.network.packet.energy.SyncMotorRpmPacket::handle
        );
        INSTANCE.registerMessage(id++, ScrollHandCrankPacket.class,
                ScrollHandCrankPacket::toBytes,
                ScrollHandCrankPacket::new,
                (msg, ctx) -> { msg.handle(ctx); ctx.get().setPacketHandled(true); });
        INSTANCE.registerMessage(id++, PacketToggleExtraButton.class,
                PacketToggleExtraButton::toBytes,
                PacketToggleExtraButton::new,
                (msg, ctx) -> { msg.handle(ctx); ctx.get().setPacketHandled(true); });
    }
}
