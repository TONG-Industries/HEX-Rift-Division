package com.trd.client.renderer.debug;


import com.trd.main.MainRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TurretDebugKeyHandler {

    public static boolean debugVisualizationEnabled = false;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // F3 = GLFW_KEY_F3
        if (event.getKey() == GLFW.GLFW_KEY_F3 && event.getAction() == GLFW.GLFW_PRESS) {
            debugVisualizationEnabled = !debugVisualizationEnabled;
        }
    }
}