package com.trd.block.basic.industrial.fluids;

import com.trd.api.fluids.system.FluidNetwork;
import com.trd.api.fluids.system.FluidNetworkManager;
import com.trd.block.entity.ModBlockEntities;
import com.trd.block.entity.industrial.fluids.ValveBlockEntity;
import com.trd.item.tools.FluidIdentifierItem;
import com.trd.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Клапан (Valve) — аналог рубильника (SwitchBlock), но для ЖИДКОСТНОЙ сети.
 *
 * POWERED == true  -> клапан ОТКРЫТ  (узел добавлен в сеть, жидкость проходит)
 * POWERED == false -> клапан ЗАКРЫТ  (узел удалён, поток разрывается)
 *
 * Управление ПКМ:
 *  - С идентификатором жидкости в руке -> задаёт фильтр клапана (как у трубы),
 *    чтобы клапан совпадал с фильтром линии труб и не блокировал сеть.
 *  - С пустой рукой (или любым другим предметом) -> переключает открыт/закрыт.
 * Также переключается фронтом сигнала редстоуна (как рубильник).
 */
public class ValveBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ValveBlock(Properties properties) {
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

        ServerLevel serverLevel = (ServerLevel) level;
        FluidNetworkManager manager = FluidNetworkManager.get(serverLevel);
        BlockEntity be = level.getBlockEntity(pos);
        ItemStack held = player.getItemInHand(hand);

        // 1. Идентификатор жидкости -> задаём фильтр клапана (как у трубы)
        if (held.getItem() instanceof FluidIdentifierItem && be instanceof ValveBlockEntity valve) {
            String selectedFluidId = FluidIdentifierItem.getSelectedFluid(held);
            Fluid fluidToSet = Fluids.EMPTY;
            if (!selectedFluidId.equals("none")) {
                fluidToSet = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(selectedFluidId));
                if (fluidToSet == null) fluidToSet = Fluids.EMPTY;
            }

            FluidNetwork net = manager.getNetwork(pos);
            if (net != null) net.invalidateFluidCache();
            manager.removeNode(pos);
            valve.setFilterFluid(fluidToSet);
            // Пере-добавляем узел только если клапан открыт
            if (state.getValue(POWERED)) manager.addNode(pos);
            // Обновляем формы соседей, чтобы трубы пересчитали коннекты под новый фильтр
            level.getBlockState(pos).updateNeighbourShapes(level, pos, 3);

            if (selectedFluidId.equals("none")) {
                player.displayClientMessage(Component.literal("\u00A7eФильтр клапана сброшен"), true);
                level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 0.8F);
            } else {
                String fluidName = Component.translatable(fluidToSet.getFluidType().getDescriptionId()).getString();
                player.displayClientMessage(Component.literal("\u00A7aФильтр клапана: \u00A7f" + fluidName), true);
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }

        // 2. Иначе — переключаем открыт/закрыт
        toggle(state, serverLevel, pos, manager);
        return InteractionResult.SUCCESS;
    }

    /** Переключение открыт/закрыт (ПКМ). */
    private void toggle(BlockState state, ServerLevel level, BlockPos pos, FluidNetworkManager manager) {
        setOpen(state, level, pos, !state.getValue(POWERED), manager);
    }

    /**
     * Явно задаёт состояние клапана (открыт/закрыт) и синхронизирует сеть.
     * Используется и в ПКМ, и при сигнале редстоуна.
     */
    private void setOpen(BlockState state, ServerLevel level, BlockPos pos, boolean open, FluidNetworkManager manager) {
        if (state.getValue(POWERED) == open) return; // уже в нужном состоянии

        BlockState newState = state.setValue(POWERED, open);
        level.setBlock(pos, newState, 3); // flag 3 -> соседние трубы пересчитают коннекты (updateShape)

        FluidNetwork net = manager.getNetwork(pos);
        if (net != null) net.invalidateFluidCache();
        manager.removeNode(pos);

        if (open) {
            level.playSound(null, pos, ModSounds.LEVER1.get(), SoundSource.BLOCKS, 0.3f, 1.0f);
            manager.addNode(pos); // открыт -> клапан снова узел сети
        } else {
            level.playSound(null, pos, ModSounds.LEVER1.get(), SoundSource.BLOCKS, 0.3f, 0.9f);
            // закрыт -> узел НЕ добавляем, поток разорван
        }
        level.updateNeighborsAt(pos, this);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        if (level.isClientSide) return;

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof ValveBlockEntity valveEntity)) return;

        boolean hasRedstoneSignal = level.hasNeighborSignal(pos);

        // Реагируем только на ИЗМЕНЕНИЕ сигнала (фронт/спад), чтобы обычные апдейты
        // соседей не сбивали ручное переключение ПКМ.
        if (hasRedstoneSignal != valveEntity.isTriggered) {
            valveEntity.isTriggered = hasRedstoneSignal;
            valveEntity.setChanged();
            // Прямое соответствие: сигнал ЕСТЬ -> открыт, сигнала НЕТ -> закрыт.
            // Поэтому выключение рычага (спад) сразу закрывает клапан — без второго нажатия.
            setOpen(state, (ServerLevel) level, pos, hasRedstoneSignal,
                    FluidNetworkManager.get((ServerLevel) level));
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            if (state.getValue(POWERED)) {
                FluidNetworkManager.get((ServerLevel) level).addNode(pos);
            }
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            FluidNetworkManager.get((ServerLevel) level).removeNode(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ValveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.VALVE_BE.get(), ValveBlockEntity::tick);
    }
}
