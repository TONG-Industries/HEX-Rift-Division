package com.trd.client.handler;


import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.trd.api.energy.ILongEnergyMenu;
import com.trd.menu.MachineBatteryMenu;

@OnlyIn(Dist.CLIENT)
public class ClientEnergySyncHandler {
    public static void handle(int containerId, long energy, long maxEnergy, long delta,
                              long chargingSpeed, long unchargingSpeed, int filledCellCount) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu != null) {
            if (player.containerMenu.containerId == containerId &&
                    player.containerMenu instanceof ILongEnergyMenu menu) {
                menu.setEnergy(energy, maxEnergy, delta);

                // Передаём доп. данные если это MachineBatteryMenu
                if (player.containerMenu instanceof MachineBatteryMenu batteryMenu) {
                    batteryMenu.setExtraData(chargingSpeed, unchargingSpeed, filledCellCount);
                }
            }
        }
    }
}