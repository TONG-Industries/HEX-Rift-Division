package com.trd.block.basic.industrial.fluids;

import com.trd.block.entity.ModBlockEntities;
import com.trd.block.entity.industrial.fluids.LowPressureSteamCondenserBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Простой полнокубический блок-конденсатор. Порты со всех сторон.
 */
public class LowPressureSteamCondenserBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public LowPressureSteamCondenserBlock(Properties properties) {
        super(properties);
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

    // ===== Сохраняем содержимое в предмет при ломке =====
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

    // ===== Тултип предмета (в инвентаре/руке). Живой тултип на блоке — в Overlay. =====
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        int steam = 0, water = 0;
        CompoundTag nbt = stack.getTag();
        if (nbt != null && nbt.contains("BlockEntityTag")) {
            CompoundTag be = nbt.getCompound("BlockEntityTag");
            steam = be.getCompound("SteamTank").getInt("Amount");
            water = be.getCompound("WaterTank").getInt("Amount");
        }
        int cap = LowPressureSteamCondenserBlockEntity.TANK_CAPACITY;
        tooltip.add(Component.literal("⬇ Пар (вход): ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(fmt(steam) + "/" + fmt(cap) + " mB").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("⬆ Вода (выход): ").withStyle(ChatFormatting.RED)
                .append(Component.literal(fmt(water) + "/" + fmt(cap) + " mB").withStyle(ChatFormatting.WHITE)));
    }

    /** 1032 -> "1032", 10000 -> "10k". */
    private static String fmt(int v) {
        if (v != 0 && v % 1000 == 0) return (v / 1000) + "k";
        return String.valueOf(v);
    }
}
