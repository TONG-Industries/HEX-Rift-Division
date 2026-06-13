package com.trd.client.overlay.hud;

import com.trd.block.basic.industrial.rotation.StatorBlock;
import com.trd.block.entity.industrial.rotation.StatorBlockEntity;
import com.trd.multiblock.system.IMultiblockPart;
import com.trd.multiblock.system.MultiblockPartBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class StatorOverlay {

    public static final IGuiOverlay HUD_STATOR = (ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);

        BlockPos controllerPos = null;
        if (state.getBlock() instanceof StatorBlock) {
            controllerPos = pos;
        } else if (state.getBlock() instanceof MultiblockPartBlock) {
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be instanceof IMultiblockPart part && part.getControllerPos() != null) {
                BlockState ctrlState = mc.level.getBlockState(part.getControllerPos());
                if (ctrlState.getBlock() instanceof StatorBlock) {
                    controllerPos = part.getControllerPos();
                }
            }
        }

        if (controllerPos == null) return;

        if (!(mc.level.getBlockEntity(controllerPos) instanceof StatorBlockEntity stator)) return;

        int centerX = screenWidth / 2 + 15;
        int centerY = screenHeight / 2 - 20;

        int lineHeight = 12;
        int bgColor = 0x80000000;
        int headerColor = 0xFFFFAA00;
        int valueColor = 0xFFFFFFFF;

        String header = "▶ Stator Network";
        long energyStored = stator.getEnergyStored();
        long maxEnergy = stator.getMaxEnergyDynamic();
        long load = stator.getConsumedTorque();
        
        String bufferText = "Buffer: " + energyStored + " / " + maxEnergy + " JE";
        String loadText = "Load: " + load + " Nm";

        int maxWidth = Math.max(mc.font.width(header),
                Math.max(mc.font.width(bufferText), mc.font.width(loadText)));

        int bgX1 = centerX - 4;
        int bgY1 = centerY - 4;
        int bgX2 = centerX + maxWidth + 8;
        int bgY2 = centerY + lineHeight * 3 + 4;
        guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, bgColor);

        guiGraphics.drawString(mc.font, header, centerX, centerY, headerColor, true);
        guiGraphics.drawString(mc.font, bufferText, centerX, centerY + lineHeight, valueColor, true);
        guiGraphics.drawString(mc.font, loadText, centerX, centerY + lineHeight * 2, valueColor, true);
    };
}
