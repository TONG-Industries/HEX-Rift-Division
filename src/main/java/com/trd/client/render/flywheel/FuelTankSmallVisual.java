package com.trd.client.render.flywheel;


import com.trd.multiblock.industrial.FuelTankSmallBlock;
import com.trd.multiblock.industrial.FuelTankSmallBlockEntity;
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

public class FuelTankSmallVisual extends AbstractBlockEntityVisual<FuelTankSmallBlockEntity> implements SimpleDynamicVisual {

    private final TransformedInstance model;
    private final Direction facing;

    // Локальные координаты относительно рендер-ориджина
    private final float localX;
    private final float localY;
    private final float localZ;

    public FuelTankSmallVisual(VisualizationContext ctx, FuelTankSmallBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        this.facing = blockState.getValue(FuelTankSmallBlock.FACING);

        Vec3i origin = ctx.renderOrigin();
        this.localX = pos.getX() - origin.getX();
        this.localY = pos.getY() - origin.getY();
        this.localZ = pos.getZ() - origin.getZ();

        // Загружаем модель цистерны как PartialModel
        this.model = instancerProvider().instancer(
                InstanceTypes.TRANSFORMED,
                Models.partial(ModModels.FUEL_TANK_SMALL)
        ).createInstance();

        setupTransform();
        updateLight(partialTick);
    }

    private void setupTransform() {
        model.setIdentityTransform()
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.5f, 0.5f);

        // Поворачиваем модель в зависимости от facing
        if (facing == Direction.SOUTH) {
            model.rotateY((float) Math.toRadians(180));
        } else if (facing == Direction.WEST) {
            model.rotateY((float) Math.toRadians(90));
        } else if (facing == Direction.EAST) {
            model.rotateY((float) Math.toRadians(270));
        }
        // NORTH — без поворота

        // Центрируем модель
        model.translate(-0.5f, -0.5f, -0.5f);
        
        // Отрегулировано по просьбе: сдвинуть на 1 блок назад (на юг, +1.0f по Z)
        model.translate(0.5f, 0.0f, 0.5f);

        model.setChanged();
    }

    @Override
    public void beginFrame(Context ctx) {
    }

    @Override
    public void updateLight(float partialTick) {
        relight(pos, model);
    }

    @Override
    protected void _delete() {
        model.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(model);
    }
}
