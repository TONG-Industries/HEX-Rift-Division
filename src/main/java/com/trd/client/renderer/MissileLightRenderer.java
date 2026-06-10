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

        // === БЕЗ МАСШТАБИРОВАНИЯ — рендерим как обычный блок 1x1x1 ===
        // Центрируем блок на позиции сущности
        poseStack.translate(-0.5, -0.5, -0.5);

        // === ПОВОРОТ: верхушка блока (= +Y) смотрит по направлению движения ===
        // Интерполяция для плавности
        float yaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTick;
        float pitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTick;

        // Сначала центрируем точку вращения в центре блока
        poseStack.translate(0.5, 0.5, 0.5);

        // Поворачиваем: верхушка (+Y) → направление движения
        // Сначала yaw (вокруг Y)
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw + 180));
        // Затем pitch (вокруг X) — наклоняем верхушку вперёд
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch - 90));

        // Возвращаем центр
        poseStack.translate(-0.5, -0.5, -0.5);

        // Рендерим блок БЕЗ масштабирования
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
        return new ResourceLocation(MainRegistry.MOD_ID, "block/missile_light");
    }
}