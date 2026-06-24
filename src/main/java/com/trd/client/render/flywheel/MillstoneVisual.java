package com.trd.client.render.flywheel;

import com.trd.block.entity.industrial.MillstoneBlockEntity;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MillstoneVisual extends AbstractBlockEntityVisual<MillstoneBlockEntity> implements SimpleDynamicVisual {

    private final TransformedInstance base;
    private final TransformedInstance top;

    // Локальные координаты относительно матрицы Engine Room
    private final float localX;
    private final float localY;
    private final float localZ;

    public MillstoneVisual(VisualizationContext ctx, MillstoneBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        // === ВЫЧИСЛЯЕМ ЛОКАЛЬНУЮ ПОЗИЦИЮ ===
        Vec3i origin = ctx.renderOrigin();
        this.localX = pos.getX() - origin.getX();
        this.localY = pos.getY() - origin.getY();
        this.localZ = pos.getZ() - origin.getZ();

        this.base = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(ModModels.JERNOVA_BASE)).createInstance();
        this.top = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(ModModels.JERNOVA_TOP)).createInstance();

        setupStaticBase();
        updateLight(partialTick);
    }

    private void setupStaticBase() {
        base.setIdentityTransform()
                .translate(localX, localY, localZ);
        base.setChanged();
    }

    private float visualAngle = 0f;
    private float lastFrameTime = -1.0f;

    @Override
    public void beginFrame(Context ctx) {
        float partialTick = net.minecraft.client.Minecraft.getInstance().getFrameTime();
        
        // Используем глобальное время для вычисления дельты
        float timeInSeconds = (level.getGameTime() + partialTick) / 20.0f;
        if (this.lastFrameTime < 0) this.lastFrameTime = timeInSeconds;
        float deltaSeconds = timeInSeconds - this.lastFrameTime;
        this.lastFrameTime = timeInSeconds;

        if (blockEntity.isGrinding()) {
            // Вращаем на 360 градусов за время кулдауна (1 секунда, если GRIND_COOLDOWN = 20)
            float speed = 360.0f / (MillstoneBlockEntity.GRIND_COOLDOWN / 20.0f);
            visualAngle += speed * deltaSeconds;
        } else {
            // Плавно доводим до ближайшего полного оборота, чтобы не застревало
            float targetSnap = Math.round(visualAngle / 360.0f) * 360.0f;
            float diff = targetSnap - visualAngle;
            if (Math.abs(diff) > 0.5f) {
                visualAngle += diff * 10.0f * deltaSeconds;
            } else {
                visualAngle = targetSnap;
            }
        }

        // Ограничиваем угол, чтобы float не переполнялся при долгой игре
        if (visualAngle >= 3600.0f) {
            visualAngle -= 3600.0f;
        }

        // Вращаем верхний меш (номер 1)
        top.setIdentityTransform()
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.0f, 0.5f) // Смещаем в центр по X и Z для вращения (ось Y)
                .rotateY((float) Math.toRadians(-visualAngle)) // Отрицательное или положительное значение
                .translate(-0.5f, 0.0f, -0.5f); // Возвращаем обратно
        
        top.setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(pos, base, top);
    }

    @Override
    protected void _delete() {
        base.delete();
        top.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(base);
        consumer.accept(top);
    }
}
