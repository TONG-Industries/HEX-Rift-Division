package com.trd.client.overlay.hud;

import com.trd.block.basic.industrial.rotation.TachometerBlock;
import com.trd.block.entity.industrial.rotation.TachometerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TachometerOverlay {

    public static final IGuiOverlay HUD_TACHOMETER = (ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Проверяем, что игрок смотрит на блок
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);

        // Проверяем, что это тахометр
        if (!(state.getBlock() instanceof TachometerBlock)) return;

        // Получаем BlockEntity на клиенте
        if (!(mc.level.getBlockEntity(pos) instanceof TachometerBlockEntity tachometer)) return;

        // Позиция текста — справа от перекрестия
        int centerX = screenWidth / 2 + 15;
        int centerY = screenHeight / 2 - 20;

        int lineHeight = 12;
        int bgColor = 0x80000000; // Полупрозрачный черный фон
        int headerColor = 0xFFFFAA00; // Оранжевый заголовок
        int valueColor = 0xFFFFFFFF; // Белый текст
        int noShaftColor = 0xFFFF5555; // Красный для предупреждения

        if (!tachometer.hasShaft()) {
            // Нет вала — показываем предупреждение
            String noShaft = "⚠ No Shaft Inserted";
            int textWidth = mc.font.width(noShaft);
            guiGraphics.fill(centerX - 4, centerY - 4, centerX + textWidth + 4, centerY + lineHeight + 2, bgColor);
            guiGraphics.drawString(mc.font, noShaft, centerX, centerY, noShaftColor, true);
        } else {
            // Вал есть — отображаем параметры сети
            String header = "▶ Network Analyzer";
            String speedText = "Speed: " + Math.abs(tachometer.getNetworkSpeed()) + " RPM";
            String torqueText = "Torque: " + tachometer.getNetworkConsumedTorque() + " / " + tachometer.getNetworkTorque() + " Nm";
            String inertiaText = String.format("Inertia: %.2f", tachometer.getNetworkInertia());
            String frictionText = String.format("Friction: x%.2f", tachometer.getNetworkFrictionMultiplier());
            
            // Расчет стресса (нагрузки)
            double load = tachometer.getNetworkLoad();
            double stressValue = Math.max(0, (load - 1.0) / 0.25);
            String stressText = String.format("Stress: %.1f%%", stressValue * 100.0);
            int stressColor = valueColor;
            if (load >= 1.25) {
                stressColor = noShaftColor; // Красный (критично)
            } else if (load >= 1.0) {
                stressColor = 0xFFFFAA00; // Оранжевый (перегруз)
            }

            // Вычисляем максимальную ширину для фона
            int maxWidth = Math.max(mc.font.width(header),
                    Math.max(mc.font.width(speedText),
                            Math.max(mc.font.width(torqueText),
                                    Math.max(mc.font.width(inertiaText), 
                                            Math.max(mc.font.width(frictionText), mc.font.width(stressText))))));

            // Фон
            int bgX1 = centerX - 4;
            int bgY1 = centerY - 4;
            int bgX2 = centerX + maxWidth + 8;
            int bgY2 = centerY + lineHeight * 6 + 4;
            guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, bgColor);

            // Заголовок
            guiGraphics.drawString(mc.font, header, centerX, centerY, headerColor, true);

            // Данные
            guiGraphics.drawString(mc.font, speedText, centerX, centerY + lineHeight, valueColor, true);
            
            int torqueColor = (tachometer.getNetworkConsumedTorque() > tachometer.getNetworkTorque() && tachometer.getNetworkTorque() > 0) ? 0xFFFFAA00 : valueColor;
            guiGraphics.drawString(mc.font, torqueText, centerX, centerY + lineHeight * 2, torqueColor, true);
            
            guiGraphics.drawString(mc.font, inertiaText, centerX, centerY + lineHeight * 3, valueColor, true);
            guiGraphics.drawString(mc.font, frictionText, centerX, centerY + lineHeight * 4, valueColor, true);
            guiGraphics.drawString(mc.font, stressText, centerX, centerY + lineHeight * 5, stressColor, true);
        }
    };
}
