package com.trd.datagen;

import com.trd.main.ResourceRegistry;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import com.trd.main.MainRegistry;
import com.trd.block.basic.ModBlocks;
import com.trd.item.ModItems;

import javax.annotation.Nullable;

public class ModLangProvider extends LanguageProvider {

    protected final String locale;

    public ModLangProvider(PackOutput output, String locale) {
        super(output, MainRegistry.MOD_ID, locale);
        this.locale = locale;

        // !!! ВАЖНО: Инициализируем ResourceRegistry !!!
        ResourceRegistry.init();
    }

    /**
     * Универсальная регистрация перевода для жидкости, её капли и ключа fluid.*
     * @param fluidId   короткое имя жидкости (например "hydrogen_peroxide")
     * @param nameRu    перевод на русский
     * @param nameUa    перевод на украинский (можно null)
     * @param nameEn    перевод на английский (можно null, но тогда будет пропущен)
     */
    private void addFluidTranslations(String fluidId, String nameRu, @Nullable String nameUa, @Nullable String nameEn) {
        switch (locale) {
            case "ru_ru":
                add("fluid_type.trd." + fluidId, nameRu);
                add("fluid.trd." + fluidId, nameRu);
                add("item.trd.fluid_drop_" + fluidId, nameRu);
                break;
            case "en_us":
                if (nameEn != null) {
                    add("fluid_type.trd." + fluidId, nameEn);
                    add("fluid.trd." + fluidId, nameEn);
                    add("item.trd.fluid_drop_" + fluidId, nameEn);
                }
                break;
            // другие локали можно добавить аналогично
        }
    }

    @Override
    protected void addTranslations() {
        // Сначала автоматические переводы для ресурсов
        ResourceDatagenHelper.generateTranslations(this, locale);

        // Затем ручные переводы
        if (locale.equals("ru_ru")) {
            addRussian();
        } else {
            addEnglish();
        }
    }

