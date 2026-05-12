package com.trd.multiblock.industrial;

import com.trd.block.basic.ModBlocks;
import com.trd.block.entity.ModBlockEntities;
import com.trd.item.ModItems;
import com.trd.menu.HeaterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class HeaterBlockEntity extends BlockEntity implements MenuProvider {
    // Инвентарь с синхронизацией
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return isFuel(stack);
            if (slot == 1) return stack.is(ModItems.FUEL_ASH.get());
            return false;
        }
    };

    // Шансы выпадения золы по тирам (в процентах)
    private static final int[] ASH_CHANCES = {
            0,    // Тир 0: 0%
            0,    // Тир 1: 0%
            40,   // Тир 2: 40%
            60,   // Тир 3: 60%
            80,   // Тир 4: 80%
            100   // Тир 5: 100%
    };

    // Данные для GUI (температура хранится как int * 10 для 1 знака после запятой)
    private final SimpleContainerData data = new SimpleContainerData(4);
    public static final int DATA_TEMP = 0;
    public static final int DATA_BURN_TIME = 1;
    public static final int DATA_TOTAL_BURN_TIME = 2;
    public static final int DATA_IS_BURNING = 3;

    public static final float MAX_TEMP = 1600.0f;

    public int getTemperatureScaled() {
        return (int) (temperature * 10.0f);
    }
    // {heatPerTick, burnTicks} - heatPerTick теперь float!
    private static final float[][] TIER_STATS = {
            {1f, 125},
            {2f, 250},
            {3f, 500},
            {4f, 800},
            {6f, 1200},
            {8f, 2400}
    };

    private float temperature = 0.0f;
    private int burnTime = 0;
    private int totalBurnTime = 0;
    private int fuelTier = 0;

    public HeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeaterBlockEntity be) {
        boolean changed = false;

        // === ОХЛАЖДЕНИЕ С ЕСТЕСТВЕННЫМИ КОЛЕБАНИЯМИ ===
        float baseCooling = (be.temperature * be.temperature) / 512000.0f;

        // Минимальное охлажение чтобы температура падала до 0
        if (baseCooling < 0.05f && be.temperature > 0) {
            baseCooling = 0.05f;
        }

        // Термический шум только при высоких температурах
        float thermalNoise = 0.0f;
        if (be.temperature > 200.0f && baseCooling > 0.5f) {
            thermalNoise = (level.random.nextFloat() * 0.4f) - 0.2f; // ±0.2
        }

        float cooling = Math.max(0.05f, baseCooling + thermalNoise);

        if (be.temperature > 0.0f) {
            be.temperature = Math.max(0.0f, be.temperature - cooling);
            changed = true;
        }

        // === НАГРЕВ ===
        if (be.burnTime > 0) {
            be.burnTime--;
            float heatPerTick = TIER_STATS[be.fuelTier][0];
            be.temperature = Math.min(MAX_TEMP, be.temperature + heatPerTick);
            changed = true;

            // Зола выпадает по шансу
            if (be.burnTime == 0 && be.fuelTier >= 2) {
                int chance = ASH_CHANCES[be.fuelTier];
                if (level.random.nextInt(100) < chance) {
                    ItemStack ash = new ItemStack(ModItems.FUEL_ASH.get(), 1);
                    ItemStack remaining = be.inventory.insertItem(1, ash, false);
                    if (!remaining.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remaining);
                    }
                }
            }
        } else {
            // Пытаемся взять новое топливо
            ItemStack fuel = be.inventory.getStackInSlot(0);
            if (!fuel.isEmpty()) {
                int tier = be.getFuelTier(fuel);
                if (tier >= 0) {
                    be.fuelTier = tier;
                    be.burnTime = (int) TIER_STATS[tier][1];
                    be.totalBurnTime = be.burnTime;

                    ItemStack remainder = fuel.getCraftingRemainingItem();
                    fuel.shrink(1);

                    if (fuel.isEmpty() && !remainder.isEmpty()) {
                        be.inventory.setStackInSlot(0, remainder);
                    } else if (!remainder.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder);
                    }

                    changed = true;
                }
            }
        }

        // Синхронизация данных (температура * 10 для сохранения 1 знака после запятой)
        be.data.set(DATA_TEMP, (int) (be.temperature * 10.0f));
        be.data.set(DATA_BURN_TIME, be.burnTime);
        be.data.set(DATA_TOTAL_BURN_TIME, be.totalBurnTime);
        be.data.set(DATA_IS_BURNING, be.burnTime > 0 ? 1 : 0);

        if (changed) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public boolean isFuel(ItemStack stack) {
        return getFuelTier(stack) >= 0;
    }

    public int getFuelTier(ItemStack stack) {
        Item item = stack.getItem();

        // ========== ТИР 0: Дешёвое деревянное топливо ==========

        // Палки и хлам
        if (item == Items.STICK) return 0;
        if (item == Items.SCAFFOLDING) return 0;
        if (item == Items.OAK_PLANKS || item == Items.SPRUCE_PLANKS ||
                item == Items.BIRCH_PLANKS || item == Items.JUNGLE_PLANKS ||
                item == Items.ACACIA_PLANKS || item == Items.DARK_OAK_PLANKS ||
                item == Items.MANGROVE_PLANKS || item == Items.CHERRY_PLANKS ||
                item == Items.BAMBOO_PLANKS || item == Items.BAMBOO_MOSAIC) return 0;
        if (item == Items.OAK_SLAB || item == Items.SPRUCE_SLAB ||
                item == Items.BIRCH_SLAB || item == Items.JUNGLE_SLAB ||
                item == Items.ACACIA_SLAB || item == Items.DARK_OAK_SLAB ||
                item == Items.MANGROVE_SLAB || item == Items.CHERRY_SLAB ||
                item == Items.BAMBOO_SLAB || item == Items.BAMBOO_MOSAIC_SLAB) return 0;
        if (item == Items.OAK_STAIRS || item == Items.SPRUCE_STAIRS ||
                item == Items.BIRCH_STAIRS || item == Items.JUNGLE_STAIRS ||
                item == Items.ACACIA_STAIRS || item == Items.DARK_OAK_STAIRS ||
                item == Items.MANGROVE_STAIRS || item == Items.CHERRY_STAIRS ||
                item == Items.BAMBOO_STAIRS || item == Items.BAMBOO_MOSAIC_STAIRS) return 0;
        if (item == Items.OAK_FENCE || item == Items.SPRUCE_FENCE ||
                item == Items.BIRCH_FENCE || item == Items.JUNGLE_FENCE ||
                item == Items.ACACIA_FENCE || item == Items.DARK_OAK_FENCE ||
                item == Items.MANGROVE_FENCE || item == Items.CHERRY_FENCE ||
                item == Items.BAMBOO_FENCE) return 0;
        if (item == Items.OAK_FENCE_GATE || item == Items.SPRUCE_FENCE_GATE ||
                item == Items.BIRCH_FENCE_GATE || item == Items.JUNGLE_FENCE_GATE ||
                item == Items.ACACIA_FENCE_GATE || item == Items.DARK_OAK_FENCE_GATE ||
                item == Items.MANGROVE_FENCE_GATE || item == Items.CHERRY_FENCE_GATE ||
                item == Items.BAMBOO_FENCE_GATE) return 0;
        if (item == Items.OAK_DOOR || item == Items.SPRUCE_DOOR ||
                item == Items.BIRCH_DOOR || item == Items.JUNGLE_DOOR ||
                item == Items.ACACIA_DOOR || item == Items.DARK_OAK_DOOR ||
                item == Items.MANGROVE_DOOR || item == Items.CHERRY_DOOR ||
                item == Items.BAMBOO_DOOR) return 0;
        if (item == Items.OAK_TRAPDOOR || item == Items.SPRUCE_TRAPDOOR ||
                item == Items.BIRCH_TRAPDOOR || item == Items.JUNGLE_TRAPDOOR ||
                item == Items.ACACIA_TRAPDOOR || item == Items.DARK_OAK_TRAPDOOR ||
                item == Items.MANGROVE_TRAPDOOR || item == Items.CHERRY_TRAPDOOR ||
                item == Items.BAMBOO_TRAPDOOR) return 0;
        if (item == Items.OAK_BUTTON || item == Items.SPRUCE_BUTTON ||
                item == Items.BIRCH_BUTTON || item == Items.JUNGLE_BUTTON ||
                item == Items.ACACIA_BUTTON || item == Items.DARK_OAK_BUTTON ||
                item == Items.MANGROVE_BUTTON || item == Items.CHERRY_BUTTON ||
                item == Items.BAMBOO_BUTTON) return 0;
        if (item == Items.OAK_PRESSURE_PLATE || item == Items.SPRUCE_PRESSURE_PLATE ||
                item == Items.BIRCH_PRESSURE_PLATE || item == Items.JUNGLE_PRESSURE_PLATE ||
                item == Items.ACACIA_PRESSURE_PLATE || item == Items.DARK_OAK_PRESSURE_PLATE ||
                item == Items.MANGROVE_PRESSURE_PLATE || item == Items.CHERRY_PRESSURE_PLATE ||
                item == Items.BAMBOO_PRESSURE_PLATE) return 0;
        if (item == Items.OAK_SIGN || item == Items.SPRUCE_SIGN ||
                item == Items.BIRCH_SIGN || item == Items.JUNGLE_SIGN ||
                item == Items.ACACIA_SIGN || item == Items.DARK_OAK_SIGN ||
                item == Items.MANGROVE_SIGN || item == Items.CHERRY_SIGN ||
                item == Items.BAMBOO_SIGN ||  item == Items.OAK_HANGING_SIGN ||
                item == Items.SPRUCE_HANGING_SIGN || item == Items.BIRCH_HANGING_SIGN ||
                item == Items.JUNGLE_HANGING_SIGN || item == Items.ACACIA_HANGING_SIGN ||
                item == Items.DARK_OAK_HANGING_SIGN || item == Items.MANGROVE_HANGING_SIGN ||
                item == Items.CHERRY_HANGING_SIGN || item == Items.BAMBOO_HANGING_SIGN) return 0;
        if (item == Items.OAK_LOG || item == Items.SPRUCE_LOG ||
                item == Items.BIRCH_LOG || item == Items.JUNGLE_LOG ||
                item == Items.ACACIA_LOG || item == Items.DARK_OAK_LOG ||
                item == Items.MANGROVE_LOG || item == Items.CHERRY_LOG ||
                item == Items.BAMBOO_BLOCK || // бамбук как бревно
                item == Items.STRIPPED_OAK_LOG || item == Items.STRIPPED_SPRUCE_LOG ||
                item == Items.STRIPPED_BIRCH_LOG || item == Items.STRIPPED_JUNGLE_LOG ||
                item == Items.STRIPPED_ACACIA_LOG || item == Items.STRIPPED_DARK_OAK_LOG ||
                item == Items.STRIPPED_MANGROVE_LOG || item == Items.STRIPPED_CHERRY_LOG ||
                item == Items.STRIPPED_BAMBOO_BLOCK ||
                item == Items.OAK_WOOD || item == Items.SPRUCE_WOOD ||
                item == Items.BIRCH_WOOD || item == Items.JUNGLE_WOOD ||
                item == Items.ACACIA_WOOD || item == Items.DARK_OAK_WOOD ||
                item == Items.MANGROVE_WOOD || item == Items.CHERRY_WOOD ||
                item == Items.STRIPPED_OAK_WOOD || item == Items.STRIPPED_SPRUCE_WOOD ||
                item == Items.STRIPPED_BIRCH_WOOD || item == Items.STRIPPED_JUNGLE_WOOD ||
                item == Items.STRIPPED_ACACIA_WOOD || item == Items.STRIPPED_DARK_OAK_WOOD ||
                item == Items.STRIPPED_MANGROVE_WOOD || item == Items.STRIPPED_CHERRY_WOOD) return 0;
        if (item == Items.BOWL) return 0;
        if (item == Items.OAK_BOAT || item == Items.SPRUCE_BOAT ||
                item == Items.BIRCH_BOAT || item == Items.JUNGLE_BOAT ||
                item == Items.ACACIA_BOAT || item == Items.DARK_OAK_BOAT ||
                item == Items.MANGROVE_BOAT || item == Items.CHERRY_BOAT ||
                item == Items.BAMBOO_RAFT || item == Items.OAK_CHEST_BOAT ||
                item == Items.SPRUCE_CHEST_BOAT || item == Items.BIRCH_CHEST_BOAT ||
                item == Items.JUNGLE_CHEST_BOAT || item == Items.ACACIA_CHEST_BOAT ||
                item == Items.DARK_OAK_CHEST_BOAT || item == Items.MANGROVE_CHEST_BOAT ||
                item == Items.CHERRY_CHEST_BOAT || item == Items.BAMBOO_CHEST_RAFT) return 0;
        if (item == Items.NOTE_BLOCK) return 0;
        if (item == Items.JUKEBOX) return 0;
        if (item == Items.BOOKSHELF) return 0;
        if (item == Items.CHISELED_BOOKSHELF) return 0;
        if (item == Items.COMPOSTER) return 0;
        if (item == Items.BARREL) return 0;
        if (item == Items.CHEST || item == Items.TRAPPED_CHEST) return 0;
        if (item == Items.CRAFTING_TABLE) return 0;
        if (item == Items.FLETCHING_TABLE) return 0;
        if (item == Items.SMITHING_TABLE) return 0;
        if (item == Items.CARTOGRAPHY_TABLE) return 0;
        if (item == Items.LOOM) return 0;
        if (item == Items.ITEM_FRAME) return 0;
        if (item == Items.GLOW_ITEM_FRAME) return 0;
        if (item == Items.PAINTING) return 0;
        if (item == Items.WHITE_BED || item == Items.ORANGE_BED ||
                item == Items.MAGENTA_BED || item == Items.LIGHT_BLUE_BED ||
                item == Items.YELLOW_BED || item == Items.LIME_BED ||
                item == Items.PINK_BED || item == Items.GRAY_BED ||
                item == Items.LIGHT_GRAY_BED || item == Items.CYAN_BED ||
                item == Items.PURPLE_BED || item == Items.BLUE_BED ||
                item == Items.BROWN_BED || item == Items.GREEN_BED ||
                item == Items.RED_BED || item == Items.BLACK_BED) return 0;
        if (item == Items.WOODEN_SWORD || item == Items.WOODEN_PICKAXE ||
                item == Items.WOODEN_AXE || item == Items.WOODEN_SHOVEL ||
                item == Items.WOODEN_HOE) return 0;
        if (item == Items.SHIELD) return 0;
        if (item == Items.BOW) return 0;
        if (item == Items.CROSSBOW) return 0;
        if (item == Items.FISHING_ROD) return 0;
        if (item == Items.CAMPFIRE || item == Items.SOUL_CAMPFIRE) return 0;
        if (item == Items.TORCH || item == Items.SOUL_TORCH ||
                item == Items.REDSTONE_TORCH) return 0;

        // ========== ТИР 1: Обычное топливо ==========
        if (item == Items.COAL || item == Items.CHARCOAL || item == Items.BLAZE_POWDER) return 1;

        // ========== ТИР 2: Blaze rod и мясо ==========
        if (item == Items.BLAZE_ROD || item == Items.MAGMA_CREAM || item == Items.PORKCHOP) return 2;

        // ========== ТИР 3: Блок угля ==========
        if (item == Item.byBlock(Blocks.COAL_BLOCK)) return 3;

        // ========== ТИР 4: Лава ==========
        if (item == Items.LAVA_BUCKET) return 4;

        // ========== ТИР 5: Специальное ==========
        if (item == ModItems.MORY_LAH.get() || item == Items.DRAGON_BREATH) return 5;

        return -1;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ContainerData getData() {
        return data;
    }

    // === ГЕТТЕРЫ С FLOAT ===
    public float getTemperature() { return temperature; }
    public float getTemperatureDisplay() { return temperature; } // Для отображения в GUI
    public int getBurnTime() { return burnTime; }
    public int getTotalBurnTime() { return totalBurnTime; }
    public boolean isBurning() { return burnTime > 0; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putFloat("Temperature", temperature);  // putFloat!
        tag.putInt("BurnTime", burnTime);
        tag.putInt("TotalBurnTime", totalBurnTime);
        tag.putInt("FuelTier", fuelTier);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        temperature = tag.getFloat("Temperature");  // getFloat!
        burnTime = tag.getInt("BurnTime");
        totalBurnTime = tag.getInt("TotalBurnTime");
        fuelTier = tag.getInt("FuelTier");

        // Восстанавливаем данные для GUI
        data.set(DATA_TEMP, (int) (temperature * 10.0f));
        data.set(DATA_BURN_TIME, burnTime);
        data.set(DATA_TOTAL_BURN_TIME, totalBurnTime);
        data.set(DATA_IS_BURNING, burnTime > 0 ? 1 : 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.trd.heater");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HeaterMenu(id, inv, this, data);
    }

    // === СИНХРОНИЗАЦИЯ ===
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("Inventory", inventory.serializeNBT());
        tag.putFloat("Temperature", temperature);  // putFloat!
        tag.putInt("BurnTime", burnTime);
        tag.putInt("TotalBurnTime", totalBurnTime);
        tag.putInt("FuelTier", fuelTier);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        if (tag.contains("Temperature", CompoundTag.TAG_FLOAT)) {  // Проверяем тип FLOAT
            temperature = tag.getFloat("Temperature");
            burnTime = tag.getInt("BurnTime");
            totalBurnTime = tag.getInt("TotalBurnTime");
            fuelTier = tag.getInt("FuelTier");

            // Обновляем данные для GUI/HUD
            data.set(DATA_TEMP, (int) (temperature * 10.0f));
            data.set(DATA_BURN_TIME, burnTime);
            data.set(DATA_TOTAL_BURN_TIME, totalBurnTime);
            data.set(DATA_IS_BURNING, burnTime > 0 ? 1 : 0);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }
}