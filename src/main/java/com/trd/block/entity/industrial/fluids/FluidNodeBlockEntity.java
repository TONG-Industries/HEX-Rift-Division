package com.trd.block.entity.industrial.fluids;

import com.trd.api.fluids.system.FluidNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Абстрактный базовый класс для всех жидкостных BlockEntity.
 *
 * Содержит общую логику:
 * - Добавление узла в сеть при onLoad()
 * - Удаление узла из сети при setRemoved()
 */
public abstract class FluidNodeBlockEntity extends BlockEntity {

    protected FluidNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }
}
