package com.trd.block.basic.industrial.rotation;

import com.trd.api.rotation.KineticNetworkManager;
import com.trd.api.rotation.ShaftDiameter;
import com.trd.block.entity.industrial.rotation.BearingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class BearingBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    // Свойство для визуала и возможных изменений хитбокса
    public static final BooleanProperty HAS_SHAFT = BooleanProperty.create("has_shaft");

    public BearingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_SHAFT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_SHAFT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BearingBlockEntity bearing)) return InteractionResult.PASS;
        ItemStack itemInHand = player.getItemInHand(hand);

        // 1. УСТАНОВКА ВАЛА
        if (!bearing.hasShaft() && itemInHand.getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof ShaftBlock shaftBlock) {
                if (shaftBlock.getDiameter() == ShaftDiameter.HEAVY) return InteractionResult.FAIL;

                if (!level.isClientSide) {
                    KineticNetworkManager manager = KineticNetworkManager.get((ServerLevel) level);

                    // ВАЖНО: Сначала меняем стейт! Теперь блок физически проводит энергию.
                    bearing.insertShaft(shaftBlock.getMaterial(), shaftBlock.getDiameter());
                    level.setBlock(pos, state.setValue(HAS_SHAFT, true), 3);
                    if (!player.isCreative()) itemInHand.shrink(1);

                    // ТЕПЕРЬ пересобираем сеть. Она увидит, что блок проводит энергию.
                    manager.updateNetworkAfterRemove(pos);
                    manager.updateNetworkAfterPlace(pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        // 2. ИЗВЛЕЧЕНИЕ ВАЛА
        if (bearing.hasShaft() && itemInHand.isEmpty() && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                KineticNetworkManager manager = KineticNetworkManager.get((ServerLevel) level);

                // ВАЖНО: Сначала удаляем вал! Теперь блок физически НЕ проводит энергию.
                bearing.removeShaft();
                level.setBlock(pos, state.setValue(HAS_SHAFT, false), 3);

                // ТЕПЕРЬ пересобираем сеть. Поиск упрется в пустоту, и сеть разорвется.
                manager.updateNetworkAfterRemove(pos);
                manager.updateNetworkAfterPlace(pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }




        return super.use(state, level, pos, player, hand, hit);
    }

    // --- ОБЯЗАТЕЛЬНЫЕ КИНЕТИЧЕСКИЕ ИВЕНТЫ ---

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && state.getBlock() != oldState.getBlock()) {
            KineticNetworkManager.get((ServerLevel) level).updateNetworkAfterPlace(pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            // Если ломаем блок с валом внутри - дропаем вал на землю
            if (state.getValue(HAS_SHAFT)) {
                // Block.popResource(level, pos, new ItemStack(...));
            }

            super.onRemove(state, level, pos, newState, isMoving);
            KineticNetworkManager.get((ServerLevel) level).updateNetworkAfterRemove(pos);
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
        return new BearingBlockEntity(pos, state);
    }
}
