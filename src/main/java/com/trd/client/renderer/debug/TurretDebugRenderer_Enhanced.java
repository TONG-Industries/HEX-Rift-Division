package com.trd.client.renderer.debug;

import com.trd.entity.weapons.turrets.TurretLightEntity;
import com.trd.entity.weapons.turrets.TurretLightLinkedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class TurretDebugRenderer_Enhanced {

    // === ЦВЕТА (RGBA) ===
    private static final float TARGET_R = 1.0F, TARGET_G = 0.0F, TARGET_B = 1.0F, TARGET_A = 1.0F; // Magenta
    private static final float TRAJ_R = 0.0F, TRAJ_G = 1.0F, TRAJ_B = 1.0F, TRAJ_A = 1.0F;   // Cyan
    private static final float IDLE_R = 1.0F, IDLE_G = 0.0F, IDLE_B = 1.0F, IDLE_A = 0.5F;

    public static void renderTurretDebug(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                         TurretLightEntity turret, double camX, double camY, double camZ) {
        renderDebugInfo(poseStack, bufferSource,
                turret.getDebugTargetPoint(),
                turret.getDebugBallisticVelocity(),
                turret.getDebugScanPoints(),
                turret.getMuzzlePos(),
                turret.yHeadRot, turret.getXRot());
    }

    public static void renderTurretDebug(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                         TurretLightLinkedEntity turret, double camX, double camY, double camZ) {
        renderDebugInfo(poseStack, bufferSource,
                turret.getDebugTargetPoint(),
                turret.getDebugBallisticVelocity(),
                turret.getDebugScanPoints(),
                turret.getMuzzlePos(),
                turret.yHeadRot, turret.getXRot());
    }

    private static void renderDebugInfo(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                        Vec3 targetPoint, Vec3 velocity,
                                        List<Pair<Vec3, Boolean>> scanPoints, Vec3 muzzlePos,
                                        float yHeadRot, float xRot) {

        RenderType renderType = RenderType.debugLineStrip(2.0);
        VertexConsumer lineBuilder = bufferSource.getBuffer(renderType);

        poseStack.pushPose();

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        // 1. СЕТКА (Grid Scan)
        if (scanPoints != null && !scanPoints.isEmpty()) {
            for (Pair<Vec3, Boolean> point : scanPoints) {
                Vec3 p = point.getFirst();
                boolean isHit = point.getSecond();
                float r = isHit ? 0.0F : 1.0F;
                float g = isHit ? 1.0F : 0.0F;

                renderBox(matrix, lineBuilder, p, 0.05, r, g, 0.0F, 0.8F);
            }
        }

        if (targetPoint != null) {
            // === ЕСТЬ ЦЕЛЬ ===

            // 2. ЦЕНТРОИД (Box)
            renderBox(matrix, lineBuilder, targetPoint, 0.25, TARGET_R, TARGET_G, TARGET_B, TARGET_A);

            // 3. ЛИНИЯ НАВОДКИ (Aim Line)
            lineBuilder.vertex(matrix, (float)muzzlePos.x, (float)muzzlePos.y, (float)muzzlePos.z)
                    .color(TARGET_R, TARGET_G, TARGET_B, TARGET_A).normal(0, 1, 0).endVertex();
            lineBuilder.vertex(matrix, (float)targetPoint.x, (float)targetPoint.y, (float)targetPoint.z)
                    .color(TARGET_R, TARGET_G, TARGET_B, TARGET_A).normal(0, 1, 0).endVertex();

            // 4. ТРАЕКТОРИЯ (Trajectory)
            if (velocity != null) {
                Vec3 currentPos = muzzlePos;
                Vec3 currentVel = velocity;
                double gravity = 0.01;
                double drag = 0.99;
                for (int i = 0; i < 50; i++) {
                    Vec3 nextPos = currentPos.add(currentVel);
                    lineBuilder.vertex(matrix, (float)currentPos.x, (float)currentPos.y, (float)currentPos.z)
                            .color(TRAJ_R, TRAJ_G, TRAJ_B, TRAJ_A).normal(0, 1, 0).endVertex();
                    lineBuilder.vertex(matrix, (float)nextPos.x, (float)nextPos.y, (float)nextPos.z)
                            .color(TRAJ_R, TRAJ_G, TRAJ_B, TRAJ_A).normal(0, 1, 0).endVertex();

                    currentPos = nextPos;
                    currentVel = currentVel.scale(drag).subtract(0, gravity, 0);
                    if (currentPos.y < targetPoint.y - 2) break;
                }
            }
        } else {
            // === НЕТ ЦЕЛИ (IDLE AIM) ===
            float f = 0.017453292F;
            float x = -Mth.sin(yHeadRot * f) * Mth.cos(xRot * f);
            float y = -Mth.sin(xRot * f);
            float z =  Mth.cos(yHeadRot * f) * Mth.cos(xRot * f);

            Vec3 aimVec = new Vec3(x, y, z).normalize().scale(3.0);
            Vec3 endPos = muzzlePos.add(aimVec);

            lineBuilder.vertex(matrix, (float)muzzlePos.x, (float)muzzlePos.y, (float)muzzlePos.z)
                    .color(IDLE_R, IDLE_G, IDLE_B, IDLE_A).normal(0, 1, 0).endVertex();
            lineBuilder.vertex(matrix, (float)endPos.x, (float)endPos.y, (float)endPos.z)
                    .color(IDLE_R, IDLE_G, IDLE_B, 0.0F).normal(0, 1, 0).endVertex();
        }

        poseStack.popPose();
        bufferSource.endBatch(renderType);
    }

    private static void renderBox(Matrix4f matrix, VertexConsumer builder, Vec3 center, double r, float red, float green, float blue, float alpha) {
        float x1 = (float)(center.x - r);
        float y1 = (float)(center.y - r);
        float z1 = (float)(center.z - r);
        float x2 = (float)(center.x + r);
        float y2 = (float)(center.y + r);
        float z2 = (float)(center.z + r);

        // Нижний квадрат
        builder.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();

        // Верхний квадрат
        builder.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();

        // Вертикальные ребра
        builder.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
    }
}