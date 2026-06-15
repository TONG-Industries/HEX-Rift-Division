package com.trd.client.render.ber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trd.multiblock.industrial.FuelTankBlock;
import com.trd.multiblock.industrial.FuelTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.registries.ForgeRegistries;

public class FuelTankRenderer implements BlockEntityRenderer<FuelTankBlockEntity> {

    public FuelTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FuelTankBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (be.fluidFilter == null || be.fluidFilter.equals("none")) return;

        Fluid filterFluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(be.fluidFilter));
        if (filterFluid == null || filterFluid == Fluids.EMPTY) return;

        BlockState state = be.getBlockState();
        if (!state.hasProperty(FuelTankBlock.FACING)) return;
        Direction facing = state.getValue(FuelTankBlock.FACING);

        Component fluidName = Component.translatable(filterFluid.getFluidType().getDescriptionId());
        int tintColor = IClientFluidTypeExtensions.of(filterFluid).getTintColor();
        
        if (filterFluid == Fluids.LAVA || filterFluid == Fluids.FLOWING_LAVA) {
            tintColor = 0xFF5500; // Огненно-оранжевый
        } else if (filterFluid == Fluids.WATER || filterFluid == Fluids.FLOWING_WATER) {
            tintColor = 0x3F76E4; // Синий
        }
        
        // Убираем прозрачность
        tintColor = 0xFF000000 | tintColor;

        poseStack.pushPose();

        // Центрируемся в контроллере
        poseStack.translate(0.5, 0.5, 0.5);

        Font font = Minecraft.getInstance().font;
        float width = font.width(fluidName);

        // Рисуем с двух сторон (спереди и сзади)
        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();

            // Поворачиваем текст в зависимости от направления мультиблока
            // i == 0 - спереди, i == 1 - сзади
            float rot = -facing.toYRot();
            if (i == 1) {
                rot += 180;
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(rot));

            // Смещение:
            // Y: на 1 блок выше (Y = 1.0)
            // Z: 1.5 блока - 2 пикселя (0.125 блока) = 1.375. Добавляем 0.001 для z-fighting = 1.376f
            poseStack.translate(0, 1.0f, 1.376f); 
            
            // Максимальная ширина для текста - 1.8 блока
            // Максимальная высота - 12 пикселей (0.75 блока)
            float maxWidth = 1.8f;
            float maxHeight = 12.0f / 16.0f; // 0.75f
            float baseScale = 0.06f;
            
            String str = fluidName.getString();
            java.util.List<String> textLines = new java.util.ArrayList<>();
            
            // Если текст в одну строку шире maxWidth и в нем есть пробелы - бьем на 2 строки по центру
            if (font.width(str) * baseScale > maxWidth && str.contains(" ")) {
                int mid = str.length() / 2;
                int bestSpace = -1;
                int minDiff = Integer.MAX_VALUE;
                for (int j = 0; j < str.length(); j++) {
                    if (str.charAt(j) == ' ') {
                        int diff = Math.abs(j - mid);
                        if (diff < minDiff) {
                            minDiff = diff;
                            bestSpace = j;
                        }
                    }
                }
                if (bestSpace != -1) {
                    textLines.add(str.substring(0, bestSpace));
                    textLines.add(str.substring(bestSpace + 1));
                } else {
                    textLines.add(str);
                }
            } else {
                textLines.add(str);
            }
            
            // Находим реальную максимальную ширину строки
            int maxLineW = 0;
            for (String line : textLines) {
                int w = font.width(line);
                if (w > maxLineW) maxLineW = w;
            }
            
            float dynamicScale = baseScale;
            if (maxLineW > 0) {
                dynamicScale = Math.min(baseScale, maxWidth / (float)maxLineW);
            }
            
            // Ограничение по высоте (максимум 12 пикселей блока)
            float heightScale = maxHeight / (textLines.size() * font.lineHeight);
            dynamicScale = Math.min(dynamicScale, heightScale);

            // Масштабируем и переворачиваем текст
            poseStack.scale(dynamicScale, -dynamicScale, dynamicScale);

            // Расчет позиций для текста слева и справа
            float leftX = -1.5f / dynamicScale;
            float rightX = 1.5f / dynamicScale;
            
            // Вычисляем стартовый Y для вертикального центрирования всего блока текста
            float totalHeight = textLines.size() * font.lineHeight;
            float startY = -totalHeight / 2f;
            
            // Рисуем каждую строку
            for (int j = 0; j < textLines.size(); j++) {
                String line = textLines.get(j);
                float w = font.width(line);
                float yOffset = startY + j * font.lineHeight;
                
                // Рисуем слева
                font.drawInBatch(line, leftX - w / 2f, yOffset, tintColor, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
                // Рисуем справа
                font.drawInBatch(line, rightX - w / 2f, yOffset, tintColor, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }
    
    @Override
    public boolean shouldRenderOffScreen(FuelTankBlockEntity pBlockEntity) {
        return true;
    }
}
