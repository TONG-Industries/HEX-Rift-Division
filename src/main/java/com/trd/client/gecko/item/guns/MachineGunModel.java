package com.trd.client.gecko.item.guns;


import net.minecraft.resources.ResourceLocation;
import com.trd.item.guns.MachineGunItem;
import com.trd.main.MainRegistry;
import software.bernie.geckolib.model.GeoModel;

public class MachineGunModel extends GeoModel<MachineGunItem> {

    @Override
    public ResourceLocation getModelResource(MachineGunItem animatable) {
        return new ResourceLocation(MainRegistry.MOD_ID, "geo/machinegun.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MachineGunItem animatable) {
        return new ResourceLocation(MainRegistry.MOD_ID, "textures/item/machinegun.png");  // ✅ PNG, НЕ JPG!
    }

    @Override
    public ResourceLocation getAnimationResource(MachineGunItem animatable) {
        return new ResourceLocation(MainRegistry.MOD_ID, "animations/machinegun.animation.json");
    }
}
