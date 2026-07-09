package com.trd.client.overlay.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trd.multiblock.industrial.SteamEngineBlock;
import com.trd.multiblock.industrial.SteamEngineBlockEntity;
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

public class SteamEngineOverlay implements IGuiOverlay {

    public static final SteamEngineOverlay INSTANCE = new SteamEngineOverlay();

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            BlockEntity be = mc.level.getBlockEntity(pos);

            SteamEngineBlockEntity engine = null;

            if (be instanceof SteamEngineBlockEntity) {
                engine = (SteamEngineBlockEntity) be;
            } else if (be instanceof MultiblockPartEntity part) {
                if (part.getControllerPos() != null) {
                    BlockEntity controllerBe = mc.level.getBlockEntity(part.getControllerPos());
                    if (controllerBe instanceof SteamEngineBlockEntity) {
                        engine = (SteamEngineBlockEntity) controllerBe;
                    }
                }
            }

            if (engine != null) {
                renderEngineHUD(guiGraphics, engine, width, height, mc.font);
            }
        }
    }

    private void renderEngineHUD(GuiGraphics guiGraphics, SteamEngineBlockEntity engine, int width, int height, Font font) {
        int x = width / 2 + 12;
        int y = height / 2 + 4;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int steamAmount = engine.steamTank.getFluidAmount();
        int steamCapacity = engine.steamTank.getCapacity();
        String steamPrefix = "Пар ";
        String steamSuffix = "§a-> §7" + steamAmount + "/" + steamCapacity + " mB";

        int lowPressureAmount = engine.lowPressureSteamTank.getFluidAmount();
        int lowPressureCapacity = engine.lowPressureSteamTank.getCapacity();
        String lowPressurePrefix = "Отраб. Пар ";
        String lowPressureSuffix = "§c<- §7" + lowPressureAmount + "/" + lowPressureCapacity + " mB";

        int steamColor = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(com.trd.api.fluids.ModFluids.STEAM_SOURCE.get()).getTintColor() | 0xFF000000;
        int lowPressureColor = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(com.trd.api.fluids.ModFluids.STEAM_SOURCE.get()).getTintColor() | 0xFF000000;

        int steamPrefixWidth = font.width(steamPrefix);
        int steamSuffixWidth = font.width(steamSuffix);
        int steamTextWidth = steamPrefixWidth + steamSuffixWidth;

        int lowPressurePrefixWidth = font.width(lowPressurePrefix);
        int lowPressureSuffixWidth = font.width(lowPressureSuffix);
        int lowPressureTextWidth = lowPressurePrefixWidth + lowPressureSuffixWidth;

        int maxWidth = Math.max(steamTextWidth, lowPressureTextWidth);

        if (x + maxWidth + 4 > width) {
            x = width / 2 - maxWidth - 12;
        }

        guiGraphics.fill(x - 4, y - 2, x + maxWidth + 4, y + 22, 0x90000000);

        int steamX = x;
        guiGraphics.drawString(font, steamPrefix, steamX, y, steamColor, true);
        guiGraphics.drawString(font, steamSuffix, steamX + steamPrefixWidth, y, 0xFFFFFF, true);

        int lowPressureX = x;
        guiGraphics.drawString(font, lowPressurePrefix, lowPressureX, y + 10, lowPressureColor, true);
        guiGraphics.drawString(font, lowPressureSuffix, lowPressureX + lowPressurePrefixWidth, y + 10, 0xFFFFFF, true);
    }
}
