package com.cim.block.basic.industrial.energy;

import com.cim.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.cim.api.energy.EnergyNetworkManager;
import com.cim.block.entity.ModBlockEntities;
import com.cim.block.entity.industrial.energy.SwitchBlockEntity;

import javax.annotation.Nullable;

public class SwitchBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public SwitchBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(POWERED, false);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 1. Toggle logic
        boolean isPowered = !state.getValue(POWERED);
        BlockState newState = state.setValue(POWERED, isPowered);

        // 2. [ИСПРАВЛЕНО] Используем флаг 3 (UPDATE_ALL = CLIENTS | NEIGHBORS).
        level.setBlock(pos, newState, 3);

        // 3. Update BE State cache
        if(level.getBlockEntity(pos) instanceof SwitchBlockEntity be) {
            be.setBlockState(newState);
        }

        // 4. Handle Network Logic
        EnergyNetworkManager manager = EnergyNetworkManager.get((ServerLevel) level);
        manager.removeNode(pos);

        if (isPowered) {
            level.playSound(null, pos, ModSounds.LEVER1.get(), SoundSource.BLOCKS, 0.3f, 1.0f);
            manager.addNode(pos);
        } else {
            level.playSound(null, pos, ModSounds.LEVER1.get(), SoundSource.BLOCKS, 0.3f, 0.9f);
        }

        // 5. Notify neighbors
        level.updateNeighborsAt(pos, this);

        return InteractionResult.SUCCESS;
    }



    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        if (level.isClientSide) return;

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof SwitchBlockEntity switchEntity)) return;

        boolean hasRedstoneSignal = level.hasNeighborSignal(pos);

        if (hasRedstoneSignal && !switchEntity.isTriggered) {
            // Фронт (переход 0 -> 1): переключаем состояние
            switchEntity.isTriggered = true;
            boolean isPowered = !state.getValue(POWERED);
            BlockState newState = state.setValue(POWERED, isPowered);
            level.setBlock(pos, newState, 3);
            switchEntity.setBlockState(newState);

            EnergyNetworkManager manager = EnergyNetworkManager.get((ServerLevel) level);
            manager.removeNode(pos);

            if (isPowered) {
                level.playSound(null, pos, ModSounds.LEVER1.get(), SoundSource.BLOCKS, 0.3f, 1.0f);
                manager.addNode(pos);
            } else {
                level.playSound(null, pos, ModSounds.LEVER1.get(), SoundSource.BLOCKS, 0.3f, 0.9f);
            }
            level.updateNeighborsAt(pos, this);

        } else if (!hasRedstoneSignal && switchEntity.isTriggered) {
            // Спад (переход 1 -> 0): сбрасываем флаг, но не меняем состояние рубильника
            switchEntity.isTriggered = false;
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            if (state.getValue(POWERED)) {
                EnergyNetworkManager.get((ServerLevel) level).addNode(pos);
            }
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SwitchBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.SWITCH_BE.get(), SwitchBlockEntity::tick);
    }
}