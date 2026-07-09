package com.trd.client.overlay.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trd.multiblock.industrial.BoilerBlock;
import com.trd.multiblock.industrial.BoilerBlockEntity;
import com.trd.multiblock.system.MultiblockPartEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class BoilerOverlay implements IGuiOverlay {

    public static final BoilerOverlay INSTANCE = new BoilerOverlay();

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            BlockEntity be = mc.level.getBlockEntity(pos);

            BoilerBlockEntity boiler = null;

            if (be instanceof BoilerBlockEntity) {
                boiler = (BoilerBlockEntity) be;
            } else if (be instanceof MultiblockPartEntity part) {
                if (part.getControllerPos() != null) {
                    BlockEntity controllerBe = mc.level.getBlockEntity(part.getControllerPos());
                    if (controllerBe instanceof BoilerBlockEntity) {
                        boiler = (BoilerBlockEntity) controllerBe;
                    }
                }
            }

            if (boiler != null) {
                renderBoilerHUD(guiGraphics, boiler, width, height, mc.font);
            }
        }
    }

    private void renderBoilerHUD(GuiGraphics guiGraphics, BoilerBlockEntity boiler, int width, int height, Font font) {
        int x = width / 2 + 12;
        int y = height / 2 + 4;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int waterAmount = boiler.getWaterTank().getFluidAmount();
        int waterCapacity = boiler.getWaterTank().getCapacity();
        String waterPrefix = "Вода ";
        String waterSuffix = "§a-> §7" + waterAmount + "/" + waterCapacity + " mB";

        int steamAmount = boiler.getSteamTank().getFluidAmount();
        int steamCapacity = boiler.getSteamTank().getCapacity();
        String steamPrefix = "Пар ";
        String steamSuffix = "§c<- §7" + steamAmount + "/" + steamCapacity + " mB";

        float temp = boiler.getTemperature();
        String tempText = String.format("Температура: %.1f °C", temp);

        int waterColor = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(net.minecraft.world.level.material.Fluids.WATER).getTintColor() | 0xFF000000;
        int steamColor = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(com.trd.api.fluids.ModFluids.STEAM_SOURCE.get()).getTintColor() | 0xFF000000;

        int waterPrefixWidth = font.width(waterPrefix);
        int waterSuffixWidth = font.width(waterSuffix);
        int waterTextWidth = waterPrefixWidth + waterSuffixWidth;

        int steamPrefixWidth = font.width(steamPrefix);
        int steamSuffixWidth = font.width(steamSuffix);
        int steamTextWidth = steamPrefixWidth + steamSuffixWidth;

        int tempWidth = font.width(tempText);

        int maxWidth = Math.max(Math.max(waterTextWidth, steamTextWidth), tempWidth);

        if (x + maxWidth + 4 > width) {
            x = width / 2 - maxWidth - 12;
        }

        guiGraphics.fill(x - 4, y - 2, x + maxWidth + 4, y + 32, 0x90000000);

        int waterX = x;
        guiGraphics.drawString(font, waterPrefix, waterX, y, waterColor, true);
        guiGraphics.drawString(font, waterSuffix, waterX + waterPrefixWidth, y, 0xFFFFFF, true);

        int steamX = x;
        guiGraphics.drawString(font, steamPrefix, steamX, y + 10, steamColor, true);
        guiGraphics.drawString(font, steamSuffix, steamX + steamPrefixWidth, y + 10, 0xFFFFFF, true);

        int tempColor = temp > 1000 ? ChatFormatting.DARK_RED.getColor() : ChatFormatting.GOLD.getColor();
        guiGraphics.drawString(font, tempText, x, y + 20, tempColor, true);
    }
}