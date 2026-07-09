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

        BlockState ctrlState = mc.level.getBlockState(controllerPos);
        net.minecraft.core.Direction facing = ctrlState.getValue(StatorBlock.FACING);
        net.minecraft.core.Direction.Axis axis = ctrlState.getValue(StatorBlock.AXIS);

        int coilCount = 0;
        long totalConversion = 0;
        for (int i = 0; i < 12; i++) {
            net.minecraft.world.item.ItemStack stack = stator.getCoilsInventory().getStackInSlot(i);
            if (!stack.isEmpty()) {
                coilCount++;
                if (stack.getItem() instanceof com.trd.item.energy.StatorCoilItem coil) {
                    totalConversion += coil.getEnergyConversionRate();
                }
            }
        }

        long energyStored = stator.getEnergyStored();
        long maxEnergy = stator.getMaxEnergyDynamic();
        long load = stator.getConsumedTorque();

        long production = 0;
        if (energyStored < maxEnergy) {
            BlockPos holeOffset = com.trd.multiblock.system.MultiblockStructureHelper.rotateStatorPos(new BlockPos(0, 1, 0), facing, axis);
            BlockPos shaftPos = controllerPos.offset(holeOffset);
            if (mc.level.getBlockEntity(shaftPos) instanceof com.trd.block.entity.industrial.rotation.ShaftBlockEntity shaft) {
                if (shaft.hasRotor()) {
                    long speed = Math.abs(shaft.getSpeed());
                    float rotorEfficiency = 1.0f;
                    com.trd.api.rotation.RotorType rotorType = shaft.getRotorType();
                    if (rotorType != null) rotorEfficiency = rotorType.getEfficiency();
                    production = (long) ((speed * totalConversion * rotorEfficiency) / 20.0f);
                }
            }
        }

        int centerX = screenWidth / 2 + 12;
        int centerY = screenHeight / 2 + 4;

        int lineHeight = 12;
        int bgColor = 0x80000000;
        int headerColor = 0xFFFFAA00;
        int whiteColor = 0xFFFFFFFF;

        String header = "▶ Stator Network";

        String gray = net.minecraft.ChatFormatting.GRAY.toString();
        String white = net.minecraft.ChatFormatting.WHITE.toString();
        String prodColStr = production > 0 ? net.minecraft.ChatFormatting.GREEN.toString() : net.minecraft.ChatFormatting.RED.toString();

        String coilsText = gray + "Coils: " + white + coilCount + " / 12";
        String bufferText = gray + "Buffer: " + white + energyStored + " / " + maxEnergy + " JE";
        String loadText = gray + "Load: " + white + load + " Nm";
        String prodText = gray + "Production: " + prodColStr + production + " JE/t";

        int maxWidth = Math.max(mc.font.width(header),
                Math.max(mc.font.width(bufferText),
                        Math.max(mc.font.width(loadText),
                                Math.max(mc.font.width(coilsText), mc.font.width(prodText)))));

        if (centerX + maxWidth + 8 > screenWidth) {
            centerX = screenWidth / 2 - maxWidth - 12;
        }

        int bgX1 = centerX - 4;
        int bgY1 = centerY - 4;
        int bgX2 = centerX + maxWidth + 8;
        int bgY2 = centerY + lineHeight * 5 + 4;
        guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, bgColor);

        guiGraphics.drawString(mc.font, header, centerX, centerY, headerColor, true);
        guiGraphics.drawString(mc.font, coilsText, centerX, centerY + lineHeight, whiteColor, true);
        guiGraphics.drawString(mc.font, bufferText, centerX, centerY + lineHeight * 2, whiteColor, true);
        guiGraphics.drawString(mc.font, loadText, centerX, centerY + lineHeight * 3, whiteColor, true);
        guiGraphics.drawString(mc.font, prodText, centerX, centerY + lineHeight * 4, whiteColor, true);
    };
}