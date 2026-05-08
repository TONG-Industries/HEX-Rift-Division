package com.cim.client.renderer;

import com.cim.block.basic.ModBlocks;
import com.cim.block.entity.deco.BeamCollisionBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class DynamicBeamModel implements IDynamicBakedModel {

    private final BakedModel baseModel;

    public DynamicBeamModel(BakedModel baseModel) {
        this.baseModel = baseModel;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();

        // Мы не рендерим грани отдельно, всё будет в общем списке (side == null)
        if (side != null) return quads;

        Vec3 startPos = extraData.get(BeamCollisionBlockEntity.START_POS);
        Vec3 endPos = extraData.get(BeamCollisionBlockEntity.END_POS);
        int[] segments = extraData.get(BeamCollisionBlockEntity.SEGMENTS);
        BlockPos myPos = extraData.get(BeamCollisionBlockEntity.MY_POS);

        if (startPos == null || endPos == null || segments == null || segments.length == 0 || myPos == null) {
            return quads;
        }

        double dx = endPos.x - startPos.x;
        double dy = endPos.y - startPos.y;
        double dz = endPos.z - startPos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance == 0) return quads;

        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        int fullBlocks = (int) distance;
        float remainder = (float) (distance - fullBlocks);

        // Получаем квады оригинальной балки (BEAM_BLOCK)
        List<BakedQuad> baseQuads = new ArrayList<>();
        baseQuads.addAll(baseModel.getQuads(ModBlocks.BEAM_BLOCK.get().defaultBlockState(), null, rand, ModelData.EMPTY, renderType));
        for (Direction dir : Direction.values()) {
            baseQuads.addAll(baseModel.getQuads(ModBlocks.BEAM_BLOCK.get().defaultBlockState(), dir, rand, ModelData.EMPTY, renderType));
        }

        if (baseQuads.isEmpty()) return quads;

        // Трансформируем модель для каждого сегмента
        for (int i : segments) {
            if (i > fullBlocks) continue;

            float scaleZ = (i == fullBlocks) ? remainder : 1.0f;
            if (scaleZ <= 0.001f) continue;

            // Локальные координаты начала отрезка относительно этого блока
            float localStartX = (float) (startPos.x - myPos.getX());
            float localStartY = (float) (startPos.y - myPos.getY());
            float localStartZ = (float) (startPos.z - myPos.getZ());

            Matrix4f matrix = new Matrix4f();
            matrix.identity();

            // 1. Сдвигаем к локальному старту балки
            matrix.translate(localStartX, localStartY, localStartZ);

            // 2. Поворачиваем (Yaw и Pitch)
            // В JOML повороты применяются в определенном порядке. Сначала вращаем по Y (yaw), затем по X (pitch)
            matrix.rotateY((float) Math.toRadians(yaw));
            matrix.rotateX((float) Math.toRadians(-pitch));

            // 3. Возвращаем центр (потому что мы вращали вокруг старта, а модель балки центрирована в 0.5)
            matrix.translate(-0.5f, -0.5f, 0.0f);

            // 4. Сдвигаем по длине на номер сегмента
            matrix.translate(0.0f, 0.0f, (float) i);

            // 5. Масштабируем (только для последнего неполного кусочка)
            if (i == fullBlocks) {
                matrix.scale(1.0f, 1.0f, scaleZ);
            }

            // Применяем матрицу к квадам
            IQuadTransformer transformer = QuadTransformers.applying(new com.mojang.math.Transformation(matrix));
            quads.addAll(transformer.process(baseQuads));
        }

        return quads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return baseModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() {
        return baseModel.getParticleIcon();
    }

    @Override
    public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
        return baseModel.getOverrides();
    }
}
