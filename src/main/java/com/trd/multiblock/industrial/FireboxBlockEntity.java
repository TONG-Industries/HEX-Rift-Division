package com.trd.multiblock.industrial;

import com.trd.api.energy.IEnergyReceiver;
import com.trd.api.fluids.ModFluids;
import com.trd.block.entity.ModBlockEntities;
import com.trd.capability.ModCapabilities;
import com.trd.menu.industrial.FireboxMenu;
import com.trd.multiblock.system.PartRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Топка угольной электростанции (мультиблок 3x3x5, прямоугольная башня).
 *
 * Логика:
 *  - Электронагреватель (кнопка в GUI) жрёт энергию и МЕДЛЕННО поднимает температуру.
 *  - При t >= 150C уголь воспламеняется и горит сам (самоподдержание), нагреватель можно выключить.
 *  - Горящий уголь даёт тепло и выделяет дым (в бак дыма -> порт наверху -> труба -> башня exhaust).
 *  - Вода превращается в пар (порт пара). Есть настраиваемая подача угля и воды.
 *  - Меряется давление и есть максимальное давление; при превышении давления/температуры -> взрыв.
 *
 * Только уголь и древесный уголь как топливо.
 */
public class FireboxBlockEntity extends BlockEntity implements MenuProvider, IEnergyReceiver {

    // ===== Константы =====
    public static final float AMBIENT_TEMP = 20.0f;
    public static final float IGNITION_TEMP = 150.0f;   // уголь воспламеняется
    public static final float BOILING_POINT = 100.0f;
    public static final float MAX_TEMP = 1500.0f;       // взрыв по температуре
    public static final float HEAT_COST_PER_MB = 0.5f;  // тепло на 1 mB пара

    public static final int MAX_PRESSURE = 1000;        // условные единицы давления (взрыв)

    public static final int WATER_CAPACITY = 16_000;
    public static final int STEAM_CAPACITY = 64_000;
    public static final int SMOKE_CAPACITY = 64_000;
    public static final int COAL_SLOTS = 3;

    // Энергия (electric heater)
    public static final long MAX_ENERGY = 400_000L;
    public static final long MAX_RECEIVE = 2_000L;
    public static final long HEATER_ENERGY_PER_TICK = 200L; // расход энергии на тик работы нагревателя
    public static final float HEATER_HEAT_PER_TICK = 2.0f;  // медленный электронагрев

    // Горение угля
    public static final int COAL_BURN_TICKS = 1600;   // тиков горения на 1 уголь при feed=1
    public static final float COAL_HEAT_BASE = 3.0f;  // тепла в тик на единицу подачи
    public static final int SMOKE_BASE = 4;           // mB дыма в тик на единицу подачи

    // Пределы регулировок
    public static final int COAL_FEED_MIN = 1;
    public static final int COAL_FEED_MAX = 5;
    public static final int WATER_FEED_MIN = 1;
    public static final int WATER_FEED_MAX = 64;

    // ===== Состояние =====
    private float temperature = AMBIENT_TEMP;
    private boolean heaterOn = false;
    private boolean coalBurning = false;
    private int coalBurnTicks = 0;
    private int pressure = 0;
    private long energyStored = 0L;
    private int coalFeedRate = 1;
    private int waterFeedRate = 16;

