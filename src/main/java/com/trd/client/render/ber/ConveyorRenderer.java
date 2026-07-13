// com.trd.client.renderer.ConveyorRenderer.java
package com.trd.client.render.ber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trd.block.entity.industrial.ConveyorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ConveyorRenderer implements BlockEntityRenderer<ConveyorBlockEntity> {

    private final ItemRenderer itemRenderer;

    public ConveyorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(ConveyorBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        ItemStack stack = be.getDisplayedItem();
        if (stack.isEmpty()) return;

        poseStack.pushPose();

        // Перемещаем в центр блока + смещение конвейера
        poseStack.translate(0.5 + be.renderX, be.renderY, 0.5 + be.renderZ);

        // Поворачиваем предмет по направлению движения
        poseStack.mulPose(Axis.YP.rotationDegrees(-be.renderRotY + 90));

        // Небольшое покачивание/вращение для красоты
        float time = (be.getLevel().getGameTime() + partialTick) * 0.05f;
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 10));

        // Масштаб
        poseStack.scale(0.4f, 0.4f, 0.4f);

        BakedModel model = itemRenderer.getModel(stack, be.getLevel(), null, 0);
        itemRenderer.render(stack, ItemDisplayContext.FIXED, true, poseStack, buffer,
                packedLight, packedOverlay, model);

        poseStack.popPose();
    }
}