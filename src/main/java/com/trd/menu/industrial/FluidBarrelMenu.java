package com.trd.menu;

import com.trd.block.basic.ModBlocks;
import com.trd.block.basic.industrial.fluids.FluidBarrelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.SlotItemHandler;
import com.trd.block.entity.industrial.fluids.FluidBarrelBlockEntity;

public class FluidBarrelMenu extends AbstractContainerMenu {
    public final FluidBarrelBlockEntity blockEntity;
    private final ContainerData data;

    public FluidBarrelMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FLUID_BARREL_MENU.get(), pContainerId);
        this.blockEntity = (FluidBarrelBlockEntity) entity;
        this.data = data;

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            // Наполнение: вход (102, 8) -> выход (102, 44)
            this.addSlot(new SlotItemHandler(handler, 0, 102, 8));
            this.addSlot(new SlotItemHandler(handler, 1, 102, 44));

            // Опустошение: вход (124, 8) -> выход (124, 44)
            this.addSlot(new SlotItemHandler(handler, 2, 124, 8));
            this.addSlot(new SlotItemHandler(handler, 3, 124, 44));

            // Защитный слой (40, 8)
            this.addSlot(new SlotItemHandler(handler, 4, 40, 8));
        });

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
        addDataSlots(data);
    }

    public FluidBarrelMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(1));
    }

    public FluidStack getFluid() {
        return blockEntity.fluidTank.getFluid();
    }

    public int getCapacity() {
        return blockEntity.fluidTank.getCapacity();
    }

    public int getMode() {
        return data.get(0);
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity.getLevel() == null) return false;
        BlockPos pos = blockEntity.getBlockPos();
        return blockEntity.getLevel().getBlockState(pos).getBlock() instanceof FluidBarrelBlock
                && player.distanceToSqr(pos.getCenter()) < 64.0;
    }

    public FluidBarrelBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    // Инвентарь начинается на 8-66 (GUI высота 148, инвентарь 3*18=54, хотбар 18, итого 72. 148-72=76, но с отступом 66 норм)
    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 66 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 66 + 3 * 18 + 4)); // 66 + 54 + 4 = 124
        }
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            // Из бочки (0-4) в инвентарь (5-41)
            if (index < 5) {
                if (!this.moveItemStackTo(stack, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Из инвентаря в бочку
                if (!this.moveItemStackTo(stack, 0, 1, false)) {      // FILL_IN
                    if (!this.moveItemStackTo(stack, 2, 3, false)) {  // DRAIN_IN
                        if (!this.moveItemStackTo(stack, 4, 5, false)) { // PROTECTOR
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
}