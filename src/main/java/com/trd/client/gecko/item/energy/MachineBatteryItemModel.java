package com.trd.client.gecko.item.energy;

import com.trd.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import com.trd.item.energy.MachineBatteryBlockItem;
import software.bernie.geckolib.model.GeoModel;

public class MachineBatteryItemModel extends GeoModel<MachineBatteryBlockItem> {

    @Override
    public ResourceLocation getModelResource(MachineBatteryBlockItem animatable) {
        return new ResourceLocation(MainRegistry.MOD_ID, "geo/machine_battery.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MachineBatteryBlockItem animatable) {
        return new ResourceLocation(MainRegistry.MOD_ID, "textures/block/machine_battery.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MachineBatteryBlockItem animatable) {
        return new ResourceLocation(MainRegistry.MOD_ID, "animations/empty.animation.json");
    }
}
