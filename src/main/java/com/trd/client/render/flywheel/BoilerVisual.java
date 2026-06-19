package com.trd.client.render.flywheel;

import com.trd.multiblock.industrial.BoilerBlock;
import com.trd.multiblock.industrial.BoilerBlockEntity;
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

public class BoilerVisual extends AbstractBlockEntityVisual<BoilerBlockEntity> implements SimpleDynamicVisual {

    private final TransformedInstance model;
    private final Direction facing;

    // Локальные координаты относительно рендер-ориджина
    private final float localX;
    private final float localY;
    private final float localZ;

    public BoilerVisual(VisualizationContext ctx, BoilerBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        this.facing = blockState.getValue(BoilerBlock.FACING);

        Vec3i origin = ctx.renderOrigin();
        this.localX = pos.getX() - origin.getX();
        this.localY = pos.getY() - origin.getY();
        this.localZ = pos.getZ() - origin.getZ();

        // Загружаем модель бойлера как PartialModel
        this.model = instancerProvider().instancer(
                InstanceTypes.TRANSFORMED,
                Models.partial(ModModels.BOILER)
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

        // Центрируем модель: бойлер 3x3x4, контроллер в центре нижнего слоя (1, 0, 1)
        model.translate(-0.5f, -0.5f, -0.5f);
        
        // Для 3x3 мы центрируем смещая на 1 блок по X и Z относительно контроллера
        // (чтобы модель рендерилась от угла, если ее пивот в углу (0,0,0) структуры)
        // Пивот у OBJ моделей часто в углу. Как и у FuelTank, смещаем:
        model.translate(-1.0f, 0.0f, -1.0f);

        model.setChanged();
    }

    @Override
    public void beginFrame(Context ctx) {
        // Статическая модель
    }

    @Override
    public void updateLight(float partialTick) {
        // Считаем свет для позиции контроллера
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
