package com.trd.block.basic.industrial.rotation;

import com.trd.api.energy.EnergyNetworkManager;
import com.trd.api.rotation.KineticNetworkManager;
import com.trd.block.entity.industrial.rotation.StatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class StatorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public StatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AXIS);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Направление от статора к блоку, на который кликнули
        Direction facing = context.getClickedFace().getOpposite();
        BlockPos clickedPos = context.getClickedPos();
        BlockState target = context.getLevel().getBlockState(clickedPos.relative(facing));

        Direction.Axis axis = Direction.Axis.Z; // Дефолт

        // 1. Если ставим на наш Вал: берем ось из свойства FACING
        if (target.getBlock() instanceof ShaftBlock) {
            axis = target.getValue(ShaftBlock.FACING).getAxis();
        }
        // 2. Если ставим на блок, у которого есть AXIS (например, колонны)
        else if (target.hasProperty(BlockStateProperties.AXIS)) {
            axis = target.getValue(BlockStateProperties.AXIS);
        }
        // 3. Если ставим на обычный блок (камень) - угадываем
        else {
            axis = facing.getAxis() == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
        }

        return this.defaultBlockState().setValue(FACING, facing).setValue(AXIS, axis);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && state.getBlock() != oldState.getBlock()) {
            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);
            KineticNetworkManager.get((ServerLevel) level).updateNetworkAfterPlace(pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);
            KineticNetworkManager.get((ServerLevel) level).updateNetworkAfterRemove(pos);
            
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, com.trd.block.entity.ModBlockEntities.STATOR_BE.get(), StatorBlockEntity.createTicker());
    }
}
