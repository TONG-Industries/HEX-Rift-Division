package com.trd.block.basic.industrial;

import com.trd.block.entity.industrial.ConveyorBufferBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ConveyorInserterBlock extends ConveyorBufferBlock {
    public ConveyorInserterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorBufferBlockEntity(pos, state, ConveyorBufferBlockEntity.Mode.INSERTER);
    }
}