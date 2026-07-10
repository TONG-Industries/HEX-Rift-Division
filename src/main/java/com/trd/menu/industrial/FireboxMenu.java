package com.trd.menu.industrial;

import com.trd.block.basic.ModBlocks;
import com.trd.menu.ModMenuTypes;
import com.trd.multiblock.industrial.FireboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Меню топки. Кнопки GUI работают через штатный clickMenuButton (без кастомных пакетов).
 *  id 0 = вкл/выкл нагреватель (красная/зелёная кнопка)
 *  id 1/2 = подача угля -/+
 *  id 3/4 = подача воды -/+
 */
public class FireboxMenu extends AbstractContainerMenu {
    private final FireboxBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess levelAccess;

    public static final int COAL_SLOTS = 3;

    public FireboxMenu(int id, Inventory inv, FireboxBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FIREBOX_MENU.get(), id);
        this.blockEntity = entity;
        this.data = data;
        this.levelAccess = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());

        // Угольные слоты (0..2)
        for (int i = 0; i < COAL_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(entity.getCoalInventory(), i, 35, 21 + i * 22) {
                @Override public boolean mayPlace(ItemStack stack) { return FireboxBlockEntity.isCoal(stack); }
            });
        }

        // Инвентарь игрока
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(inv, col + row * 9 + 9, 20 + col * 18, 112 + row * 18));
        for (int i = 0; i < 9; i++)
            this.addSlot(new Slot(inv, i, 20 + i * 18, 170));

        this.addDataSlots(data);
    }

    public static FireboxMenu create(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity entity = inv.player.level().getBlockEntity(pos);
        SimpleContainerData data = new SimpleContainerData(12);
        return new FireboxMenu(id, inv, (FireboxBlockEntity) entity, data);
    }

    // ==== Кнопки GUI ====
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) return false;
        switch (id) {
            case 0 -> blockEntity.toggleHeater();
            case 1 -> blockEntity.setCoalFeedRate(blockEntity.getCoalFeedRate() - 1);
            case 2 -> blockEntity.setCoalFeedRate(blockEntity.getCoalFeedRate() + 1);
            case 3 -> blockEntity.setWaterFeedRate(blockEntity.getWaterFeedRate() - 4);
            case 4 -> blockEntity.setWaterFeedRate(blockEntity.getWaterFeedRate() + 4);
            default -> { return false; }
        }
        return true;
    }

    // ==== Геттеры для экрана ====
    public float getTemperature() { return data.get(FireboxBlockEntity.DATA_TEMP) / 10.0f; }
    public int getPressure() { return data.get(FireboxBlockEntity.DATA_PRESSURE); }
    public int getMaxPressure() { int m = data.get(FireboxBlockEntity.DATA_MAX_PRESSURE); return m > 0 ? m : FireboxBlockEntity.MAX_PRESSURE; }
    public boolean isHeaterOn() { return data.get(FireboxBlockEntity.DATA_HEATER_ON) > 0; }
    public boolean isCoalBurning() { return data.get(FireboxBlockEntity.DATA_COAL_BURNING) > 0; }
    public long getEnergy() { return data.get(FireboxBlockEntity.DATA_ENERGY); }
    public long getMaxEnergy() { int m = data.get(FireboxBlockEntity.DATA_ENERGY_MAX); return m > 0 ? m : (int) FireboxBlockEntity.MAX_ENERGY; }
    public int getWater() { return data.get(FireboxBlockEntity.DATA_WATER); }
    public int getSteam() { return data.get(FireboxBlockEntity.DATA_STEAM); }
    public int getSmoke() { return data.get(FireboxBlockEntity.DATA_SMOKE); }
    public int getCoalFeed() { return data.get(FireboxBlockEntity.DATA_COAL_FEED); }
    public int getWaterFeed() { return data.get(FireboxBlockEntity.DATA_WATER_FEED); }
    public FireboxBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.FIREBOX.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ret = stack.copy();
            int invStart = COAL_SLOTS;        // 3
            int invEnd = COAL_SLOTS + 36;     // 39
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else {
                if (FireboxBlockEntity.isCoal(stack)) {
                    if (!this.moveItemStackTo(stack, 0, invStart, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        }
        return ret;
    }
}
