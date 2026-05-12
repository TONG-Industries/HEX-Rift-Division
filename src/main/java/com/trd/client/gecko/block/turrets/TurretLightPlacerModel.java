package com.trd.client.gecko.block.turrets;


import com.trd.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import com.trd.block.entity.weapons.TurretLightPlacerBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class TurretLightPlacerModel extends GeoModel<TurretLightPlacerBlockEntity> {
    @Override
    public ResourceLocation getModelResource(TurretLightPlacerBlockEntity animatable) {
        // Имя файла: buffer_small.geo.json
        return new ResourceLocation(MainRegistry.MOD_ID, "geo/buffer_small.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TurretLightPlacerBlockEntity animatable) {
        // Имя файла: buffer_light.jpg (лучше переименовать в .png, Minecraft любит png)
        return new ResourceLocation(MainRegistry.MOD_ID, "textures/block/buffer_light.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TurretLightPlacerBlockEntity animatable) {
        // Если анимаций нет, можно вернуть null или заглушку
        return new ResourceLocation(MainRegistry.MOD_ID, "animations/turret_light_placer.animation.json");
    }
}
