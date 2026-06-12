package com.trd.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trd.block.basic.industrial.rotation.StatorBlock;
import com.trd.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientHighlightEvent {

    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        BlockHitResult hit = event.getTarget();
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        BlockPos ctrlPos = pos;

        if (state.getBlock() instanceof com.trd.multiblock.system.MultiblockPartBlock) {
            var be = mc.level.getBlockEntity(pos);
            if (be instanceof com.trd.multiblock.system.IMultiblockPart part && part.getControllerPos() != null) {
                ctrlPos = part.getControllerPos();
                state = mc.level.getBlockState(ctrlPos);
            }
        }

        if (!(state.getBlock() instanceof StatorBlock)) return;

        Direction facing = state.getValue(StatorBlock.FACING);
        Direction.Axis axis = state.getValue(StatorBlock.AXIS);

        BlockPos holeOffset = com.trd.multiblock.system.MultiblockStructureHelper.rotateStatorPos(new BlockPos(0, 1, 0), facing, axis);
        Vec3 holeCenter = new Vec3(ctrlPos.getX() + 0.5 + holeOffset.getX(), ctrlPos.getY() + 0.5 + holeOffset.getY(), ctrlPos.getZ() + 0.5 + holeOffset.getZ());
        Vec3 hitVec = hit.getLocation().subtract(holeCenter);

        org.joml.Vector3f localVec = new org.joml.Vector3f((float)hitVec.x, (float)hitVec.y, (float)hitVec.z);
        
        if (axis == Direction.Axis.X) {
            localVec.rotateY((float) Math.toRadians(-90));
        } else if (axis == Direction.Axis.Y) {
            localVec.rotateX((float) Math.toRadians(-90));
            if (facing == Direction.WEST) {
                localVec.rotateY((float) Math.toRadians(-90));
            } else if (facing == Direction.SOUTH) {
                localVec.rotateY((float) Math.toRadians(-180));
            } else if (facing == Direction.EAST) {
                localVec.rotateY((float) Math.toRadians(-270));
            }
        }

        double u = localVec.y;
        double v = -localVec.x;

        double angle = Math.toDegrees(Math.atan2(v, u));
        if (angle < 0) angle += 360;
        int slot = (int) Math.round(angle / 30.0) % 12;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        
        poseStack.pushPose();
        poseStack.translate(holeCenter.x - camPos.x, holeCenter.y - camPos.y, holeCenter.z - camPos.z);

        // Rotate just like the coil
        if (axis == Direction.Axis.X) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
        } else if (axis == Direction.Axis.Y) {
            if (facing == Direction.WEST) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
            } else if (facing == Direction.SOUTH) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            } else if (facing == Direction.EAST) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270));
            }
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
        }

        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slot * 30));
        poseStack.translate(0, 1.1, 0);

        // Box size 0.25 (coil approximate size)
        AABB box = new AABB(-0.15, -0.15, -0.15, 0.15, 0.15, 0.15);
        VertexConsumer builder = event.getMultiBufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, builder, box, 0.0F, 1.0F, 0.0F, 0.8F);

        poseStack.popPose();
    }
}
