package com.trd.block.basic.industrial.fluids;

import com.trd.block.entity.ModBlockEntities;
import com.trd.block.entity.industrial.fluids.LowPressureSteamCondenserBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Полнокубический блок-конденсатор.
 * Порты жидкости и стыковка — только 2 боковые стороны (относительно FACING).
 * Поддерживает waterlogging.
 */
public class LowPressureSteamCondenserBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public LowPressureSteamCondenserBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LowPressureSteamCondenserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.LOW_PRESSURE_STEAM_CONDENSER_BE.get(),
                (lvl, pos, st, be) -> LowPressureSteamCondenserBlockEntity.serverTick(lvl, pos, st, be));
    }

    // ===== Waterlogging =====
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WATERLOGGED)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LowPressureSteamCondenserBlockEntity condenserBE && condenserBE.getSteamTank().getFluidAmount() > 0) {
            if (random.nextInt(3) == 0) {
                double d0 = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.5D;
                double d1 = pos.getY() + 0.6D + (random.nextDouble() * 0.4D);
                double d2 = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.5D;
                level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, d0, d1, d2, 0.0D, 0.05D, 0.0D);
            }
        }
    }

    // ===== Дропы с сохранением содержимого =====
    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof LowPressureSteamCondenserBlockEntity cond) {
            ItemStack stack = new ItemStack(this);
            CompoundTag nbt = new CompoundTag();
            cond.saveAdditional(nbt);
            stack.addTagElement("BlockEntityTag", nbt);
            return Collections.singletonList(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LowPressureSteamCondenserBlockEntity cond) {
            CompoundTag itemNbt = stack.getTag();
            if (itemNbt != null && itemNbt.contains("BlockEntityTag")) {
                cond.load(itemNbt.getCompound("BlockEntityTag"));
                cond.setChanged();
            }
        }
    }

    // ===== Тултип =====
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        int steam = 0, water = 0;
        float multiplier = 1.0f;
        CompoundTag nbt = stack.getTag();
        if (nbt != null && nbt.contains("BlockEntityTag")) {
            CompoundTag be = nbt.getCompound("BlockEntityTag");
            steam = be.getCompound("SteamTank").getInt("Amount");
            water = be.getCompound("WaterTank").getInt("Amount");
            if (be.contains("CoolingMultiplier")) {
                multiplier = be.getFloat("CoolingMultiplier");
            }
        }
        int cap = LowPressureSteamCondenserBlockEntity.TANK_CAPACITY;
        tooltip.add(Component.literal("⬇ Пар (вход): ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(fmt(steam) + "/" + fmt(cap) + " mB").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("⬆ Вода (выход): ").withStyle(ChatFormatting.RED)
                .append(Component.literal(fmt(water) + "/" + fmt(cap) + " mB").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("❄ Охлаждение: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("%.1fx", multiplier)).withStyle(ChatFormatting.WHITE)));
    }

    private static String fmt(int v) {
        if (v != 0 && v % 1000 == 0) return (v / 1000) + "k";
        return String.valueOf(v);
    }
}