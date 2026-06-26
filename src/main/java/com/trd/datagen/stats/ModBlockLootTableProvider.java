package com.trd.datagen.stats;

import com.trd.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.RegistryObject;
import com.trd.block.basic.ModBlocks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.item.Item;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    private final Set<Block> exceptions = new HashSet<>();

    // ═══════════════════════════════════════════════════════
    // СПИСОК РУД С ОПЫТОМ — единственная точка конфигурации
    // ═══════════════════════════════════════════════════════

    public record OreXpConfig(
            RegistryObject<Block> block,
            int minXp,
            int maxXp
    ) {}

    /** Руды с _ore — дают опыт при добыче без Silk Touch */
    public static final List<OreXpConfig> ORES_WITH_EXPERIENCE = List.of(
            new OreXpConfig(ModBlocks.LIGNITE_ORE,              2, 5),
            new OreXpConfig(ModBlocks.CINNABAR_ORE,             2, 5),
            new OreXpConfig(ModBlocks.CINNABAR_ORE_DEEPSLATE,   2, 5),
            new OreXpConfig(ModBlocks.SULFUR_ORE,               2, 5),
            new OreXpConfig(ModBlocks.SULFUR_ORE_DEEPSLATE,     2, 5),
            new OreXpConfig(ModBlocks.SEQUESTRUM_ORE,           2, 5),
            new OreXpConfig(ModBlocks.SEQUESTRUM_ORE_DEEPSLATE, 2, 5),
            new OreXpConfig(ModBlocks.FLUORITE_ORE,             2, 5),
            new OreXpConfig(ModBlocks.FLUORITE_ORE_DEEPSLATE,   2, 5),
            new OreXpConfig(ModBlocks.ASBESOTS_ORE,             2, 5)
    );

    // ═══════════════════════════════════════════════════════

    public ModBlockLootTableProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        // Руды с опытом — дроп 1-3 с удачей
        for (OreXpConfig xpConfig : ORES_WITH_EXPERIENCE) {
            registerOreWithMultipleDrops(xpConfig.block, getItemForOre(xpConfig.block));
        }

        // Минералы без опыта — дроп 1-3 с удачей
        registerOreWithMultipleDrops(ModBlocks.BAUXITE, ModItems.BAUXITE_CHUNK);
        registerOreWithMultipleDrops(ModBlocks.LIMESTONE, ModItems.LIMESTONE_CHUNK);
        registerOreWithMultipleDrops(ModBlocks.DOLOMITE, ModItems.DOLOMITE_CHUNK);
        registerOreWithMultipleDrops(ModBlocks.SULFUR_CLUSTER, ModItems.SULFUR);


        // --- ДЕФОЛТ ДЛЯ ВСЕХ ОСТАЛЬНЫХ БЛОКОВ ---
        for (RegistryObject<Block> entry : ModBlocks.BLOCKS.getEntries()) {
            Block block = entry.get();
            if (exceptions.contains(block)) continue;
            if (block == ModBlocks.BEAM_COLLISION.get() || block == ModBlocks.MULTIBLOCK_PART.get() || block == ModBlocks.PIPE_SPOTS.get()) continue;

            this.dropSelf(block);
        }
    }

    // Хелпер: получаем предмет-дроп по блоку (для руд из списка)
    private RegistryObject<Item> getItemForOre(RegistryObject<Block> block) {
        // Маппинг блок→предмет (можно сделать красивее, но для 10 руд — ок)
        if (block == ModBlocks.LIGNITE_ORE) return ModItems.LIGNITE;
        if (block == ModBlocks.CINNABAR_ORE || block == ModBlocks.CINNABAR_ORE_DEEPSLATE) return ModItems.CINNABAR;
        if (block == ModBlocks.SULFUR_ORE || block == ModBlocks.SULFUR_ORE_DEEPSLATE || block == ModBlocks.SULFUR_CLUSTER) return ModItems.SULFUR;
        if (block == ModBlocks.SEQUESTRUM_ORE || block == ModBlocks.SEQUESTRUM_ORE_DEEPSLATE) return ModItems.SEQUESTRUM;
        if (block == ModBlocks.FLUORITE_ORE || block == ModBlocks.FLUORITE_ORE_DEEPSLATE) return ModItems.FLUORITE;
        if (block == ModBlocks.ASBESOTS_ORE) return ModItems.ASBESTOS;
        return ModItems.LIGNITE; // fallback
    }

    /**
     * Спавнит опыт для руд из списка ORES_WITH_EXPERIENCE.
     * Использовать при кастомной добыче (литые кирки, взрывы и т.д.),
     * т.к. BlockEvent.BreakEvent при этом не вызывается.
     */
    public static void spawnOreExperience(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        for (OreXpConfig config : ORES_WITH_EXPERIENCE) {
            if (config.block.get() == block) {
                int xp = level.random.nextIntBetweenInclusive(config.minXp, config.maxXp);
                block.popExperience(level, pos, xp);
                return;
            }
        }
    }

    // Новый метод для руд из конфига (с опытом)
    private void registerOreWithMultipleDrops(RegistryObject<Block> block, RegistryObject<Item> item) {
        this.add(block.get(), createSilkTouchDispatchTable(block.get(),
                this.applyExplosionDecay(block.get(),
                        LootItem.lootTableItem(item.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))
                )));
        exceptions.add(block.get());
    }

    private void registerOre1(RegistryObject<Block> block, RegistryObject<Item> item) {
        this.add(block.get(), createOreDrop(block.get(), item.get()));
        exceptions.add(block.get());
    }

    private void registerCustomDrop(RegistryObject<Block> block, RegistryObject<Item> drop) {
        this.add(block.get(), createSingleItemTable(drop.get()));
        exceptions.add(block.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .map(RegistryObject::get)
                .filter(block -> block != ModBlocks.BEAM_COLLISION.get() && block != ModBlocks.MULTIBLOCK_PART.get() && block != ModBlocks.PIPE_SPOTS.get())
                .collect(Collectors.toList());
    }
}