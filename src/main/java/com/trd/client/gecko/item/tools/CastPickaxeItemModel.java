package com.trd.client.gecko.item.tools;


import com.trd.item.tools.cast_pickaxes.CastPickaxeItem;
import com.trd.item.tools.cast_pickaxes.materials.CastPickaxeSteelItem;
import com.trd.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CastPickaxeItemModel extends GeoModel<CastPickaxeItem> {

    @Override
    public ResourceLocation getModelResource(CastPickaxeItem animatable) {
        if (animatable instanceof CastPickaxeSteelItem) {
            return new ResourceLocation(MainRegistry.MOD_ID, "geo/cast_pickaxe_steel.geo.json");
        }
        return new ResourceLocation(MainRegistry.MOD_ID, "geo/cast_pickaxe_iron.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CastPickaxeItem animatable) {
        if (animatable instanceof CastPickaxeSteelItem) {
            return new ResourceLocation(MainRegistry.MOD_ID, "textures/item/cast_pickaxe_steel.png");
        }
        return new ResourceLocation(MainRegistry.MOD_ID, "textures/item/cast_pickaxe_iron.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CastPickaxeItem animatable) {
        return new ResourceLocation(MainRegistry.MOD_ID, "animations/cast_pickaxe_iron.animation.json");
    }

    /**
     * Переопределяем для динамической скорости анимации
     */
    @Override
    public void setCustomAnimations(CastPickaxeItem animatable, long instanceId, software.bernie.geckolib.core.animation.AnimationState<CastPickaxeItem> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // Устанавливаем скорость анимации на основе времени зарядки
        float speed = animatable.getAnimationSpeed();
        if (speed != 1.0f) {
            animationState.getController().setAnimationSpeed(speed);
        }
    }
}