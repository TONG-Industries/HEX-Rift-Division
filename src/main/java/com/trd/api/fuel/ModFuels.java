package com.trd.api.fuel;

import com.trd.block.basic.ModBlocks;
import com.trd.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Реестр топлива мода TONG: Rift Division.
 *
 * Для справки: 1 уголь = 1600 тиков (~80 секунд).
 */
public class ModFuels {

    // --- Регистрация (вызывается в статическом блоке) ---
    public static final Map<RegistryObject<Item>, Integer> ITEM_FUELS = new HashMap<>();
    public static final Map<RegistryObject<Block>, Integer> BLOCK_FUELS = new HashMap<>();

    static {
        // ============ ПРЕДМЕТЫ ============
        ITEM_FUELS.put(ModItems.LIGNITE, 1100);
        ITEM_FUELS.put(ModItems.WOODEN_HANDLE, 1000);
        ITEM_FUELS.put(ModItems.FUEL_ASH, 200);
        ITEM_FUELS.put(ModItems.ROPE, 200);

        // ============ БЛОКИ ============
        BLOCK_FUELS.put(ModBlocks.LIGNITE_BLOCK, 9900);
        BLOCK_FUELS.put(ModBlocks.SEQUOIA_PLANKS, 300);
        BLOCK_FUELS.put(ModBlocks.SEQUOIA_BARK, 300);
        BLOCK_FUELS.put(ModBlocks.SEQUOIA_HEARTWOOD, 300);
    }

    // --- Быстрый lookup для событий (строится автоматически) ---
    private static final Map<Item, Integer> ITEM_LOOKUP;
    private static final Map<Item, Integer> BLOCK_AS_ITEM_LOOKUP;

    static {
        Map<Item, Integer> itemMap = new HashMap<>();
        for (var entry : ITEM_FUELS.entrySet()) {
            itemMap.put(entry.getKey().get(), entry.getValue());
        }
        ITEM_LOOKUP = Collections.unmodifiableMap(itemMap);

        Map<Item, Integer> blockItemMap = new HashMap<>();
        for (var entry : BLOCK_FUELS.entrySet()) {
            blockItemMap.put(entry.getKey().get().asItem(), entry.getValue());
        }
        BLOCK_AS_ITEM_LOOKUP = Collections.unmodifiableMap(blockItemMap);
    }

    // ==================== ТАБЛИЦА ВАНИЛЬНЫХ ПРЕДМЕТОВ ====================

    /**
     * Справочная таблица эффективности ванильных предметов.
     *
     * <pre>
     * ┌─────────────────────────────┬──────────┐
     * │ Предмет                     │ Тики     │
     * ├─────────────────────────────┼──────────┤
     * │ LAVA_BUCKET                 │ 20000    │
     * │ BLOCK_OF_COAL               │ 16000    │
     * │ DRIED_KELP_BLOCK            │ 4000     │
     * │ BLAZE_ROD                   │ 2400     │
     * │ COAL / CHARCOAL             │ 1600     │
     * │ BOAT                        │ 1200     │
     * │ SCAFFOLDING                 │ 400      │
     * │ WOODEN_PRESSURE_PLATE       │ 300      │
     * │ WOODEN_BUTTON               │ 100      │
     * │ STICK / SAPLING             │ 100      │
     * │ BOWL                        │ 100      │
     * │ CARPET                      │ 67       │
     * │ BAMBOO                      │ 50       │
     * └─────────────────────────────┴──────────┘
     * </pre>
     */

    // ==================== УТИЛИТЫ ====================

    /**
     * Быстрый lookup для события FurnaceFuelBurnTimeEvent.
     * O(1) вместо O(n).
     *
     * @param item предмет из стака
     * @return тики горения, или -1 если предмет не является топливом мода
     */
    public static int getBurnTimeForItem(Item item) {
        Integer time = ITEM_LOOKUP.get(item);
        if (time != null) return time;

        time = BLOCK_AS_ITEM_LOOKUP.get(item);
        if (time != null) return time;

        return -1; // не наше топливо — пусть Forge решает сам
    }

    public static int getBurnTime(RegistryObject<Item> itemRegistryObj) {
        return ITEM_FUELS.getOrDefault(itemRegistryObj, 0);
    }

    public static int getBlockBurnTime(RegistryObject<Block> blockRegistryObj) {
        return BLOCK_FUELS.getOrDefault(blockRegistryObj, 0);
    }

    public static boolean isFuel(RegistryObject<Item> itemRegistryObj) {
        return ITEM_FUELS.containsKey(itemRegistryObj);
    }

    public static boolean isBlockFuel(RegistryObject<Block> blockRegistryObj) {
        return BLOCK_FUELS.containsKey(blockRegistryObj);
    }

    /**
     * Возвращает строку эффективности относительно угля.
     * Используйте только после проверки isFuel() / isBlockFuel()!
     */
    public static String getEfficiencyComparedToCoal(int burnTime) {
        double ratio = (double) burnTime / 1600.0;
        return String.format("%.2f×Coal", ratio);
    }
}