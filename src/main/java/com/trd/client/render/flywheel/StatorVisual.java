package com.trd.client.render.flywheel;

import com.trd.block.basic.industrial.rotation.StatorBlock;
import com.trd.block.entity.industrial.rotation.StatorBlockEntity;
import com.trd.item.ModItems;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StatorVisual extends AbstractBlockEntityVisual<StatorBlockEntity> implements SimpleDynamicVisual {

    private final TransformedInstance statorInstance;
    private final List<TransformedInstance> coilInstances = new ArrayList<>();

    private final Direction.Axis axis;
    private final float localX;
    private final float localY;
    private final float localZ;

    public StatorVisual(VisualizationContext ctx, StatorBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
        this.axis = blockState.getValue(StatorBlock.AXIS);

        Vec3i origin = ctx.renderOrigin();
        this.localX = pos.getX() - origin.getX();
        this.localY = pos.getY() - origin.getY();
        this.localZ = pos.getZ() - origin.getZ();

        this.statorInstance = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(ModModels.STATOR)).createInstance();
        setupBaseTransform(this.statorInstance);
        
        rebuildCoils();
        updateLight(partialTick);
    }

    private void setupBaseTransform(TransformedInstance instance) {
        Direction facing = blockState.getValue(StatorBlock.FACING);

        instance.setIdentityTransform()
                .translate(localX, localY, localZ)
                .translate(0.5f, 0.5f, 0.5f);

        if (axis == Direction.Axis.X) {
            instance.rotateY((float) Math.toRadians(90));
        } else if (axis == Direction.Axis.Z) {
            // default
        } else if (axis == Direction.Axis.Y) {
            if (facing == Direction.WEST) {
                instance.rotateY((float) Math.toRadians(90));
            } else if (facing == Direction.SOUTH) {
                instance.rotateY((float) Math.toRadians(180));
            } else if (facing == Direction.EAST) {
                instance.rotateY((float) Math.toRadians(270));
            }
            // NORTH is 0 degrees
            instance.rotateX((float) Math.toRadians(90));
        }

        instance.translate(-0.5f, -0.5f, -0.5f);
        instance.setChanged();
    }

    private void rebuildCoils() {
        coilInstances.forEach(Instance::delete);
        coilInstances.clear();

        IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    boolean isCopper = handler.getStackInSlot(i).getItem() == ModItems.COPPER_COIL.get();
                    // Assuming for now we only have copper coil, or fallback to it
                    TransformedInstance coil = instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, Models.partial(ModModels.STATOR_COIL_COPPER))
                            .createInstance();

                    coil.setIdentityTransform()
                        .translate(localX, localY, localZ)
                        .translate(0.5f, 0.5f, 0.5f);

                    Direction facing = blockState.getValue(StatorBlock.FACING);
                    if (axis == Direction.Axis.X) {
                        coil.rotateY((float) Math.toRadians(90));
                    } else if (axis == Direction.Axis.Z) {
                        // default
                    } else if (axis == Direction.Axis.Y) {
                        if (facing == Direction.WEST) {
                            coil.rotateY((float) Math.toRadians(90));
                        } else if (facing == Direction.SOUTH) {
                            coil.rotateY((float) Math.toRadians(180));
                        } else if (facing == Direction.EAST) {
                            coil.rotateY((float) Math.toRadians(270));
                        }
                        coil.rotateX((float) Math.toRadians(90));
                    }
                    
                    // Rotate for slot position
                    coil.rotateZ((float) Math.toRadians(i * 30));

                    coil.translate(-0.5f, -0.5f, -0.5f);
                    coil.setChanged();
                    coilInstances.add(coil);
                }
            }
        }
    }

    @Override
    public void beginFrame(Context ctx) {
        // Coils change infrequently (only when players insert/extract)
        // We can just rebuild them on demand or check if inventory changed.
        // For simplicity, we just rebuild if needed (can optimize later).
        rebuildCoils();
        for (TransformedInstance coil : coilInstances) {
            relight(pos, coil);
        }
    }

    @Override
    public void updateLight(float partialTick) {
        relight(pos, statorInstance);
        for (TransformedInstance coil : coilInstances) {
            relight(pos, coil);
        }
    }

    @Override
    protected void _delete() {
        statorInstance.delete();
        coilInstances.forEach(Instance::delete);
        coilInstances.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(statorInstance);
        for (TransformedInstance coil : coilInstances) {
            consumer.accept(coil);
        }
    }
}
