package com.trd.client.render.flywheel;

import com.trd.block.basic.industrial.rotation.MotorElectroBlock;
import com.trd.block.entity.industrial.rotation.MotorElectroBlockEntity;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MotorVisual extends AbstractBlockEntityVisual<MotorElectroBlockEntity> implements SimpleDynamicVisual {

    private final TransformedInstance base;
    private final TransformedInstance shaft;
    private final Direction facing;

    // Локальные координаты относительно матрицы Engine Room
    private final float localX;
    private final float localY;
    private final float localZ;

    public MotorVisual(VisualizationContext ctx, MotorElectroBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        if (blockState.hasProperty(MotorElectroBlock.FACING)) {
            this.facing = blockState.getValue(MotorElectroBlock.FACING);
        } else {
            this.facing = Direction.NORTH;
        }

        // === САМОЕ ВАЖНОЕ: ВЫЧИСЛЯЕМ ЛОКАЛЬНУЮ ПОЗИЦИЮ ===
        Vec3i origin = ctx.renderOrigin();
        this.localX = pos.getX() - origin.getX();
        this.localY = pos.getY() - origin.getY();
        this.localZ = pos.getZ() - origin.getZ();

        this.base = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(ModModels.MOTOR_BASE)).createInstance();
        this.shaft = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(ModModels.HALF_SHAFT)).createInstance();

        setupStaticBase();
        updateLight(partialTick);
    }

    private void setupStaticBase() {
        base.setIdentityTransform()
                // Используем localX, Y, Z вместо pos!
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.5f, 0.5f);

        Direction.Axis axis = facing.getAxis();
        if (axis == Direction.Axis.X) {
            base.rotateY((float) Math.toRadians(facing == Direction.EAST ? 270 : 90));
        } else if (axis == Direction.Axis.Y) {
            base.rotateX((float) Math.toRadians(facing == Direction.UP ? 90 : -90));
        } else if (facing == Direction.SOUTH) {
            base.rotateY((float) Math.toRadians(180));
        }

        base.translate(-0.5f, -0.5f, -0.5f);
        base.setChanged();
    }

    private float smoothedSpeed = 0f;
    private float currentAngle = 0f;
    private boolean phaseSynced = false;
    private float lastFrameTime = -1.0f;

    @Override
    public void beginFrame(Context ctx) {
// --- МАТЕМАТИКА ВРАЩЕНИЯ (Глобальная синхронизация) ---
        float partialTick = net.minecraft.client.Minecraft.getInstance().getFrameTime();
        float timeInSeconds = (level.getGameTime() + partialTick) / 20.0f;

        if (this.lastFrameTime < 0) this.lastFrameTime = timeInSeconds;
        float deltaSeconds = timeInSeconds - this.lastFrameTime;
        this.lastFrameTime = timeInSeconds;

        float physicalTargetSpeed = blockEntity.getVisualSpeed();

        // Ограничитель скорости для рендера (защита от стробоскопического эффекта на сверхвысоких RPM)
        float maxRenderSpeed = 300f; 
        float targetSpeed = physicalTargetSpeed;
        if (Math.abs(targetSpeed) > maxRenderSpeed) {
            targetSpeed = Math.signum(targetSpeed) * maxRenderSpeed;
        }

        if (this.smoothedSpeed == 0 && targetSpeed != 0) {
            this.smoothedSpeed = targetSpeed;
            this.currentAngle = (timeInSeconds * targetSpeed * ((float) Math.PI / 30.0f)) % ((float) Math.PI * 2);
            if (this.currentAngle < 0) this.currentAngle += (float) Math.PI * 2;
        }

        float speedDiff = targetSpeed - this.smoothedSpeed;
        if (Math.abs(speedDiff) > 0.1f) {
            this.smoothedSpeed += speedDiff * 4.0f * deltaSeconds;
            this.phaseSynced = false;
        } else {
            this.smoothedSpeed = targetSpeed;
        }

        this.currentAngle += this.smoothedSpeed * ((float) Math.PI / 30.0f) * deltaSeconds;
        float twoPi = (float) (2 * Math.PI);
        this.currentAngle = this.currentAngle % twoPi;
        if (this.currentAngle < 0) this.currentAngle += twoPi;

        if (this.smoothedSpeed == targetSpeed && targetSpeed != 0) {
            // Идеальный глобальный угол, одинаковый для всех блоков сети
            float globalAngle = (timeInSeconds * targetSpeed * ((float) Math.PI / 30.0f)) % twoPi;
            if (globalAngle < 0) globalAngle += twoPi;

            float diff = (globalAngle - this.currentAngle) % twoPi;
            if (diff > Math.PI) diff -= twoPi;
            if (diff < -Math.PI) diff += twoPi;

            // Плавно притягиваем текущий угол к идеальному, устраняя любые погрешности
            this.currentAngle += diff * 10.0f * deltaSeconds;
        }

        if (targetSpeed == 0 && Math.abs(this.smoothedSpeed) < 5.0f) {
            float PI_OVER_4 = (float) (Math.PI / 4.0);
            float targetSnap = Math.round(this.currentAngle / PI_OVER_4) * PI_OVER_4;
            float snapDiff = targetSnap - this.currentAngle;
            
            if (Math.abs(snapDiff) > 0.001f) {
                float pull = 8.0f * (1.0f - (Math.abs(this.smoothedSpeed) / 5.0f));
                this.currentAngle += snapDiff * pull * deltaSeconds;
            } else {
                this.currentAngle = targetSnap;
            }
        }
// --- КОНЕЦ МАТЕМАТИКИ ---

        shaft.setIdentityTransform()
                // Используем localX, Y, Z вместо pos!
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.5f, 0.5f);

        Direction.Axis axis = facing.getAxis();
        if (axis == Direction.Axis.X) {
            shaft.rotateY((float) Math.toRadians(facing == Direction.EAST ? 270 : 90));
        } else if (axis == Direction.Axis.Y) {
            shaft.rotateX((float) Math.toRadians(facing == Direction.UP ? 90 : -90));
        } else if (facing == Direction.SOUTH) {
            shaft.rotateY((float) Math.toRadians(180));
        }

        // Крутим плавно!
        shaft.rotateZ(currentAngle);

        shaft.translate(-0.5f, -0.5f, -0.5f);
        shaft.setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        // relight всегда требует АБСОЛЮТНЫЙ pos для проверки света в мире, здесь всё правильно
        relight(pos, base, shaft);
    }

    @Override
    protected void _delete() {
        base.delete();
        shaft.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(base);
        consumer.accept(shaft);
    }
}