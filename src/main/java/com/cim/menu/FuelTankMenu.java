package com.cim.menu;

import com.cim.multiblock.industrial.FuelTankBlock;
import com.cim.multiblock.industrial.FuelTankBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.SlotItemHandler;

public class FuelTankMenu extends AbstractContainerMenu {
    public final FuelTankBlockEntity blockEntity;
    private final ContainerData data;

    public FuelTankMenu(int pContainerId, Inventory inv, FuelTankBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FUEL_TANK_MENU.get(), pContainerId);
        this.blockEntity = entity;
        this.data = data;

        entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            // fill input, fill output, drain input, drain output как в бочке
            this.addSlot(new SlotItemHandler(handler, 0, 102, 8));
            this.addSlot(new SlotItemHandler(handler, 1, 102, 44));
            this.addSlot(new SlotItemHandler(handler, 2, 124, 8));
            this.addSlot(new SlotItemHandler(handler, 3, 124, 44));
            // protector slot – тот же x=40, y=8 как в бочке
            this.addSlot(new SlotItemHandler(handler, 4, 40, 8));
        });

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
        addDataSlots(data);
    }

    public FuelTankMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv,
                (FuelTankBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(1));
    }

    public FluidStack getFluid() { return blockEntity.getFluid(); }
    public int getCapacity() { return blockEntity.getCapacity(); }
    public int getMode() { return data.get(0); }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null &&
                blockEntity.getLevel().getBlockState(blockEntity.getBlockPos()).getBlock() instanceof FuelTankBlock &&
                player.distanceToSqr(blockEntity.getBlockPos().getCenter()) < 64.0;
    }

    // Инвентарь и хотбар – координаты как в бочке (y=66 для первого слота инвентаря,
    // хотбар y=124, но у бочки высота 148, поэтому хотбар на y=124)
    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 66 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 124));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            stack = slotStack.copy();
            if (index < 4) { // из слотов цистерны в инвентарь
                if (!this.moveItemStackTo(slotStack, 4, 40, true)) return ItemStack.EMPTY;
            } else { // из инвентаря в слоты цистерны
                if (!this.moveItemStackTo(slotStack, 0, 1, false) &&   // fill input
                        !this.moveItemStackTo(slotStack, 2, 3, false)) {   // drain input
                    return ItemStack.EMPTY;
                }
            }
            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return stack;
    }
}