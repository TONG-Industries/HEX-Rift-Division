package com.trd.client.overlay.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trd.api.fluids.ModFluids;
import com.trd.block.basic.industrial.fluids.LowPressureSteamCondenserBlock;
import com.trd.block.entity.industrial.fluids.LowPressureSteamCondenserBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.trd.main.MainRegistry;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LowPressureSteamCondenserOverlay {

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = blockHit.getBlockPos();
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (!(be instanceof LowPressureSteamCondenserBlockEntity condenser)) return;

        BlockState state = mc.level.getBlockState(pos);
        renderHUD(event.getGuiGraphics(), condenser, state, event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(), mc.font);
    }

    private static void renderHUD(GuiGraphics guiGraphics, LowPressureSteamCondenserBlockEntity be,
                                  BlockState state, int screenWidth, int screenHeight, Font font) {
        int x = screenWidth / 2 + 12;
        int y = screenHeight / 2 + 4;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int steamAmount = be.getSteamTank().getFluidAmount();
        int steamCapacity = be.getSteamTank().getCapacity();
        int waterAmount = be.getWaterTank().getFluidAmount();
        int waterCapacity = be.getWaterTank().getCapacity();

        // Цвета названий жидкостей (только названия, цифры белые)
        int steamColor = IClientFluidTypeExtensions.of(ModFluids.LOW_PRESSURE_STEAM_SOURCE.get()).getTintColor() | 0xFF000000;
        int waterColor = IClientFluidTypeExtensions.of(Fluids.WATER).getTintColor() | 0xFF000000;

        // Строка пара: цветное название + зелёная стрелка + белые цифры
        String steamPrefix = "Пар Н.Д. ";
        String steamArrow  = "§a-> ";
        String steamNums   = "§7" + steamAmount + "/" + steamCapacity + " mB";
        int steamPrefixW = font.width(steamPrefix);
        int steamArrowW  = font.width(steamArrow);
        int steamNumsW   = font.width(steamNums);
        int steamTotalW  = steamPrefixW + steamArrowW + steamNumsW;

        // Строка воды: цветное название + красная стрелка + белые цифры
        String waterPrefix = "Вода ";
        String waterArrow  = "§c<- ";
        String waterNums   = "§7" + waterAmount + "/" + waterCapacity + " mB";
        int waterPrefixW = font.width(waterPrefix);
        int waterArrowW  = font.width(waterArrow);
        int waterNumsW   = font.width(waterNums);
        int waterTotalW  = waterPrefixW + waterArrowW + waterNumsW;

        // Третья строка — статус (без воды / коэффициент)
        boolean isWaterlogged = state.getValue(LowPressureSteamCondenserBlock.WATERLOGGED);
        String statusText;
        if (!isWaterlogged) {
            statusText = "§cТребуется залить водой!";
        } else {
            statusText = String.format("§7Охлаждение: §b%.2fx", be.getCoolingMultiplier());
        }
        int statusW = font.width(statusText);

        int maxWidth = Math.max(steamTotalW, Math.max(waterTotalW, statusW));

        // Авто-сдвиг влево, если текст вылезает за экран
        if (x + maxWidth + 4 > screenWidth) {
            x = screenWidth / 2 - maxWidth - 12;
        }

        int lineHeight = font.lineHeight + 2;
        int totalHeight = lineHeight * 3;
        guiGraphics.fill(x - 3, y - 2, x + maxWidth + 3, y + totalHeight + 2, 0x90000000);

        // --- Пар (вход) ---
        int cx = x;
        guiGraphics.drawString(font, steamPrefix, cx, y, steamColor, true);
        guiGraphics.drawString(font, steamArrow,  cx + steamPrefixW, y, 0xFFFFFF, true);
        guiGraphics.drawString(font, steamNums,   cx + steamPrefixW + steamArrowW, y, 0xFFFFFF, true);

        // --- Вода (выход) ---
        cx = x;
        guiGraphics.drawString(font, waterPrefix, cx, y + lineHeight, waterColor, true);
        guiGraphics.drawString(font, waterArrow,  cx + waterPrefixW, y + lineHeight, 0xFFFFFF, true);
        guiGraphics.drawString(font, waterNums,   cx + waterPrefixW + waterArrowW, y + lineHeight, 0xFFFFFF, true);

        // --- Статус (только коэффициент голубой, остальное серое/красное через цветовые коды в строке) ---
        guiGraphics.drawString(font, statusText, x, y + lineHeight * 2, 0xFFFFFF, true);
    }
}