package com.trd.client.render.ber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trd.api.paint.IPaintableConduit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Рисует поверх базовой модели-куба:
 *  1) камуфляж — модель выбранного блока (его текстура), чуть увеличенная;
 *  2) чёрные жерла-дырки по центру каждой грани (текстура trd:block/conduit_core).
 * Генерик: обслуживает любой BlockEntity, реализующий IPaintableConduit (труба и провод).
 */
public class PaintableConduitRenderer<T extends BlockEntity & IPaintableConduit> implements BlockEntityRenderer<T> {

    private static final ResourceLocation HOLE_TEX = new ResourceLocation("trd", "block/conduit_core");
    private static final float LO = 0.28F, HI = 0.72F, EPS = 0.006F;

    public PaintableConduitRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState mimic = be.getMimicState();

        // 1) камуфляж
        if (mimic != null && !mimic.isAir()) {
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(1.004F, 1.004F, 1.004F);
            pose.translate(-0.5, -0.5, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    mimic, pose, buffers, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
        }

        // 2) чёрные жерла
// было:
// TextureAtlasSprite sprite = Minecraft.getInstance()
//         .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(HOLE_TEX);

// стало — текстура ядра берётся у конкретного блока:
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(be.getCoreTexture());
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        for (Direction dir : Direction.values()) {
            renderHole(pose, vc, sprite, dir, light);
        }
    }

    private void renderHole(PoseStack pose, VertexConsumer vc, TextureAtlasSprite s, Direction dir, int light) {
        Matrix4f mat = pose.last().pose();
        Matrix3f nrm = pose.last().normal();
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        float nx = dir.getStepX(), ny = dir.getStepY(), nz = dir.getStepZ();
        float[][] c = corners(dir);
        quadVertex(vc, mat, nrm, c[0], u0, v1, nx, ny, nz, light);
        quadVertex(vc, mat, nrm, c[1], u1, v1, nx, ny, nz, light);
        quadVertex(vc, mat, nrm, c[2], u1, v0, nx, ny, nz, light);
        quadVertex(vc, mat, nrm, c[3], u0, v0, nx, ny, nz, light);
    }

    private void quadVertex(VertexConsumer vc, Matrix4f mat, Matrix3f nrm, float[] p,
                            float u, float v, float nx, float ny, float nz, int light) {
        vc.vertex(mat, p[0], p[1], p[2])
          .color(255, 255, 255, 255)
          .uv(u, v)
          .overlayCoords(OverlayTexture.NO_OVERLAY)
          .uv2(light)
          .normal(nrm, nx, ny, nz)
          .endVertex();
    }

    // Углы квадрата-дырки (с выносом наружу на EPS). Рендер-тип без куллинга,
    // поэтому порядок обхода не критичен.
    private static float[][] corners(Direction dir) {
        float lo = LO, hi = HI, e = EPS;
        switch (dir) {
            case NORTH: { float z = -e;    return new float[][]{{hi,lo,z},{lo,lo,z},{lo,hi,z},{hi,hi,z}}; }
            case SOUTH: { float z = 1 + e; return new float[][]{{lo,lo,z},{hi,lo,z},{hi,hi,z},{lo,hi,z}}; }
            case WEST:  { float x = -e;    return new float[][]{{x,lo,lo},{x,lo,hi},{x,hi,hi},{x,hi,lo}}; }
            case EAST:  { float x = 1 + e; return new float[][]{{x,lo,hi},{x,lo,lo},{x,hi,lo},{x,hi,hi}}; }
            case DOWN:  { float y = -e;    return new float[][]{{lo,y,lo},{hi,y,lo},{hi,y,hi},{lo,y,hi}}; }
            case UP:    { float y = 1 + e; return new float[][]{{lo,y,hi},{hi,y,hi},{hi,y,lo},{lo,y,lo}}; }
        }
        return new float[][]{{0,0,0},{0,0,0},{0,0,0},{0,0,0}};
    }

    @Override
    public boolean shouldRenderOffScreen(T be) { return false; }
}
