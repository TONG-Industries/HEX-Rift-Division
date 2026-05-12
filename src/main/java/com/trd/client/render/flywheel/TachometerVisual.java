package com.trd.client.render.flywheel;

import com.trd.api.rotation.ShaftDiameter;
import com.trd.api.rotation.ShaftMaterial;
import com.trd.block.basic.industrial.rotation.TachometerBlock;
import com.trd.block.entity.industrial.rotation.TachometerBlockEntity;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static net.minecraft.core.Direction.EAST;
import static net.minecraft.core.Direction.UP;

public class TachometerVisual extends AbstractBlockEntityVisual<TachometerBlockEntity> implements SimpleDynamicVisual {

    private final TransformedInstance body;
    @Nullable
    private TransformedInstance shaft;

    private final Direction facing;
    private ShaftMaterial currentMaterial;
    private ShaftDiameter currentDiameter;

    private final float localX;
    private final float localY;
    private final float localZ;

    public TachometerVisual(VisualizationContext ctx, TachometerBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
        this.facing = blockState.getValue(TachometerBlock.FACING);

        Vec3i origin = ctx.renderOrigin();
        this.localX = pos.getX() - origin.getX();
        this.localY = pos.getY() - origin.getY();
        this.localZ = pos.getZ() - origin.getZ();

        // Статический корпус тахометра (НЕ вращается)
        this.body = instancerProvider().instancer(
                InstanceTypes.TRANSFORMED,
                Models.partial(ModModels.TACHOMETER)
        ).createInstance();

        this.currentMaterial = blockEntity.getShaftMaterial();
        this.currentDiameter = blockEntity.getShaftDiameter();

        createShaftInstance();
        setupStaticBody();
        if (this.shaft != null) {
            applyStaticTransform(this.shaft);
        }
        updateLight(partialTick);
    }

    private void createShaftInstance() {
        if (blockEntity.hasShaft() && currentMaterial != null && currentDiameter != null) {
            String matName = currentMaterial.name().toLowerCase();
            String diaName = currentDiameter.name().toLowerCase();
            String shaftName = "shaft_" + diaName + "_" + matName;

            PartialModel shaftModel = ModModels.SHAFT_MODELS.get(shaftName);
            if (shaftModel == null) {
                System.out.println("[trd-Debug] ВНИМАНИЕ: Модель " + shaftName + " не найдена! Используем HALF_SHAFT.");
                shaftModel = ModModels.HALF_SHAFT;
            }

            this.shaft = instancerProvider().instancer(
                    InstanceTypes.TRANSFORMED,
                    Models.partial(shaftModel)
            ).createInstance();
        }
    }

    private void setupStaticBody() {
        applyStaticTransform(this.body);
    }

    private void applyStaticTransform(TransformedInstance instance) {
        instance.setIdentityTransform()
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.5f, 0.5f);

        Direction.Axis axis = facing.getAxis();
        if (axis == Direction.Axis.X) {
            instance.rotateY((float) Math.toRadians(facing == EAST ? 270 : 90));
        } else if (axis == Direction.Axis.Y) {
            instance.rotateX((float) Math.toRadians(facing == UP ? 90 : -90));
        } else if (facing == Direction.SOUTH) {
            instance.rotateY((float) Math.toRadians(180));
        }

        instance.translate(-0.5f, -0.5f, -0.5f);
        instance.setChanged();
    }

    // --- Плавное вращение вала ---
    private float smoothedSpeed = 0f;
    private float currentAngle = 0f;
    private boolean phaseSynced = false;
    private float lastFrameTime = -1.0f;

    @Override
    public void beginFrame(Context ctx) {
        // 1. Проверка состояния вала
        boolean shaftStateChanged = blockEntity.hasShaft() != (this.shaft != null);
        boolean materialChanged = blockEntity.getShaftMaterial() != currentMaterial;
        boolean diameterChanged = blockEntity.getShaftDiameter() != currentDiameter;

        if (shaftStateChanged || materialChanged || diameterChanged) {
            this.currentMaterial = blockEntity.getShaftMaterial();
            this.currentDiameter = blockEntity.getShaftDiameter();

            if (this.shaft != null) {
                this.shaft.delete();
                this.shaft = null;
            }

            createShaftInstance();

            if (this.shaft != null) {
                applyStaticTransform(this.shaft);
                relight(pos, this.shaft);
            }
        }

        // Если нет вала — нечего вращать
        if (this.shaft == null) return;

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

        // Вращаем ТОЛЬКО вал, корпус остаётся неподвижным
        applyRotation(this.shaft, currentAngle);
    }

    private void applyRotation(TransformedInstance instance, float angle) {
        instance.setIdentityTransform()
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.5f, 0.5f);

        Direction.Axis axis = facing.getAxis();
        if (axis == Direction.Axis.X) {
            instance.rotateY((float) Math.toRadians(facing == EAST ? 270 : 90));
        } else if (axis == Direction.Axis.Y) {
            instance.rotateX((float) Math.toRadians(facing == UP ? 90 : -90));
        } else if (facing == Direction.SOUTH) {
            instance.rotateY((float) Math.toRadians(180));
        }

        instance.rotateZ(angle);
        instance.translate(-0.5f, -0.5f, -0.5f);
        instance.setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        if (this.shaft != null) {
            relight(pos, this.body, this.shaft);
        } else {
            relight(pos, this.body);
        }
    }

    @Override
    protected void _delete() {
        this.body.delete();
        if (this.shaft != null) {
            this.shaft.delete();
        }
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(this.body);
        if (this.shaft != null) {
            consumer.accept(this.shaft);
        }
    }
}
