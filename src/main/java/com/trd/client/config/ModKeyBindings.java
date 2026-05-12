package com.trd.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final KeyMapping RELOAD_KEY = new KeyMapping(
            "key.trd.reload",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.trd"
    );
    public static final KeyMapping UNLOAD_KEY = new KeyMapping(
            "key.trd.unload", // Название в настройках
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, // Кнопка G по умолчанию
            "key.categories.trd"
    );


}
