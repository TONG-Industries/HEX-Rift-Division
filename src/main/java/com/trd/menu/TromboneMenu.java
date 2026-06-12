package com.trd.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import com.trd.block.entity.weapons.TurretAmmoContainer;
import com.trd.item.energy.EnergyCellItem;
import com.trd.item.energy.ModBatteryItem;
import com.trd.item.weapons.turrets.TurretChipItem;

public class TromboneMenu extends AbstractContainerMenu {

    private final TurretAmmoContainer ammoContainer;
    private final ContainerData data;
    private final BlockPos pos;

    public static final int AMMO_SLOT_COUNT = 9;
    public static final int CHIP_SLOT_INDEX = 9;
    public static final int BATTERY_SLOT_INDEX = 10;
    public static final int TOTAL_TURRET_SLOTS = 11;

    // --- КОНСТАНТЫ ДАННЫХ ---
    public static final int DATA_ENERGY = 0;
    public static final int DATA_MAX_ENERGY = 1;
    public static final int DATA_STATUS = 2;
    public static final int DATA_SWITCH = 3;
    public static final int DATA_BOOT_TIMER = 4;
    public static final int DATA_TARGET_HOSTILE = 5;
    public static final int DATA_TARGET_NEUTRAL = 6;
    public static final int DATA_TARGET_PLAYERS = 7;
    public static final int DATA_KILLS = 8;
    public static final int DATA_LIFETIME = 9;
    public static final int DATA_EXTRA_1 = 10; // [НОВОЕ] Доп кнопка 1
    public static final int DATA_EXTRA_2 = 11; // [НОВОЕ] Доп кнопка 2
    private static final int DATA_COUNT = 12;

    public TromboneMenu(int containerId, Inventory playerInventory, TurretAmmoContainer ammoContainer, ContainerData data, BlockPos pos) {
        super(ModMenuTypes.TROMBONE_MENU.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);

        this.ammoContainer = ammoContainer;
        this.data = data;
        this.pos = pos;
        this.addDataSlots(data);

        // Слоты патронов 0-8
        int ammoStartX = 115;
        int ammoStartY = 44;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new SlotItemHandler(ammoContainer, row * 3 + col, ammoStartX + col * 18, ammoStartY + row * 18) {
                    @Override public void setChanged() { super.setChanged(); ammoContainer.onContentsChanged(this.getSlotIndex()); }
                });
            }
        }

        // Слот ЧИПА (9) — 91, 80
        this.addSlot(new SlotItemHandler(ammoContainer, CHIP_SLOT_INDEX, 90, 79) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof TurretChipItem; }
            @Override public void setChanged() { super.setChanged(); ammoContainer.onContentsChanged(this.getSlotIndex()); }
        });

        // Слот БАТАРЕИ (10) — 180, 81
        this.addSlot(new SlotItemHandler(ammoContainer, BATTERY_SLOT_INDEX, 180, 81) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                        || stack.getItem() instanceof ModBatteryItem
                        || stack.getItem() instanceof EnergyCellItem;
            }
            @Override public void setChanged() { super.setChanged(); ammoContainer.onContentsChanged(this.getSlotIndex()); }
        });

        // Инвентарь игрока
        int playerStartX = 8;
        int playerStartY = 106;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, playerStartX + column * 18, playerStartY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, playerStartX + column * 18, playerStartY + 58));
        }
    }

    public TromboneMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new TurretAmmoContainer(), new SimpleContainerData(DATA_COUNT), extraData.readBlockPos());
    }

    public BlockPos getPos() { return pos; }
    public int getDataSlot(int index) { return this.data.get(index); }
    public TurretAmmoContainer getAmmoContainer() { return ammoContainer; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            int totalSlots = TOTAL_TURRET_SLOTS;
            int playerInvStart = totalSlots;
            int playerInvEnd = playerInvStart + 27;
            int hotbarStart = playerInvEnd;
            int hotbarEnd = hotbarStart + 9;

            if (index < totalSlots) {
                if (!this.moveItemStackTo(stack, playerInvStart, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stack.getItem() instanceof TurretChipItem) {
                    if (!this.moveItemStackTo(stack, CHIP_SLOT_INDEX, CHIP_SLOT_INDEX + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                        || stack.getItem() instanceof ModBatteryItem
                        || stack.getItem() instanceof EnergyCellItem) {
                    if (!this.moveItemStackTo(stack, BATTERY_SLOT_INDEX, BATTERY_SLOT_INDEX + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(stack, 0, AMMO_SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) { return true; }
}