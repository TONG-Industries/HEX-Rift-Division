package com.trd.block.entity.weapons;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import com.trd.item.energy.EnergyCellItem;
import com.trd.item.energy.ModBatteryItem;
import com.trd.item.weapons.ammo.IAmmoItem;
import com.trd.item.weapons.turrets.TurretChipItem;

public class TurretAmmoContainer extends ItemStackHandler {

    private static final int SLOT_COUNT = 11;
    private Runnable onContentsChanged;

    public TurretAmmoContainer() {
        super(SLOT_COUNT);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        // Слот 9 — чип
        if (slot == 9) {
            return stack.getItem() instanceof TurretChipItem;
        }
        // [ФИКС] Слот 10 — только батарейки / предметы с энергией
        if (slot == 10) {
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                    || stack.getItem() instanceof ModBatteryItem
                    || stack.getItem() instanceof EnergyCellItem;
        }
        // Слоты 0-8 — только патроны
        return stack.getItem() instanceof IAmmoItem;
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
     * Проверяет наличие патрона нужного калибра, но НЕ забирает его.
     */
    public IAmmoItem peekAmmo(String caliber) {
        for (int i = 0; i < 9; i++) { // [ИЗМЕНЕНО] Только слоты патронов 0-8
            ItemStack stack = getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof IAmmoItem ammo) {
                if (ammo.getCaliber().equals(caliber)) {
                    return ammo;
                }
            }
        }
        return null;
    }

    public IAmmoItem takeAmmoAndGet(String caliber) {
        for (int i = 0; i < 9; i++) { // [ИЗМЕНЕНО] Только слоты патронов 0-8
            ItemStack stack = getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof IAmmoItem ammo) {
                if (ammo.getCaliber().equals(caliber)) {
                    stack.shrink(1);
                    return ammo;
                }
            }
        }
        return null;
    }

    public int countAmmo(String caliber) {
        int count = 0;
        for (int i = 0; i < 9; i++) { // [ИЗМЕНЕНО] Только слоты патронов 0-8
            ItemStack stack = getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof IAmmoItem ammo) {
                if (ammo.getCaliber().equals(caliber)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    public boolean takeAmmo(String caliber) {
        for (int i = 0; i < 9; i++) { // [ИЗМЕНЕНО] Только слоты патронов 0-8
            ItemStack stack = getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof IAmmoItem ammo) {
                if (ammo.getCaliber().equals(caliber)) {
                    stack.shrink(1);
                    return true;
                }
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