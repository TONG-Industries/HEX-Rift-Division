package com.cim.block.entity.industrial.fluids;

import com.cim.api.fluids.system.*;
import com.cim.block.basic.ModBlocks;
import com.cim.block.basic.industrial.fluids.FluidBarrelBlock;
import com.cim.block.entity.ModBlockEntities;
import com.cim.item.ModItems;
import com.cim.menu.FluidBarrelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidBarrelBlockEntity extends BlockEntity implements MenuProvider, ITankWithMode {

    public static final int MAX_TRANSFER_RATE = 200;
    public static final int TOTAL_SLOTS = 5;
    public static final int FILL_IN_SLOT = 0;
    public static final int FILL_OUT_SLOT = 1;
    public static final int DRAIN_IN_SLOT = 2;
    public static final int DRAIN_OUT_SLOT = 3;
    public static final int PROTECTOR_SLOT = 4;

    public int mode = 0;
    public String fluidFilter = "none";

    public FluidTank fluidTank;
    public IFluidHandler networkFluidHandler;

    public final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }

        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == FILL_IN_SLOT) return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            if (slot == DRAIN_IN_SLOT) {
                if (stack.getItem() instanceof com.cim.item.tools.InfiniteFluidBarrelItem) return true;
                return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            }
            if (slot == PROTECTOR_SLOT) {
                Item item = stack.getItem();
                return item == ModItems.PROTECTOR_STEEL.get()
                        || item == ModItems.PROTECTOR_LEAD.get()
                        || item == ModItems.PROTECTOR_TUNGSTEN.get();
            }
            return false;
        }

        @Override public void deserializeNBT(CompoundTag nbt) {
            super.deserializeNBT(nbt);
            if (this.getSlots() != TOTAL_SLOTS) this.setSize(TOTAL_SLOTS);
        }
    };

    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return mode; }
        @Override public void set(int index, int value) { mode = value; }
        @Override public int getCount() { return 1; }
    };

    public FluidBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_BARREL_BE.get(), pos, state);
        int cap = getTier().getCapacity();
        this.fluidTank = createTank(cap);
        this.networkFluidHandler = createNetworkHandler();
    }
    @Override
    public void changeMode() {
        this.mode = (this.mode + 1) % 4;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public int getMode() {
        return mode;
    }
    private FluidTank createTank(int capacity) {
        return new FluidTank(capacity) {
            @Override protected void onContentsChanged() {
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                }
            }
            @Override public boolean isFluidValid(FluidStack stack) {
                if (fluidFilter.equals("none")) return false;
                ResourceLocation loc = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
                if (loc != null && !loc.toString().equals(fluidFilter)) return false;
                return super.isFluidValid(stack);
            }
        };
    }

    private IFluidHandler createNetworkHandler() {
        return new IFluidHandler() {
            @Override public int getTanks() { return fluidTank.getTanks(); }
            @Override public @NotNull FluidStack getFluidInTank(int tank) { return fluidTank.getFluidInTank(tank); }
            @Override public int getTankCapacity(int tank) { return fluidTank.getTankCapacity(tank); }
            @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return fluidTank.isFluidValid(tank, stack); }

            @Override public int fill(FluidStack resource, FluidAction action) {
                if (mode == 2 || mode == 3) return 0;
                FluidStack toFill = resource.copy();
                toFill.setAmount(Math.min(toFill.getAmount(), MAX_TRANSFER_RATE));
                return fluidTank.fill(toFill, action);
            }
            @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                if (mode == 1 || mode == 3) return FluidStack.EMPTY;
                int maxDrain = Math.min(resource.getAmount(), MAX_TRANSFER_RATE);
                FluidStack toDrain = resource.copy(); toDrain.setAmount(maxDrain);
                return fluidTank.drain(toDrain, action);
            }
            @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                if (mode == 1 || mode == 3) return FluidStack.EMPTY;
                return fluidTank.drain(Math.min(maxDrain, MAX_TRANSFER_RATE), action);
            }
        };
    }

    public BarrelTier getTier() {
        Block b = getBlockState().getBlock();
        return (b instanceof FluidBarrelBlock barrel) ? barrel.getTier() : BarrelTier.IRON;
    }

    private int[] getProtectorBonus() {
        ItemStack stack = itemHandler.getStackInSlot(PROTECTOR_SLOT);
        if (stack.isEmpty()) return new int[]{0, 0};
        Item it = stack.getItem();
        if (it == ModItems.PROTECTOR_STEEL.get()) return new int[]{720, 40};
        if (it == ModItems.PROTECTOR_LEAD.get()) return new int[]{135, 225};
        if (it == ModItems.PROTECTOR_TUNGSTEN.get()) return new int[]{1700, 270};
        return new int[]{0, 0};
    }

    public int getTotalMeltingPoint() { return getTier().getMeltingPoint() + getProtectorBonus()[0]; }
    public int getTotalCorrosionResistance() { return getTier().getCorrosionResistance() + getProtectorBonus()[1]; }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidBarrelBlockEntity be) {
        if (level.isClientSide) {
            be.spawnLeakParticles();
            return;
        }
        be.processBuckets();
        be.processLeaking();
        be.checkDamage();
    }

    private void processLeaking() {
        if (!getTier().isLeaking() || fluidTank.isEmpty()) return;
        if (level.getGameTime() % 20 != 0) return;
        int rate = getTier().getLeakRate();
        if (rate > 0) fluidTank.drain(rate, IFluidHandler.FluidAction.EXECUTE);
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnLeakParticles() {
        if (!getTier().isLeaking() || fluidTank.isEmpty()) return;
        RandomSource rnd = level.random;
        int chance = (getTier() == BarrelTier.CORRUPTED) ? 3 : 15;
        if (rnd.nextInt(chance) != 0) return;

        FluidStack fluid = fluidTank.getFluid();
        int tint = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor();
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;

        double x = worldPosition.getX() + 0.1 + rnd.nextDouble() * 0.8;
        double y = worldPosition.getY() + 0.95;
        double z = worldPosition.getZ() + 0.1 + rnd.nextDouble() * 0.8;
        double vy = -0.25 - rnd.nextDouble() * 0.15;

        level.addParticle(new DustParticleOptions(new Vector3f(r, g, b), 1.0f), x, y, z, 0.0, vy, 0.0);
    }

    private void checkDamage() {
        if (fluidTank.isEmpty()) return;

        Block currentBlock = getBlockState().getBlock();
        if (currentBlock == ModBlocks.CORRUPTED_BARREL.get() || currentBlock == ModBlocks.LEAKING_BARREL.get())
            return;

        FluidStack fluid = fluidTank.getFluid();
        int temp = getFluidTemperatureCelsius(fluid);
        int corr = getFluidCorrosivity(fluid);

        int melt = getTotalMeltingPoint();
        int cRes = getTotalCorrosionResistance();

        int tempExcess = temp - melt;
        int corrExcess = corr - cRes;
        int maxExcess = Math.max(tempExcess, corrExcess);

        Block target = null;
        if (maxExcess > 100) {
            target = ModBlocks.CORRUPTED_BARREL.get();
        } else if (maxExcess > 0) {
            target = ModBlocks.LEAKING_BARREL.get();
        }

        if (target != null) {
            // === ЗАКРЫВАЕМ GUI ВСЕХ ИГРОКОВ, СМОТРЯЩИХ В ЭТУ БОЧКУ ===
            closeAllViewers();

            // === СОХРАНЯЕМ NBT ПЕРЕД ЗАМЕНОЙ ===
            CompoundTag tag = saveWithoutMetadata();

            // === ЗАМЕНЯЕМ БЛОК ===
            level.setBlock(worldPosition, target.defaultBlockState(), 3);

            // === ЗВУК: ВОДА КАСАЕТСЯ ЛАВЫ (как будто жидкость проедает бочку) ===
            level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.2F);

            // === ЗАГРУЖАЕМ ДАННЫЕ В НОВУЮ БОЧКУ ===
            BlockEntity newBe = level.getBlockEntity(worldPosition);
            if (newBe instanceof FluidBarrelBlockEntity newBarrel) {
                newBarrel.load(tag);
                int cap = newBarrel.getTier().getCapacity();
                if (newBarrel.fluidTank.getFluidAmount() > cap) {
                    newBarrel.fluidTank.getFluid().setAmount(cap);
                }
                newBarrel.setChanged();
            }
        }
    }

    /**
     * Принудительно закрывает GUI у всех игроков, которые смотрят в инвентарь этой бочки
     */
    private void closeAllViewers() {
        if (level == null || level.isClientSide) return;

        // Получаем список всех игроков на сервере
        for (Player player : level.players()) {
            if (player instanceof ServerPlayer serverPlayer) {
                // Если игрок открыл контейнер и этот контейнер принадлежит нашей бочке
                if (serverPlayer.containerMenu instanceof FluidBarrelMenu menu) {
                    // Проверяем, что это именно наша бочка по позиции
                    if (menu.getBlockEntity() == this) {
                        serverPlayer.closeContainer();
                    }
                }
            }
        }
    }

    private void processBuckets() {
        ItemStack drainIn = itemHandler.getStackInSlot(DRAIN_IN_SLOT);
        if (!drainIn.isEmpty()) {
            if (drainIn.getItem() instanceof com.cim.item.tools.InfiniteFluidBarrelItem) {
                if (!this.fluidFilter.equals("none")) {
                    Fluid filterFluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(this.fluidFilter));
                    if (filterFluid != null && filterFluid != Fluids.EMPTY) {
                        int space = fluidTank.getSpace();
                        if (space > 0) {
                            fluidTank.fill(new FluidStack(filterFluid, space), IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                }
            } else {
                var result = FluidUtil.tryEmptyContainer(drainIn, fluidTank, fluidTank.getSpace(), null, true);
                if (result.isSuccess()) {
                    if (insertOrMerge(DRAIN_OUT_SLOT, result.getResult())) drainIn.shrink(1);
                }
            }
        }

        if (fluidTank.getFluidAmount() > 0) {
            ItemStack fillIn = itemHandler.getStackInSlot(FILL_IN_SLOT);
            if (!fillIn.isEmpty()) {
                var result = FluidUtil.tryFillContainer(fillIn, fluidTank, fluidTank.getFluidAmount(), null, true);
                if (result.isSuccess()) {
                    if (insertOrMerge(FILL_OUT_SLOT, result.getResult())) fillIn.shrink(1);
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
        } else if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) {
            existing.grow(stack.getCount());
            return true;
        }
        return false;
    }

    public void setFilter(String newFilter) {
        this.fluidFilter = newFilter;
        if (!newFilter.equals("none") && !fluidTank.isEmpty()) {
            ResourceLocation cur = ForgeRegistries.FLUIDS.getKey(fluidTank.getFluid().getFluid());
            if (cur != null && !cur.toString().equals(newFilter)) fluidTank.setFluid(FluidStack.EMPTY);
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            FluidNetworkManager manager = FluidNetworkManager.get((ServerLevel) level);
            manager.removeNode(this.getBlockPos());
            manager.addNode(this.getBlockPos());
        }
    }


    @Override public void onLoad() {
        super.onLoad();
        lazyFluidHandler = LazyOptional.of(() -> networkFluidHandler);
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        if (level != null && !level.isClientSide) FluidNetworkManager.get((ServerLevel) level).addNode(worldPosition);
    }

    @Override public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) FluidNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        lazyFluidHandler.invalidate();
        lazyItemHandler.invalidate();
    }

    private int getFluidTemperatureCelsius(FluidStack stack) {
        int nbtTemp = FluidPropertyHelper.getTemperature(stack);
        Fluid fluid = stack.getFluid();
        int defaultTemp = fluid.getFluidType().getTemperature();
        if (nbtTemp != defaultTemp) return nbtTemp;
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) return 20;
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) return 1000;
        if (fluid.getFluidType() instanceof BaseFluidType base) return base.getDisplayTemperature();
        return defaultTemp - 273;
    }

    private int getFluidCorrosivity(FluidStack stack) {
        int nbt = FluidPropertyHelper.getCorrosivity(stack);
        if (nbt > 0) return nbt;
        if (stack.getFluid().getFluidType() instanceof BaseFluidType base) return base.getCorrosivity();
        return 0;
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        fluidTank.readFromNBT(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        mode = tag.getInt("Mode");
        if (tag.contains("FluidFilter")) this.fluidFilter = tag.getString("FluidFilter");
        if (fluidTank.getFluidAmount() > getTier().getCapacity()) {
            fluidTank.getFluid().setAmount(getTier().getCapacity());
        }
    }

    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        fluidTank.writeToNBT(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("Mode", mode);
        tag.putString("FluidFilter", this.fluidFilter);
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FluidBarrelMenu(id, inv, this, this.data);
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.cim." + getTier().name().toLowerCase() + "_barrel");
    }
}