package com.trd.client.gecko.entity.mobs;


import com.trd.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import com.trd.entity.mobs.depth_worm.DepthWormEntity;
import software.bernie.geckolib.model.GeoModel;

public class DepthWormModel extends GeoModel<DepthWormEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(MainRegistry.MOD_ID, "geo/depth_worm.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(MainRegistry.MOD_ID, "textures/entity/depth_worm.png");
    private static final ResourceLocation TEXTURE_ATTACK = new ResourceLocation(MainRegistry.MOD_ID, "textures/entity/depth_worm_attack.png");
    private static final ResourceLocation ANIM = new ResourceLocation(MainRegistry.MOD_ID, "animations/depth_worm.animation.json");

    @Override
    public ResourceLocation getModelResource(DepthWormEntity entity) { return MODEL; }
    @Override
    public ResourceLocation getAnimationResource(DepthWormEntity entity) { return ANIM; }
    @Override
    public ResourceLocation getTextureResource(DepthWormEntity entity) {
        // Условие: открывает рот при атаке, в полете, при замахе ИЛИ когда получил урон (isAngry)
        if (entity.isAttacking() || entity.isFlying() || entity.swingTime > 0 || entity.isAngry()) {
            return TEXTURE_ATTACK;
        }
        return TEXTURE;
    }


}