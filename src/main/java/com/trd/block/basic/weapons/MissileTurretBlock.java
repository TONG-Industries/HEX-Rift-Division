package com.trd.block.basic.weapons;

import com.trd.block.entity.ModBlockEntities;
import com.trd.block.entity.weapons.MissileTurretBlockEntity;
import com.trd.menu.TromboneMenu;
import com.trd.api.energy.EnergyNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
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
import org.jetbrains.annotations.Nullable;

public class MissileTurretBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public MissileTurretBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MissileTurretBlockEntity(pos, state);
    }

    // === ЭНЕРГОСЕТЬ (1:1 с TurretLightPlacerBlock) ===
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MissileTurretBlockEntity turret) {
            turret.onPlace();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                // Выбрасываем содержимое инвентаря
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof MissileTurretBlockEntity turretBE) {
                    com.trd.block.entity.weapons.TurretAmmoContainer container = turretBE.getAmmoContainer();
                    for (int i = 0; i < container.getSlots(); i++) {
                        net.minecraft.world.item.ItemStack stack = container.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            net.minecraft.world.Containers.dropItemStack(
                                    level, pos.getX(), pos.getY(), pos.getZ(), stack);
                        }
                    }
                }
                EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MissileTurretBlockEntity turret) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer,
                        new net.minecraft.world.SimpleMenuProvider(
                                (windowId, playerInventory, playerEntity) ->
                                        new TromboneMenu(windowId, playerInventory,
                                                turret.getAmmoContainer(),
                                                turret.getDataAccess(),
                                                pos),
                                net.minecraft.network.chat.Component.literal("Trombone")
                        ),
                        buf -> buf.writeBlockPos(pos)
                );
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.MISSILE_TURRET_BE.get(),
                (lvl, pos, st, be) -> MissileTurretBlockEntity.tick(lvl, pos, st, (MissileTurretBlockEntity) be));
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MissileTurretBlockEntity turret) {
            return turret.getCooldownProgress();
        }
        return 0;
    }
}