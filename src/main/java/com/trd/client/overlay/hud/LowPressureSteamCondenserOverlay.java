package com.trd.client.overlay.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trd.api.fluids.ModFluids;
import com.trd.block.entity.industrial.fluids.LowPressureSteamCondenserBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * HUD-оверлей конденсатора (по образцу BoilerOverlay).
 * Показывается у прицела, когда игрок смотрит на блок.
 * 🟢 Пар (вход, ->)  🔴 Вода (выход, <-).
 */
public class LowPressureSteamCondenserOverlay implements IGuiOverlay {

    public static final LowPressureSteamCondenserOverlay INSTANCE = new LowPressureSteamCondenserOverlay();

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (!(be instanceof LowPressureSteamCondenserBlockEntity condenser)) return;

        renderHUD(guiGraphics, condenser, width, height, mc.font);
    }

    private void renderHUD(GuiGraphics guiGraphics, LowPressureSteamCondenserBlockEntity be, int width, int height, Font font) {
        int x = width / 2;
        int y = height / 2 + 15; // чуть ниже прицела

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Пар — вход (зелёная ->)
        int steamAmount = be.getSteamTank().getFluidAmount();
        int steamCapacity = be.getSteamTank().getCapacity();
        String steamPrefix = "Пар ";
        String steamSuffix = "§a-> §7" + steamAmount + "/" + steamCapacity + " mB";

        // Вода — выход (красная <-)
        int waterAmount = be.getWaterTank().getFluidAmount();
        int waterCapacity = be.getWaterTank().getCapacity();
        String waterPrefix = "Вода ";
        String waterSuffix = "§c<- §7" + waterAmount + "/" + waterCapacity + " mB";

        int steamColor = IClientFluidTypeExtensions.of(ModFluids.LOW_PRESSURE_STEAM_SOURCE.get()).getTintColor() | 0xFF000000;
        int waterColor = IClientFluidTypeExtensions.of(Fluids.WATER).getTintColor() | 0xFF000000;

        int steamPrefixWidth = font.width(steamPrefix);
        int steamTextWidth = steamPrefixWidth + font.width(steamSuffix);

        int waterPrefixWidth = font.width(waterPrefix);
        int waterTextWidth = waterPrefixWidth + font.width(waterSuffix);

        int maxWidth = Math.max(steamTextWidth, waterTextWidth);
        guiGraphics.fill(x - maxWidth / 2 - 4, y - 2, x + maxWidth / 2 + 4, y + 22, 0x90000000);

        int steamX = x - steamTextWidth / 2;
        guiGraphics.drawString(font, steamPrefix, steamX, y, steamColor, true);
        guiGraphics.drawString(font, steamSuffix, steamX + steamPrefixWidth, y, 0xFFFFFF, true);

        int waterX = x - waterTextWidth / 2;
        guiGraphics.drawString(font, waterPrefix, waterX, y + 10, waterColor, true);
        guiGraphics.drawString(font, waterSuffix, waterX + waterPrefixWidth, y + 10, 0xFFFFFF, true);
    }
}
