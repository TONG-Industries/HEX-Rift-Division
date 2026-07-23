package com.trd.client.render.visual;

import com.trd.block.basic.industrial.rotation.HandCrankBlock;
import com.trd.block.entity.industrial.rotation.HandCrankBlockEntity;
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

public class HandCrankVisual extends AbstractBlockEntityVisual<HandCrankBlockEntity> implements SimpleDynamicVisual {
    private final TransformedInstance handle;
    
    private final Direction facing;
    private final float localX;
    private final float localY;
    private final float localZ;

    private float smoothedSpeed = 0f;
    private float currentAngle = 0f;
    private float lastFrameTime = -1.0f;

    public HandCrankVisual(VisualizationContext ctx, HandCrankBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        if (blockState.hasProperty(HandCrankBlock.FACING)) {
            this.facing = blockState.getValue(HandCrankBlock.FACING);
        } else {
            this.facing = Direction.NORTH;
        }

        Vec3i origin = ctx.renderOrigin();
        this.localX = pos.getX() - origin.getX();
        this.localY = pos.getY() - origin.getY();
        this.localZ = pos.getZ() - origin.getZ();

        this.handle = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(com.trd.client.render.flywheel.ModModels.HAND_CRANK)).createInstance();

        updateLight(partialTick);
    }

    @Override
    public void beginFrame(Context context) {
        float physicalTargetSpeed = blockEntity.getVisualSpeed();
        float partialTick = net.minecraft.client.Minecraft.getInstance().getFrameTime();
        float timeInSeconds = (level.getGameTime() + partialTick) / 20.0f;

        if (this.lastFrameTime < 0) this.lastFrameTime = timeInSeconds;
        float deltaSeconds = timeInSeconds - this.lastFrameTime;
        this.lastFrameTime = timeInSeconds;

        float targetSpeed = physicalTargetSpeed;

        if (this.smoothedSpeed == 0 && targetSpeed != 0) {
            this.smoothedSpeed = targetSpeed;
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

        handle.setIdentityTransform()
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.5f, 0.5f);

        // Orient handle to facing
        Direction.Axis axis = facing.getAxis();
        if (axis == Direction.Axis.X) {
            handle.rotateY((float) Math.toRadians(facing == Direction.EAST ? 270 : 90));
        } else if (axis == Direction.Axis.Z) {
            if (facing == Direction.SOUTH) {
                handle.rotateY((float) Math.toRadians(180));
            }
        } else if (axis == Direction.Axis.Y) {
            handle.rotateX((float) Math.toRadians(facing == Direction.UP ? 270 : 90));
        }

        // Apply rotation
        handle.rotateZ(currentAngle);
        
        // The model is already offset by 0.5 according to the user, so no need to translate back if it's already centered at origin natively
        // Wait, "Модель уже выставлена со сдвигом в 0.5 по всем осям" means the vertices are likely from 0 to 1.
        // So translating back -0.5 is needed to rotate around center, OR the user meant it's already centered.
        // If it's already shifted by 0.5, we just translate -0.5 after rotation.
        handle.translate(-0.5f, -0.5f, -0.5f);
        handle.setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(pos, handle);
    }

    @Override
    protected void _delete() {
        handle.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(handle);
    }
}
