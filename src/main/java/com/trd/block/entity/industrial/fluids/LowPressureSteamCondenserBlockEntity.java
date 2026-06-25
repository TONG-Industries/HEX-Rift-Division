package com.trd.block.entity.industrial.fluids;

import com.trd.api.fluids.ModFluids;
import com.trd.api.fluids.system.FluidNetworkManager;
import com.trd.block.basic.industrial.fluids.LowPressureSteamCondenserBlock;
import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Конденсатор пара низкого давления.
 * Работает ТОЛЬКО если залит водой (WATERLOGGED).
 * Вход/выход жидкости — только 2 боковые стороны (перпендикулярно FACING).
 * Эффективность 1.00× … 2.00× в зависимости от воды в радиусе 10 блоков.
 * Равномерно балансирует пар/воду с соседними конденсаторами по боковым портам.
 */
public class LowPressureSteamCondenserBlockEntity extends BlockEntity {

    public static final int TANK_CAPACITY = 10_000;
    public static final int CONVERT_RATE  = 10; // базовая скорость, mB/тик

    private static final int WATER_CHECK_INTERVAL = 60; // тиков между проверками
    private static final int WATER_RADIUS         = 5; // радиус сканирования
    private static final int WATER_MAX_COUNT      = 100; // при 100 блоках воды = 2.00×

