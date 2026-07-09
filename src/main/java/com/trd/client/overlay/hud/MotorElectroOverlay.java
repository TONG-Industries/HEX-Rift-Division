package com.trd.client.overlay.hud;

import com.trd.block.basic.industrial.rotation.MotorElectroBlock;
import com.trd.block.entity.industrial.rotation.MotorElectroBlockEntity;
import com.trd.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HUD при наведении на MotorElectroBlock.
 * Позиция: правый нижний угол возле прицела (справа-снизу от центра экрана).
 * Данные читаются напрямую из клиентского BlockEntity — синхронизация
 * происходит каждый тик через serverTick → sendBlockUpdated.
 */
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MotorElectroOverlay {

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Проверяем: смотрит ли игрок на MotorElectroBlock
        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHit)) return;
        if (hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = blockHit.getBlockPos();
        if (!(mc.level.getBlockState(pos).getBlock() instanceof MotorElectroBlock)) return;
        if (!(mc.level.getBlockEntity(pos) instanceof MotorElectroBlockEntity motor)) return;

        // Данные из клиентского BE (синхронизируются каждый тик)
        long rpm         = Math.abs(motor.getVisualSpeed());
        long torque      = motor.getTorqueNm();
        int  consumption = motor.getConsumptionPerSecond();
        long energy      = motor.getEnergyStored();
        long maxEnergy   = motor.getMaxEnergyStored();
        boolean running  = energy > 0;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();

        // Позиция: правый нижний угол возле прицела
        // Прицел в центре (screenW/2, screenH/2). Смещаем: правее +12, ниже +4
        int panelW = 160;
        int lineH  = font.lineHeight + 2;
        int lines  = 5; // заголовок + 4 строки
        int panelH = lineH * lines + 4;

        int x = screenW / 2 + 12;
        int y = screenH / 2 + 4;

        // Если панель выходит за правый край — сдвигаем влево
        if (x + panelW > screenW - 4) {
            x = screenW / 2 - panelW - 12;
        }

        // Фон
        graphics.fill(x - 4, y - 4, x + panelW, y + panelH, 0x88000000);

        // Заголовок
        String runColor = running ? "§a" : "§c";
        String status   = running ? "ON" : "OFF";
        graphics.drawString(font, "§e⚡ Мотор §7[" + runColor + status + "§7]", x, y, 0xFFFFFF, false);
        y += lineH;

        // Скорость
        graphics.drawString(font, "§7Скорость:    §f" + rpm + " RPM", x, y, 0xFFFFFF, false);
        y += lineH;

        // Момент
        graphics.drawString(font, "§7Момент:      §f" + torque + " Нм", x, y, 0xFFFFFF, false);
        y += lineH;

        // Потребление
        graphics.drawString(font, "§7Потребление: §f" + consumption + " JE/s", x, y, 0xFFFFFF, false);
        y += lineH;

        // Заряд
        String chargeColor = energy > maxEnergy / 4 ? "§a" : "§c";
        graphics.drawString(font, "§7Заряд: " + chargeColor + energy + "§7/" + maxEnergy + " JE",
                x, y, 0xFFFFFF, false);
    }
}