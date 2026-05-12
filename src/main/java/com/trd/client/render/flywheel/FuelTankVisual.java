package com.trd.client.render.flywheel;


import com.trd.multiblock.industrial.FuelTankBlock;
import com.trd.multiblock.industrial.FuelTankBlockEntity;
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

public class FuelTankVisual extends AbstractBlockEntityVisual<FuelTankBlockEntity> implements SimpleDynamicVisual {

    private final TransformedInstance model;
    private final Direction facing;

    // Локальные координаты относительно рендер-ориджина
    private final float localX;
    private final float localY;
    private final float localZ;

    public FuelTankVisual(VisualizationContext ctx, FuelTankBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        this.facing = blockState.getValue(FuelTankBlock.FACING);

        Vec3i origin = ctx.renderOrigin();
        this.localX = pos.getX() - origin.getX();
        this.localY = pos.getY() - origin.getY();
        this.localZ = pos.getZ() - origin.getZ();

        // Загружаем модель цистерны как PartialModel
        this.model = instancerProvider().instancer(
                InstanceTypes.TRANSFORMED,
                Models.partial(ModModels.FUEL_TANK_BIG)
        ).createInstance();

        setupTransform();
        updateLight(partialTick);
    }

    private void setupTransform() {
        model.setIdentityTransform()
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.5f, 0.5f);

        // Поворачиваем модель в зависимости от facing
        // Цистерна симметрична по Y, но может быть повёрнута по горизонтали
        if (facing == Direction.SOUTH) {
            model.rotateY((float) Math.toRadians(180));
        } else if (facing == Direction.WEST) {
            model.rotateY((float) Math.toRadians(90));
        } else if (facing == Direction.EAST) {
            model.rotateY((float) Math.toRadians(270));
        }
        // NORTH — без поворота

        // Центрируем модель: она 3×3×7, контроллер в центре нижнего слоя
        // Смещаем так, чтобы контроллер (центр 1,0,1 в локальных координатах мультиблока)
        // совпадал с центром блока (0.5, 0.5, 0.5)
        model.translate(-0.5f, -0.5f, -0.5f);

        // Дополнительное смещение если модель не совпадает с центром
        // Для 3×3×7: центр по X/Z = 1.5 блока от края, по Y = 3.5 блока
        // Контроллер находится в позиции (1, 0, 1) относительно угла мультиблока
        // Смещаем модель так, чтобы контроллер был в центре текущего блока
        model.translate(-1.0f, 0.0f, -1.0f);

        model.setChanged();
    }

    @Override
    public void beginFrame(Context ctx) {
        // Статическая модель — ничего не анимируем
        // Но можно добавить пульсацию жидкости или индикаторы уровня здесь
    }

    @Override
    public void updateLight(float partialTick) {
        // Считаем свет для центральной позиции контроллера
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