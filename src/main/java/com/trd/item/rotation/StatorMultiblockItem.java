package com.trd.item.rotation;

import com.trd.block.basic.industrial.rotation.StatorBlock;
import com.trd.multiblock.system.IMultiblockController;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class StatorMultiblockItem extends BlockItem {

    public StatorMultiblockItem(Block block, Properties properties) {
        super(block, properties);
        if (!(block instanceof IMultiblockController)) {
            throw new IllegalArgumentException("StatorMultiblockItem can only be used with blocks that implement IMultiblockController!");
        }
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        IMultiblockController controller = (IMultiblockController) this.getBlock();
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (!state.hasProperty(StatorBlock.AXIS) || !state.hasProperty(StatorBlock.FACING)) {
            return false;
        }

        Direction.Axis axis = state.getValue(StatorBlock.AXIS);
        Direction facing = state.getValue(StatorBlock.FACING);

        // checkPlacement
        if (controller.getStructureHelper().checkPlacementStator(level, context.getClickedPos(), facing, axis, player)) {
            return super.placeBlock(context, state);
        } else {
            return false;
        }
    }
}
