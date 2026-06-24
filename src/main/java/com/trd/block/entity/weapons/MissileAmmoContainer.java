package com.trd.block.entity.weapons;

import com.trd.item.tags.IMissileItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import com.trd.item.energy.EnergyCellItem;
import com.trd.item.energy.ModBatteryItem;
import com.trd.item.weapons.turrets.TurretChipItem;

/**
 * Контейнер боеприпасов для ракетницы Тромбон.
 * Слоты:
 * 0-8 — ракеты (только IMissileItem)
 * 9 — чип доступа (TurretChipItem)
 * 10 — батарейка (предметы с энергией)
 */
public class MissileAmmoContainer extends ItemStackHandler {

    private static final int SLOT_COUNT = 11;
    private static final int MISSILE_SLOTS = 9; // Слоты 0-8
    public static final int CHIP_SLOT_INDEX = 9;
    public static final int BATTERY_SLOT_INDEX = 10;

    private Runnable onContentsChanged;

    public MissileAmmoContainer() {
        super(SLOT_COUNT);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        // Слоты 0-8 — только ракеты
        if (slot >= 0 && slot < MISSILE_SLOTS) {
            return stack.getItem() instanceof IMissileItem;
        }
        // Слот 9 — чип
        if (slot == CHIP_SLOT_INDEX) {
            return stack.getItem() instanceof TurretChipItem;
        }
        // Слот 10 — батарейки / предметы с энергией
        if (slot == BATTERY_SLOT_INDEX) {
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                    || stack.getItem() instanceof ModBatteryItem
                    || stack.getItem() instanceof EnergyCellItem;
        }
        return false;
    }

    public void setOnContentsChanged(Runnable callback) {
        this.onContentsChanged = callback;
    }

    @Override
    public void onContentsChanged(int slot) {
        if (onContentsChanged != null) {
            onContentsChanged.run();
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    /**
     * Проверяет наличие ракеты любого типа, но НЕ забирает её.
     * @return тип ракеты или null, если ракет нет
     */
    public String peekMissileType() {
        for (int i = 0; i < MISSILE_SLOTS; i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IMissileItem missile) {
                return missile.getMissileType();
            }
        }
        return null;
    }

    /**
     * Забирает 1 ракету и возвращает её тип.
     * @return тип ракеты или null, если ракет нет
     */
    public String takeMissileAndGetType() {
        for (int i = 0; i < MISSILE_SLOTS; i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IMissileItem missile) {
                String type = missile.getMissileType();
                stack.shrink(1);
                return type;
            }
        }
        return null;
    }

    /**
     * Подсчитывает общее количество ракет.
     */
    public int countMissiles() {
        int count = 0;
        for (int i = 0; i < MISSILE_SLOTS; i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IMissileItem) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Подсчитывает ракеты конкретного типа.
     */
    public int countMissilesByType(String type) {
        int count = 0;
        for (int i = 0; i < MISSILE_SLOTS; i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IMissileItem missile) {
                if (missile.getMissileType().equals(type)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    /**
     * Проверяет, есть ли хотя бы 1 ракета.
     */
    public boolean hasMissiles() {
        for (int i = 0; i < MISSILE_SLOTS; i++) {
            if (!getStackInSlot(i).isEmpty() && getStackInSlot(i).getItem() instanceof IMissileItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag nbtList = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!stacks.get(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stacks.get(i).save(itemTag);
                nbtList.add(itemTag);
            }
        }
        tag.put("Items", nbtList);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        ListTag nbtList = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < nbtList.size(); i++) {
            CompoundTag itemTag = nbtList.getCompound(i);
            int slot = itemTag.getByte("Slot") & 0xFF;
            if (slot < SLOT_COUNT) {
                stacks.set(slot, ItemStack.of(itemTag));
            }
        }
    }
}