    private final FluidTank steamTank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ModFluids.LOW_PRESSURE_STEAM_SOURCE.get();
        }
        @Override protected void onContentsChanged() { setChanged(); sync(); }
    };

    private final FluidTank waterTank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.WATER;
        }
        @Override protected void onContentsChanged() { setChanged(); sync(); }
    };

    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? steamTank.getFluid() : waterTank.getFluid();
        }
        @Override public int getTankCapacity(int tank) { return TANK_CAPACITY; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 ? steamTank.isFluidValid(stack) : waterTank.isFluidValid(stack);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!steamTank.isFluidValid(resource)) return 0;
            return steamTank.fill(resource, action);
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid() != Fluids.WATER) return FluidStack.EMPTY;
            return waterTank.drain(resource, action);
        }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return waterTank.drain(maxDrain, action);
        }
    });

    private float coolingMultiplier = 1.0f;
    private int waterCheckCooldown = 0;

    public LowPressureSteamCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOW_PRESSURE_STEAM_CONDENSER_BE.get(), pos, state);
    }

    // ===== Регистрация в флюид-сети =====
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && level instanceof ServerLevel sl) {
            FluidNetworkManager.get(sl).addNode(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && level instanceof ServerLevel sl) {
            FluidNetworkManager.get(sl).removeNode(worldPosition);
        }
    }

    // ===== Серверный тик =====
    public static void serverTick(Level level, BlockPos pos, BlockState state, LowPressureSteamCondenserBlockEntity be) {
        if (level.isClientSide) return;

        // БЕЗ ВОДЫ ВНУТРИ — НЕ РАБОТАЕТ
        if (!state.getValue(LowPressureSteamCondenserBlock.WATERLOGGED)) {
            return;
        }

        // Обновление множителя охлаждения
        if (--be.waterCheckCooldown <= 0) {
            be.waterCheckCooldown = WATER_CHECK_INTERVAL;
            float newMult = calculateCoolingMultiplier(level, pos);
            if (Math.abs(newMult - be.coolingMultiplier) > 0.001f) {
                be.coolingMultiplier = newMult;
                be.setChanged();
                be.sync();
            }
        }

        // Равномерное распределение с соседними конденсаторами по боковым портам
        be.balanceWithNeighbors(level, pos, state);

        int effectiveRate = Math.max(1, (int) (CONVERT_RATE * be.coolingMultiplier));
        int waterSpace = be.waterTank.getCapacity() - be.waterTank.getFluidAmount();

        if (be.steamTank.getFluidAmount() >= effectiveRate && waterSpace >= effectiveRate) {
            be.steamTank.drain(effectiveRate, IFluidHandler.FluidAction.EXECUTE);
            be.waterTank.fill(new FluidStack(Fluids.WATER, effectiveRate), IFluidHandler.FluidAction.EXECUTE);
            be.setChanged();

        }
    }

    /** Считает блоки воды в кубе 21×21×21 (без самого блока). База 1.0 + бонус до 1.0. */
    private static float calculateCoolingMultiplier(Level level, BlockPos pos) {
        int waterCount = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = -WATER_RADIUS; x <= WATER_RADIUS; x++) {
            for (int y = -WATER_RADIUS; y <= WATER_RADIUS; y++) {
                for (int z = -WATER_RADIUS; z <= WATER_RADIUS; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // сам конденсатор не считаем
                    mutable.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (level.getFluidState(mutable).getType() == Fluids.WATER) {
                        waterCount++;
                        if (waterCount >= WATER_MAX_COUNT) {
                            return 2.0f;
                        }
                    }
                }
            }
        }
        return 1.0f + (waterCount / (float) WATER_MAX_COUNT);
    }

    /** Балансирует пар и воду с соседними конденсаторами (только по боковым портам). */
    private void balanceWithNeighbors(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(LowPressureSteamCondenserBlock.FACING);
        Direction left  = facing.getClockWise();
        Direction right = facing.getCounterClockWise();

        for (Direction side : new Direction[]{left, right}) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(side));
            if (neighbor instanceof LowPressureSteamCondenserBlockEntity other) {
                // Защита от двойного переноса: балансирует только "меньший" по координате
                if (pos.asLong() < other.getBlockPos().asLong()) {
                    balanceTanks(this.steamTank, other.steamTank);
                    balanceTanks(this.waterTank, other.waterTank);
                }
            }
        }
    }

    /** Переливает жидкость между двумя танками до равного уровня. */
    private static void balanceTanks(FluidTank a, FluidTank b) {
        int total = a.getFluidAmount() + b.getFluidAmount();
        if (total == 0) return;

        // Если оба не пусты — проверяем совместимость
        if (!a.getFluid().isEmpty() && !b.getFluid().isEmpty() && !a.getFluid().isFluidEqual(b.getFluid())) {
            return;
        }

        int avg = total / 2;
        int aAmt = a.getFluidAmount();

        if (aAmt > avg + 1) {
            int diff = aAmt - avg;
            FluidStack drain = a.drain(diff, IFluidHandler.FluidAction.EXECUTE);
            if (!drain.isEmpty()) {
                int filled = b.fill(drain, IFluidHandler.FluidAction.EXECUTE);
                if (filled < drain.getAmount()) {
                    drain.setAmount(drain.getAmount() - filled);
                    a.fill(drain, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        } else if (aAmt < avg - 1) {
            int diff = avg - aAmt;
            FluidStack drain = b.drain(diff, IFluidHandler.FluidAction.EXECUTE);
            if (!drain.isEmpty()) {
                int filled = a.fill(drain, IFluidHandler.FluidAction.EXECUTE);
                if (filled < drain.getAmount()) {
                    drain.setAmount(drain.getAmount() - filled);
                    b.fill(drain, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    // ===== Capabilities — только 2 боковые стороны =====
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side == null) {
                return fluidCap.cast(); // внутренние запросы (в т.ч. трубы без стороны)
            }
            Direction facing = getBlockState().getValue(LowPressureSteamCondenserBlock.FACING);
            Direction left  = facing.getClockWise();
            Direction right = facing.getCounterClockWise();
            if (side == left || side == right) {
                return fluidCap.cast();
            }
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCap.invalidate();
    }

    // ===== NBT =====
    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("SteamTank", steamTank.writeToNBT(new CompoundTag()));
        tag.put("WaterTank", waterTank.writeToNBT(new CompoundTag()));
        tag.putFloat("CoolingMultiplier", coolingMultiplier);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        steamTank.readFromNBT(tag.getCompound("SteamTank"));
        waterTank.readFromNBT(tag.getCompound("WaterTank"));
        if (tag.contains("CoolingMultiplier")) {
            coolingMultiplier = tag.getFloat("CoolingMultiplier");
        }
    }

    // ===== Синхронизация =====
    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }

    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) handleUpdateTag(pkt.getTag());
    }

    // ===== Геттеры =====
    public FluidTank getSteamTank() { return steamTank; }
    public FluidTank getWaterTank() { return waterTank; }
    public float getCoolingMultiplier() { return coolingMultiplier; }
}