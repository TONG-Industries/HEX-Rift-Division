package com.trd.client.gecko.entity.mobs;

import com.trd.entity.mobs.depth_worm.DepthWormBrutalEntity;
import com.trd.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DepthWormBrutalModel extends GeoModel<DepthWormBrutalEntity> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(MainRegistry.MOD_ID, "geo/depth_worm_brutal.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MainRegistry.MOD_ID, "textures/entity/depth_worm_brutal.png");
    private static final ResourceLocation TEXTURE_ATTACK =
            new ResourceLocation(MainRegistry.MOD_ID, "textures/entity/depth_worm_brutal_attack.png");
    private static final ResourceLocation ANIM =
            new ResourceLocation(MainRegistry.MOD_ID, "animations/depth_worm_brutal.animation.json");

    @Override
    public ResourceLocation getModelResource(DepthWormBrutalEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getAnimationResource(DepthWormBrutalEntity entity) {
        return ANIM;
    }

    @Override
    public ResourceLocation getTextureResource(DepthWormBrutalEntity entity) {
        // Текстура атаки: prepare, jump (flying), обычная атака, злость
        if (entity.isPreparingJump()
                || entity.isFlying()
                || entity.isAttacking()
                || entity.isAngry()
                || entity.isImpaling()) {
            return TEXTURE_ATTACK;
        }
        return TEXTURE;
    }
}