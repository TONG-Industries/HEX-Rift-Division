package com.trd.menu.industrial;

import com.trd.block.entity.industrial.ElectricFurnaceBlockEntity;
import com.trd.item.energy.EnergyCellItem;
import com.trd.item.energy.ModBatteryItem;
import com.trd.menu.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class ElectricFurnaceMenu extends AbstractContainerMenu {

    public final ElectricFurnaceBlockEntity blockEntity;
    private final ContainerData data;
    private final Level level;
    public int getDataSlot(int index) {
        return this.data.get(index);
    }
    public ElectricFurnaceMenu(int containerId, Inventory inv, ElectricFurnaceBlockEntity be, ContainerData data) {
        super(ModMenuTypes.ELECTRIC_FURNACE_MENU.get(), containerId);
        checkContainerDataCount(data, 4);

        this.blockEntity = be;
        this.data = data;
        this.level = inv.player.level();
        addDataSlots(data);

        // 0 — Input (56, 46)
        this.addSlot(new SlotItemHandler(be.getItemHandler(), 0, 56, 46));

        // 1 — Output (104, 46)
        this.addSlot(new SlotItemHandler(be.getItemHandler(), 1, 104, 46) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                be.awardExperience(player);
            }
        });

        // 2 — Battery (132, 70)
        this.addSlot(new SlotItemHandler(be.getItemHandler(), 2, 132, 70) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                        || stack.getItem() instanceof ModBatteryItem
                        || stack.getItem() instanceof EnergyCellItem;
            }
        });

        // Inventory (8, 114)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 114 + row * 18));
            }
        }
        // Hotbar (8, 172)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 172));
        }
    }

    public ElectricFurnaceMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv,
                (ElectricFurnaceBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(4));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < 3) {
            if (index == 1) blockEntity.awardExperience(player);
            if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
        } else {
            if (isEnergyItem(stack)) {
                if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        blockEntity.awardExperience(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(level, blockEntity.getBlockPos()).evaluate((lvl, pos) ->
                lvl.getBlockState(pos).getBlock() instanceof com.trd.block.basic.industrial.ElectricFurnaceBlock
                        && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }

    private boolean isEnergyItem(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                || stack.getItem() instanceof ModBatteryItem
                || stack.getItem() instanceof EnergyCellItem;
    }
}