package com.trd.multiblock.industrial;

import com.trd.block.basic.ModBlocks;
import com.trd.block.entity.ModBlockEntities;
import com.trd.item.ModItems;
import com.trd.menu.industrial.HeaterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
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
import java.util.*;

public class HeaterBlockEntity extends BlockEntity implements MenuProvider {

    // ═══════════════════════════════════════════════════════
    // СТРУКТУРА ДАННЫХ О ТОПЛИВЕ (единый источник правды)
    // ═══════════════════════════════════════════════════════

    public record FuelTierInfo(
            int tier,
            float heatPerTick,
            int burnTicks,
            int ashChance,
            Component displayName
    ) {
        public int getBurnSeconds() {
            return burnTicks / 20;
        }
    }

    private static final List<FuelTierInfo> TIER_INFOS = List.of(
            new FuelTierInfo(0, 1f, 125, 0,
                    Component.translatable("gui.trd.heater.tier0")),
            new FuelTierInfo(1, 2f, 250, 0,
                    Component.translatable("gui.trd.heater.tier1")),
            new FuelTierInfo(2, 3f, 800, 40,
                    Component.translatable("gui.trd.heater.tier2")),
            new FuelTierInfo(3, 4f, 2400, 60,
                    Component.translatable("gui.trd.heater.tier3")),
            new FuelTierInfo(4, 5f, 3600, 80,
                    Component.translatable("gui.trd.heater.tier4")),
            new FuelTierInfo(5, 6f, 4800, 100,
                    Component.translatable("gui.trd.heater.tier5"))
    );

    // Предметы по тирам — единый источник правды для GUI и логики
    @SuppressWarnings("unchecked")
    private static final List<ItemStack>[] FUEL_ITEMS_BY_TIER = new List[6];

