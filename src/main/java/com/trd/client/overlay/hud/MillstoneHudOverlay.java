package com.trd.client.overlay.hud;

import com.trd.block.entity.industrial.MillstoneBlockEntity;
import com.trd.main.MainRegistry;
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

import java.util.List;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MillstoneHudOverlay {

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = blockHit.getBlockPos();
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (!(be instanceof MillstoneBlockEntity millstone)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        int x = screenWidth / 2 + 12;
        int y = screenHeight / 2 + 4;

        ItemStack input = millstone.getInputStack();
        List<ItemStack> results = millstone.getResultStacks();
        boolean processing = millstone.isProcessing();
        int current = millstone.getCurrentGrinds();
        int required = millstone.getRequiredGrinds();
        int remaining = millstone.getRemainingGrinds();

        String mainText;
        int mainColor;
        String subText = null;
        int subColor = 0xAAAAAA;

        if (!results.isEmpty()) {
            // Готово к сбору
            mainText = "✓ " + results.get(0).getHoverName().getString();
            if (results.size() > 1) {
                mainText += " + " + (results.size() - 1);
            }
            mainColor = 0x55FF55; // Зелёный
            subText = "ПКМ чтобы забрать";

        } else if (processing) {
            // В процессе помола
            mainText = String.format("%d/%d оборотов", current, required);

            // Цвет от прогресса: серый -> оранжевый -> зелёный
            float progress = (float) current / required;
            mainColor = getProgressColor(progress);

            if (remaining > 0) {
                subText = "Осталось: " + remaining;
            }

        } else if (!input.isEmpty()) {
            // Есть вход, ждёт начала (редкий кейс)
            mainText = input.getHoverName().getString();
            mainColor = 0xFFAA00;
            subText = "ПКМ для помола";

        } else {
            // Пусто
            mainText = "Жернова пусты";
            mainColor = 0xAAAAAA;
            subText = "Положите минерал";
        }

        // Рендер основного текста
        int textWidth = font.width(mainText);
        int maxWidth = textWidth;

        // Корректировка если вылезает за экран
        if (x + textWidth + 4 > screenWidth) {
            x = screenWidth / 2 - textWidth - 12;
        }

        // Дополнительная строка (если есть)
        if (subText != null) {
            maxWidth = Math.max(maxWidth, font.width(subText));
        }

        int bgHeight = subText != null ? y + font.lineHeight * 2 + 5 : y + font.lineHeight + 2;
        graphics.fill(x - 3, y - 2, x + maxWidth + 3, bgHeight, 0x90000000);
        graphics.drawString(font, mainText, x, y, mainColor, true);

        if (subText != null) {
            int subY = y + font.lineHeight + 3;
            graphics.drawString(font, subText, x, subY, subColor, true);
        }

        // Иконка предмета слева от текста
        if (!results.isEmpty()) {
            graphics.renderItem(results.get(0), x - 20, y - 2);
        } else if (!input.isEmpty()) {
            graphics.renderItem(input, x - 20, y - 2);
        }
    }

    private static int getProgressColor(float percent) {
        percent = Math.max(0.0f, Math.min(1.0f, percent));
        int grey = 0xAAAAAA;
        int orange = 0xFFAA00;
        int green = 0x55FF55;

        if (percent <= 0.5f) {
            return lerpColor(grey, orange, percent * 2.0f);
        } else {
            return lerpColor(orange, green, (percent - 0.5f) * 2.0f);
        }
    }

    private static int lerpColor(int color1, int color2, float t) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }
}