package com.trd.menu.industrial;

import com.trd.menu.ModMenuTypes;
import com.trd.multiblock.industrial.SteelStorageBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class SteelStorageMenu extends AbstractContainerMenu {

    public static final int SLOTS = SteelStorageBlockEntity.SLOTS;
    private final SteelStorageBlockEntity blockEntity;

    // Конструктор для открытия на стороне сервера
    public SteelStorageMenu(int id, Inventory playerInv, SteelStorageBlockEntity be) {
        super(ModMenuTypes.STEEL_STORAGE_MENU.get(), id);
        this.blockEntity = be;

        // Сетка 13x4, начало (8, 15)
        for (int row = 0; row < SteelStorageBlockEntity.ROWS; row++) {
            for (int col = 0; col < SteelStorageBlockEntity.COLS; col++) {
                int index = row * SteelStorageBlockEntity.COLS + col;
                int x = 8 + col * 18;
                int y = 15 + row * 18;
                this.addSlot(new SlotItemHandler(be.getInventory(), index, x, y));
            }
        }

        // Инвентарь игрока (44, 91)
        int invX = 44;
        int invY = 91;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }

        // Хотбар (44, 149) — 91 + 58
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, invX + col * 18, invY + 58));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < SLOTS) {
                if (!this.moveItemStackTo(itemstack1, SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !blockEntity.isRemoved();
    }
}