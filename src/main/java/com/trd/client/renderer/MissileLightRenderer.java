package com.trd.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trd.block.basic.ModBlocks;
import com.trd.entity.weapons.missiles.MissileLightEntity;
import com.trd.main.MainRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class MissileLightRenderer extends EntityRenderer<MissileLightEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public MissileLightRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.3f;
    }

    @Override
    public void render(MissileLightEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        // Центрируем модель
        poseStack.translate(-0.5, 0, -0.5);

        // Масштабируем под размер ракеты (блок 1x1x1 → 0.4x1.2)
        poseStack.scale(0.4f, 1.2f, 0.4f);

        // Поворачиваем по направлению движения
        // Сначала центрируем точку вращения
        poseStack.translate(0.5, 0.5, 0.5);

        // Применяем повороты
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() + 180));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

        // Возвращаем центр
        poseStack.translate(-0.5, -0.5, -0.5);

        // Рендерим блок MISSILE_LIGHT
        BlockState state = ModBlocks.MISSILE_LIGHT.get().defaultBlockState();

        blockRenderer.renderSingleBlock(
                state,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                null
        );

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MissileLightEntity entity) {
        // Не используется, т.к. рендерим как блок
        return new ResourceLocation(MainRegistry.MOD_ID, "block/missile_light");
    }
}