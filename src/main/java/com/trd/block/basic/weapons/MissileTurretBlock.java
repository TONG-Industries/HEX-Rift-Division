package com.trd.block.basic.weapons;

import com.trd.block.basic.ModBlocks;
import com.trd.block.entity.ModBlockEntities;
import com.trd.block.entity.weapons.MissileAmmoContainer;
import com.trd.block.entity.weapons.MissileTurretBlockEntity;
import com.trd.menu.TromboneMenu;
import com.trd.api.energy.EnergyNetworkManager;
import com.trd.multiblock.system.IMultiblockController;
import com.trd.multiblock.system.MultiblockStructureHelper;
import com.trd.multiblock.system.PartRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import java.util.Map;
import java.util.function.Supplier;

public class MissileTurretBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static MultiblockStructureHelper helper;

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

    // === ОБЩАЯ ФОРМА МУЛЬТИБЛОКА (outline + collision) ===
    // generateShapeFromParts собирает форму всех частей в единый VoxelShape.
    // MultiblockPartBlock сам смещает её при наведении на структурный блок.
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return getStructureHelper().generateShapeFromParts(facing);
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

    // === МУЛЬТИБЛОК: паттерн 1×1×2 ===
    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (helper == null) {
            Map<Character, Supplier<BlockState>> symbols = Map.of(
                    '#', () -> ModBlocks.MULTIBLOCK_PART.get().defaultBlockState(),
                    'O', () -> this.defaultBlockState()
            );
            Map<Character, PartRole> roles = Map.of(
                    '#', PartRole.DEFAULT,
                    'O', PartRole.CONTROLLER
            );
            // Слои снизу вверх: y=0 — контроллер (точка привязки), y=1 — структурный блок
            helper = MultiblockStructureHelper.createFromLayersWithRoles(
                    new String[][]{
                            {"O"}, // y = 0 — контроллер
                            {"#"}  // y = 1 — структурная часть
                    },
                    symbols,
                    () -> ModBlocks.MULTIBLOCK_PART.get().defaultBlockState(),
                    roles
            );
        }
        return helper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return PartRole.DEFAULT;
    }

    // === ПОСТРОЕНИЕ / РАЗРУШЕНИЕ ===
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            // Защита: если блок поставили не через MultiblockBlockItem (командой / другим модом)
            if (getStructureHelper().checkPlacement(level, pos, facing, placer instanceof Player ? (Player) placer : null)) {
                getStructureHelper().placeStructure(level, pos, facing, this);
            } else {
                // Место занято — откатываем установку контроллера и дропаем предмет
                level.destroyBlock(pos, true);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                Direction facing = state.getValue(FACING);
                getStructureHelper().destroyStructure(level, pos, facing);

                // Дропаем содержимое инвентаря
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof MissileTurretBlockEntity turretBE) {
                    MissileAmmoContainer container = turretBE.getMissileContainer();
                    for (int i = 0; i < container.getSlots(); i++) {
                        ItemStack itemStack = container.getStackInSlot(i);
                        if (!itemStack.isEmpty()) {
                            net.minecraft.world.Containers.dropItemStack(
                                    level, pos.getX(), pos.getY(), pos.getZ(), itemStack);
                        }
                    }
                }

                EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // === ЭНЕРГОСЕТЬ + TileEntity onPlace ===
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        // pos здесь — позиция контроллера, даже если клик был по структурной части
        // (MultiblockPartBlock перенаправляет hit с ctrlPos внутри своего use())
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MissileTurretBlockEntity turret) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer,
                        new net.minecraft.world.SimpleMenuProvider(
                                (windowId, playerInventory, playerEntity) ->
                                        new TromboneMenu(windowId, playerInventory,
                                                turret.getMissileContainer(),
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
                (lvl, p, st, be) -> MissileTurretBlockEntity.tick(lvl, p, st, (MissileTurretBlockEntity) be));
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