package com.trd.client.render.flywheel;

import com.trd.api.rotation.ShaftDiameter;
import com.trd.api.rotation.ShaftMaterial;
import com.trd.block.basic.industrial.rotation.BearingBlock;
import com.trd.block.entity.industrial.rotation.BearingBlockEntity;
import com.trd.main.MainRegistry;
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

public class BearingVisual extends AbstractBlockEntityVisual<BearingBlockEntity> implements SimpleDynamicVisual {

    private final TransformedInstance innerRing;
    @Nullable
    private TransformedInstance shaft;

    private final Direction facing;
    private ShaftMaterial currentMaterial;
    private ShaftDiameter currentDiameter;

    // Локальные координаты
    private final float localX;
    private final float localY;
    private final float localZ;

    public BearingVisual(VisualizationContext ctx, BearingBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
        this.facing = blockState.getValue(BearingBlock.FACING);

        Vec3i origin = ctx.renderOrigin();
        this.localX = pos.getX() - origin.getX();
        this.localY = pos.getY() - origin.getY();
        this.localZ = pos.getZ() - origin.getZ();

        this.innerRing = instancerProvider().instancer(
                InstanceTypes.TRANSFORMED,
                Models.partial(ModModels.BEARING_INNER_RING)
        ).createInstance();

        this.currentMaterial = blockEntity.getShaftMaterial();
        this.currentDiameter = blockEntity.getShaftDiameter();

        createShaftInstance();
        setupStaticPositions();
        updateLight(partialTick);

        // DEBUG: Логируем создание визуала для отслеживания бага с прозрачностью
        if (MainRegistry.LOGGER.isInfoEnabled()) {
            MainRegistry.LOGGER.info("[trd-Visual] BearingVisual CREATED at {} | hasShaft={}", pos, blockEntity.hasShaft());
        }
    }

    private void createShaftInstance() {
        if (blockEntity.hasShaft() && currentMaterial != null && currentDiameter != null) {
            // ИСПОЛЬЗУЕМ .name(), чтобы избежать багов с переопределенным toString()
            String matName = currentMaterial.name().toLowerCase();
            String diaName = currentDiameter.name().toLowerCase();
            String shaftName = "shaft_" + diaName + "_" + matName;

            PartialModel shaftModel = ModModels.SHAFT_MODELS.get(shaftName);
            if (shaftModel == null) {
                // Если модель не зарегистрирована, логируем это, а не просто крашимся
                System.out.println("[trd-Debug] ВНИМАНИЕ: Модель " + shaftName + " не найдена в ModModels! Используем HALF_SHAFT.");
                shaftModel = ModModels.HALF_SHAFT;
            }

            this.shaft = instancerProvider().instancer(
                    InstanceTypes.TRANSFORMED,
                    Models.partial(shaftModel)
            ).createInstance();
        }
    }

    private void setupStaticPositions() {
        applyStaticTransform(this.innerRing);
        if (this.shaft != null) {
            applyStaticTransform(this.shaft);
        }
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

    private float smoothedSpeed = 0f;
    private float currentAngle = 0f;
    private boolean phaseSynced = false;
    private float lastFrameTime = -1.0f;

    @Override
    public void beginFrame(Context ctx) {
        // 1. АБСОЛЮТНАЯ ЗАЩИТА ОТ ДЕСИНКА
        // Проверяем актуальное состояние NBT каждый кадр
        boolean shaftStateChanged = blockEntity.hasShaft() != (this.shaft != null);
        boolean materialChanged = blockEntity.getShaftMaterial() != currentMaterial;
        boolean diameterChanged = blockEntity.getShaftDiameter() != currentDiameter;

        // Если что-то изменилось (например, мы вставили вал) — мгновенно пересобираем
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
                relight(pos, this.shaft); // Считаем свет для нового вала
            }
        }

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
        
        applyRotation(this.innerRing, currentAngle);
        if (this.shaft != null) {
            applyRotation(this.shaft, currentAngle);
        }
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
            relight(pos, this.innerRing, this.shaft);
        } else {
            relight(pos, this.innerRing);
        }
    }

    @Override
    protected void _delete() {
        this.innerRing.delete();
        if (this.shaft != null) {
            this.shaft.delete();
        }
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(this.innerRing);
        if (this.shaft != null) {
            consumer.accept(this.shaft);
        }
    }
}