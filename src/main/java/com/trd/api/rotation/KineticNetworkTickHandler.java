package com.trd.api.rotation;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.trd.main.MainRegistry; // Замени на свой главный класс мода

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KineticNetworkTickHandler {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        // Нам нужен только серверный тик, причем выполняем один раз в конце фазы [cite: 23]
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            KineticNetworkManager.get(serverLevel).tickAllNetworks(); // [cite: 24]
        }
    }
}
