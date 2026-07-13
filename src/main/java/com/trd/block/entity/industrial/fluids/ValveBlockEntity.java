package com.trd.block.entity.industrial.fluids;

import com.trd.api.fluids.system.FluidNetworkManager;
import com.trd.block.basic.industrial.fluids.ValveBlock;
import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

/**
 * BlockEntity клапана.
 *
 * Наследуется от {@link FluidPipeBlockEntity}, потому что жидкостная сеть
 * (FluidNetworkManager.canConnectLogically) соединяет узлы только если ОБА
 * являются FluidPipeBlockEntity с одинаковым фильтром. Клапан ведёт себя как
 * труба, но становится узлом сети ТОЛЬКО когда открыт (POWERED == true).
 */
public class ValveBlockEntity extends FluidPipeBlockEntity {

    // Флаг для отслеживания фронта сигнала редстоуна (как у рубильника)
    public boolean isTriggered = false;

    public ValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VALVE_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        // НЕ вызываем super.onLoad(): труба добавляет узел безусловно.
        // Клапан регистрирует узел только если он ОТКРЫТ.
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = getBlockState();
            if (state.getBlock() instanceof ValveBlock && state.getValue(ValveBlock.POWERED)) {
                FluidNetworkManager manager = FluidNetworkManager.get((ServerLevel) this.level);
                if (!manager.hasNode(getBlockPos())) manager.addNode(getBlockPos());
            }
        }
    }

    /** Тик: если клапан открыт, но узел по какой-то причине отсутствует — восстанавливаем его. */
    public static void tick(Level level, BlockPos pos, BlockState state, ValveBlockEntity be) {
        if (level.isClientSide) return;
        if (state.getBlock() instanceof ValveBlock && state.getValue(ValveBlock.POWERED)) {
            FluidNetworkManager manager = FluidNetworkManager.get((ServerLevel) level);
            if (!manager.hasNode(pos)) manager.addNode(pos);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); // сохраняет фильтр (FilterFluid) и HasFlowed
        tag.putBoolean("isTriggered", isTriggered);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isTriggered = tag.getBoolean("isTriggered");
    }

    // ==========================================
    // ПУСТОЙ FLUID_HANDLER (только для визуального коннекта труб)
    // ==========================================
    // FluidPipeBlock.canConnectTo() рисует отвод к соседу, если у него есть FLUID_HANDLER.
    // Клапан — не FluidPipeBlock, поэтому без capability труба его не видит.
    // Хэндлер ПУСТОЙ: ничего не хранит и не принимает, поэтому сеть НЕ считает
    // клапан баком/машиной. Сам поток идёт через клапан как через узел сети (FluidPipeBlockEntity).
    private final LazyOptional<IFluidHandler> dummyHandler = LazyOptional.of(() -> new IFluidHandler() {
        @Override public int getTanks() { return 0; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return 0; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return false; }
        @Override public int fill(FluidStack resource, IFluidHandler.FluidAction action) { return 0; }
        @Override public @NotNull FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) { return FluidStack.EMPTY; }
    });

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return dummyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        dummyHandler.invalidate();
    }
}