    // ===== Баки =====
    private final FluidTank waterTank = new FluidTank(WATER_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) { return stack.getFluid().is(FluidTags.WATER); }
        @Override
        protected void onContentsChanged() { markUpdated(); }
    };
    private final FluidTank steamTank = new FluidTank(STEAM_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) { return stack.getFluid() == ModFluids.STEAM_SOURCE.get(); }
        @Override
        protected void onContentsChanged() { markUpdated(); }
    };
    private final FluidTank smokeTank = new FluidTank(SMOKE_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) { return stack.getFluid() == ModFluids.SMOKE_SOURCE.get(); }
        @Override
        protected void onContentsChanged() { markUpdated(); }
    };

    // Угольный инвентарь (только уголь / древесный уголь)
    private final ItemStackHandler coalInventory = new ItemStackHandler(COAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) { markUpdated(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isCoal(stack); }
    };

    // ===== Обёртки capability =====
    // Вода: только заливать
    private final LazyOptional<IFluidHandler> waterHandler = LazyOptional.of(() -> new IFluidHandler() {
        @Override public int getTanks() { return waterTank.getTanks(); }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return waterTank.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return waterTank.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return waterTank.isFluidValid(tank, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return waterTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    });
    // Пар: только сливать
    private final LazyOptional<IFluidHandler> steamHandler = LazyOptional.of(() -> new IFluidHandler() {
        @Override public int getTanks() { return steamTank.getTanks(); }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return steamTank.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return steamTank.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return steamTank.isFluidValid(tank, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return steamTank.drain(resource, action); }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return steamTank.drain(maxDrain, action); }
    });
    // Дым: только сливать
    private final LazyOptional<IFluidHandler> smokeHandler = LazyOptional.of(() -> new IFluidHandler() {
        @Override public int getTanks() { return smokeTank.getTanks(); }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return smokeTank.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return smokeTank.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return smokeTank.isFluidValid(tank, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return smokeTank.drain(resource, action); }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return smokeTank.drain(maxDrain, action); }
    });
    // Уголь: только вставлять
    private final LazyOptional<IItemHandler> coalInsertHandler = LazyOptional.of(() -> new IItemHandler() {
        @Override public int getSlots() { return coalInventory.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return coalInventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return coalInventory.insertItem(slot, stack, simulate); }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return coalInventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isCoal(stack); }
    });
    // Энергия (приёмник)
    private final LazyOptional<IEnergyReceiver> energyCap = LazyOptional.of(() -> this);

    // ===== GUI данные =====
    public static final int DATA_TEMP = 0;        // temperature * 10
    public static final int DATA_PRESSURE = 1;
    public static final int DATA_MAX_PRESSURE = 2;
    public static final int DATA_HEATER_ON = 3;
    public static final int DATA_COAL_BURNING = 4;
    public static final int DATA_ENERGY = 5;
    public static final int DATA_ENERGY_MAX = 6;
    public static final int DATA_WATER = 7;
    public static final int DATA_STEAM = 8;
    public static final int DATA_SMOKE = 9;
    public static final int DATA_COAL_FEED = 10;
    public static final int DATA_WATER_FEED = 11;
    private final SimpleContainerData data = new SimpleContainerData(12);

    public FireboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIREBOX_BE.get(), pos, state);
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public static boolean isCoal(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    // ===== Тик сервера =====
    public static void serverTick(Level level, BlockPos pos, BlockState state, FireboxBlockEntity be) {
        boolean changed = false;

        // 1. Охлаждение
        if (be.temperature > AMBIENT_TEMP) {
            float cooling = Math.max(0.05f, (be.temperature * be.temperature) / 600000f);
            be.temperature = Math.max(AMBIENT_TEMP, be.temperature - cooling);
            changed = true;
        }

        // 2. Электронагреватель: жрёт энергию, медленно греет (пока включён)
        if (be.heaterOn && be.energyStored >= HEATER_ENERGY_PER_TICK && be.temperature < MAX_TEMP) {
            be.energyStored -= HEATER_ENERGY_PER_TICK;
            be.temperature = Math.min(MAX_TEMP, be.temperature + HEATER_HEAT_PER_TICK);
            changed = true;
        }

        // 3. Воспламенение угля при достижении температуры
        if (!be.coalBurning && be.temperature >= IGNITION_TEMP && be.hasCoal()) {
            be.consumeCoal();
            be.coalBurning = true;
            be.coalBurnTicks = COAL_BURN_TICKS;
            changed = true;
        }

        // 4. Горение угля (самоподдержание) + выделение дыма
        if (be.coalBurning) {
            int feed = be.coalFeedRate;
            be.temperature = Math.min(MAX_TEMP, be.temperature + COAL_HEAT_BASE * feed);
            int smokeAmt = SMOKE_BASE * feed;
            be.smokeTank.fill(new FluidStack(ModFluids.SMOKE_SOURCE.get(), smokeAmt), IFluidHandler.FluidAction.EXECUTE);
            be.coalBurnTicks -= feed;
            if (be.coalBurnTicks <= 0) {
                if (be.hasCoal()) {
                    be.consumeCoal();
                    be.coalBurnTicks = COAL_BURN_TICKS;
                } else {
                    be.coalBurning = false;
                }
            }
            changed = true;
        }

        // 5. Кипение воды -> пар (ограничено подачей воды и теплом)
        if (be.temperature > BOILING_POINT && !be.waterTank.isEmpty()) {
            float availableHeat = be.temperature - BOILING_POINT;
            int maxByHeat = (int) (availableHeat / HEAT_COST_PER_MB);
            int steamSpace = be.steamTank.getCapacity() - be.steamTank.getFluidAmount();
            int boil = Math.min(be.waterFeedRate, Math.min(maxByHeat, Math.min(be.waterTank.getFluidAmount(), steamSpace)));
            if (boil > 0) {
                be.waterTank.drain(boil, IFluidHandler.FluidAction.EXECUTE);
                be.steamTank.fill(new FluidStack(ModFluids.STEAM_SOURCE.get(), boil), IFluidHandler.FluidAction.EXECUTE);
                be.temperature -= boil * HEAT_COST_PER_MB;
                changed = true;
            }
        }

        // 6. Давление = f(заполнение бака пара, температура)
        float steamFrac = be.steamTank.getFluidAmount() / (float) be.steamTank.getCapacity();
        float tempFrac = Math.min(1.0f, be.temperature / MAX_TEMP);
        be.pressure = (int) ((steamFrac * 0.7f + tempFrac * 0.3f) * MAX_PRESSURE);

        // 7. Взрыв при превышении давления или температуры
        if (be.pressure >= MAX_PRESSURE || be.temperature >= MAX_TEMP) {
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6.0f, Level.ExplosionInteraction.BLOCK);
            level.removeBlock(pos, false);
            return;
        }

        be.syncData();
        if (changed) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private boolean hasCoal() {
        for (int i = 0; i < coalInventory.getSlots(); i++) {
            if (!coalInventory.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    private void consumeCoal() {
        for (int i = 0; i < coalInventory.getSlots(); i++) {
            ItemStack s = coalInventory.getStackInSlot(i);
            if (!s.isEmpty()) { coalInventory.extractItem(i, 1, false); return; }
        }
    }

    private void syncData() {
        data.set(DATA_TEMP, (int) (temperature * 10.0f));
        data.set(DATA_PRESSURE, pressure);
        data.set(DATA_MAX_PRESSURE, MAX_PRESSURE);
        data.set(DATA_HEATER_ON, heaterOn ? 1 : 0);
        data.set(DATA_COAL_BURNING, coalBurning ? 1 : 0);
        data.set(DATA_ENERGY, (int) energyStored);
        data.set(DATA_ENERGY_MAX, (int) MAX_ENERGY);
        data.set(DATA_WATER, waterTank.getFluidAmount());
        data.set(DATA_STEAM, steamTank.getFluidAmount());
        data.set(DATA_SMOKE, smokeTank.getFluidAmount());
        data.set(DATA_COAL_FEED, coalFeedRate);
        data.set(DATA_WATER_FEED, waterFeedRate);
    }

    // ===== Управление из GUI/меню =====
    public void toggleHeater() { heaterOn = !heaterOn; markUpdated(); }
    public void setHeaterOn(boolean on) { heaterOn = on; markUpdated(); }
    public void setCoalFeedRate(int rate) { coalFeedRate = Math.max(COAL_FEED_MIN, Math.min(COAL_FEED_MAX, rate)); markUpdated(); }
    public void setWaterFeedRate(int rate) { waterFeedRate = Math.max(WATER_FEED_MIN, Math.min(WATER_FEED_MAX, rate)); markUpdated(); }

    // ===== Геттеры =====
    public float getTemperature() { return temperature; }
    public int getPressure() { return pressure; }
    public int getMaxPressure() { return MAX_PRESSURE; }
    public boolean isHeaterOn() { return heaterOn; }
    public boolean isCoalBurning() { return coalBurning; }
    public int getCoalFeedRate() { return coalFeedRate; }
    public int getWaterFeedRate() { return waterFeedRate; }
    public FluidTank getWaterTank() { return waterTank; }
    public FluidTank getSteamTank() { return steamTank; }
    public FluidTank getSmokeTank() { return smokeTank; }
    public ItemStackHandler getCoalInventory() { return coalInventory; }
    public ContainerData getData() { return data; }

    // ===== Capability для частей мультиблока =====
    public @NotNull <T> LazyOptional<T> getCapabilityForPart(@NotNull Capability<T> cap, @Nullable Direction side, PartRole role) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (role == PartRole.FLUID_INPUT) return waterHandler.cast();
            if (role == PartRole.FLUID_OUTPUT) return steamHandler.cast();
            if (role == PartRole.FLUID_CONNECTOR) return smokeHandler.cast(); // дым
        } else if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (role == PartRole.ITEM_INPUT) return coalInsertHandler.cast();
        }
        return LazyOptional.empty();
    }

    // Энергия делегируется контроллеру через часть ENERGY_CONNECTOR
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.ENERGY_RECEIVER || cap == ModCapabilities.ENERGY_CONNECTOR) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        waterHandler.invalidate();
        steamHandler.invalidate();
        smokeHandler.invalidate();
        coalInsertHandler.invalidate();
        energyCap.invalidate();
    }

    // ===== IEnergyReceiver =====
    @Override public boolean canConnectEnergy(Direction side) { return true; }
    @Override public long getEnergyStored() { return energyStored; }
    @Override public long getMaxEnergyStored() { return MAX_ENERGY; }
    @Override public void setEnergyStored(long energy) { energyStored = Math.max(0L, Math.min(MAX_ENERGY, energy)); setChanged(); }
    @Override public long getReceiveSpeed() { return MAX_RECEIVE; }
    @Override public Priority getPriority() { return Priority.NORMAL; }
    @Override public boolean canReceive() { return true; }
    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        long room = MAX_ENERGY - energyStored;
        long accepted = Math.min(room, Math.min(maxReceive, MAX_RECEIVE));
        if (accepted > 0 && !simulate) { energyStored += accepted; setChanged(); }
        return accepted;
    }

    // ===== NBT =====
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("WaterTank", waterTank.writeToNBT(new CompoundTag()));
        tag.put("SteamTank", steamTank.writeToNBT(new CompoundTag()));
        tag.put("SmokeTank", smokeTank.writeToNBT(new CompoundTag()));
        tag.put("Coal", coalInventory.serializeNBT());
        tag.putFloat("Temperature", temperature);
        tag.putBoolean("HeaterOn", heaterOn);
        tag.putBoolean("CoalBurning", coalBurning);
        tag.putInt("CoalBurnTicks", coalBurnTicks);
        tag.putInt("Pressure", pressure);
        tag.putLong("Energy", energyStored);
        tag.putInt("CoalFeed", coalFeedRate);
        tag.putInt("WaterFeed", waterFeedRate);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        waterTank.readFromNBT(tag.getCompound("WaterTank"));
        steamTank.readFromNBT(tag.getCompound("SteamTank"));
        smokeTank.readFromNBT(tag.getCompound("SmokeTank"));
        coalInventory.deserializeNBT(tag.getCompound("Coal"));
        temperature = tag.getFloat("Temperature");
        heaterOn = tag.getBoolean("HeaterOn");
        coalBurning = tag.getBoolean("CoalBurning");
        coalBurnTicks = tag.getInt("CoalBurnTicks");
        pressure = tag.getInt("Pressure");
        energyStored = tag.getLong("Energy");
        coalFeedRate = tag.contains("CoalFeed") ? tag.getInt("CoalFeed") : 1;
        waterFeedRate = tag.contains("WaterFeed") ? tag.getInt("WaterFeed") : 16;
        syncData();
    }

    // ===== Синхронизация клиента =====
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) { load(tag); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) handleUpdateTag(tag);
    }

    // ===== Меню (GUI сделаешь позже: FireboxMenu + FireboxScreen) =====
    @Override
    public Component getDisplayName() { return Component.translatable("block.trd.firebox"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FireboxMenu(id, inv, this, this.data);
    }
}
