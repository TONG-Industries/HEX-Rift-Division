package com.trd.multiblock.industrial;

import com.trd.block.basic.ModBlocks;
import com.trd.block.entity.ModBlockEntities;
import com.trd.item.tools.FluidIdentifierItem;
import com.trd.multiblock.system.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FuelTankBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static MultiblockStructureHelper helper;

    public FuelTankBlock(Properties properties) {
        // ФИКС: noOcclusion() отключает затемнение граней, не ломая lightmap
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
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 1.0F;
    }

    // ФИКС: пустой occlusion shape — блок не считается сплошным для соседей
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    // Возвращаем объединённый shape всего мультиблока — так любая часть структуры
    // выделяется целиком чёрной обводкой (как у Heater).
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return getStructureHelper().generateShapeFromParts(facing);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (helper == null) {
            Map<Character, Supplier<BlockState>> symbols = Map.of(
                    '#', () -> ModBlocks.MULTIBLOCK_PART.get().defaultBlockState(),
                    '@', () -> this.defaultBlockState(),
                    '$', () -> ModBlocks.MULTIBLOCK_PART.get().defaultBlockState(),
                    'L', () -> ModBlocks.MULTIBLOCK_PART.get().defaultBlockState()
            );

            Map<Character, PartRole> roles = Map.of(
                    '#', PartRole.DEFAULT,
                    '@', PartRole.CONTROLLER,
                    '$', PartRole.FLUID_CONNECTOR,
                    'L', PartRole.LADDER
            );

            String[][] layers = {
                    {
                            "##$L$##",
                            "###@###",
                            "##$L$##"
                    },
                    {
                            "###L###",
                            "#######",
                            "###L###"
                    },
                    {
                            "###L###",
                            "#######",
                            "###L###"
                    }};
            helper = MultiblockStructureHelper.createFromLayersWithRoles(
                    layers,
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
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FuelTankBlockEntity tank) {
                ItemStackHandler inv = tank.getInventory();
                for (int i = 0; i < inv.getSlots(); i++) {
                    ItemStack stack = inv.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            Direction facing = state.getValue(FACING);
            getStructureHelper().destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof FuelTankBlockEntity tank) {
                    String selectedFluidId = FluidIdentifierItem.getSelectedFluid(stack);
                    tank.setFilter(selectedFluidId);
                    if (selectedFluidId.equals("none")) {
                        player.displayClientMessage(Component.literal("§eФильтр сброшен (цистерна закрыта)"), true);
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
                    } else {
                        Fluid fluid = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(new net.minecraft.resources.ResourceLocation(selectedFluidId));
                        String fluidName = fluid != null ? Component.translatable(fluid.getFluidType().getDescriptionId()).getString() : selectedFluidId;
                        player.displayClientMessage(Component.literal("§aФильтр установлен: §f" + fluidName), true);
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.2F);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FuelTankBlockEntity tank) {
            net.minecraftforge.network.NetworkHooks.openScreen((ServerPlayer) player, tank, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FUEL_TANK_BE.get(), FuelTankBlockEntity::tick);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("Ёмкость: 768 000 mB").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Устойчив к коррозии и нагреву").withStyle(ChatFormatting.GREEN));
    }
}