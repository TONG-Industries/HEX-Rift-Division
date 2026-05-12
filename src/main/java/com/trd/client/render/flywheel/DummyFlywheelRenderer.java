package com.trd.client.render.flywheel;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Важный костыль для Flywheel 1.0.
 * Чтобы Flywheel мог находить наши BlockEntity после перезагрузки чанков (F3+A) или смены дистанции прорисовки,
 * они ДОЛЖНЫ попадать в список renderableBlockEntities ванильного чанка.
 * Ванильный Minecraft добавляет их туда, ТОЛЬКО ЕСЛИ для них зарегистрирован BlockEntityRenderer.
 * Этот рендерер ничего не рисует, но позволяет Flywheel подхватывать блоки при перестроении чанков.
 */
public class DummyFlywheelRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    
    public DummyFlywheelRenderer(BlockEntityRendererProvider.Context context) {
        // Dummy
    }

    @Override
    public void render(T be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Ничего не делаем, реальный рендер берет на себя Flywheel
    }

    @Override
    public boolean shouldRenderOffScreen(T pBlockEntity) {
        return true; // ВАЖНО: без этого Embeddium может выкинуть BlockEntity из списка рендера!
    }

    @Override
    public int getViewDistance() {
        return 256; // Увеличиваем дистанцию видимости, чтобы Flywheel мог рендерить издалека
    }
}
