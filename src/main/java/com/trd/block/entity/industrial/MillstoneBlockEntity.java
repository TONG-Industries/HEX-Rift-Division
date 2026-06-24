package com.trd.block.entity.industrial;

import com.trd.block.basic.ModBlocks;
import com.trd.block.entity.ModBlockEntities;
import com.trd.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MillstoneBlockEntity extends BlockEntity {

    public static final int GRIND_COOLDOWN = 20;
    public static final float ROTATION_SPEED = 360.0f / GRIND_COOLDOWN;

    public static final Map<Item, GrindRecipe> RECIPES = new HashMap<>();

    static {
        // --- Предметы ---
        RECIPES.put(ModItems.LIMESTONE_CHUNK.get(), new GrindRecipe(
                List.of(new ItemStack(ModItems.LIMESTONE_POWDER.get(), 1)), 2
        ));
        RECIPES.put(ModItems.BAUXITE_CHUNK.get(), new GrindRecipe(
                List.of(new ItemStack(ModItems.BAUXITE_POWDER.get(), 1)), 2
        ));
        RECIPES.put(ModItems.DOLOMITE_CHUNK.get(), new GrindRecipe(
                List.of(new ItemStack(ModItems.DOLOMITE_POWDER.get(), 1)), 2
        ));

        // --- Блоки (новые рецепты) ---
        // ВАЖНО: замени имена LIMESTONE/BAUXITE/DOLOMITE на реальные из твоего ModBlocks если они отличаются
        RECIPES.put(ModBlocks.LIMESTONE.get().asItem(), new GrindRecipe(
                List.of(
                        new ItemStack(ModItems.LIMESTONE_CHUNK.get(), 1),
                        new ItemStack(Blocks.GRAVEL, 1)
                ), 4
        ));
        RECIPES.put(ModBlocks.BAUXITE.get().asItem(), new GrindRecipe(
                List.of(
                        new ItemStack(ModItems.BAUXITE_CHUNK.get(), 1),
                        new ItemStack(Blocks.GRAVEL, 1)
                ), 5
        ));
        RECIPES.put(ModBlocks.DOLOMITE.get().asItem(), new GrindRecipe(
                List.of(
                        new ItemStack(ModItems.DOLOMITE_CHUNK.get(), 1),
                        new ItemStack(Blocks.GRAVEL, 1)
                ), 6
        ));
    }

    private final ItemStackHandler itemHandler = new ItemStackHandler(5) {
        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : 64;
        }
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    };

    private int currentGrinds = 0;
    private int requiredGrinds = 0;
    private boolean isProcessing = false;

    private int cooldownTicks = 0;
    private float rotationAngle = 0.0f;
    private boolean isGrinding = false;

    public MillstoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MILLSTONE.get(), pos, state);
    }

    public ItemStack getInputStack() { return itemHandler.getStackInSlot(0); }

    public List<ItemStack> getResultStacks() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) list.add(stack);
        }
        return list;
    }

    public boolean isOutputEmpty() {
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    public boolean isProcessing() { return isProcessing; }
    public int getCurrentGrinds() { return currentGrinds; }
    public int getRequiredGrinds() { return requiredGrinds; }
    public int getRemainingGrinds() { return Math.max(0, requiredGrinds - currentGrinds); }
    public boolean canGrind() { return isProcessing && getRemainingGrinds() > 0 && cooldownTicks <= 0; }

    public float getRotationAngle() { return rotationAngle; }
    public boolean isGrinding() { return isGrinding; }
    public int getCooldownProgress() {
        return cooldownTicks > 0 ? (GRIND_COOLDOWN - cooldownTicks) : GRIND_COOLDOWN;
    }

    public int getComparatorSignal() {
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return 15;
        }
        if (isProcessing && requiredGrinds > 0) {
            return 1 + (int)((14.0f * currentGrinds) / requiredGrinds);
        }
        return 0;
    }

    public ItemStack insertItem(ItemStack stack) {
        GrindRecipe recipe = RECIPES.get(stack.getItem());
        if (recipe == null) return stack;
        if (!itemHandler.getStackInSlot(0).isEmpty() || !isOutputEmpty()) {
            return stack;
        }

        ItemStack single = stack.split(1);
        itemHandler.setStackInSlot(0, single);

        currentGrinds = 0;
        requiredGrinds = recipe.grindsRequired;
        isProcessing = true;
        cooldownTicks = 0;
        rotationAngle = 0;

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

        return stack;
    }

    public boolean doGrind() {
        if (!canGrind()) return false;

        cooldownTicks = GRIND_COOLDOWN;
        isGrinding = true;
        currentGrinds++;

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }

    private void finishGrind() {
        ItemStack input = itemHandler.getStackInSlot(0);
        GrindRecipe recipe = RECIPES.get(input.getItem());

        if (recipe != null) {
            itemHandler.setStackInSlot(0, ItemStack.EMPTY);
            List<ItemStack> outputs = recipe.outputs();
            for (int i = 0; i < outputs.size() && i < itemHandler.getSlots() - 1; i++) {
                itemHandler.setStackInSlot(i + 1, outputs.get(i).copy());
            }
        }

        isProcessing = false;
        isGrinding = false;
        currentGrinds = 0;
        requiredGrinds = 0;
        cooldownTicks = 0;
    }

    public ItemStack extractResult() {
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack result = itemHandler.getStackInSlot(i);
            if (!result.isEmpty()) {
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    public List<ItemStack> extractAllResults() {
        List<ItemStack> results = new ArrayList<>();
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                results.add(stack);
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        if (!results.isEmpty()) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return results;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MillstoneBlockEntity be) {
        if (!level.isClientSide) {
            if (!be.isProcessing && !be.itemHandler.getStackInSlot(0).isEmpty() && be.isOutputEmpty()) {
                ItemStack input = be.itemHandler.getStackInSlot(0);
                GrindRecipe recipe = RECIPES.get(input.getItem());
                if (recipe != null) {
                    be.currentGrinds = 0;
                    be.requiredGrinds = recipe.grindsRequired;
                    be.isProcessing = true;
                    be.cooldownTicks = 0;
                    be.rotationAngle = 0;
                    be.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                }
            }
        }

        if (be.cooldownTicks > 0) {
            be.cooldownTicks--;
            if (be.isGrinding) {
                be.rotationAngle += ROTATION_SPEED;
                if (be.rotationAngle >= 360.0f) {
                    be.rotationAngle -= 360.0f;
                }
            }
            if (be.cooldownTicks <= 0) {
                be.isGrinding = false;
                if (!level.isClientSide && be.isProcessing && be.currentGrinds >= be.requiredGrinds && be.requiredGrinds > 0) {
                    be.finishGrind();
                    level.sendBlockUpdated(pos, state, state, 3);
                }
            }
            be.setChanged();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        currentGrinds = tag.getInt("CurrentGrinds");
        requiredGrinds = tag.getInt("RequiredGrinds");
        isProcessing = tag.getBoolean("IsProcessing");
        cooldownTicks = tag.getInt("CooldownTicks");
        rotationAngle = tag.getFloat("RotationAngle");
        isGrinding = tag.getBoolean("IsGrinding");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("CurrentGrinds", currentGrinds);
        tag.putInt("RequiredGrinds", requiredGrinds);
        tag.putBoolean("IsProcessing", isProcessing);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putFloat("RotationAngle", rotationAngle);
        tag.putBoolean("IsGrinding", isGrinding);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    public void dropContents() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), itemHandler.getStackInSlot(i));
        }
    }

    public record GrindRecipe(List<ItemStack> outputs, int grindsRequired) {}
}