package com.trd.client.render.ber;

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

import java.util.List;

public class ConveyorRenderer implements BlockEntityRenderer<ConveyorBlockEntity> {

    private final ItemRenderer itemRenderer;

    public ConveyorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(ConveyorBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        List<ConveyorBlockEntity.ConveyorItem> currentItems = be.getClientItems();
        List<ConveyorBlockEntity.ConveyorItem> prevItems = be.getPrevClientItems();
        if (currentItems.isEmpty()) return;

        Direction facing = be.getBlockState().getValue(ConveyorBlock.FACING);

        // Проходим по текущим предметам. Если в предыдущем списке нет такого же (по индексу или по стеку) – используем текущий прогресс как есть.
        // Упростим: предполагаем, что порядок предметов сохраняется. Если нет – можно сопоставлять по стеку, но для простоты используем индекс.
        int size = Math.min(currentItems.size(), prevItems.size());
        for (int i = 0; i < currentItems.size(); i++) {
            ConveyorBlockEntity.ConveyorItem curr = currentItems.get(i);
            ConveyorBlockEntity.ConveyorItem prev = (i < prevItems.size()) ? prevItems.get(i) : curr;

            ItemStack stack = curr.stack;
            if (stack.isEmpty()) continue;

            // Интерполяция прогресса
            double progress = prev.progress + (curr.progress - prev.progress) * partialTick;
            // Ограничение [0, 1]
            progress = Math.max(0.0, Math.min(1.0, progress));

            double offset = (progress - 0.5) * 0.9; // чуть больше отступ, чтобы не вылезать
            float x = 0.5f + facing.getStepX() * (float) offset;
            float z = 0.5f + facing.getStepZ() * (float) offset;
            float y = 0.5f + (float) ConveyorBlockEntity.ITEM_Y_OFFSET; // 0.65625

            poseStack.pushPose();
            poseStack.translate(x, y, z);

            // Поворот по направлению конвейера
            float rotY = facing.toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(-rotY + 90));

            // Укладываем плашмя, как в литейном котле
            poseStack.mulPose(Axis.XP.rotationDegrees(90));

            // Увеличение размера в 2.5 раза: базовый 0.35 * 2.5 = 0.875
            float scale = 0.875f;
            poseStack.scale(scale, scale, scale);

            BakedModel model = itemRenderer.getModel(stack, be.getLevel(), null, 0);
            itemRenderer.render(stack, ItemDisplayContext.GROUND, false, poseStack, buffer,
                    packedLight, packedOverlay, model);

            // Дополнительные спрайты для стака (>1)
            int count = stack.getCount();
            if (count > 1) {
                int copies = Math.min(count - 1, 3);
                for (int j = 0; j < copies; j++) {
                    poseStack.pushPose();
                    double angle = (j * 0.8 + 0.2) * Math.PI * 2;
                    float dx = (float) (Math.cos(angle) * 0.08);
                    float dz = (float) (Math.sin(angle) * 0.08);
                    poseStack.translate(dx, 0.0, dz);
                    poseStack.mulPose(Axis.ZP.rotationDegrees((float) (Math.random() * 10 - 5)));
                    itemRenderer.render(stack, ItemDisplayContext.GROUND, false, poseStack, buffer,
                            packedLight, packedOverlay, model);
                    poseStack.popPose();
                }
            }

            poseStack.popPose();
        }
    }
}