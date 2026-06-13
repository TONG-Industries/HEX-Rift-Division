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
        net.minecraft.world.phys.Vec3 normal;
        if (axis == Direction.Axis.X) normal = new net.minecraft.world.phys.Vec3(1, 0, 0);
        else if (axis == Direction.Axis.Y) normal = new net.minecraft.world.phys.Vec3(0, 1, 0);
        else normal = new net.minecraft.world.phys.Vec3(0, 0, 1);

        Vec3 eyePos = player.getEyePosition(event.getPartialTick());
        Vec3 lookVec = player.getViewVector(event.getPartialTick());
        
        Vec3 hitVec = hit.getLocation().subtract(holeCenter); // fallback
        double denom = normal.dot(lookVec);
        if (Math.abs(denom) > 0.0001) {
            double t = normal.dot(holeCenter.subtract(eyePos)) / denom;
            if (t > 0 && t < 10) {
                hitVec = eyePos.add(lookVec.scale(t)).subtract(holeCenter);
            }
        }

        org.joml.Vector3f localVec = new org.joml.Vector3f((float)hitVec.x, (float)hitVec.y, (float)hitVec.z);
        
        if (axis == Direction.Axis.X) {
            localVec.rotateY((float) Math.toRadians(-90));
        } else if (axis == Direction.Axis.Y) {
            if (facing == Direction.NORTH) {
                localVec.rotateY((float) Math.toRadians(-180));
            } else if (facing == Direction.EAST) {
                localVec.rotateY((float) Math.toRadians(-90));
            } else if (facing == Direction.WEST) {
                localVec.rotateY((float) Math.toRadians(-270));
            }
            localVec.rotateX((float) Math.toRadians(90));
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
            if (facing == Direction.NORTH) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            } else if (facing == Direction.EAST) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
            } else if (facing == Direction.WEST) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270));
            }
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
        }

        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slot * 30));
        poseStack.translate(0, 1.1, 0);

        poseStack.scale(0.99f, 0.99f, 0.99f);

        long time = mc.level.getGameTime() * 50 + (long)(event.getPartialTick() * 50);
        float alpha = 0.1f + 0.4f * (float) ((Math.sin(time / 200.0) + 1.0) / 2.0);

        net.minecraft.client.renderer.MultiBufferSource originalBuffer = event.getMultiBufferSource();
        net.minecraft.client.renderer.MultiBufferSource customBuffer = renderType -> {
            RenderType translucent = RenderType.entityTranslucent(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
            VertexConsumer original = originalBuffer.getBuffer(translucent);
            return new VertexConsumer() {
                @Override
                public VertexConsumer vertex(double x, double y, double z) { return original.vertex(x, y, z); }
                @Override
                public VertexConsumer color(int r, int g, int b, int a) {
                    return original.color(r, g, b, (int)(a * alpha));
                }
                @Override
                public VertexConsumer uv(float u, float v) { return original.uv(u, v); }
                @Override
                public VertexConsumer overlayCoords(int u, int v) { return original.overlayCoords(u, v); }
                @Override
                public VertexConsumer uv2(int u, int v) { return original.uv2(u, v); }
                @Override
                public VertexConsumer normal(float x, float y, float z) { return original.normal(x, y, z); }
                @Override
                public void endVertex() { original.endVertex(); }
                @Override
                public void defaultColor(int r, int g, int b, int a) { original.defaultColor(r, g, b, (int)(a * alpha)); }
                @Override
                public void unsetDefaultColor() { original.unsetDefaultColor(); }
            };
        };

        net.minecraft.client.resources.model.BakedModel bakedModel = com.trd.client.render.flywheel.ModModels.STATOR_COILS.get("copper").get();
        poseStack.translate(-0.5, -0.5, -0.5);

        if (bakedModel != null) {
            mc.getBlockRenderer().getModelRenderer().renderModel(
                    poseStack.last(),
                    customBuffer.getBuffer(RenderType.translucent()),
                    null,
                    bakedModel,
                    1.0F, 1.0F, 1.0F,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY
            );
        }

        poseStack.popPose();
    }
}
