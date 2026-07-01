package com.trd.worldgen.feature;

import com.trd.block.basic.ModBlocks;
import com.trd.main.MainRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class OreVeinRegistry {
    public static final List<OreEntry> ORES = new ArrayList<>();

    public static class OreEntry {
        public final String name;
        public final Block block;
        public final int veinSize;      // размер жилы
        public final int minY;          // нижняя граница
        public final int maxY;          // верхняя граница
        public final int countPerChunk; // сколько попыток на чанк

        public final ResourceKey<ConfiguredFeature<?, ?>> configuredKey;
        public final ResourceKey<PlacedFeature> placedKey;
        public final ResourceKey<BiomeModifier> biomeModifierKey;

        public OreEntry(String name, Block block, int veinSize, int minY, int maxY, int countPerChunk) {
            this.name = name;
            this.block = block;
            this.veinSize = veinSize;
            this.minY = minY;
            this.maxY = maxY;
            this.countPerChunk = countPerChunk;

            this.configuredKey = ResourceKey.create(
                    net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
                    new ResourceLocation(MainRegistry.MOD_ID, "ore_" + name)
            );
            this.placedKey = ResourceKey.create(
                    net.minecraft.core.registries.Registries.PLACED_FEATURE,
                    new ResourceLocation(MainRegistry.MOD_ID, "ore_" + name + "_placed")
            );
            this.biomeModifierKey = ResourceKey.create(
                    ForgeRegistries.Keys.BIOME_MODIFIERS,
                    new ResourceLocation(MainRegistry.MOD_ID, "add_ore_" + name)
            );
        }
    }

    public static final List<SpecialOreEntry> SPECIAL_ORES = new ArrayList<>();

    public static class SpecialOreEntry {
        public final String name;
        public final Block block;
        public final int minY, maxY;
        public final int minSize;      // размер на нижней границе
        public final int maxSize;      // размер на верхней границе
        public final int rarity;       // раз в N чанков (1 = каждый чанк)
        public final boolean respectAir;
        public final float density;    // 0.0 – 1.0

        public final ResourceKey<ConfiguredFeature<?, ?>> configuredKey;
        public final ResourceKey<PlacedFeature> placedKey;
        public final ResourceKey<BiomeModifier> biomeModifierKey;

        public SpecialOreEntry(String name, Block block, int minY, int maxY,
                               int minSize, int maxSize, int rarity,
                               boolean respectAir, float density) {
            this.name = name;
            this.block = block;
            this.minY = minY;
            this.maxY = maxY;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.rarity = rarity;
            this.respectAir = respectAir;
            this.density = density;

            this.configuredKey = ResourceKey.create(
                    net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
                    new ResourceLocation(MainRegistry.MOD_ID, "special_ore_" + name)
            );
            this.placedKey = ResourceKey.create(
                    net.minecraft.core.registries.Registries.PLACED_FEATURE,
                    new ResourceLocation(MainRegistry.MOD_ID, "special_ore_" + name + "_placed")
            );
            this.biomeModifierKey = ResourceKey.create(
                    ForgeRegistries.Keys.BIOME_MODIFIERS,
                    new ResourceLocation(MainRegistry.MOD_ID, "add_special_ore_" + name)
            );
        }
    }

    public static void registerSpecial(String name, Block block, int minY, int maxY,
                                       int minSize, int maxSize, int rarity,
                                       boolean respectAir, float density) {
        SPECIAL_ORES.add(new SpecialOreEntry(name, block, minY, maxY, minSize, maxSize, rarity, respectAir, density));
    }

    public static void register(String name, Block block, int veinSize, int minY, int maxY, int countPerChunk) {
        ORES.add(new OreEntry(name, block, veinSize, minY, maxY, countPerChunk));
    }

    public static final List<ConglomerateEntry> CONGLOMERATES = new ArrayList<>();

    public static class ConglomerateEntry {
        public final String name;
        public final int minY, maxY;
        public final int minSize, maxSize;
        public final int rarity;
        public final float density;
        public final float depletionChance;

        public final ResourceKey<ConfiguredFeature<?, ?>> configuredKey;
        public final ResourceKey<PlacedFeature> placedKey;
        public final ResourceKey<BiomeModifier> biomeModifierKey;

        public ConglomerateEntry(String name, int minY, int maxY, int minSize, int maxSize,
                                 int rarity, float density, float depletionChance) {
            this.name = name;
            this.minY = minY;
            this.maxY = maxY;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.rarity = rarity;
            this.density = density;
            this.depletionChance = depletionChance;

            this.configuredKey = ResourceKey.create(
                    net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
                    new ResourceLocation(MainRegistry.MOD_ID, "conglomerate_" + name)
            );
            this.placedKey = ResourceKey.create(
                    net.minecraft.core.registries.Registries.PLACED_FEATURE,
                    new ResourceLocation(MainRegistry.MOD_ID, "conglomerate_" + name + "_placed")
            );
            this.biomeModifierKey = ResourceKey.create(
                    ForgeRegistries.Keys.BIOME_MODIFIERS,
                    new ResourceLocation(MainRegistry.MOD_ID, "add_conglomerate_" + name)
            );
        }
    }

    public static void registerConglomerate(String name, int minY, int maxY, int minSize, int maxSize,
                                            int rarity, float density, float depletionChance) {
        CONGLOMERATES.add(new ConglomerateEntry(name, minY, maxY, minSize, maxSize, rarity, density, depletionChance));
    }

    static {

        register("asbestos_ore", ModBlocks.ASBESOTS_ORE.get(), 9, 0, 150, 6);
        register("lignite_ore", ModBlocks.LIGNITE_ORE.get(), 18, 0, 150, 6);

        register("sulfur_ore", ModBlocks.SULFUR_ORE.get(), 9, 0, 150, 2);
        register("sulfur_ore_deepslate", ModBlocks.SULFUR_ORE_DEEPSLATE.get(), 9, -64, 0, 2);

        register("cinnabar_ore", ModBlocks.CINNABAR_ORE.get(), 9, 0, 150, 2);
        register("cinnabar_ore_deepslate", ModBlocks.CINNABAR_ORE_DEEPSLATE.get(), 9, -64, 0, 2);

        register("fluorite_ore", ModBlocks.FLUORITE_ORE.get(), 9, 0, 150, 2);
        register("fluorite_ore_deepslate", ModBlocks.FLUORITE_ORE_DEEPSLATE.get(), 9, -64, 0, 2);

        register("sequestrum_ore", ModBlocks.SEQUESTRUM_ORE.get(), 9, 0, 150, 2);
        register("sequestrum_ore_deepslate", ModBlocks.SEQUESTRUM_ORE_DEEPSLATE.get(), 9, -64, 0, 2);


        // --- спец-залежи ---
            registerSpecial("bauxite", ModBlocks.BAUXITE.get(), -64, 150, 15, 25, 30, true, 0.7f);

            registerSpecial("limestone", ModBlocks.LIMESTONE.get(), -20, 150, 6, 10, 8, false, 0.9f);

            registerSpecial("dolomite", ModBlocks.DOLOMITE.get(), -64, 70, 5, 8, 4, true, 0.6f);

            registerSpecial("sulfur_cluster", ModBlocks.SULFUR_CLUSTER.get(), -64, 150, 5, 8, 10, true, 0.8f);

        // === КОНГЛОМЕРАТЫ ===
        registerConglomerate("surface", 40, 150, 6, 11, 10, 0.65f, 0.3f);
        registerConglomerate("medium", -20, 40, 5, 9, 14, 0.7f, 0.4f);
        registerConglomerate("deep", -64, -20, 4, 7, 18, 0.85f, 0.5f);
    }

    }
