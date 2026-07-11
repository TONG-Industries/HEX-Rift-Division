package com.trd.multiblock.industrial;

import com.trd.block.basic.ModBlocks;
import com.trd.multiblock.system.IMultiblockController;
import com.trd.multiblock.system.MultiblockStructureHelper;
import com.trd.multiblock.system.PartRole;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SteelStorageBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static MultiblockStructureHelper helper;

    public SteelStorageBlock(Properties properties) {
        super(properties.noOcclusion().strength(2.5f, 6.0f));
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

    // ═══════════════════════════════════════════════════════
    // ФИКС: форма, коллизия и обводка для ВСЕГО мультиблока
    // ═══════════════════════════════════════════════════════
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return getStructureHelper().generateShapeFromParts(facing);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (helper == null) {
            Map<Character, Supplier<net.minecraft.world.level.block.state.BlockState>> symbols = Map.of(
                    '#', () -> ModBlocks.MULTIBLOCK_PART.get().defaultBlockState(),
                    'O', () -> this.defaultBlockState()
            );
            Map<Character, PartRole> roles = Map.of(
                    '#', PartRole.DEFAULT,
                    'O', PartRole.CONTROLLER
            );
            // 1 высота, 1 длина, 2 ширина (по X в паттерне)
            helper = MultiblockStructureHelper.createFromLayersWithRoles(
                    new String[][]{{"O#"}},
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

    // === ВОССТАНОВЛЕНИЕ ПРИ УСТАНОВКЕ ===
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            getStructureHelper().placeStructure(level, pos, facing, this);

            if (stack.hasTag() && level.getBlockEntity(pos) instanceof SteelStorageBlockEntity be) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("BlockEntityTag")) {
                    be.load(tag.getCompound("BlockEntityTag"));
                }
            }
        }
    }

    // === ДРОП (лут-таблица пустая, дропаем вручную с NBT) ===
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.emptyList();
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.getAbilities().instabuild) {
            super.playerWillDestroy(level, pos, state, player);
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SteelStorageBlockEntity storage && !level.isClientSide) {
            ItemStack stack = new ItemStack(this);
            if (!storage.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                storage.saveAdditional(tag);
                stack.addTagElement("BlockEntityTag", tag);
            }
            popResource(level, pos, stack);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getDropStack(Level level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof SteelStorageBlockEntity storage && !storage.isEmpty()) {
            CompoundTag tag = new CompoundTag();
            storage.saveAdditional(tag);
            stack.addTagElement("BlockEntityTag", tag);
        }
        return stack;
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
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SteelStorageBlockEntity be) {
            NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, be, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ═══════════════════════════════════════════════════════
    // ТУЛТИПЫ КАК В HBM (цвет + список предметов)
    // ═══════════════════════════════════════════════════════
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag == null || !blockEntityTag.contains("Inventory")) {
            tooltip.add(Component.literal("Пусто").withStyle(ChatFormatting.GRAY));
            return;
        }

        CompoundTag invTag = blockEntityTag.getCompound("Inventory");
        ItemStackHandler handler = new ItemStackHandler();
        handler.deserializeNBT(invTag);

        int filled = 0;
        int total = handler.getSlots();
        java.util.Map<String, Integer> items = new java.util.LinkedHashMap<>();

        for (int i = 0; i < total; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty()) {
                filled++;
                String name = s.getHoverName().getString();
                items.merge(name, s.getCount(), Integer::sum);
            }
        }

        if (filled == 0) {
            tooltip.add(Component.literal("Пусто").withStyle(ChatFormatting.GRAY));
            return;
        }

        // Цвет по заполнению: зелёный -> жёлтый -> красный
        float ratio = (float) filled / total;
        ChatFormatting color = ratio < 0.33 ? ChatFormatting.GREEN
                : (ratio < 0.66 ? ChatFormatting.YELLOW : ChatFormatting.RED);

        tooltip.add(Component.literal("Содержит: " + filled + "/" + total).withStyle(color));

        // Первые 5 предметов
        int shown = 0;
        for (java.util.Map.Entry<String, Integer> entry : items.entrySet()) {
            if (shown >= 5) {
                tooltip.add(Component.literal("... и ещё " + (items.size() - 5)).withStyle(ChatFormatting.GRAY));
                break;
            }
            tooltip.add(Component.literal("• " + entry.getKey() + " x" + entry.getValue()).withStyle(ChatFormatting.GRAY));
            shown++;
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteelStorageBlockEntity(pos, state);
    }
}