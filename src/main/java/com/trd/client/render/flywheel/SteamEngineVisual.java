package com.trd.client.render.flywheel;

import com.trd.multiblock.industrial.SteamEngineBlock;
import com.trd.multiblock.industrial.SteamEngineBlockEntity;
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

public class SteamEngineVisual extends AbstractBlockEntityVisual<SteamEngineBlockEntity> implements SimpleDynamicVisual {

    private final TransformedInstance base;
    private final TransformedInstance crankshaft;
    private final TransformedInstance connectingRod;
    
    private final Direction facing;
    private final Direction.Axis crankshaftAxis;

    private final float localX;
    private final float localY;
    private final float localZ;

    public SteamEngineVisual(VisualizationContext ctx, SteamEngineBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        if (blockState.hasProperty(SteamEngineBlock.FACING)) {
            this.facing = blockState.getValue(SteamEngineBlock.FACING);
        } else {
            this.facing = Direction.NORTH;
        }
        
        this.crankshaftAxis = facing.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;

        Vec3i origin = ctx.renderOrigin();
        this.localX = pos.getX() - origin.getX();
        this.localY = pos.getY() - origin.getY();
        this.localZ = pos.getZ() - origin.getZ();

        this.base = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(ModModels.STEAM_ENGINE_BASE)).createInstance();
        this.crankshaft = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(ModModels.STEAM_ENGINE_CRANKSHAFT)).createInstance();
        this.connectingRod = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(ModModels.STEAM_ENGINE_ROD)).createInstance();

        setupStaticBase();
        updateLight(partialTick);
    }

    private void setupStaticBase() {
        base.setIdentityTransform()
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.5f, 0.5f);

        Direction.Axis axis = facing.getAxis();
        if (axis == Direction.Axis.X) {
            base.rotateY((float) Math.toRadians(facing == Direction.EAST ? 270 : 90));
        } else if (facing == Direction.SOUTH) {
            base.rotateY((float) Math.toRadians(180));
        }

        base.translate(0.0f, -0.5f, 0.0f);
        base.setChanged();
    }

    private float smoothedSpeed = 0f;
    private float currentAngle = 0f;
    private float lastFrameTime = -1.0f;

    @Override
    public void beginFrame(Context ctx) {
        float partialTick = net.minecraft.client.Minecraft.getInstance().getFrameTime();
        float timeInSeconds = (level.getGameTime() + partialTick) / 20.0f;

        if (this.lastFrameTime < 0) this.lastFrameTime = timeInSeconds;
        float deltaSeconds = timeInSeconds - this.lastFrameTime;
        this.lastFrameTime = timeInSeconds;

        float targetSpeed = blockEntity.getVisualSpeed();
        float maxRenderSpeed = 300f; 
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
        } else {
            this.smoothedSpeed = targetSpeed;
        }

        this.currentAngle += this.smoothedSpeed * ((float) Math.PI / 30.0f) * deltaSeconds;
        float twoPi = (float) (2 * Math.PI);
        this.currentAngle = this.currentAngle % twoPi;
        if (this.currentAngle < 0) this.currentAngle += twoPi;

        if (this.smoothedSpeed == targetSpeed && targetSpeed != 0) {
            float globalAngle = (timeInSeconds * targetSpeed * ((float) Math.PI / 30.0f)) % twoPi;
            if (globalAngle < 0) globalAngle += twoPi;

            float diff = (globalAngle - this.currentAngle) % twoPi;
            if (diff > Math.PI) diff -= twoPi;
            if (diff < -Math.PI) diff += twoPi;

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

        // --- АНИМАЦИЯ ---
        crankshaft.setIdentityTransform()
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.5f, 0.5f); 

        Direction.Axis axis = facing.getAxis();
        if (axis == Direction.Axis.X) {
            crankshaft.rotateY((float) Math.toRadians(facing == Direction.EAST ? 270 : 90));
        } else if (facing == Direction.SOUTH) {
            crankshaft.rotateY((float) Math.toRadians(180));
        }

        // Вращаем коленвал
        crankshaft.rotateZ(currentAngle); 
        
        // Сдвигаем модель в локальных осях (вниз и на пол пикселя на юг)
        crankshaft.translate(0.0f, -0.5625f, 0.03125f);
        crankshaft.setChanged();

        // Математика поршня
        float R = 0.41f;
        float L = 1.225f;
        
        // Инвертируем обе координаты, так как визуально пин находится на 180 градусов с другой стороны
        float xCrank = R * (float) Math.sin(currentAngle);
        float yCrank = -R * (float) Math.cos(currentAngle);

        float sinBeta = xCrank / L;
        float beta = (float) Math.asin(sinBeta);

        connectingRod.setIdentityTransform()
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.5f, 0.5f);

        if (axis == Direction.Axis.X) {
            connectingRod.rotateY((float) Math.toRadians(facing == Direction.EAST ? 270 : 90));
        } else if (facing == Direction.SOUTH) {
            connectingRod.rotateY((float) Math.toRadians(180));
        }

        // Сдвигаем вдоль оси (как у коленвала)
        connectingRod.translate(0.0f, 0.0f, -0.0625f);

        // Основная ось вращения находится точно по центру (0.5, 0.5, 0.5).
        // Сдвигаем шатун на расстояние R (пин коленвала)
        connectingRod.translate(xCrank, yCrank, 0);
        // Поворачиваем шатун на угол beta
        connectingRod.rotateZ(beta);
        
        connectingRod.setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(pos, base, crankshaft, connectingRod);
    }

    @Override
    protected void _delete() {
        base.delete();
        crankshaft.delete();
        connectingRod.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(base);
        consumer.accept(crankshaft);
        consumer.accept(connectingRod);
    }
}
