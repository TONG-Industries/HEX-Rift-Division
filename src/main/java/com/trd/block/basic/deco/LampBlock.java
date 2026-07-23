package com.trd.block.basic.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LampBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    // Хитбоксы 8×3.25×8 — отзеркалены на 180° по всем осям

    // На полу (смотрит вверх) — прижат к потолку (отзеркалено)
    private static final VoxelShape SHAPE_UP = Block.box(4.0, 12.75, 4.0, 12.0, 16.0, 12.0);

    // На потолке (смотрит вниз) — прижат к полу (отзеркалено)
    private static final VoxelShape SHAPE_DOWN = Block.box(4.0, 0.0, 4.0, 12.0, 3.25, 12.0);

    // На стенах — торчит ВНУТРЬ комнаты от стены на 3.25 пикселя
    private static final VoxelShape SHAPE_NORTH = Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 3.25);     // северная стена
    private static final VoxelShape SHAPE_SOUTH = Block.box(4.0, 4.0, 12.75, 12.0, 12.0, 16.0);   // южная стена
    private static final VoxelShape SHAPE_EAST  = Block.box(12.75, 4.0, 4.0, 16.0, 12.0, 12.0);   // восточная стена
    private static final VoxelShape SHAPE_WEST  = Block.box(0.0, 4.0, 4.0, 3.25, 12.0, 12.0);     // западная стена

    public LampBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case UP    -> SHAPE_UP;
            case DOWN  -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos supportPos = pos.relative(direction);
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, direction.getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.level.LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (!state.canSurvive(level, currentPos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }
}