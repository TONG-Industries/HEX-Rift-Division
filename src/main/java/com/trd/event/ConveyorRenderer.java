package com.trd.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.trd.block.basic.industrial.ConveyorBlock;
import com.trd.block.entity.industrial.ConveyorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
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

        // Центр блока + смещение по направлению движения
        poseStack.translate(0.5 + be.renderX, be.renderY, 0.5 + be.renderZ);

        Direction facing = be.getBlockState().getValue(ConveyorBlock.FACING);

        // Поворачиваем по направлению конвейера
        poseStack.mulPose(Axis.YP.rotationDegrees(-be.renderRotY + 90));

        // ЛЕЖАЩЕЕ ПОЛОЖЕНИЕ — как в CastingPotRenderer
        poseStack.mulPose(Axis.XP.rotationDegrees(90));

        // Размер 12×12 пикселей = 0.75 от 16
        float scale = 0.75f;
        poseStack.scale(scale, scale, scale);

        BakedModel model = itemRenderer.getModel(stack, be.getLevel(), null, 0);
        itemRenderer.render(stack, ItemDisplayContext.FIXED, true, poseStack, buffer,
                packedLight, packedOverlay, model);

        poseStack.popPose();
    }
}