    private void addEnglish() {
        // Creative Tabs
        add("itemGroup.trd.trd_build_tab", "Building Blocks");
        add("itemGroup.trd.trd_tech_tab", "Technology");
        add("itemGroup.trd.trd_weapons_tab", "Arsenal");
        add("itemGroup.trd.trd_recourses_tab", "Resources");
        add("itemGroup.trd.trd_nature_tab", "Nature");


        // Tooltips & Messages
        add("tooltip.trd.detminer.desc", "Breaks blocks in a natural blast pattern");
        add("tooltip.trd.detminer.hardness", "Only affects blocks with hardness < 30");
        add("tooltip.trd.detminer.conglomerate", "Has a chance to extract resources from conglomerate");
        add("item.trd.fluid_identifier", "Fluid Identifier");
        add("message.trd.selected_fluid", "Selected");
        add("tooltip.trd.no_fluid", "No fluid selected");
        add("tooltip.trd.shaft_material", "Material");
        add("tooltip.trd.max_speed", "Max Speed");
        add("tooltip.trd.max_torque", "Max Torque");
        add("tooltip.trd.inertia", "Inertia");
        add("message.trd.too_far_from_support", "Unsupported span! Max distance from support for this diameter: %s blocks.");

        // Heater Tiers
        add("gui.trd.heater.tier0", "Tier 0");
        add("gui.trd.heater.tier1", "Tier I");
        add("gui.trd.heater.tier2", "Tier II");
        add("gui.trd.heater.tier3", "Tier III");
        add("gui.trd.heater.tier4", "Tier IV");
        add("gui.trd.heater.tier5", "Tier V");

        // Fluids
        addFluidTranslations("hydrogen_peroxide", "Пероксид водорода", null, "Hydrogen Peroxide");
        addFluidTranslations("sulfuric_acid", "Серная кислота", null, "Sulfuric Acid");
        addFluidTranslations("natural_gas", "Природный газ", null, "Natural Gas");
        addFluidTranslations("steam", "Пар", null, "Steam");
        addFluidTranslations("low_pressure_steam", "Пар низкого давления", null, "Low Pressure Steam");
        addFluidTranslations("water", "Вода", "Вода", "Water");
        addFluidTranslations("lava", "Лава", "Лава", "Lava");

        // JEI Categories
        add("jei.category.trd.smelting", "Smelting");
        add("jei.category.trd.casting", "Casting");
        add("jei.category.trd.alloying", "Alloying");
        add("jei.category.trd.millstone", "Millstone");
        add("jei.category.trd.boiling", "Boiler");
        add("jei.category.trd.steam_engine", "Steam Engine");
        add("jei.category.trd.condensing", "Condensing");
        add("jei.category.trd.electric_furnace", "Electric Furnace");

        // Metals
        add("metal.trd.gold", "Gold");
        add("metal.trd.iron", "Iron");
        add("metal.trd.copper", "Copper");
        add("metal.trd.netherite", "Netherite");
        add("metal.trd.steel", "Steel");
        add("metal.trd.aluminum", "Aluminum");
        add("metal.trd.bronze", "Bronze");
        add("metal.trd.tin", "Tin");
        add("metal.trd.zinc", "Zinc");
        add("metal.trd.titanium", "Titanium");
        add("metal.trd.lead", "Lead");
        add("metal.trd.beryllium", "Beryllium");
        add("metal.trd.industrial_copper", "Industrial Copper");
        add("metal.trd.tungsten", "Tungsten");
        add("metal.trd.neodymium", "Neodymium");

        // Cast Pickaxe Tooltips
        add("item.trd.cast_pickaxe.desc.charge", "§7Hold RMB for a powerful strike");
        add("item.trd.cast_pickaxe.desc.mining_power", "§6Power: %s");
        add("item.trd.cast_pickaxe.desc.vein_miner_info", "Vein Miner: %s");
        add("item.trd.cast_pickaxe.desc.tunnel_miner", "Tunnel Miner: %s");


        // Sequoia
        add(ModBlocks.SEQUOIA_BARK.get(), "Sequoia Bark");
        add(ModBlocks.SEQUOIA_HEARTWOOD.get(), "Sequoia Heartwood");
        add(ModBlocks.SEQUOIA_PLANKS.get(), "Sequoia Planks");
        add(ModBlocks.SEQUOIA_ROOTS.get(), "Sequoia Roots");
        add(ModBlocks.SEQUOIA_ROOTS_MOSSY.get(), "Mossy Sequoia Roots");
        add(ModBlocks.SEQUOIA_BARK_DARK.get(), "Dark Sequoia Bark");
        add(ModBlocks.SEQUOIA_BARK_MOSSY.get(), "Mossy Sequoia Bark");
        add(ModBlocks.SEQUOIA_BARK_LIGHT.get(), "Light Sequoia Bark");
        add(ModBlocks.SEQUOIA_DOOR.get(), "Sequoia Door");
        add(ModBlocks.SEQUOIA_TRAPDOOR.get(), "Sequoia Trapdoor");
        add(ModBlocks.SEQUOIA_BIOME_MOSS.get(), "Dark Moss");
        add(ModBlocks.SEQUOIA_LEAVES.get(), "Sequoia Leaves");
        add(ModBlocks.SEQUOIA_SLAB.get(), "Sequoia Slab");
        add(ModBlocks.SEQUOIA_STAIRS.get(), "Sequoia Stairs");


        // Smelting & Casting
        add(ModBlocks.SMALL_SMELTER.get(), "Small Smelter");
        add(ModBlocks.SMELTER.get(), "Smelter");
        add(ModBlocks.CASTING_DESCENT.get(), "Casting Trough");
        add(ModBlocks.CASTING_POT.get(), "Casting Pot");
        add(ModItems.HEATER_ITEM.get(), "Heater");
        add(ModItems.LIQUID_METAL.get(), "Liquid Metal");

        // Electronics & Energy
        add(ModItems.ENERGY_CELL_BASIC.get(), "Energy Cell");
        add(ModItems.CREATIVE_BATTERY.get(), "Creative Battery");
        add(ModItems.BATTERY.get(), "Battery");
        add(ModItems.BATTERY_ADVANCED.get(), "Advanced Battery");
        add(ModItems.BATTERY_LITHIUM.get(), "Lithium Battery");
        add(ModItems.BATTERY_TRIXITE.get(), "Trixite Battery");
        add(ModBlocks.MACHINE_BATTERY.get(), "Modular Energy Storage");
        add(ModBlocks.CONVERTER_BLOCK.get(), "Energy Converter");
        add(ModBlocks.WIRE_COATED.get(), "Coated Copper Wire");
        add(ModBlocks.PAINTABLE_WIRE.get(), "Paintable Wire");
        add(ModBlocks.MEDIUM_CONNECTOR.get(), "Medium Connector");
        add(ModBlocks.LARGE_CONNECTOR.get(), "Large Connector");
        add(ModBlocks.SWITCH.get(), "Switch");
        add(ModBlocks.VALVE.get(), "Valve");
        add(ModBlocks.TURRET_LIGHT_PLACER.get(), "Light Landing Turret \'Nagual\'");

        // Concrete Variants
        add(ModBlocks.CONCRETE.get(), "Concrete");
        add(ModBlocks.CONCRETE_SLAB.get(), "Concrete Slab");
        add(ModBlocks.CONCRETE_STAIRS.get(), "Concrete Stairs");
        add(ModBlocks.CONCRETE_RED.get(), "Red Concrete");
        add(ModBlocks.CONCRETE_RED_SLAB.get(), "Red Concrete Slab");
        add(ModBlocks.CONCRETE_RED_STAIRS.get(), "Red Concrete Stairs");
        add(ModBlocks.CONCRETE_BLUE.get(), "Blue Concrete");
        add(ModBlocks.CONCRETE_BLUE_SLAB.get(), "Blue Concrete Slab");
        add(ModBlocks.CONCRETE_BLUE_STAIRS.get(), "Blue Concrete Stairs");
        add(ModBlocks.CONCRETE_GREEN.get(), "Green Concrete");
        add(ModBlocks.CONCRETE_GREEN_SLAB.get(), "Green Concrete Slab");
        add(ModBlocks.CONCRETE_GREEN_STAIRS.get(), "Green Concrete Stairs");
        add(ModBlocks.CONCRETE_HAZARD_NEW.get(), "New Hazard Concrete");
        add(ModBlocks.CONCRETE_HAZARD_NEW_SLAB.get(), "New Hazard Concrete Slab");
        add(ModBlocks.CONCRETE_HAZARD_NEW_STAIRS.get(), "New Hazard Concrete Stairs");
        add(ModBlocks.CONCRETE_HAZARD_OLD.get(), "Old Hazard Concrete");
        add(ModBlocks.CONCRETE_HAZARD_OLD_SLAB.get(), "Old Hazard Concrete Slab");
        add(ModBlocks.CONCRETE_HAZARD_OLD_STAIRS.get(), "Old Hazard Concrete Stairs");
        add(ModBlocks.CONCRETE_TILE.get(), "Concrete Tile");
        add(ModBlocks.CONCRETE_TILE_SLAB.get(), "Concrete Tile Slab");
        add(ModBlocks.CONCRETE_TILE_STAIRS.get(), "Concrete Tile Stairs");
        add(ModBlocks.CONCRETE_TILE_ALT.get(), "Faceted Concrete Tile");
        add(ModBlocks.CONCRETE_TILE_ALT_SLAB.get(), "Faceted Concrete Tile Slab");
        add(ModBlocks.CONCRETE_TILE_ALT_STAIRS.get(), "Faceted Concrete Tile Stairs");
        add(ModBlocks.CONCRETE_TILE_ALT_BLUE.get(), "Painted Faceted Concrete Tile");
        add(ModBlocks.CONCRETE_TILE_ALT_BLUE_SLAB.get(), "Painted Faceted Concrete Tile Slab");
        add(ModBlocks.CONCRETE_TILE_ALT_BLUE_STAIRS.get(), "Painted Faceted Concrete Tile Stairs");
        add(ModBlocks.CONCRETE_STRIPPED.get(), "Light Textured Concrete");
        add(ModBlocks.CONCRETE_STRIPPED_SLAB.get(), "Light Textured Concrete Slab");
        add(ModBlocks.CONCRETE_STRIPPED_STAIRS.get(), "Light Textured Concrete Stairs");
        add(ModBlocks.CONCRETE_REINFORCED.get(), "Gray Textured Concrete");
        add(ModBlocks.CONCRETE_REINFORCED_SLAB.get(), "Gray Textured Concrete Slab");
        add(ModBlocks.CONCRETE_REINFORCED_STAIRS.get(), "Gray Textured Concrete Stairs");
        add(ModBlocks.CONCRETE_REINFORCED_HEAVY.get(), "Dark Textured Concrete");
        add(ModBlocks.CONCRETE_REINFORCED_HEAVY_SLAB.get(), "Dark Textured Concrete Slab");
        add(ModBlocks.CONCRETE_REINFORCED_HEAVY_STAIRS.get(), "Dark Textured Concrete Stairs");
        add(ModBlocks.CONCRETE_NET.get(), "Reinforced Concrete");

        // Bricks
        add(ModBlocks.FIREBRICK_BLOCK.get(), "Firebrick Block");
        add(ModBlocks.FIREBRICK_SLAB.get(), "Firebrick Slab");
        add(ModBlocks.FIREBRICK_STAIRS.get(), "Firebrick Stairs");
        add(ModBlocks.REINFORCEDBRICK_BLOCK.get(), "Dolomite Brick Block");
        add(ModBlocks.REINFORCEDBRICK_SLAB.get(), "Dolomite Brick Slab");
        add(ModBlocks.REINFORCEDBRICK_STAIRS.get(), "Dolomite Brick Stairs");
        add(ModItems.FIREBRICK.get(), "Firebrick");

        // Decorative Blocks
        add(ModBlocks.CRATE.get(), "Crate");
        add(ModBlocks.CRATE_AMMO.get(), "Ammo Crate");
        add(ModBlocks.BEAM_BLOCK.get(), "Beam Block");
        add(ModBlocks.STEEL_PROPS.get(), "Steel Props");
        add(ModBlocks.DIRT_ROUGH.get(), "Rough Dirt");
        add(ModBlocks.ROUND_LAMP.get(), "Round Lamp");
        add(ModBlocks.MORY_BLOCK.get(), "Mory Block");
        add(ModBlocks.DOLOMITE_TILE.get(), "Dolomite Tile");
        add(ModBlocks.TILE_LIGHT.get(), "Light Tile");
        add(ModBlocks.SULFUR_TILE.get(), "Sulfur Tile");
        add(ModBlocks.SULFUR_BRICKS.get(), "Sulfur Bricks");
        add(ModBlocks.NECROSIS_TEST.get(), "Necrosis Test Block");
        add(ModBlocks.NECROSIS_TEST2.get(), "Necrosis Test Block 2");
        add(ModBlocks.NECROSIS_TEST3.get(), "Necrosis Test Block 3");
        add(ModBlocks.NECROSIS_TEST4.get(), "Necrosis Test Block 4");
        add(ModBlocks.NECROSIS_PORTAL.get(), "Necrosis Portal");
        add(ModBlocks.WASTE_LOG.get(), "Waste Log");

        // Kinetic & Shafts
        add(ModBlocks.HAND_CRANK_BLOCK.get(), "Hand Crank");
        add(ModBlocks.SHAFT_LIGHT_IRON.get(), "Light Iron Shaft");
        add(ModBlocks.SHAFT_MEDIUM_IRON.get(), "Medium Iron Shaft");
        add(ModBlocks.SHAFT_HEAVY_IRON.get(), "Heavy Iron Shaft");
        add(ModBlocks.SHAFT_LIGHT_DURALUMIN.get(), "Light Duralumin Shaft");
        add(ModBlocks.SHAFT_MEDIUM_DURALUMIN.get(), "Medium Duralumin Shaft");
        add(ModBlocks.SHAFT_HEAVY_DURALUMIN.get(), "Heavy Duralumin Shaft");
        add(ModBlocks.SHAFT_LIGHT_STEEL.get(), "Light Steel Shaft");
        add(ModBlocks.SHAFT_MEDIUM_STEEL.get(), "Medium Steel Shaft");
        add(ModBlocks.SHAFT_HEAVY_STEEL.get(), "Heavy Steel Shaft");
        add(ModBlocks.SHAFT_LIGHT_TITANIUM.get(), "Light Titanium Shaft");
        add(ModBlocks.SHAFT_MEDIUM_TITANIUM.get(), "Medium Titanium Shaft");
        add(ModBlocks.SHAFT_HEAVY_TITANIUM.get(), "Heavy Titanium Shaft");
        add(ModBlocks.SHAFT_LIGHT_TUNGSTEN_CARBIDE.get(), "Light Tungsten Carbide Shaft");
        add(ModBlocks.SHAFT_MEDIUM_TUNGSTEN_CARBIDE.get(), "Medium Tungsten Carbide Shaft");
        add(ModBlocks.SHAFT_HEAVY_TUNGSTEN_CARBIDE.get(), "Heavy Tungsten Carbide Shaft");
        add(ModItems.BEVEL_GEAR.get(), "Bevel Gear");
        add(ModItems.GEAR1_STEEL.get(), "Small Steel Gear");
        add(ModItems.GEAR2_STEEL.get(), "Medium Steel Gear");
        add(ModItems.PULLEY.get(), "Pulley");
        add(ModItems.FLYWHEEL_LIGHT.get(), "Light Flywheel");
        add(ModItems.COPPER_ROTOR.get(), "Copper Rotor");
        add(ModBlocks.BEARING_BLOCK.get(), "Bearing");
        add(ModBlocks.MOTOR_ELECTRO.get(), "Electric Motor");
        add(ModBlocks.TACHOMETER.get(), "Tachometer");
        add(ModItems.STEAM_ENGINE_ITEM.get(), "Steam Engine");
        add(ModBlocks.DROBITEL.get(), "Crusher");
        add(ModBlocks.STATOR_BLOCK.get(), "Stator");

        // Barrels, Tanks & Fluids
        add(ModItems.CORRUPTED_BARREL_ITEM.get(), "Corrupted Barrel");
        add(ModItems.LEAKING_BARREL_ITEM.get(), "Leaking Barrel");
        add(ModItems.IRON_BARREL_ITEM.get(), "Iron Barrel");
        add(ModItems.STEEL_BARREL_ITEM.get(), "Steel Barrel");
        add(ModItems.LEAD_BARREL_ITEM.get(), "Lead Barrel");
        add(ModItems.INFINITE_FLUID_BARREL.get(), "Infinite Fluid Barrel");
        add(ModBlocks.FUEL_TANK_SMALL.get(), "Small Fuel Tank");
        add(ModBlocks.FUEL_TANK_BIG.get(), "Big Fuel Tank");
        add(ModBlocks.LOW_PRESSURE_STEAM_CONDENSER.get(), "Low Pressure Steam Condenser");

        // Conveyors & Storage
        add(ModBlocks.CONVEYOR_VSTAVSHIK.get(), "Conveyor Inserter");
        add(ModBlocks.CONVEYOR_IZVLEKATEL.get(), "Conveyor Extractor");
        add(ModBlocks.CONVEYOR.get(), "Conveyor");
        add(ModBlocks.STEEL_STORAGE.get(), "Steel Storage");

        // Weapons & Ammo
        add(ModItems.CAST_PICKAXE_IRON.get(), "Cast Iron Pickaxe");
        add(ModItems.CAST_PICKAXE_STEEL.get(), "Cast Steel Pickaxe");
        add(ModItems.GRENADIER_GOGGLES.get(), "Grenadier Goggles");
        add(ModBlocks.DET_MINER.get(), "Mining Charge");
        add(ModItems.DETONATOR.get(), "Detonator");
        add(ModItems.MULTI_DETONATOR.get(), "Multi-Detonator");
        add(ModItems.RANGE_DETONATOR.get(), "Long-Range Detonator");
        add(ModItems.TURRET_LIGHT_PORTATIVE_PLACER.get(), "Portable Light Turret");
        add(ModItems.MACHINEGUN.get(), "\'A.P. 17\'");
        add(ModItems.AMMO_TURRET.get(), "20mm Turret Round");
        add(ModItems.AMMO_TURRET_PIERCING.get(), "20mm Armor-Piercing Turret Round");
        add(ModItems.AMMO_TURRET_HOLLOW.get(), "20mm Hollow-Point Turret Round");
        add(ModItems.AMMO_TURRET_FIRE.get(), "20mm Incendiary Turret Round");
        add(ModItems.AMMO_TURRET_RADIO.get(), "20mm Turret Round with Radio Fuze");
        add(ModItems.MISSILE_100MM_HE.get(), "100mm HE Missile");
        add(ModItems.MISSILE_100MM_FIRE.get(), "100mm Incendiary Missile");

        // Resources & Materials
        add(ModItems.IRON_PLATE.get(), "Iron Plate");
        add(ModItems.TITANIUM_PLATE.get(), "Titanium Plate");
        add(ModItems.STEEL_PLATE.get(), "Steel Plate");
        add(ModItems.TUNGSTEN_PLATE.get(), "Tungsten Plate");
        add(ModItems.LEAD_PLATE.get(), "Lead Plate");
        add(ModItems.ALUMINUM_PLATE.get(), "Aluminum Plate");
        add(ModItems.INDUSTRIAL_COPPER_PLATE.get(), "Industrial Copper Plate");
        add(ModItems.GOLD_PLATE.get(), "Gold Plate");
        add(ModItems.CAST_PICKAXE_IRON_BASE.get(), "Cast Iron Pickaxe Base");
        add(ModItems.CAST_PICKAXE_STEEL_BASE.get(), "Cast Steel Pickaxe Base");
        add(ModItems.ROPE.get(), "Rope");
        add(ModItems.WOODEN_HANDLE.get(), "Wooden Handle");
        add(ModItems.FIRE_SMES.get(), "Fireproof Mixture");
        add(ModItems.DOLOMITE_SMES.get(), "Dolomite Mixture");
        add(ModItems.CONGLOMERATE_CHUNK.get(), "Conglomerate Chunk");
        add(ModItems.HARD_ROCK.get(), "Hard Rock");
        add(ModItems.DOLOMITE_CHUNK.get(), "Dolomite Chunk");
        add(ModItems.LIMESTONE_CHUNK.get(), "Limestone Chunk");
        add(ModItems.BAUXITE_CHUNK.get(), "Bauxite Chunk");
        add(ModItems.ASBESTOS.get(), "Asbestos");
        add(ModItems.CINNABAR.get(), "Cinnabar");
        add(ModItems.LIGNITE.get(), "Lignite");
        add(ModItems.FLUORITE.get(), "Fluorite");
        add(ModItems.SULFUR.get(), "Sulfur");
        add(ModItems.CONGLOMERATE_POWDER.get(), "Conglomerate Powder");
        add(ModItems.DOLOMITE_POWDER.get(), "Dolomite Powder");
        add(ModItems.LIMESTONE_POWDER.get(), "Limestone Powder");
        add(ModItems.BAUXITE_POWDER.get(), "Bauxite Powder");
        add(ModItems.FUEL_ASH.get(), "Fuel Ash");
        add(ModItems.TRASH.get(), "Trash");
        add(ModItems.SLAG.get(), "Slag");
        add(ModItems.BELT.get(), "Belt");
        add(ModItems.BEAM_PLACER.get(), "Beam Placer");
        add(ModItems.POKER.get(), "Poker");
        add(ModItems.SCREWDRIVER.get(), "Screwdriver");
        add(ModItems.CROWBAR.get(), "Crowbar");
        add(ModBlocks.LIGNITE_BLOCK.get(), "Lignite Block");

        // Ores & nature (updated)
        add(ModBlocks.ASBESOTS_ORE.get(), "Asbestos Ore");
        add(ModBlocks.LIGNITE_ORE.get(), "Lignite Ore");
        add(ModBlocks.CINNABAR_ORE.get(), "Cinnabar Ore");
        add(ModBlocks.CINNABAR_ORE_DEEPSLATE.get(), "Deepslate Cinnabar Ore");
        add(ModBlocks.FLUORITE_ORE.get(), "Fluorite Ore");
        add(ModBlocks.FLUORITE_ORE_DEEPSLATE.get(), "Deepslate Fluorite Ore");
        add(ModBlocks.SEQUESTRUM_ORE.get(), "Saltpeter Ore");

        add(ModBlocks.SEQUESTRUM_ORE_DEEPSLATE.get(), "Deepslate Sequestrum Ore");
        add(ModBlocks.SULFUR_ORE.get(), "Sulfur Ore");
        add(ModBlocks.SULFUR_ORE_DEEPSLATE.get(), "Deepslate Sulfur Ore");
        add(ModBlocks.CONGLOMERATE.get(), "Conglomerate");
        add(ModBlocks.DEPLETED_CONGLOMERATE.get(), "Depleted Conglomerate");
        add(ModBlocks.DOLOMITE.get(), "Unrefined Dolomite Deposit");
        add(ModBlocks.LIMESTONE.get(), "Unrefined Limestone Deposit");
        add(ModBlocks.SULFUR_CLUSTER.get(), "Unrefined Sulfur Deposit");
        add(ModBlocks.BAUXITE.get(), "Unrefined Bauxite Deposit");
        add(ModBlocks.MINERAL1.get(), "Sapphire-Bearing Cluster");
        add(ModBlocks.MINERAL3.get(), "Deep Sapphire-Bearing Cluster");
        add(ModBlocks.BASALT_ROUGH.get(), "Rough Basalt");

        // Spawn Eggs
        add(ModItems.DEPTH_WORM_SPAWN_EGG.get(), "Depth Worm Spawn Egg");
        add(ModItems.DEPTH_WORM_BRUTAL_SPAWN_EGG.get(), "Brutal Depth Worm Spawn Egg");
        add(ModItems.GRENADIER_ZOMBIE_SPAWN_EGG.get(), "Grenadier Zombie Spawn Egg");


        add(ModBlocks.ANTON_CHIGUR.get(), "Anton Chigur Block");
        add(ModBlocks.MINERAL_BLOCK2.get(), "Depth Sapphire Decorative Block");
        add(ModBlocks.MINERAL_TILE.get(), "Depth Sapphire Tile");
        add(ModBlocks.DECO_STEEL.get(), "Decorative Steel Block");
        add(ModBlocks.DECO_STEEL_DARK.get(), "Dark Decorative Steel Block");
        add(ModBlocks.DECO_STEEL_SMOG.get(), "Sooty Decorative Steel Block");
        add(ModBlocks.DECO_LEAD.get(), "Decorative Lead Block");
        add(ModBlocks.DECO_BEAM.get(), "Decorative Industrial Block");
        add(ModItems.WIRE_COIL.get(), "Copper Wire Spool");
        add(ModItems.COPPER_COIL.get(), "Stator Copper Coil");
        add(ModBlocks.CONNECTOR.get(), "Small Connector");
        add(ModBlocks.ELECTRO_FURNACE.get(), "Electric Furnace");
        add(ModItems.PROTECTOR_LEAD.get(), "Lead Internal Wall Protector");
        add(ModItems.PROTECTOR_STEEL.get(), "Steel Internal Wall Protector");
        add(ModItems.PROTECTOR_TUNGSTEN.get(), "Tungsten Internal Wall Protector");

        // Fluid pipes
        add(ModBlocks.BRONZE_FLUID_PIPE.get(), "Bronze Fluid Pipe");
        add(ModBlocks.STEEL_FLUID_PIPE.get(), "Steel Fluid Pipe");
        add(ModBlocks.LEAD_FLUID_PIPE.get(), "Lead Fluid Pipe");
        add(ModBlocks.TUNGSTEN_FLUID_PIPE.get(), "Tungsten Fluid Pipe");
        add(ModBlocks.PAINTABLE_PIPE.get(), "Paintable Fluid Pipe");

        // Machines
        add(ModItems.BOILER_ITEM.get(), "Copper Liquid Boiler");
        add(ModBlocks.WATER_PUMP_ITEM.get(), "Liquid Pump");

        // Casting molds
        add(ModItems.MOLD_INGOT.get(), "Ingot Casting Mold");
        add(ModItems.MOLD_PICKAXE.get(), "Pickaxe Casting Mold");
        add(ModItems.MOLD_EMPTY.get(), "Empty Casting Mold");
        add(ModItems.MOLD_NUGGET.get(), "Nugget Casting Mold");
        add(ModItems.MOLD_BLOCK.get(), "Block Casting Mold");
        add(ModItems.MOLD_PLATE.get(), "Plate Casting Mold");

        // Misc blocks & items
        add(ModBlocks.JERNOVA.get(), "Stone Millstone");
        add(ModItems.MORY_LAH.get(), "Inconceivably Suspicious Artifact Possessing the Power of a Thousand Suns");
        add(ModItems.GRENADE.get(), "Grenade");
        add(ModItems.GRENADEHE.get(), "High Explosive Grenade");
        add(ModItems.GRENADEFIRE.get(), "Incendiary Grenade");
        add(ModItems.GRENADESMART.get(), "Smart Grenade");
        add(ModItems.GRENADESLIME.get(), "Sticky Grenade");
        add(ModItems.GRENADE_IF.get(), "Impact Grenade");
        add(ModItems.GRENADE_IF_HE.get(), "HE Impact Grenade");
        add(ModItems.GRENADE_IF_SLIME.get(), "Sticky Impact Grenade");
        add(ModItems.GRENADE_IF_FIRE.get(), "Incendiary Impact Grenade");
        add(ModItems.GRENADE_NUC.get(), "Hydrogen-Cremating Grenade");
        add(ModItems.TURRET_CHIP.get(), "Turret Combat Chip");
        add(ModItems.GRAVITY_GRENADE.get(), "Gravi-Grenade");
        add(ModItems.MISSILE_100MM.get(), "100mm Missile (Small Charge)");
        add(ModBlocks.TROMBONE.get(), "Stationary Rocket Launcher 'Trombone'");
        add(ModItems.REINFORCEDBRICK.get(), "Dolomite Brick");
        add(ModItems.SEQUESTRUM.get(), "Saltpeter");


        // Necrosis
        add(ModBlocks.DEPTH_WORM_NEST.get(), "Depth Worm Hive Node");
        add(ModBlocks.HIVE_SOIL.get(), "Depth Worm Hive Flesh");
        add(ModBlocks.HIVE_ROOTS.get(), "Depth Worm Hive Nerve Endings");

        // Entities
        add("entity.trd.turret_light", "Light Turret");
        add("entity.trd.turret_light_linked", "Linked Light Turret");
        add("entity.trd.turret_bullet", "Turret Bullet");
        add("entity.trd.depth_worm", "Depth Worm");
        add("entity.trd.grenade_projectile", "Grenade");
        add("entity.trd.grenadehe_projectile", "HE Grenade");
        add("entity.trd.grenadefire_projectile", "Incendiary Grenade");
        add("entity.trd.grenadesmart_projectile", "Smart Grenade");
        add("entity.trd.grenadeslime_projectile", "Slime Grenade");
        add("entity.trd.grenade_if_projectile", "Impact Grenade");
        add("entity.trd.grenade_if_fire_projectile", "Incendiary Impact Grenade");
        add("entity.trd.grenade_if_slime_projectile", "Slime Impact Grenade");
        add("entity.trd.grenade_if_he_projectile", "HE Impact Grenade");
        add("entity.trd.grenade_nuc_projectile", "Nuclear Grenade");

        // Misc Tooltips
        add("item.trd.hot_ingot.tooltip", "§6§lHOT! §r§7(%s%%)");
        add("item.trd.grenadier_goggles.desc.explosion_resist", "Explosion Resistance: +%s%%");
    }