    static {
        // ========== ТИР 0: Дешёвое деревянное топливо ==========
        FUEL_ITEMS_BY_TIER[0] = Arrays.asList(
                new ItemStack(Items.STICK), new ItemStack(Items.SCAFFOLDING),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.SPRUCE_PLANKS),
                new ItemStack(Items.BIRCH_PLANKS), new ItemStack(Items.JUNGLE_PLANKS),
                new ItemStack(Items.ACACIA_PLANKS), new ItemStack(Items.DARK_OAK_PLANKS),
                new ItemStack(Items.MANGROVE_PLANKS), new ItemStack(Items.CHERRY_PLANKS),
                new ItemStack(Items.BAMBOO_PLANKS), new ItemStack(Items.BAMBOO_MOSAIC),
                new ItemStack(Items.OAK_SLAB), new ItemStack(Items.SPRUCE_SLAB),
                new ItemStack(Items.BIRCH_SLAB), new ItemStack(Items.JUNGLE_SLAB),
                new ItemStack(Items.ACACIA_SLAB), new ItemStack(Items.DARK_OAK_SLAB),
                new ItemStack(Items.MANGROVE_SLAB), new ItemStack(Items.CHERRY_SLAB),
                new ItemStack(Items.BAMBOO_SLAB), new ItemStack(Items.BAMBOO_MOSAIC_SLAB),
                new ItemStack(Items.OAK_STAIRS), new ItemStack(Items.SPRUCE_STAIRS),
                new ItemStack(Items.BIRCH_STAIRS), new ItemStack(Items.JUNGLE_STAIRS),
                new ItemStack(Items.ACACIA_STAIRS), new ItemStack(Items.DARK_OAK_STAIRS),
                new ItemStack(Items.MANGROVE_STAIRS), new ItemStack(Items.CHERRY_STAIRS),
                new ItemStack(Items.BAMBOO_STAIRS), new ItemStack(Items.BAMBOO_MOSAIC_STAIRS),
                new ItemStack(Items.OAK_FENCE), new ItemStack(Items.SPRUCE_FENCE),
                new ItemStack(Items.BIRCH_FENCE), new ItemStack(Items.JUNGLE_FENCE),
                new ItemStack(Items.ACACIA_FENCE), new ItemStack(Items.DARK_OAK_FENCE),
                new ItemStack(Items.MANGROVE_FENCE), new ItemStack(Items.CHERRY_FENCE),
                new ItemStack(Items.BAMBOO_FENCE),
                new ItemStack(Items.OAK_FENCE_GATE), new ItemStack(Items.SPRUCE_FENCE_GATE),
                new ItemStack(Items.BIRCH_FENCE_GATE), new ItemStack(Items.JUNGLE_FENCE_GATE),
                new ItemStack(Items.ACACIA_FENCE_GATE), new ItemStack(Items.DARK_OAK_FENCE_GATE),
                new ItemStack(Items.MANGROVE_FENCE_GATE), new ItemStack(Items.CHERRY_FENCE_GATE),
                new ItemStack(Items.BAMBOO_FENCE_GATE),
                new ItemStack(Items.OAK_DOOR), new ItemStack(Items.SPRUCE_DOOR),
                new ItemStack(Items.BIRCH_DOOR), new ItemStack(Items.JUNGLE_DOOR),
                new ItemStack(Items.ACACIA_DOOR), new ItemStack(Items.DARK_OAK_DOOR),
                new ItemStack(Items.MANGROVE_DOOR), new ItemStack(Items.CHERRY_DOOR),
                new ItemStack(Items.BAMBOO_DOOR),
                new ItemStack(Items.OAK_TRAPDOOR), new ItemStack(Items.SPRUCE_TRAPDOOR),
                new ItemStack(Items.BIRCH_TRAPDOOR), new ItemStack(Items.JUNGLE_TRAPDOOR),
                new ItemStack(Items.ACACIA_TRAPDOOR), new ItemStack(Items.DARK_OAK_TRAPDOOR),
                new ItemStack(Items.MANGROVE_TRAPDOOR), new ItemStack(Items.CHERRY_TRAPDOOR),
                new ItemStack(Items.BAMBOO_TRAPDOOR),
                new ItemStack(Items.OAK_BUTTON), new ItemStack(Items.SPRUCE_BUTTON),
                new ItemStack(Items.BIRCH_BUTTON), new ItemStack(Items.JUNGLE_BUTTON),
                new ItemStack(Items.ACACIA_BUTTON), new ItemStack(Items.DARK_OAK_BUTTON),
                new ItemStack(Items.MANGROVE_BUTTON), new ItemStack(Items.CHERRY_BUTTON),
                new ItemStack(Items.BAMBOO_BUTTON),
                new ItemStack(Items.OAK_PRESSURE_PLATE), new ItemStack(Items.SPRUCE_PRESSURE_PLATE),
                new ItemStack(Items.BIRCH_PRESSURE_PLATE), new ItemStack(Items.JUNGLE_PRESSURE_PLATE),
                new ItemStack(Items.ACACIA_PRESSURE_PLATE), new ItemStack(Items.DARK_OAK_PRESSURE_PLATE),
                new ItemStack(Items.MANGROVE_PRESSURE_PLATE), new ItemStack(Items.CHERRY_PRESSURE_PLATE),
                new ItemStack(Items.BAMBOO_PRESSURE_PLATE),
                new ItemStack(Items.OAK_SIGN), new ItemStack(Items.SPRUCE_SIGN),
                new ItemStack(Items.BIRCH_SIGN), new ItemStack(Items.JUNGLE_SIGN),
                new ItemStack(Items.ACACIA_SIGN), new ItemStack(Items.DARK_OAK_SIGN),
                new ItemStack(Items.MANGROVE_SIGN), new ItemStack(Items.CHERRY_SIGN),
                new ItemStack(Items.BAMBOO_SIGN), new ItemStack(Items.OAK_HANGING_SIGN),
                new ItemStack(Items.SPRUCE_HANGING_SIGN), new ItemStack(Items.BIRCH_HANGING_SIGN),
                new ItemStack(Items.JUNGLE_HANGING_SIGN), new ItemStack(Items.ACACIA_HANGING_SIGN),
                new ItemStack(Items.DARK_OAK_HANGING_SIGN), new ItemStack(Items.MANGROVE_HANGING_SIGN),
                new ItemStack(Items.CHERRY_HANGING_SIGN), new ItemStack(Items.BAMBOO_HANGING_SIGN),
                new ItemStack(Items.OAK_LOG), new ItemStack(Items.SPRUCE_LOG),
                new ItemStack(Items.BIRCH_LOG), new ItemStack(Items.JUNGLE_LOG),
                new ItemStack(Items.ACACIA_LOG), new ItemStack(Items.DARK_OAK_LOG),
                new ItemStack(Items.MANGROVE_LOG), new ItemStack(Items.CHERRY_LOG),
                new ItemStack(Items.BAMBOO_BLOCK), new ItemStack(Items.STRIPPED_BAMBOO_BLOCK),
                new ItemStack(Items.STRIPPED_OAK_LOG), new ItemStack(Items.STRIPPED_SPRUCE_LOG),
                new ItemStack(Items.STRIPPED_BIRCH_LOG), new ItemStack(Items.STRIPPED_JUNGLE_LOG),
                new ItemStack(Items.STRIPPED_ACACIA_LOG), new ItemStack(Items.STRIPPED_DARK_OAK_LOG),
                new ItemStack(Items.STRIPPED_MANGROVE_LOG), new ItemStack(Items.STRIPPED_CHERRY_LOG),
                new ItemStack(Items.OAK_WOOD), new ItemStack(Items.SPRUCE_WOOD),
                new ItemStack(Items.BIRCH_WOOD), new ItemStack(Items.JUNGLE_WOOD),
                new ItemStack(Items.ACACIA_WOOD), new ItemStack(Items.DARK_OAK_WOOD),
                new ItemStack(Items.MANGROVE_WOOD), new ItemStack(Items.CHERRY_WOOD),
                new ItemStack(Items.STRIPPED_OAK_WOOD), new ItemStack(Items.STRIPPED_SPRUCE_WOOD),
                new ItemStack(Items.STRIPPED_BIRCH_WOOD), new ItemStack(Items.STRIPPED_JUNGLE_WOOD),
                new ItemStack(Items.STRIPPED_ACACIA_WOOD), new ItemStack(Items.STRIPPED_DARK_OAK_WOOD),
                new ItemStack(Items.STRIPPED_MANGROVE_WOOD), new ItemStack(Items.STRIPPED_CHERRY_WOOD),
                new ItemStack(Items.BOWL), new ItemStack(Items.NOTE_BLOCK),
                new ItemStack(Items.JUKEBOX), new ItemStack(Items.BOOKSHELF),
                new ItemStack(Items.CHISELED_BOOKSHELF), new ItemStack(Items.COMPOSTER),
                new ItemStack(Items.BARREL), new ItemStack(Items.CRAFTING_TABLE),
                new ItemStack(Items.CHEST), new ItemStack(Items.TRAPPED_CHEST),
                new ItemStack(Items.OAK_BOAT), new ItemStack(Items.SPRUCE_BOAT),
                new ItemStack(Items.BIRCH_BOAT), new ItemStack(Items.JUNGLE_BOAT),
                new ItemStack(Items.ACACIA_BOAT), new ItemStack(Items.DARK_OAK_BOAT),
                new ItemStack(Items.MANGROVE_BOAT), new ItemStack(Items.CHERRY_BOAT),
                new ItemStack(Items.BAMBOO_RAFT), new ItemStack(Items.OAK_CHEST_BOAT),
                new ItemStack(Items.SPRUCE_CHEST_BOAT), new ItemStack(Items.BIRCH_CHEST_BOAT),
                new ItemStack(Items.JUNGLE_CHEST_BOAT), new ItemStack(Items.ACACIA_CHEST_BOAT),
                new ItemStack(Items.DARK_OAK_CHEST_BOAT), new ItemStack(Items.MANGROVE_CHEST_BOAT),
                new ItemStack(Items.CHERRY_CHEST_BOAT), new ItemStack(Items.BAMBOO_CHEST_RAFT),
                new ItemStack(Items.FLETCHING_TABLE), new ItemStack(Items.SMITHING_TABLE),
                new ItemStack(Items.CARTOGRAPHY_TABLE), new ItemStack(Items.LOOM),
                new ItemStack(Items.ITEM_FRAME), new ItemStack(Items.GLOW_ITEM_FRAME),
                new ItemStack(Items.PAINTING),
                new ItemStack(Items.WHITE_BED), new ItemStack(Items.ORANGE_BED),
                new ItemStack(Items.MAGENTA_BED), new ItemStack(Items.LIGHT_BLUE_BED),
                new ItemStack(Items.YELLOW_BED), new ItemStack(Items.LIME_BED),
                new ItemStack(Items.PINK_BED), new ItemStack(Items.GRAY_BED),
                new ItemStack(Items.LIGHT_GRAY_BED), new ItemStack(Items.CYAN_BED),
                new ItemStack(Items.PURPLE_BED), new ItemStack(Items.BLUE_BED),
                new ItemStack(Items.BROWN_BED), new ItemStack(Items.GREEN_BED),
                new ItemStack(Items.RED_BED), new ItemStack(Items.BLACK_BED),
                new ItemStack(Items.WOODEN_SWORD), new ItemStack(Items.WOODEN_PICKAXE),
                new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WOODEN_SHOVEL),
                new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.SHIELD),
                new ItemStack(Items.BOW), new ItemStack(Items.CROSSBOW),
                new ItemStack(Items.FISHING_ROD),
                new ItemStack(Items.CAMPFIRE), new ItemStack(Items.SOUL_CAMPFIRE),
                new ItemStack(Items.TORCH), new ItemStack(Items.SOUL_TORCH),
                new ItemStack(Items.REDSTONE_TORCH),
                new ItemStack(ModItems.FUEL_ASH.get()),
                new ItemStack(ModItems.ROPE.get()),
                new ItemStack(ModItems.WOODEN_HANDLE.get()),
                new ItemStack(Item.byBlock(ModBlocks.SEQUOIA_BARK.get())),
                new ItemStack(Item.byBlock(ModBlocks.SEQUOIA_HEARTWOOD.get()))
        );

        // ========== ТИР 1: Обычное топливо ==========
        FUEL_ITEMS_BY_TIER[1] = Arrays.asList(
                new ItemStack(Items.COAL),
                new ItemStack(Items.CHARCOAL),
                new ItemStack(Items.BLAZE_POWDER),
                new ItemStack(ModItems.LIGNITE.get())
        );

        // ========== ТИР 2: Blaze rod и прочее ==========
        FUEL_ITEMS_BY_TIER[2] = Arrays.asList(
                new ItemStack(Items.BLAZE_ROD),
                new ItemStack(Items.MAGMA_CREAM),
                new ItemStack(Items.PORKCHOP)
        );

        // ========== ТИР 3: Блок угля ==========
        FUEL_ITEMS_BY_TIER[3] = Arrays.asList(
                new ItemStack(Items.COAL_BLOCK),
                new ItemStack(Item.byBlock(Blocks.MAGMA_BLOCK))
        );

        // ========== ТИР 4: Лава ==========
        FUEL_ITEMS_BY_TIER[4] = Arrays.asList(
                new ItemStack(Items.LAVA_BUCKET)
        );

        // ========== ТИР 5: Специальное ==========
        FUEL_ITEMS_BY_TIER[5] = Arrays.asList(
                new ItemStack(Items.DRAGON_BREATH)
        );
    }

    // Быстрый поиск тира по предмету (для getFuelTier)
    private static final Map<Item, Integer> ITEM_TO_TIER_MAP = new IdentityHashMap<>();

    static {
        for (int tier = 0; tier < FUEL_ITEMS_BY_TIER.length; tier++) {
            List<ItemStack> items = FUEL_ITEMS_BY_TIER[tier];
            if (items != null) {
                for (ItemStack stack : items) {
                    ITEM_TO_TIER_MAP.put(stack.getItem(), tier);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // ПУБЛИЧНЫЕ API ДЛЯ GUI
    // ═══════════════════════════════════════════════════════

    /** Возвращает информацию о тире по номеру */
    public static FuelTierInfo getTierInfo(int tier) {
        if (tier < 0 || tier >= TIER_INFOS.size()) return null;
        return TIER_INFOS.get(tier);
    }

    /** Возвращает список предметов для указанного тира */
    public static List<ItemStack> getFuelItemsForTier(int tier) {
        if (tier < 0 || tier >= FUEL_ITEMS_BY_TIER.length) return Collections.emptyList();
        List<ItemStack> list = FUEL_ITEMS_BY_TIER[tier];
        return list != null ? list : Collections.emptyList();
    }

    /** Возвращает количество тиров */
    public static int getTierCount() {
        return TIER_INFOS.size();
    }

    /** Возвращает список всех тиров */
    public static List<FuelTierInfo> getAllTierInfos() {
        return TIER_INFOS;
    }

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
    private static final int[] ASH_CHANCES = {0, 0, 40, 60, 80, 100};

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
            {3f, 800},
            {4f, 2400},
            {5f, 3600},
            {6f, 4800}
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
        return ITEM_TO_TIER_MAP.getOrDefault(stack.getItem(), -1);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ContainerData getData() {
        return data;
    }

    // === ГЕТТЕРЫ С FLOAT ===
    public float getTemperature() { return temperature; }
    public float getTemperatureDisplay() { return temperature; }
    public int getBurnTime() { return burnTime; }
    public int getTotalBurnTime() { return totalBurnTime; }
    public boolean isBurning() { return burnTime > 0; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putFloat("Temperature", temperature);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("TotalBurnTime", totalBurnTime);
        tag.putInt("FuelTier", fuelTier);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        temperature = tag.getFloat("Temperature");
        burnTime = tag.getInt("BurnTime");
        totalBurnTime = tag.getInt("TotalBurnTime");
        fuelTier = tag.getInt("FuelTier");

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
        tag.putFloat("Temperature", temperature);
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
        if (tag.contains("Temperature", CompoundTag.TAG_FLOAT)) {
            temperature = tag.getFloat("Temperature");
            burnTime = tag.getInt("BurnTime");
            totalBurnTime = tag.getInt("TotalBurnTime");
            fuelTier = tag.getInt("FuelTier");

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