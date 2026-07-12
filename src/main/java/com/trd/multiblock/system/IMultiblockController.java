package com.trd.multiblock.system;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;

public interface IMultiblockController {

    MultiblockStructureHelper getStructureHelper();

    PartRole getPartRole(BlockPos localOffset);

    @Nullable
    default VoxelShape getCustomMasterVoxelShape(BlockState state) {
        return null;
    }

    default net.minecraft.world.item.ItemStack getDropStack(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new net.minecraft.world.item.ItemStack(state.getBlock());
    }

}