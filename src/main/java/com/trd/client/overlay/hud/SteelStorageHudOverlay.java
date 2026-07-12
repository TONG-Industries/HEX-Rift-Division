package com.trd.client.overlay.hud;

import com.trd.main.MainRegistry;
import com.trd.multiblock.industrial.SteelStorageBlockEntity;
import com.trd.multiblock.system.IMultiblockPart;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SteelStorageHudOverlay {

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = blockHit.getBlockPos();
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (be == null) return;

        SteelStorageBlockEntity storage = null;
        if (be instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockEntity controller = mc.level.getBlockEntity(controllerPos);
                if (controller instanceof SteelStorageBlockEntity s) {
                    storage = s;
                }
            }
        } else if (be instanceof SteelStorageBlockEntity s) {
            storage = s;
        }

        if (storage == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        ItemStackHandler inventory = storage.getInventory();
        int totalSlots = inventory.getSlots();
        int filledSlots = 0;
        Map<String, Integer> items = new LinkedHashMap<>();

        for (int i = 0; i < totalSlots; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                filledSlots++;
                String name = stack.getHoverName().getString();
                items.merge(name, stack.getCount(), Integer::sum);
            }
        }

        // === ПОЗИЦИОНИРОВАНИЕ (как у нагревателя) ===
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        int baseX = screenWidth / 2 + 12;
        int baseY = screenHeight / 2 + 4;

        // === ЗАГОЛОВОК: заполнение с цветом ===
        float ratio = totalSlots > 0 ? (float) filledSlots / totalSlots : 0;
        ChatFormatting headerColor = ratio < 0.33f ? ChatFormatting.GREEN
                : (ratio < 0.66f ? ChatFormatting.YELLOW : ChatFormatting.RED);

        String header = String.format("%d/%d слотов", filledSlots, totalSlots);
        int headerWidth = font.width(header);

        // Считаем максимальную ширину текста для фона
        int maxTextWidth = headerWidth;

        // Собираем строки контента
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (filledSlots == 0) {
            lines.add("Пусто");
        } else {
            int shown = 0;
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                if (shown >= 8) {
                    lines.add("... и ещё " + (items.size() - 8));
                    break;
                }
                String line = "• " + entry.getKey() + " x" + entry.getValue();
                lines.add(line);
                maxTextWidth = Math.max(maxTextWidth, font.width(line));
                shown++;
            }
        }

        // === РАСЧЁТ РАЗМЕРОВ ФОНА ===
        int lineHeight = font.lineHeight;
        int paddingX = 4;
        int paddingY = 3;
        int lineSpacing = 1;

        int contentHeight = lines.size() * (lineHeight + lineSpacing) - lineSpacing;
        int totalHeight = lineHeight + 2 + contentHeight; // заголовок + отступ + контент
        int bgWidth = maxTextWidth + paddingX * 2;
        int bgHeight = totalHeight + paddingY * 2;

        // Если выходит за правый край — сдвигаем влево
        if (baseX + bgWidth > screenWidth) {
            baseX = screenWidth / 2 - bgWidth - 12;
        }

        int bgX = baseX - paddingX;
        int bgY = baseY - paddingY;

        // === ФОН (как у нагревателя: 0x90000000) ===
        graphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0x90000000);

        // === ЗАГОЛОВОК ===
        int currentY = baseY;
        graphics.drawString(font, header, baseX, currentY, headerColor.getColor(), true);
        currentY += lineHeight + 2;

        // === РАЗДЕЛИТЕЛЬ (тонкая линия) ===
        if (filledSlots > 0) {
            graphics.fill(bgX + 2, currentY - 1, bgX + bgWidth - 2, currentY, 0x60FFFFFF);
        }

        // === КОНТЕНТ ===
        for (String line : lines) {
            int color = line.startsWith("•") ? 0xFFCCCCCC : 0xFF888888;
            if (line.equals("Пусто")) color = 0xFF888888;
            graphics.drawString(font, line, baseX, currentY, color, true);
            currentY += lineHeight + lineSpacing;
        }
    }
}