package com.trd.client.gui;

import com.trd.block.entity.industrial.WaterPumpBlockEntity;
import com.trd.block.industrial.WaterPumpBlock;
import com.trd.multiblock.system.MultiblockPartBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraft.client.gui.GuiGraphics;

public class WaterPumpHUD implements IGuiOverlay {
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // RayTrace на 5 блоков
        HitResult hit = mc.player.pick(5.0D, partialTick, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            
            BlockPos controllerPos = pos;
            
            // Если смотрим на нижний парт, берем контроллер сверху
            if (state.getBlock() instanceof MultiblockPartBlock) {
                controllerPos = pos.above();
                state = mc.level.getBlockState(controllerPos);
            }

            if (state.getBlock() instanceof WaterPumpBlock) {
                BlockEntity be = mc.level.getBlockEntity(controllerPos);
                if (be instanceof WaterPumpBlockEntity pump) {
                    int x = width / 2 + 15; // Справа от прицела
                    int y = height / 2 + 15; // Снизу от прицела

                    String speedText = "Скорость накачки: " + pump.getLastPumpedVolume() + " mB/t";
                    String volumeText = "Объем водоема: " + pump.getCachedWaterVolume() + " / 1000";

                    guiGraphics.drawString(mc.font, speedText, x, y, 0xFFFFFF, true);
                    guiGraphics.drawString(mc.font, volumeText, x, y + 12, 0x44AAFF, true);
                }
            }
        }
    }
}
