package com.trd.menu.industrial;

import com.trd.block.entity.industrial.ConveyorBufferBlockEntity;
import com.trd.menu.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ConveyorBufferMenu extends AbstractContainerMenu {
    private final ConveyorBufferBlockEntity blockEntity;

    // Серверный конструктор
    public ConveyorBufferMenu(int id, Inventory playerInv, ConveyorBufferBlockEntity be) {
        super(ModMenuTypes.CONVEYOR_BUFFER.get(), id);
        this.blockEntity = be;
        initSlots(playerInv);
    }

    // Клиентский конструктор (читает BlockPos из буфера, который записал NetworkHooks.openScreen)
    public ConveyorBufferMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenuTypes.CONVEYOR_BUFFER.get(), id);
        BlockPos pos = buf.readBlockPos();
        this.blockEntity = playerInv.player.level().getBlockEntity(pos) instanceof ConveyorBufferBlockEntity be ? be : null;
        initSlots(playerInv);
    }

    private void initSlots(Inventory playerInv) {
        var handler = blockEntity != null ? blockEntity.getInventory() : new net.minecraftforge.items.ItemStackHandler(27);

        // Буфер: 3×9, начало 8, 29
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(handler, row * 9 + col, 8 + col * 18, 29 + row * 18));
            }
        }

        // Инвентарь игрока: начало 8, 91
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, 9 + row * 9 + col, 8 + col * 18, 91 + row * 18));
            }
        }

        // Хотбар: 8, 149
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 149));
        }
    }

    public ConveyorBufferBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 27) {
                if (!this.moveItemStackTo(itemstack1, 27, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(itemstack1, 0, 27, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null;
    }
}