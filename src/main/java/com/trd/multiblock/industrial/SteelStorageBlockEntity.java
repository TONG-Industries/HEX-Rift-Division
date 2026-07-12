package com.trd.multiblock.industrial;

import com.trd.block.entity.ModBlockEntities;
import com.trd.menu.industrial.SteelStorageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SteelStorageBlockEntity extends BlockEntity implements MenuProvider {

    public static final int ROWS = 7;
    public static final int COLS = 13;
    public static final int SLOTS = ROWS * COLS; // 91

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // ═══ ФИКС: шлём обновление клиентам при изменении инвентаря ═══
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (stack.isEmpty()) return true;
            if (stack.getItem() instanceof BlockItem bi) {
                Block block = bi.getBlock();
                if (block instanceof ShulkerBoxBlock) return false;
                if (block instanceof SteelStorageBlock) return false;
            }
            return super.isItemValid(slot, stack);
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    public SteelStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEEL_STORAGE_BE.get(), pos, state);
    }

    public boolean isEmpty() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(net.minecraft.nbt.CompoundTag tag) {
        load(tag);
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void dropContents() {
        if (level != null && !level.isClientSide) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            CompoundTag invTag = tag.getCompound("Inventory");
            // ═══════════════════════════════════════════════════════
            // ФИКС: старые NBT могут содержать меньше слотов (52 вместо 91)
            // Расширяем до SLOTS, сохраняя старые предметы
            // ═══════════════════════════════════════════════════════
            if (invTag.contains("Size", net.minecraft.nbt.Tag.TAG_INT)) {
                int oldSize = invTag.getInt("Size");
                if (oldSize < SLOTS) {
                    invTag.putInt("Size", SLOTS);
                }
            } else {
                // Если нет поля Size — добавляем
                invTag.putInt("Size", SLOTS);
            }
            inventory.deserializeNBT(invTag);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.trd.steel_storage");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SteelStorageMenu(id, inv, this);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }
}