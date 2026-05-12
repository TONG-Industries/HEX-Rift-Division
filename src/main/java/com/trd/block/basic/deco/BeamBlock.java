package com.trd.block.basic.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BeamBlock extends DirectionalBlock {

    // 3 пикселя = 3/16 = 0.1875
    private static final double THIN_INSET = 3.0 / 16.0;
    // Толщина балки по бокам = 16 - 3*2 = 10 пикселей = 0.625
    private static final double THICKNESS = 1.0 - (THIN_INSET * 2);

    // Кэш шейпов для всех 6 направлений
    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        for (Direction dir : Direction.values()) {
            SHAPES[dir.get3DDataValue()] = calculateShape(dir);
        }
    }

    public BeamBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get3DDataValue()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getShape(state, level, pos, CollisionContext.empty());
    }

    private static VoxelShape calculateShape(Direction facing) {
        double minX = 0, minY = 0, minZ = 0;
        double maxX = 1, maxY = 1, maxZ = 1;

        // Балка ориентирована по оси facing — фронт/тыл полные
        // Уменьшаем перпендикулярные направления (бока)

        switch (facing) {
            case NORTH, SOUTH -> {
                // Балка по Z, бока по X (уменьшаем), Y полный
                minX = THIN_INSET;
                maxX = 1.0 - THIN_INSET;
                // Y и Z остаются 0..1 (полные)
            }
            case WEST, EAST -> {
                // Балка по X, бока по Z (уменьшаем), Y полный
                minZ = THIN_INSET;
                maxZ = 1.0 - THIN_INSET;
                // Y и X остаются 0..1 (полные)
            }
            case UP, DOWN -> {
                // Балка по Y, бока по X и Z (уменьшаем оба!)
                minX = THIN_INSET;
                maxX = 1.0 - THIN_INSET;
                minZ = THIN_INSET;
                maxZ = 1.0 - THIN_INSET;
                // Y остаётся 0..1 (полные верх/низ)
            }
        }

        return Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}