    private void addRussian() {
        // Вкладки креатива
        add("itemGroup.trd.trd_build_tab", "Строительные блоки");
        add("itemGroup.trd.trd_tech_tab", "Технологии");
        add("itemGroup.trd.trd_weapons_tab", "Арсенал");
        add("itemGroup.trd.trd_recourses_tab", "Ресурсы");
        add("itemGroup.trd.trd_nature_tab", "Природа");

        // JEI Категории
        add("jei.category.trd.smelting", "Плавка");
        add("jei.category.trd.casting", "Литьё");
        add("jei.category.trd.alloying", "Сплавление");
        add("jei.category.trd.millstone", "Жернов");
        add("jei.category.trd.boiling", "Бойлер");
        add("jei.category.trd.steam_engine", "Паровой двигатель");
        add("jei.category.trd.condensing", "Конденсация");
        add("jei.category.trd.electric_furnace", "Электропечь");

        // Литые кирки

        add("item.trd.cast_pickaxe.desc.charge", "§7Зажмите ПКМ для мощного удара");
        add("item.trd.cast_pickaxe.desc.mining_power", "§6Мощность: %s");
        add("item.trd.cast_pickaxe.desc.vein_miner_info", "Жильный майнер: %s");
        add("item.trd.cast_pickaxe.desc.tunnel_miner", "Туннельный майнер: %s");

        // Гренадёр
        add(ModItems.GRENADIER_GOGGLES.get(), "Очки гренадёра");
        add(ModItems.FLYWHEEL_LIGHT.get(), "Лёгкий маховик");
        add("item.trd.grenadier_goggles.desc.explosion_resist", "Защита от взрывов: +%s%%");

        // Металлы
        add("metal.trd.gold", "Золото");
        add("metal.trd.iron", "Железо");
        add("metal.trd.copper", "Медь");
        add("metal.trd.netherite", "Незерит");
        add("metal.trd.steel", "Сталь");
        add("metal.trd.aluminum", "Алюминий");
        add("metal.trd.bronze", "Бронза");
        add("metal.trd.tin", "Олово");
        add("metal.trd.zinc", "Цинк");
        add("metal.trd.titanium", "Титан");
        add("metal.trd.lead", "Свинец");
        add("metal.trd.beryllium", "Бериллий");
        add("metal.trd.industrial_copper", "Промышленная медь");
        add("metal.trd.tungsten", "Вольфрам");
        add("metal.trd.neodymium", "Неодим");

        // Уровни нагревателя
        add("gui.trd.heater.tier0", "Уровень 0");
        add("gui.trd.heater.tier1", "Уровень I");
        add("gui.trd.heater.tier2", "Уровень II");
        add("gui.trd.heater.tier3", "Уровень III");
        add("gui.trd.heater.tier4", "Уровень IV");
        add("gui.trd.heater.tier5", "Уровень V");

        add("item.trd.hot_ingot.tooltip", "§6§lРАСКАЛЁННЫЙ! §r§7(%s%%)");

        // Секвойя
        add(ModBlocks.SEQUOIA_BARK.get(), "Кора секвойи");
        add(ModBlocks.SEQUOIA_HEARTWOOD.get(), "Бревно секвойи");
        add(ModBlocks.SEQUOIA_PLANKS.get(), "Доски из секвойи");
        add(ModBlocks.SEQUOIA_ROOTS.get(), "Корни секвойи");
        add(ModBlocks.SEQUOIA_ROOTS_MOSSY.get(), "Корни секвойи с мхом");
        add(ModBlocks.SEQUOIA_BARK_DARK.get(), "Тёмная кора секвойи");
        add(ModBlocks.SEQUOIA_BARK_MOSSY.get(), "Кора секвойи с мхом");
        add(ModBlocks.SEQUOIA_BARK_LIGHT.get(), "Светлая кора секвойи");
        add(ModBlocks.SEQUOIA_DOOR.get(), "Дверь из секвойи");
        add(ModBlocks.SEQUOIA_TRAPDOOR.get(), "Люк из секвойи");
        add(ModBlocks.SEQUOIA_BIOME_MOSS.get(), "Тёмный мох");
        add(ModBlocks.SEQUOIA_LEAVES.get(), "Листья секвойи");
        add(ModBlocks.SEQUOIA_SLAB.get(), "Плита из секвойи");
        add(ModBlocks.SEQUOIA_STAIRS.get(), "Ступени из секвойи");

        // Электроника
        add(ModItems.ENERGY_CELL_BASIC.get(), "Энергетическая ячейка");
        add(ModItems.CREATIVE_BATTERY.get(), "Бесконечный аккумулятор");
        add(ModItems.BATTERY.get(), "Аккумулятор");
        add(ModItems.BATTERY_ADVANCED.get(), "Улучшенный аккумулятор");
        add(ModItems.BATTERY_LITHIUM.get(), "Литий-ионный аккумулятор");
        add(ModItems.BATTERY_TRIXITE.get(), "Продвинутый аккумулятор");
        add(ModBlocks.MACHINE_BATTERY.get(), "Модульное энергохранилище");
        add(ModBlocks.CONVERTER_BLOCK.get(), "Энергетический конвертер");
        add(ModBlocks.WIRE_COATED.get(), "Провод из красной меди");
        add(ModBlocks.PAINTABLE_WIRE.get(), "Окрашиваемый провод");
        add(ModBlocks.CONNECTOR.get(), "Малый коннектор");
        add(ModBlocks.MEDIUM_CONNECTOR.get(), "Средний коннектор");
        add(ModBlocks.LARGE_CONNECTOR.get(), "Большой коннектор");
        add(ModBlocks.SWITCH.get(), "Рубильник");
        add(ModBlocks.VALVE.get(), "Жидкостный клапан");
        add(ModBlocks.ELECTRO_FURNACE.get(), "Электро-печь");
        add(ModBlocks.TURRET_LIGHT_PLACER.get(), "Лёгкая десантная турель \'Нагваль\'");

        // Формы
        add(ModItems.MOLD_INGOT.get(), "Литейная форма слитка");
        add(ModItems.MOLD_PICKAXE.get(), "Литейная форма кирки");
        add(ModItems.MOLD_EMPTY.get(), "Пустая литейная форма");
        add(ModItems.MOLD_NUGGET.get(), "Литейная форма самородка");
        add(ModItems.MOLD_BLOCK.get(), "Литейная форма блока");
        add(ModItems.MOLD_PLATE.get(), "Литейная форма пластины");

        // Плавильные установки
        add(ModBlocks.SMALL_SMELTER.get(), "Малая плавильня");
        add(ModBlocks.SMELTER.get(), "Плавильня");
        add(ModBlocks.JERNOVA.get(), "Каменные жернова");
        add(ModBlocks.CASTING_DESCENT.get(), "Литейный желоб");
        add(ModBlocks.CASTING_POT.get(), "Литейный котёл");
        add(ModItems.HEATER_ITEM.get(), "Нагреватель");
        add(ModItems.LIQUID_METAL.get(), "Жидкий металл");

        // Некроз
        add(ModBlocks.DEPTH_WORM_NEST.get(), "Узел улья глубинного червя");
        add(ModBlocks.HIVE_SOIL.get(), "Плоть улья глубинного червя");
        add(ModBlocks.HIVE_ROOTS.get(), "Нервные окончания улья глубинного червя");

        // Варианты бетона
        add(ModBlocks.CONCRETE.get(), "Бетон");
        add(ModBlocks.CONCRETE_SLAB.get(), "Бетонная плита");
        add(ModBlocks.CONCRETE_STAIRS.get(), "Бетонные ступени");
        add(ModBlocks.CONCRETE_RED.get(), "Красный бетон");
        add(ModBlocks.CONCRETE_RED_SLAB.get(), "Плита из красного бетона");
        add(ModBlocks.CONCRETE_RED_STAIRS.get(), "Ступени из красного бетона");
        add(ModBlocks.CONCRETE_BLUE.get(), "Синий бетон");
        add(ModBlocks.CONCRETE_BLUE_SLAB.get(), "Плита из синего бетона");
        add(ModBlocks.CONCRETE_BLUE_STAIRS.get(), "Ступени из синего бетона");
        add(ModBlocks.CONCRETE_GREEN.get(), "Зелёный бетон");
        add(ModBlocks.CONCRETE_GREEN_SLAB.get(), "Плита из зелёного бетона");
        add(ModBlocks.CONCRETE_GREEN_STAIRS.get(), "Ступени из зелёного бетона");
        add(ModBlocks.CONCRETE_HAZARD_NEW.get(), "Бетон с разметкой");
        add(ModBlocks.CONCRETE_HAZARD_NEW_SLAB.get(), "Плита из бетона с разметкой");
        add(ModBlocks.CONCRETE_HAZARD_NEW_STAIRS.get(), "Ступени из бетона с разметкой");
        add(ModBlocks.CONCRETE_HAZARD_OLD.get(), "Изношенный бетон с разметкой");
        add(ModBlocks.CONCRETE_HAZARD_OLD_SLAB.get(), "Плита из изношенного бетона с разметкой");
        add(ModBlocks.CONCRETE_HAZARD_OLD_STAIRS.get(), "Ступени из изношенного бетона с разметкой");
        add(ModBlocks.CONCRETE_TILE.get(), "Бетонная плитка");
        add(ModBlocks.CONCRETE_TILE_SLAB.get(), "Плита из бетонной плитки");
        add(ModBlocks.CONCRETE_TILE_STAIRS.get(), "Ступени из бетонной плитки");
        add(ModBlocks.CONCRETE_TILE_ALT.get(), "Гранённая бетонная плитка");
        add(ModBlocks.CONCRETE_TILE_ALT_SLAB.get(), "Плита из гранённой бетонной плитки");
        add(ModBlocks.CONCRETE_TILE_ALT_STAIRS.get(), "Ступени из гранённой бетонной плитки");
        add(ModBlocks.CONCRETE_TILE_ALT_BLUE.get(), "Окрашенная гранённая бетонная плитка");
        add(ModBlocks.CONCRETE_TILE_ALT_BLUE_SLAB.get(), "Плита из окрашенной гранённой бетонной плитки");
        add(ModBlocks.CONCRETE_TILE_ALT_BLUE_STAIRS.get(), "Ступени из окрашенной гранённой бетонной плитки");
        add(ModBlocks.CONCRETE_STRIPPED.get(), "Светлый текстурированный бетон");
        add(ModBlocks.CONCRETE_STRIPPED_SLAB.get(), "Плита из светлого текстурированного бетона");
        add(ModBlocks.CONCRETE_STRIPPED_STAIRS.get(), "Ступени из светлого текстурированного бетона");
        add(ModBlocks.CONCRETE_REINFORCED.get(), "Серый текстурированный бетон");
        add(ModBlocks.CONCRETE_REINFORCED_SLAB.get(), "Плита из серого текстурированного бетона");
        add(ModBlocks.CONCRETE_REINFORCED_STAIRS.get(), "Ступени из серого текстурированного бетона");
        add(ModBlocks.CONCRETE_REINFORCED_HEAVY.get(), "Тёмный текстурированный бетон");
        add(ModBlocks.CONCRETE_REINFORCED_HEAVY_SLAB.get(), "Плита из тёмного текстурированного бетона");
        add(ModBlocks.CONCRETE_REINFORCED_HEAVY_STAIRS.get(), "Ступени из тёмного текстурированного бетона");
        add(ModBlocks.CONCRETE_NET.get(), "Усиленный бетон");

        // Кирпичи
        add(ModBlocks.FIREBRICK_BLOCK.get(), "Блок огнеупорного кирпича");
        add(ModBlocks.FIREBRICK_SLAB.get(), "Плита из огнеупорного кирпича");
        add(ModBlocks.FIREBRICK_STAIRS.get(), "Ступени из огнеупорного кирпича");
        add(ModBlocks.REINFORCEDBRICK_BLOCK.get(), "Блок доломитового кирпича");
        add(ModBlocks.REINFORCEDBRICK_SLAB.get(), "Плита из доломитового кирпича");
        add(ModBlocks.REINFORCEDBRICK_STAIRS.get(), "Ступени из доломитового кирпича");
        add(ModItems.FIREBRICK.get(), "Огнеупорный кирпич");
        add(ModItems.REINFORCEDBRICK.get(), "Доломитовый кирпич");

        // Декоративные блоки
        add(ModBlocks.CRATE.get(), "Ящик");
        add(ModBlocks.CRATE_AMMO.get(), "Ящик с патронами");
        add(ModBlocks.BEAM_BLOCK.get(), "Балка");
        add(ModBlocks.STEEL_PROPS.get(), "Стальные подпорки");
        add(ModBlocks.DECO_STEEL.get(), "Декоративный стальной блок");
        add(ModBlocks.DECO_STEEL_DARK.get(), "Тёмный декоративный стальной блок");
        add(ModBlocks.DECO_STEEL_SMOG.get(), "Закоптелый декоративный стальной блок");
        add(ModBlocks.DECO_LEAD.get(), "Декоративный свинцовый блок");
        add(ModBlocks.DECO_BEAM.get(), "Декоративный индустриальный блок");
        add(ModBlocks.DIRT_ROUGH.get(), "Грубая земля");
        add(ModBlocks.ROUND_LAMP.get(), "Круглая лампа");
        add(ModBlocks.MORY_BLOCK.get(), "Блок Мори");
        add(ModBlocks.ANTON_CHIGUR.get(), "Блок Антона Чигура");
        add(ModBlocks.MINERAL_BLOCK2.get(), "Декоративный блок из глубинного сапфира");
        add(ModBlocks.MINERAL_TILE.get(), "Плитка из глубинного сапфира");
        add(ModBlocks.DOLOMITE_TILE.get(), "Доломитовая плитка");
        add(ModBlocks.TILE_LIGHT.get(), "Асбестовая плитка");
        add(ModBlocks.SULFUR_TILE.get(), "Серная плитка");
        add(ModBlocks.SULFUR_BRICKS.get(), "Серные кирпичи");
        add(ModBlocks.NECROSIS_TEST.get(), "Тестовый блок Некроза");
        add(ModBlocks.NECROSIS_TEST2.get(), "Тестовый блок Некроза 2");
        add(ModBlocks.NECROSIS_TEST3.get(), "Тестовый блок Некроза 3");
        add(ModBlocks.NECROSIS_TEST4.get(), "Тестовый блок Некроза 4");
        add(ModBlocks.NECROSIS_PORTAL.get(), "Портал Некроза");
        add(ModBlocks.WASTE_LOG.get(), "Обугленное бревно");

        // Кинетика и валы
        add(ModBlocks.HAND_CRANK_BLOCK.get(), "Ручной привод");
        add(ModBlocks.SHAFT_LIGHT_IRON.get(), "Лёгкий железный вал");
        add(ModBlocks.SHAFT_MEDIUM_IRON.get(), "Средний железный вал");
        add(ModBlocks.SHAFT_HEAVY_IRON.get(), "Тяжёлый железный вал");
        add(ModBlocks.SHAFT_LIGHT_DURALUMIN.get(), "Лёгкий дюралюминиевый вал");
        add(ModBlocks.SHAFT_MEDIUM_DURALUMIN.get(), "Средний дюралюминиевый вал");
        add(ModBlocks.SHAFT_HEAVY_DURALUMIN.get(), "Тяжёлый дюралюминиевый вал");
        add(ModBlocks.SHAFT_LIGHT_STEEL.get(), "Лёгкий стальной вал");
        add(ModBlocks.SHAFT_MEDIUM_STEEL.get(), "Средний стальной вал");
        add(ModBlocks.SHAFT_HEAVY_STEEL.get(), "Тяжёлый стальной вал");
        add(ModBlocks.SHAFT_LIGHT_TITANIUM.get(), "Лёгкий титановый вал");
        add(ModBlocks.SHAFT_MEDIUM_TITANIUM.get(), "Средний титановый вал");
        add(ModBlocks.SHAFT_HEAVY_TITANIUM.get(), "Тяжёлый титановый вал");
        add(ModBlocks.SHAFT_LIGHT_TUNGSTEN_CARBIDE.get(), "Лёгкий вал из карбида вольфрама");
        add(ModBlocks.SHAFT_MEDIUM_TUNGSTEN_CARBIDE.get(), "Средний вал из карбида вольфрама");
        add(ModBlocks.SHAFT_HEAVY_TUNGSTEN_CARBIDE.get(), "Тяжёлый вал из карбида вольфрама");
        add(ModItems.BEVEL_GEAR.get(), "Коническая шестерня");
        add(ModItems.GEAR1_STEEL.get(), "Стальная шестерня (малая)");
        add(ModItems.GEAR2_STEEL.get(), "Стальная шестерня (средняя)");
        add(ModItems.PULLEY.get(), "Шкив");
        add(ModItems.COPPER_ROTOR.get(), "Медный ротор");
        add(ModItems.COPPER_COIL.get(), "Медная катушка статора");
        add(ModBlocks.BEARING_BLOCK.get(), "Подшипник");
        add(ModBlocks.MOTOR_ELECTRO.get(), "Электромотор");
        add(ModBlocks.TACHOMETER.get(), "Тахометр");
        add(ModItems.STEAM_ENGINE_ITEM.get(), "Паровой двигатель");
        add(ModBlocks.DROBITEL.get(), "Дробитель");
        add(ModBlocks.STATOR_BLOCK.get(), "Статор");

        // Бочки, баки и жидкости
        add(ModItems.CORRUPTED_BARREL_ITEM.get(), "Выжженная бочка");
        add(ModItems.LEAKING_BARREL_ITEM.get(), "Протекающая бочка");
        add(ModItems.IRON_BARREL_ITEM.get(), "Железная бочка");
        add(ModItems.STEEL_BARREL_ITEM.get(), "Стальная бочка");
        add(ModItems.LEAD_BARREL_ITEM.get(), "Свинцовая бочка");
        add(ModItems.INFINITE_FLUID_BARREL.get(), "Бесконечный жидкостный источник");
        add(ModBlocks.FUEL_TANK_SMALL.get(), "Малая жидкостная цистерна");
        add(ModBlocks.FUEL_TANK_BIG.get(), "Большая жидкостная цистерна");
        add(ModItems.BOILER_ITEM.get(), "Медный жидкостный бойлер");
        add(ModBlocks.LOW_PRESSURE_STEAM_CONDENSER.get(), "Конденсатор пара низкого давления");
        add(ModBlocks.WATER_PUMP_ITEM.get(), "Жидкостная помпа");
        add(ModItems.PROTECTOR_LEAD.get(), "Свинцовый протектор внутренних стенок");
        add(ModItems.PROTECTOR_STEEL.get(), "Стальной протектор внутренних стенок");
        add(ModItems.PROTECTOR_TUNGSTEN.get(), "Вольфрамовый протектор внутренних стенок");
        add(ModBlocks.BRONZE_FLUID_PIPE.get(), "Бронзовая жидкостная труба");
        add(ModBlocks.STEEL_FLUID_PIPE.get(), "Стальная жидкостная труба");
        add(ModBlocks.LEAD_FLUID_PIPE.get(), "Свинцовая жидкостная труба");
        add(ModBlocks.TUNGSTEN_FLUID_PIPE.get(), "Вольфрамовая жидкостная труба");
        add(ModBlocks.PAINTABLE_PIPE.get(), "Окрашиваемая жидкостная труба");

        // Конвейеры и хранилища
        add(ModBlocks.CONVEYOR_VSTAVSHIK.get(), "Конвейерный вставщик");
        add(ModBlocks.CONVEYOR_IZVLEKATEL.get(), "Конвейерный извлекатель");
        add(ModBlocks.CONVEYOR.get(), "Конвейер");
        add(ModBlocks.STEEL_STORAGE.get(), "Стальное хранилище");

        // Оружие и боеприпасы
        add(ModItems.CAST_PICKAXE_IRON.get(), "Литая железная кирка");
        add(ModItems.CAST_PICKAXE_STEEL.get(), "Литая стальная кирка");
        add(ModItems.GRAVITY_GRENADE.get(), "Грави-граната");
        add(ModBlocks.DET_MINER.get(), "Шахтёрский заряд");
        add(ModItems.DETONATOR.get(), "Детонатор");
        add(ModItems.MULTI_DETONATOR.get(), "Мульти-детонатор");
        add(ModItems.RANGE_DETONATOR.get(), "Детонатор дальнего действия");
        add(ModItems.MORY_LAH.get(), "Невообразимо подозрительное изделие обладающее силой тысяч Солнц");
        add(ModItems.GRENADE.get(), "Граната");
        add(ModItems.GRENADEHE.get(), "Фугасная граната");
        add(ModItems.GRENADEFIRE.get(), "Зажигательная граната");
        add(ModItems.GRENADESMART.get(), "УМная граната");
        add(ModItems.GRENADESLIME.get(), "Липкая граната");
        add(ModItems.GRENADE_IF.get(), "Ударная граната");
        add(ModItems.GRENADE_IF_HE.get(), "Фугасная ударная граната");
        add(ModItems.GRENADE_IF_SLIME.get(), "Ударная липкая граната");
        add(ModItems.GRENADE_IF_FIRE.get(), "Зажигательная ударная граната");
        add(ModItems.GRENADE_NUC.get(), "Водород-кремирующая граната");
        add(ModItems.TURRET_CHIP.get(), "Турельный боевой чип");
        add(ModItems.TURRET_LIGHT_PORTATIVE_PLACER.get(), "Портативная лёгкая десантная турель \'Нагваль\'");
        add(ModItems.MACHINEGUN.get(), "'А.П. 17'");
        add(ModBlocks.TROMBONE.get(), "Стационарная ракетная установка 'Тромбон'");
        add(ModItems.AMMO_TURRET.get(), "20-мм турельный боеприпас");
        add(ModItems.AMMO_TURRET_PIERCING.get(), "20-мм бронебойный боеприпас для турели");
        add(ModItems.AMMO_TURRET_HOLLOW.get(), "20-мм экспансивный боеприпас для турели");
        add(ModItems.AMMO_TURRET_FIRE.get(), "20-мм зажигательный боеприпас для турели");
        add(ModItems.AMMO_TURRET_RADIO.get(), "20-мм боеприпас для турели с радиовзрывателем");
        add(ModItems.MISSILE_100MM.get(), "100-мм ракета (малый заряд)");
        add(ModItems.MISSILE_100MM_HE.get(), "100-мм фугасная ракета");
        add(ModItems.MISSILE_100MM_FIRE.get(), "100-мм зажигательная ракета");

        // Ресурсы и материалы
        add(ModItems.IRON_PLATE.get(), "Железная пластина");
        add(ModItems.TITANIUM_PLATE.get(), "Титановая пластина");
        add(ModItems.STEEL_PLATE.get(), "Стальная пластина");
        add(ModItems.TUNGSTEN_PLATE.get(), "Вольфрамовая пластина");
        add(ModItems.LEAD_PLATE.get(), "Свинцовая пластина");
        add(ModItems.ALUMINUM_PLATE.get(), "Алюминиевая пластина");
        add(ModItems.INDUSTRIAL_COPPER_PLATE.get(), "Промышленномедная пластина");
        add(ModItems.GOLD_PLATE.get(), "Золотая пластина");
        add(ModItems.CAST_PICKAXE_IRON_BASE.get(), "Основа литой железной кирки");
        add(ModItems.CAST_PICKAXE_STEEL_BASE.get(), "Основа литой стальной кирки");
        add(ModItems.ROPE.get(), "Верёвка");
        add(ModItems.WOODEN_HANDLE.get(), "Деревянная рукоять");
        add(ModItems.FIRE_SMES.get(), "Огнеупорная смесь");
        add(ModItems.DOLOMITE_SMES.get(), "Доломитовая смесь");
        add(ModItems.CONGLOMERATE_CHUNK.get(), "Кусок конгломерата");
        add(ModItems.HARD_ROCK.get(), "Твёрдая порода");
        add(ModItems.DOLOMITE_CHUNK.get(), "Кусок доломита");
        add(ModItems.LIMESTONE_CHUNK.get(), "Кусок известняка");
        add(ModItems.BAUXITE_CHUNK.get(), "Кусок боксита");
        add(ModItems.ASBESTOS.get(), "Асбест");
        add(ModItems.CINNABAR.get(), "Киноварь");
        add(ModItems.LIGNITE.get(), "Лигнит");
        add(ModItems.FLUORITE.get(), "Флюорит");
        add(ModItems.SEQUESTRUM.get(), "Селитра");
        add(ModItems.SULFUR.get(), "Сера");
        add(ModItems.CONGLOMERATE_POWDER.get(), "Порошок конгломерата");
        add(ModItems.DOLOMITE_POWDER.get(), "Порошок доломита");
        add(ModItems.LIMESTONE_POWDER.get(), "Порошок известняка");
        add(ModItems.BAUXITE_POWDER.get(), "Порошок боксита");
        add(ModItems.FUEL_ASH.get(), "Топливный пепел");
        add(ModItems.TRASH.get(), "Мусор");
        add(ModItems.SLAG.get(), "Шлак");
        add(ModItems.BELT.get(), "Ремень");
        add(ModItems.WIRE_COIL.get(), "Катушка медного провода");
        add(ModItems.BEAM_PLACER.get(), "Установщик балок");
        add(ModItems.POKER.get(), "Кочерга");
        add(ModItems.SCREWDRIVER.get(), "Отвёртка");
        add(ModItems.CROWBAR.get(), "Монтировка");
        add(ModBlocks.LIGNITE_BLOCK.get(), "Блок лигнита");

        // Природа и руды
        add(ModBlocks.ASBESOTS_ORE.get(), "Асбестовая руда");
        add(ModBlocks.LIGNITE_ORE.get(), "Лигнитовая руда");
        add(ModBlocks.CINNABAR_ORE.get(), "Киноварная руда");
        add(ModBlocks.CINNABAR_ORE_DEEPSLATE.get(), "Киновароносный глубинный сланец");
        add(ModBlocks.FLUORITE_ORE.get(), "Флюоритовая руда");
        add(ModBlocks.FLUORITE_ORE_DEEPSLATE.get(), "Флюоритоносный глубинный сланец");
        add(ModBlocks.SEQUESTRUM_ORE.get(), "Селитровая руда");
        add(ModBlocks.SEQUESTRUM_ORE_DEEPSLATE.get(), "Селитроносный глубинный сланец");
        add(ModBlocks.SULFUR_ORE.get(), "Серная руда");
        add(ModBlocks.SULFUR_ORE_DEEPSLATE.get(), "Серноносный глубинный сланец");
        add(ModBlocks.CONGLOMERATE.get(), "Конгломерат");
        add(ModBlocks.DEPLETED_CONGLOMERATE.get(), "Истощённый конгломерат");
        add(ModBlocks.DOLOMITE.get(), "Неочищенная доломитовая залежа");
        add(ModBlocks.LIMESTONE.get(), "Неочищенная известняковая залежа");
        add(ModBlocks.SULFUR_CLUSTER.get(), "Неочищенная серная залежа");
        add(ModBlocks.BAUXITE.get(), "Неочищенная бокситовая залежа");
        add(ModBlocks.MINERAL1.get(), "Сапфироносный кластер");
        add(ModBlocks.MINERAL3.get(), "Глубинный сапфироносный кластер");
        add(ModBlocks.BASALT_ROUGH.get(), "Грубый базальт");

        // Яйца призыва
        add(ModItems.DEPTH_WORM_SPAWN_EGG.get(), "Яйцо призыва глубинного червя");
        add(ModItems.DEPTH_WORM_BRUTAL_SPAWN_EGG.get(), "Яйцо призыва брутального глубинного червя");
        add(ModItems.GRENADIER_ZOMBIE_SPAWN_EGG.get(), "Яйцо призыва зомби-гренадёра");

        // Сущности
        add("entity.trd.turret_light", "Лёгкая турель");
        add("entity.trd.turret_light_linked", "Связанная лёгкая турель");
        add("entity.trd.turret_bullet", "Пуля турели");
        add("entity.trd.depth_worm", "Глубинный червь");
        add("entity.trd.grenade_projectile", "Граната");
        add("entity.trd.grenadehe_projectile", "Фугасная граната");
        add("entity.trd.grenadefire_projectile", "Зажигательная граната");
        add("entity.trd.grenadesmart_projectile", "Умная граната");
        add("entity.trd.grenadeslime_projectile", "Слизевая граната");
        add("entity.trd.grenade_if_projectile", "Ударная граната");
        add("entity.trd.grenade_if_fire_projectile", "Зажигательная ударная граната");
        add("entity.trd.grenade_if_slime_projectile", "Ударная слизевая граната");
        add("entity.trd.grenade_if_he_projectile", "Фугасная ударная граната");
        add("entity.trd.grenade_nuc_projectile", "Ядерная граната");

        // Жидкостный идентификатор
        add("item.trd.fluid_identifier", "Жидкостный идентификатор");
        add("message.trd.selected_fluid", "Выбрано");
        add("tooltip.trd.no_fluid", "Жидкость не выбрана");
        add("tooltip.trd.shaft_material", "Материал");
        add("tooltip.trd.max_speed", "Макс. скорость");
        add("tooltip.trd.max_torque", "Макс. момент");
        add("tooltip.trd.inertia", "Инерция");
        add("message.trd.too_far_from_support", "Слишком длинный пролёт! Макс. расстояние от опоры для этого диаметра: %s бл.");

        // Fluids
        addFluidTranslations("hydrogen_peroxide", "Пероксид водорода", null, "Hydrogen Peroxide");
        addFluidTranslations("sulfuric_acid", "Серная кислота", null, "Sulfuric Acid");
        addFluidTranslations("natural_gas", "Природный газ", null, "Natural Gas");
        addFluidTranslations("steam", "Пар", null, "Steam");
        addFluidTranslations("low_pressure_steam", "Пар низкого давления", null, "Low Pressure Steam");
        addFluidTranslations("water", "Вода", "Вода", "Water");
        addFluidTranslations("lava", "Лава", "Лава", "Lava");
    }
}
