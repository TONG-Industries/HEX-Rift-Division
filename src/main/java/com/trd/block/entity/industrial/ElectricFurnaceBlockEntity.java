package com.trd.block.entity.industrial;

import com.trd.api.energy.IEnergyConnector;
import com.trd.api.energy.IEnergyReceiver;
import com.trd.block.basic.industrial.ElectricFurnaceBlock;
import com.trd.block.entity.ModBlockEntities;
import com.trd.capability.ModCapabilities;
import com.trd.item.energy.EnergyCellItem;
import com.trd.item.energy.ModBatteryItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ElectricFurnaceBlockEntity extends BlockEntity implements IEnergyReceiver {

    public static final long MAX_ENERGY = 10_000L;
    public static final long RECEIVE_SPEED = 1_000L;
    public static final int ENERGY_PER_TICK = 5; // 100 JE/s

    private long energyStored = 0L;
    private int progress = 0;
    private int maxProgress = 0;
    private float experience = 0f;

    private SmeltingRecipe cachedRecipe = null;
    private ItemStack lastInput = ItemStack.EMPTY;

    // --- Инвентарь: 0=input, 1=output, 2=battery ---
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == 0) {
                cachedRecipe = null;
                lastInput = ItemStack.EMPTY;
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> true;
                case 1 -> false;
                case 2 -> stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                        || stack.getItem() instanceof ModBatteryItem
                        || stack.getItem() instanceof EnergyCellItem;
                default -> false;
            };
        }
    };

    private final LazyOptional<net.minecraftforge.items.IItemHandler> itemCap = LazyOptional.of(() -> itemHandler);
    private final LazyOptional<IEnergyReceiver> receiverCap = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyConnector> connectorCap = LazyOptional.of(() -> this);

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> (int) energyStored;
                case 3 -> (int) MAX_ENERGY;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 1) maxProgress = value;
        }

        @Override
        public int getCount() { return 4; }
    };

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE_BE.get(), pos, state);
    }

    // ===================== CAPABILITIES =====================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }
        if (side == null || side == getBackSide()) {
            if (cap == ModCapabilities.ENERGY_RECEIVER) return receiverCap.cast();
            if (cap == ModCapabilities.ENERGY_CONNECTOR) return connectorCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        receiverCap.invalidate();
        connectorCap.invalidate();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        invalidateCaps();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) {
            com.trd.api.energy.EnergyNetworkManager.get((ServerLevel) level).removeNode(getBlockPos());
        }
    }

    // ===================== ENERGY =====================

    @Override
    public long getEnergyStored() { return energyStored; }

    @Override
    public long getMaxEnergyStored() { return MAX_ENERGY; }

    @Override
    public void setEnergyStored(long energy) {
        this.energyStored = Math.max(0, Math.min(energy, MAX_ENERGY));
    }

    @Override
    public long getReceiveSpeed() { return RECEIVE_SPEED; }

    @Override
    public IEnergyReceiver.Priority getPriority() { return IEnergyReceiver.Priority.NORMAL; }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        long can = Math.min(MAX_ENERGY - energyStored, Math.min(maxReceive, RECEIVE_SPEED));
        if (!simulate && can > 0) {
            energyStored += can;
            setChanged();
        }
        return can;
    }

    @Override
    public boolean canReceive() { return energyStored < MAX_ENERGY; }

    public boolean canConnectEnergy(Direction side) { return side == getBackSide(); }

    private Direction getBackSide() {
        BlockState state = getBlockState();
        if (!state.hasProperty(ElectricFurnaceBlock.FACING)) return Direction.NORTH;
        return state.getValue(ElectricFurnaceBlock.FACING).getOpposite();
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    // ===================== TICK =====================

    public void serverTick(ServerLevel level) {
        boolean changed = tryChargeFromBattery();

        Optional<SmeltingRecipe> recipeOpt = getRecipe();
        boolean recipeValid = recipeOpt.isPresent() && canProcess(recipeOpt.get());
        boolean hasEnergy = energyStored >= ENERGY_PER_TICK;

        if (recipeValid && hasEnergy) {
            if (maxProgress == 0) {
                maxProgress = Math.max(1, (int) (recipeOpt.get().getCookingTime() * 0.7f));
            }
            energyStored -= ENERGY_PER_TICK;
            progress++;
            changed = true;

            if (progress >= maxProgress) {
                finishRecipe(recipeOpt.get());
                progress = 0;
                maxProgress = 0;
                changed = true;
                // Пересчитываем рецепт для следующего предмета в очереди
                recipeOpt = getRecipe();
                recipeValid = recipeOpt.isPresent() && canProcess(recipeOpt.get());
            }
        } else {
            if (progress > 0 || maxProgress > 0) {
                progress = 0;
                maxProgress = 0;
                changed = true;
            }
        }

        // === LIT: горит если активно плавит ИЛИ может начать плавку следующего предмета ===
        BlockState state = getBlockState();
        boolean wasLit = state.getValue(ElectricFurnaceBlock.LIT);
        boolean isLit = (progress > 0 && maxProgress > 0) ||
                (recipeOpt.isPresent() && canProcess(recipeOpt.get()) && energyStored >= ENERGY_PER_TICK);

        if (wasLit != isLit) {
            level.setBlock(getBlockPos(), state.setValue(ElectricFurnaceBlock.LIT, isLit), 3);
            changed = true;
        }

        if (changed) {
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    // ===================== RECIPE LOGIC =====================

    private Optional<SmeltingRecipe> getRecipe() {
        ItemStack input = itemHandler.getStackInSlot(0);
        if (input.isEmpty()) {
            cachedRecipe = null;
            lastInput = ItemStack.EMPTY;
            return Optional.empty();
        }
        if (!ItemStack.matches(input, lastInput)) {
            lastInput = input.copy();
            SimpleContainer container = new SimpleContainer(1);
            container.setItem(0, input);
            cachedRecipe = level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, container, level)
                    .orElse(null);
        }
        return Optional.ofNullable(cachedRecipe);
    }

    private boolean canProcess(SmeltingRecipe recipe) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        ItemStack output = itemHandler.getStackInSlot(1);
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void finishRecipe(SmeltingRecipe recipe) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        ItemStack output = itemHandler.getStackInSlot(1);
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(1, result.copy());
        } else {
            output.grow(result.getCount());
        }
        itemHandler.getStackInSlot(0).shrink(1);
        this.experience += recipe.getExperience();
        cachedRecipe = null;
        lastInput = ItemStack.EMPTY;
    }

    // ===================== BATTERY CHARGE =====================

    private boolean tryChargeFromBattery() {
        ItemStack battery = itemHandler.getStackInSlot(2);
        if (battery.isEmpty() || energyStored >= MAX_ENERGY) return false;

        boolean[] changed = {false};

        battery.getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> {
            if (storage.canExtract()) {
                int max = (int) Math.min(MAX_ENERGY - energyStored, RECEIVE_SPEED);
                int extracted = storage.extractEnergy(max, false);
                if (extracted > 0) {
                    energyStored += extracted;
                    changed[0] = true;
                }
            }
        });

        if (!changed[0] && energyStored < MAX_ENERGY) {
            battery.getCapability(ModCapabilities.ENERGY_PROVIDER).ifPresent(provider -> {
                if (provider.canExtract()) {
                    long max = Math.min(MAX_ENERGY - energyStored, RECEIVE_SPEED);
                    long extracted = provider.extractEnergy(max, false);
                    if (extracted > 0) {
                        energyStored += extracted;
                        changed[0] = true;
                    }
                }
            });
        }

        return changed[0];
    }

    // ===================== EXPERIENCE =====================

    public void awardExperience(Player player) {
        if (level instanceof ServerLevel serverLevel && this.experience > 0) {
            int xp = (int) this.experience;
            this.experience -= xp;
            if (xp > 0) {
                ExperienceOrb.award(serverLevel, player.position(), xp);
            }
        }
    }

    // ===================== NBT =====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putLong("Energy", energyStored);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putFloat("Experience", experience);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        energyStored = tag.getLong("Energy");
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        experience = tag.getFloat("Experience");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }
}