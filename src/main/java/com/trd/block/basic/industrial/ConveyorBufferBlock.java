package com.trd.block.basic.industrial;

import com.trd.block.entity.industrial.ConveyorBufferBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public abstract class ConveyorBufferBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    protected static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public ConveyorBufferBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // === ФИКС УСТАНОВКИ ===
    // Front (cargo_port) теперь смотрит НА игрока, как печка.
    // Без .getOpposite() front смотрел от игрока (как раздатчик).
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ConveyorBufferBlockEntity buffer) {
                NetworkHooks.openScreen(serverPlayer, buffer, buf -> buf.writeBlockPos(pos));
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ConveyorBufferBlockEntity buffer) buffer.dropAllContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, com.trd.block.entity.ModBlockEntities.CONVEYOR_BUFFER_BE.get(), ConveyorBufferBlockEntity::tick);
    }
}