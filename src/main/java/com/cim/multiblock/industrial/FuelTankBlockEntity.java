package com.cim.multiblock.industrial;

import com.cim.api.fluids.system.FluidNetworkManager;
import com.cim.api.fluids.system.ITankWithMode;
import com.cim.block.entity.ModBlockEntities;
import com.cim.item.ModItems;
import com.cim.item.tools.InfiniteFluidBarrelItem;
import com.cim.menu.FuelTankMenu;
import com.cim.multiblock.system.IFluidTankProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FuelTankBlockEntity extends BlockEntity implements MenuProvider, IFluidTankProvider, ITankWithMode {

    public static final int CAPACITY = 768_000;
    public static final int MAX_TRANSFER_RATE = 400;

    // Слоты: 0 - fill input, 1 - fill output, 2 - drain input, 3 - drain output, 4 - protector (визуальный)
    public final ItemStackHandler itemHandler = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0 || slot == 2) {
                if (stack.getItem() instanceof InfiniteFluidBarrelItem) return true;
                return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            }
            if (slot == 4) {
                Item item = stack.getItem();
                return item == ModItems.PROTECTOR_STEEL.get()
                        || item == ModItems.PROTECTOR_LEAD.get()
                        || item == ModItems.PROTECTOR_TUNGSTEN.get();
            }
            return false;
        }
    };
    @Override
    public LazyOptional<IFluidHandler> getFluidHandlerCapability() {
        return lazyFluidHandler;
    }
    // Геттер для совместимости со старым кодом (дроп при разрушении блока)
    public ItemStackHandler getInventory() {
        return itemHandler;
    }

    private final FluidTank fluidTank = createTank();

    private int mode = 0; // 0 - both, 1 - input only, 2 - output only, 3 - disabled
    private String fluidFilter = "none";

    private final IFluidHandler networkFluidHandler = createNetworkHandler();

    private final LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.of(() -> networkFluidHandler);
    private final LazyOptional<ItemStackHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);

    private final ContainerData data = new SimpleContainerData(1) {
        @Override public void set(int index, int value) { mode = value; }
        @Override public int get(int index) { return mode; }
    };

    public FuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_TANK_BE.get(), pos, state);
    }

    private FluidTank createTank() {
        return new FluidTank(CAPACITY) {
            @Override
            protected void onContentsChanged() {
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                }
            }

            @Override
            public boolean isFluidValid(FluidStack stack) {
                if (fluidFilter.equals("none")) return false;
                ResourceLocation loc = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
                return loc != null && loc.toString().equals(fluidFilter);
            }
        };
    }

    private IFluidHandler createNetworkHandler() {
        return new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public @NotNull FluidStack getFluidInTank(int tank) { return fluidTank.getFluid(); }
            @Override public int getTankCapacity(int tank) { return fluidTank.getCapacity(); }
            @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return fluidTank.isFluidValid(stack); }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (mode == 2 || mode == 3) return 0;
                FluidStack toFill = resource.copy();
                toFill.setAmount(Math.min(toFill.getAmount(), MAX_TRANSFER_RATE));
                return fluidTank.fill(toFill, action);
            }

            @Override
            public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                if (mode == 1 || mode == 3) return FluidStack.EMPTY;
                int maxDrain = Math.min(resource.getAmount(), MAX_TRANSFER_RATE);
                FluidStack toDrain = resource.copy();
                toDrain.setAmount(maxDrain);
                return fluidTank.drain(toDrain, action);
            }

            @Override
            public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                if (mode == 1 || mode == 3) return FluidStack.EMPTY;
                return fluidTank.drain(Math.min(maxDrain, MAX_TRANSFER_RATE), action);
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FuelTankBlockEntity be) {
        if (level.isClientSide) return;
        be.processBuckets();
    }

    private void processBuckets() {
        // 1. Бесконечный источник в слоте 2
        ItemStack infiniteSource = itemHandler.getStackInSlot(2);
        if (!infiniteSource.isEmpty() && infiniteSource.getItem() instanceof InfiniteFluidBarrelItem) {
            if (!fluidFilter.equals("none")) {
                Fluid filterFluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidFilter));
                if (filterFluid != null && filterFluid != Fluids.EMPTY) {
                    int space = fluidTank.getSpace();
                    if (space > 0) {
                        fluidTank.fill(new FluidStack(filterFluid, space), IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }

        // 2. Обычные вёдра: выливание (drain input -> бак)
        ItemStack drainIn = itemHandler.getStackInSlot(2);
        if (!drainIn.isEmpty() && !(drainIn.getItem() instanceof InfiniteFluidBarrelItem)) {
            FluidActionResult result = FluidUtil.tryEmptyContainer(drainIn, fluidTank, fluidTank.getSpace(), null, true);
            if (result.isSuccess()) {
                if (insertOrMerge(3, result.getResult())) {
                    drainIn.shrink(1);
                }
            }
        }

        // 3. Наполнение вёдер из бака (бак -> fill output)
        if (!fluidTank.isEmpty()) {
            ItemStack fillIn = itemHandler.getStackInSlot(0);
            if (!fillIn.isEmpty()) {
                FluidActionResult result = FluidUtil.tryFillContainer(fillIn, fluidTank, fluidTank.getFluidAmount(), null, true);
                if (result.isSuccess()) {
                    if (insertOrMerge(1, result.getResult())) {
                        fillIn.shrink(1);
                    }
                }
            }
        }
    }

    private boolean insertOrMerge(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;
        ItemStack existing = itemHandler.getStackInSlot(slot);
        if (existing.isEmpty()) {
            itemHandler.setStackInSlot(slot, stack.copy());
            return true;
        } else if (ItemStack.isSameItemSameTags(existing, stack) &&
                existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) {
            existing.grow(stack.getCount());
            return true;
        }
        return false;
    }

    @Override
    public void changeMode() {
        mode = (mode + 1) % 4;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public int getMode() {
        return mode;
    }

    public void setFilter(String newFilter) {
        this.fluidFilter = newFilter;
        if (!newFilter.equals("none") && !fluidTank.isEmpty()) {
            ResourceLocation cur = ForgeRegistries.FLUIDS.getKey(fluidTank.getFluid().getFluid());
            if (cur != null && !cur.toString().equals(newFilter)) {
                fluidTank.setFluid(FluidStack.EMPTY);
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            FluidNetworkManager manager = FluidNetworkManager.get((ServerLevel) level);
            manager.removeNode(getBlockPos());
            manager.addNode(getBlockPos());
        }
    }

    public String getFilter() {
        return fluidFilter;
    }

    public FluidStack getFluid() { return fluidTank.getFluid(); }
    public int getCapacity() { return CAPACITY; }
    public ContainerData getData() { return data; }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            FluidNetworkManager.get((ServerLevel) level).addNode(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            FluidNetworkManager.get((ServerLevel) level).removeNode(worldPosition);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFluidHandler.invalidate();
        lazyItemHandler.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluidHandler.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.put("FluidTank", fluidTank.writeToNBT(new CompoundTag()));
        tag.putInt("Mode", mode);
        tag.putString("FluidFilter", fluidFilter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        fluidTank.readFromNBT(tag.getCompound("FluidTank"));
        mode = tag.getInt("Mode");
        if (tag.contains("FluidFilter")) {
            fluidFilter = tag.getString("FluidFilter");
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cim.fuel_tank_big");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FuelTankMenu(id, inv, this, this.data);
    }
}