package com.trd.multiblock.industrial;

import com.trd.block.basic.ModBlocks;
import com.trd.block.entity.ModBlockEntities;
import com.trd.multiblock.system.IMultiblockController;
import com.trd.multiblock.system.MultiblockStructureHelper;
import com.trd.multiblock.system.PartRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Контроллер топки угольной электростанции.
 * Мультиблок 3x3x5 (прямоугольная башня, выше чем шире).
 *
 * Символы структуры:
 *   @ - контроллер (этот блок)
 *   # - корпус (MULTIBLOCK_PART, DEFAULT)
 *   C - вход угля (ITEM_INPUT)
 *   E - вход энергии (ENERGY_CONNECTOR)
 *   I - вход воды (FLUID_INPUT)
 *   O - выход пара (FLUID_OUTPUT)
 *   G - выход дыма (FLUID_CONNECTOR) сверху -> труба -> башня exhaust
 */
public class FireboxBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static MultiblockStructureHelper helper;

    public FireboxBlock(Properties properties) {
        super(properties.noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return getStructureHelper().generateShapeFromParts(facing);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (helper == null) {
            Supplier<net.minecraft.world.level.block.state.BlockState> part = () -> ModBlocks.MULTIBLOCK_PART.get().defaultBlockState();

            Map<Character, Supplier<net.minecraft.world.level.block.state.BlockState>> symbols = new HashMap<>();
            symbols.put('#', part);
            symbols.put('@', this::defaultBlockState);
            symbols.put('C', part);
            symbols.put('E', part);
            symbols.put('I', part);
            symbols.put('O', part);
            symbols.put('G', part);

            Map<Character, PartRole> roles = new HashMap<>();
            roles.put('#', PartRole.DEFAULT);
            roles.put('@', PartRole.CONTROLLER);
            roles.put('C', PartRole.ITEM_INPUT);
            roles.put('E', PartRole.ENERGY_CONNECTOR);
            roles.put('I', PartRole.FLUID_INPUT);
            roles.put('O', PartRole.FLUID_OUTPUT);
            roles.put('G', PartRole.FLUID_CONNECTOR);

            // 3x3 основание, высота 5 (снизу вверх)
            String[][] layers = {
                    { // y=0 (низ): контроллер и порты
                            "#C#",
                            "E@I",
                            "#O#"
                    },
                    { // y=1
                            "###",
                            "###",
                            "###"
                    },
                    { // y=2
                            "###",
                            "###",
                            "###"
                    },
                    { // y=3
                            "###",
                            "###",
                            "###"
                    },
                    { // y=4 (верх): выход дыма
                            "###",
                            "#G#",
                            "###"
                    }
            };

            helper = MultiblockStructureHelper.createFromLayersWithRoles(
                    layers,
                    symbols,
                    part,
                    roles
            );
        }
        return helper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return PartRole.DEFAULT;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            getStructureHelper().placeStructure(level, pos, facing, this);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            Direction facing = state.getValue(FACING);
            getStructureHelper().destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FireboxBlockEntity firebox) {
                NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, (MenuProvider) firebox, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FireboxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FIREBOX_BE.get(), FireboxBlockEntity::serverTick);
    }
}
