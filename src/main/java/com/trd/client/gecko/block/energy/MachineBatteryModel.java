package com.trd.client.gecko.block.energy;

import com.trd.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import com.trd.block.entity.industrial.energy.MachineBatteryBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class MachineBatteryModel extends GeoModel<MachineBatteryBlockEntity> {

    // Путь к .geo.json — кладёшь в:
    // src/main/resources/assets/trd/geo/machine_battery.geo.json
    private static final ResourceLocation MODEL =
            new ResourceLocation(MainRegistry.MOD_ID, "geo/machine_battery.geo.json");

    // Путь к текстуре — кладёшь в:
    // src/main/resources/assets/trd/textures/block/machine_battery.png
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MainRegistry.MOD_ID, "textures/block/machine_battery.png");

    @Override
    public ResourceLocation getModelResource(MachineBatteryBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MachineBatteryBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MachineBatteryBlockEntity animatable) {
        // Нет анимаций — возвращаем пустой ресурс
        // GeckoLib это нормально принимает для статичных моделей
        return new ResourceLocation(MainRegistry.MOD_ID, "animations/empty.animation.json");
    }
}