package com.cim.block.basic.industrial.rotation;

import com.cim.api.energy.EnergyNetworkManager;
import com.cim.api.rotation.KineticNetworkManager;
import com.cim.block.entity.ModBlockEntities;
import com.cim.block.entity.industrial.rotation.MotorElectroBlockEntity;
import com.cim.menu.MotorElectroMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MotorElectroBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MotorElectroBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // ===================== USE (только ПКМ → GUI) =====================

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof MotorElectroBlockEntity motor)) {
            return InteractionResult.PASS;
        }

        // Обычный ПКМ → открываем GUI
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,
                    new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return Component.translatable("gui.cim.motor_electro");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                            return new MotorElectroMenu(id, inv, motor, motor.dataAccess);
                        }
                    },
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    // ===================== РЕДСТОУН → ПЕРЕКЛЮЧЕНИЕ НАПРАВЛЕНИЯ =====================

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block fromBlock, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, fromBlock, fromPos, isMoving);
        if (level.isClientSide) return;

        if (!(level.getBlockEntity(pos) instanceof MotorElectroBlockEntity motor)) return;

        boolean hasRedstoneSignal = level.hasNeighborSignal(pos);

        if (hasRedstoneSignal && !motor.isTriggered) {
            // Фронт (LOW → HIGH): переключаем направление
            motor.isTriggered = true;
            motor.toggleDirection();
        } else if (!hasRedstoneSignal && motor.isTriggered) {
            // Спад (HIGH → LOW): сбрасываем флаг, направление НЕ меняем
            motor.isTriggered = false;
        }
    }

    // ===================== TICKER =====================

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> beType) {
        if (level.isClientSide) return null;
        return createTickerHelper(beType, ModBlockEntities.MOTOR_ELECTRO_BE.get(),
                MotorElectroBlockEntity.createTicker());
    }

    // ===================== PLACE / REMOVE =====================

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            KineticNetworkManager.get((ServerLevel) level).updateNetworkAfterPlace(pos);
            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            super.onRemove(state, level, pos, newState, isMoving);
            KineticNetworkManager.get((ServerLevel) level).updateNetworkAfterRemove(pos);
            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ===================== STATE / ENTITY =====================

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MotorElectroBlockEntity(pos, state);
    }
}
